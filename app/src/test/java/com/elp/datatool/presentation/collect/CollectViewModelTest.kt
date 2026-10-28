package com.elp.datatool.presentation.collect

import com.elp.datatool.data.repository.InMemorySettingsRepository
import com.elp.datatool.domain.model.AppSettings
import com.elp.datatool.domain.model.DataSource
import com.elp.datatool.domain.model.FetchErrorKind
import com.elp.datatool.domain.model.FetchException
import com.elp.datatool.domain.model.QueueState
import com.elp.datatool.domain.model.SearchHit
import com.elp.datatool.domain.model.VerifiedPage
import com.elp.datatool.domain.repository.ProductSearchRepository
import com.elp.datatool.presentation.FakeCollectionQueue
import com.elp.datatool.presentation.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CollectViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val searchRepository = object : ProductSearchRepository {
        override suspend fun search(source: DataSource, query: String): List<SearchHit> {
            if (source != DataSource.UNITY_ASSET_STORE) throw FetchException(FetchErrorKind.UNSUPPORTED)
            return listOf(SearchHit(source, "96478", "MicroSplat"), SearchHit(source, "65563", "Terrain Composer 2"))
        }
    }

    private val queue = FakeCollectionQueue()
    private val settings = InMemorySettingsRepository()

    private fun createViewModel() = CollectViewModel(searchRepository, queue, settings)

    @Test
    fun start_sendsNormalizedValidIdsToQueue() {
        val viewModel = createViewModel()
        viewModel.onInputChange(
            "96478\ninvalid-item\nhttps://assetstore.unity.com/packages/tools/terrain/microsplat-36050"
        )

        viewModel.onStartProcessing()

        assertEquals(listOf(DataSource.UNITY_ASSET_STORE to listOf("96478", "36050")), queue.started)
        assertTrue(viewModel.uiState.value.queue.running)
    }

    @Test
    fun start_withUnsupportedSourceSendsEvent() = runTest {
        val viewModel = createViewModel()
        viewModel.onSourceChange(DataSource.STEAM_STORE)
        viewModel.onInputChange("413150")

        viewModel.onStartProcessing()

        assertTrue(queue.started.isEmpty())
        assertEquals(CollectEvent.SourceUnsupported(DataSource.STEAM_STORE), viewModel.events.first())
    }

    @Test
    fun finishedBatch_showsSummaryUntilInputChanges() {
        val viewModel = createViewModel()
        viewModel.onInputChange("96478")
        viewModel.onStartProcessing()

        queue.state.value = QueueState(total = 1, completed = 1, finished = true)
        assertTrue(viewModel.uiState.value.showSummary)

        viewModel.onInputChange("96478\n65563")
        assertFalse(viewModel.uiState.value.showSummary)
    }

    @Test
    fun cancel_delegatesToQueue() {
        val viewModel = createViewModel()

        viewModel.onCancelProcessing()

        assertEquals(1, queue.cancelCount)
    }

    @Test
    fun search_unsupportedSourceShowsUnsupportedState() = runTest {
        val viewModel = createViewModel()
        viewModel.onSourceChange(DataSource.AMAZON_BOOKS)

        viewModel.onQueryChange("clean code")
        viewModel.onSearchSubmit()
        advanceUntilIdle()

        assertEquals(SearchStatus.UNSUPPORTED, viewModel.searchState.value.status)
    }

    @Test
    fun search_blockedSourcePreservesErrorKind() = runTest {
        val repository = object : ProductSearchRepository {
            override suspend fun search(source: DataSource, query: String): List<SearchHit> =
                throw FetchException(FetchErrorKind.BLOCKED)
        }
        val viewModel = CollectViewModel(repository, queue, settings)
        viewModel.onSourceChange(DataSource.AVE)

        viewModel.onQueryChange("RHJ-101")
        viewModel.onSearchSubmit()
        advanceUntilIdle()

        assertEquals(SearchStatus.FAILED, viewModel.searchState.value.status)
        assertEquals(FetchErrorKind.BLOCKED, viewModel.searchState.value.error)
    }

    @Test
    fun search_challengeCanBeCompletedWithVerifiedPage() = runTest {
        val challengeUrl = "https://www.aventertainments.com/dvd/search?keyword=RHJ-101"
        val repository = object : ProductSearchRepository {
            override suspend fun search(source: DataSource, query: String): List<SearchHit> =
                throw FetchException(FetchErrorKind.BLOCKED, challengeUrl = challengeUrl)

            override suspend fun searchFromVerifiedPage(source: DataSource, page: VerifiedPage) =
                listOf(SearchHit(source, "RHJ-101", "Red Hot Jam Vol.101"))
        }
        val viewModel = CollectViewModel(repository, queue, settings)
        viewModel.onSourceChange(DataSource.AVE)

        viewModel.onQueryChange("RHJ-101")
        viewModel.onSearchSubmit()
        advanceUntilIdle()
        assertEquals(challengeUrl, viewModel.searchState.value.challengeUrl)

        viewModel.onVerifiedPage(VerifiedPage(url = challengeUrl, html = "<html></html>"))
        advanceUntilIdle()

        val state = viewModel.searchState.value
        assertEquals(SearchStatus.LOADED, state.status)
        assertEquals(listOf("RHJ-101"), state.hits.map { it.productId })
        assertEquals(null, state.challengeUrl)
    }

    @Test
    fun inputChange_countsValidAndInvalidItems() {
        val viewModel = createViewModel()

        viewModel.onInputChange("96478\n36050\ninvalid-item")

        val state = viewModel.uiState.value
        assertEquals(3, state.items.size)
        assertEquals(2, state.validCount)
        assertEquals(1, state.invalidCount)
    }

    @Test
    fun sourceChange_revalidatesExistingInput() {
        val viewModel = createViewModel()
        viewModel.onInputChange("hollow-knight")
        assertEquals(0, viewModel.uiState.value.validCount)

        viewModel.onSourceChange(DataSource.IGDB)

        assertEquals(1, viewModel.uiState.value.validCount)
        assertEquals(DataSource.IGDB, settings.settings.value.selectedSource)
    }

    @Test
    fun initialSource_usesSavedSelection() {
        val savedSettings = InMemorySettingsRepository(
            AppSettings(selectedSource = DataSource.AMAZON_BOOKS)
        )

        val viewModel = CollectViewModel(searchRepository, queue, savedSettings)

        assertEquals(DataSource.AMAZON_BOOKS, viewModel.uiState.value.source)
    }

    @Test
    fun paste_appendsBelowCurrentInput() {
        val viewModel = createViewModel()
        viewModel.onInputChange("96478\n")

        viewModel.onPaste("36050")

        assertEquals("96478\n36050", viewModel.uiState.value.input)
    }

    @Test
    fun addSelected_appendsOnlyNewIds() = runTest {
        val viewModel = createViewModel()
        viewModel.onInputChange("96478")
        viewModel.onQueryChange("terrain")
        viewModel.onSearchSubmit()
        advanceUntilIdle()

        viewModel.onToggleHit("96478")
        viewModel.onToggleHit("65563")
        viewModel.onAddSelected()

        assertEquals("96478\n65563", viewModel.uiState.value.input)
        assertEquals(emptySet<String>(), viewModel.searchState.value.selectedIds)
    }

    @Test
    fun search_loadsHits() = runTest {
        val viewModel = createViewModel()

        viewModel.onQueryChange("terrain")
        viewModel.onSearchSubmit()
        advanceUntilIdle()

        assertEquals(SearchStatus.LOADED, viewModel.searchState.value.status)
        assertEquals(2, viewModel.searchState.value.hits.size)
    }
}
