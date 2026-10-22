package com.elp.datatool.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DataSourceValidationTest {

    @Test
    fun unity_acceptsNumericIdAndPackageUrl() {
        assertValid(DataSource.UNITY_ASSET_STORE, "96478", "96478")
        assertValid(
            DataSource.UNITY_ASSET_STORE,
            "https://assetstore.unity.com/packages/tools/terrain/microsplat-96478",
            "96478",
        )
        assertValid(
            DataSource.UNITY_ASSET_STORE,
            "https://assetstore.unity.com/packages/tools/terrain/microsplat-96478?aid=1",
            "96478",
        )
    }

    @Test
    fun unity_rejectsText() {
        assertInvalid(DataSource.UNITY_ASSET_STORE, "invalid-item", InvalidReason.BAD_FORMAT)
    }

    @Test
    fun steam_acceptsAppIdAndStoreUrl() {
        assertValid(DataSource.STEAM_STORE, "606950", "606950")
        assertValid(DataSource.STEAM_STORE, "https://store.steampowered.com/app/413150/Stardew_Valley/", "413150")
        assertInvalid(DataSource.STEAM_STORE, "ABC", InvalidReason.BAD_FORMAT)
    }

    @Test
    fun amazon_normalizesAsinToUpperCase() {
        assertValid(DataSource.AMAZON_BOOKS, "020161622x", "020161622X")
        assertValid(DataSource.AMAZON_BOOKS, "https://www.amazon.com/Clean-Code/dp/0132350882/ref=sr_1_1", "0132350882")
        assertInvalid(DataSource.AMAZON_BOOKS, "12345", InvalidReason.BAD_FORMAT)
    }

    @Test
    fun igdb_acceptsIdSlugAndUrl() {
        assertValid(DataSource.IGDB, "1942", "1942")
        assertValid(DataSource.IGDB, "hollow-knight", "hollow-knight")
        assertValid(DataSource.IGDB, "https://www.igdb.com/games/hollow-knight", "hollow-knight")
        assertInvalid(DataSource.IGDB, "Hollow Knight!", InvalidReason.BAD_FORMAT)
    }

    @Test
    fun blankInput_isEmpty() {
        assertInvalid(DataSource.STEAM_STORE, "   ", InvalidReason.EMPTY)
    }

    @Test
    fun parseInputLines_trimsAndSkipsBlankLines() {
        assertEquals(listOf("96478", "36050"), parseInputLines(" 96478 \n\n36050\r\n  "))
    }

    private fun assertValid(source: DataSource, input: String, expectedId: String) {
        assertEquals(ValidationResult.Valid(expectedId), source.validate(input))
    }

    private fun assertInvalid(source: DataSource, input: String, reason: InvalidReason) {
        assertEquals(ValidationResult.Invalid(reason), source.validate(input))
    }
}
