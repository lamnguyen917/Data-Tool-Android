package com.elp.datatool.data.adapter.amazon

import com.elp.datatool.data.parse.arr
import com.elp.datatool.domain.model.DataSource
import com.elp.datatool.domain.model.FetchErrorKind
import com.elp.datatool.domain.model.FetchException
import com.elp.datatool.domain.model.ProductAttributes
import com.elp.datatool.domain.model.ProductResult
import com.elp.datatool.domain.model.ResultStatus
import com.elp.datatool.domain.model.ValidationResult
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/** Parses Amazon.com book pages. See `.knowledge/pages/amazon-book-crawl.md`. */
object AmazonBookParser {

    private val json = Json { ignoreUnknownKeys = true }
    private val ratingPattern = Regex("""(\d+(?:\.\d+)?)\s+out of 5 stars""", RegexOption.IGNORE_CASE)
    private val ratingCountPattern = Regex("""([\d,]+)\s+(?:global\s+)?ratings?""", RegexOption.IGNORE_CASE)
    private val authorSuffix = Regex("""\s*\([^)]*author[^)]*\)\s*""", RegexOption.IGNORE_CASE)
    private val directionMarks = Regex("[\u200E\u200F]")

    fun parseProduct(html: String, productId: String, normalizedUrl: String): ProductResult {
        val document = Jsoup.parse(html, normalizedUrl)
        detectBlockPage(document, html)

        val title = document.selectFirst("#productTitle")?.text()?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: throw FetchException(FetchErrorKind.PARSE, "Amazon product title not found")
        val details = detailBullets(document)

        return ProductResult(
            source = DataSource.AMAZON_BOOKS,
            productId = productId,
            status = ResultStatus.COMPLETED,
            title = title,
            description = description(document),
            creators = authors(document),
            categories = categories(document),
            canonicalUrl = canonicalUrl(document, normalizedUrl),
            imageUrl = coverImage(document),
            attributes = buildMap {
                putIfPresent(ProductAttributes.PUBLISHER, details.value("Publisher"))
                putIfPresent(ProductAttributes.PUBLICATION_DATE, details.value("Publication date"))
                putIfPresent(ProductAttributes.LANGUAGE, details.value("Language"))
                putIfPresent(ProductAttributes.PRINT_LENGTH, details.value("Print length"))
                putIfPresent(ProductAttributes.ISBN_10, details.value("ISBN-10"))
                putIfPresent(ProductAttributes.ISBN_13, details.value("ISBN-13"))
                putIfPresent(ProductAttributes.RATING, rating(document))
                putIfPresent(ProductAttributes.RATING_COUNT, ratingCount(document))
            },
        )
    }

    private fun detectBlockPage(document: Document, html: String) {
        val pageTitle = document.title()
        val bodyText = document.body()?.text().orEmpty()
        val hasCaptcha = document.selectFirst("form[action*=validateCaptcha], #captchacharacters") != null
        val hasAutomationWarning = bodyText.contains("api-services-support@amazon.com", ignoreCase = true)
        val hasProductTitle = document.selectFirst("#productTitle") != null
        val isSmallPageWithoutProduct = html.length < MIN_PRODUCT_PAGE_SIZE && !hasProductTitle
        if (pageTitle.equals("Robot Check", ignoreCase = true) ||
            hasCaptcha ||
            hasAutomationWarning ||
            isSmallPageWithoutProduct
        ) {
            throw FetchException(FetchErrorKind.BLOCKED, "Amazon blocked the product request")
        }
    }

    private fun detailBullets(document: Document): Map<String, String> = buildMap {
        document.select("#detailBullets_feature_div li").forEach { item ->
            if (item.hasClass("detail-bullets-wrapper") ||
                item.select(".detail-bullets-wrapper").isNotEmpty()
            ) {
                return@forEach
            }
            val text = cleanBulletText(item.text())
            val separator = text.indexOf(':')
            if (separator > 0) {
                val label = text.substring(0, separator).trim()
                val value = text.substring(separator + 1).trim()
                if (label.isNotEmpty() && value.isNotEmpty()) put(label, value)
            }
        }
    }

    private fun cleanBulletText(value: String): String = directionMarks
        .replace(value, "")
        .replace(Regex("""\s+"""), " ")
        .trim()

    private fun authors(document: Document): List<String> {
        val authorContainers = document.select("#bylineInfo .author")
        val values = if (authorContainers.isNotEmpty()) {
            authorContainers.map { author -> author.selectFirst("a")?.text() ?: author.text() }
        } else {
            document.select("#bylineInfo a").map(Element::text)
        }
        return values
            .map { authorSuffix.replace(it, "").replaceFirst(BY_PREFIX, "").trim() }
            .filter {
                it.isNotEmpty() &&
                    !it.equals("Author", ignoreCase = true) &&
                    !it.startsWith("Visit Amazon's", ignoreCase = true)
            }
            .distinct()
    }

    private fun categories(document: Document): List<String> =
        document.select("#wayfinding-breadcrumbs_feature_div a")
            .map { it.text().trim() }
            .filter { it.isNotEmpty() && !it.equals("Books", ignoreCase = true) }
            .distinct()

    private fun description(document: Document): String? =
        document.selectFirst("#bookDescription_feature_div, #productDescription")
            ?.text()
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    private fun canonicalUrl(document: Document, fallback: String): String =
        document.selectFirst("link[rel=canonical][href]")
            ?.let { it.absUrl("href").ifBlank { it.attr("href") } }
            ?.takeIf { DataSource.AMAZON_BOOKS.validate(it) is ValidationResult.Valid }
            ?: fallback

    private fun rating(document: Document): String? {
        val source = document.selectFirst("#acrPopover")?.attr("title")
            ?.takeIf { it.isNotBlank() }
            ?: document.text()
        return ratingPattern.find(source)?.groupValues?.get(1)
    }

    private fun ratingCount(document: Document): String? =
        document.selectFirst("#acrCustomerReviewText")
            ?.text()
            ?.let(ratingCountPattern::find)
            ?.groupValues
            ?.get(1)
            ?.replace(",", "")

    private fun coverImage(document: Document): String? {
        val image = document.selectFirst("#landingImage") ?: return null
        val dynamicImages = image.attr("data-a-dynamic-image")
        val candidates = runCatching { json.parseToJsonElement(dynamicImages) as? JsonObject }
            .getOrNull()
            ?.map { (url, dimensions) ->
                val size = dimensions.arr()?.mapNotNull { it.jsonPrimitive.intOrNull }?.let {
                    it.getOrNull(0).orZero() * it.getOrNull(1).orZero()
                } ?: 0
                url to size
            }
            .orEmpty()
        return candidates.maxByOrNull { it.second }?.first
            ?: image.absUrl("src").ifBlank { image.attr("src") }.takeIf { it.isNotBlank() }
    }

    private fun MutableMap<String, String>.putIfPresent(key: String, value: String?) {
        value?.takeIf { it.isNotBlank() }?.let { put(key, it) }
    }

    private fun Map<String, String>.value(key: String): String? = entries
        .firstOrNull { (label) -> cleanBulletText(label).trimEnd(':').equals(key, ignoreCase = true) }
        ?.value

    private fun Int?.orZero() = this ?: 0

    private val BY_PREFIX = Regex("""^by\s+""", RegexOption.IGNORE_CASE)
    private const val MIN_PRODUCT_PAGE_SIZE = 20 * 1024
}
