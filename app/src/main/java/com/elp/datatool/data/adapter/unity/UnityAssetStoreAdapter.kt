package com.elp.datatool.data.adapter.unity

import com.elp.datatool.data.remote.HttpClient
import com.elp.datatool.domain.model.DataSource
import com.elp.datatool.domain.model.ProductResult
import com.elp.datatool.domain.model.SearchHit
import com.elp.datatool.domain.repository.SourceAdapter
import java.net.URLEncoder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UnityAssetStoreAdapter(
    private val http: HttpClient,
    private val baseUrl: String = BASE_URL,
    private val parseDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : SourceAdapter {

    override val source = DataSource.UNITY_ASSET_STORE

    override suspend fun search(query: String): List<SearchHit> {
        val page = http.get("$baseUrl/search?q=${URLEncoder.encode(query.trim(), "UTF-8")}")
        return withContext(parseDispatcher) { UnityAssetStoreParser.parseSearch(page.body) }
    }

    /**
     * Requests the seed URL `/packages/unknown/{id}`; the store redirects to the canonical page.
     * The middle path segment is required (see the crawl knowledge page).
     */
    override suspend fun fetch(productId: String): ProductResult {
        val page = http.get(seedUrl(productId))
        return withContext(parseDispatcher) {
            UnityAssetStoreParser.parseProduct(page.body, page.url, productId)
        }
    }

    fun seedUrl(productId: String) = "$baseUrl/packages/unknown/$productId"

    companion object {
        const val BASE_URL = "https://assetstore.unity.com"
    }
}
