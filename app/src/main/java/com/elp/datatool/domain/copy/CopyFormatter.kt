package com.elp.datatool.domain.copy

import com.elp.datatool.domain.model.DataSource
import com.elp.datatool.domain.model.ProductAttributes
import com.elp.datatool.domain.model.ProductResult

/** A value of a result that can be copied as a column or a template placeholder. */
enum class CopyField(val key: String) {
    SKU("sku"),
    TITLE("title"),
    CATEGORIES("categories"),
    AUTHOR("author"),
    URL("url"),
    SOURCE("source"),
    PRICE("price"),
    RATING("rating"),
    VERSION("version"),
    LATEST_RELEASE("latest_release"),
    FIRST_PUBLISHED("first_published"),
    UNITY_VERSIONS("unity_versions"),
    FILE_SIZE("file_size"),
    ASSET_COUNT("asset_count"),
    TAGS("tags"),
    IMAGE_URL("image_url"),
    DESCRIPTION("description");

    val placeholder: String get() = "{$key}"

    fun valueOf(result: ProductResult): String = when (this) {
        SKU -> result.productId
        TITLE -> result.title.orEmpty()
        CATEGORIES -> result.categories.joinToString(" / ")
        AUTHOR -> result.creators.joinToString(", ")
        URL -> result.canonicalUrl.orEmpty()
        SOURCE -> result.source.displayName
        PRICE -> if (result.attributes[ProductAttributes.IS_FREE] == "true") {
            "Free"
        } else {
            result.attributes[ProductAttributes.PRICE].orEmpty()
        }
        RATING -> result.attributes[ProductAttributes.RATING]?.let { rating ->
            result.attributes[ProductAttributes.RATING_COUNT]?.let { "$rating ($it)" } ?: rating
        }.orEmpty()
        VERSION -> result.attributes[ProductAttributes.VERSION].orEmpty()
        LATEST_RELEASE -> result.attributes[ProductAttributes.LATEST_RELEASE].orEmpty()
        FIRST_PUBLISHED -> result.attributes[ProductAttributes.FIRST_PUBLISHED].orEmpty()
        UNITY_VERSIONS -> result.attributes[ProductAttributes.UNITY_VERSIONS].orEmpty()
        FILE_SIZE -> result.attributes[ProductAttributes.FILE_SIZE].orEmpty()
        ASSET_COUNT -> result.attributes[ProductAttributes.ASSET_COUNT].orEmpty()
        TAGS -> result.attributes[ProductAttributes.TAGS].orEmpty()
        IMAGE_URL -> result.imageUrl.orEmpty()
        DESCRIPTION -> result.description.orEmpty()
    }

    companion object {
        private val byKey = entries.associateBy { it.key }
        fun fromKey(key: String): CopyField? = byKey[key]
    }
}

enum class LineFormat { CSV, TSV }

/** Builds clipboard text for results. Pure functions, no Android dependencies. */
object CopyFormatter {

    val DEFAULT_LINE_FIELDS = listOf(
        CopyField.SKU,
        CopyField.TITLE,
        CopyField.CATEGORIES,
        CopyField.AUTHOR,
        CopyField.URL,
    )

    val DEFAULT_LINE_FORMAT = LineFormat.TSV

    val DEFAULT_DETAILS_TEMPLATE = """
        {title}
        SKU: {sku}
        Source: {source}
        Author: {author}
        Categories: {categories}
        Price: {price}
        Rating: {rating}
        Version: {version}
        URL: {url}

        {description}
    """.trimIndent()

    private val PLACEHOLDER = Regex("""\{([a-z_]+)\}""")

    /** One delimited line. CSV follows RFC 4180 quoting; TSV flattens tabs and line breaks. */
    fun line(result: ProductResult, fields: List<CopyField>, format: LineFormat): String =
        fields.joinToString(if (format == LineFormat.CSV) "," else "\t") { field ->
            val value = field.valueOf(result)
            when (format) {
                LineFormat.CSV -> escapeCsv(value)
                LineFormat.TSV -> value.replace(Regex("""[\t\r\n]+"""), " ").trim()
            }
        }

    /**
     * Fills `{placeholder}` tokens. Unknown tokens are kept as typed.
     * A line whose known placeholders are all empty is removed, so missing fields do not leave
     * "Price: " style lines behind.
     */
    fun details(result: ProductResult, template: String): String {
        val lines = template.lines().mapNotNull { line ->
            val fields = PLACEHOLDER.findAll(line).mapNotNull { CopyField.fromKey(it.groupValues[1]) }.toList()
            if (fields.isNotEmpty() && fields.all { it.valueOf(result).isBlank() }) {
                null
            } else {
                PLACEHOLDER.replace(line) { match ->
                    CopyField.fromKey(match.groupValues[1])?.valueOf(result) ?: match.value
                }
            }
        }
        return lines.joinToString("\n").replace(Regex("\n{3,}"), "\n\n").trim()
    }

    private fun escapeCsv(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
}

/** English source name used in copied and exported data (UI labels use string resources). */
val DataSource.displayName: String
    get() = when (this) {
        DataSource.UNITY_ASSET_STORE -> "Unity Asset Store"
        DataSource.AMAZON_BOOKS -> "Amazon Books"
        DataSource.AVE -> "Ave"
        DataSource.TOKYO_HOT -> "Tokyo-Hot"
        DataSource.FC2 -> "FC2"
        DataSource.STEAM_STORE -> "Steam Store"
        DataSource.IGDB -> "IGDB"
    }
