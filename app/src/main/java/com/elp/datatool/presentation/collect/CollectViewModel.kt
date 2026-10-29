package com.elp.datatool.presentation.collect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elp.datatool.domain.model.DataSource
import com.elp.datatool.domain.model.FetchErrorKind
import com.elp.datatool.domain.model.FetchException
import com.elp.datatool.domain.model.QueueState
import com.elp.datatool.domain.model.SearchHit
import com.elp.datatool.domain.model.ValidationResult
import com.elp.datatool.domain.model.VerifiedPage
import com.elp.datatool.domain.model.parseInputLines
import com.elp.datatool.domain.repository.CollectionQueue
import com.elp.datatool.domain.repository.ProductSearchRepository
import com.elp.datatool.domain.repository.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QueueItem(val raw: String, val validation: ValidationResult) {
    val isValid: Boolean get() = validation is ValidationResult.Valid
}

data class CollectUiState(
    val source: DataSource = DataSource.UNITY_ASSET_STORE,
    val input: String = "",
    val items: List<QueueItem> = emptyList(),
    val queue: QueueState = QueueState(),
    /** Shows the result of the last batch until the user edits the input. */
    val showSummary: Boolean = false,
) {
    val validCount: Int get() = items.count { it.isValid }
    val invalidCount: Int get() = items.size - validCount
    val validProductIds: List<String>
        get() = items.mapNotNull { (it.validation as? ValidationResult.Valid)?.productId }
}

enum class SearchStatus { IDLE, LOADING, LOADED, FAILED, UNSUPPORTED }

data class SearchUiState(
    val query: String = "",
    val status: SearchStatus = SearchStatus.IDLE,
    val hits: List<SearchHit> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val error: FetchErrorKind? = null,
    /** Set when the site asks for human verification that the user can complete in-app. */
    val challengeUrl: String? = null,
)

sealed interface CollectEvent {
    data object ClipboardEmpty : CollectEvent
    data class SourceUnsupported(val source: DataSource) : CollectEvent
}

/** Shared by the Collect screen and the Search products screen. */
class CollectViewModel(
    private val searchRepository: ProductSearchRepository,
    private val queue: CollectionQueue,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        CollectUiState(
            source = settingsRepository.settings.value.selectedSource,
            queue = queue.state.value,
        )
    )
    val uiState: StateFlow<CollectUiState> = _uiState.asStateFlow()

    private val _searchState = MutableStateFlow(SearchUiState())
    val searchState: StateFlow<SearchUiState> = _searchState.asStateFlow()
    val processingVerification = queue.verification

    private val _events = Channel<CollectEvent>(Channel.BUFFERED)
    val events: Flow<CollectEvent> = _events.receiveAsFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            queue.state.collect { queueState ->
                _uiState.update { state ->
                    val justFinished = state.queue.running && !queueState.running && queueState.finished
                    state.copy(queue = queueState, showSummary = state.showSummary || justFinished)
                }
            }
        }
    }

    fun onSourceChange(source: DataSource) {
        if (source == _uiState.value.source) return
        settingsRepository.setSelectedSource(source)
        _uiState.update { it.withInput(source, it.input) }
        resetSearch()
    }

    fun onInputChange(text: String) {
        _uiState.update { it.withInput(it.source, text) }
    }

    /** Appends clipboard text below the current input. */
    fun onPaste(text: String?) {
        if (text.isNullOrBlank()) {
            _events.trySend(CollectEvent.ClipboardEmpty)
            return
        }
        onInputChange(appendLines(_uiState.value.input, listOf(text.trim())))
    }

    fun onClear() = onInputChange("")

    fun onStartProcessing() {
        val state = _uiState.value
        if (state.validCount == 0 || state.queue.running) return
        if (!queue.isSupported(state.source)) {
            _events.trySend(CollectEvent.SourceUnsupported(state.source))
            return
        }
        _uiState.update { it.copy(showSummary = false) }
        queue.start(state.source, state.validProductIds)
    }

    fun onCancelProcessing() = queue.cancel()

    fun onProcessingVerified(page: VerifiedPage) = queue.submitVerification(page)

    fun onProcessingVerificationCancelled() = queue.cancelVerification()

    fun onDismissSummary() = _uiState.update { it.copy(showSummary = false) }

    fun onQueryChange(query: String) {
        _searchState.update { it.copy(query = query) }
        launchSearch(query, debounce = true)
    }

    fun onSearchSubmit() = launchSearch(_searchState.value.query, debounce = false)

    fun onToggleHit(productId: String) {
        _searchState.update { state ->
            val selected = if (productId in state.selectedIds) {
                state.selectedIds - productId
            } else {
                state.selectedIds + productId
            }
            state.copy(selectedIds = selected)
        }
    }

    /** Adds the selected search hits to the input, skipping IDs that are already present. */
    fun onAddSelected() {
        val selected = _searchState.value.selectedIds
        if (selected.isEmpty()) return
        val existing = parseInputLines(_uiState.value.input).toSet()
        onInputChange(appendLines(_uiState.value.input, selected.filterNot { it in existing }))
        _searchState.update { it.copy(selectedIds = emptySet()) }
    }

    private fun resetSearch() {
        searchJob?.cancel()
        _searchState.value = SearchUiState()
    }

    /** A new query cancels the previous request so stale results are never shown. */
    private fun launchSearch(query: String, debounce: Boolean) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchState.update { it.copy(status = SearchStatus.IDLE, hits = emptyList(), error = null) }
            return
        }
        val source = _uiState.value.source
        searchJob = viewModelScope.launch {
            if (debounce) delay(SEARCH_DEBOUNCE_MS)
            runSearch { searchRepository.search(source, query) }
        }
    }

    /** Shows the results the user reached in the in-app browser after completing verification. */
    fun onVerifiedPage(page: VerifiedPage) {
        searchJob?.cancel()
        val source = _uiState.value.source
        searchJob = viewModelScope.launch {
            runSearch { searchRepository.searchFromVerifiedPage(source, page) }
        }
    }

    private suspend fun runSearch(load: suspend () -> List<SearchHit>) {
        _searchState.update { it.copy(status = SearchStatus.LOADING, error = null, challengeUrl = null) }
        var error: FetchErrorKind? = null
        var challengeUrl: String? = null
        val status = try {
            val hits = load()
            _searchState.update { it.copy(hits = hits) }
            SearchStatus.LOADED
        } catch (e: CancellationException) {
            throw e
        } catch (e: FetchException) {
            error = e.kind
            challengeUrl = e.challengeUrl
            if (e.kind == FetchErrorKind.UNSUPPORTED) SearchStatus.UNSUPPORTED else SearchStatus.FAILED
        } catch (_: Exception) {
            SearchStatus.FAILED
        }
        _searchState.update {
            it.copy(
                status = status,
                hits = if (status == SearchStatus.LOADED) it.hits else emptyList(),
                error = error,
                challengeUrl = challengeUrl,
            )
        }
    }

    private fun CollectUiState.withInput(source: DataSource, text: String) = copy(
        source = source,
        input = text,
        items = parseInputLines(text).map { QueueItem(it, source.validate(it)) },
        showSummary = false,
    )

    private fun appendLines(current: String, lines: List<String>): String {
        if (lines.isEmpty()) return current
        val base = current.trimEnd()
        val addition = lines.joinToString("\n")
        return if (base.isEmpty()) addition else "$base\n$addition"
    }

    private companion object {
        // Each search downloads a full page, so wait a little longer before searching while typing.
        const val SEARCH_DEBOUNCE_MS = 600L
    }
}
