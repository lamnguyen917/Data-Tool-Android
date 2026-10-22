package com.elp.datatool.data.repository

import com.elp.datatool.domain.model.AppSettings
import com.elp.datatool.domain.model.DataSource
import com.elp.datatool.domain.model.FetchErrorKind
import com.elp.datatool.domain.model.FetchException
import com.elp.datatool.domain.model.ProductResult
import com.elp.datatool.domain.model.SearchHit
import com.elp.datatool.domain.model.ThemeMode
import com.elp.datatool.domain.repository.ProductSearchRepository
import com.elp.datatool.domain.repository.ResponseCache
import com.elp.datatool.domain.repository.ResultRepository
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
}

/** Routes searches to the adapter of the selected source. */
class AdapterSearchRepository(adapters: List<SourceAdapter>) : ProductSearchRepository {
    private val adapters = adapters.associateBy { it.source }

    override suspend fun search(source: DataSource, query: String): List<SearchHit> {
        val adapter = adapters[source] ?: throw FetchException(FetchErrorKind.UNSUPPORTED)
        return adapter.search(query)
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

    fun put(result: ProductResult) {
        entries[result.key] = clock() to result
    }

    override fun clear() = entries.clear()

    private companion object {
        const val DEFAULT_TTL_MILLIS = 30 * 60 * 1000L
    }
}

/** Settings kept in memory; will be replaced by DataStore. */
class InMemorySettingsRepository : SettingsRepository {
    private val state = MutableStateFlow(AppSettings())
    override val settings: StateFlow<AppSettings> = state.asStateFlow()

    override fun setThemeMode(mode: ThemeMode) = state.update { it.copy(themeMode = mode) }
    override fun setParallelRequests(value: Int) = state.update { it.copy(parallelRequests = value) }
    override fun setTimeoutSeconds(value: Int) = state.update { it.copy(timeoutSeconds = value) }
    override fun setAutoRetry(enabled: Boolean) = state.update { it.copy(autoRetry = enabled) }
}
