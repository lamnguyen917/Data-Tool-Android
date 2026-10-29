package com.elp.datatool.data.adapter.caribbeancom

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

/** Parses Caribbeancom pages. See `.knowledge/pages/caribbeancom-adapter-research.md`. */
object CaribbeancomParser {

    fun parseEnglishProduct(html: String, finalUrl: String, sku: String): ProductResult {
        val document = Jsoup.parse(html, finalUrl)
        detectErrorPage(document)
        val title = productTitle(document)
        if (title == null) {
            // The English site answers unknown SKUs with HTTP 200, an empty title, and empty spec rows.
            if (document.title().trim().startsWith("- Caribbeancom", ignoreCase = true)) {
                throw FetchException(FetchErrorKind.NOT_FOUND, "Caribbeancom product $sku not found")
            }
            throw FetchException(FetchErrorKind.PARSE, "Caribbeancom English title not found")
        }
        val specs = specRows(document)
        if (specs.isEmpty()) {
            throw FetchException(FetchErrorKind.PARSE, "Caribbeancom English product markers not found")
        }

        // English performers share one link whose name is comma-separated.
        val creators = specs.content("Starring")
            ?.select("[itemprop=actor] [itemprop=name]")
            ?.flatMap { it.text().split(',') }
            .orEmpty()
            .cleanList()
        val duration = specs.content("Duration")?.let { content ->
            (content.selectFirst("[itemprop=duration]") ?: content).text().normalizeWhitespace()
        }
        val attributes = buildMap {
            put(ProductAttributes.STUDIO, STUDIO)
            releaseDateFromSku(sku)?.let { put(ProductAttributes.RELEASE_DATE, it) }
            duration?.takeIf(String::isNotEmpty)?.let { put(ProductAttributes.DURATION, it) }
        }

        return ProductResult(
            source = DataSource.CARIBBEANCOM,
            productId = sku,
            status = ResultStatus.COMPLETED,
            title = title,
            description = description(document),
            creators = creators,
            categories = specs.linkTexts("Tags"),
            canonicalUrl = finalUrl,
            attributes = attributes,
        )
    }

    fun parseJapaneseProduct(html: String, finalUrl: String): LocalizedProductMetadata {
        val document = Jsoup.parse(html, finalUrl)
        detectErrorPage(document)
        val title = productTitle(document)
            ?: throw FetchException(FetchErrorKind.PARSE, "Caribbeancom Japanese title not found")
        val specs = specRows(document)
        if (specs.isEmpty()) {
            throw FetchException(FetchErrorKind.PARSE, "Caribbeancom Japanese product markers not found")
        }

        val attributes = buildMap {
            put(ProductAttributes.STUDIO, JAPANESE_STUDIO)
            specs.linkTexts("シリーズ").firstOrNull()?.let { put(ProductAttributes.SERIES, it) }
        }
        return LocalizedProductMetadata(
            title = title,
            description = description(document),
            creators = specs.content("出演")
                ?.select("[itemprop=actor] [itemprop=name]")
                ?.map(Element::text)
                .orEmpty()
                .cleanList(),
            categories = specs.linkTexts("タグ"),
            canonicalUrl = finalUrl,
            attributes = attributes,
        )
    }

    /**
     * The catalog date encoded in the SKU (`MMDDYY-NNN`) as ISO `YYYY-MM-DD`.
     * Two-digit years from 80 map to the 1900s. Returns null for impossible dates.
     */
    fun releaseDateFromSku(sku: String): String? {
        val digits = sku.substringBefore('-')
        if (digits.length != 6 || !digits.all(Char::isDigit)) return null
        val month = digits.substring(0, 2).toInt()
        val day = digits.substring(2, 4).toInt()
        val shortYear = digits.substring(4, 6).toInt()
        val year = if (shortYear >= 80) 1900 + shortYear else 2000 + shortYear
        if (month !in 1..12 || day !in 1..daysInMonth(year, month)) return null
        return String.format(Locale.ROOT, "%04d-%02d-%02d", year, month, day)
    }

    private fun daysInMonth(year: Int, month: Int): Int = when (month) {
        2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
        4, 6, 9, 11 -> 30
        else -> 31
    }

    private fun productTitle(document: Document): String? = document.selectFirst("h1[itemprop=name]")
        ?.text()
        ?.normalizeWhitespace()
        ?.takeIf(String::isNotEmpty)

    /** Keeps the source line breaks while trimming each line. */
    private fun description(document: Document): String? = document.selectFirst("p[itemprop=description]")
        ?.wholeText()
        ?.lineSequence()
        ?.map(String::normalizeWhitespace)
        ?.filter(String::isNotEmpty)
        ?.joinToString("\n")
        ?.takeIf(String::isNotEmpty)

    /** Spec rows keyed by label without the trailing colon used on the English site. */
    private fun specRows(document: Document): Map<String, Element> =
        document.select("li.movie-detail__spec, li.movie-spec").mapNotNull { row ->
            val label = row.selectFirst("span.spec-title")?.text()?.normalizeWhitespace()?.removeSuffix(":")
            val content = row.selectFirst(".spec-content")
            if (label.isNullOrEmpty() || content == null) null else label to content
        }.toMap()

    private fun Map<String, Element>.content(label: String): Element? = this[label]

    private fun Map<String, Element>.linkTexts(label: String): List<String> =
        content(label)?.select("a")?.map(Element::text).orEmpty().cleanList()

    private fun List<String>.cleanList(): List<String> =
        map { it.normalizeWhitespace() }.filter(String::isNotEmpty).distinct()

    private fun detectErrorPage(document: Document) {
        val signal = (document.title() + " " + document.body()?.text().orEmpty()).lowercase(Locale.ROOT)
        val blocked = document.selectFirst(
            "#challenge-form, .cf-challenge, .g-recaptcha, [class*=captcha], " +
                "script[src*=challenges.cloudflare.com]",
        ) != null || BLOCK_SIGNALS.any(signal::contains)
        if (blocked) throw FetchException(FetchErrorKind.BLOCKED, "Caribbeancom blocked the request")

        if (document.selectFirst(".error404-heading, .error404-wrap") != null) {
            throw FetchException(FetchErrorKind.NOT_FOUND, "Caribbeancom product not found")
        }
    }

    private val BLOCK_SIGNALS = listOf(
        "access denied",
        "attention required",
        "just a moment",
        "verify you are human",
    )
    private const val STUDIO = "Caribbeancom"
    private const val JAPANESE_STUDIO = "カリビアンコム"
}

private val WHITESPACE = Regex("""\s+""")

private fun String.normalizeWhitespace(): String = trim().replace(WHITESPACE, " ")
