package com.elp.datatool.data.adapter.fc2

import com.elp.datatool.data.parse.htmlToPlainText
import com.elp.datatool.data.parse.obj
import com.elp.datatool.data.parse.str
import com.elp.datatool.data.parse.text
import com.elp.datatool.domain.model.DataSource
import com.elp.datatool.domain.model.FetchErrorKind
import com.elp.datatool.domain.model.FetchException
import com.elp.datatool.domain.model.ProductAttributes
import com.elp.datatool.domain.model.ProductResult
import com.elp.datatool.domain.model.ResultStatus
import java.net.URI
import java.util.Locale
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/** Parses FC2 Content Market product and description pages. */
object Fc2Parser {

    private val json = Json { ignoreUnknownKeys = true }
    private val releaseDatePattern = Regex("""\d{4}/\d{1,2}/\d{1,2}""")

    internal data class ParsedPage(
        val result: ProductResult,
        val descriptionUrl: String?,
    )

    internal fun parseProductPage(html: String, finalUrl: String, expectedSku: String): ParsedPage {
        val document = Jsoup.parse(html, finalUrl)
        detectBlockedPage(document)
        val product = productJson(document)
        if (product == null) {
            if (isNotFoundPage(document)) {
                throw FetchException(FetchErrorKind.NOT_FOUND, "FC2 article $expectedSku not found")
            }
            throw FetchException(FetchErrorKind.PARSE, "FC2 Product JSON-LD not found")
        }

        val sku = product.text("sku") ?: product.text("productID") ?: expectedSku
        if (!sku.all(Char::isDigit) || sku != expectedSku) {
            throw FetchException(FetchErrorKind.PARSE, "Unexpected FC2 article ID")
        }
        val title = product.text("name")
            ?: document.selectFirst(".items_article_MainitemThumb img[alt]")?.attr("alt")?.trim()
                ?.takeIf(String::isNotEmpty)
            ?: document.selectFirst(".items_article_MainitemThumb img[title]")?.attr("title")?.trim()
                ?.takeIf(String::isNotEmpty)
            ?: throw FetchException(FetchErrorKind.PARSE, "FC2 product title not found")

        val offers = product.obj("offers")
        val rating = product.obj("aggregateRating")
        val seller = document.selectFirst("[data-article-seller-name]")?.text()?.trim()
            ?.takeIf(String::isNotEmpty)
        val attributes = buildMap {
            putIfPresent(ProductAttributes.STUDIO, seller)
            putIfPresent(ProductAttributes.RELEASE_DATE, releaseDate(document))
            putIfPresent(ProductAttributes.DURATION, duration(document))
            putIfPresent(ProductAttributes.SUPPORTED_DEVICES, supportedDevices(document))
            putIfPresent(ProductAttributes.PRICE, offers?.text("price")?.let { "$it pt" })
            putIfPresent(ProductAttributes.RATING, rating?.text("ratingValue"))
            putIfPresent(ProductAttributes.RATING_COUNT, rating?.text("reviewCount"))
        }

        val result = ProductResult(
            source = DataSource.FC2,
            productId = sku,
            status = ResultStatus.COMPLETED,
            title = title,
            description = product.text("description"),
            creators = listOfNotNull(seller),
            canonicalUrl = canonicalUrl(offers?.text("url"), finalUrl, sku),
            imageUrl = productImage(product) ?: mainImage(document),
            attributes = attributes,
        )
        return ParsedPage(
            result = result,
            descriptionUrl = document.selectFirst("iframe[data-iframe=description][src]")
                ?.let { absoluteUrl(it, "src") }
                ?.takeIf(String::isNotBlank),
        )
    }

    fun parseDescription(html: String, finalUrl: String): String? {
        val document = Jsoup.parse(html, finalUrl)
        val container = document.selectFirst("div[data-container]") ?: return null
        container.select("script, style, link, img, .kJhxnUI, .sample_block, [hidden], [data-id]").remove()
        container.select("[style]").forEach { element ->
            val style = element.attr("style").lowercase(Locale.ROOT).replace(" ", "")
            if (
                style.contains("display:none") ||
                style.contains("opacity:0") ||
                style.contains("height:0") ||
                (style.contains("color:#fff") && style.contains("overflow:hidden"))
            ) {
                element.remove()
            }
        }
        return htmlToPlainText(container.html()).takeIf(String::isNotBlank)
    }

