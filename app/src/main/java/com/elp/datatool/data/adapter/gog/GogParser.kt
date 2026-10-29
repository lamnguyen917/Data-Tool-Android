package com.elp.datatool.data.adapter.gog

import com.elp.datatool.data.parse.arr
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

/** Parses the public GOG Catalog and Product API responses. */
object GogParser {

    private val json = Json { ignoreUnknownKeys = true }

    fun parseCatalog(body: String): List<GogCatalogProduct> {
        val root = parseObject(body, "GOG catalog")
        val products = root.arr("products")
            ?: throw FetchException(FetchErrorKind.PARSE, "GOG catalog products are missing")
        return products.mapNotNull { element -> element.obj()?.toCatalogProduct() }
    }

    fun parseSearch(body: String): List<SearchHit> = parseCatalog(body)
        .map { product ->
            SearchHit(
                source = DataSource.GOG,
                productId = product.id,
                title = product.title,
                publisher = product.publishers.firstOrNull(),
                imageUrl = product.coverUrl,
            )
        }
        .distinctBy(SearchHit::productId)

    fun parseProduct(body: String, expectedId: String? = null): GogProductDetails {
        val root = parseObject(body, "GOG product")
        val id = root.text("id")
            ?: throw FetchException(FetchErrorKind.PARSE, "GOG product id is missing")
        if (expectedId != null && id != expectedId) {
            throw FetchException(FetchErrorKind.PARSE, "GOG product id mismatch")
        }
        val slug = root.text("slug")
            ?: throw FetchException(FetchErrorKind.PARSE, "GOG product slug is missing")
        val title = root.text("title")
            ?: throw FetchException(FetchErrorKind.PARSE, "GOG product title is missing")
        val description = root.obj("description")
            ?.let { it.text("full") ?: it.text("lead") }
            ?.let(::htmlToPlainText)
            ?.ifBlank { null }
        val languages = root.obj("languages")
            ?.values
            ?.mapNotNull { it.str() }
            ?.distinct()
            .orEmpty()
        val compatibleSystems = root.obj("content_system_compatibility")
            ?.entries
            ?.filter { (_, supported) -> supported.str()?.toBooleanStrictOrNull() == true }
            ?.map { (system, _) -> system.toDisplaySystem() }
            .orEmpty()
        val images = root.obj("images")
        val imageUrl = listOf("background", "logo", "icon")
            .firstNotNullOfOrNull { key -> images?.text(key) }
            ?.let(::absoluteUrl)

        return GogProductDetails(
            id = id,
            slug = slug,
            title = title,
            description = description,
            languages = languages,
            compatibleSystems = compatibleSystems,
            storeReleaseDate = normalizeDate(root.text("release_date")),
            imageUrl = imageUrl,
        )
    }

    fun merge(catalog: GogCatalogProduct, details: GogProductDetails): ProductResult {
        if (catalog.id != details.id || !catalog.slug.equals(details.slug, ignoreCase = true)) {
            throw FetchException(FetchErrorKind.PARSE, "GOG catalog and product identities do not match")
        }
        val platforms = catalog.operatingSystems.ifEmpty { details.compatibleSystems }
        return ProductResult(
            source = DataSource.GOG,
            productId = catalog.id,
            status = ResultStatus.COMPLETED,
            title = catalog.title,
            description = details.description,
            creators = catalog.developers,
            categories = catalog.genres,
            canonicalUrl = "https://www.gog.com/en/game/${catalog.slug}",
            imageUrl = catalog.coverUrl ?: details.imageUrl,
            attributes = buildMap {
                catalog.price?.let { put(ProductAttributes.PRICE, it) }
                if (catalog.isFree) put(ProductAttributes.IS_FREE, "true")
                catalog.rating?.let { put(ProductAttributes.RATING, it) }
                catalog.ratingCount?.let { put(ProductAttributes.RATING_COUNT, it) }
                catalog.publishers.takeIf { it.isNotEmpty() }
                    ?.let { put(ProductAttributes.PUBLISHER, it.joinToString(", ")) }
                catalog.originalReleaseDate
                    ?.let { put(ProductAttributes.PUBLICATION_DATE, it) }
                (catalog.storeReleaseDate ?: details.storeReleaseDate)
                    ?.let { put(ProductAttributes.GOG_RELEASE_DATE, it) }
                details.languages.takeIf { it.isNotEmpty() }
                    ?.let { put(ProductAttributes.LANGUAGE, it.joinToString(", ")) }
                platforms.takeIf { it.isNotEmpty() }
                    ?.let { put(ProductAttributes.PLATFORMS, it.joinToString(", ")) }
                catalog.features.takeIf { it.isNotEmpty() }
                    ?.let { put(ProductAttributes.FEATURES, it.joinToString(", ")) }
                catalog.tags.takeIf { it.isNotEmpty() }
                    ?.let { put(ProductAttributes.TAGS, it.joinToString(", ")) }
            },
        )
    }

