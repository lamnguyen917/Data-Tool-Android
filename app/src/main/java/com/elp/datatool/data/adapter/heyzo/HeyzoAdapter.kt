package com.elp.datatool.data.adapter.heyzo

import com.elp.datatool.data.adapter.withBrowserChallenge
import com.elp.datatool.data.remote.HttpClient
import com.elp.datatool.domain.model.DataSource
import com.elp.datatool.domain.model.FetchErrorKind
import com.elp.datatool.domain.model.FetchException
import com.elp.datatool.domain.model.ProductResult
import com.elp.datatool.domain.model.SearchHit
import com.elp.datatool.domain.model.ValidationResult
import com.elp.datatool.domain.repository.SourceAdapter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HeyzoAdapter(
    private val http: HttpClient,
    private val japaneseBaseUrl: String = JAPANESE_BASE_URL,
    private val englishBaseUrl: String = ENGLISH_BASE_URL,
    private val parseDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : SourceAdapter {

    override val source = DataSource.HEYZO

    override suspend fun search(query: String): List<SearchHit> {
        throw FetchException(FetchErrorKind.UNSUPPORTED, "Heyzo keyword search is not supported")
    }

    override suspend fun fetch(productId: String): ProductResult {
        val sku = when (val validation = DataSource.HEYZO.validate(productId)) {
            is ValidationResult.Valid -> validation.productId
            is ValidationResult.Invalid -> throw FetchException(FetchErrorKind.PARSE, "Invalid Heyzo SKU or product URL")
        }

        val englishUrl = englishProductUrl(sku)
        var result = withBrowserChallenge(englishUrl, DESKTOP_USER_AGENT) {
            val englishPage = http.get(englishUrl, ENGLISH_HEADERS)
            withContext(parseDispatcher) {
                HeyzoParser.parseEnglishProduct(englishPage.body, englishPage.url, sku)
            }
        }

        val japanese = try {
            val japanesePage = http.get(japaneseProductUrl(sku), JAPANESE_HEADERS)
            withContext(parseDispatcher) {
                HeyzoParser.parseJapaneseProduct(japanesePage.body, japanesePage.url)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        }

        if (japanese != null) {
            result = result.copy(
                attributes = result.attributes + japanese.sharedAttributes,
                localized = mapOf(JAPANESE_LANGUAGE to japanese.metadata),
            )
        }
        return result
    }

    internal fun englishProductUrl(sku: String): String =
        "${englishBaseUrl.trimEnd('/')}/moviepages/$sku/index.html"

    internal fun japaneseProductUrl(sku: String): String =
        "${japaneseBaseUrl.trimEnd('/')}/moviepages/$sku/index.html"

    companion object {
        const val JAPANESE_BASE_URL = "https://www.heyzo.com"
        const val ENGLISH_BASE_URL = "https://en.heyzo.com"
        private const val JAPANESE_LANGUAGE = "ja"
        private const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/130.0 Safari/537.36"
        private val ENGLISH_HEADERS = mapOf(
            "User-Agent" to DESKTOP_USER_AGENT,
            "Accept-Language" to "en-US,en;q=0.9",
        )
        private val JAPANESE_HEADERS = mapOf(
            "User-Agent" to DESKTOP_USER_AGENT,
            "Accept-Language" to "ja-JP,ja;q=0.9,en;q=0.7",
        )
    }
}
