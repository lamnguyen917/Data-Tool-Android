package com.elp.datatool.domain.model

import java.net.URI
import java.util.Locale

/** A website the app can collect product metadata from. */
enum class DataSource(val id: String) {
    UNITY_ASSET_STORE("unity"),
    AMAZON_BOOKS("amazon"),
    AVE("ave"),
    TOKYO_HOT("tokyo-hot"),
    FC2("fc2"),
    HEYZO("heyzo"),
    CARIBBEANCOM("caribbeancom"),
    STEAM_STORE("steam"),
    IGDB("igdb"),
    GOG("gog");

    /** Normalizes one input line (ID or product URL) into a product ID. */
    fun validate(input: String): ValidationResult {
        val value = input.trim()
        if (value.isEmpty()) return ValidationResult.Invalid(InvalidReason.EMPTY)
        val productId = when (this) {
            UNITY_ASSET_STORE -> DIGITS.matchEntire(value)?.value
                ?: UNITY_URL.find(value)?.groupValues?.get(1)

            STEAM_STORE -> DIGITS.matchEntire(value)?.value
                ?: STEAM_URL.find(value)?.groupValues?.get(1)

            AMAZON_BOOKS -> amazonProductId(value)

            AVE -> aveProductId(value)

            TOKYO_HOT -> tokyoHotProductId(value)

            FC2 -> fc2ProductId(value)

            HEYZO -> heyzoProductId(value)

            CARIBBEANCOM -> caribbeancomProductId(value)

            IGDB -> DIGITS.matchEntire(value)?.value
                ?: IGDB_URL.find(value)?.groupValues?.get(1)
                ?: IGDB_SLUG.matchEntire(value)?.value

            GOG -> gogProductId(value)
        }
        return if (productId != null) {
            ValidationResult.Valid(productId)
        } else {
            ValidationResult.Invalid(InvalidReason.BAD_FORMAT)
        }
    }

