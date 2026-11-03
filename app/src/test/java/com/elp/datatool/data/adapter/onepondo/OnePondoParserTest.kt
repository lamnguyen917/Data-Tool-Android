package com.elp.datatool.data.adapter.onepondo

import com.elp.datatool.domain.model.DataSource
import com.elp.datatool.domain.model.FetchErrorKind
import com.elp.datatool.domain.model.FetchException
import com.elp.datatool.domain.model.ProductAttributes
import com.elp.datatool.domain.model.ResultStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class OnePondoParserTest {

    @Test
    fun parseProduct_mapsEnglishAndJapaneseMetadata() {
        val result = OnePondoParser.parseProduct(fixture(), SKU, PRODUCT_URL)

        assertEquals(DataSource.ONE_PONDO, result.source)
        assertEquals(ResultStatus.COMPLETED, result.status)
        assertEquals(SKU, result.productId)
        assertEquals("English Sample Title", result.title)
        assertEquals("Japanese sample description.", result.description)
        assertEquals(listOf("English Performer", "Shared Performer"), result.creators)
        assertEquals(
            listOf("English Category A", "Japanese Category B", "English Category C"),
            result.categories,
        )
        assertEquals(PRODUCT_URL, result.canonicalUrl)
        assertEquals(
            "https://www.1pondo.tv/moviepages/010121_001/images/thum_b.jpg",
            result.imageUrl,
        )
        assertEquals("1pondo", result.attributes[ProductAttributes.STUDIO])
        assertEquals("2024-02-29", result.attributes[ProductAttributes.RELEASE_DATE])
        assertEquals("01:00:54", result.attributes[ProductAttributes.DURATION])
        assertEquals("English Series", result.attributes[ProductAttributes.SERIES])
        assertEquals("2.9", result.attributes[ProductAttributes.RATING])
        assertFalse(result.attributes.containsKey(ProductAttributes.RATING_COUNT))

        val japanese = result.localized.getValue("ja")
        assertEquals("Japanese Sample Title", japanese.title)
        assertEquals("Japanese sample description.", japanese.description)
        assertEquals(listOf("Japanese Performer", "Shared Performer"), japanese.creators)
        assertEquals(
            listOf("Japanese Category A", "Japanese Category B", "Japanese Category C"),
            japanese.categories,
        )
        assertEquals("Japanese Series", japanese.attributes[ProductAttributes.SERIES])
        assertFalse(result.toString().contains("secret-download"))
        assertFalse(result.toString().contains("samples.example.test"))
    }

    @Test
    fun parseProduct_usesKeyedActressFallbackAndOmitsInvalidOptionalValues() {
        val body = """
            {
              "MovieID": "$SKU",
              "Title": "Fallback Title",
              "ActressesList": {
                "1": {"NameJa": "Japanese Name", "NameEn": "English Name"}
              },
              "Release": "2023-02-29",
              "Duration": -1,
              "AvgRating": "not-a-number",
              "ThumbHigh": "http://www.1pondo.tv/image.jpg",
              "Status": true
            }
        """.trimIndent()

        val result = OnePondoParser.parseProduct(body, SKU, PRODUCT_URL)

        assertEquals("Fallback Title", result.title)
        assertEquals(listOf("English Name"), result.creators)
        assertEquals(listOf("Japanese Name"), result.localized.getValue("ja").creators)
        assertNull(result.imageUrl)
        assertFalse(result.attributes.containsKey(ProductAttributes.RELEASE_DATE))
        assertFalse(result.attributes.containsKey(ProductAttributes.DURATION))
        assertFalse(result.attributes.containsKey(ProductAttributes.RATING))
    }

    @Test
    fun parseProduct_rejectsMismatchedIdentityAndInactiveProduct() {
        assertError(
            FetchErrorKind.PARSE,
            """{"MovieID":"010121_002","Title":"Title","Status":true}""",
        )
        assertError(
            FetchErrorKind.NOT_FOUND,
            """{"MovieID":"$SKU","Title":"Title","Status":false}""",
        )
    }

    @Test
    fun parseProduct_detectsHtmlChallengeBeforeJsonParsing() {
        assertError(
            FetchErrorKind.BLOCKED,
            "<html><title>Just a moment</title><div id=\"challenge-form\">Verify you are human</div></html>",
        )
    }

    private fun assertError(kind: FetchErrorKind, body: String) {
        try {
            OnePondoParser.parseProduct(body, SKU, PRODUCT_URL)
            fail("Expected FetchException")
        } catch (error: FetchException) {
            assertEquals(kind, error.kind)
        }
    }

    private fun fixture(): String = checkNotNull(
        javaClass.classLoader?.getResource("onepondo/onepondo_product.json"),
    ).readText()

    private companion object {
        const val SKU = "010121_001"
        const val PRODUCT_URL = "https://www.1pondo.tv/movies/010121_001/"
    }
}
