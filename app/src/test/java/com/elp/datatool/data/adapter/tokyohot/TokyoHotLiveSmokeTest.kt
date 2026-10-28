package com.elp.datatool.data.adapter.tokyohot

import com.elp.datatool.data.remote.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test

class TokyoHotLiveSmokeTest {

    @Ignore("Live network smoke test; run manually when validating Tokyo-Hot HTML")
    @Test
    fun searchAndFetch_n0001() = runBlocking {
        val adapter = TokyoHotAdapter(
            http = HttpClient(HttpClient.createOkHttpClient()),
            parseDispatcher = Dispatchers.Unconfined,
        )

        val hits = adapter.search("n0001")
        assertTrue(hits.any { it.productId.equals("n0001", ignoreCase = true) })

        val result = adapter.fetch("n0001")
        assertEquals("n0001", result.productId)
        assertEquals("Office Lady", result.title)
        assertEquals("OL無残社内ナマ中出し", result.localized.getValue("ja").title)
    }
}
