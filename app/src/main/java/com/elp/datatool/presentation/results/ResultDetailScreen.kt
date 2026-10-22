package com.elp.datatool.presentation.results

import android.content.ClipData
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import coil3.compose.SubcomposeAsyncImage
import com.elp.datatool.domain.model.ProductAttributes
import com.elp.datatool.presentation.components.descriptionRes
import com.elp.datatool.presentation.components.labelRes
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elp.datatool.R
import com.elp.datatool.data.sample.SampleData
import com.elp.datatool.domain.model.ProductResult
import com.elp.datatool.domain.model.ResultStatus
import com.elp.datatool.presentation.components.BackButton
import com.elp.datatool.presentation.components.BottomActionBar
import com.elp.datatool.presentation.components.DtTopBar
import com.elp.datatool.presentation.components.EmptyState
import com.elp.datatool.presentation.components.StatusLabel
import com.elp.datatool.presentation.components.displayTitle
import com.elp.datatool.presentation.components.nameRes
import com.elp.datatool.presentation.components.productIdLabel
import com.elp.datatool.presentation.components.toShareText
import com.elp.datatool.ui.icons.DtIcons
import com.elp.datatool.ui.theme.CaptionTextStyle
import com.elp.datatool.ui.theme.DataToolTheme
import kotlinx.coroutines.launch

@Composable
fun ResultDetailRoute(
    viewModel: ResultDetailViewModel,
    onBack: () -> Unit,
) {
    val result by viewModel.result.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val uriHandler = LocalUriHandler.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    ResultDetailScreen(
        result = result,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onShare = { item ->
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, item.toShareText(context))
            }
            context.startActivity(Intent.createChooser(send, context.getString(R.string.share_chooser_title)))
        },
        onCopy = { item ->
            scope.launch {
                val text = item.toShareText(context)
                clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(item.displayKey(), text)))
                snackbarHostState.showSnackbar(context.getString(R.string.detail_copied))
            }
        },
        onOpenLink = { url ->
            try {
                uriHandler.openUri(url)
            } catch (_: IllegalArgumentException) {
                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.detail_no_app)) }
            }
        },
    )
}

@Composable
fun ResultDetailScreen(
    result: ProductResult?,
    onBack: () -> Unit,
    onShare: (ProductResult) -> Unit,
    onCopy: (ProductResult) -> Unit,
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            DtTopBar(
                title = stringResource(R.string.detail_title),
                centered = true,
                navigationIcon = { BackButton(onBack) },
                actions = {
                    if (result != null) {
                        IconButton(onClick = { onShare(result) }) {
                            Icon(DtIcons.Share, contentDescription = stringResource(R.string.action_share))
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (result != null) {
                DetailActions(
                    canOpenLink = result.canonicalUrl != null,
                    onCopy = { onCopy(result) },
                    onOpenLink = { result.canonicalUrl?.let(onOpenLink) },
                )
            }
        },
    ) { padding ->
        if (result == null) {
            EmptyState(
                icon = DtIcons.SearchX,
                title = stringResource(R.string.detail_not_found_title),
                message = stringResource(R.string.detail_not_found_body),
                actionLabel = stringResource(R.string.action_back),
                onAction = onBack,
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }
        DetailContent(
            result = result,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        )
    }
}

@Composable
private fun DetailContent(result: ProductResult, modifier: Modifier = Modifier) {
    val isError = result.status == ResultStatus.ERROR
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(
                    if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center,
        ) {
            val placeholder: @Composable () -> Unit = {
                Icon(
                    imageVector = if (isError) DtIcons.Alert else DtIcons.Image,
                    contentDescription = null,
                    tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp),
                )
            }
            val imageUrl = result.imageUrl.takeUnless { isError }
            if (imageUrl != null) {
                SubcomposeAsyncImage(
                    model = imageUrl,
                    contentDescription = result.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { placeholder() } },
                    error = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { placeholder() } },
                )
            } else {
                placeholder()
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(result.source.nameRes),
                style = CaptionTextStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            StatusLabel(result.status)
        }
        Text(result.displayTitle(), style = MaterialTheme.typography.titleLarge)
        Text(
            text = productIdLabel(result.source, result.productId),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )

        Spacer(Modifier.height(6.dp))
        // Empty fields are not shown.
        DetailSection(
            R.string.detail_error,
            result.error?.let { "${stringResource(it.labelRes)}. ${stringResource(it.descriptionRes)}" },
        )
        DetailSection(R.string.detail_publisher, result.creators.joinToString().ifEmpty { null })
        DetailSection(R.string.detail_category, result.categories.joinToString(" / ").ifEmpty { null })
        AttributesSection(result.attributes)
        DetailSection(R.string.detail_overview, result.description)
        DetailSection(R.string.detail_url, result.canonicalUrl)
    }
}

