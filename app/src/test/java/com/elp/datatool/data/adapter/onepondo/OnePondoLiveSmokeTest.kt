package com.elp.datatool.data.adapter.onepondo

import com.elp.datatool.data.remote.HttpClient
import com.elp.datatool.domain.model.ProductAttributes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test

class OnePondoLiveSmokeTest {

    @Ignore("Live network smoke test; run manually when validating 1pondo JSON")
    @Test
    fun fetch_010121_001() = runBlocking {
        val adapter = OnePondoAdapter(
            http = HttpClient(HttpClient.createOkHttpClient()),
            parseDispatcher = Dispatchers.Unconfined,
        )

        val result = adapter.fetch("010121_001")

        assertEquals("010121_001", result.productId)
        assertTrue(result.title.orEmpty().isNotBlank())
        assertEquals("2021-01-01", result.attributes[ProductAttributes.RELEASE_DATE])
        assertEquals("01:00:54", result.attributes[ProductAttributes.DURATION])
        assertTrue(result.localized.getValue("ja").title.orEmpty().isNotBlank())
        assertTrue(result.imageUrl.orEmpty().startsWith("https://www.1pondo.tv/"))
    }
}
