package com.elp.datatool.data.adapter.ave

import com.elp.datatool.domain.model.DataSource
import com.elp.datatool.domain.model.FetchErrorKind
import com.elp.datatool.domain.model.FetchException
import com.elp.datatool.domain.model.LocalizedProductMetadata
import com.elp.datatool.domain.model.ProductAttributes
import com.elp.datatool.domain.model.ProductResult
import com.elp.datatool.domain.model.ResultStatus
import com.elp.datatool.domain.model.SearchHit
import java.net.URI
import java.util.Locale
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/** Parses AV Entertainments pages. See `.knowledge/pages/ave-adapter-research.md`. */
object AveParser {

    private val datePattern = Regex("""\d{1,2}/\d{1,2}/\d{4}""")
    private val imageSkuPattern = Regex("""[A-Z0-9]{2,}[-_]?[A-Z0-9]*\d{2,5}""")
    private val productImageSelector = listOf(
        ".product-image img[src]",
        ".single-product-image img[src]",
        "#product-image img[src]",
    ).joinToString(", ")

    internal data class Candidate(
        val sku: String,
        val url: String,
        val title: String,
        val imageUrl: String?,
    ) {
        fun toSearchHit() = SearchHit(
            source = DataSource.AVE,
            productId = sku,
            title = title.ifBlank { sku },
            imageUrl = imageUrl,
        )
    }

    fun parseSearch(html: String, pageUrl: String): List<SearchHit> =
        parseCandidates(html, pageUrl).map(Candidate::toSearchHit)

    internal fun parseCandidates(html: String, pageUrl: String): List<Candidate> {
        val document = Jsoup.parse(html, pageUrl)
        detectBlockPage(document)
        return document.select(".single-slider-product")
            .mapNotNull { card -> candidate(card) }
            .distinctBy(Candidate::url)
    }

    fun parseProduct(
        html: String,
        finalUrl: String,
        requestedSku: String?,
    ): ProductResult {
        val page = parsePage(html, finalUrl)
        val sku = page.sku ?: requestedSku?.let(::normalizeSku)
            ?: throw FetchException(FetchErrorKind.PARSE, "Ave product SKU not found")
        return ProductResult(
            source = DataSource.AVE,
            productId = sku,
            status = ResultStatus.COMPLETED,
            title = page.title,
            description = page.description,
            creators = page.creators,
            categories = page.categories,
            canonicalUrl = page.canonicalUrl,
            attributes = page.attributes,
        )
    }

    fun parseLocalizedProduct(html: String, finalUrl: String): LocalizedProductMetadata {
        val page = parsePage(html, finalUrl)
        return LocalizedProductMetadata(
            title = page.title,
            description = page.description,
            creators = page.creators,
            categories = page.categories,
            canonicalUrl = page.canonicalUrl,
            attributes = page.attributes,
        )
    }

    fun normalizeSku(value: String): String {
        val trimmed = value.trim()
        val withoutPrefix = if (trimmed.startsWith("dvd1", ignoreCase = true)) {
            trimmed.substring(4)
        } else {
            trimmed
        }
        return withoutPrefix.uppercase(Locale.ROOT)
    }

    private fun candidate(card: Element): Candidate? {
        val anchor = card.selectFirst(".single-slider-product__image a") ?: return null
        val url = anchor.absUrl("href").ifBlank { anchor.attr("href") }
        if (url.isBlank()) return null
        val image = anchor.selectFirst("img")
        val rawImageUrl = image?.attr("src")?.takeIf(String::isNotBlank)
            ?: image?.attr("data-src")?.takeIf(String::isNotBlank)
        val imageUrl = rawImageUrl?.let { source ->
            image?.absUrl(if (image.hasAttr("src") && image.attr("src").isNotBlank()) "src" else "data-src")
                .orEmpty()
                .ifBlank { source }
        }
        val sku = rawImageUrl?.let(::skuFromImageUrl)
            ?.takeIf(String::isNotBlank)
            ?: productRecordId(url)?.let { "PRO$it" }
            ?: return null
        val title = image?.attr("alt")?.trim().orEmpty().ifBlank {
            card.selectFirst(".product-title")?.text()?.trim().orEmpty()
        }
        return Candidate(normalizeSku(sku), url, title, imageUrl)
    }

