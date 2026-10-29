package com.elp.datatool.data.adapter.caribbeancom

import com.elp.datatool.data.remote.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test

@Ignore("Live network smoke test")
class CaribbeancomLiveSmokeTest {

    @Test
    fun fetch_091317_498() = runBlocking {
        val adapter = CaribbeancomAdapter(
            http = HttpClient(HttpClient.createOkHttpClient()),
            parseDispatcher = Dispatchers.Unconfined,
        )

        val result = adapter.fetch("091317-498")

        assertEquals("091317-498", result.productId)
        assertTrue(result.title.orEmpty().contains("Chie Aoi"))
        assertTrue(result.localized.getValue("ja").creators.contains("葵千恵"))
    }
}
