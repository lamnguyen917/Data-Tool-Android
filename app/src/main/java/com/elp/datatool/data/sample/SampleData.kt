package com.elp.datatool.data.sample

import com.elp.datatool.domain.model.DataSource
import com.elp.datatool.domain.model.FetchErrorKind
import com.elp.datatool.domain.model.ProductResult
import com.elp.datatool.domain.model.ResultStatus
import com.elp.datatool.domain.model.SearchHit

/**
 * Sample data for Compose previews and tests.
 * Mirrors the content of the wireframes.
 */
object SampleData {

    private const val MINUTE = 60_000L
    private const val BASE_TIME = 1_792_000_000_000L

    val results: List<ProductResult> = listOf(
        ProductResult(
            source = DataSource.UNITY_ASSET_STORE,
            productId = "96478",
            status = ResultStatus.COMPLETED,
            title = "MicroSplat",
            description = "Terrain shading system with modular features and extensions.",
            creators = listOf("Jason Booth"),
            categories = listOf("Tools", "Terrain"),
            canonicalUrl = "https://assetstore.unity.com/packages/tools/terrain/microsplat-96478",
            collectedAtMillis = BASE_TIME,
        ),
        ProductResult(
            source = DataSource.UNITY_ASSET_STORE,
            productId = "65563",
            status = ResultStatus.COMPLETED,
            title = "Terrain Composer 2",
            description = "Node-based terrain creation and automation toolkit.",
            creators = listOf("Nathaniel Doldersum"),
            categories = listOf("Tools", "Terrain"),
            canonicalUrl = "https://assetstore.unity.com/packages/tools/terrain/terrain-composer-2-65563",
            collectedAtMillis = BASE_TIME - 1 * MINUTE,
        ),
        ProductResult(
            source = DataSource.UNITY_ASSET_STORE,
            productId = "155852",
            status = ResultStatus.COMPLETED,
            title = "Gaia Pro",
            description = "Complete environment and terrain generation solution.",
            creators = listOf("Procedural Worlds"),
            categories = listOf("Tools", "Terrain"),
            canonicalUrl = "https://assetstore.unity.com/packages/tools/terrain/gaia-pro-155852",
            collectedAtMillis = BASE_TIME - 2 * MINUTE,
        ),
        ProductResult(
            source = DataSource.UNITY_ASSET_STORE,
            productId = "99101",
            status = ResultStatus.ERROR,
            error = FetchErrorKind.TIMEOUT,
            collectedAtMillis = BASE_TIME - 3 * MINUTE,
        ),
    )

    val catalog: List<SearchHit> = listOf(
        SearchHit(DataSource.UNITY_ASSET_STORE, "96478", "MicroSplat", publisher = "Jason Booth"),
        SearchHit(DataSource.UNITY_ASSET_STORE, "65563", "Terrain Composer 2", publisher = "Nathaniel Doldersum"),
        SearchHit(DataSource.UNITY_ASSET_STORE, "155852", "Gaia Pro", publisher = "Procedural Worlds"),
        SearchHit(DataSource.UNITY_ASSET_STORE, "89041", "Odin Inspector and Serializer"),
        SearchHit(DataSource.UNITY_ASSET_STORE, "68570", "Amplify Shader Editor"),
        SearchHit(DataSource.STEAM_STORE, "413150", "Stardew Valley"),
        SearchHit(DataSource.STEAM_STORE, "504230", "Celeste"),
        SearchHit(DataSource.STEAM_STORE, "1145360", "Hades"),
        SearchHit(DataSource.AMAZON_BOOKS, "0132350882", "Clean Code"),
        SearchHit(DataSource.AMAZON_BOOKS, "020161622X", "The Pragmatic Programmer"),
        SearchHit(DataSource.AMAZON_BOOKS, "0134757599", "Refactoring"),
        SearchHit(DataSource.IGDB, "1942", "The Witcher 3: Wild Hunt"),
        SearchHit(DataSource.IGDB, "14593", "Hollow Knight"),
    )
}
