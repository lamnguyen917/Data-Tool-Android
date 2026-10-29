package com.elp.datatool.data.repository

import com.elp.datatool.domain.model.AppSettings
import com.elp.datatool.domain.model.AdapterSettings
import com.elp.datatool.domain.model.DataSource
import com.elp.datatool.domain.model.FetchErrorKind
import com.elp.datatool.domain.model.FetchException
import com.elp.datatool.domain.model.ProductResult
import com.elp.datatool.domain.model.SearchHit
import com.elp.datatool.domain.repository.SearchEngine
import com.elp.datatool.domain.repository.SourceAdapter
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class AdapterSearchRepositoryTest {

    @Test
    fun nativeResultsDoNotCallSearchEngine() = runTest {
        val nativeHit = hit("1", "Native")
        val engine = FakeSearchEngine(listOf(hit("2", "Fallback")))
        val repository = repository(FakeAdapter { listOf(nativeHit) }, enabled = true, engine)

        assertEquals(listOf(nativeHit), repository.search(DataSource.UNITY_ASSET_STORE, "terrain"))
        assertEquals(0, engine.calls)
    }

    @Test
    fun emptyNativeResultsUseSearchEngineOnlyWhenEnabled() = runTest {
        val fallback = hit("2", "Fallback")
        val disabledEngine = FakeSearchEngine(listOf(fallback))
        val disabled = repository(FakeAdapter { emptyList() }, enabled = false, disabledEngine)
        assertEquals(emptyList<SearchHit>(), disabled.search(DataSource.UNITY_ASSET_STORE, "terrain"))
        assertEquals(0, disabledEngine.calls)

        val enabledEngine = FakeSearchEngine(listOf(fallback))
        val enabled = repository(FakeAdapter { emptyList() }, enabled = true, enabledEngine)
        assertEquals(listOf(fallback), enabled.search(DataSource.UNITY_ASSET_STORE, "terrain"))
        assertEquals(1, enabledEngine.calls)
    }

    @Test
    fun unsupportedNativeSearchUsesEnabledFallback() = runTest {
        val fallback = hit("2", "Fallback")
        val engine = FakeSearchEngine(listOf(fallback))
        val adapter = FakeAdapter { throw FetchException(FetchErrorKind.UNSUPPORTED) }
        val repository = repository(adapter, enabled = true, engine)

        assertEquals(listOf(fallback), repository.search(DataSource.UNITY_ASSET_STORE, "terrain"))
        assertEquals(1, engine.calls)
    }

    @Test
    fun operationalErrorsDoNotUseSearchEngine() = runTest {
        val engine = FakeSearchEngine(emptyList())
        val adapter = FakeAdapter { throw FetchException(FetchErrorKind.NETWORK) }
        val repository = repository(adapter, enabled = true, engine)

        try {
            repository.search(DataSource.UNITY_ASSET_STORE, "terrain")
            fail("Expected the native network error")
        } catch (error: FetchException) {
            assertEquals(FetchErrorKind.NETWORK, error.kind)
        }
        assertEquals(0, engine.calls)
    }

    private fun repository(adapter: SourceAdapter, enabled: Boolean, engine: SearchEngine) =
        AdapterSearchRepository(
            adapters = listOf(adapter),
            settings = InMemorySettingsRepository(
                AppSettings(
                    adapterSettings = mapOf(
                        adapter.source to AdapterSettings(searchEngineFallbackEnabled = enabled)
                    )
                )
            ),
            searchEngine = engine,
        )

    private fun hit(id: String, title: String) =
        SearchHit(DataSource.UNITY_ASSET_STORE, id, title)

    private class FakeAdapter(
        private val searchResult: suspend () -> List<SearchHit>,
    ) : SourceAdapter {
        override val source = DataSource.UNITY_ASSET_STORE
        override suspend fun search(query: String): List<SearchHit> = searchResult()
        override suspend fun fetch(productId: String): ProductResult = error("Not used")
    }

    private class FakeSearchEngine(
        private val results: List<SearchHit>,
    ) : SearchEngine {
        var calls = 0
        override suspend fun search(source: DataSource, query: String): List<SearchHit> {
            calls++
            return results
        }
    }
}
