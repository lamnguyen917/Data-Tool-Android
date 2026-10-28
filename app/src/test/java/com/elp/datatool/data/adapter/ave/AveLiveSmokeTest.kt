package com.elp.datatool.data.adapter.ave

import com.elp.datatool.data.remote.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test

@Ignore("Live network smoke test")
class AveLiveSmokeTest {

    @Test
    fun fetch_rhj101DirectProduct() = runBlocking {
        val adapter = AveAdapter(
            http = HttpClient(HttpClient.createOkHttpClient()),
            parseDispatcher = Dispatchers.Unconfined,
        )

        val result = adapter.fetch(
            "https://www.aventertainments.com/dvd/detail?lang=1&cat=29&culture=en-US&pro=30719"
        )

        assertEquals("RHJ-101", result.productId)
        assertTrue(result.title.orEmpty().contains("Red Hot Jam Vol.101"))
    }
}
