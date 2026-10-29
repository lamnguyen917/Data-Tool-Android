package com.elp.datatool.domain.repository

import com.elp.datatool.domain.copy.CopyField
import com.elp.datatool.domain.copy.LineFormat
import com.elp.datatool.domain.model.AdapterSettings
import com.elp.datatool.domain.model.AppSettings
import com.elp.datatool.domain.model.DataSource
import com.elp.datatool.domain.model.ProductResult
import com.elp.datatool.domain.model.QueueState
import com.elp.datatool.domain.model.SearchHit
import com.elp.datatool.domain.model.ThemeMode
import kotlinx.coroutines.flow.StateFlow

/**
 * Fetches product data from one website.
 * Implementations throw [com.elp.datatool.domain.model.FetchException] on failure.
 */
interface SourceAdapter {
    val source: DataSource
    suspend fun search(query: String): List<SearchHit>
    suspend fun fetch(productId: String): ProductResult
}

interface ResultRepository {
    val results: StateFlow<List<ProductResult>>
    fun get(key: String): ProductResult?
    fun upsert(result: ProductResult)
    fun delete(keys: Set<String>)
    fun clear()
    fun clear(source: DataSource)
}

interface ProductSearchRepository {
    suspend fun search(source: DataSource, query: String): List<SearchHit>
}

/** Searches public web results for trusted product pages from one data source. */
interface SearchEngine {
    suspend fun search(source: DataSource, query: String): List<SearchHit>
}

/** Runs collection batches and single retries, writing into [ResultRepository]. */
interface CollectionQueue {
    val state: StateFlow<QueueState>
    fun isSupported(source: DataSource): Boolean
    fun start(source: DataSource, productIds: List<String>)
    fun cancel()
    fun retry(source: DataSource, productId: String)
}

interface ResponseCache {
    fun clear()
    fun clear(source: DataSource)
}

interface SettingsRepository {
    val settings: StateFlow<AppSettings>
    fun setThemeMode(mode: ThemeMode)
    fun setParallelRequests(value: Int)
    fun setTimeoutSeconds(value: Int)
    fun setAutoRetry(enabled: Boolean)
    fun setAdapterSettings(source: DataSource, adapterSettings: AdapterSettings)
    fun setCopyLineFormat(format: LineFormat)
    fun setCopyLineFields(fields: List<CopyField>)
    fun setCopyDetailsTemplate(template: String)
    fun setSelectedSource(source: DataSource)
}
