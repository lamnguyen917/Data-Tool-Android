package com.elp.datatool.domain.model

/** A website the app can collect product metadata from. */
enum class DataSource(val id: String) {
    UNITY_ASSET_STORE("unity"),
    AMAZON_BOOKS("amazon"),
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
