package com.elp.datatool.data.search

import com.elp.datatool.data.remote.HttpClient
import com.elp.datatool.domain.model.DataSource
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class DuckDuckGoSearchEngineTest {

    private val server = MockWebServer()

    @Before
    fun setUp() = server.start()

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun searchBuildsScopedQueryAndKeepsOnlyTrustedProductLinks() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """
                <a class="result__a" href="https://assetstore.unity.com/packages/tools/terrain/terrain-tool-123">
                  Terrain Tool
                </a>
                <a class="result__a" href="https://assetstore.unity.com/packages/duplicate-123">Duplicate</a>
                <a class="result__a" href="https://example.test/?url=assetstore.unity.com/packages/fake-999">Fake</a>
                <a class="result__a" href="https://assetstore.unity.com/publishers/123">Publisher</a>
                """.trimIndent()
            )
        )
        val engine = DuckDuckGoSearchEngine(
            http = HttpClient(HttpClient.createOkHttpClient()),
            baseUrl = server.url("/html/").toString(),
        )

        val results = engine.search(DataSource.UNITY_ASSET_STORE, "terrain tools")

        assertEquals(listOf("123"), results.map { it.productId })
        assertEquals("Terrain Tool", results.single().title)
        assertEquals(
            "site:assetstore.unity.com/packages terrain tools",
            server.takeRequest().requestUrl?.queryParameter("q"),
        )
    }

    @Test
    fun parserUnwrapsDuckDuckGoRedirectForAmazon() {
        val html = """
            <a class="result__a"
               href="//duckduckgo.com/l/?uddg=https%3A%2F%2Fwww.amazon.com%2Fdp%2F1098119517&amp;rut=abc">
              Effective Java
            </a>
        """.trimIndent()

        val results = DuckDuckGoSearchParser.parse(html, DataSource.AMAZON_BOOKS)

        assertEquals(listOf("1098119517"), results.map { it.productId })
        assertEquals("Effective Java", results.single().title)
    }
}
