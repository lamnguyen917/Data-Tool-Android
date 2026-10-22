package com.elp.datatool.data.adapter.unity

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
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class UnityAssetStoreAdapterTest {

    private val server = MockWebServer()
    private lateinit var adapter: UnityAssetStoreAdapter

    @Before
    fun setUp() {
        server.start()
        adapter = UnityAssetStoreAdapter(
            http = HttpClient(HttpClient.createOkHttpClient()),
            baseUrl = server.url("/").toString().trimEnd('/'),
            parseDispatcher = Dispatchers.Unconfined,
        )
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun fetch_followsSeedRedirectsToCanonicalPage() = runBlocking {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.path) {
                "/packages/unknown/36050" -> redirect("/packages/unknown/azure-sky-dynamic-skybox-36050")
                "/packages/unknown/azure-sky-dynamic-skybox-36050" ->
                    redirect("/packages/tools/particles-effects/azure-sky-dynamic-skybox-36050")
                "/packages/tools/particles-effects/azure-sky-dynamic-skybox-36050" ->
                    MockResponse().setBody(fixture("unity_product_36050.html"))
                else -> MockResponse().setResponseCode(404)
            }
        }

        val result = adapter.fetch("36050")

        assertEquals("Azure[Sky] Dynamic Skybox", result.title)
        assertEquals(3, server.requestCount)
        val firstRequest = server.takeRequest()
        assertEquals("/packages/unknown/36050", firstRequest.path)
        assertTrue(firstRequest.getHeader("User-Agent").orEmpty().contains("Mozilla"))
    }

    @Test
    fun fetch_mapsHttpStatusToErrorKind() = runBlocking {
        assertFetchError(429, FetchErrorKind.RATE_LIMITED)
        assertFetchError(403, FetchErrorKind.BLOCKED)
        assertFetchError(503, FetchErrorKind.HTTP)
        assertFetchError(404, FetchErrorKind.NOT_FOUND)
    }

    @Test
    fun search_requestsEncodedQuery() = runBlocking {
        server.enqueue(MockResponse().setBody(fixture("unity_search_terrain.html")))

        val hits = adapter.search("terrain tools")

        assertEquals(3, hits.size)
        assertEquals("/search?q=terrain+tools", server.takeRequest().path)
    }

    private suspend fun assertFetchError(code: Int, expected: FetchErrorKind) {
        server.enqueue(MockResponse().setResponseCode(code))
        try {
            adapter.fetch("1")
            fail("Expected FetchException for HTTP $code")
        } catch (e: FetchException) {
            assertEquals(expected, e.kind)
        }
    }

    private fun redirect(location: String) =
        MockResponse().setResponseCode(301).setHeader("Location", location)

    private fun fixture(name: String): String =
        checkNotNull(javaClass.classLoader?.getResource("unity/$name")).readText()
}
