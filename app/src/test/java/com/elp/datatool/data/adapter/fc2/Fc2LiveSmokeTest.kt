package com.elp.datatool.data.adapter.fc2

import com.elp.datatool.data.remote.HttpClient
import com.elp.datatool.domain.model.ProductAttributes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test

class Fc2LiveSmokeTest {

    @Ignore("Live network smoke test; run manually when validating FC2 HTML")
    @Test
    fun fetch_4871322() = runBlocking {
        val adapter = Fc2Adapter(
            http = HttpClient(HttpClient.createOkHttpClient()),
            parseDispatcher = Dispatchers.Unconfined,
        )

        val result = adapter.fetch("4871322")

        assertEquals("4871322", result.productId)
        assertTrue(result.title.orEmpty().isNotBlank())
        assertFalse(result.title.orEmpty().contains("pxoox", ignoreCase = true))
        assertEquals("KEN REAL", result.attributes[ProductAttributes.STUDIO])
        assertEquals("42:40", result.attributes[ProductAttributes.DURATION])
        assertTrue(result.description.orEmpty().length > 100)
        assertTrue(result.imageUrl.orEmpty().startsWith("https://"))
    }
}
