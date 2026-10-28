package com.elp.datatool.data.adapter.amazon

import com.elp.datatool.data.remote.HttpClient
import com.elp.datatool.domain.model.DataSource
import com.elp.datatool.domain.model.FetchErrorKind
import com.elp.datatool.domain.model.FetchException
import com.elp.datatool.domain.model.ProductResult
import com.elp.datatool.domain.model.SearchHit
import com.elp.datatool.domain.repository.SourceAdapter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class AmazonBookAdapter(
    private val http: HttpClient,
    private val baseUrl: String = BASE_URL,
    private val parseDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val requestIntervalMillis: Long = DEFAULT_REQUEST_INTERVAL_MILLIS,
    private val clock: () -> Long = System::currentTimeMillis,
) : SourceAdapter {

    override val source = DataSource.AMAZON_BOOKS

    private val requestMutex = Mutex()
    private var lastRequestAtMillis = 0L

    override suspend fun search(query: String): List<SearchHit> =
        throw FetchException(FetchErrorKind.UNSUPPORTED, "Amazon book search is not implemented")

    override suspend fun fetch(productId: String): ProductResult {
        val normalizedId = productId.uppercase()
        val normalizedUrl = seedUrl(normalizedId)
        val page = requestMutex.withLock {
            val waitMillis = requestIntervalMillis - (clock() - lastRequestAtMillis)
            if (lastRequestAtMillis > 0L && waitMillis > 0L) delay(waitMillis)
            lastRequestAtMillis = clock()
            http.get(normalizedUrl)
        }
        return withContext(parseDispatcher) {
            AmazonBookParser.parseProduct(page.body, normalizedId, normalizedUrl)
        }
    }

    fun seedUrl(productId: String) = "$baseUrl/dp/${productId.uppercase()}"

    companion object {
        const val BASE_URL = "https://www.amazon.com"
        const val DEFAULT_REQUEST_INTERVAL_MILLIS = 1_000L
    }
}
