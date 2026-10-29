package com.elp.datatool.data.adapter.heyzo

import com.elp.datatool.domain.model.DataSource
import com.elp.datatool.domain.model.FetchErrorKind
import com.elp.datatool.domain.model.FetchException
import com.elp.datatool.domain.model.LocalizedProductMetadata
import com.elp.datatool.domain.model.ProductAttributes
import com.elp.datatool.domain.model.ProductResult
import com.elp.datatool.domain.model.ResultStatus
import java.util.Locale
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/** Parses Heyzo English and Japanese product pages. */
object HeyzoParser {

    internal data class JapaneseProduct(
        val metadata: LocalizedProductMetadata,
        val sharedAttributes: Map<String, String>,
    )

    fun parseEnglishProduct(html: String, finalUrl: String, sku: String): ProductResult {
        val document = Jsoup.parse(html, finalUrl)
        detectErrorPage(document)
        val rows = tableRows(document)
        val hasProductMarker = rows.any { row ->
            val label = row.label
            label.equals("Type", ignoreCase = true) ||
                label.equals("Sex Styles", ignoreCase = true) ||
                label.contains("Actress(es)", ignoreCase = true)
        }
        if (!hasProductMarker) {
            throw FetchException(FetchErrorKind.PARSE, "Heyzo English product markers not found")
        }

        val title = requiredTitle(document)
        val creators = rows.firstOrNull { it.label.contains("Actress(es)", ignoreCase = true) }
            ?.value
            ?.let(::linkTexts)
            .orEmpty()
            .cleanList()
        val categories = rows
            .filter {
                it.label.equals("Type", ignoreCase = true) ||
                    it.label.equals("Sex Styles", ignoreCase = true)
            }
            .flatMap { linkTexts(it.value) }
            .cleanList()

        return ProductResult(
            source = DataSource.HEYZO,
            productId = sku,
            status = ResultStatus.COMPLETED,
            title = title,
            creators = creators,
            categories = categories,
            canonicalUrl = finalUrl,
            attributes = mapOf(ProductAttributes.STUDIO to STUDIO),
        )
    }

    internal fun parseJapaneseProduct(html: String, finalUrl: String): JapaneseProduct {
        val document = Jsoup.parse(html, finalUrl)
        detectErrorPage(document)
        val rows = tableRows(document)
        val hasProductMarker = document.selectFirst(
            "tr.table-actor, tr[class*=table-actor], tr.table-actor-type, " +
                "tr[class*=table-actor-type], ul.tag-keyword-list",
        ) != null || rows.any { it.label.contains("公開日") }
        if (!hasProductMarker) {
            throw FetchException(FetchErrorKind.PARSE, "Heyzo Japanese product markers not found")
        }

        val releaseDate = rows.firstOrNull { it.label.contains("公開日") }
            ?.value
            ?.text()
            ?.normalizeWhitespace()
            ?.takeIf(String::isNotEmpty)
        val attributes = buildMap {
            put(ProductAttributes.STUDIO, STUDIO)
            releaseDate?.let { put(ProductAttributes.RELEASE_DATE, it) }
        }
        val categories = buildList {
            addAll(document.select("tr.table-actor-type a, tr[class*=table-actor-type] a").map(::linkText))
            addAll(document.select("ul.tag-keyword-list li a").map(::linkText))
        }.cleanList()

        return JapaneseProduct(
            metadata = LocalizedProductMetadata(
                title = requiredTitle(document),
                creators = japaneseActorLinks(document),
                categories = categories,
                canonicalUrl = finalUrl,
                attributes = attributes,
            ),
            sharedAttributes = releaseDate
                ?.let { mapOf(ProductAttributes.RELEASE_DATE to it) }
                .orEmpty(),
        )
    }

    private fun requiredTitle(document: Document): String = document.selectFirst("h1")
        ?.text()
        ?.normalizeWhitespace()
        ?.takeIf(String::isNotEmpty)
        ?: throw FetchException(FetchErrorKind.PARSE, "Heyzo product title not found")

    private fun tableRows(document: Document): List<TableRow> = document.select("tr").mapNotNull { row ->
        val cells = row.children().filter { child ->
            child.tagName().equals("td", ignoreCase = true) ||
                child.tagName().equals("th", ignoreCase = true)
        }
        if (cells.size < 2) return@mapNotNull null
        val label = cells.first().text().normalizeWhitespace()
        label.takeIf(String::isNotEmpty)?.let { TableRow(it, cells[1]) }
    }

    private fun linkTexts(element: Element): List<String> = element.select("a").map(::linkText)

    private fun japaneseActorLinks(document: Document): List<String> = document.select("tr")
        .filter { row ->
            val className = row.className()
            className.contains("table-actor") && !className.contains("table-actor-type")
        }
        .flatMap { row -> row.select("a").map(::linkText) }
        .cleanList()

    private fun linkText(element: Element): String = element.text()
        .normalizeWhitespace()
        .ifEmpty { element.attr("title").normalizeWhitespace() }

    private fun List<String>.cleanList(): List<String> =
        map { it.normalizeWhitespace() }.filter(String::isNotEmpty).distinct()

    private fun String.normalizeWhitespace(): String = trim().replace(WHITESPACE, " ")

    private fun detectErrorPage(document: Document) {
        val signal = (document.title() + " " + document.body()?.text().orEmpty()).lowercase(Locale.ROOT)
        val blocked = document.selectFirst(
            "#challenge-form, .cf-challenge, .g-recaptcha, [class*=captcha], " +
                "script[src*=challenges.cloudflare.com]",
        ) != null || BLOCK_SIGNALS.any(signal::contains)
        if (blocked) {
            throw FetchException(FetchErrorKind.BLOCKED, "Heyzo blocked the product request")
        }

        val notFound = NOT_FOUND_SIGNALS.any(signal::contains) ||
            document.selectFirst("h1")?.text()?.trim() == "404"
        if (notFound) {
            throw FetchException(FetchErrorKind.NOT_FOUND, "Heyzo product not found")
        }
    }

    private data class TableRow(val label: String, val value: Element)

    private val WHITESPACE = Regex("""\s+""")
    private val BLOCK_SIGNALS = listOf(
        "access denied",
        "attention required",
        "are you over 18 years old",
        "captcha",
        "cloudflare",
        "just a moment",
        "verify you are human",
        "18歳以上",
    )
    private val NOT_FOUND_SIGNALS = listOf(
        "404 not found",
        "page not found",
        "ページが見つかりません",
    )
    private const val STUDIO = "Heyzo"
}
