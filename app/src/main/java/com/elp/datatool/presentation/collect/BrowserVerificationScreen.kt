package com.elp.datatool.presentation.collect

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elp.datatool.R
import com.elp.datatool.data.remote.HttpClient
import com.elp.datatool.domain.model.VerifiedPage
import com.elp.datatool.presentation.components.BackButton
import com.elp.datatool.presentation.components.DtTopBar
import com.elp.datatool.presentation.components.nameRes
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive

/**
 * Opens the site's challenge page so the user can complete it (for example reCAPTCHA).
 * The app never interacts with the challenge; it only reads the results page afterwards.
 */
@Composable
fun BrowserVerificationRoute(
    viewModel: CollectViewModel,
    onBack: () -> Unit,
) {
    val collectState by viewModel.uiState.collectAsStateWithLifecycle()
    // Keep the first URL: the ViewModel clears it once the verified page is submitted.
    val challengeUrl = remember { viewModel.searchState.value.challengeUrl }
    if (challengeUrl == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    BrowserVerificationScreen(
        url = challengeUrl,
        sourceName = stringResource(collectState.source.nameRes),
        onBack = onBack,
        onVerified = { page ->
            viewModel.onVerifiedPage(page)
            onBack()
        },
    )
}

@Composable
fun BrowserVerificationScreen(
    url: String,
    sourceName: String,
    onBack: () -> Unit,
    onVerified: (VerifiedPage) -> Unit,
    modifier: Modifier = Modifier,
) {
    var progress by remember { mutableIntStateOf(0) }
    val currentOnVerified by rememberUpdatedState(onVerified)

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            DtTopBar(
                title = stringResource(R.string.verify_title),
                subtitle = sourceName,
                navigationIcon = { BackButton(onBack) },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Text(
                text = stringResource(R.string.verify_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (progress in 1..99) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        configure(
                            targetUrl = url,
                            onProgress = { progress = it },
                            onVerified = { page -> currentOnVerified(page) },
                        )
                        loadUrl(url)
                    }
                },
                onRelease = { webView ->
                    webView.stopLoading()
                    webView.destroy()
                },
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun WebView.configure(
    targetUrl: String,
    onProgress: (Int) -> Unit,
    onVerified: (VerifiedPage) -> Unit,
) {
    val target = Uri.parse(targetUrl)
    var submitted = false

    // reCAPTCHA needs JavaScript and DOM storage. Share the HTTP user agent so the
    // imported cookies match the session the HTTP client presents.
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    settings.userAgentString = HttpClient.USER_AGENT
    CookieManager.getInstance().apply {
        setAcceptCookie(true)
        setAcceptThirdPartyCookies(this@configure, true)
    }

    webChromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView, newProgress: Int) = onProgress(newProgress)
    }
    webViewClient = object : WebViewClient() {
        // Keep top-level navigation on the source site; reCAPTCHA itself runs in sub-frames.
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            if (!request.isForMainFrame) return false
            val uri = request.url
            return !(uri.scheme.equals("https", ignoreCase = true) &&
                uri.host.equals(target.host, ignoreCase = true))
        }

        override fun onPageFinished(view: WebView, url: String) {
            if (submitted || !isTargetPage(Uri.parse(url), target)) return
            view.evaluateJavascript(READ_HTML_SCRIPT) { raw ->
                if (submitted) return@evaluateJavascript
                val html = runCatching { Json.parseToJsonElement(raw).jsonPrimitive.content }.getOrNull()
                if (html.isNullOrBlank()) return@evaluateJavascript
                submitted = true
                val cookies = CookieManager.getInstance().getCookie(url).orEmpty()
                onVerified(VerifiedPage(url = url, html = html, cookieHeader = cookies))
            }
        }
    }
}

/** The challenge redirects back to the original page once the user passes it. */
private fun isTargetPage(current: Uri, target: Uri): Boolean =
    current.scheme.equals(target.scheme, ignoreCase = true) &&
        current.host.equals(target.host, ignoreCase = true) &&
        current.path.equals(target.path, ignoreCase = true)

private const val READ_HTML_SCRIPT = "document.documentElement.outerHTML"
