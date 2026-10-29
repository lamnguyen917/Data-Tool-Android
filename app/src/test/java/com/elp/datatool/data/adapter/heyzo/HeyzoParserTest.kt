package com.elp.datatool.data.adapter.heyzo

import com.elp.datatool.domain.model.DataSource
import com.elp.datatool.domain.model.FetchErrorKind
import com.elp.datatool.domain.model.FetchException
import com.elp.datatool.domain.model.ProductAttributes
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class HeyzoParserTest {

    @Test
    fun parseEnglishProduct_readsTitlePerformersAndCategories() {
        val result = HeyzoParser.parseEnglishProduct(
            fixture("heyzo_product_en.html"),
            "https://en.heyzo.com/moviepages/0123/index.html",
            "0123",
        )

        assertEquals(DataSource.HEYZO, result.source)
        assertEquals("0123", result.productId)
        assertEquals("English Sample Movie", result.title)
        assertEquals(listOf("Alice Example", "Fallback Actress"), result.creators)
        assertEquals(listOf("Drama", "Featured", "Romance"), result.categories)
        assertEquals("Heyzo", result.attributes[ProductAttributes.STUDIO])
        assertEquals("https://en.heyzo.com/moviepages/0123/index.html", result.canonicalUrl)
    }

    @Test
    fun parseJapaneseProduct_keepsLocalizedMetadataSeparate() {
        val product = HeyzoParser.parseJapaneseProduct(
            fixture("heyzo_product_ja.html"),
            "https://www.heyzo.com/moviepages/0123/index.html",
        )

        assertEquals("日本語サンプル作品", product.metadata.title)
        assertEquals(listOf("女優A", "女優B"), product.metadata.creators)
        assertEquals(listOf("タイプA", "タグA", "タグB"), product.metadata.categories)
        assertEquals("Heyzo", product.metadata.attributes[ProductAttributes.STUDIO])
        assertEquals("2026-10-29", product.metadata.attributes[ProductAttributes.RELEASE_DATE])
        assertEquals("2026-10-29", product.sharedAttributes[ProductAttributes.RELEASE_DATE])
    }

    @Test
    fun parseEnglishProduct_classifiesCloudflarePageAsBlocked() {
        assertEnglishError(FetchErrorKind.BLOCKED, fixture("heyzo_cloudflare.html"))
    }

    @Test
    fun parseEnglishProduct_classifiesNotFoundPage() {
        assertEnglishError(
            FetchErrorKind.NOT_FOUND,
            "<html><head><title>404 Not Found</title></head><body><h1>404</h1></body></html>",
        )
    }

    @Test
    fun parseEnglishProduct_rejectsPageWithoutProductMarkers() {
        assertEnglishError(
            FetchErrorKind.PARSE,
            "<html><body><h1>Generic page</h1></body></html>",
        )
    }

    private fun assertEnglishError(expected: FetchErrorKind, html: String) {
        try {
            HeyzoParser.parseEnglishProduct(
                html,
                "https://en.heyzo.com/moviepages/0123/index.html",
                "0123",
            )
            fail("Expected FetchException")
        } catch (error: FetchException) {
            assertEquals(expected, error.kind)
        }
    }

    private fun fixture(name: String): String =
        checkNotNull(javaClass.classLoader?.getResource("heyzo/$name")).readText()
}
