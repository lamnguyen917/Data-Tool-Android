package com.elp.datatool.presentation.results

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elp.datatool.R
import com.elp.datatool.data.sample.SampleData
import com.elp.datatool.domain.model.ProductResult
import com.elp.datatool.domain.model.ResultStatus
import com.elp.datatool.presentation.components.BottomActionBar
import com.elp.datatool.presentation.components.DtTopBar
import com.elp.datatool.presentation.components.EmptyState
import com.elp.datatool.presentation.components.copyText
import com.elp.datatool.presentation.components.shouldConfirmCopy
import com.elp.datatool.presentation.components.toShareText
import com.elp.datatool.ui.icons.DtIcons
import com.elp.datatool.ui.theme.CaptionTextStyle
import com.elp.datatool.ui.theme.DataToolTheme

@Composable
fun ResultsRoute(
    viewModel: ResultsViewModel,
    onOpenDetail: (String) -> Unit,
    onGoToCollect: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val clipboard = LocalClipboard.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ResultsEvent.Copy -> {
                    clipboard.copyText(context.getString(R.string.app_name), event.text)
                    if (shouldConfirmCopy) snackbarHostState.showSnackbar(
                        when {
                            event.kind == CopyKind.DETAILS -> context.getString(R.string.copied_details)
                            event.count > 1 -> context.resources.getQuantityString(
                                R.plurals.copied_lines, event.count, event.count
                            )
                            else -> context.getString(R.string.copied_line)
                        }
                    )
                }

                is ResultsEvent.Deleted -> snackbarHostState.showSnackbar(
                    context.resources.getQuantityString(R.plurals.results_deleted, event.count, event.count)
                )

                is ResultsEvent.Share -> context.shareText(
                    event.results.joinToString("\n\n") { it.toShareText(context) }
                )

                ResultsEvent.ExportUnavailable ->
                    snackbarHostState.showSnackbar(context.getString(R.string.msg_export_unavailable))
            }
        }
    }

    BackHandler(enabled = state.selectionMode, onBack = viewModel::onExitSelection)

    ResultsScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onQueryChange = viewModel::onQueryChange,
        onFilterChange = viewModel::onFilterChange,
        onToggleSort = viewModel::onToggleSort,
        onToggleSelectionMode = viewModel::onToggleSelectionMode,
        onSelectAll = viewModel::onSelectAll,
        onCardClick = { key ->
            if (state.selectionMode) viewModel.onToggleSelection(key) else onOpenDetail(key)
        },
        onCardLongClick = viewModel::onLongPress,
        onRetry = viewModel::onRetry,
        onCopyLine = viewModel::onCopyLine,
        onCopyDetails = viewModel::onCopyDetails,
        onCopySelected = viewModel::onCopySelectedLines,
        onShare = viewModel::onShareSelected,
        onExport = viewModel::onExportSelected,
        onDelete = viewModel::onDeleteSelected,
        onGoToCollect = onGoToCollect,
    )
}

