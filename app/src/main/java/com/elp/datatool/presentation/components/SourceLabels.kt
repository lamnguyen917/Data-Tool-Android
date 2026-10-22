package com.elp.datatool.presentation.components

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.elp.datatool.R
import com.elp.datatool.domain.model.DataSource
import com.elp.datatool.domain.model.FetchErrorKind
import com.elp.datatool.domain.model.ProductResult

@get:StringRes
val DataSource.nameRes: Int
    get() = when (this) {
        DataSource.UNITY_ASSET_STORE -> R.string.source_unity
        DataSource.AMAZON_BOOKS -> R.string.source_amazon
        DataSource.STEAM_STORE -> R.string.source_steam
        DataSource.IGDB -> R.string.source_igdb
    }

@get:StringRes
val DataSource.shortLabelRes: Int
    get() = when (this) {
        DataSource.UNITY_ASSET_STORE -> R.string.source_short_unity
        DataSource.AMAZON_BOOKS -> R.string.source_short_amazon
        DataSource.STEAM_STORE -> R.string.source_short_steam
        DataSource.IGDB -> R.string.source_short_igdb
    }

@get:StringRes
val DataSource.idLabelRes: Int
    get() = when (this) {
        DataSource.UNITY_ASSET_STORE -> R.string.id_label_unity
        DataSource.AMAZON_BOOKS -> R.string.id_label_amazon
        DataSource.STEAM_STORE -> R.string.id_label_steam
        DataSource.IGDB -> R.string.id_label_igdb
    }

@get:StringRes
val DataSource.invalidMessageRes: Int
    get() = when (this) {
        DataSource.UNITY_ASSET_STORE -> R.string.invalid_unity
        DataSource.AMAZON_BOOKS -> R.string.invalid_amazon
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
