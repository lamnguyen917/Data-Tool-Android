package com.elp.datatool.domain.repository

import com.elp.datatool.domain.copy.CopyField
import com.elp.datatool.domain.copy.LineFormat
import com.elp.datatool.domain.model.AppSettings
import com.elp.datatool.domain.model.DataSource
import com.elp.datatool.domain.model.FetchErrorKind
import com.elp.datatool.domain.model.FetchException
import com.elp.datatool.domain.model.ProductResult
import com.elp.datatool.domain.model.QueueState
import com.elp.datatool.domain.model.SearchHit
import com.elp.datatool.domain.model.ThemeMode
import com.elp.datatool.domain.model.VerifiedPage
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

/**
 * Adapters whose search challenge (for example reCAPTCHA) can be completed by the user in the
 * in-app browser. The app never solves the challenge itself; it only parses the page the user
 * reached afterwards.
 */
interface BrowserVerifiedSearch {
    suspend fun searchFromVerifiedPage(page: VerifiedPage): List<SearchHit>
}

interface ResultRepository {
    val results: StateFlow<List<ProductResult>>
    fun get(key: String): ProductResult?
    fun upsert(result: ProductResult)
    fun delete(keys: Set<String>)
    fun clear()
}

interface ProductSearchRepository {
    suspend fun search(source: DataSource, query: String): List<SearchHit>

    /** Reads search results from a page the user opened after completing a site challenge. */
    suspend fun searchFromVerifiedPage(source: DataSource, page: VerifiedPage): List<SearchHit> =
        throw FetchException(FetchErrorKind.UNSUPPORTED)
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
}

interface SettingsRepository {
    val settings: StateFlow<AppSettings>
    fun setThemeMode(mode: ThemeMode)
    fun setParallelRequests(value: Int)
    fun setTimeoutSeconds(value: Int)
    fun setAutoRetry(enabled: Boolean)
    fun setCopyLineFormat(format: LineFormat)
    fun setCopyLineFields(fields: List<CopyField>)
    fun setCopyDetailsTemplate(template: String)
    fun setSelectedSource(source: DataSource)
}
