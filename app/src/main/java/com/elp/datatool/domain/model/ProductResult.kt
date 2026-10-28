package com.elp.datatool.domain.model

import com.elp.datatool.domain.copy.CopyField
import com.elp.datatool.domain.copy.CopyFormatter
import com.elp.datatool.domain.copy.LineFormat

/** Normalized metadata collected for one product. */
data class ProductResult(
    val source: DataSource,
    val productId: String,
    val status: ResultStatus,
    val title: String? = null,
    val description: String? = null,
    val creators: List<String> = emptyList(),
    val categories: List<String> = emptyList(),
    val canonicalUrl: String? = null,
    val imageUrl: String? = null,
    /** Source-specific fields, keyed by [ProductAttributes]. */
    val attributes: Map<String, String> = emptyMap(),
    val error: FetchErrorKind? = null,
    val collectedAtMillis: Long = 0L,
) {
    /** Unique key across sources. */
    val key: String get() = keyOf(source, productId)

    companion object {
        fun keyOf(source: DataSource, productId: String) = "${source.id}:$productId"
    }
}

enum class ResultStatus { PROCESSING, COMPLETED, ERROR }

/** Keys for [ProductResult.attributes]. Values are plain, display-ready strings. */
object ProductAttributes {
    const val PRICE = "price"
    const val IS_FREE = "is_free"
    const val RATING = "rating"
    const val RATING_COUNT = "rating_count"
    const val PUBLISHER = "publisher"
    const val PUBLICATION_DATE = "publication_date"
    const val LANGUAGE = "language"
    const val PRINT_LENGTH = "print_length"
    const val ISBN_10 = "isbn_10"
    const val ISBN_13 = "isbn_13"
    const val VERSION = "version"
    const val LATEST_RELEASE = "latest_release"
    const val FIRST_PUBLISHED = "first_published"
    const val UNITY_VERSIONS = "unity_versions"
    const val FILE_SIZE = "file_size"
    const val ASSET_COUNT = "asset_count"
    const val TAGS = "tags"
}

/** Why collecting a product failed. Drives the message and the retry policy. */
enum class FetchErrorKind(val isTemporary: Boolean) {
    NETWORK(true),
    TIMEOUT(true),
    RATE_LIMITED(true),
    HTTP(true),
    BLOCKED(false),
    NOT_FOUND(false),
    PARSE(false),
    UNSUPPORTED(false);

    /** Whether a manual retry can succeed without changing the input. */
    val canRetry: Boolean get() = this != NOT_FOUND && this != UNSUPPORTED
}

class FetchException(
    val kind: FetchErrorKind,
    message: String? = null,
    cause: Throwable? = null,
) : Exception(message ?: kind.name, cause)

/** A product returned by a keyword search. */
data class SearchHit(
    val source: DataSource,
    val productId: String,
    val title: String,
    val publisher: String? = null,
    val imageUrl: String? = null,
)

/** Progress of the current collection batch. */
data class QueueState(
    val running: Boolean = false,
    val total: Int = 0,
    val completed: Int = 0,
    val failed: Int = 0,
    val finished: Boolean = false,
    val cancelled: Boolean = false,
) {
    val processed: Int get() = completed + failed
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val parallelRequests: Int = 3,
    val timeoutSeconds: Int = 30,
    val autoRetry: Boolean = true,
    val copyLineFormat: LineFormat = CopyFormatter.DEFAULT_LINE_FORMAT,
    val copyLineFields: List<CopyField> = CopyFormatter.DEFAULT_LINE_FIELDS,
    val copyDetailsTemplate: String = CopyFormatter.DEFAULT_DETAILS_TEMPLATE,
)
