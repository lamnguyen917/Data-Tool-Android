package com.elp.datatool.data.adapter.unity

import com.elp.datatool.data.parse.arr
import com.elp.datatool.data.parse.findJsonEnd
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
import com.elp.datatool.domain.model.SearchHit
import java.util.Locale
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * Parses Unity Asset Store pages. See `.knowledge/pages/unity-asset-store-crawl.md`.
 *
 * Product pages embed a normalized entity cache passed to `ReactDOMrender({...})`
 * (`data.ENTITY.Product[id]`, with references to `ProductPublisher`, `Category`, ...),
 * plus `application/ld+json` Product and BreadcrumbList blocks used as a fallback.
 *
 * The search page is server-rendered with Next.js; its RSC payload
 * (`self.__next_f.push([1, "..."])`) contains the Coveo response with a `results` array.
 */
object UnityAssetStoreParser {

    private val json = Json { ignoreUnknownKeys = true }

    private const val STATE_CALL = "ReactDOMrender("
    private const val RSC_PREFIX = "self.__next_f.push([1,"
    private const val PRODUCT_URI_PREFIX = "assetstore://product/"
    private const val MAX_TAGS = 10

    fun parseProduct(html: String, finalUrl: String, productId: String): ProductResult {
        val document = Jsoup.parse(html, finalUrl)
        val entities = findEntities(document)
        val product = entities?.obj("Product")?.obj(productId)
        val ldProduct = findLdJson(document, "Product")
        val canonicalUrl = canonicalUrl(document, ldProduct, finalUrl)

        if (product == null && ldProduct == null) {
            // Unknown ids redirect to the store home page instead of returning 404.
            val isPackagePage = canonicalUrl.contains("/packages/") && finalUrl.contains(productId)
            throw FetchException(
                if (isPackagePage) FetchErrorKind.PARSE else FetchErrorKind.NOT_FOUND,
                "Package $productId not found at $finalUrl",
            )
        }

        val publisher = entities.resolve(product?.get("publisher").obj())
        val category = entities.resolve(product?.get("category").obj())
        val version = entities.resolve(product?.get("currentVersion").obj())

        val title = product?.text("name") ?: ldProduct?.text("name")
        val description = product?.text("description")?.let(::htmlToPlainText)?.ifBlank { null }
            ?: product?.text("elevatorPitch")?.let(::htmlToPlainText)?.ifBlank { null }
            ?: ldProduct?.text("description")?.let(::htmlToPlainText)
        val creators = listOfNotNull(
            publisher?.text("name") ?: ldProduct?.obj("brand")?.text("name")
        )
        val categories = category?.text("longName")
            ?.split('/')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: breadcrumbCategories(document)
        val imageUrl = product?.obj("mainImage")?.let { it.text("big") ?: it.text("small") }
            ?: ldProduct?.get("image")?.arr()?.firstOrNull()?.str()

        return ProductResult(
            source = DataSource.UNITY_ASSET_STORE,
            productId = productId,
            status = ResultStatus.COMPLETED,
            title = title,
            description = description,
            creators = creators,
            categories = categories,
            canonicalUrl = canonicalUrl,
            imageUrl = imageUrl?.let(::absoluteUrl),
            attributes = attributes(product, version, entities, ldProduct),
        )
    }

    fun parseSearch(html: String): List<SearchHit> {
        val document = Jsoup.parse(html)
        val payload = buildString {
            document.select("script").forEach { script ->
                val data = script.data().trim()
                if (data.startsWith(RSC_PREFIX) && data.endsWith("])")) {
                    val literal = data.substring(RSC_PREFIX.length, data.length - 2)
                    append(json.parseToJsonElement(literal).jsonPrimitive.content)
                }
            }
        }
        if (payload.isEmpty()) throw FetchException(FetchErrorKind.PARSE, "Search payload not found")

        // The payload may contain several "results" arrays; use the first one with products.
        var index = payload.indexOf("\"results\":[")
        while (index >= 0) {
            val start = index + "\"results\":".length
            val end = findJsonEnd(payload, start)
            if (end < 0) break
            val hits = runCatching { json.parseToJsonElement(payload.substring(start, end)).arr() }
                .getOrNull()
                ?.mapNotNull { it.obj()?.toSearchHit() }
                .orEmpty()
            if (hits.isNotEmpty()) return hits.distinctBy { it.productId }
            index = payload.indexOf("\"results\":[", end)
        }
        return emptyList()
    }

    private fun JsonObject.toSearchHit(): SearchHit? {
        val raw = obj("raw")
        val uri = text("uri") ?: raw?.text("uri") ?: return null
        if (!uri.startsWith(PRODUCT_URI_PREFIX)) return null
        val id = raw?.text("permanentid") ?: uri.removePrefix(PRODUCT_URI_PREFIX)
        if (id.isEmpty() || !id.all(Char::isDigit)) return null
        return SearchHit(
            source = DataSource.UNITY_ASSET_STORE,
            productId = id,
            title = raw?.text("ec_name") ?: text("title") ?: id,
            publisher = raw?.text("publisher_name"),
            imageUrl = raw?.text("ec_thumbnails")?.let(::absoluteUrl),
        )
    }

