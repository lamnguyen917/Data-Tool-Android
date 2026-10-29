package com.elp.datatool.data.adapter.gog

import com.elp.datatool.data.remote.HttpClient
import com.elp.datatool.domain.model.DataSource
import com.elp.datatool.domain.model.FetchErrorKind
import com.elp.datatool.domain.model.FetchException
import com.elp.datatool.domain.model.ProductResult
import com.elp.datatool.domain.model.SearchHit
import com.elp.datatool.domain.repository.SourceAdapter
import java.net.URLEncoder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Public GOG adapter backed by the Catalog and Product JSON APIs. */
class GogAdapter(
    private val http: HttpClient,
    private val catalogBaseUrl: String = CATALOG_BASE_URL,
    private val productApiBaseUrl: String = PRODUCT_API_BASE_URL,
    private val parseDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : SourceAdapter {

    override val source = DataSource.GOG

    override suspend fun search(query: String): List<SearchHit> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) return emptyList()
        val page = http.get(catalogUrl(normalizedQuery))
        return withContext(parseDispatcher) { GogParser.parseSearch(page.body) }
    }

    override suspend fun fetch(productId: String): ProductResult {
        val input = productId.trim()
        val prefetchedDetails = if (input.all(Char::isDigit)) {
            fetchProduct(input)
        } else {
            null
        }
        val query = (prefetchedDetails?.slug ?: input).replace(Regex("[_-]+"), " ")
        val catalogPage = http.get(catalogUrl(query))
        val products = withContext(parseDispatcher) { GogParser.parseCatalog(catalogPage.body) }
        val catalogProduct = if (prefetchedDetails != null) {
            products.firstOrNull { it.id == prefetchedDetails.id }
        } else {
            products.firstOrNull { it.slug.equals(input, ignoreCase = true) }
        } ?: throw FetchException(FetchErrorKind.NOT_FOUND, "GOG product $input was not found")

        val details = prefetchedDetails ?: fetchProduct(catalogProduct.id)
        return withContext(parseDispatcher) { GogParser.merge(catalogProduct, details) }
    }

    private suspend fun fetchProduct(id: String): GogProductDetails {
        val page = http.get(productUrl(id))
        return withContext(parseDispatcher) { GogParser.parseProduct(page.body, id) }
    }

    fun catalogUrl(query: String): String =
        "${catalogBaseUrl.trimEnd('/')}?query=${URLEncoder.encode(query, Charsets.UTF_8.name())}&limit=$SEARCH_LIMIT"

    fun productUrl(id: String): String =
        "${productApiBaseUrl.trimEnd('/')}/products/$id?expand=$PRODUCT_EXPANSIONS"

    companion object {
        const val CATALOG_BASE_URL = "https://catalog.gog.com/v1/catalog"
        const val PRODUCT_API_BASE_URL = "https://api.gog.com"
        const val SEARCH_LIMIT = 20
        const val PRODUCT_EXPANSIONS = "description,screenshots,videos,related_products,changelog"
    }
}
