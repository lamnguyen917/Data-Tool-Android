package com.elp.datatool.data.adapter.onepondo

import com.elp.datatool.data.parse.arr
import com.elp.datatool.data.parse.obj
import com.elp.datatool.data.parse.str
import com.elp.datatool.data.parse.text
import com.elp.datatool.domain.model.DataSource
import com.elp.datatool.domain.model.FetchErrorKind
import com.elp.datatool.domain.model.FetchException
import com.elp.datatool.domain.model.LocalizedProductMetadata
import com.elp.datatool.domain.model.ProductAttributes
import com.elp.datatool.domain.model.ProductResult
import com.elp.datatool.domain.model.ResultStatus
import java.net.URI
import java.util.Locale
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/** Parses the public 1pondo movie-details JSON response. */
object OnePondoParser {

    private val json = Json { ignoreUnknownKeys = true }

    fun parseProduct(body: String, expectedSku: String, canonicalUrl: String): ProductResult {
        detectBlockedDocument(body)
        val root = runCatching { json.parseToJsonElement(body).obj() }.getOrNull()
            ?: throw FetchException(FetchErrorKind.PARSE, "1pondo response is not a JSON object")

        val movieId = root.cleanText("MovieID")
            ?: throw FetchException(FetchErrorKind.PARSE, "1pondo movie id is missing")
        if (movieId != expectedSku) {
            throw FetchException(FetchErrorKind.PARSE, "1pondo movie id mismatch")
        }
        if (root["Status"].str()?.toBooleanStrictOrNull() == false) {
            throw FetchException(FetchErrorKind.NOT_FOUND, "1pondo product is inactive")
        }

        val japaneseTitle = root.cleanText("Title")
        val title = root.cleanText("TitleEn") ?: japaneseTitle
            ?: throw FetchException(FetchErrorKind.PARSE, "1pondo product title is missing")
        val japaneseDescription = root.cleanText("Desc")
        val description = root.cleanText("DescEn") ?: japaneseDescription
        val japaneseCreators = root.performers("ActressesJa", "NameJa")
        val creators = root.performers("ActressesEn", "NameEn").ifEmpty { japaneseCreators }
        val japaneseCategories = root.categoryNames("NameJa", "UCNAME")
        val categories = root.categoryNames(
            primaryNameKey = "NameEn",
            primaryArrayKey = "UCNAMEEn",
            fallbackNameKey = "NameJa",
            fallbackArrayKey = "UCNAME",
        ).ifEmpty { japaneseCategories }
        val releaseDate = root.cleanText("Release")?.takeIf(::isValidIsoDate)
        val duration = root["Duration"].str()
            ?.toLongOrNull()
            ?.takeIf { it >= 0 }
            ?.let(::formatDuration)
        val japaneseSeries = root.cleanText("Series")
        val series = root.cleanText("SeriesEn") ?: japaneseSeries
        val rating = root["AvgRating"].str()
            ?.trim()
            ?.takeIf { value -> value.toDoubleOrNull()?.let { it.isFinite() && it >= 0.0 } == true }
        val imageUrl = IMAGE_FIELDS.firstNotNullOfOrNull { field ->
            root.cleanText(field)?.takeIf(::isTrustedImageUrl)
        }
        val sharedAttributes = buildMap {
            put(ProductAttributes.STUDIO, STUDIO)
            releaseDate?.let { put(ProductAttributes.RELEASE_DATE, it) }
            duration?.let { put(ProductAttributes.DURATION, it) }
        }
        val attributes = buildMap {
            putAll(sharedAttributes)
            series?.let { put(ProductAttributes.SERIES, it) }
            rating?.let { put(ProductAttributes.RATING, it) }
        }
        val japaneseAttributes = buildMap {
            putAll(sharedAttributes)
            japaneseSeries?.let { put(ProductAttributes.SERIES, it) }
        }
        val localized = LocalizedProductMetadata(
            title = japaneseTitle,
            description = japaneseDescription,
            creators = japaneseCreators,
            categories = japaneseCategories,
            canonicalUrl = canonicalUrl,
            attributes = japaneseAttributes,
        ).takeIf { metadata ->
            metadata.title != null || metadata.description != null ||
                metadata.creators.isNotEmpty() || metadata.categories.isNotEmpty() ||
                japaneseSeries != null
        }

        return ProductResult(
            source = DataSource.ONE_PONDO,
            productId = movieId,
            status = ResultStatus.COMPLETED,
            title = title,
            description = description,
            creators = creators,
            categories = categories,
            canonicalUrl = canonicalUrl,
            imageUrl = imageUrl,
            attributes = attributes,
            localized = localized?.let { mapOf(JAPANESE_LANGUAGE to it) }.orEmpty(),
        )
    }