    private companion object {
        val DIGITS = Regex("""\d+""")
        val ASIN = Regex("""(?:\d{9}[0-9X]|B[A-Z0-9]{9})""")
        val IGDB_SLUG = Regex("""[a-z0-9]+(?:-[a-z0-9]+)*""")
        val AVE_SKU = Regex("""[A-Z0-9_]{2,}-?[A-Z0-9_]?\d{2,5}""")
        val TOKYO_HOT_ID = Regex("""[A-Za-z0-9][A-Za-z0-9_-]*""")
        val FC2_ALIAS = Regex("""FC2[-\s]?PPV[-\s]?(\d+)""", RegexOption.IGNORE_CASE)
        val HEYZO_SKU = Regex("""\d{4}""")
        val HEYZO_PRODUCT_PATH = Regex("""/moviepages/(\d{4})/index\.html""")
        val CARIBBEANCOM_SKU = Regex("""\d{6}-\d{3}""")
        val CARIBBEANCOM_JA_PATH = Regex("""/moviepages/(\d{6}-\d{3})/(?:index\.html)?""")
        val CARIBBEANCOM_EN_PATH = Regex("""/eng/moviepages/(\d{6}-\d{3})/(?:index\.html)?""")
        val GOG_SLUG = Regex("""[a-z0-9]+(?:[_-][a-z0-9]+)*""")
        val GOG_LOCALE = Regex("""[a-z]{2}(?:-[a-z]{2})?""", RegexOption.IGNORE_CASE)
        val UNITY_URL = Regex("""assetstore\.unity\.com/packages/\S*?-(\d+)(?:[/?#]|$)""")
        val STEAM_URL = Regex("""store\.steampowered\.com/app/(\d+)""")
        val AMAZON_URL = Regex(
            """(?:^|https?://)(?:www\.)?amazon\.(?:com(?:\.[a-z]{2})?|co\.[a-z]{2}|[a-z]{2,3})/""" +
                """(?:\S*/)?(?:dp|gp/product)/([A-Z0-9]{10})""",
            RegexOption.IGNORE_CASE,
        )
        val AMAZON_PATH = Regex("""(?:^|/)(?:dp|gp/product)/([A-Za-z0-9]{10})(?:[/?#]|$)""")
        val IGDB_URL = Regex("""igdb\.com/games/([a-z0-9-]+)""")

        fun aveProductId(value: String): String? {
            val normalizedSku = value
                .removePrefixIgnoreCase("dvd1")
                .uppercase()
                .takeIf(AVE_SKU::matches)
            if (normalizedSku != null) return normalizedSku

            val uri = runCatching { java.net.URI(value) }.getOrNull() ?: return null
            val trustedHost = uri.host?.lowercase() in setOf(
                "aventertainments.com",
                "www.aventertainments.com",
            )
            val hasProductId = uri.rawQuery
                ?.split('&')
                ?.any { parameter ->
                    val parts = parameter.split('=', limit = 2)
                    parts.size == 2 && parts[0].equals("pro", ignoreCase = true) &&
                        parts[1].all(Char::isDigit)
                } == true
            return value.takeIf {
                uri.scheme.equals("https", ignoreCase = true) &&
                    trustedHost &&
                    uri.path.equals("/dvd/detail", ignoreCase = true) &&
                    hasProductId
            }
        }

        fun tokyoHotProductId(value: String): String? {
            if (TOKYO_HOT_ID.matches(value)) return value.lowercaseIfClassicTokyoHotId()

            val uri = runCatching { java.net.URI(value) }.getOrNull() ?: return null
            val trustedHost = uri.host?.lowercase() in setOf("tokyo-hot.com", "www.tokyo-hot.com")
            val hasProductPath = Regex("""/product/[A-Za-z0-9_-]+/?""")
                .matches(uri.path.orEmpty())
            return value.takeIf {
                uri.scheme.equals("https", ignoreCase = true) && trustedHost && hasProductPath
            }
        }

        fun String.lowercaseIfClassicTokyoHotId(): String =
            if (Regex("""[kKnN]\d{4}""").matches(this)) lowercase(Locale.ROOT) else this

        fun fc2ProductId(value: String): String? {
            DIGITS.matchEntire(value)?.value?.let { return it }
            FC2_ALIAS.matchEntire(value)?.groupValues?.get(1)?.let { return it }

            val uri = runCatching { java.net.URI(value) }.getOrNull() ?: return null
            val trustedHost = uri.host.equals("adult.contents.fc2.com", ignoreCase = true)
            val productId = Regex("""/article/(\d+)/?""")
                .matchEntire(uri.path.orEmpty())
                ?.groupValues
                ?.get(1)
            return productId?.takeIf {
                uri.scheme.equals("https", ignoreCase = true) && trustedHost
            }
        }

        fun heyzoProductId(value: String): String? {
            HEYZO_SKU.matchEntire(value)?.value?.let { return it }

            val uri = runCatching { java.net.URI(value) }.getOrNull() ?: return null
            val trustedHost = uri.host?.lowercase() in setOf("www.heyzo.com", "en.heyzo.com")
            val productId = HEYZO_PRODUCT_PATH
                .matchEntire(uri.path.orEmpty())
                ?.groupValues
                ?.get(1)
            return productId?.takeIf {
                uri.scheme.equals("https", ignoreCase = true) &&
                    trustedHost &&
                    uri.userInfo == null &&
                    (uri.port == -1 || uri.port == 443) &&
                    uri.rawQuery == null &&
                    uri.rawFragment == null
            }
        }

        /** Accepts `MMDDYY-NNN` or a canonical English or Japanese Caribbeancom movie URL. */
        fun caribbeancomProductId(value: String): String? {
            CARIBBEANCOM_SKU.matchEntire(value)?.value?.let { return it }

            val uri = runCatching { URI(value) }.getOrNull() ?: return null
            val pathPattern = when (uri.host?.lowercase()) {
                "www.caribbeancom.com" -> CARIBBEANCOM_JA_PATH
                "en.caribbeancom.com" -> CARIBBEANCOM_EN_PATH
                else -> return null
            }
            val productId = pathPattern.matchEntire(uri.path.orEmpty())?.groupValues?.get(1)
            return productId?.takeIf {
                uri.scheme.equals("https", ignoreCase = true) &&
                    uri.rawUserInfo == null &&
                    (uri.port == -1 || uri.port == 443) &&
                    uri.rawQuery == null &&
                    uri.rawFragment == null
            }
        }

        fun String.removePrefixIgnoreCase(prefix: String): String =
            if (startsWith(prefix, ignoreCase = true)) substring(prefix.length) else this

        fun amazonProductId(value: String): String? {
            val candidate = AMAZON_URL.find(value)?.groupValues?.get(1)
                ?: value.takeIf { it.startsWith('/') }
                    ?.let { AMAZON_PATH.find(it)?.groupValues?.get(1) }
                ?: value
            return candidate.uppercase().takeIf(ASIN::matches)
        }

        fun gogProductId(value: String): String? {
            DIGITS.matchEntire(value)?.value?.let { return it }
            GOG_SLUG.matchEntire(value)?.value?.let { return it.lowercase() }

            val uri = runCatching { URI(value) }.getOrNull() ?: return null
            if (!uri.scheme.equals("https", ignoreCase = true) || uri.rawUserInfo != null) return null
            if (uri.port != -1 && uri.port != 443) return null
            val host = uri.host?.lowercase() ?: return null
            if (host != "gog.com" && host != "www.gog.com") return null

            val segments = uri.path.orEmpty().trim('/').split('/').filter(String::isNotEmpty)
            val slug = when {
                segments.size == 2 && segments[0].equals("game", ignoreCase = true) -> segments[1]
                segments.size == 3 && GOG_LOCALE.matches(segments[0]) &&
                    segments[1].equals("game", ignoreCase = true) -> segments[2]
                else -> return null
            }
            return slug.lowercase().takeIf(GOG_SLUG::matches)
        }
    }
}

sealed interface ValidationResult {
    data class Valid(val productId: String) : ValidationResult
    data class Invalid(val reason: InvalidReason) : ValidationResult
}

enum class InvalidReason { EMPTY, BAD_FORMAT }

/** Splits multi-line user input into trimmed, non-empty lines. */
fun parseInputLines(text: String): List<String> =
    text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