    private fun JsonObject.toCatalogProduct(): GogCatalogProduct? {
        val id = text("id") ?: return null
        val slug = text("slug") ?: return null
        val title = text("title") ?: return null
        val finalMoney = obj("price")?.obj("finalMoney")
        val amount = finalMoney?.text("amount")
        val currency = finalMoney?.text("currency")
        val numericAmount = amount?.toDoubleOrNull()
        val encodedRating = text("reviewsRating")?.toDoubleOrNull()
        val rating = encodedRating
            ?.let { if (it > 5.0) it / 10.0 else it }
            ?.let { String.format(Locale.US, "%.1f", it) }

        return GogCatalogProduct(
            id = id,
            slug = slug,
            title = title,
            developers = stringArray("developers"),
            publishers = stringArray("publishers"),
            originalReleaseDate = normalizeDate(text("releaseDate")),
            storeReleaseDate = normalizeDate(text("storeReleaseDate")),
            operatingSystems = stringArray("operatingSystems").map { it.toDisplaySystem() },
            features = objectNames("features"),
            genres = objectNames("genres"),
            tags = objectNames("tags"),
            coverUrl = (text("coverVertical") ?: text("coverHorizontal"))?.let(::absoluteUrl),
            price = if (amount != null && currency != null && numericAmount != 0.0) "$amount $currency" else null,
            isFree = numericAmount == 0.0,
            rating = rating,
            ratingCount = text("reviewsCount"),
        )
    }

    private fun JsonObject.stringArray(name: String): List<String> =
        arr(name)?.mapNotNull { it.str() }?.filter(String::isNotBlank).orEmpty()

    private fun JsonObject.objectNames(name: String): List<String> =
        arr(name)?.mapNotNull { it.obj()?.text("name") }?.distinct().orEmpty()

    private fun parseObject(body: String, label: String): JsonObject = runCatching {
        json.parseToJsonElement(body).obj()
    }.getOrNull() ?: throw FetchException(FetchErrorKind.PARSE, "$label response is not a JSON object")

    private fun normalizeDate(value: String?): String? {
        if (value == null) return null
        val date = value.take(10).replace('.', '-')
        return date.takeIf { DATE.matches(it) }
    }

    private fun String.toDisplaySystem(): String = when (lowercase()) {
        "windows" -> "Windows"
        "osx", "mac", "macos" -> "macOS"
        "linux" -> "Linux"
        else -> replaceFirstChar { it.titlecase() }
    }

    private fun absoluteUrl(url: String): String = when {
        url.startsWith("//") -> "https:$url"
        else -> url
    }

    private val DATE = Regex("""\d{4}-\d{2}-\d{2}""")
}

data class GogCatalogProduct(
    val id: String,
    val slug: String,
    val title: String,
    val developers: List<String>,
    val publishers: List<String>,
    val originalReleaseDate: String?,
    val storeReleaseDate: String?,
    val operatingSystems: List<String>,
    val features: List<String>,
    val genres: List<String>,
    val tags: List<String>,
    val coverUrl: String?,
    val price: String?,
    val isFree: Boolean,
    val rating: String?,
    val ratingCount: String?,
)

data class GogProductDetails(
    val id: String,
    val slug: String,
    val title: String,
    val description: String?,
    val languages: List<String>,
    val compatibleSystems: List<String>,
    val storeReleaseDate: String?,
    val imageUrl: String?,
)