    /** Finds `data.ENTITY` in the script that renders the product detail component. */
    private fun findEntities(document: Document): JsonObject? {
        for (script in document.select("script")) {
            val data = script.data()
            val callIndex = data.indexOf(STATE_CALL)
            if (callIndex < 0 || !data.contains("\"Product\":{")) continue
            val start = callIndex + STATE_CALL.length
            val end = findJsonEnd(data, start)
            if (end < 0) continue
            val root = runCatching { json.parseToJsonElement(data.substring(start, end)).obj() }.getOrNull()
            root?.obj("data")?.obj("ENTITY")?.let { return it }
        }
        return null
    }

    /** Resolves `{"type":"id","id":["Type","123"]}` references against the entity cache. */
    private fun JsonObject?.resolve(reference: JsonObject?): JsonObject? {
        if (this == null || reference == null) return null
        val id = reference.arr("id") ?: return reference
        val type = id.getOrNull(0).str() ?: return null
        val key = id.getOrNull(1).str() ?: return null
        return obj(type)?.obj(key)
    }

    private fun findLdJson(document: Document, type: String): JsonObject? =
        document.select("script[type=application/ld+json]").firstNotNullOfOrNull { script ->
            runCatching { json.parseToJsonElement(script.data()).obj() }.getOrNull()
                ?.takeIf { it.text("@type") == type }
        }

    private fun canonicalUrl(document: Document, ldProduct: JsonObject?, finalUrl: String): String =
        document.selectFirst("meta[property=og:url]")?.attr("content")?.takeIf { it.isNotBlank() }
            ?: ldProduct?.obj("offers")?.text("url")
            ?: finalUrl

    /** Breadcrumb names without "Home" and without the last crumb (the product itself). */
    private fun breadcrumbCategories(document: Document): List<String> {
        val items = findLdJson(document, "BreadcrumbList")?.arr("itemListElement") ?: return emptyList()
        val names = items.mapNotNull { it.obj()?.text("name") }.map { Jsoup.parse(it).text() }
        return names.drop(1).dropLast(1)
    }

    private fun attributes(
        product: JsonObject?,
        version: JsonObject?,
        entities: JsonObject?,
        ldProduct: JsonObject?,
    ): Map<String, String> = buildMap {
        val price = product?.obj("originalPrice")
        if (price?.get("isFree").str() == "true") {
            put(ProductAttributes.IS_FREE, "true")
        } else {
            val amount = price?.text("finalPrice") ?: ldProduct?.obj("offers")?.text("price")
            val currency = price?.text("currency") ?: ldProduct?.obj("offers")?.text("priceCurrency")
            if (amount != null) put(ProductAttributes.PRICE, listOfNotNull(amount, currency).joinToString(" "))
        }

        val rating = product?.obj("rating")
        val average = rating?.text("average")?.toDoubleOrNull()
        val count = rating?.text("count")?.toIntOrNull() ?: 0
        if (average != null && count > 0) {
            put(ProductAttributes.RATING, String.format(Locale.US, "%.1f", average))
            put(ProductAttributes.RATING_COUNT, count.toString())
        }

        version?.text("name")?.let { put(ProductAttributes.VERSION, it) }
        version?.text("publishedDate")?.let { put(ProductAttributes.LATEST_RELEASE, it.take(10)) }
        product?.text("firstPublishedDate")?.let { put(ProductAttributes.FIRST_PUBLISHED, it.take(10)) }
        product?.arr("supportedUnityVersions")
            ?.mapNotNull { it.str() }
            ?.takeIf { it.isNotEmpty() }
            ?.let { put(ProductAttributes.UNITY_VERSIONS, it.joinToString(", ")) }
        product?.text("downloadSize")?.toLongOrNull()?.takeIf { it > 0 }
            ?.let { put(ProductAttributes.FILE_SIZE, formatBytes(it)) }
        product?.text("assetCount")?.toIntOrNull()?.takeIf { it > 0 }
            ?.let { put(ProductAttributes.ASSET_COUNT, it.toString()) }
        product?.arr("popularTags")
            ?.mapNotNull { entities.resolve(it.obj())?.text("name") }
            ?.take(MAX_TAGS)
            ?.takeIf { it.isNotEmpty() }
            ?.let { put(ProductAttributes.TAGS, it.joinToString(", ")) }
    }

    private fun absoluteUrl(url: String): String = if (url.startsWith("//")) "https:$url" else url

    private fun formatBytes(bytes: Long): String {
        val units = listOf("B", "KB", "MB", "GB")
        var value = bytes.toDouble()
        var unit = 0
        while (value >= 1024 && unit < units.lastIndex) {
            value /= 1024
            unit++
        }
        return if (unit == 0) "$bytes B" else String.format(Locale.US, "%.1f %s", value, units[unit])
    }
}
