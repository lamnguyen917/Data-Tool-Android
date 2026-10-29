package com.elp.datatool.data.adapter.caribbeancom

import com.elp.datatool.data.remote.HttpClient
import com.elp.datatool.domain.model.FetchErrorKind
import com.elp.datatool.domain.model.FetchException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class CaribbeancomAdapterTest {

    private val server = MockWebServer()
    private lateinit var adapter: CaribbeancomAdapter

    @Before
    fun setUp() {
        server.start()
        adapter = CaribbeancomAdapter(
            http = HttpClient(HttpClient.createOkHttpClient()),
            japaneseBaseUrl = server.url("/jp").toString().trimEnd('/'),
            englishBaseUrl = server.url("/en").toString().trimEnd('/'),
            parseDispatcher = Dispatchers.Unconfined,
        )
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun fetch_loadsCanonicalEnglishAndEucJpJapanesePages() = runBlocking {
        server.dispatcher = dispatcher(japanese = eucJpResponse("caribbeancom_product_ja.euc-jp.html"))

        val result = adapter.fetch("https://www.caribbeancom.com/moviepages/091317-498/index.html")

        assertEquals("091317-498", result.productId)
        assertEquals(listOf("Chie Aoi", "Kurumi Chino"), result.creators)
        val japanese = result.localized.getValue("ja")
        assertEquals(listOf("葵千恵", "千野くるみ"), japanese.creators)
        assertTrue(result.canonicalUrl.orEmpty().endsWith("/en/eng/moviepages/091317-498/index.html"))

        assertEquals("/en/eng/moviepages/091317-498/index.html", server.takeRequest().path)
        assertEquals("/jp/moviepages/091317-498/index.html", server.takeRequest().path)
    }

    @Test
    fun fetch_keepsEnglishResultWhenJapanesePageFails() = runBlocking {
        server.dispatcher = dispatcher(japanese = MockResponse().setResponseCode(500))

        val result = adapter.fetch("091317-498")

        assertEquals("The Undisclosed: Scolding By Chie Aoi And Kurumi Chino", result.title)
        assertTrue(result.localized.isEmpty())
    }

    @Test
    fun fetch_englishSoftNotFoundReturnsNotFound() = runBlocking {
        server.dispatcher = dispatcher(
            english = MockResponse().setBody(fixture("caribbeancom_not_found_en.html")),
            japanese = eucJpResponse("caribbeancom_product_ja.euc-jp.html"),
        )

        assertFetchError(FetchErrorKind.NOT_FOUND)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun fetch_englishForbiddenCarriesChallengeUrl() = runBlocking {
        server.dispatcher = dispatcher(english = MockResponse().setResponseCode(403))

        val error = assertFetchError(FetchErrorKind.BLOCKED)
        assertEquals(adapter.englishProductUrl("091317-498"), error.challengeUrl)
    }

    @Test
    fun search_isUnsupported() = runBlocking {
        try {
            adapter.search("Chie Aoi")
            fail("Expected FetchException")
        } catch (error: FetchException) {
            assertEquals(FetchErrorKind.UNSUPPORTED, error.kind)
        }
    }

    private suspend fun assertFetchError(kind: FetchErrorKind): FetchException {
        try {
            adapter.fetch("091317-498")
        } catch (error: FetchException) {
            assertEquals(kind, error.kind)
            return error
        }
        fail("Expected FetchException")
        throw AssertionError()
    }

    private fun dispatcher(
        english: MockResponse = MockResponse().setBody(fixture("caribbeancom_product_en.html")),
        japanese: MockResponse = MockResponse().setResponseCode(404),
    ) = object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse = when (request.path) {
            "/en/eng/moviepages/091317-498/index.html" -> english
            "/jp/moviepages/091317-498/index.html" -> japanese
            else -> MockResponse().setResponseCode(404)
        }
    }

    private fun eucJpResponse(name: String) = MockResponse()
        .setHeader("Content-Type", "text/html; charset=EUC-JP")
        .setBody(Buffer().write(bytes(name)))

    private fun fixture(name: String): String = bytes(name).toString(Charsets.UTF_8)

    private fun bytes(name: String): ByteArray =
        checkNotNull(javaClass.classLoader?.getResource("caribbeancom/$name")).readBytes()
}
