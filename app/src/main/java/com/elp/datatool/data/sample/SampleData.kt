package com.elp.datatool.data.sample

import com.elp.datatool.domain.model.DataSource
import com.elp.datatool.domain.model.FetchErrorKind
import com.elp.datatool.domain.model.ProductAttributes
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

    /** One representative result per source, used by adapter copy-setting previews. */
    val copyPreviews: Map<DataSource, ProductResult> = mapOf(
        DataSource.UNITY_ASSET_STORE to results.first().copy(
            imageUrl = "https://assetstore-keyimage.unity3d.com/microsplat.jpg",
            attributes = mapOf(
                ProductAttributes.PRICE to "$30.00",
                ProductAttributes.RATING to "4.8",
                ProductAttributes.RATING_COUNT to "512",
                ProductAttributes.VERSION to "3.9.2",
                ProductAttributes.LATEST_RELEASE to "2026-09-14",
                ProductAttributes.FIRST_PUBLISHED to "2017-08-02",
                ProductAttributes.UNITY_VERSIONS to "2021.3, 2022.3, 6000.0",
                ProductAttributes.FILE_SIZE to "48.2 MB",
                ProductAttributes.ASSET_COUNT to "312",
                ProductAttributes.TAGS to "terrain, shader, splat",
            ),
        ),
        DataSource.AMAZON_BOOKS to ProductResult(
            source = DataSource.AMAZON_BOOKS,
            productId = "0132350882",
            status = ResultStatus.COMPLETED,
            title = "Clean Code: A Handbook of Agile Software Craftsmanship",
            description = "Even bad code can function. But if code isn't clean, it can bring a development organization to its knees.",
            creators = listOf("Robert C. Martin"),
            categories = listOf("Books", "Computers & Technology", "Programming"),
            canonicalUrl = "https://www.amazon.com/dp/0132350882",
            imageUrl = "https://m.media-amazon.com/images/I/clean-code.jpg",
            attributes = mapOf(
                ProductAttributes.PUBLISHER to "Pearson",
                ProductAttributes.PUBLICATION_DATE to "August 1, 2008",
                ProductAttributes.LANGUAGE to "English",
                ProductAttributes.PRINT_LENGTH to "464 pages",
                ProductAttributes.ISBN_10 to "0132350882",
                ProductAttributes.ISBN_13 to "978-0132350884",
                ProductAttributes.RATING to "4.7",
                ProductAttributes.RATING_COUNT to "6,214",
            ),
            collectedAtMillis = BASE_TIME,
        ),
        DataSource.GOG to ProductResult(
            source = DataSource.GOG,
            productId = "1207664663",
            status = ResultStatus.COMPLETED,
            title = "The Witcher 3: Wild Hunt",
            description = "Become a professional monster slayer and embark on an adventure of epic proportions.",
            creators = listOf("CD PROJEKT RED"),
            categories = listOf("Role-playing", "Adventure"),
            canonicalUrl = "https://www.gog.com/en/game/the_witcher_3_wild_hunt",
            imageUrl = "https://images.gog-statics.com/witcher3.jpg",
            attributes = mapOf(
                ProductAttributes.PUBLISHER to "CD PROJEKT RED",
                ProductAttributes.PUBLICATION_DATE to "2015-05-19",
                ProductAttributes.GOG_RELEASE_DATE to "2015-05-18",
                ProductAttributes.PLATFORMS to "Windows",
                ProductAttributes.LANGUAGE to "English, Polish, German",
                ProductAttributes.FEATURES to "Single-player, Achievements, Cloud saves",
                ProductAttributes.TAGS to "Open World, Fantasy, Story Rich",
                ProductAttributes.PRICE to "$39.99",
                ProductAttributes.RATING to "4.8",
                ProductAttributes.RATING_COUNT to "15,032",
            ),
            collectedAtMillis = BASE_TIME,
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
