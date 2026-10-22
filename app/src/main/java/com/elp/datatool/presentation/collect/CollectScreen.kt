package com.elp.datatool.presentation.collect

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elp.datatool.R
import com.elp.datatool.domain.model.DataSource
import com.elp.datatool.domain.model.ValidationResult
import com.elp.datatool.domain.model.parseInputLines
import com.elp.datatool.presentation.components.BottomActionBar
import com.elp.datatool.presentation.components.DtTopBar
import com.elp.datatool.presentation.components.FieldLabel
import com.elp.datatool.presentation.components.IconTile
import com.elp.datatool.presentation.components.invalidMessageRes
import com.elp.datatool.presentation.components.nameRes
import com.elp.datatool.presentation.components.productIdLabel
import com.elp.datatool.ui.icons.DtIcons
import com.elp.datatool.ui.theme.DataToolTheme
import kotlinx.coroutines.launch

private const val PREVIEW_LIMIT = 3

@Composable
fun CollectRoute(
    viewModel: CollectViewModel,
    onOpenSearch: () -> Unit,
    onViewResults: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val message = when (event) {
                CollectEvent.ClipboardEmpty -> context.getString(R.string.collect_clipboard_empty)
                is CollectEvent.SourceUnsupported -> context.getString(
                    R.string.msg_source_unsupported,
                    context.getString(event.source.nameRes),
                )
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    CollectScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onSourceChange = viewModel::onSourceChange,
        onInputChange = viewModel::onInputChange,
        onPaste = {
            scope.launch {
                val clipData = clipboard.getClipEntry()?.clipData
                val text = clipData?.takeIf { it.itemCount > 0 }
                    ?.getItemAt(0)?.coerceToText(context)?.toString()
                viewModel.onPaste(text)
            }
        },
        onOpenSearch = onOpenSearch,
        onClear = viewModel::onClear,
        onStart = viewModel::onStartProcessing,
        onCancel = viewModel::onCancelProcessing,
        onViewResults = {
            viewModel.onDismissSummary()
            onViewResults()
        },
    )
}

