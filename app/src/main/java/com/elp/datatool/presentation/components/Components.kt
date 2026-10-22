package com.elp.datatool.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.elp.datatool.R
import com.elp.datatool.domain.model.ResultStatus
import com.elp.datatool.ui.icons.DtIcons
import com.elp.datatool.ui.theme.CaptionTextStyle
import com.elp.datatool.ui.theme.DataToolTheme
import com.elp.datatool.ui.theme.EyebrowTextStyle

/** Top app bar with an optional eyebrow caption, followed by a divider. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DtTopBar(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    subtitle: String? = null,
    centered: Boolean = false,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surface,
        scrolledContainerColor = MaterialTheme.colorScheme.surface,
    )
    val titleContent: @Composable () -> Unit = {
        Column {
            if (eyebrow != null) {
                Text(
                    text = eyebrow,
                    style = EyebrowTextStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    Column(modifier) {
        if (centered) {
            CenterAlignedTopAppBar(
                title = titleContent,
                navigationIcon = navigationIcon,
                actions = actions,
                colors = colors,
            )
        } else {
            TopAppBar(
                title = titleContent,
                navigationIcon = navigationIcon,
                actions = actions,
                colors = colors,
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun BackButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(DtIcons.ArrowLeft, contentDescription = stringResource(R.string.action_back))
    }
}

/** Bottom bar with a top divider. Respects navigation bar and IME insets. */
@Composable
fun BottomActionBar(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
            .imePadding()
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            content = content,
        )
    }
}

/** Small medium-weight label placed above an input field. */
@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        modifier = modifier.padding(bottom = 6.dp),
    )
}

/** Rounded square with a muted icon, used as a leading element in list rows. */
@Composable
fun IconTile(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Int = 32,
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
    }
}

/** Status shown with both an icon and a label so it never relies on color alone. */
@Composable
fun StatusLabel(status: ResultStatus, modifier: Modifier = Modifier) {
    when (status) {
        ResultStatus.PROCESSING -> StatusRow(
            label = stringResource(R.string.status_processing),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        ) { color ->
            CircularProgressIndicator(modifier = Modifier.size(10.dp), strokeWidth = 1.5.dp, color = color)
        }

        ResultStatus.COMPLETED -> StatusRow(
            label = stringResource(R.string.status_completed),
            color = DataToolTheme.extendedColors.success,
            modifier = modifier,
        ) { color ->
            Icon(DtIcons.Check, contentDescription = null, tint = color, modifier = Modifier.size(11.dp))
        }

        ResultStatus.ERROR -> StatusRow(
            label = stringResource(R.string.status_error),
            color = MaterialTheme.colorScheme.error,
            modifier = modifier,
        ) { color ->
            Icon(DtIcons.Alert, contentDescription = null, tint = color, modifier = Modifier.size(11.dp))
        }
    }
}

@Composable
private fun StatusRow(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    leading: @Composable (Color) -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        leading(color)
        Text(
            text = label,
            style = CaptionTextStyle,
            color = color,
            fontWeight = MaterialTheme.typography.labelMedium.fontWeight,
        )
    }
}

/** Centered empty, idle or error message with an optional action. */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.height(10.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 280.dp),
        )
        if (actionLabel != null) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAction, shape = MaterialTheme.shapes.small) {
                Text(actionLabel)
            }
        }
    }
}
