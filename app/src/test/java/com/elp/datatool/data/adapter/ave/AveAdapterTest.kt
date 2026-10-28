package com.elp.datatool.data.adapter.ave

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
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class AveAdapterTest {

    private val server = MockWebServer()
    private lateinit var adapter: AveAdapter

    @Before
    fun setUp() {
        server.start()
        adapter = AveAdapter(
            http = HttpClient(HttpClient.createOkHttpClient()),
            baseUrl = server.url("/").toString().trimEnd('/'),
            parseDispatcher = Dispatchers.Unconfined,
        )
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun search_requestsEncodedQuery() = runBlocking {
        server.enqueue(MockResponse().setBody(fixture("ave_search.html")))

        val hits = adapter.search("Reiko Nakamori")

        assertEquals(2, hits.size)
        assertEquals(
            "/dvd/search?lang=1&cat=29&culture=en-US&keyword=Reiko+Nakamori",
            server.takeRequest().path,
        )
    }

    @Test
    fun fetch_resolvesExactSkuAndLoadsBothLanguages() = runBlocking {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when {
                request.path.orEmpty().startsWith("/dvd/search?") ->
                    MockResponse().setBody(fixture("ave_search.html"))
                request.requestUrl?.queryParameter("lang") == "1" ->
                    MockResponse().setBody(fixture("ave_product_en.html"))
                request.requestUrl?.queryParameter("lang") == "2" ->
                    MockResponse().setBody(fixture("ave_product_ja.html"))
                else -> MockResponse().setResponseCode(404)
            }
        }

        val result = adapter.fetch("dvd1rhj-100")

        assertEquals("RHJ-100", result.productId)
        assertEquals("REDHOT", result.attributes[ProductAttributes.STUDIO])
        assertTrue(result.localized.getValue("ja").title.orEmpty().contains("中森玲子"))
        assertEquals(3, server.requestCount)
    }

    @Test
    fun toJapaneseUrl_addsLocaleToDirectProductUrl() {
        assertEquals(
            "https://www.aventertainments.com/dvd/detail?pro=30718&lang=2&culture=ja-JP",
            adapter.toJapaneseUrl("https://www.aventertainments.com/dvd/detail?pro=30718"),
        )
    }

    @Test
    fun search_rhj101ClassifiesVerifyBotRedirectAsBlocked() = runBlocking {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when {
                request.path.orEmpty().startsWith("/dvd/search?") -> MockResponse()
                    .setResponseCode(302)
                    .setHeader("Location", "/cart/verifybot?referral=RHJ-101")
                request.path.orEmpty().startsWith("/cart/verifybot") ->
                    MockResponse().setBody(fixture("ave_verifybot.html"))
                else -> MockResponse().setResponseCode(404)
            }
        }

        try {
            adapter.search("RHJ-101")
            fail("Expected FetchException")
        } catch (error: com.elp.datatool.domain.model.FetchException) {
            assertEquals(com.elp.datatool.domain.model.FetchErrorKind.BLOCKED, error.kind)
        }
    }

    private fun fixture(name: String): String =
        checkNotNull(javaClass.classLoader?.getResource("ave/$name")).readText()
}