    private fun productJson(document: Document): JsonObject? = document
        .select("script[type=application/ld+json]")
        .firstNotNullOfOrNull { script ->
            runCatching { json.parseToJsonElement(script.data().ifBlank { script.html() }) }
                .getOrNull()
                ?.findProduct()
        }

    private fun JsonElement.findProduct(): JsonObject? = when (this) {
        is JsonArray -> firstNotNullOfOrNull { it.findProduct() }
        is JsonObject -> {
            val types = this["@type"]
            val isProduct = when (types) {
                is JsonArray -> types.any { it.str().equals("Product", ignoreCase = true) }
                else -> types.str().equals("Product", ignoreCase = true)
            }
            if (isProduct) this else this["@graph"]?.findProduct()
        }
        else -> null
    }

    private fun productImage(product: JsonObject): String? = when (val image = product["image"]) {
        is JsonObject -> image.text("url")
        is JsonArray -> image.firstNotNullOfOrNull { item ->
            item.obj()?.text("url") ?: item.str()?.takeIf(String::isNotBlank)
        }
        else -> image.str()?.takeIf(String::isNotBlank)
    }

    private fun mainImage(document: Document): String? = document
        .selectFirst(".items_article_MainitemThumb img")
        ?.let { absoluteUrl(it, "src") }
        ?.takeIf(String::isNotBlank)

    private fun releaseDate(document: Document): String? = document
        .select(".items_article_softDevice p")
        .firstNotNullOfOrNull { element ->
            val text = element.text()
            if (text.contains("Sale Day", ignoreCase = true) || text.contains("販売日")) {
                releaseDatePattern.find(text)?.value
            } else {
                null
            }
        }

    private fun duration(document: Document): String? = document
        .selectFirst(".items_article_MainitemThumb .items_article_info")
        ?.text()
        ?.trim()
        ?.takeIf(String::isNotEmpty)

    private fun supportedDevices(document: Document): String? = document
        .select(".items_article_softDevice")
        .firstOrNull { section ->
            section.selectFirst("h3")?.text()?.let { heading ->
                heading.contains("Supported Devices", ignoreCase = true) || heading.contains("対応デバイス")
            } == true
        }
        ?.select("li")
        ?.map { it.text().trim() }
        ?.filter(String::isNotEmpty)
        ?.distinct()
        ?.joinToString(", ")
        ?.takeIf(String::isNotBlank)

    private fun canonicalUrl(candidate: String?, fallback: String, sku: String): String {
        val candidateUri = candidate?.let { runCatching { URI(it) }.getOrNull() }
        val fallbackUri = runCatching { URI(fallback) }.getOrNull()
        val candidateSku = candidateUri?.path?.let {
            Regex("""/article/(\d+)/?""").matchEntire(it)?.groupValues?.get(1)
        }
        return if (
            candidateUri != null &&
            fallbackUri != null &&
            candidateUri.scheme.equals(fallbackUri.scheme, ignoreCase = true) &&
            candidateUri.host.equals(fallbackUri.host, ignoreCase = true) &&
            candidateSku == sku
        ) {
            candidate
        } else {
            fallback.substringBefore('?').substringBefore('#')
        }
    }

    private fun detectBlockedPage(document: Document) {
        val title = document.title()
        val hasLoginForm = document.selectFirst("form[name=form_login]") != null
        val hasCaptcha = document.selectFirst("script[src*=recaptcha], [class*=captcha]") != null
        if (title.contains("Log in FC2", ignoreCase = true) || hasLoginForm || hasCaptcha) {
            throw FetchException(FetchErrorKind.BLOCKED, "FC2 blocked the product request")
        }
    }

    private fun isNotFoundPage(document: Document): Boolean {
        val title = document.title()
        val mainHeading = document.selectFirst("main h1, main h2, #main h1, #main h2")?.text().orEmpty()
        return title.contains("not found", ignoreCase = true) ||
            title.contains("見つかりませんでした") ||
            mainHeading.contains("product was not found", ignoreCase = true) ||
            mainHeading.contains("商品が見つかりませんでした")
    }

    private fun absoluteUrl(element: Element, attribute: String): String {
        val raw = element.attr(attribute).trim()
        if (raw.isEmpty()) return ""
        return element.absUrl(attribute).ifBlank {
            runCatching { URI(element.baseUri()).resolve(raw).toString() }.getOrDefault(raw)
        }
    }

    private fun MutableMap<String, String>.putIfPresent(key: String, value: String?) {
        value?.takeIf(String::isNotBlank)?.let { put(key, it) }
    }
}