@Composable
fun CollectScreen(
    state: CollectUiState,
    onSourceChange: (DataSource) -> Unit,
    onInputChange: (String) -> Unit,
    onPaste: () -> Unit,
    onOpenSearch: () -> Unit,
    onClear: () -> Unit,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    onViewResults: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    var showHelp by rememberSaveable { mutableStateOf(false) }
    var showAllItems by rememberSaveable { mutableStateOf(false) }
    var confirmCancel by rememberSaveable { mutableStateOf(false) }
    val inputFocus = remember { FocusRequester() }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            DtTopBar(
                eyebrow = stringResource(R.string.eyebrow_data_tool),
                title = stringResource(R.string.collect_title),
                actions = {
                    IconButton(onClick = { showHelp = true }) {
                        Icon(DtIcons.Help, contentDescription = stringResource(R.string.collect_help))
                    }
                },
            )
        },
        bottomBar = {
            CollectBottomBar(
                state = state,
                onStart = onStart,
                onCancel = { confirmCancel = true },
                onViewResults = onViewResults,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            FieldLabel(stringResource(R.string.collect_source_label))
            SourceDropdown(selected = state.source, onSelect = onSourceChange)

            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                FieldLabel(stringResource(R.string.collect_input_label), Modifier.weight(1f))
                Text(
                    text = pluralStringResource(R.plurals.items_count, state.items.size, state.items.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            OutlinedTextField(
                value = state.input,
                onValueChange = onInputChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(inputFocus),
                placeholder = { Text(stringResource(R.string.collect_input_placeholder)) },
                minLines = 5,
                maxLines = 10,
                shape = MaterialTheme.shapes.small,
                keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, keyboardType = KeyboardType.Uri),
            )

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InputActionButton(DtIcons.Clipboard, stringResource(R.string.action_paste), onPaste, Modifier.weight(1f))
                InputActionButton(DtIcons.Search, stringResource(R.string.action_search), onOpenSearch, Modifier.weight(1f))
                InputActionButton(
                    icon = DtIcons.Trash,
                    label = stringResource(R.string.action_clear),
                    onClick = onClear,
                    modifier = Modifier.weight(1f),
                    enabled = state.input.isNotEmpty(),
                )
            }

            Spacer(Modifier.height(10.dp))
            ValidationBanner(state)

            if (state.items.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                QueuePreview(
                    state = state,
                    showAll = showAllItems,
                    onToggleShowAll = { showAllItems = !showAllItems },
                    onEdit = { inputFocus.requestFocus() },
                )
            }
        }
    }

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            confirmButton = {
                TextButton(onClick = { showHelp = false }) { Text(stringResource(R.string.action_ok)) }
            },
            title = { Text(stringResource(R.string.collect_help_title)) },
            text = { Text(stringResource(R.string.collect_help_body)) },
        )
    }

    if (confirmCancel) {
        AlertDialog(
            onDismissRequest = { confirmCancel = false },
            title = { Text(stringResource(R.string.collect_cancel_title)) },
            text = { Text(stringResource(R.string.collect_cancel_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmCancel = false
                    onCancel()
                }) {
                    Text(stringResource(R.string.collect_stop), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmCancel = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SourceDropdown(selected: DataSource, onSelect: (DataSource) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = stringResource(selected.nameRes),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            shape = MaterialTheme.shapes.small,
            trailingIcon = {
                Icon(
                    DtIcons.ChevronDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DataSource.entries.forEach { source ->
                DropdownMenuItem(
                    text = { Text(stringResource(source.nameRes)) },
                    onClick = {
                        onSelect(source)
                        expanded = false
                    },
                    contentPadding = PaddingValues(horizontal = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun InputActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 44.dp),
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        contentPadding = PaddingValues(horizontal = 8.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

@Composable
private fun ValidationBanner(state: CollectUiState) {
    val hasIssues = state.invalidCount > 0
    val text = when {
        state.items.isEmpty() -> buildAnnotatedString { append(stringResource(R.string.collect_validation_empty)) }
        else -> buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                append(stringResource(R.string.collect_ready, state.validCount))
            }
            append(" · ")
            append(
                if (hasIssues) {
                    pluralStringResource(R.plurals.collect_validation_attention, state.invalidCount, state.invalidCount)
                } else {
                    stringResource(R.string.collect_validation_all_valid)
                }
            )
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 42.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(9.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(8.dp)
                .background(
                    if (hasIssues) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    CircleShape,
                )
        )
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun QueuePreview(
    state: CollectUiState,
    showAll: Boolean,
    onToggleShowAll: () -> Unit,
    onEdit: () -> Unit,
) {
    // Items that need attention come first so they are visible in the preview.
    val ordered = remember(state.items) { state.items.sortedBy { it.isValid } }
    val visible = if (showAll) ordered else ordered.take(PREVIEW_LIMIT)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.collect_queue_preview),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f),
        )
        if (ordered.size > PREVIEW_LIMIT) {
            OutlinedButton(
                onClick = onToggleShowAll,
                shape = RoundedCornerShape(9.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                contentPadding = PaddingValues(horizontal = 10.dp),
                modifier = Modifier.height(36.dp),
            ) {
                Text(
                    text = stringResource(if (showAll) R.string.collect_show_less else R.string.collect_review_all),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    Surface(
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column {
            visible.forEachIndexed { index, item ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                QueueRow(item = item, source = state.source, onEdit = onEdit)
            }
        }
    }
}

@Composable
private fun QueueRow(item: QueueItem, source: DataSource, onEdit: () -> Unit) {
    val validation = item.validation
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (item.isValid) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.errorContainer
            )
            .heightIn(min = 54.dp)
            .padding(start = 10.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(if (item.isValid) DtIcons.Package else DtIcons.Alert)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = item.raw,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val subtitle = when (validation) {
                is ValidationResult.Valid -> if (validation.productId == item.raw) {
                    stringResource(source.nameRes)
                } else {
                    "${stringResource(source.nameRes)} · ${productIdLabel(source, validation.productId)}"
                }

                is ValidationResult.Invalid -> stringResource(source.invalidMessageRes)
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = if (item.isValid) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (item.isValid) {
            Text(
                text = stringResource(R.string.collect_status_ready),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 6.dp),
            )
        } else {
            TextButton(onClick = onEdit) {
                Text(stringResource(R.string.action_edit), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun CollectBottomBar(
    state: CollectUiState,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    onViewResults: () -> Unit,
) {
    val queue = state.queue
    BottomActionBar {
        when {
            queue.running -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BottomBarSummary(
                        label = stringResource(R.string.collect_processing),
                        value = stringResource(R.string.collect_processing_progress, queue.processed, queue.total),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedButton(
                        onClick = onCancel,
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.heightIn(min = 44.dp),
                    ) {
                        Text(stringResource(R.string.collect_cancel), style = MaterialTheme.typography.labelMedium)
                    }
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { if (queue.total == 0) 0f else queue.processed.toFloat() / queue.total },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            state.showSummary -> Row(verticalAlignment = Alignment.CenterVertically) {
                BottomBarSummary(
                    label = stringResource(if (queue.cancelled) R.string.collect_cancelled else R.string.collect_finished),
                    value = stringResource(R.string.collect_summary, queue.completed, queue.failed),
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = onViewResults,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.heightIn(min = 44.dp),
                ) {
                    Text(stringResource(R.string.collect_view_results), style = MaterialTheme.typography.labelMedium)
                }
            }

            else -> Row(verticalAlignment = Alignment.CenterVertically) {
                val validCount = state.validCount
                BottomBarSummary(
                    label = stringResource(R.string.collect_ready_to_process),
                    value = pluralStringResource(R.plurals.items_count, validCount, validCount),
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = onStart,
                    enabled = validCount > 0,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.heightIn(min = 44.dp),
                ) {
                    Text(
                        text = stringResource(if (validCount > 0) R.string.collect_start else R.string.collect_add_valid_items),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomBarSummary(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Preview(showBackground = true, heightDp = 760)
@Composable
private fun CollectScreenPreview() {
    val input = "96478\n36050\ninvalid-item"
    val source = DataSource.UNITY_ASSET_STORE
    DataToolTheme {
        CollectScreen(
            state = CollectUiState(
                source = source,
                input = input,
                items = parseInputLines(input).map { QueueItem(it, source.validate(it)) },
            ),
            onSourceChange = {},
            onInputChange = {},
            onPaste = {},
            onOpenSearch = {},
            onClear = {},
            onStart = {},
            onCancel = {},
            onViewResults = {},
        )
    }
}
