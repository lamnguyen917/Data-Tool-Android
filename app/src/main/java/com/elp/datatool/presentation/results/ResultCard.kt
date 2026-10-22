package com.elp.datatool.presentation.results

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.elp.datatool.R
import com.elp.datatool.domain.model.ProductResult
import com.elp.datatool.domain.model.ResultStatus
import com.elp.datatool.presentation.components.StatusLabel
import com.elp.datatool.presentation.components.descriptionRes
import com.elp.datatool.presentation.components.displayTitle
import com.elp.datatool.presentation.components.labelRes
import com.elp.datatool.presentation.components.nameRes
import com.elp.datatool.presentation.components.shortLabelRes
import com.elp.datatool.ui.icons.DtIcons
import com.elp.datatool.ui.theme.CaptionTextStyle
import com.elp.datatool.ui.theme.EyebrowTextStyle

/** Visual result card: thumbnail, title, status, description, metadata and footer. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ResultCard(
    result: ProductResult,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRetry: () -> Unit,
    onCopyLine: () -> Unit,
    onCopyDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isError = result.status == ResultStatus.ERROR
    val shape = MaterialTheme.shapes.medium
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        border = if (selected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        },
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .combinedClickable(
                onClickLabel = stringResource(R.string.results_open_details),
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .semantics { if (selectionMode) this.selected = selected },
    ) {
        Column {
            Thumbnail(
                imageUrl = result.imageUrl.takeUnless { isError },
                isError = isError,
                label = if (isError) stringResource(R.string.status_error).uppercase() else stringResource(result.source.shortLabelRes),
                selectionMode = selectionMode,
                selected = selected,
            )
            Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 11.dp, bottom = 4.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(result.source.nameRes),
                            style = CaptionTextStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = result.displayTitle(),
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    StatusLabel(result.status)
                }

                val description = when {
                    isError -> result.error?.let { stringResource(it.descriptionRes) }
                    result.status == ResultStatus.PROCESSING && result.title == null ->
                        stringResource(R.string.results_processing_description)
                    else -> result.description
                }
                if (description != null) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 7.dp),
                    )
                }

                if (!isError && result.title != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        MetadataItem(DtIcons.Hash, result.productId)
                        result.creators.firstOrNull()?.let { MetadataItem(DtIcons.User, it, Modifier.weight(1f, fill = false)) }
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 9.dp),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.heightIn(min = 40.dp),
                ) {
                    Text(
                        text = if (isError) {
                            result.error?.let { stringResource(it.labelRes) }.orEmpty()
                        } else {
                            result.categories.joinToString(" / ")
                        },
                        style = CaptionTextStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (isError && result.error?.canRetry != false) {
                        TextButton(onClick = onRetry) {
                            Icon(DtIcons.Retry, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(stringResource(R.string.action_retry), style = MaterialTheme.typography.labelMedium)
                        }
                    } else {
                        Icon(
                            DtIcons.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(16.dp),
                        )
                    }
                }

                // Copy actions are hidden in selection mode, where the selection bar copies instead.
                if (result.status == ResultStatus.COMPLETED && !selectionMode) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 8.dp),
                    ) {
                        CopyButton(DtIcons.Copy, stringResource(R.string.action_copy_line), onCopyLine, Modifier.weight(1f))
                        CopyButton(DtIcons.FileText, stringResource(R.string.action_copy_details), onCopyDetails, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CopyButton(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 40.dp),
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        contentPadding = PaddingValues(horizontal = 8.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

@Composable
private fun Thumbnail(
    imageUrl: String?,
    isError: Boolean,
    label: String,
    selectionMode: Boolean,
    selected: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp)
            .background(
                if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
            ),
    ) {
        if (imageUrl != null) {
            SubcomposeAsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { ThumbnailPlaceholder(isError = false, label = label) },
                error = { ThumbnailPlaceholder(isError = false, label = label) },
            )
        } else {
            ThumbnailPlaceholder(isError = isError, label = label)
        }
        if (selectionMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .size(34.dp)
                    .shadow(4.dp, CircleShape)
                    .background(MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                // The card itself handles the toggle; the checkbox is visual only.
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                    Checkbox(checked = selected, onCheckedChange = null)
                }
            }
        }
    }
}

/** Icon and source label shown while there is no image. */
@Composable
private fun ThumbnailPlaceholder(isError: Boolean, label: String) {
    val tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = if (isError) DtIcons.Alert else DtIcons.Image,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.height(3.dp))
        Text(text = label, style = EyebrowTextStyle.copy(fontSize = CaptionTextStyle.fontSize), color = tint)
    }
}

@Composable
private fun MetadataItem(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(11.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = text,
            style = CaptionTextStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
