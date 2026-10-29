package com.elp.datatool.data.adapter.gog

import com.elp.datatool.data.remote.HttpClient
import com.elp.datatool.domain.model.FetchErrorKind
import com.elp.datatool.domain.model.FetchException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class GogAdapterTest {

    private val server = MockWebServer()
    private lateinit var adapter: GogAdapter

    @Before
    fun setUp() {
        server.start()
        val baseUrl = server.url("/").toString().trimEnd('/')
        adapter = GogAdapter(
            http = HttpClient(HttpClient.createOkHttpClient()),
            catalogBaseUrl = "$baseUrl/v1/catalog",
            productApiBaseUrl = baseUrl,
            parseDispatcher = Dispatchers.Unconfined,
        )
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun search_requestsEncodedCatalogQuery() = runBlocking {
        server.enqueue(MockResponse().setBody(fixture("gog_catalog_alien_nations.json")))

        val hits = adapter.search("alien nations")

        assertEquals(listOf("1207658717"), hits.map { it.productId })
        assertEquals("/v1/catalog?query=alien+nations&limit=20", server.takeRequest().path)
    }

    @Test
    fun fetchBySlug_resolvesExactCatalogProductThenLoadsDetails() = runBlocking {
        server.enqueue(MockResponse().setBody(fixture("gog_catalog_alien_nations.json")))
        server.enqueue(MockResponse().setBody(fixture("gog_product_1207658717.json")))

        val result = adapter.fetch("alien_nations")

        assertEquals("1207658717", result.productId)
        assertEquals("Alien Nations", result.title)
        assertEquals("/v1/catalog?query=alien+nations&limit=20", server.takeRequest().path)
        assertEquals(
            "/products/1207658717?expand=description,screenshots,videos,related_products,changelog",
            server.takeRequest().path,
        )
    }

    @Test
    fun fetchByNumericId_validatesProductBeforeCatalogLookup() = runBlocking {
        server.enqueue(MockResponse().setBody(fixture("gog_product_1207658717.json")))
        server.enqueue(MockResponse().setBody(fixture("gog_catalog_alien_nations.json")))

        val result = adapter.fetch("1207658717")

        assertEquals("1207658717", result.productId)
        assertEquals(
            "/products/1207658717?expand=description,screenshots,videos,related_products,changelog",
            server.takeRequest().path,
        )
        assertEquals("/v1/catalog?query=alien+nations&limit=20", server.takeRequest().path)
    }

    @Test
    fun fetchBySlug_rejectsCatalogWithoutExactSlug() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                fixture("gog_catalog_alien_nations.json").replace("alien_nations", "alien_nations_gold")
            )
        )

        try {
            adapter.fetch("alien_nations")
            fail("Expected FetchException")
        } catch (error: FetchException) {
            assertEquals(FetchErrorKind.NOT_FOUND, error.kind)
        }
        assertEquals(1, server.requestCount)
    }

    private fun fixture(name: String): String =
        checkNotNull(javaClass.classLoader?.getResource("gog/$name")).readText()
}
