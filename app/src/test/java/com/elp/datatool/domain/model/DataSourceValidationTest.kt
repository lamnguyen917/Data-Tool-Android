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
        assertValid(DataSource.AMAZON_BOOKS, "/dp/1098119517?ref=test", "1098119517")
        assertValid(DataSource.AMAZON_BOOKS, "/gp/product/b0abcdefgh#details", "B0ABCDEFGH")
        assertValid(DataSource.AMAZON_BOOKS, "https://www.amazon.co.uk/dp/B0BOOK1234", "B0BOOK1234")
        assertInvalid(DataSource.AMAZON_BOOKS, "ABCDEFGHIJ", InvalidReason.BAD_FORMAT)
        assertInvalid(DataSource.AMAZON_BOOKS, "https://example.com/dp/1098119517", InvalidReason.BAD_FORMAT)
        assertInvalid(DataSource.AMAZON_BOOKS, "https://www.amazon.com.evil.test/dp/1098119517", InvalidReason.BAD_FORMAT)
        assertInvalid(DataSource.AMAZON_BOOKS, "12345", InvalidReason.BAD_FORMAT)
    }

    @Test
    fun ave_acceptsSkuAndTrustedProductUrl() {
        assertValid(DataSource.AVE, "RHJ-100", "RHJ-100")
        assertValid(DataSource.AVE, "dvd1pt-101", "PT-101")
        assertValid(
            DataSource.AVE,
            "https://www.aventertainments.com/dvd/detail?lang=1&pro=30718",
            "https://www.aventertainments.com/dvd/detail?lang=1&pro=30718",
        )
        assertInvalid(DataSource.AVE, "https://example.com/dvd/detail?pro=30718", InvalidReason.BAD_FORMAT)
        assertInvalid(DataSource.AVE, "https://www.aventertainments.com/dvd/search?keyword=RHJ-100", InvalidReason.BAD_FORMAT)
    }

    @Test
    fun tokyoHot_acceptsProductIdAndTrustedProductUrl() {
        assertValid(DataSource.TOKYO_HOT, "N0001", "n0001")
        assertValid(DataSource.TOKYO_HOT, "HTN0001", "HTN0001")
        assertValid(
            DataSource.TOKYO_HOT,
            "https://www.tokyo-hot.com/product/5000/",
            "https://www.tokyo-hot.com/product/5000/",
        )
        assertInvalid(DataSource.TOKYO_HOT, "https://example.com/product/5000/", InvalidReason.BAD_FORMAT)
        assertInvalid(DataSource.TOKYO_HOT, "https://www.tokyo-hot.com/product/?q=n0001", InvalidReason.BAD_FORMAT)
    }

    @Test
    fun fc2_normalizesArticleIdAliasAndTrustedUrl() {
        assertValid(DataSource.FC2, "4871322", "4871322")
        assertValid(DataSource.FC2, "FC2-PPV-4871322", "4871322")
        assertValid(DataSource.FC2, "FC2 PPV 4871322", "4871322")
        assertValid(
            DataSource.FC2,
            "https://adult.contents.fc2.com/article/4871322/?lang=ja",
            "4871322",
        )
        assertInvalid(DataSource.FC2, "http://adult.contents.fc2.com/article/4871322/", InvalidReason.BAD_FORMAT)
        assertInvalid(DataSource.FC2, "https://adult.contents.fc2.com.evil.test/article/4871322/", InvalidReason.BAD_FORMAT)
        assertInvalid(DataSource.FC2, "https://adult.contents.fc2.com/embed/4871322/", InvalidReason.BAD_FORMAT)
    }

    @Test
    fun heyzo_acceptsFourDigitSkuAndTrustedProductUrls() {
        assertValid(DataSource.HEYZO, " 0123 ", "0123")
        assertValid(
            DataSource.HEYZO,
            "https://www.heyzo.com/moviepages/0123/index.html",
            "0123",
        )
        assertValid(
            DataSource.HEYZO,
            "https://en.heyzo.com/moviepages/9876/index.html",
            "9876",
        )
        assertInvalid(DataSource.HEYZO, "123", InvalidReason.BAD_FORMAT)
        assertInvalid(DataSource.HEYZO, "12345", InvalidReason.BAD_FORMAT)
        assertInvalid(
            DataSource.HEYZO,
            "http://www.heyzo.com/moviepages/0123/index.html",
            InvalidReason.BAD_FORMAT,
        )
        assertInvalid(
            DataSource.HEYZO,
            "https://www.heyzo.com.evil.test/moviepages/0123/index.html",
            InvalidReason.BAD_FORMAT,
        )
        assertInvalid(
            DataSource.HEYZO,
            "https://www.heyzo.com:8443/moviepages/0123/index.html",
            InvalidReason.BAD_FORMAT,
        )
        assertInvalid(
            DataSource.HEYZO,
            "https://www.heyzo.com/moviepages/0123/index.html?ref=test",
            InvalidReason.BAD_FORMAT,
        )
        assertInvalid(
            DataSource.HEYZO,
            "https://www.heyzo.com/moviepages/0123/index.html/",
            InvalidReason.BAD_FORMAT,
        )
    }

    @Test
    fun igdb_acceptsIdSlugAndUrl() {
        assertValid(DataSource.IGDB, "1942", "1942")
        assertValid(DataSource.IGDB, "hollow-knight", "hollow-knight")
        assertValid(DataSource.IGDB, "https://www.igdb.com/games/hollow-knight", "hollow-knight")
        assertInvalid(DataSource.IGDB, "Hollow Knight!", InvalidReason.BAD_FORMAT)
    }

    @Test
    fun gog_acceptsIdSlugAndTrustedProductUrls() {
        assertValid(DataSource.GOG, "1207658717", "1207658717")
        assertValid(DataSource.GOG, "alien_nations", "alien_nations")
        assertValid(DataSource.GOG, "https://www.gog.com/game/alien_nations", "alien_nations")
        assertValid(DataSource.GOG, "https://www.gog.com/en/game/alien_nations?ref=test", "alien_nations")
        assertValid(DataSource.GOG, "https://gog.com/pl/game/alien_nations#details", "alien_nations")
    }

    @Test
    fun gog_rejectsUntrustedOrNonProductUrls() {
        assertInvalid(DataSource.GOG, "Alien Nations", InvalidReason.BAD_FORMAT)
        assertInvalid(DataSource.GOG, "http://www.gog.com/game/alien_nations", InvalidReason.BAD_FORMAT)
        assertInvalid(DataSource.GOG, "https://www.gog.com.evil.test/game/alien_nations", InvalidReason.BAD_FORMAT)
        assertInvalid(DataSource.GOG, "https://www.gog.com/en/games", InvalidReason.BAD_FORMAT)
        assertInvalid(DataSource.GOG, "https://www.gog.com/en/game/alien_nations/extra", InvalidReason.BAD_FORMAT)
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
