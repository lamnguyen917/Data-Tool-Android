package com.elp.datatool.data.repository

import com.elp.datatool.domain.model.DataSource
import com.elp.datatool.domain.model.FetchErrorKind
import com.elp.datatool.domain.model.FetchException
import com.elp.datatool.domain.model.ProductResult
import com.elp.datatool.domain.model.ProcessingVerification
import com.elp.datatool.domain.model.QueueState
import com.elp.datatool.domain.model.ResultStatus
import com.elp.datatool.domain.model.VerifiedPage
import com.elp.datatool.domain.repository.CollectionQueue
import com.elp.datatool.domain.repository.ResultRepository
import com.elp.datatool.domain.repository.SettingsRepository
import com.elp.datatool.domain.repository.SourceAdapter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeout

/**
 * Collects products with the source adapters.
 *
 * - Parallelism, timeout and automatic retry follow the current [SettingsRepository] values.
 * - Only temporary errors are retried automatically, with a linear backoff.
 * - Cancelling stops pending requests; results that are already collected are kept.
 *
 * Runs in an application-wide [scope] so it survives navigation, but not process death.
 */
class DefaultCollectionQueue(
    adapters: List<SourceAdapter>,
    private val results: ResultRepository,
    private val settings: SettingsRepository,
    private val cache: ProductCache,
    private val scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis,
    private val retryBackoffMillis: Long = DEFAULT_BACKOFF_MILLIS,
    private val importBrowserCookies: (url: String, cookieHeader: String) -> Unit = { _, _ -> },
) : CollectionQueue {

    private val adapters = adapters.associateBy { it.source }
    private val _state = MutableStateFlow(QueueState())
    override val state: StateFlow<QueueState> = _state.asStateFlow()
    private val _verification = MutableStateFlow<ProcessingVerification?>(null)
    override val verification: StateFlow<ProcessingVerification?> = _verification.asStateFlow()

    private var batchJob: Job? = null
    private val verificationLock = Any()
    private var activeVerification: CompletableDeferred<Boolean>? = null
    private val verificationVersions = mutableMapOf<DataSource, Long>()

    override fun isSupported(source: DataSource) = source in adapters

    override fun start(source: DataSource, productIds: List<String>) {
        val adapter = adapters[source] ?: return
        val ids = productIds.distinct()
        if (ids.isEmpty() || _state.value.running) return

        _state.value = QueueState(running = true, total = ids.size)
        batchJob = scope.launch {
            val semaphore = Semaphore(settings.settings.value.parallelRequests.coerceAtLeast(1))
            try {
                coroutineScope {
                    ids.forEach { id ->
                        launch {
                            semaphore.withPermit {
                                val result = collect(adapter, id, useCache = true)
                                results.upsert(result)
                                _state.update {
                                    if (result.status == ResultStatus.COMPLETED) {
                                        it.copy(completed = it.completed + 1)
                                    } else {
                                        it.copy(failed = it.failed + 1)
                                    }
                                }
                            }
                        }
                    }
                }
                _state.update { it.copy(running = false, finished = true) }
            } catch (e: CancellationException) {
                _state.update { it.copy(running = false, finished = true, cancelled = true) }
                throw e
            }
        }
    }

    override fun cancel() {
        cancelVerification()
        batchJob?.cancel()
    }

    override fun retry(source: DataSource, productId: String) {
        val adapter = adapters[source] ?: return
        val previous = results.get(ProductResult.keyOf(source, productId))
        results.upsert(
            (previous ?: ProductResult(source, productId, ResultStatus.PROCESSING, collectedAtMillis = clock()))
                .copy(status = ResultStatus.PROCESSING, error = null)
        )
        scope.launch {
            results.upsert(collect(adapter, productId, useCache = false))
        }
    }

    override fun submitVerification(page: VerifiedPage) {
        val deferred = synchronized(verificationLock) {
            val current = activeVerification?.takeIf { it.isActive } ?: return
            val request = _verification.value ?: return
            importBrowserCookies(page.url, page.cookieHeader)
            verificationVersions[request.source] = verificationVersion(request.source) + 1L
            activeVerification = null
            _verification.value = null
            current
        }
        deferred.complete(true)
    }

    override fun cancelVerification() {
        val deferred = synchronized(verificationLock) {
            val current = activeVerification?.takeIf { it.isActive } ?: return
            activeVerification = null
            _verification.value = null
            current
        }
        deferred.complete(false)
    }

    private suspend fun collect(adapter: SourceAdapter, productId: String, useCache: Boolean): ProductResult {
        val key = ProductResult.keyOf(adapter.source, productId)
        if (useCache) cache.get(key)?.let { return it.copy(collectedAtMillis = clock()) }

        val current = settings.settings.value
        val maxAttempts = if (current.autoRetry) MAX_ATTEMPTS else 1
        var attempt = 0
        var browserVerificationUsed = false
        while (true) {
            attempt++
            val verificationVersionBeforeRequest = synchronized(verificationLock) {
                verificationVersion(adapter.source)
            }
            val kind = try {
                val result = withTimeout(current.timeoutSeconds * 1000L) { adapter.fetch(productId) }
                    .copy(collectedAtMillis = clock())
                cache.put(result, aliasKey = key)
                return result
            } catch (_: TimeoutCancellationException) {
                FetchErrorKind.TIMEOUT
            } catch (e: CancellationException) {
                throw e
            } catch (e: FetchException) {
                if (
                    e.kind == FetchErrorKind.BLOCKED &&
                    e.challengeUrl != null &&
                    !browserVerificationUsed
                ) {
                    val verified = awaitBrowserVerification(
                        ProcessingVerification(
                            source = adapter.source,
                            productId = productId,
                            challengeUrl = e.challengeUrl,
                            userAgent = e.challengeUserAgent,
                        ),
                        verificationVersionBeforeRequest,
                    )
                    if (verified) {
                        browserVerificationUsed = synchronized(verificationLock) {
                            verificationVersion(adapter.source) > verificationVersionBeforeRequest
                        }
                        continue
                    }
                }
                e.kind
            } catch (_: Exception) {
                FetchErrorKind.PARSE
            }

            if (!kind.isTemporary || attempt >= maxAttempts) {
                return ProductResult(
                    source = adapter.source,
                    productId = productId,
                    status = ResultStatus.ERROR,
                    error = kind,
                    collectedAtMillis = clock(),
                )
            }
            delay(retryBackoffMillis * attempt)
        }
    }

    /** Waits for the active browser or opens one without racing a just-completed verification. */
    private suspend fun awaitBrowserVerification(
        request: ProcessingVerification,
        knownVersion: Long,
    ): Boolean {
        val deferred = synchronized(verificationLock) {
            if (verificationVersion(request.source) > knownVersion) return true
            activeVerification?.takeIf { it.isActive }
                ?: CompletableDeferred<Boolean>().also {
                    activeVerification = it
                    _verification.value = request
                }
        }
        return deferred.await()
    }

    private fun verificationVersion(source: DataSource): Long = verificationVersions[source] ?: 0L

    private companion object {
        const val MAX_ATTEMPTS = 3
        const val DEFAULT_BACKOFF_MILLIS = 1_000L
    }
}
