package com.elp.datatool.data.adapter.ave

import com.elp.datatool.data.remote.HttpClient
import com.elp.datatool.domain.model.DataSource
import com.elp.datatool.domain.model.FetchErrorKind
import com.elp.datatool.domain.model.FetchException
import com.elp.datatool.domain.model.ProductResult
import com.elp.datatool.domain.model.SearchHit
import com.elp.datatool.domain.repository.SourceAdapter
import java.net.URI
import java.net.URLEncoder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class AveAdapter(
    private val http: HttpClient,
    private val baseUrl: String = BASE_URL,
    private val parseDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : SourceAdapter {

    override val source = DataSource.AVE

    override suspend fun search(query: String): List<SearchHit> {
        val page = http.get(searchUrl(query))
        return withContext(parseDispatcher) { AveParser.parseSearch(page.body, page.url) }
    }

    override suspend fun fetch(productId: String): ProductResult {
        val input = productId.trim()
        val requestedSku: String?
        val productUrl: String
        if (input.startsWith("https://", ignoreCase = true)) {
            if (!isTrustedProductUrl(input)) {
                throw FetchException(FetchErrorKind.PARSE, "Invalid AV Entertainments product URL")
            }
            requestedSku = null
            productUrl = input
        } else {
            requestedSku = AveParser.normalizeSku(input)
            val candidates = loadCandidates(requestedSku)
            productUrl = candidates
                .firstOrNull {
                    AveParser.normalizeSku(it.sku) == requestedSku && isTrustedProductUrl(it.url)
                }
                ?.url
                ?: throw FetchException(FetchErrorKind.NOT_FOUND, "Ave product $requestedSku not found")
        }

        val englishPage = http.get(productUrl)
        var result = withContext(parseDispatcher) {
            AveParser.parseProduct(englishPage.body, englishPage.url, requestedSku)
        }

        val japaneseUrl = toJapaneseUrl(englishPage.url)
        if (japaneseUrl != englishPage.url) {
            val localized = try {
                val japanesePage = http.get(japaneseUrl)
                withContext(parseDispatcher) {
                    AveParser.parseLocalizedProduct(japanesePage.body, japanesePage.url)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                null
            }
            if (localized != null) result = result.copy(localized = mapOf(JAPANESE_LANGUAGE to localized))
        }
        return result
    }

    internal fun searchUrl(query: String): String =
        "$baseUrl/dvd/search?lang=1&cat=29&culture=en-US&keyword=" +
            URLEncoder.encode(query.trim(), Charsets.UTF_8.name())

    internal fun toJapaneseUrl(url: String): String {
        val pathAdjusted = url.replace("/1/", "/2/")
        return pathAdjusted.toHttpUrlOrNull()
            ?.newBuilder()
            ?.setQueryParameter("lang", "2")
            ?.setQueryParameter("culture", "ja-JP")
            ?.build()
            ?.toString()
            ?: pathAdjusted
                .replace("lang=1", "lang=2")
                .replace("culture=en-US", "culture=ja-JP")
    }

    private suspend fun loadCandidates(query: String): List<AveParser.Candidate> {
        val page = http.get(searchUrl(query))
        return withContext(parseDispatcher) { AveParser.parseCandidates(page.body, page.url) }
    }

    private fun isTrustedProductUrl(value: String): Boolean {
        val uri = runCatching { URI(value) }.getOrNull() ?: return false
        val baseUri = runCatching { URI(baseUrl) }.getOrNull() ?: return false
        val hasProductId = uri.rawQuery
            ?.split('&')
            ?.any { parameter ->
                val parts = parameter.split('=', limit = 2)
                parts.size == 2 && parts[0].equals("pro", ignoreCase = true) &&
                    parts[1].all(Char::isDigit)
            } == true
        return uri.scheme.equals(baseUri.scheme, ignoreCase = true) &&
            uri.host.equals(baseUri.host, ignoreCase = true) &&
            uri.path.equals("/dvd/detail", ignoreCase = true) &&
            hasProductId
    }

    companion object {
        const val BASE_URL = "https://www.aventertainments.com"
        private const val JAPANESE_LANGUAGE = "ja"
    }
}
