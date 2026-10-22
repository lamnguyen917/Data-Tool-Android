package com.elp.datatool

import android.app.Application
import com.elp.datatool.data.adapter.unity.UnityAssetStoreAdapter
import com.elp.datatool.data.remote.HttpClient
import com.elp.datatool.data.repository.AdapterSearchRepository
import com.elp.datatool.data.repository.DefaultCollectionQueue
import com.elp.datatool.data.repository.InMemoryResultRepository
import com.elp.datatool.data.repository.InMemorySettingsRepository
import com.elp.datatool.data.repository.ProductCache
import com.elp.datatool.domain.repository.CollectionQueue
import com.elp.datatool.domain.repository.ProductSearchRepository
import com.elp.datatool.domain.repository.ResponseCache
import com.elp.datatool.domain.repository.ResultRepository
import com.elp.datatool.domain.repository.SettingsRepository
import com.elp.datatool.domain.repository.SourceAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class DataToolApplication : Application() {
    val container: AppContainer by lazy { AppContainer() }
}

/** Manual dependency container shared by all ViewModels. */
class AppContainer {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val httpClient = HttpClient(HttpClient.createOkHttpClient())
    private val productCache = ProductCache()

    // Adapters for the other sources are added here as they are implemented.
    private val adapters: List<SourceAdapter> = listOf(
        UnityAssetStoreAdapter(httpClient),
    )

    val resultRepository: ResultRepository = InMemoryResultRepository()
    val settingsRepository: SettingsRepository = InMemorySettingsRepository()
    val responseCache: ResponseCache = productCache
    val productSearchRepository: ProductSearchRepository = AdapterSearchRepository(adapters)
    val collectionQueue: CollectionQueue = DefaultCollectionQueue(
        adapters = adapters,
        results = resultRepository,
        settings = settingsRepository,
        cache = productCache,
        scope = applicationScope,
    )
}