@Composable
fun ResultsScreen(
    state: ResultsUiState,
    onQueryChange: (String) -> Unit,
    onFilterChange: (ResultFilter) -> Unit,
    onToggleSort: () -> Unit,
    onToggleSelectionMode: () -> Unit,
    onSelectAll: () -> Unit,
    onCardClick: (String) -> Unit,
    onCardLongClick: (String) -> Unit,
    onRetry: (String) -> Unit,
    onCopyLine: (String) -> Unit,
    onCopyDetails: (String) -> Unit,
    onCopySelected: () -> Unit,
    onShare: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
    onGoToCollect: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            DtTopBar(
                eyebrow = stringResource(R.string.eyebrow_data_tool),
                title = stringResource(R.string.results_title),
                actions = {
                    if (state.totalCount > 0) {
                        if (state.selectionMode) {
                            TextButton(onClick = onSelectAll) {
                                Text(
                                    text = stringResource(R.string.action_select_all),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                        TextButton(onClick = onToggleSelectionMode) {
                            Text(
                                text = stringResource(if (state.selectionMode) R.string.action_done else R.string.action_select),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (state.selectionMode) {
                SelectionBar(
                    count = state.selectedKeys.size,
                    onCopy = onCopySelected,
                    onShare = onShare,
                    onExport = onExport,
                    onDelete = { confirmDelete = true },
                )
            }
        },
    ) { padding ->
        if (state.totalCount == 0) {
            EmptyState(
                icon = DtIcons.LayoutList,
                title = stringResource(R.string.results_no_data_title),
                message = stringResource(R.string.results_no_data_body),
                actionLabel = stringResource(R.string.results_go_collect),
                onAction = onGoToCollect,
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "controls") {
                ResultControls(
                    state = state,
                    onQueryChange = onQueryChange,
                    onFilterChange = onFilterChange,
                    onToggleSort = onToggleSort,
                )
            }
            if (state.visibleResults.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        icon = DtIcons.SearchX,
                        title = stringResource(R.string.results_empty_title),
                        message = stringResource(R.string.results_empty_body),
                    )
                }
            }
            items(state.visibleResults, key = { it.key }) { result ->
                ResultCard(
                    result = result,
                    selectionMode = state.selectionMode,
                    selected = result.key in state.selectedKeys,
                    onClick = { onCardClick(result.key) },
                    onLongClick = { onCardLongClick(result.key) },
                    onRetry = { onRetry(result.key) },
                    onCopyLine = { onCopyLine(result.key) },
                    onCopyDetails = { onCopyDetails(result.key) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }

    if (confirmDelete) {
        val count = state.selectedKeys.size
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.results_delete_title)) },
            text = { Text(pluralStringResource(R.plurals.results_delete_body, count, count)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun ResultControls(
    state: ResultsUiState,
    onQueryChange: (String) -> Unit,
    onFilterChange: (ResultFilter) -> Unit,
    onToggleSort: () -> Unit,
) {
    Column {
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(11.dp),
            placeholder = { Text(stringResource(R.string.results_search_placeholder)) },
            leadingIcon = {
                Icon(
                    DtIcons.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            },
            trailingIcon = {
                if (state.query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            DtIcons.Close,
                            contentDescription = stringResource(R.string.search_clear),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            modifier = Modifier.padding(top = 6.dp),
        ) {
            StatusChip(R.string.filter_all, state.totalCount, state.filter == ResultFilter.ALL) {
                onFilterChange(ResultFilter.ALL)
            }
            StatusChip(R.string.filter_completed, state.completedCount, state.filter == ResultFilter.COMPLETED) {
                onFilterChange(ResultFilter.COMPLETED)
            }
            StatusChip(R.string.filter_errors, state.errorCount, state.filter == ResultFilter.ERRORS) {
                onFilterChange(ResultFilter.ERRORS)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            val count = state.visibleResults.size
            Text(
                text = pluralStringResource(R.plurals.results_count, count, count),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onToggleSort) {
                Icon(
                    DtIcons.Sort,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = stringResource(if (state.newestFirst) R.string.sort_newest else R.string.sort_oldest),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatusChip(labelRes: Int, count: Int, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(stringResource(R.string.filter_label, stringResource(labelRes), count)) },
        shape = RoundedCornerShape(18.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outline,
            selectedBorderColor = MaterialTheme.colorScheme.primary,
        ),
    )
}

@Composable
private fun SelectionBar(
    count: Int,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
) {
    BottomActionBar {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.results_selected, count),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f),
            )
            SelectionAction(DtIcons.Copy, stringResource(R.string.action_copy), count > 0, onCopy)
            SelectionAction(DtIcons.Share, stringResource(R.string.action_share), count > 0, onShare)
            SelectionAction(DtIcons.Download, stringResource(R.string.action_export), count > 0, onExport)
            SelectionAction(DtIcons.Trash, stringResource(R.string.action_delete), count > 0, onDelete)
        }
    }
}

@Composable
private fun SelectionAction(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    val color = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .widthIn(min = 56.dp)
            .heightIn(min = 52.dp)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Text(label, style = CaptionTextStyle, color = color, fontWeight = FontWeight.Medium)
    }
}

private fun Context.shareText(text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    startActivity(Intent.createChooser(send, getString(R.string.share_chooser_title)))
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun ResultsScreenPreview() {
    val results: List<ProductResult> = SampleData.results
    DataToolTheme {
        ResultsScreen(
            state = ResultsUiState(
                visibleResults = results,
                totalCount = results.size,
                completedCount = results.count { it.status == ResultStatus.COMPLETED },
                errorCount = results.count { it.status == ResultStatus.ERROR },
            ),
            onQueryChange = {},
            onFilterChange = {},
            onToggleSort = {},
            onToggleSelectionMode = {},
            onSelectAll = {},
            onCardClick = {},
            onCardLongClick = {},
            onRetry = {},
            onCopyLine = {},
            onCopyDetails = {},
            onCopySelected = {},
            onShare = {},
            onExport = {},
            onDelete = {},
            onGoToCollect = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun ResultsSelectionPreview() {
    val error = SampleData.results.last()
    DataToolTheme(darkTheme = true) {
        ResultsScreen(
            state = ResultsUiState(
                filter = ResultFilter.ERRORS,
                selectionMode = true,
                selectedKeys = setOf(error.key),
                visibleResults = listOf(error),
                totalCount = 4,
                completedCount = 3,
                errorCount = 1,
            ),
            onQueryChange = {},
            onFilterChange = {},
            onToggleSort = {},
            onToggleSelectionMode = {},
            onSelectAll = {},
            onCardClick = {},
            onCardLongClick = {},
            onRetry = {},
            onCopyLine = {},
            onCopyDetails = {},
            onCopySelected = {},
            onShare = {},
            onExport = {},
            onDelete = {},
            onGoToCollect = {},
        )
    }
}
