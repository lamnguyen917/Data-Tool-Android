package com.elp.datatool.data.repository

import com.elp.datatool.domain.model.DataSource
import com.elp.datatool.domain.model.FetchErrorKind
import com.elp.datatool.domain.model.FetchException
import com.elp.datatool.domain.model.ProductResult
import com.elp.datatool.domain.model.ResultStatus
import com.elp.datatool.domain.model.SearchHit
import com.elp.datatool.domain.repository.SourceAdapter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultCollectionQueueTest {

    private val results = InMemoryResultRepository()
    private val settings = InMemorySettingsRepository()
    private val cache = ProductCache(clock = { 0L })

    /** Adapter driven by a per-id script; counts calls per id. */
    private class FakeAdapter(
        private val behavior: suspend (id: String, attempt: Int) -> ProductResult,
    ) : SourceAdapter {
        val calls = mutableMapOf<String, Int>()
        override val source = DataSource.UNITY_ASSET_STORE
        override suspend fun search(query: String): List<SearchHit> = emptyList()
        override suspend fun fetch(productId: String): ProductResult {
            val attempt = (calls[productId] ?: 0) + 1
            calls[productId] = attempt
            return behavior(productId, attempt)
        }
    }

    private fun completed(id: String) =
        ProductResult(DataSource.UNITY_ASSET_STORE, id, ResultStatus.COMPLETED, title = "Product $id")

    private fun TestScope.queueWith(adapter: SourceAdapter) = DefaultCollectionQueue(
        adapters = listOf(adapter),
        results = results,
        settings = settings,
        cache = cache,
        scope = this,
        clock = { testScheduler.currentTime },
        retryBackoffMillis = 10,
    )

    @Test
    fun batch_storesResultsAndCounts() = runTest {
        val adapter = FakeAdapter { id, _ ->
            if (id == "404") throw FetchException(FetchErrorKind.NOT_FOUND) else completed(id)
        }
        val queue = queueWith(adapter)

        queue.start(DataSource.UNITY_ASSET_STORE, listOf("1", "2", "404", "2"))
        advanceUntilIdle()

        val state = queue.state.value
        assertFalse(state.running)
        assertTrue(state.finished)
        assertEquals(3, state.total)
        assertEquals(2, state.completed)
        assertEquals(1, state.failed)
        assertEquals(FetchErrorKind.NOT_FOUND, results.get("unity:404")?.error)
        assertEquals(1, adapter.calls["404"])
    }

    @Test
    fun temporaryErrors_areRetriedAutomatically() = runTest {
        val adapter = FakeAdapter { id, attempt ->
            if (attempt < 3) throw FetchException(FetchErrorKind.NETWORK) else completed(id)
        }
        val queue = queueWith(adapter)

        queue.start(DataSource.UNITY_ASSET_STORE, listOf("7"))
        advanceUntilIdle()

        assertEquals(3, adapter.calls["7"])
        assertEquals(ResultStatus.COMPLETED, results.get("unity:7")?.status)
    }

    @Test
    fun autoRetryDisabled_failsOnFirstError() = runTest {
        settings.setAutoRetry(false)
        val adapter = FakeAdapter { _, _ -> throw FetchException(FetchErrorKind.NETWORK) }
        val queue = queueWith(adapter)

        queue.start(DataSource.UNITY_ASSET_STORE, listOf("7"))
        advanceUntilIdle()

        assertEquals(1, adapter.calls["7"])
        assertEquals(FetchErrorKind.NETWORK, results.get("unity:7")?.error)
    }

    @Test
    fun slowRequest_timesOut() = runTest {
        settings.setAutoRetry(false)
        settings.setTimeoutSeconds(15)
        val queue = queueWith(FakeAdapter { _, _ -> awaitCancellation() })

        queue.start(DataSource.UNITY_ASSET_STORE, listOf("9"))
        advanceUntilIdle()

        assertEquals(FetchErrorKind.TIMEOUT, results.get("unity:9")?.error)
    }

    @Test
    fun cancel_keepsCollectedResults() = runTest {
        settings.setParallelRequests(1)
        val queue = queueWith(FakeAdapter { id, _ ->
            if (id == "2") awaitCancellation() else completed(id)
        })

        queue.start(DataSource.UNITY_ASSET_STORE, listOf("1", "2", "3"))
        runCurrent()
        queue.cancel()
        advanceUntilIdle()

        val state = queue.state.value
        assertTrue(state.cancelled)
        assertFalse(state.running)
        assertEquals(listOf("1"), results.results.value.map { it.productId })
    }

    @Test
    fun batch_usesCacheButRetryBypassesIt() = runTest {
        val adapter = FakeAdapter { id, _ -> completed(id) }
        val queue = queueWith(adapter)

        queue.start(DataSource.UNITY_ASSET_STORE, listOf("5"))
        advanceUntilIdle()
        queue.start(DataSource.UNITY_ASSET_STORE, listOf("5"))
        advanceUntilIdle()
        assertEquals(1, adapter.calls["5"])

        queue.retry(DataSource.UNITY_ASSET_STORE, "5")
        assertEquals(ResultStatus.PROCESSING, results.get("unity:5")?.status)
        advanceUntilIdle()

        assertEquals(2, adapter.calls["5"])
        assertEquals(ResultStatus.COMPLETED, results.get("unity:5")?.status)
    }

    @Test
    fun parallelism_followsSettings() = runTest {
        settings.setParallelRequests(2)
        var active = 0
        var maxActive = 0
        val queue = queueWith(FakeAdapter { id, _ ->
            active++
            maxActive = maxOf(maxActive, active)
            delay(100)
            active--
            completed(id)
        })

        queue.start(DataSource.UNITY_ASSET_STORE, (1..6).map { it.toString() })
        advanceTimeBy(1_000)
        advanceUntilIdle()

        assertEquals(2, maxActive)
        assertEquals(6, queue.state.value.completed)
    }
}
