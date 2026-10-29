package com.elp.datatool.data.adapter.caribbeancom

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

/**
 * Collects Caribbeancom products by `MMDDYY-NNN` SKU. The English page is required;
 * the Japanese page (served as EUC-JP) is optional localization.
 */
class CaribbeancomAdapter(
    private val http: HttpClient,
    private val japaneseBaseUrl: String = JAPANESE_BASE_URL,
    private val englishBaseUrl: String = ENGLISH_BASE_URL,
    private val parseDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : SourceAdapter {

    override val source = DataSource.CARIBBEANCOM

    override suspend fun search(query: String): List<SearchHit> {
        throw FetchException(FetchErrorKind.UNSUPPORTED, "Caribbeancom keyword search is not supported")
    }

    override suspend fun fetch(productId: String): ProductResult {
        val sku = when (val validation = DataSource.CARIBBEANCOM.validate(productId)) {
            is ValidationResult.Valid -> validation.productId
            is ValidationResult.Invalid ->
                throw FetchException(FetchErrorKind.PARSE, "Invalid Caribbeancom SKU or product URL")
        }

        val englishUrl = englishProductUrl(sku)
        val result = withBrowserChallenge(englishUrl) {
            val englishPage = http.get(englishUrl, ENGLISH_HEADERS)
            withContext(parseDispatcher) {
                CaribbeancomParser.parseEnglishProduct(englishPage.body, englishPage.url, sku)
            }
        }

        val japanese = try {
            val japanesePage = http.get(japaneseProductUrl(sku), JAPANESE_HEADERS)
            withContext(parseDispatcher) {
                CaribbeancomParser.parseJapaneseProduct(japanesePage.body, japanesePage.url)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        }
        return if (japanese != null) result.copy(localized = mapOf(JAPANESE_LANGUAGE to japanese)) else result
    }

    internal fun englishProductUrl(sku: String): String =
        "${englishBaseUrl.trimEnd('/')}/eng/moviepages/$sku/index.html"

    internal fun japaneseProductUrl(sku: String): String =
        "${japaneseBaseUrl.trimEnd('/')}/moviepages/$sku/index.html"

    companion object {
        const val JAPANESE_BASE_URL = "https://www.caribbeancom.com"
        const val ENGLISH_BASE_URL = "https://en.caribbeancom.com"
        private const val JAPANESE_LANGUAGE = "ja"
        private val ENGLISH_HEADERS = mapOf("Accept-Language" to "en-US,en;q=0.9")
        private val JAPANESE_HEADERS = mapOf("Accept-Language" to "ja-JP,ja;q=0.9,en;q=0.7")
    }
}
