package com.elp.datatool.presentation.collect

import com.elp.datatool.domain.model.DataSource
import com.elp.datatool.domain.model.FetchErrorKind
import com.elp.datatool.domain.model.FetchException
import com.elp.datatool.domain.model.QueueState
import com.elp.datatool.domain.model.SearchHit
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

    @Test
    fun start_sendsNormalizedValidIdsToQueue() {
        val viewModel = CollectViewModel(searchRepository, queue)
        viewModel.onInputChange(
            "96478\ninvalid-item\nhttps://assetstore.unity.com/packages/tools/terrain/microsplat-36050"
        )

        viewModel.onStartProcessing()

        assertEquals(listOf(DataSource.UNITY_ASSET_STORE to listOf("96478", "36050")), queue.started)
        assertTrue(viewModel.uiState.value.queue.running)
    }

    @Test
    fun start_withUnsupportedSourceSendsEvent() = runTest {
        val viewModel = CollectViewModel(searchRepository, queue)
        viewModel.onSourceChange(DataSource.STEAM_STORE)
        viewModel.onInputChange("413150")

        viewModel.onStartProcessing()

        assertTrue(queue.started.isEmpty())
        assertEquals(CollectEvent.SourceUnsupported(DataSource.STEAM_STORE), viewModel.events.first())
    }

    @Test
    fun finishedBatch_showsSummaryUntilInputChanges() {
        val viewModel = CollectViewModel(searchRepository, queue)
        viewModel.onInputChange("96478")
        viewModel.onStartProcessing()

        queue.state.value = QueueState(total = 1, completed = 1, finished = true)
        assertTrue(viewModel.uiState.value.showSummary)

        viewModel.onInputChange("96478\n65563")
        assertFalse(viewModel.uiState.value.showSummary)
    }

    @Test
    fun cancel_delegatesToQueue() {
        val viewModel = CollectViewModel(searchRepository, queue)

        viewModel.onCancelProcessing()

        assertEquals(1, queue.cancelCount)
    }

    @Test
    fun search_unsupportedSourceShowsUnsupportedState() = runTest {
        val viewModel = CollectViewModel(searchRepository, queue)
        viewModel.onSourceChange(DataSource.AMAZON_BOOKS)

        viewModel.onQueryChange("clean code")
        viewModel.onSearchSubmit()
        advanceUntilIdle()

        assertEquals(SearchStatus.UNSUPPORTED, viewModel.searchState.value.status)
    }

    @Test
    fun inputChange_countsValidAndInvalidItems() {
        val viewModel = CollectViewModel(searchRepository, queue)

        viewModel.onInputChange("96478\n36050\ninvalid-item")

        val state = viewModel.uiState.value
        assertEquals(3, state.items.size)
        assertEquals(2, state.validCount)
        assertEquals(1, state.invalidCount)
    }

    @Test
    fun sourceChange_revalidatesExistingInput() {
        val viewModel = CollectViewModel(searchRepository, queue)
        viewModel.onInputChange("hollow-knight")
        assertEquals(0, viewModel.uiState.value.validCount)

        viewModel.onSourceChange(DataSource.IGDB)

        assertEquals(1, viewModel.uiState.value.validCount)
    }

    @Test
    fun paste_appendsBelowCurrentInput() {
        val viewModel = CollectViewModel(searchRepository, queue)
        viewModel.onInputChange("96478\n")

        viewModel.onPaste("36050")

        assertEquals("96478\n36050", viewModel.uiState.value.input)
    }

    @Test
    fun addSelected_appendsOnlyNewIds() = runTest {
        val viewModel = CollectViewModel(searchRepository, queue)
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
        val viewModel = CollectViewModel(searchRepository, queue)

        viewModel.onQueryChange("terrain")
        viewModel.onSearchSubmit()
        advanceUntilIdle()

        assertEquals(SearchStatus.LOADED, viewModel.searchState.value.status)
        assertEquals(2, viewModel.searchState.value.hits.size)
    }
}
