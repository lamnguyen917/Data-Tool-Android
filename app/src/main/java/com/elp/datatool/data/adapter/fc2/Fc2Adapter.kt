package com.elp.datatool.data.adapter.fc2

import com.elp.datatool.data.adapter.withBrowserChallenge
import com.elp.datatool.data.remote.HttpClient
import com.elp.datatool.domain.model.DataSource
import com.elp.datatool.domain.model.FetchErrorKind
import com.elp.datatool.domain.model.FetchException
import com.elp.datatool.domain.model.ProductResult
import com.elp.datatool.domain.model.SearchHit
import com.elp.datatool.domain.model.ValidationResult
import com.elp.datatool.domain.repository.SourceAdapter
import java.net.URI
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class Fc2Adapter(
    private val http: HttpClient,
    private val baseUrl: String = BASE_URL,
    private val parseDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : SourceAdapter {

    override val source = DataSource.FC2

    override suspend fun search(query: String): List<SearchHit> {
        throw FetchException(FetchErrorKind.UNSUPPORTED, "FC2 search requires authentication")
    }

    override suspend fun fetch(productId: String): ProductResult {
        val sku = when (val validation = DataSource.FC2.validate(productId)) {
            is ValidationResult.Valid -> validation.productId
            is ValidationResult.Invalid -> throw FetchException(FetchErrorKind.PARSE, "Invalid FC2 article ID")
        }
        val url = productUrl(sku)
        val (page, parsed) = withBrowserChallenge(url, FC2_DESKTOP_USER_AGENT) {
            val loadedPage = http.get(url, FC2_HEADERS)
            loadedPage to withContext(parseDispatcher) {
                Fc2Parser.parseProductPage(loadedPage.body, loadedPage.url, sku)
            }
        }
        val descriptionUrl = parsed.descriptionUrl
        if (descriptionUrl == null || !isTrustedDescriptionUrl(descriptionUrl, sku)) return parsed.result

        val description = try {
            val descriptionPage = http.get(descriptionUrl, FC2_HEADERS + ("Referer" to page.url))
            withContext(parseDispatcher) {
                Fc2Parser.parseDescription(descriptionPage.body, descriptionPage.url)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        }
        return if (description.isNullOrBlank()) parsed.result else parsed.result.copy(description = description)
    }

    internal fun productUrl(sku: String): String = "$baseUrl/article/$sku/?lang=ja"

    private fun isTrustedDescriptionUrl(value: String, sku: String): Boolean {
        val uri = runCatching { URI(value) }.getOrNull() ?: return false
        val baseUri = runCatching { URI(baseUrl) }.getOrNull() ?: return false
        return uri.scheme.equals(baseUri.scheme, ignoreCase = true) &&
            uri.host.equals(baseUri.host, ignoreCase = true) &&
            uri.path == "/widget/article/$sku/description"
    }

    companion object {
        const val BASE_URL = "https://adult.contents.fc2.com"
        private const val FC2_DESKTOP_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/130.0 Safari/537.36"
        private val FC2_HEADERS = mapOf("User-Agent" to FC2_DESKTOP_USER_AGENT)
    }
}
