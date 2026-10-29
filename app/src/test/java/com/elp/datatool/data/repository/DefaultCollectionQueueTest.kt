package com.elp.datatool.data.repository

import com.elp.datatool.domain.model.DataSource
import com.elp.datatool.domain.model.FetchErrorKind
import com.elp.datatool.domain.model.FetchException
import com.elp.datatool.domain.model.ProductResult
import com.elp.datatool.domain.model.ResultStatus
import com.elp.datatool.domain.model.SearchHit
import com.elp.datatool.domain.model.VerifiedPage
import com.elp.datatool.domain.repository.SourceAdapter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
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
        override val source: DataSource = DataSource.UNITY_ASSET_STORE,
        private val behavior: suspend (id: String, attempt: Int) -> ProductResult,
    ) : SourceAdapter {
        val calls = mutableMapOf<String, Int>()
        override suspend fun search(query: String): List<SearchHit> = emptyList()
        override suspend fun fetch(productId: String): ProductResult {
            val attempt = (calls[productId] ?: 0) + 1
            calls[productId] = attempt
            return behavior(productId, attempt)
        }
    }

    private fun completed(id: String) =
        ProductResult(DataSource.UNITY_ASSET_STORE, id, ResultStatus.COMPLETED, title = "Product $id")

    private fun TestScope.queueWith(
        adapter: SourceAdapter,
        importBrowserCookies: (String, String) -> Unit = { _, _ -> },
    ) = queueWith(listOf(adapter), importBrowserCookies)

    private fun TestScope.queueWith(
        adapters: List<SourceAdapter>,
        importBrowserCookies: (String, String) -> Unit = { _, _ -> },
    ) = DefaultCollectionQueue(
        adapters = adapters,
        results = results,
        settings = settings,
        cache = cache,
        scope = this,
        clock = { testScheduler.currentTime },
        retryBackoffMillis = 10,
        importBrowserCookies = importBrowserCookies,
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
    fun batch_cacheAliasesOriginalRequestWhenResultIdDiffers() = runTest {
        val requestUrl = "https://example.test/product/30718"
        val adapter = FakeAdapter { _, _ -> completed("RHJ-100") }
        val queue = queueWith(adapter)

        queue.start(DataSource.UNITY_ASSET_STORE, listOf(requestUrl))
        advanceUntilIdle()
        queue.start(DataSource.UNITY_ASSET_STORE, listOf(requestUrl))
        advanceUntilIdle()

        assertEquals(1, adapter.calls[requestUrl])
        assertEquals(ResultStatus.COMPLETED, results.get("unity:RHJ-100")?.status)
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

    @Test
    fun concurrentCaptchaRequests_shareOneVerificationAndResumeTogether() = runTest {
        settings.setParallelRequests(3)
        var cookiesImported = false
        var importCount = 0
        val adapter = FakeAdapter { id, _ ->
            if (!cookiesImported) {
                throw FetchException(
                    FetchErrorKind.BLOCKED,
                    challengeUrl = "https://example.test/products/$id",
                    challengeUserAgent = "Test Browser",
                )
            }
            completed(id)
        }
        val queue = queueWith(adapter) { _, cookieHeader ->
            importCount++
            cookiesImported = cookieHeader == "verified=true"
        }

        queue.start(DataSource.UNITY_ASSET_STORE, listOf("1", "2", "3"))
        runCurrent()

        val verification = queue.verification.value
        assertTrue(queue.state.value.running)
        assertTrue(results.results.value.isEmpty())
        assertTrue(verification?.productId in setOf("1", "2", "3"))
        assertEquals("Test Browser", verification?.userAgent)
        assertEquals(1, adapter.calls.values.toSet().single())

        queue.submitVerification(
            VerifiedPage(
                url = checkNotNull(verification).challengeUrl,
                html = "<html>Verified</html>",
                cookieHeader = "verified=true",
            )
        )
        advanceUntilIdle()

        assertEquals(1, importCount)
        assertEquals(null, queue.verification.value)
        assertEquals(3, queue.state.value.completed)
        assertEquals(0, queue.state.value.failed)
        assertEquals(setOf(2), adapter.calls.values.toSet())
    }

    @Test
    fun cancellingCaptchaReleasesAllWaitingItemsAsBlocked() = runTest {
        settings.setParallelRequests(2)
        val adapter = FakeAdapter { id, _ ->
            throw FetchException(
                FetchErrorKind.BLOCKED,
                challengeUrl = "https://example.test/products/$id",
            )
        }
        val queue = queueWith(adapter)

        queue.start(DataSource.UNITY_ASSET_STORE, listOf("1", "2"))
        runCurrent()
        assertTrue(queue.verification.value != null)

        queue.cancelVerification()
        advanceUntilIdle()

        assertEquals(null, queue.verification.value)
        assertEquals(0, queue.state.value.completed)
        assertEquals(2, queue.state.value.failed)
        assertEquals(FetchErrorKind.BLOCKED, results.get("unity:1")?.error)
        assertEquals(FetchErrorKind.BLOCKED, results.get("unity:2")?.error)
    }

    @Test
    fun staleBlockedResponseAfterVerificationRetriesWithoutOpeningAnotherBrowser() = runTest {
        settings.setParallelRequests(2)
        var cookiesImported = false
        val releaseStaleResponse = CompletableDeferred<Unit>()
        val adapter = FakeAdapter { id, attempt ->
            if (id == "2" && attempt == 1) {
                releaseStaleResponse.await()
                throw FetchException(
                    FetchErrorKind.BLOCKED,
                    challengeUrl = "https://example.test/products/2",
                )
            }
            if (!cookiesImported) {
                throw FetchException(
                    FetchErrorKind.BLOCKED,
                    challengeUrl = "https://example.test/products/$id",
                )
            }
            completed(id)
        }
        val queue = queueWith(adapter) { _, _ -> cookiesImported = true }

        queue.start(DataSource.UNITY_ASSET_STORE, listOf("1", "2"))
        runCurrent()
        val verification = checkNotNull(queue.verification.value)

        queue.submitVerification(VerifiedPage(verification.challengeUrl, "<html>Verified</html>"))
        releaseStaleResponse.complete(Unit)
        advanceUntilIdle()

        assertEquals(null, queue.verification.value)
        assertEquals(2, queue.state.value.completed)
        assertEquals(setOf(2), adapter.calls.values.toSet())
    }

    @Test
    fun blockedRequestsFromDifferentSourcesUseWebViewsSequentially() = runTest {
        val verifiedSources = mutableSetOf<DataSource>()
        fun adapter(source: DataSource, host: String) = FakeAdapter(source) { id, _ ->
            if (source !in verifiedSources) {
                throw FetchException(
                    FetchErrorKind.BLOCKED,
                    challengeUrl = "https://$host/products/$id",
                )
            }
            ProductResult(source, id, ResultStatus.COMPLETED, title = "Product $id")
        }
        val unity = adapter(DataSource.UNITY_ASSET_STORE, "unity.test")
        val amazon = adapter(DataSource.AMAZON_BOOKS, "amazon.test")
        val queue = queueWith(listOf(unity, amazon)) { url, _ ->
            when {
                "unity.test" in url -> verifiedSources += DataSource.UNITY_ASSET_STORE
                "amazon.test" in url -> verifiedSources += DataSource.AMAZON_BOOKS
            }
        }

        queue.retry(DataSource.UNITY_ASSET_STORE, "1")
        queue.retry(DataSource.AMAZON_BOOKS, "2")
        runCurrent()

        val first = checkNotNull(queue.verification.value)
        queue.submitVerification(VerifiedPage(first.challengeUrl, "<html>Verified</html>"))
        runCurrent()

        val second = checkNotNull(queue.verification.value)
        assertTrue(second.source != first.source)
        queue.submitVerification(VerifiedPage(second.challengeUrl, "<html>Verified</html>"))
        advanceUntilIdle()

        assertEquals(ResultStatus.COMPLETED, results.get("unity:1")?.status)
        assertEquals(ResultStatus.COMPLETED, results.get("amazon:2")?.status)
        assertEquals(listOf(2, 3), listOf(unity.calls["1"], amazon.calls["2"]).sortedBy { it })
    }
}
