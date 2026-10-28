package com.elp.datatool.data.adapter.amazon

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

class AmazonBookAdapterTest {

    private val server = MockWebServer()
    private lateinit var adapter: AmazonBookAdapter

    @Before
    fun setUp() {
        server.start()
        adapter = AmazonBookAdapter(
            http = HttpClient(HttpClient.createOkHttpClient()),
            baseUrl = server.url("/").toString().trimEnd('/'),
            parseDispatcher = Dispatchers.Unconfined,
            requestIntervalMillis = 0L,
        )
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun fetch_requestsNormalizedDpUrlWithBrowserHeaders() = runBlocking {
        server.enqueue(MockResponse().setBody(fixture()))

        val result = adapter.fetch("1098119517")

        assertEquals("Flutter and Dart Cookbook: Developing Full-Stack Applications for the Cloud", result.title)
        val request = server.takeRequest()
        assertEquals("/dp/1098119517", request.path)
        assertTrue(request.getHeader("User-Agent").orEmpty().contains("Mozilla"))
        assertEquals("en-US,en;q=0.9", request.getHeader("Accept-Language"))
    }

    @Test
    fun search_isExplicitlyUnsupported() = runBlocking {
        try {
            adapter.search("flutter")
            fail("Expected FetchException")
        } catch (e: FetchException) {
            assertEquals(FetchErrorKind.UNSUPPORTED, e.kind)
        }
    }

    private fun fixture(): String =
        checkNotNull(javaClass.classLoader?.getResource("amazon/amazon_book_1098119517.html")).readText()
}
