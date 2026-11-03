package com.elp.datatool.data.adapter.onepondo

import com.elp.datatool.data.remote.HttpClient
import com.elp.datatool.domain.model.FetchErrorKind
import com.elp.datatool.domain.model.FetchException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class OnePondoAdapterTest {

    private val server = MockWebServer()
    private lateinit var adapter: OnePondoAdapter

    @Before
    fun setUp() {
        server.start()
        adapter = OnePondoAdapter(
            http = HttpClient(HttpClient.createOkHttpClient()),
            metadataBaseUrl = server.url("/").toString().trimEnd('/'),
            parseDispatcher = Dispatchers.Unconfined,
        )
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun fetch_resolvesProductUrlAndRequestsOnlyCanonicalJsonPath() = runBlocking {
        server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody(fixture()))

        val result = adapter.fetch("https://www.1pondo.tv/movies/010121_001/")

        assertEquals("010121_001", result.productId)
        assertEquals("English Sample Title", result.title)
        assertEquals("https://www.1pondo.tv/movies/010121_001/", result.canonicalUrl)
        val request = server.takeRequest()
        assertEquals("/dyn/phpauto/movie_details/movie_id/010121_001.json", request.path)
        assertEquals("application/json", request.getHeader("Accept"))
        assertTrue(request.getHeader("Accept-Language").orEmpty().startsWith("en-US"))
    }

    @Test
    fun fetch_mapsHttp404ToNotFound() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404).setBody("<html>Not found</html>"))

        assertFetchError(FetchErrorKind.NOT_FOUND)
        Unit
    }

    @Test
    fun fetch_attachesProductPageToHttpAndHtmlBlocks() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(403))
        val forbidden = assertFetchError(FetchErrorKind.BLOCKED)
        assertEquals(adapter.productUrl("010121_001"), forbidden.challengeUrl)

        server.enqueue(
            MockResponse().setBody(
                "<html><title>Just a moment</title><div id=\"challenge-form\">Captcha</div></html>",
            ),
        )
        val challenge = assertFetchError(FetchErrorKind.BLOCKED)
        assertEquals(adapter.productUrl("010121_001"), challenge.challengeUrl)
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

    private suspend fun assertFetchError(kind: FetchErrorKind): FetchException {
        try {
            adapter.fetch("010121_001")
        } catch (error: FetchException) {
            assertEquals(kind, error.kind)
            return error
        }
        fail("Expected FetchException")
        throw AssertionError()
    }

    private fun fixture(): String = checkNotNull(
        javaClass.classLoader?.getResource("onepondo/onepondo_product.json"),
    ).readText()
}