    private fun parsePage(html: String, finalUrl: String): PageData {
        val document = Jsoup.parse(html, finalUrl)
        detectBlockPage(document)
        val title = document.selectFirst(".section-title h3")?.text()?.trim()
            ?.takeIf(String::isNotEmpty)
            ?: throw FetchException(FetchErrorKind.PARSE, "Ave product title not found")

        var sku: String? = null
        var studio: String? = null
        var releaseDate: String? = null
        var series: String? = null
        val creators = mutableListOf<String>()
        val categories = mutableListOf<String>()

        fun apply(label: String, value: String, links: List<String> = emptyList()) {
            val cleanLabel = label.trim().trimEnd(':').trim()
            val cleanValue = value.trim()
            when (cleanLabel) {
                "Studio", "スタジオ" -> if (studio.isNullOrBlank()) studio = cleanValue
                "Starring", "主演女優" -> if (creators.isEmpty()) {
                    creators += links.ifEmpty { splitValues(cleanValue) }
                }
                "Category", "カテゴリ" -> if (categories.isEmpty()) {
                    categories += links.ifEmpty { splitValues(cleanValue) }
                }
                "Series", "シリーズ" -> if (series.isNullOrBlank()) {
                    series = links.ifEmpty { splitValues(cleanValue) }.joinToString(", ").ifBlank { cleanValue }
                }
                "Item Number", "Item#", "Item #", "SKU", "品番", "商品番号" -> {
                    if (cleanValue.isNotBlank()) sku = normalizeSku(cleanValue)
                }
                "Date", "発売日" -> if (releaseDate.isNullOrBlank()) releaseDate = cleanDate(cleanValue)
            }
        }

        document.select(".additional-info").forEach { block ->
            val heading = block.selectFirst("h4")?.text()?.trim().orEmpty()
            val links = linkTexts(block)
            when (heading) {
                "Starring", "主演女優" -> creators += links
                "Category", "カテゴリ" -> categories += links
                "Series", "シリーズ" -> {
                    if (series.isNullOrBlank()) {
                        series = links.joinToString(", ").takeIf(String::isNotBlank)
                    }
                }
                "Features", "DVD特典" -> Unit
                else -> detailPairs(block).forEach { (label, value) -> apply(label, value) }
            }
        }

        document.select(".single-info").forEach { row ->
            val label = row.selectFirst(".title")?.text().orEmpty()
            val valueElement = row.selectFirst(".value, .tag-title, .value-category")
            val value = valueElement?.text().orEmpty()
            val links = valueElement?.let(::linkTexts).orEmpty()
            apply(label, value, links)
        }

        if (sku.isNullOrBlank()) {
            sku = document.selectFirst(productImageSelector)
                ?.attr("src")
                ?.let(::skuFromImageUrl)
                ?.takeIf(imageSkuPattern::matches)
        }

        return PageData(
            sku = sku,
            title = title,
            description = document.selectFirst(".product-description")?.text()?.trim()?.ifBlank { null },
            creators = creators.cleanList(),
            categories = categories.cleanList(),
            canonicalUrl = canonicalUrl(document, finalUrl),
            attributes = buildMap {
                studio?.takeIf(String::isNotBlank)?.let { put(ProductAttributes.STUDIO, it) }
                releaseDate?.takeIf(String::isNotBlank)?.let { put(ProductAttributes.RELEASE_DATE, it) }
                series?.takeIf(String::isNotBlank)?.let { put(ProductAttributes.SERIES, it) }
            },
        )
    }

    private fun detailPairs(block: Element): Map<String, String> = buildMap {
        block.select(".single-info").forEach { row ->
            val label = row.selectFirst(".title")?.text()?.trim().orEmpty().trimEnd(':')
            val value = row.selectFirst(".value, .tag-title, .value-category")?.text()?.trim().orEmpty()
            if (label.isNotBlank() && value.isNotBlank()) put(label, value)
        }
        block.select(".title").forEach { element ->
            val text = element.text()
            val separator = text.indexOf(':')
            if (separator > 0) {
                val label = text.substring(0, separator).trim()
                val value = text.substring(separator + 1).trim()
                if (label.isNotBlank() && value.isNotBlank()) putIfAbsent(label, value)
            }
        }
    }

    private fun cleanDate(value: String): String = datePattern.find(value)?.value
        ?: value.replace("(Discontinue)", "")
            .replace("(Available)", "")
            .trim()

    private fun splitValues(value: String): List<String> = value
        .split(',', '/', '\n')
        .map(String::trim)
        .filter(String::isNotEmpty)

    private fun linkTexts(element: Element): List<String> = element.select("a")
        .map { it.text().trim() }
        .filter(String::isNotEmpty)

    private fun MutableList<String>.cleanList(): List<String> =
        map(String::trim).filter(String::isNotEmpty).distinct()

    private fun skuFromImageUrl(imageUrl: String): String {
        val path = runCatching { URI(imageUrl).path }.getOrNull().orEmpty().ifBlank { imageUrl }
        val filename = path.substringAfterLast('/').substringBeforeLast('.', path.substringAfterLast('/'))
        return normalizeSku(filename)
    }

    private fun productRecordId(url: String): String? = runCatching { URI(url).rawQuery }
        .getOrNull()
        ?.split('&')
        ?.firstNotNullOfOrNull { parameter ->
            val parts = parameter.split('=', limit = 2)
            parts.getOrNull(1)?.takeIf {
                parts.firstOrNull().equals("pro", ignoreCase = true) && it.all(Char::isDigit)
            }
        }

    private fun detectBlockPage(document: Document) {
        val title = document.title()
        val text = document.body()?.text().orEmpty()
        val blocked = document.location().contains("/cart/verifybot", ignoreCase = true) ||
            document.selectFirst("form[action*=verifybot], .g-recaptcha") != null ||
            text.contains("I am not a robot", ignoreCase = true) ||
            title.contains("Access Denied", ignoreCase = true) ||
            title.contains("Robot Check", ignoreCase = true) ||
            text.contains("captcha", ignoreCase = true) ||
            text.contains("Cloudflare", ignoreCase = true)
        if (blocked) throw FetchException(FetchErrorKind.BLOCKED, "Ave blocked the request")
    }

    private fun canonicalUrl(document: Document, finalUrl: String): String {
        val candidate = document.selectFirst("link[rel=canonical][href]")
            ?.absUrl("href")
            ?.takeIf(String::isNotBlank)
            ?: return finalUrl
        val candidateHost = runCatching { URI(candidate).host }.getOrNull()
        val finalHost = runCatching { URI(finalUrl).host }.getOrNull()
        return candidate.takeIf { candidateHost.equals(finalHost, ignoreCase = true) } ?: finalUrl
    }

    private data class PageData(
        val sku: String?,
        val title: String,
        val description: String?,
        val creators: List<String>,
        val categories: List<String>,
        val canonicalUrl: String,
        val attributes: Map<String, String>,
    )
}
