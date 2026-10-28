package com.elp.datatool.data.adapter.tokyohot

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
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/** Parses Tokyo-Hot search and bilingual product pages. */
object TokyoHotParser {

    private val productIdPattern = Regex("""Product\s+ID\s*:\s*([A-Za-z0-9_-]+)""", RegexOption.IGNORE_CASE)
    private val classicProductIdPattern = Regex("""[kn]\d{4}""", RegexOption.IGNORE_CASE)

    internal data class Candidate(
        val productId: String,
        val url: String,
        val title: String,
        val imageUrl: String?,
    ) {
        fun toSearchHit() = SearchHit(
            source = DataSource.TOKYO_HOT,
            productId = productId,
            title = title.ifBlank { productId },
            imageUrl = imageUrl,
        )
    }

    fun parseSearch(html: String, pageUrl: String): List<SearchHit> =
        parseCandidates(html, pageUrl).map(Candidate::toSearchHit)

    internal fun parseCandidates(html: String, pageUrl: String): List<Candidate> {
        val document = Jsoup.parse(html, pageUrl)
        detectBlockPage(document)
        return document.select("li.detail")
            .mapNotNull(::candidate)
            .distinctBy { it.url }
    }

    fun parseProduct(html: String, finalUrl: String, requestedProductId: String?): ProductResult {
        val page = parsePage(html, finalUrl)
        val productId = page.productId
            ?: requestedProductId?.let(::normalizeProductId)
            ?: throw FetchException(FetchErrorKind.PARSE, "Tokyo-Hot product ID not found")
        return ProductResult(
            source = DataSource.TOKYO_HOT,
            productId = productId,
            status = ResultStatus.COMPLETED,
            title = page.title,
            description = page.description,
            creators = page.creators,
            categories = page.categories,
            canonicalUrl = canonicalProductUrl(finalUrl),
            imageUrl = page.imageUrl,
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
            canonicalUrl = finalUrl,
            attributes = page.attributes,
        )
    }

    fun normalizeProductId(value: String): String {
        val trimmed = value.trim()
        return if (classicProductIdPattern.matches(trimmed)) {
            trimmed.lowercase(Locale.ROOT)
        } else {
            trimmed
        }
    }

    private fun candidate(card: Element): Candidate? {
        val anchor = card.selectFirst("a[href]") ?: return null
        val url = anchor.absUrl("href").ifBlank { anchor.attr("href") }
        if (url.isBlank()) return null

        val image = anchor.selectFirst("img")
        val actorText = card.selectFirst(".description2 .actor, .actor")?.text().orEmpty()
        val rawProductId = productIdPattern.find(actorText)?.groupValues?.get(1)
            ?: image?.attr("alt")?.trim()?.takeIf(String::isNotBlank)
            ?: image?.attr("title")?.trim()?.takeIf(String::isNotBlank)
            ?: return null
        val productId = normalizeProductId(rawProductId)
        val title = card.selectFirst(".description2 .title, .title")?.text()?.trim().orEmpty()
            .ifBlank { image?.attr("title")?.trim().orEmpty() }
        val imageUrl = image?.let { element ->
            val attribute = when {
                element.attr("src").isNotBlank() -> "src"
                element.attr("data-src").isNotBlank() -> "data-src"
                else -> null
            }
            attribute?.let { absoluteUrl(element, it) }
        }
        return Candidate(productId, url, title, imageUrl)
    }

    private fun parsePage(html: String, finalUrl: String): PageData {
        val document = Jsoup.parse(html, finalUrl)
        detectBlockPage(document)
        val title = document.selectFirst("#main .contents h2, #main .contents h1, .contents h2")
            ?.text()
            ?.trim()
            ?.takeIf(String::isNotEmpty)
            ?: throw FetchException(FetchErrorKind.PARSE, "Tokyo-Hot product title not found")

        var productId: String? = null
        val creators = mutableListOf<String>()
        val categories = mutableListOf<String>()
        val attributes = linkedMapOf<String, String>()
        document.select("div.infowrapper dl.info dt, dl.info dt").forEach { labelElement ->
            val valueElement = labelElement.nextElementSibling() ?: return@forEach
            if (!valueElement.tagName().equals("dd", ignoreCase = true)) return@forEach
            val label = labelElement.text().trim()
            val values = valueElement.select("a")
                .map { it.text().trim() }
                .filter(String::isNotEmpty)
                .distinct()
            val text = values.takeIf { it.isNotEmpty() }?.joinToString(", ")
                ?: valueElement.text().trim()
            when (label) {
                "Model", "出演者" -> creators.replaceWith(values.ifEmpty { splitValues(text) })
                "Play", "プレイ内容" -> categories.replaceWith(values.ifEmpty { splitValues(text) })
                "Tags", "タグ" -> attributes.putIfNotBlank(ProductAttributes.TAGS, text)
                "Theme" -> attributes.putIfNotBlank(ProductAttributes.THEME, text)
                "Series", "シリーズ" -> attributes.putIfNotBlank(ProductAttributes.SERIES, text)
                "Label", "レーベル" -> attributes.putIfNotBlank(ProductAttributes.STUDIO, text)
                "Release", "Release Date", "配信開始日" ->
                    attributes.putIfNotBlank(ProductAttributes.RELEASE_DATE, text)
                "Duration", "収録時間" -> attributes.putIfNotBlank(ProductAttributes.DURATION, text)
                "Product ID", "作品番号" -> productId = text.takeIf(String::isNotBlank)?.let(::normalizeProductId)
            }
        }

        val poster = document.selectFirst("[data-poster]")?.let { absoluteUrl(it, "data-poster") }
            ?.takeIf(String::isNotBlank)
        return PageData(
            productId = productId,
            title = title,
            description = document.selectFirst("#main .contents .sentence, .contents .sentence")
                ?.text()
                ?.trim()
                ?.takeIf(String::isNotEmpty),
            creators = creators.distinct(),
            categories = categories.distinct(),
            imageUrl = poster,
            attributes = attributes,
        )
    }

    private fun detectBlockPage(document: Document) {
        val signal = (document.title() + " " + document.body()?.text().orEmpty()).lowercase(Locale.ROOT)
        if (
            signal.contains("access denied") ||
            signal.contains("cloudflare") ||
            signal.contains("captcha") ||
            signal.contains("verify you are human")
        ) {
            throw FetchException(FetchErrorKind.BLOCKED, "Tokyo-Hot blocked the request")
        }
    }

    private fun absoluteUrl(element: Element, attribute: String): String {
        val raw = element.attr(attribute).trim()
        if (raw.isEmpty()) return ""
        return element.absUrl(attribute).ifBlank {
            runCatching { URI(element.baseUri()).resolve(raw).toString() }.getOrDefault(raw)
        }
    }

    private fun canonicalProductUrl(url: String): String = url.toHttpUrlOrNull()
        ?.newBuilder()
        ?.query(null)
        ?.fragment(null)
        ?.build()
        ?.toString()
        ?: url.substringBefore('?').substringBefore('#')

    private fun MutableList<String>.replaceWith(values: List<String>) {
        clear()
        addAll(values.filter(String::isNotBlank))
    }

    private fun splitValues(value: String): List<String> = value
        .split(',', '、')
        .map(String::trim)
        .filter(String::isNotEmpty)

    private fun MutableMap<String, String>.putIfNotBlank(key: String, value: String) {
        if (value.isNotBlank()) this[key] = value
    }

    private data class PageData(
        val productId: String?,
        val title: String,
        val description: String?,
        val creators: List<String>,
        val categories: List<String>,
        val imageUrl: String?,
        val attributes: Map<String, String>,
    )
}
