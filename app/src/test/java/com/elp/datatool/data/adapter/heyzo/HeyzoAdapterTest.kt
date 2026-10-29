package com.elp.datatool.data.adapter.heyzo

import com.elp.datatool.data.remote.HttpClient
import com.elp.datatool.domain.model.FetchErrorKind
import com.elp.datatool.domain.model.FetchException
import com.elp.datatool.domain.model.ProductAttributes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class HeyzoAdapterTest {

    private val server = MockWebServer()
    private lateinit var adapter: HeyzoAdapter

    @Before
    fun setUp() {
        server.start()
        adapter = HeyzoAdapter(
            http = HttpClient(HttpClient.createOkHttpClient()),
            japaneseBaseUrl = server.url("/jp").toString().trimEnd('/'),
            englishBaseUrl = server.url("/en").toString().trimEnd('/'),
            parseDispatcher = Dispatchers.Unconfined,
        )
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun fetch_loadsEnglishThenJapaneseAndMergesReleaseDate() = runBlocking {
        server.dispatcher = productDispatcher()

        val result = adapter.fetch("https://en.heyzo.com/moviepages/0123/index.html")

        assertEquals("0123", result.productId)
        assertEquals("English Sample Movie", result.title)
        assertEquals(listOf("Drama", "Featured", "Romance"), result.categories)
        assertEquals("2026-10-29", result.attributes[ProductAttributes.RELEASE_DATE])
        assertEquals("日本語サンプル作品", result.localized.getValue("ja").title)
        assertEquals(listOf("タイプA", "タグA", "タグB"), result.localized.getValue("ja").categories)

        val englishRequest = server.takeRequest()
        val japaneseRequest = server.takeRequest()
        assertEquals("/en/moviepages/0123/index.html", englishRequest.path)
        assertEquals("/jp/moviepages/0123/index.html", japaneseRequest.path)
        assertTrue(englishRequest.getHeader("User-Agent").orEmpty().contains("Windows NT"))
        assertTrue(japaneseRequest.getHeader("Accept-Language").orEmpty().startsWith("ja-JP"))
    }

    @Test
    fun fetch_keepsEnglishResultWhenJapaneseRequestFails() = runBlocking {
        server.enqueue(MockResponse().setBody(fixture("heyzo_product_en.html")))
        server.enqueue(MockResponse().setResponseCode(403))

        val result = adapter.fetch("0123")

        assertEquals("English Sample Movie", result.title)
        assertTrue(result.localized.isEmpty())
        assertFalse(result.attributes.containsKey(ProductAttributes.RELEASE_DATE))
    }

    @Test
    fun fetch_propagatesEnglishBlock() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(403))

        try {
            adapter.fetch("0123")
            fail("Expected FetchException")
        } catch (error: FetchException) {
            assertEquals(FetchErrorKind.BLOCKED, error.kind)
        }
        assertEquals(1, server.requestCount)
    }

    @Test
    fun search_isExplicitlyUnsupported() = runBlocking {
        try {
            adapter.search("sample")
            fail("Expected FetchException")
        } catch (error: FetchException) {
            assertEquals(FetchErrorKind.UNSUPPORTED, error.kind)
        }
        assertEquals(0, server.requestCount)
    }

    private fun productDispatcher() = object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse = when (request.requestUrl?.encodedPath) {
            "/en/moviepages/0123/index.html" -> MockResponse().setBody(fixture("heyzo_product_en.html"))
            "/jp/moviepages/0123/index.html" -> MockResponse().setBody(fixture("heyzo_product_ja.html"))
            else -> MockResponse().setResponseCode(404)
        }
    }

    private fun fixture(name: String): String =
        checkNotNull(javaClass.classLoader?.getResource("heyzo/$name")).readText()
}
