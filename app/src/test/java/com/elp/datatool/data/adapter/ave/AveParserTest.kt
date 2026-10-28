package com.elp.datatool.data.adapter.ave

import com.elp.datatool.domain.model.DataSource
import com.elp.datatool.domain.model.FetchErrorKind
import com.elp.datatool.domain.model.FetchException
import com.elp.datatool.domain.model.ProductAttributes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class AveParserTest {

    private val productUrl =
        "https://www.aventertainments.com/dvd/detail?lang=1&cat=29&culture=en-US&pro=30718"

    @Test
    fun parseSearch_readsCardsAndDeduplicatesLinks() {
        val hits = AveParser.parseSearch(fixture("ave_search.html"), "https://www.aventertainments.com/dvd/search")

        assertEquals(2, hits.size)
        assertEquals(DataSource.AVE, hits[0].source)
        assertEquals("RHJ-100", hits[0].productId)
        assertTrue(hits[0].title.startsWith("Red Hot Jam Vol.100"))
        assertEquals("PT-101", hits[1].productId)
        assertEquals("Second Ave Product", hits[1].title)
        assertTrue(hits[1].imageUrl.orEmpty().endsWith("/images/DVD1pt-101.webp"))
    }

    @Test
    fun parseProduct_readsEnglishMetadata() {
        val result = AveParser.parseProduct(fixture("ave_product_en.html"), productUrl, "RHJ-100")

        assertEquals(DataSource.AVE, result.source)
        assertEquals("RHJ-100", result.productId)
        assertTrue(result.title.orEmpty().startsWith("Red Hot Jam Vol.100"))
        assertEquals(listOf("Reiko Nakamori"), result.creators)
        assertEquals(listOf("Drama", "Featured Actress"), result.categories)
        assertEquals("REDHOT", result.attributes[ProductAttributes.STUDIO])
        assertEquals("07/21/2009", result.attributes[ProductAttributes.RELEASE_DATE])
        assertEquals("Red Hot Jam", result.attributes[ProductAttributes.SERIES])
        assertEquals(productUrl, result.canonicalUrl)
    }

    @Test
    fun parseLocalizedProduct_readsJapaneseMetadata() {
        val localized = AveParser.parseLocalizedProduct(
            fixture("ave_product_ja.html"),
            productUrl.replace("lang=1", "lang=2").replace("culture=en-US", "culture=ja-JP"),
        )

        assertTrue(localized.title.orEmpty().contains("中森玲子"))
        assertEquals(listOf("中森玲子"), localized.creators)
        assertEquals(listOf("ドラマ"), localized.categories)
        assertEquals("レッドホット", localized.attributes[ProductAttributes.STUDIO])
        assertEquals("レッドホットジャム", localized.attributes[ProductAttributes.SERIES])
    }

    @Test
    fun parseProduct_classifiesChallengePageAsBlocked() {
        try {
            AveParser.parseProduct("<html><title>Access Denied</title></html>", productUrl, "RHJ-100")
            fail("Expected FetchException")
        } catch (error: FetchException) {
            assertEquals(FetchErrorKind.BLOCKED, error.kind)
        }
    }

    @Test
    fun parseSearch_classifiesVerifyBotAsBlockedForRhj101() {
        try {
            AveParser.parseSearch(
                fixture("ave_verifybot.html"),
                "https://www.aventertainments.com/cart/verifybot?referral=RHJ-101",
            )
            fail("Expected FetchException")
        } catch (error: FetchException) {
            assertEquals(FetchErrorKind.BLOCKED, error.kind)
        }
    }

    @Test
    fun normalizeSku_removesDvd1Prefix() {
        assertEquals("RHJ-100", AveParser.normalizeSku("DVD1rhj-100"))
        assertEquals("相川まみ", AveParser.normalizeSku("相川まみ"))
    }

    private fun fixture(name: String): String =
        checkNotNull(javaClass.classLoader?.getResource("ave/$name")).readText()
}
