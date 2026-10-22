package com.elp.datatool.presentation.results

import com.elp.datatool.data.repository.InMemoryResultRepository
import com.elp.datatool.data.sample.SampleData
import com.elp.datatool.presentation.FakeCollectionQueue
import com.elp.datatool.presentation.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ResultsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = InMemoryResultRepository(SampleData.results)
    private val queue = FakeCollectionQueue()

    @Test
    fun retry_requeuesTheFailedProduct() {
        val viewModel = ResultsViewModel(repository, queue)
        val failed = SampleData.results.last()

        viewModel.onRetry(failed.key)

        assertEquals(listOf(failed.source to failed.productId), queue.retried)
    }

    @Test
    fun errorFilter_showsOnlyFailedResults() = runTest {
        val viewModel = ResultsViewModel(repository, queue)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.onFilterChange(ResultFilter.ERRORS)

        val state = viewModel.uiState.value
        assertEquals(listOf("99101"), state.visibleResults.map { it.productId })
        assertEquals(4, state.totalCount)
        assertEquals(3, state.completedCount)
        assertEquals(1, state.errorCount)
    }

    @Test
    fun query_matchesTitleIdAndCreator() = runTest {
        val viewModel = ResultsViewModel(repository, queue)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.onQueryChange("procedural")

        assertEquals(listOf("155852"), viewModel.uiState.value.visibleResults.map { it.productId })
    }

    @Test
    fun longPressThenDelete_removesSelectedAndExitsSelection() = runTest {
        val viewModel = ResultsViewModel(repository, queue)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        val key = SampleData.results.first().key

        viewModel.onLongPress(key)
        assertTrue(viewModel.uiState.value.selectionMode)
        assertEquals(setOf(key), viewModel.uiState.value.selectedKeys)

        viewModel.onDeleteSelected()

        val state = viewModel.uiState.value
        assertFalse(state.selectionMode)
        assertEquals(3, state.totalCount)
        assertFalse(state.visibleResults.any { it.key == key })
    }

    @Test
    fun toggleSort_reversesOrder() = runTest {
        val viewModel = ResultsViewModel(repository, queue)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        val newestFirst = viewModel.uiState.value.visibleResults.map { it.productId }

        viewModel.onToggleSort()

        assertEquals(newestFirst.reversed(), viewModel.uiState.value.visibleResults.map { it.productId })
    }
}
