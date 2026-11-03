package com.elp.datatool.data.adapter.onepondo

import com.elp.datatool.data.adapter.withBrowserChallenge
import com.elp.datatool.data.remote.HttpClient
import com.elp.datatool.domain.model.DataSource
import com.elp.datatool.domain.model.FetchErrorKind
import com.elp.datatool.domain.model.FetchException
import com.elp.datatool.domain.model.ProductResult
import com.elp.datatool.domain.model.SearchHit
import com.elp.datatool.domain.model.ValidationResult
import com.elp.datatool.domain.repository.SourceAdapter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Collects 1pondo products from the site's public movie-details JSON endpoint. */
class OnePondoAdapter(
    private val http: HttpClient,
    private val metadataBaseUrl: String = BASE_URL,
    private val publicBaseUrl: String = BASE_URL,
    private val parseDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : SourceAdapter {

    override val source = DataSource.ONE_PONDO

    override suspend fun search(query: String): List<SearchHit> {
        throw FetchException(FetchErrorKind.UNSUPPORTED, "1pondo keyword search is not supported")
    }

    override suspend fun fetch(productId: String): ProductResult {
        val sku = when (val validation = DataSource.ONE_PONDO.validate(productId)) {
            is ValidationResult.Valid -> validation.productId
            is ValidationResult.Invalid ->
                throw FetchException(FetchErrorKind.PARSE, "Invalid 1pondo SKU or product URL")
        }
        val canonicalUrl = productUrl(sku)
        return withBrowserChallenge(canonicalUrl) {
            val page = http.get(metadataUrl(sku), JSON_HEADERS)
            withContext(parseDispatcher) {
                OnePondoParser.parseProduct(page.body, sku, canonicalUrl)
            }
        }
    }

    internal fun metadataUrl(sku: String): String =
        "${metadataBaseUrl.trimEnd('/')}/dyn/phpauto/movie_details/movie_id/$sku.json"

    internal fun productUrl(sku: String): String =
        "${publicBaseUrl.trimEnd('/')}/movies/$sku/"

    companion object {
        const val BASE_URL = "https://www.1pondo.tv"
        private val JSON_HEADERS = mapOf(
            "Accept" to "application/json",
            "Accept-Language" to "en-US,en;q=0.9,ja;q=0.8",
        )
    }
}
