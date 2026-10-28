package com.elp.datatool.data.adapter.tokyohot

import com.elp.datatool.data.remote.HttpClient
import com.elp.datatool.domain.model.ProductAttributes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TokyoHotAdapterTest {

    private val server = MockWebServer()
    private lateinit var adapter: TokyoHotAdapter

    @Before
    fun setUp() {
        server.start()
        adapter = TokyoHotAdapter(
            http = HttpClient(HttpClient.createOkHttpClient()),
            baseUrl = server.url("/").toString().trimEnd('/'),
            parseDispatcher = Dispatchers.Unconfined,
        )
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun search_requestsEncodedQuery() = runBlocking {
        server.enqueue(MockResponse().setBody(fixture("tokyo_hot_search.html")))

        val hits = adapter.search("office lady")

        assertEquals(2, hits.size)
        assertEquals("/product/?q=office+lady&x=0&y=0", server.takeRequest().path)
    }

    @Test
    fun fetch_n0001SkipsSubstringMatchAndLoadsBothLanguages() = runBlocking {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when {
                request.requestUrl?.encodedPath == "/product/" && request.requestUrl?.queryParameter("q") != null ->
                    MockResponse().setBody(fixture("tokyo_hot_search.html"))
                request.requestUrl?.encodedPath == "/product/5000/" &&
                    request.requestUrl?.queryParameter("lang") == "en" ->
                    MockResponse().setBody(fixture("tokyo_hot_product_en.html"))
                request.requestUrl?.encodedPath == "/product/5000/" &&
                    request.requestUrl?.queryParameter("lang") == "ja" ->
                    MockResponse().setBody(fixture("tokyo_hot_product_ja.html"))
                else -> MockResponse().setResponseCode(404)
            }
        }

        val result = adapter.fetch("N0001")

        assertEquals("n0001", result.productId)
        assertEquals("Office Lady", result.title)
        assertEquals("Tokyo-Hot", result.attributes[ProductAttributes.STUDIO])
        assertEquals("OL無残社内ナマ中出し", result.localized.getValue("ja").title)
        assertTrue(result.canonicalUrl.orEmpty().endsWith("/product/5000/"))
        assertEquals(3, server.requestCount)
    }

    @Test
    fun languageUrl_replacesExistingLanguage() {
        assertEquals(
            "https://www.tokyo-hot.com/product/5000/?lang=ja",
            adapter.languageUrl("https://www.tokyo-hot.com/product/5000/?lang=en", "ja"),
        )
    }

    private fun fixture(name: String): String =
        checkNotNull(javaClass.classLoader?.getResource("tokyohot/$name")).readText()
}
