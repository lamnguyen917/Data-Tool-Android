package com.elp.datatool.data.adapter.unity

import com.elp.datatool.domain.model.DataSource
import com.elp.datatool.domain.model.FetchErrorKind
import com.elp.datatool.domain.model.FetchException
import com.elp.datatool.domain.model.ProductAttributes
import com.elp.datatool.domain.model.ResultStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Fixtures are trimmed copies of real pages captured on 2026-10-22. */
class UnityAssetStoreParserTest {

    private val canonical =
        "https://assetstore.unity.com/packages/tools/particles-effects/azure-sky-dynamic-skybox-36050"

    @Test
    fun parseProduct_readsEmbeddedState() {
        val result = UnityAssetStoreParser.parseProduct(fixture("unity_product_36050.html"), canonical, "36050")

        assertEquals(DataSource.UNITY_ASSET_STORE, result.source)
        assertEquals("36050", result.productId)
        assertEquals(ResultStatus.COMPLETED, result.status)
        assertEquals("Azure[Sky] Dynamic Skybox", result.title)
        assertEquals(listOf("7stars"), result.creators)
        assertEquals(listOf("Tools", "Particles & Effects"), result.categories)
        assertEquals(canonical, result.canonicalUrl)
        assertEquals(
            "https://assetstorev1-prd-cdn.unity3d.com/key-image/dc570b30-4eac-423c-9dab-acce67bd49b8.jpg",
            result.imageUrl,
        )
    }

    @Test
    fun parseProduct_convertsHtmlDescriptionToText() {
        val description = UnityAssetStoreParser
            .parseProduct(fixture("unity_product_36050.html"), canonical, "36050")
            .description.orEmpty()

        assertTrue(description.startsWith("Azure[Sky] Dynamic Skybox is a complete and robust sky system"))
        assertFalse(description.contains("<p"))
        assertTrue(description.contains("• Legacy: Yes"))
    }

    @Test
    fun parseProduct_readsAttributes() {
        val attributes = UnityAssetStoreParser
            .parseProduct(fixture("unity_product_36050.html"), canonical, "36050")
            .attributes

        assertEquals("35.00 USD", attributes[ProductAttributes.PRICE])
        assertEquals("5.0", attributes[ProductAttributes.RATING])
        assertEquals("108", attributes[ProductAttributes.RATING_COUNT])
        assertEquals("8.0.8", attributes[ProductAttributes.VERSION])
        assertEquals("2026-10-19", attributes[ProductAttributes.LATEST_RELEASE])
        assertEquals("2015-05-14", attributes[ProductAttributes.FIRST_PUBLISHED])
        assertEquals("2021.3.43, 2022.3.0", attributes[ProductAttributes.UNITY_VERSIONS])
        assertEquals("258.4 MB", attributes[ProductAttributes.FILE_SIZE])
        assertEquals("112", attributes[ProductAttributes.ASSET_COUNT])
        assertTrue(attributes[ProductAttributes.TAGS].orEmpty().startsWith("Sky"))
    }

    @Test
    fun parseProduct_fallsBackToLdJsonWithoutEmbeddedState() {
        val html = fixture("unity_product_36050.html").replace("ReactDOMrender(", "removedRender(")

        val result = UnityAssetStoreParser.parseProduct(html, canonical, "36050")

        assertEquals("Azure[Sky] Dynamic Skybox", result.title)
        assertEquals(listOf("7stars"), result.creators)
        assertEquals(listOf("Tools", "Particles & Effects"), result.categories)
        assertEquals("35.00 USD", result.attributes[ProductAttributes.PRICE])
    }

    @Test
    fun parseProduct_homePageMeansNotFound() {
        val seedUrl = "https://assetstore.unity.com/packages/unknown/999999999"
        try {
            UnityAssetStoreParser.parseProduct(fixture("unity_not_found.html"), seedUrl, "999999999")
            fail("Expected FetchException")
        } catch (e: FetchException) {
            assertEquals(FetchErrorKind.NOT_FOUND, e.kind)
        }
    }

    @Test
    fun parseProduct_packagePageWithoutDataIsParseError() {
        val html = """<html><head><meta property="og:url" content="$canonical"/></head><body></body></html>"""
        try {
            UnityAssetStoreParser.parseProduct(html, canonical, "36050")
            fail("Expected FetchException")
        } catch (e: FetchException) {
            assertEquals(FetchErrorKind.PARSE, e.kind)
        }
    }

    @Test
    fun parseSearch_readsCoveoResultsFromRscPayload() {
        val hits = UnityAssetStoreParser.parseSearch(fixture("unity_search_terrain.html"))

        assertEquals(listOf("263149", "368344", "145808"), hits.map { it.productId })
        val first = hits.first()
        assertEquals("Gaia Pro VS - Terrain, Trees, Grass & Water for Unity 6", first.title)
        assertEquals("Procedural Worlds Inc", first.publisher)
        assertTrue(first.imageUrl.orEmpty().startsWith("https://assetstorev1-prd-cdn.unity3d.com/"))
    }

    @Test
    fun parseSearch_pageWithoutPayloadIsParseError() {
        try {
            UnityAssetStoreParser.parseSearch("<html><body>Maintenance</body></html>")
            fail("Expected FetchException")
        } catch (e: FetchException) {
            assertEquals(FetchErrorKind.PARSE, e.kind)
        }
    }

    private fun fixture(name: String): String =
        checkNotNull(javaClass.classLoader?.getResource("unity/$name")) { "Missing fixture $name" }.readText()
}
