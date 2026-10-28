package com.elp.datatool.data.adapter.tokyohot

import com.elp.datatool.domain.model.DataSource
import com.elp.datatool.domain.model.FetchErrorKind
import com.elp.datatool.domain.model.FetchException
import com.elp.datatool.domain.model.ProductAttributes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class TokyoHotParserTest {

    private val productUrl = "https://www.tokyo-hot.com/product/5000/?lang=en"

    @Test
    fun parseSearch_readsCurrentCardsAndDeduplicatesLinks() {
        val hits = TokyoHotParser.parseSearch(
            fixture("tokyo_hot_search.html"),
            "https://www.tokyo-hot.com/product/?q=n0001&x=0&y=0",
        )

        assertEquals(2, hits.size)
        assertEquals(DataSource.TOKYO_HOT, hits[0].source)
        assertEquals("HTN0001", hits[0].productId)
        assertEquals("n0001", hits[1].productId)
        assertEquals("Office Lady", hits[1].title)
        assertTrue(hits[1].imageUrl.orEmpty().endsWith("/media/5000/list_image.jpg"))
    }

    @Test
    fun parseProduct_readsEnglishMetadata() {
        val result = TokyoHotParser.parseProduct(fixture("tokyo_hot_product_en.html"), productUrl, "n0001")

        assertEquals(DataSource.TOKYO_HOT, result.source)
        assertEquals("n0001", result.productId)
        assertEquals("Office Lady", result.title)
        assertEquals(listOf("Mai Kuramoto"), result.creators)
        assertEquals(listOf("Bukkake", "Masturbation", "Creampie"), result.categories)
        assertEquals("Tokyo-Hot", result.attributes[ProductAttributes.STUDIO])
        assertEquals("2003/09/15", result.attributes[ProductAttributes.RELEASE_DATE])
        assertEquals("00:41:26", result.attributes[ProductAttributes.DURATION])
        assertEquals("Office, Exclusive", result.attributes[ProductAttributes.TAGS])
        assertEquals(
            "Tokyo Hot Exclusive, AI Digital Remaster 1080p",
            result.attributes[ProductAttributes.THEME],
        )
        assertEquals("https://www.tokyo-hot.com/product/5000/", result.canonicalUrl)
        assertTrue(result.imageUrl.orEmpty().endsWith("/media/5000/list_image/820x462_default.jpg"))
    }

    @Test
    fun parseLocalizedProduct_readsJapaneseMetadata() {
        val localized = TokyoHotParser.parseLocalizedProduct(
            fixture("tokyo_hot_product_ja.html"),
            "https://www.tokyo-hot.com/product/5000/?lang=ja",
        )

        assertEquals("OL無残社内ナマ中出し", localized.title)
        assertEquals(listOf("倉本舞"), localized.creators)
        assertEquals(listOf("ブッカケ", "オナニー", "中出し"), localized.categories)
        assertEquals("東熱", localized.attributes[ProductAttributes.STUDIO])
        assertTrue(localized.attributes[ProductAttributes.SERIES].orEmpty().contains("撮りおろし徹底陵辱ビデオ"))
        assertEquals("00:41:26", localized.attributes[ProductAttributes.DURATION])
    }

    @Test
    fun parseProduct_classifiesChallengePageAsBlocked() {
        try {
            TokyoHotParser.parseProduct("<html><title>Access Denied</title></html>", productUrl, "n0001")
            fail("Expected FetchException")
        } catch (error: FetchException) {
            assertEquals(FetchErrorKind.BLOCKED, error.kind)
        }
    }

    @Test
    fun normalizeProductId_lowercasesClassicIdsOnly() {
        assertEquals("n0001", TokyoHotParser.normalizeProductId("N0001"))
        assertEquals("HTN0001", TokyoHotParser.normalizeProductId("HTN0001"))
    }

    private fun fixture(name: String): String =
        checkNotNull(javaClass.classLoader?.getResource("tokyohot/$name")).readText()
}
