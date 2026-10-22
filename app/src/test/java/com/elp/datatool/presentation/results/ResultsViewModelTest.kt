package com.elp.datatool.presentation.results

import com.elp.datatool.data.repository.InMemoryResultRepository
import com.elp.datatool.data.repository.InMemorySettingsRepository
import com.elp.datatool.data.sample.SampleData
import com.elp.datatool.domain.copy.CopyField
import com.elp.datatool.domain.copy.LineFormat
import com.elp.datatool.presentation.FakeCollectionQueue
import com.elp.datatool.presentation.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
    private val settings = InMemorySettingsRepository()

    @Test
    fun copyLine_usesFormatAndFieldsFromSettings() = runTest {
        settings.setCopyLineFormat(LineFormat.CSV)
        settings.setCopyLineFields(listOf(CopyField.SKU, CopyField.TITLE))
        val viewModel = ResultsViewModel(repository, queue, settings)

        viewModel.onCopyLine(SampleData.results.first().key)

        assertEquals(ResultsEvent.Copy("96478,MicroSplat", CopyKind.LINE), viewModel.events.first())
    }

    @Test
    fun copyDetails_usesTemplateFromSettings() = runTest {
        settings.setCopyDetailsTemplate("{title} by {author}")
        val viewModel = ResultsViewModel(repository, queue, settings)

        viewModel.onCopyDetails(SampleData.results.first().key)

        assertEquals(ResultsEvent.Copy("MicroSplat by Jason Booth", CopyKind.DETAILS), viewModel.events.first())
    }

    @Test
    fun copySelected_copiesOneLinePerCompletedResultInDisplayOrder() = runTest {
        settings.setCopyLineFields(listOf(CopyField.SKU))
        val viewModel = ResultsViewModel(repository, queue, settings)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        SampleData.results.forEach { viewModel.onLongPress(it.key) }

        viewModel.onCopySelectedLines()

        // The failed result has no data and is skipped.
        assertEquals(ResultsEvent.Copy("96478\n65563\n155852", CopyKind.LINE, 3), viewModel.events.first())
    }

    @Test
    fun retry_requeuesTheFailedProduct() {
        val viewModel = ResultsViewModel(repository, queue, settings)
        val failed = SampleData.results.last()

        viewModel.onRetry(failed.key)

        assertEquals(listOf(failed.source to failed.productId), queue.retried)
    }

    @Test
    fun errorFilter_showsOnlyFailedResults() = runTest {
        val viewModel = ResultsViewModel(repository, queue, settings)
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
        val viewModel = ResultsViewModel(repository, queue, settings)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.onQueryChange("procedural")

        assertEquals(listOf("155852"), viewModel.uiState.value.visibleResults.map { it.productId })
    }

    @Test
    fun longPressThenDelete_removesSelectedAndExitsSelection() = runTest {
        val viewModel = ResultsViewModel(repository, queue, settings)
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
        val viewModel = ResultsViewModel(repository, queue, settings)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        val newestFirst = viewModel.uiState.value.visibleResults.map { it.productId }

        viewModel.onToggleSort()

        assertEquals(newestFirst.reversed(), viewModel.uiState.value.visibleResults.map { it.productId })
    }
}
