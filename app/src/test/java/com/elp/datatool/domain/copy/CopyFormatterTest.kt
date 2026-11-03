package com.elp.datatool.domain.copy

import com.elp.datatool.data.sample.SampleData
import com.elp.datatool.domain.model.DataSource
import com.elp.datatool.domain.model.ProductAttributes
import com.elp.datatool.domain.model.ProductResult
import com.elp.datatool.domain.model.ResultStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CopyFormatterTest {

    private val result = ProductResult(
        source = DataSource.UNITY_ASSET_STORE,
        productId = "36050",
        status = ResultStatus.COMPLETED,
        title = "Azure[Sky] Dynamic Skybox",
        description = "Line one\nLine two",
        creators = listOf("7stars"),
        categories = listOf("Tools", "Particles & Effects"),
        canonicalUrl = "https://assetstore.unity.com/packages/tools/particles-effects/azure-sky-dynamic-skybox-36050",
        attributes = mapOf(
            ProductAttributes.PRICE to "35.00 USD",
            ProductAttributes.RATING to "5.0",
            ProductAttributes.RATING_COUNT to "108",
        ),
    )

    @Test
    fun line_defaultFieldsAsTsv() {
        assertEquals(
            "36050\tAzure[Sky] Dynamic Skybox\tTools / Particles & Effects\t7stars\t" +
                "https://assetstore.unity.com/packages/tools/particles-effects/azure-sky-dynamic-skybox-36050",
            CopyFormatter.line(result, CopyFormatter.DEFAULT_LINE_FIELDS, LineFormat.TSV),
        )
    }

    @Test
    fun line_csvQuotesValuesWithDelimitersQuotesOrBreaks() {
        val tricky = result.copy(title = "Say \"Hi\", world", creators = listOf("A", "B"))

        assertEquals(
            "36050,\"Say \"\"Hi\"\", world\",\"A, B\",\"Line one\nLine two\"",
            CopyFormatter.line(
                tricky,
                listOf(CopyField.SKU, CopyField.TITLE, CopyField.AUTHOR, CopyField.DESCRIPTION),
                LineFormat.CSV,
            ),
        )
    }

    @Test
    fun line_tsvFlattensTabsAndLineBreaks() {
        val tabbed = result.copy(title = "A\tB")

        assertEquals(
            "A B\tLine one Line two",
            CopyFormatter.line(tabbed, listOf(CopyField.TITLE, CopyField.DESCRIPTION), LineFormat.TSV),
        )
    }

    @Test
    fun details_fillsPlaceholdersAndKeepsUnknownTokens() {
        val text = CopyFormatter.details(result, "{title} by {author}\nRating: {rating}\n{unknown}")

        assertEquals("Azure[Sky] Dynamic Skybox by 7stars\nRating: 5.0 (108)\n{unknown}", text)
    }

    @Test
    fun details_dropsLinesWhosePlaceholdersAreAllEmpty() {
        val text = CopyFormatter.details(result, "{title}\nVersion: {version}\nPrice: {price}\nFixed line")

        assertEquals("Azure[Sky] Dynamic Skybox\nPrice: 35.00 USD\nFixed line", text)
    }

    @Test
    fun details_defaultTemplate() {
        val expected = """
            Azure[Sky] Dynamic Skybox
            SKU: 36050
            Source: Unity Asset Store
            Author: 7stars
            Categories: Tools / Particles & Effects
            Price: 35.00 USD
            Rating: 5.0 (108)
            URL: https://assetstore.unity.com/packages/tools/particles-effects/azure-sky-dynamic-skybox-36050

            Line one
            Line two
        """.trimIndent()

        assertEquals(expected, CopyFormatter.details(result, CopyFormatter.DEFAULT_DETAILS_TEMPLATE))
    }

    @Test
    fun price_freeProduct() {
        val free = result.copy(attributes = mapOf(ProductAttributes.IS_FREE to "true"))
        assertEquals("Free", CopyField.PRICE.valueOf(free))
    }

    @Test
    fun adapterDefaults_onlyUseFieldsTheAdapterCollects() {
        DataSource.entries.forEach { source ->
            val fields = source.copyFields
            assertTrue(CopyFormatter.defaultLineFields(source).all { it in fields })
            Regex("""\{([a-z0-9_]+)\}""").findAll(CopyFormatter.defaultDetailsTemplate(source)).forEach { match ->
                val field = CopyField.fromKey(match.groupValues[1])
                assertTrue("$source template uses ${match.value}", field != null && field in fields)
            }
        }
    }

    @Test
    fun details_amazonDefaultTemplateUsesBookMetadata() {
        val book = SampleData.copyPreviews.getValue(DataSource.AMAZON_BOOKS)

        val text = CopyFormatter.details(book, CopyFormatter.defaultDetailsTemplate(DataSource.AMAZON_BOOKS))

        assertTrue(text.contains("Publisher: Pearson"))
        assertTrue(text.contains("ISBN-13: 978-0132350884"))
        assertTrue(text.contains("Print length: 464 pages"))
        assertTrue(text.contains("Rating: 4.7 (6,214)"))
    }

    @Test
    fun line_gogDefaultFieldsUseGameMetadata() {
        val game = SampleData.copyPreviews.getValue(DataSource.GOG)

        assertEquals(
            "1207664663\tThe Witcher 3: Wild Hunt\tCD PROJEKT RED\tCD PROJEKT RED\t2015-05-19\tWindows\t" +
                "https://www.gog.com/en/game/the_witcher_3_wild_hunt",
            CopyFormatter.line(game, CopyFormatter.defaultLineFields(DataSource.GOG), LineFormat.TSV),
        )
    }
}
