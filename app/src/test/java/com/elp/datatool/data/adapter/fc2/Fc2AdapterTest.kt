package com.elp.datatool.data.adapter.fc2

import com.elp.datatool.data.remote.HttpClient
import com.elp.datatool.domain.model.FetchErrorKind
import com.elp.datatool.domain.model.FetchException
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

class Fc2AdapterTest {

    private val server = MockWebServer()
    private lateinit var adapter: Fc2Adapter

    @Before
    fun setUp() {
        server.start()
        adapter = Fc2Adapter(
            http = HttpClient(HttpClient.createOkHttpClient()),
            baseUrl = server.url("/").toString().trimEnd('/'),
            parseDispatcher = Dispatchers.Unconfined,
        )
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun fetch_loadsProductAndSessionDescription() = runBlocking {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.requestUrl?.encodedPath) {
                "/article/4871322/" -> MockResponse().setBody(fixture("fc2_product.html"))
                "/widget/article/4871322/description" -> MockResponse().setBody(fixture("fc2_description.html"))
                else -> MockResponse().setResponseCode(404)
            }
        }

        val result = adapter.fetch("4871322")

        assertEquals("4871322", result.productId)
        assertEquals("Sample FC2 Product", result.title)
        assertEquals("Full product description", result.description.orEmpty().lineSequence().first())
        assertFalse(result.description.orEmpty().contains("HIDDEN"))
        val productRequest = server.takeRequest()
        val descriptionRequest = server.takeRequest()
        assertEquals("/article/4871322/?lang=ja", productRequest.path)
        assertTrue(productRequest.getHeader("User-Agent").orEmpty().contains("Windows NT"))
        assertEquals("/widget/article/4871322/description?ac=test-token", descriptionRequest.path)
        assertEquals(productRequest.requestUrl.toString(), descriptionRequest.getHeader("Referer"))
    }

    @Test
    fun fetch_keepsJsonLdPreviewWhenDescriptionRequestFails() = runBlocking {
        server.enqueue(MockResponse().setBody(fixture("fc2_product.html")))
        server.enqueue(MockResponse().setResponseCode(500))

        val result = adapter.fetch("4871322")

        assertEquals("Short description preview…", result.description)
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

    private fun fixture(name: String): String =
        checkNotNull(javaClass.classLoader?.getResource("fc2/$name")).readText()
}