    private fun JsonObject.performers(arrayKey: String, objectNameKey: String): List<String> =
        stringArray(arrayKey).ifEmpty {
            obj("ActressesList")
                ?.values
                ?.mapNotNull { actress -> actress.obj()?.cleanText(objectNameKey) }
                ?.cleanList()
                .orEmpty()
        }

    private fun JsonObject.categoryNames(
        primaryNameKey: String,
        primaryArrayKey: String,
        fallbackNameKey: String? = null,
        fallbackArrayKey: String? = null,
    ): List<String> {
        val ids = arr("UC")?.mapNotNull { it.str()?.trim()?.takeIf(String::isNotEmpty) }.orEmpty()
        val namesById = obj("UcNameList")
        val primaryArray = nullableStringArray(primaryArrayKey)
        val fallbackArray = fallbackArrayKey?.let { nullableStringArray(it) }.orEmpty()
        val ordered = ids.mapIndexedNotNull { index, id ->
            val entry = namesById?.obj(id)
            entry?.cleanText(primaryNameKey)
                ?: primaryArray.getOrNull(index)
                ?: fallbackNameKey?.let { entry?.cleanText(it) }
                ?: fallbackArray.getOrNull(index)
        }.cleanList()
        val primaryNames = primaryArray.filterNotNull().cleanList()
        val fallbackNames = fallbackArray.filterNotNull().cleanList()
        return ordered.ifEmpty { primaryNames.ifEmpty { fallbackNames } }
    }

    private fun JsonObject.stringArray(name: String): List<String> = arr(name)
        ?.mapNotNull { it.str()?.normalizeListValue() }
        ?.cleanList()
        .orEmpty()

    private fun JsonObject.nullableStringArray(name: String): List<String?> = arr(name)
        ?.map { it.str()?.normalizeListValue() }
        .orEmpty()

    private fun JsonObject.cleanText(name: String): String? = text(name)?.trim()?.takeIf(String::isNotEmpty)

    private fun List<String>.cleanList(): List<String> =
        mapNotNull { it.normalizeListValue() }.distinct()

    private fun String.normalizeListValue(): String? = trim()
        .replace(WHITESPACE, " ")
        .takeIf(String::isNotEmpty)

    private fun isTrustedImageUrl(value: String): Boolean {
        val uri = runCatching { URI(value) }.getOrNull() ?: return false
        return uri.scheme.equals("https", ignoreCase = true) &&
            uri.host.equals(IMAGE_HOST, ignoreCase = true) &&
            uri.rawUserInfo == null &&
            (uri.port == -1 || uri.port == 443) &&
            uri.rawQuery == null &&
            uri.rawFragment == null
    }

    private fun isValidIsoDate(value: String): Boolean {
        val match = ISO_DATE.matchEntire(value) ?: return false
        val year = match.groupValues[1].toInt()
        if (year < 1) return false
        val month = match.groupValues[2].toInt()
        val day = match.groupValues[3].toInt()
        val maxDay = when (month) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            4, 6, 9, 11 -> 30
            2 -> if (year % 400 == 0 || year % 4 == 0 && year % 100 != 0) 29 else 28
            else -> return false
        }
        return day in 1..maxDay
    }

    private fun formatDuration(seconds: Long): String = String.format(
        Locale.US,
        "%02d:%02d:%02d",
        seconds / 3600,
        seconds % 3600 / 60,
        seconds % 60,
    )

    private fun detectBlockedDocument(body: String) {
        val trimmed = body.trimStart()
        if (!trimmed.startsWith("<")) return
        val signal = trimmed.lowercase(Locale.ROOT)
        if (BLOCK_SIGNALS.any(signal::contains)) {
            throw FetchException(FetchErrorKind.BLOCKED, "1pondo blocked the metadata request")
        }
    }

    private const val STUDIO = "1pondo"
    private const val IMAGE_HOST = "www.1pondo.tv"
    private const val JAPANESE_LANGUAGE = "ja"
    private val IMAGE_FIELDS = listOf("ThumbHigh", "MovieThumb", "ThumbMed", "ThumbUltra", "ThumbLow")
    private val WHITESPACE = Regex("""\s+""")
    private val ISO_DATE = Regex("""(\d{4})-(\d{2})-(\d{2})""")
    private val BLOCK_SIGNALS = listOf(
        "access denied",
        "attention required",
        "captcha",
        "cf-chl",
        "challenge-form",
        "challenges.cloudflare.com",
        "cloudflare",
        "just a moment",
        "verify you are human",
    )
}
