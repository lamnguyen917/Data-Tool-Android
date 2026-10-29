package com.elp.datatool.domain.model

import java.net.URI

/** A website the app can collect product metadata from. */
enum class DataSource(val id: String) {
    UNITY_ASSET_STORE("unity"),
    AMAZON_BOOKS("amazon"),
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
