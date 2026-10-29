package com.elp.datatool.data.adapter.tokyohot

import com.elp.datatool.data.adapter.withBrowserChallenge
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

class TokyoHotAdapter(
    private val http: HttpClient,
    private val baseUrl: String = BASE_URL,
    private val parseDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : SourceAdapter {

    override val source = DataSource.TOKYO_HOT

    override suspend fun search(query: String): List<SearchHit> {
        if (query.isBlank()) return emptyList()
        val page = http.get(searchUrl(query))
        return withContext(parseDispatcher) { TokyoHotParser.parseSearch(page.body, page.url) }
    }

    override suspend fun fetch(productId: String): ProductResult {
        val input = productId.trim()
        val requestedProductId: String?
        val productUrl: String
        if (input.startsWith("http://", ignoreCase = true) || input.startsWith("https://", ignoreCase = true)) {
            if (!isTrustedProductUrl(input)) {
                throw FetchException(FetchErrorKind.PARSE, "Invalid Tokyo-Hot product URL")
            }
            requestedProductId = null
            productUrl = normalizeProductUrl(input)
        } else {
            requestedProductId = TokyoHotParser.normalizeProductId(input)
            productUrl = loadCandidates(requestedProductId)
                .firstOrNull { candidate ->
                    candidate.productId.equals(requestedProductId, ignoreCase = true) &&
                        isTrustedProductUrl(candidate.url)
                }
                ?.url
                ?: throw FetchException(
                    FetchErrorKind.NOT_FOUND,
                    "Tokyo-Hot product $requestedProductId not found",
                )
        }

        val englishUrl = languageUrl(productUrl, ENGLISH_LANGUAGE)
        val (englishPage, parsedResult) = withBrowserChallenge(englishUrl) {
            val loadedPage = http.get(englishUrl)
            loadedPage to withContext(parseDispatcher) {
                TokyoHotParser.parseProduct(loadedPage.body, loadedPage.url, requestedProductId)
            }
        }
        var result = parsedResult

        val japaneseUrl = languageUrl(englishPage.url, JAPANESE_LANGUAGE)
        val localized = try {
            val japanesePage = http.get(japaneseUrl)
            withContext(parseDispatcher) {
                TokyoHotParser.parseLocalizedProduct(japanesePage.body, japanesePage.url)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        }
        if (localized != null) result = result.copy(localized = mapOf(JAPANESE_LANGUAGE to localized))
        return result
    }

    internal fun searchUrl(query: String): String =
        "$baseUrl/product/?q=${URLEncoder.encode(query.trim(), Charsets.UTF_8.name())}&x=0&y=0"

    internal fun languageUrl(url: String, language: String): String = url.toHttpUrlOrNull()
        ?.newBuilder()
        ?.setQueryParameter("lang", language)
        ?.build()
        ?.toString()
        ?: "$url?lang=$language"

    private suspend fun loadCandidates(query: String): List<TokyoHotParser.Candidate> {
        val url = searchUrl(query)
        return withBrowserChallenge(url) {
            val page = http.get(url)
            withContext(parseDispatcher) { TokyoHotParser.parseCandidates(page.body, page.url) }
        }
    }

    private fun isTrustedProductUrl(value: String): Boolean {
        val uri = runCatching { URI(value) }.getOrNull() ?: return false
        val baseUri = runCatching { URI(baseUrl) }.getOrNull() ?: return false
        return uri.scheme.equals(baseUri.scheme, ignoreCase = true) &&
            uri.host.equals(baseUri.host, ignoreCase = true) &&
            Regex("""/product/[A-Za-z0-9_-]+/?""").matches(uri.path.orEmpty())
    }

    private fun normalizeProductUrl(url: String): String = url.toHttpUrlOrNull()
        ?.newBuilder()
        ?.query(null)
        ?.fragment(null)
        ?.build()
        ?.toString()
        ?: url.substringBefore('?').substringBefore('#')

    companion object {
        const val BASE_URL = "https://www.tokyo-hot.com"
        private const val ENGLISH_LANGUAGE = "en"
        private const val JAPANESE_LANGUAGE = "ja"
    }
}
