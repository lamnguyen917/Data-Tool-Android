package com.elp.datatool.data.remote

import com.elp.datatool.domain.model.FetchErrorKind
import com.elp.datatool.domain.model.FetchException
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

/** Final response after redirects. */
data class HttpPage(val url: String, val code: Int, val body: String)

/** Thin coroutine wrapper over OkHttp that maps failures to [FetchException]. */
class HttpClient(private val client: OkHttpClient) {

    suspend fun get(url: String): HttpPage {
        val request = Request.Builder().url(url).get().build()
        val response = client.newCall(request).await()
        return response.use {
            val page = HttpPage(it.request.url.toString(), it.code, it.body?.string().orEmpty())
            when {
                it.isSuccessful -> page
                it.code == 404 -> throw FetchException(FetchErrorKind.NOT_FOUND, "HTTP 404")
                it.code == 429 -> throw FetchException(FetchErrorKind.RATE_LIMITED, "HTTP 429")
                it.code == 401 || it.code == 403 -> throw FetchException(FetchErrorKind.BLOCKED, "HTTP ${it.code}")
                else -> throw FetchException(FetchErrorKind.HTTP, "HTTP ${it.code}")
            }
        }
    }

    private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { cancel() }
        enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) = continuation.resume(response)

            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isCancelled) return
                val kind = if (e is SocketTimeoutException) FetchErrorKind.TIMEOUT else FetchErrorKind.NETWORK
                continuation.resumeWithException(FetchException(kind, e.message, e))
            }
        })
    }

    companion object {
        /** Mobile Chrome user agent; Asset Store serves the same HTML as to browsers. */
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/130.0 Mobile Safari/537.36"

        fun createOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .cookieJar(InMemoryCookieJar())
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", USER_AGENT)
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .header("Accept-Language", "en-US,en;q=0.9")
                        .build()
                )
            }
            .build()
    }
}

/** Session cookies kept in memory; never persisted or logged. */
private class InMemoryCookieJar : CookieJar {
    private val cookies = mutableMapOf<String, List<Cookie>>()

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val existing = this.cookies[url.host].orEmpty().filterNot { old -> cookies.any { it.name == old.name } }
        this.cookies[url.host] = existing + cookies
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        return cookies[url.host].orEmpty().filter { it.expiresAt > now && it.matches(url) }
    }
}
