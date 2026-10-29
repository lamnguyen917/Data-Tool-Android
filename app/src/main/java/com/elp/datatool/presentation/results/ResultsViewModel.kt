package com.elp.datatool.presentation.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elp.datatool.domain.model.ProductResult
import com.elp.datatool.domain.model.ResultStatus
import com.elp.datatool.domain.copy.CopyFormatter
import com.elp.datatool.domain.repository.CollectionQueue
import com.elp.datatool.domain.repository.ResultRepository
import com.elp.datatool.domain.repository.SettingsRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

enum class ResultFilter { ALL, COMPLETED, ERRORS }

data class ResultsUiState(
    val query: String = "",
    val filter: ResultFilter = ResultFilter.ALL,
    val newestFirst: Boolean = true,
    val selectionMode: Boolean = false,
    val selectedKeys: Set<String> = emptySet(),
    val visibleResults: List<ProductResult> = emptyList(),
    val totalCount: Int = 0,
    val completedCount: Int = 0,
    val errorCount: Int = 0,
)

sealed interface ResultsEvent {
    data class Deleted(val count: Int) : ResultsEvent
    data class Share(val results: List<ProductResult>) : ResultsEvent
    data object ExportUnavailable : ResultsEvent
    data class Copy(val text: String, val kind: CopyKind, val count: Int = 1) : ResultsEvent
}

enum class CopyKind { LINE, DETAILS }

private data class Controls(
    val query: String = "",
    val filter: ResultFilter = ResultFilter.ALL,
    val newestFirst: Boolean = true,
    val selectionMode: Boolean = false,
    val selectedKeys: Set<String> = emptySet(),
)

class ResultsViewModel(
    private val repository: ResultRepository,
    private val queue: CollectionQueue,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val controls = MutableStateFlow(Controls())

    val uiState: StateFlow<ResultsUiState> = combine(repository.results, controls, ::buildState)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = buildState(repository.results.value, controls.value),
        )

    private val _events = Channel<ResultsEvent>(Channel.BUFFERED)
    val events: Flow<ResultsEvent> = _events.receiveAsFlow()

    fun onQueryChange(query: String) = controls.update { it.copy(query = query) }

    fun onFilterChange(filter: ResultFilter) = controls.update { it.copy(filter = filter) }

    fun onToggleSort() = controls.update { it.copy(newestFirst = !it.newestFirst) }

    fun onToggleSelectionMode() = controls.update {
        it.copy(selectionMode = !it.selectionMode, selectedKeys = emptySet())
    }

    fun onExitSelection() = controls.update { it.copy(selectionMode = false, selectedKeys = emptySet()) }

    fun onToggleSelection(key: String) = controls.update {
        val keys = if (key in it.selectedKeys) it.selectedKeys - key else it.selectedKeys + key
        it.copy(selectedKeys = keys)
    }

    fun onSelectAll() = controls.update {
        it.copy(selectedKeys = it.selectedKeys + uiState.value.visibleResults.map(ProductResult::key))
    }

    /** Long press starts selection mode with the pressed item selected. */
    fun onLongPress(key: String) = controls.update {
        it.copy(selectionMode = true, selectedKeys = it.selectedKeys + key)
    }

    fun onDeleteSelected() {
        val keys = controls.value.selectedKeys
        if (keys.isEmpty()) return
        repository.delete(keys)
        onExitSelection()
        _events.trySend(ResultsEvent.Deleted(keys.size))
    }

    fun onShareSelected() {
        val keys = controls.value.selectedKeys
        val selected = repository.results.value.filter { it.key in keys }
        if (selected.isNotEmpty()) _events.trySend(ResultsEvent.Share(selected))
    }

    fun onExportSelected() {
        // TODO: Write the export through the Storage Access Framework once the exporter exists.
        _events.trySend(ResultsEvent.ExportUnavailable)
    }

    fun onCopyLine(key: String) {
        val result = repository.get(key) ?: return
        val current = settings.settings.value
        _events.trySend(
            ResultsEvent.Copy(
                CopyFormatter.line(
                    result,
                    current.copyLineFieldsFor(result.source),
                    current.copyLineFormatFor(result.source),
                ),
                CopyKind.LINE,
            )
        )
    }

    fun onCopyDetails(key: String) {
        val result = repository.get(key) ?: return
        val current = settings.settings.value
        _events.trySend(
            ResultsEvent.Copy(
                CopyFormatter.details(result, current.copyDetailsTemplateFor(result.source)),
                CopyKind.DETAILS,
            )
        )
    }

    /** Copies one line per selected result that has collected data, in the order shown. */
    fun onCopySelectedLines() {
        val keys = controls.value.selectedKeys
        val current = settings.settings.value
        val selected = uiState.value.visibleResults
            .filter { it.key in keys && it.status == ResultStatus.COMPLETED }
        if (selected.isEmpty()) return
        val text = selected.joinToString("\n") {
            CopyFormatter.line(
                it,
                current.copyLineFieldsFor(it.source),
                current.copyLineFormatFor(it.source),
            )
        }
        _events.trySend(ResultsEvent.Copy(text, CopyKind.LINE, selected.size))
    }

    fun onRetry(key: String) {
        val result = repository.get(key) ?: return
        queue.retry(result.source, result.productId)
    }

    private fun buildState(results: List<ProductResult>, controls: Controls): ResultsUiState {
        val term = controls.query.trim()
        val visible = results
            .asSequence()
            .filter { result ->
                when (controls.filter) {
                    ResultFilter.ALL -> true
                    ResultFilter.COMPLETED -> result.status == ResultStatus.COMPLETED
                    ResultFilter.ERRORS -> result.status == ResultStatus.ERROR
                }
            }
            .filter { result -> term.isEmpty() || result.matches(term) }
            .let { sequence ->
                if (controls.newestFirst) {
                    sequence.sortedByDescending { it.collectedAtMillis }
                } else {
                    sequence.sortedBy { it.collectedAtMillis }
                }
            }
            .toList()
        val existingKeys = results.mapTo(HashSet()) { it.key }
        return ResultsUiState(
            query = controls.query,
            filter = controls.filter,
            newestFirst = controls.newestFirst,
            selectionMode = controls.selectionMode,
            selectedKeys = controls.selectedKeys.filterTo(LinkedHashSet()) { it in existingKeys },
            visibleResults = visible,
            totalCount = results.size,
            completedCount = results.count { it.status == ResultStatus.COMPLETED },
            errorCount = results.count { it.status == ResultStatus.ERROR },
        )
    }

    private fun ProductResult.matches(term: String): Boolean =
        productId.contains(term, ignoreCase = true) ||
            title?.contains(term, ignoreCase = true) == true ||
            creators.any { it.contains(term, ignoreCase = true) }
}
