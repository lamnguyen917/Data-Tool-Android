package com.elp.datatool.data.adapter.gog

import com.elp.datatool.domain.model.DataSource
import com.elp.datatool.domain.model.ProductAttributes
import com.elp.datatool.domain.model.ResultStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class GogParserTest {

    @Test
    fun parseSearch_mapsOnlyProductFields() {
        val hits = GogParser.parseSearch(fixture("gog_catalog_alien_nations.json"))

        assertEquals(1, hits.size)
        assertEquals(DataSource.GOG, hits.single().source)
        assertEquals("1207658717", hits.single().productId)
        assertEquals("Alien Nations", hits.single().title)
        assertEquals("THQ Nordic GmbH", hits.single().publisher)
    }

    @Test
    fun merge_keepsOriginalAndStoreReleaseDatesSeparate() {
        val catalog = GogParser.parseCatalog(fixture("gog_catalog_alien_nations.json")).single()
        val details = GogParser.parseProduct(fixture("gog_product_1207658717.json"), catalog.id)

        val result = GogParser.merge(catalog, details)

        assertEquals(ResultStatus.COMPLETED, result.status)
        assertEquals("1207658717", result.productId)
        assertEquals("Alien Nations", result.title)
        assertEquals(listOf("Neo Software Produktions"), result.creators)
        assertEquals(listOf("Strategy", "Real-time", "Fantasy"), result.categories)
        assertEquals("https://www.gog.com/en/game/alien_nations", result.canonicalUrl)
        assertEquals("1999-07-01", result.attributes[ProductAttributes.PUBLICATION_DATE])
        assertEquals("2014-12-19", result.attributes[ProductAttributes.GOG_RELEASE_DATE])
        assertEquals("5.99 USD", result.attributes[ProductAttributes.PRICE])
        assertEquals("3.5", result.attributes[ProductAttributes.RATING])
        assertEquals("405", result.attributes[ProductAttributes.RATING_COUNT])
        assertEquals("THQ Nordic GmbH", result.attributes[ProductAttributes.PUBLISHER])
        assertEquals("Windows", result.attributes[ProductAttributes.PLATFORMS])
        assertEquals("Multi-player, Single-player", result.attributes[ProductAttributes.FEATURES])
        assertEquals("Deutsch, English, français", result.attributes[ProductAttributes.LANGUAGE])
        assertEquals("Fantasy, Classic", result.attributes[ProductAttributes.TAGS])
        assertFalse(result.attributes.getValue(ProductAttributes.TAGS).contains("Global filter value"))
        assertEquals("Build a kingdom.\n\n• Trade\n• Research", result.description)
    }

    private fun fixture(name: String): String =
        checkNotNull(javaClass.classLoader?.getResource("gog/$name")).readText()
}
