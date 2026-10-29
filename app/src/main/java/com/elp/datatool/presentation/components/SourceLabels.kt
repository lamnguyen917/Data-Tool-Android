package com.elp.datatool.presentation.components

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.elp.datatool.R
import com.elp.datatool.domain.copy.CopyField
import com.elp.datatool.domain.model.DataSource
import com.elp.datatool.domain.model.FetchErrorKind
import com.elp.datatool.domain.model.ProductResult

@get:StringRes
val DataSource.nameRes: Int
    get() = when (this) {
        DataSource.UNITY_ASSET_STORE -> R.string.source_unity
        DataSource.AMAZON_BOOKS -> R.string.source_amazon
        DataSource.AVE -> R.string.source_ave
        DataSource.TOKYO_HOT -> R.string.source_tokyo_hot
        DataSource.FC2 -> R.string.source_fc2
        DataSource.HEYZO -> R.string.source_heyzo
        DataSource.STEAM_STORE -> R.string.source_steam
        DataSource.IGDB -> R.string.source_igdb
    }

@get:StringRes
val DataSource.shortLabelRes: Int
    get() = when (this) {
        DataSource.UNITY_ASSET_STORE -> R.string.source_short_unity
        DataSource.AMAZON_BOOKS -> R.string.source_short_amazon
        DataSource.AVE -> R.string.source_short_ave
        DataSource.TOKYO_HOT -> R.string.source_short_tokyo_hot
        DataSource.FC2 -> R.string.source_short_fc2
        DataSource.HEYZO -> R.string.source_short_heyzo
        DataSource.STEAM_STORE -> R.string.source_short_steam
        DataSource.IGDB -> R.string.source_short_igdb
    }

@get:StringRes
val DataSource.idLabelRes: Int
    get() = when (this) {
        DataSource.UNITY_ASSET_STORE -> R.string.id_label_unity
        DataSource.AMAZON_BOOKS -> R.string.id_label_amazon
        DataSource.AVE -> R.string.id_label_ave
        DataSource.TOKYO_HOT -> R.string.id_label_tokyo_hot
        DataSource.FC2 -> R.string.id_label_fc2
        DataSource.HEYZO -> R.string.id_label_heyzo
        DataSource.STEAM_STORE -> R.string.id_label_steam
        DataSource.IGDB -> R.string.id_label_igdb
    }

@get:StringRes
val DataSource.invalidMessageRes: Int
    get() = when (this) {
        DataSource.UNITY_ASSET_STORE -> R.string.invalid_unity
        DataSource.AMAZON_BOOKS -> R.string.invalid_amazon
        DataSource.AVE -> R.string.invalid_ave
        DataSource.TOKYO_HOT -> R.string.invalid_tokyo_hot
        DataSource.FC2 -> R.string.invalid_fc2
        DataSource.HEYZO -> R.string.invalid_heyzo
        DataSource.STEAM_STORE -> R.string.invalid_steam
        DataSource.IGDB -> R.string.invalid_igdb
    }

@get:StringRes
val FetchErrorKind.labelRes: Int
    get() = when (this) {
        FetchErrorKind.NETWORK -> R.string.error_network
        FetchErrorKind.TIMEOUT -> R.string.error_timeout
        FetchErrorKind.RATE_LIMITED -> R.string.error_rate_limited
        FetchErrorKind.HTTP -> R.string.error_http
        FetchErrorKind.BLOCKED -> R.string.error_blocked
        FetchErrorKind.NOT_FOUND -> R.string.error_not_found
        FetchErrorKind.PARSE -> R.string.error_parse
        FetchErrorKind.UNSUPPORTED -> R.string.error_unsupported
    }

@get:StringRes
val FetchErrorKind.descriptionRes: Int
    get() = when (this) {
        FetchErrorKind.NETWORK -> R.string.error_network_desc
        FetchErrorKind.TIMEOUT -> R.string.error_timeout_desc
        FetchErrorKind.RATE_LIMITED -> R.string.error_rate_limited_desc
        FetchErrorKind.HTTP -> R.string.error_http_desc
        FetchErrorKind.BLOCKED -> R.string.error_blocked_desc
        FetchErrorKind.NOT_FOUND -> R.string.error_not_found_desc
        FetchErrorKind.PARSE -> R.string.error_parse_desc
        FetchErrorKind.UNSUPPORTED -> R.string.error_unsupported_desc
    }

@get:StringRes
val CopyField.labelRes: Int
    get() = when (this) {
        CopyField.SKU -> R.string.field_sku
        CopyField.TITLE -> R.string.field_title
        CopyField.CATEGORIES -> R.string.field_categories
        CopyField.AUTHOR -> R.string.field_author
        CopyField.URL -> R.string.field_url
        CopyField.SOURCE -> R.string.field_source
        CopyField.PRICE -> R.string.field_price
        CopyField.RATING -> R.string.field_rating
        CopyField.VERSION -> R.string.field_version
        CopyField.LATEST_RELEASE -> R.string.field_latest_release
        CopyField.FIRST_PUBLISHED -> R.string.field_first_published
        CopyField.UNITY_VERSIONS -> R.string.field_unity_versions
        CopyField.FILE_SIZE -> R.string.field_file_size
        CopyField.ASSET_COUNT -> R.string.field_asset_count
        CopyField.TAGS -> R.string.field_tags
        CopyField.IMAGE_URL -> R.string.field_image_url
        CopyField.DESCRIPTION -> R.string.field_description
    }

/** "Package ID 96478" style label. */
@Composable
fun productIdLabel(source: DataSource, productId: String): String =
    stringResource(R.string.product_id_format, stringResource(source.idLabelRes), productId)

/** Title, or the product ID label when the title could not be collected. */
@Composable
fun ProductResult.displayTitle(): String = title ?: productIdLabel(source, productId)

/** Plain-text summary used for sharing and copying. */
fun ProductResult.toShareText(context: Context): String = buildList {
    add(title ?: context.getString(R.string.product_id_format, context.getString(source.idLabelRes), productId))
    add(
        "${context.getString(source.nameRes)} · " +
            context.getString(R.string.product_id_format, context.getString(source.idLabelRes), productId)
    )
    creators.takeIf { it.isNotEmpty() }?.let { add(it.joinToString()) }
    canonicalUrl?.let { add(it) }
}.joinToString("\n")
