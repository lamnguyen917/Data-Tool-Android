package com.elp.datatool.domain.model

import java.util.Locale

/** A website the app can collect product metadata from. */
enum class DataSource(val id: String) {
    UNITY_ASSET_STORE("unity"),
    AMAZON_BOOKS("amazon"),
    AVE("ave"),
    TOKYO_HOT("tokyo-hot"),
    FC2("fc2"),
    STEAM_STORE("steam"),
    IGDB("igdb");

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

            IGDB -> DIGITS.matchEntire(value)?.value
                ?: IGDB_URL.find(value)?.groupValues?.get(1)
                ?: IGDB_SLUG.matchEntire(value)?.value
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

        fun String.removePrefixIgnoreCase(prefix: String): String =
            if (startsWith(prefix, ignoreCase = true)) substring(prefix.length) else this

        fun amazonProductId(value: String): String? {
            val candidate = AMAZON_URL.find(value)?.groupValues?.get(1)
                ?: value.takeIf { it.startsWith('/') }
                    ?.let { AMAZON_PATH.find(it)?.groupValues?.get(1) }
                ?: value
            return candidate.uppercase().takeIf(ASIN::matches)
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
