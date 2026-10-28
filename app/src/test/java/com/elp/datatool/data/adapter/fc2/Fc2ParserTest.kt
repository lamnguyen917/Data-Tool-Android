package com.elp.datatool.data.adapter.fc2

import com.elp.datatool.domain.model.DataSource
import com.elp.datatool.domain.model.FetchErrorKind
import com.elp.datatool.domain.model.FetchException
import com.elp.datatool.domain.model.ProductAttributes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class Fc2ParserTest {

    private val productUrl = "https://adult.contents.fc2.com/article/4871322/?lang=ja"

    @Test
    fun parseProductPage_prefersJsonLdAndReadsSupplementalMetadata() {
        val parsed = Fc2Parser.parseProductPage(fixture("fc2_product.html"), productUrl, "4871322")
        val result = parsed.result

        assertEquals(DataSource.FC2, result.source)
        assertEquals("4871322", result.productId)
        assertEquals("Sample FC2 Product", result.title)
        assertFalse(result.title.orEmpty().contains("HIDDEN-NOISE"))
        assertEquals("Short description preview…", result.description)
        assertEquals(listOf("KEN REAL"), result.creators)
        assertEquals("https://images.example/fc2-4871322.png", result.imageUrl)
        assertEquals("https://adult.contents.fc2.com/article/4871322/", result.canonicalUrl)
        assertEquals("KEN REAL", result.attributes[ProductAttributes.STUDIO])
        assertEquals("2026/04/17", result.attributes[ProductAttributes.RELEASE_DATE])
        assertEquals("42:40", result.attributes[ProductAttributes.DURATION])
        assertEquals("PC, iOS, Android", result.attributes[ProductAttributes.SUPPORTED_DEVICES])
        assertEquals("1639 pt", result.attributes[ProductAttributes.PRICE])
        assertEquals("5", result.attributes[ProductAttributes.RATING])
        assertEquals("196", result.attributes[ProductAttributes.RATING_COUNT])
        assertEquals(
            "https://adult.contents.fc2.com/widget/article/4871322/description?ac=test-token",
            parsed.descriptionUrl,
        )
    }

    @Test
    fun parseDescription_removesHiddenMarkersAndRecommendations() {
        val description = Fc2Parser.parseDescription(
            fixture("fc2_description.html"),
            "https://adult.contents.fc2.com/widget/article/4871322/description?ac=test-token",
        ).orEmpty()

        assertTrue(description.contains("Full product description"))
        assertTrue(description.contains("complete seller-authored description"))
        assertFalse(description.contains("HIDDEN"))
        assertFalse(description.contains("RELATED-PRODUCT"))
        assertFalse(description.contains("FC2-RECOMMENDATION"))
        assertFalse(description.contains("SCRIPT-MARKER"))
    }

    @Test
    fun parseProductPage_classifiesSoftNotFoundPage() {
        assertError(FetchErrorKind.NOT_FOUND, fixture("fc2_not_found.html"))
    }

    @Test
    fun parseProductPage_classifiesLoginPageAsBlocked() {
        assertError(FetchErrorKind.BLOCKED, fixture("fc2_login.html"))
    }

    @Test
    fun parseProductPage_normalizesProtocolRelativeFallbackImage() {
        val html = fixture("fc2_product.html")
            .replace("https://images.example/fc2-4871322.png", "")

        val result = Fc2Parser.parseProductPage(html, productUrl, "4871322").result

        assertEquals("https://images.example/fallback.png", result.imageUrl)
    }

    private fun assertError(expected: FetchErrorKind, html: String) {
        try {
            Fc2Parser.parseProductPage(html, productUrl, "4871322")
            fail("Expected FetchException")
        } catch (error: FetchException) {
            assertEquals(expected, error.kind)
        }
    }

    private fun fixture(name: String): String =
        checkNotNull(javaClass.classLoader?.getResource("fc2/$name")).readText()
}
