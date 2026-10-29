package com.elp.datatool.data.search

import com.elp.datatool.data.remote.HttpClient
import com.elp.datatool.domain.model.DataSource
import com.elp.datatool.domain.model.SearchHit
import com.elp.datatool.domain.model.ValidationResult
import com.elp.datatool.domain.repository.SearchEngine
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import org.jsoup.Jsoup

/** Optional public web-search fallback used only when native adapter search finds nothing. */
class DuckDuckGoSearchEngine(
    private val http: HttpClient,
    private val baseUrl: String = BASE_URL,
) : SearchEngine {

    override suspend fun search(source: DataSource, query: String): List<SearchHit> {
        val scope = source.searchScope ?: return emptyList()
        val encodedQuery = URLEncoder.encode("site:$scope ${query.trim()}", Charsets.UTF_8.name())
        val page = http.get("${baseUrl.trimEnd('/')}/?q=$encodedQuery")
        return DuckDuckGoSearchParser.parse(page.body, source)
    }

    private val DataSource.searchScope: String?
        get() = when (this) {
            DataSource.UNITY_ASSET_STORE -> "assetstore.unity.com/packages"
            DataSource.AMAZON_BOOKS -> "amazon.com"
            DataSource.STEAM_STORE -> "store.steampowered.com/app"
            DataSource.IGDB -> "igdb.com/games"
            DataSource.GOG -> "gog.com"
        }

    private companion object {
        const val BASE_URL = "https://html.duckduckgo.com/html/"
    }
}

internal object DuckDuckGoSearchParser {
    private const val MAX_RESULTS = 20
    private val AMAZON_HOST = Regex(
        """^(?:www\.)?amazon\.(?:com(?:\.[a-z]{2})?|co\.[a-z]{2}|[a-z]{2,3})$""",
        RegexOption.IGNORE_CASE,
    )

    fun parse(html: String, source: DataSource): List<SearchHit> =
        Jsoup.parse(html)
            .select("a.result__a, a[data-testid=result-title-a]")
            .mapNotNull { link ->
                val url = unwrapResultUrl(link.attr("href")) ?: return@mapNotNull null
                if (!isTrustedHost(source, url)) return@mapNotNull null
                val validation = source.validate(url) as? ValidationResult.Valid ?: return@mapNotNull null
                SearchHit(
                    source = source,
                    productId = validation.productId,
                    title = link.text().trim().ifEmpty { validation.productId },
                )
            }
            .distinctBy(SearchHit::productId)
            .take(MAX_RESULTS)

    private fun unwrapResultUrl(value: String): String? {
        if (value.isBlank()) return null
        val absolute = when {
            value.startsWith("//") -> "https:$value"
            value.startsWith("/") -> "https://duckduckgo.com$value"
            else -> value
        }
        val uri = runCatching { URI(absolute) }.getOrNull() ?: return null
        val host = uri.host.orEmpty()
        val isDuckDuckGo = host.equals("duckduckgo.com", ignoreCase = true) ||
            host.endsWith(".duckduckgo.com", ignoreCase = true)
        if (!isDuckDuckGo) return absolute
        val encodedTarget = uri.rawQuery.orEmpty()
            .split('&')
            .firstOrNull { it.substringBefore('=') == "uddg" }
            ?.substringAfter('=', "")
            ?.takeIf(String::isNotEmpty)
            ?: return null
        return runCatching { URLDecoder.decode(encodedTarget, Charsets.UTF_8.name()) }.getOrNull()
    }

    private fun isTrustedHost(source: DataSource, url: String): Boolean {
        val host = runCatching { URI(url).host?.lowercase() }.getOrNull() ?: return false
        return when (source) {
            DataSource.UNITY_ASSET_STORE -> host == "assetstore.unity.com"
            DataSource.AMAZON_BOOKS -> AMAZON_HOST.matches(host)
            DataSource.STEAM_STORE -> host == "store.steampowered.com"
            DataSource.IGDB -> host == "igdb.com" || host == "www.igdb.com"
            DataSource.GOG -> host == "gog.com" || host == "www.gog.com"
        }
    }
}