/** Display order and labels for known attributes. */
private val ATTRIBUTE_LABELS = listOf(
    ProductAttributes.PRICE to R.string.attr_price,
    ProductAttributes.RATING to R.string.attr_rating,
    ProductAttributes.VERSION to R.string.attr_version,
    ProductAttributes.LATEST_RELEASE to R.string.attr_latest_release,
    ProductAttributes.FIRST_PUBLISHED to R.string.attr_first_published,
    ProductAttributes.UNITY_VERSIONS to R.string.attr_unity_versions,
    ProductAttributes.FILE_SIZE to R.string.attr_file_size,
    ProductAttributes.ASSET_COUNT to R.string.attr_asset_count,
    ProductAttributes.TAGS to R.string.attr_tags,
)

@Composable
private fun AttributesSection(attributes: Map<String, String>) {
    val rows = ATTRIBUTE_LABELS.mapNotNull { (key, labelRes) ->
        val value = when (key) {
            ProductAttributes.PRICE -> if (attributes[ProductAttributes.IS_FREE] == "true") {
                stringResource(R.string.attr_price_free)
            } else {
                attributes[key]
            }

            ProductAttributes.RATING -> attributes[key]?.let { rating ->
                val count = attributes[ProductAttributes.RATING_COUNT]?.toIntOrNull() ?: 0
                pluralStringResource(R.plurals.attr_rating_value, count, rating, count)
            }

            else -> attributes[key]
        }
        value?.let { stringResource(labelRes) to it }
    }
    if (rows.isEmpty()) return

    Column(Modifier.padding(vertical = 11.dp)) {
        Text(stringResource(R.string.detail_details), style = MaterialTheme.typography.titleSmall)
        rows.forEach { (label, value) ->
            Row(Modifier.padding(top = 6.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(120.dp),
                )
                Text(text = value, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            }
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
}

@Composable
private fun DetailSection(titleRes: Int, body: String?) {
    if (body.isNullOrBlank()) return
    Column(Modifier.padding(vertical = 11.dp)) {
        Text(stringResource(titleRes), style = MaterialTheme.typography.titleSmall)
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
}

@Composable
private fun DetailActions(canOpenLink: Boolean, onCopy: () -> Unit, onOpenLink: () -> Unit) {
    BottomActionBar {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
            OutlinedButton(
                onClick = onCopy,
                shape = MaterialTheme.shapes.small,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 46.dp),
            ) {
                ButtonContent(DtIcons.Copy, stringResource(R.string.action_copy))
            }
            Button(
                onClick = onOpenLink,
                enabled = canOpenLink,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .weight(1.5f)
                    .heightIn(min = 46.dp),
            ) {
                ButtonContent(DtIcons.ExternalLink, stringResource(R.string.action_open_link))
            }
        }
    }
}

@Composable
private fun ButtonContent(icon: ImageVector, label: String) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
    Spacer(Modifier.width(6.dp))
    Text(label)
}

private fun ProductResult.displayKey(): String = "$productId (${source.id})"

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun ResultDetailScreenPreview() {
    DataToolTheme {
        ResultDetailScreen(
            result = SampleData.results.first(),
            onBack = {},
            onShare = {},
            onCopy = {},
            onOpenLink = {},
        )
    }
}
