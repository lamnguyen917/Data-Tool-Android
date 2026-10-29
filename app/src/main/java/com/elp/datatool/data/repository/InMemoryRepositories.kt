package com.elp.datatool.data.repository

import com.elp.datatool.domain.copy.CopyField
import com.elp.datatool.domain.copy.LineFormat
import com.elp.datatool.domain.model.AdapterSettings
import com.elp.datatool.domain.model.AppSettings
import com.elp.datatool.domain.model.DataSource
import com.elp.datatool.domain.model.FetchErrorKind
import com.elp.datatool.domain.model.FetchException
import com.elp.datatool.domain.model.ProductResult
import com.elp.datatool.domain.model.SearchHit
import com.elp.datatool.domain.model.ThemeMode
import com.elp.datatool.domain.model.VerifiedPage
import com.elp.datatool.domain.repository.BrowserVerifiedSearch
import com.elp.datatool.domain.repository.ProductSearchRepository
import com.elp.datatool.domain.repository.ResponseCache
import com.elp.datatool.domain.repository.ResultRepository
import com.elp.datatool.domain.repository.SearchEngine
import com.elp.datatool.domain.repository.SettingsRepository
import com.elp.datatool.domain.repository.SourceAdapter
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Result store kept in memory; will be replaced by the local database. */
class InMemoryResultRepository(
    initial: List<ProductResult> = emptyList(),
) : ResultRepository {
    private val state = MutableStateFlow(initial)
    override val results: StateFlow<List<ProductResult>> = state.asStateFlow()

    override fun get(key: String): ProductResult? = state.value.find { it.key == key }

    override fun upsert(result: ProductResult) {
        state.update { list ->
            val index = list.indexOfFirst { it.key == result.key }
            if (index < 0) list + result else list.toMutableList().also { it[index] = result }
        }
    }

    override fun delete(keys: Set<String>) {
        state.update { list -> list.filterNot { it.key in keys } }
    }

    override fun clear() {
        state.value = emptyList()
    }

    override fun clear(source: DataSource) {
        state.update { results -> results.filterNot { it.source == source } }
    }
}

/**
 * Routes searches to the adapter of the selected source with an optional public search fallback.
 * [importCookies] receives browser cookies after the user completes a site challenge.
 */
class AdapterSearchRepository(
    adapters: List<SourceAdapter>,
    private val settings: SettingsRepository,
    private val searchEngine: SearchEngine,
    private val importCookies: (url: String, cookieHeader: String) -> Unit = { _, _ -> },
) : ProductSearchRepository {
    private val adapters = adapters.associateBy { it.source }

    override suspend fun search(source: DataSource, query: String): List<SearchHit> {
        val adapter = adapters[source] ?: throw FetchException(FetchErrorKind.UNSUPPORTED)
        val fallbackEnabled = settings.settings.value.settingsFor(source).searchEngineFallbackEnabled
        val nativeHits = try {
            adapter.search(query)
        } catch (error: FetchException) {
            val canFallback = error.kind == FetchErrorKind.NOT_FOUND || error.kind == FetchErrorKind.UNSUPPORTED
            if (!fallbackEnabled || !canFallback) throw error
            emptyList()
        }
        return if (nativeHits.isNotEmpty() || !fallbackEnabled) {
            nativeHits
        } else {
            searchEngine.search(source, query)
        }
    }

    override suspend fun searchFromVerifiedPage(source: DataSource, page: VerifiedPage): List<SearchHit> {
        val adapter = adapters[source] as? BrowserVerifiedSearch
            ?: throw FetchException(FetchErrorKind.UNSUPPORTED)
        val hits = adapter.searchFromVerifiedPage(page)
        if (page.cookieHeader.isNotBlank()) importCookies(page.url, page.cookieHeader)
        return hits
    }
}

/** Completed results keyed by source and product id, with a time-to-live. */
class ProductCache(
    private val ttlMillis: Long = DEFAULT_TTL_MILLIS,
    private val clock: () -> Long = System::currentTimeMillis,
) : ResponseCache {
    private val entries = ConcurrentHashMap<String, Pair<Long, ProductResult>>()

    fun get(key: String): ProductResult? {
        val (storedAt, result) = entries[key] ?: return null
        if (clock() - storedAt > ttlMillis) {
            entries.remove(key)
            return null
        }
        return result
    }

    fun put(result: ProductResult, aliasKey: String? = null) {
        val entry = clock() to result
        entries[result.key] = entry
        if (aliasKey != null) entries[aliasKey] = entry
    }

    override fun clear() = entries.clear()

    override fun clear(source: DataSource) {
        entries.keys
            .filter { it.startsWith("${source.id}:") }
            .forEach(entries::remove)
    }

    private companion object {
        const val DEFAULT_TTL_MILLIS = 30 * 60 * 1000L
    }
}

/** Lightweight settings store used by unit tests. */
class InMemorySettingsRepository(initial: AppSettings = AppSettings()) : SettingsRepository {
    private val state = MutableStateFlow(initial)
    override val settings: StateFlow<AppSettings> = state.asStateFlow()

    override fun setThemeMode(mode: ThemeMode) = state.update { it.copy(themeMode = mode) }
    override fun setParallelRequests(value: Int) = state.update { it.copy(parallelRequests = value) }
    override fun setTimeoutSeconds(value: Int) = state.update { it.copy(timeoutSeconds = value) }
    override fun setAutoRetry(enabled: Boolean) = state.update { it.copy(autoRetry = enabled) }
    override fun setAdapterSettings(source: DataSource, adapterSettings: AdapterSettings) = state.update { settings ->
        settings.copy(adapterSettings = settings.adapterSettings + (source to adapterSettings))
    }
    override fun setCopyLineFormat(format: LineFormat) = state.update { it.copy(copyLineFormat = format) }
    override fun setCopyLineFields(fields: List<CopyField>) = state.update { it.copy(copyLineFields = fields) }
    override fun setCopyDetailsTemplate(template: String) = state.update { it.copy(copyDetailsTemplate = template) }
    override fun setSelectedSource(source: DataSource) = state.update { it.copy(selectedSource = source) }
}
