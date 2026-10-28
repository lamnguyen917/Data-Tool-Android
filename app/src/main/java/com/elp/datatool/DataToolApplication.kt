package com.elp.datatool

import android.app.Application
import com.elp.datatool.data.adapter.amazon.AmazonBookAdapter
import com.elp.datatool.data.adapter.ave.AveAdapter
import com.elp.datatool.data.adapter.fc2.Fc2Adapter
import com.elp.datatool.data.adapter.tokyohot.TokyoHotAdapter
import com.elp.datatool.data.adapter.unity.UnityAssetStoreAdapter
import com.elp.datatool.data.remote.HttpClient
import com.elp.datatool.data.repository.AdapterSearchRepository
import com.elp.datatool.data.repository.DefaultCollectionQueue
import com.elp.datatool.data.repository.InMemoryResultRepository
import com.elp.datatool.data.repository.PreferencesSettingsRepository
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
    val container: AppContainer by lazy { AppContainer(this) }
}

/** Manual dependency container shared by all ViewModels. */
class AppContainer(application: Application) {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val httpClient = HttpClient(HttpClient.createOkHttpClient())
    private val productCache = ProductCache()

    private val adapters: List<SourceAdapter> = listOf(
        UnityAssetStoreAdapter(httpClient),
        AmazonBookAdapter(httpClient),
        AveAdapter(httpClient),
        TokyoHotAdapter(httpClient),
        Fc2Adapter(httpClient),
    )

    val resultRepository: ResultRepository = InMemoryResultRepository()
    val settingsRepository: SettingsRepository = PreferencesSettingsRepository(application.applicationContext)
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
