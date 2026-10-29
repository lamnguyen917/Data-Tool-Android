package com.elp.datatool.data.adapter.caribbeancom

import com.elp.datatool.domain.model.DataSource
import com.elp.datatool.domain.model.FetchErrorKind
import com.elp.datatool.domain.model.FetchException
import com.elp.datatool.domain.model.ProductAttributes
import java.nio.charset.Charset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

class CaribbeancomParserTest {

    @Test
    fun parseEnglishProduct_readsLiveLayout() {
        val result = CaribbeancomParser.parseEnglishProduct(fixture("caribbeancom_product_en.html"), EN_URL, SKU)

        assertEquals(DataSource.CARIBBEANCOM, result.source)
        assertEquals(SKU, result.productId)
        assertEquals("The Undisclosed: Scolding By Chie Aoi And Kurumi Chino", result.title)
        assertEquals(listOf("Chie Aoi", "Kurumi Chino"), result.creators)
        assertEquals(listOf("original", "nicetitties", "handjob"), result.categories)
        assertEquals("00:19:54", result.attributes[ProductAttributes.DURATION])
        assertEquals("Caribbeancom", result.attributes[ProductAttributes.STUDIO])
        assertEquals("2017-09-13", result.attributes[ProductAttributes.RELEASE_DATE])
        assertEquals(EN_URL, result.canonicalUrl)
        assertNull(result.description)
    }

    @Test
    fun parseEnglishProduct_classifiesSoftNotFoundPage() {
        assertError(FetchErrorKind.NOT_FOUND) {
            CaribbeancomParser.parseEnglishProduct(fixture("caribbeancom_not_found_en.html"), EN_URL, SKU)
        }
    }

    @Test
    fun parseEnglishProduct_rejectsPageWithoutProductMarkers() {
        assertError(FetchErrorKind.PARSE) {
            CaribbeancomParser.parseEnglishProduct(
                "<html><head><title>Home</title></head><body><h1 itemprop=\"name\">Home</h1></body></html>",
                EN_URL,
                SKU,
            )
        }
    }

    @Test
    fun parseEnglishProduct_classifiesChallengePageAsBlocked() {
        assertError(FetchErrorKind.BLOCKED) {
            CaribbeancomParser.parseEnglishProduct(
                "<html><head><title>Just a moment...</title></head><body></body></html>",
                EN_URL,
                SKU,
            )
        }
    }

    @Test
    fun parseJapaneseProduct_decodesEucJpAndReadsLocalizedFields() {
        val result = CaribbeancomParser.parseJapaneseProduct(eucJpFixture("caribbeancom_product_ja.euc-jp.html"), JA_URL)

        assertEquals("THE 未公開 〜葵千恵と千野くるみに罵られたい〜", result.title)
        assertEquals(listOf("葵千恵", "千野くるみ"), result.creators)
        assertEquals(listOf("オリジナル動画", "手コキ"), result.categories)
        assertEquals("THE 未公開", result.attributes[ProductAttributes.SERIES])
        assertEquals("カリビアンコム", result.attributes[ProductAttributes.STUDIO])
        assertEquals(
            "綺麗な人に怒られたい、そんな貴方に美女二人がブチキレル！\n" +
                "普段のやさしい二人も好きだけど、たまにはこんな乱暴な二人も興奮してしまう。",
            result.description,
        )
        assertEquals(JA_URL, result.canonicalUrl)
    }

    @Test
    fun parseJapaneseProduct_classifiesNotFoundPage() {
        assertError(FetchErrorKind.NOT_FOUND) {
            CaribbeancomParser.parseJapaneseProduct(eucJpFixture("caribbeancom_not_found_ja.euc-jp.html"), JA_URL)
        }
    }

    @Test
    fun releaseDateFromSku_mapsTwoDigitYearsAndRejectsImpossibleDates() {
        assertEquals("2017-09-13", CaribbeancomParser.releaseDateFromSku("091317-498"))
        assertEquals("1980-01-01", CaribbeancomParser.releaseDateFromSku("010180-001"))
        assertEquals("2079-12-31", CaribbeancomParser.releaseDateFromSku("123179-001"))
        assertEquals("2020-02-29", CaribbeancomParser.releaseDateFromSku("022920-001"))
        assertNull(CaribbeancomParser.releaseDateFromSku("022921-001"))
        assertNull(CaribbeancomParser.releaseDateFromSku("023017-001"))
        assertNull(CaribbeancomParser.releaseDateFromSku("999999-999"))
    }

    private fun assertError(kind: FetchErrorKind, block: () -> Unit) {
        try {
            block()
            fail("Expected FetchException")
        } catch (error: FetchException) {
            assertEquals(kind, error.kind)
        }
    }

    private fun fixture(name: String): String = bytes(name).toString(Charsets.UTF_8)

    private fun eucJpFixture(name: String): String = bytes(name).toString(Charset.forName("EUC-JP"))

    private fun bytes(name: String): ByteArray =
        checkNotNull(javaClass.classLoader?.getResource("caribbeancom/$name")).readBytes()

    private companion object {
        const val SKU = "091317-498"
        const val EN_URL = "https://en.caribbeancom.com/eng/moviepages/091317-498/index.html"
        const val JA_URL = "https://www.caribbeancom.com/moviepages/091317-498/index.html"
    }
}
