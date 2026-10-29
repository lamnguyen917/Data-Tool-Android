package com.elp.datatool.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elp.datatool.BuildConfig
import com.elp.datatool.R
import com.elp.datatool.domain.copy.CopyFormatter
import com.elp.datatool.domain.copy.LineFormat
import com.elp.datatool.domain.model.AppSettings
import com.elp.datatool.domain.model.DataSource
import com.elp.datatool.domain.model.ThemeMode
import com.elp.datatool.presentation.components.DtTopBar
import com.elp.datatool.presentation.components.labelRes
import com.elp.datatool.presentation.components.nameRes
import com.elp.datatool.ui.theme.DataToolTheme
import com.elp.datatool.ui.theme.EyebrowTextStyle
import kotlin.math.roundToInt

private val TIMEOUT_OPTIONS = listOf(15, 30, 60)
private const val MIN_PARALLEL = 1
private const val MAX_PARALLEL = 8

private enum class PendingConfirm { CLEAR_CACHE, CLEAR_HISTORY }

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
    onOpenCopyLine: () -> Unit,
    onOpenCopyDetails: () -> Unit,
    onOpenAdapterSettings: (DataSource) -> Unit,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val message = when (event) {
                SettingsEvent.CacheCleared -> R.string.settings_cache_cleared
                SettingsEvent.HistoryCleared -> R.string.settings_history_cleared
                is SettingsEvent.AdapterDataCleared -> R.string.settings_adapter_data_cleared
            }
            snackbarHostState.showSnackbar(
                if (event is SettingsEvent.AdapterDataCleared) {
                    context.getString(message, context.getString(event.source.nameRes))
                } else {
                    context.getString(message)
                }
            )
        }
    }

    SettingsScreen(
        settings = settings,
        adapterSources = viewModel.adapterSources,
        snackbarHostState = snackbarHostState,
        onThemeModeChange = viewModel::onThemeModeChange,
        onParallelRequestsChange = viewModel::onParallelRequestsChange,
        onTimeoutChange = viewModel::onTimeoutChange,
        onAutoRetryChange = viewModel::onAutoRetryChange,
        onClearCache = viewModel::onClearCache,
        onClearHistory = viewModel::onClearHistory,
        onOpenCopyLine = onOpenCopyLine,
        onOpenCopyDetails = onOpenCopyDetails,
        onOpenAdapterSettings = onOpenAdapterSettings,
    )
}

@Composable
fun SettingsScreen(
    settings: AppSettings,
    adapterSources: List<DataSource>,
    onThemeModeChange: (ThemeMode) -> Unit,
    onParallelRequestsChange: (Int) -> Unit,
    onTimeoutChange: (Int) -> Unit,
    onAutoRetryChange: (Boolean) -> Unit,
    onClearCache: () -> Unit,
    onClearHistory: () -> Unit,
    onOpenCopyLine: () -> Unit,
    onOpenCopyDetails: () -> Unit,
    onOpenAdapterSettings: (DataSource) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    var pendingConfirm by rememberSaveable { mutableStateOf<PendingConfirm?>(null) }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            DtTopBar(
                eyebrow = stringResource(R.string.eyebrow_data_tool),
                title = stringResource(R.string.settings_title),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp)
        ) {
            SectionHeader(R.string.settings_appearance)
            Column(Modifier.selectableGroup()) {
                ThemeOption(R.string.theme_system, settings.themeMode == ThemeMode.SYSTEM) {
                    onThemeModeChange(ThemeMode.SYSTEM)
                }
                ThemeOption(R.string.theme_light, settings.themeMode == ThemeMode.LIGHT) {
                    onThemeModeChange(ThemeMode.LIGHT)
                }
                ThemeOption(R.string.theme_dark, settings.themeMode == ThemeMode.DARK) {
                    onThemeModeChange(ThemeMode.DARK)
                }
            }

            SectionHeader(R.string.settings_processing)
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                SettingText(
                    title = stringResource(R.string.settings_parallel),
                    description = stringResource(R.string.settings_parallel_value, settings.parallelRequests),
                )
                Slider(
                    value = settings.parallelRequests.toFloat(),
                    onValueChange = { onParallelRequestsChange(it.roundToInt()) },
                    valueRange = MIN_PARALLEL.toFloat()..MAX_PARALLEL.toFloat(),
                    steps = MAX_PARALLEL - MIN_PARALLEL - 1,
                )
            }
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                SettingText(title = stringResource(R.string.settings_timeout))
                Spacer(Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    TIMEOUT_OPTIONS.forEachIndexed { index, seconds ->
                        SegmentedButton(
                            selected = settings.timeoutSeconds == seconds,
                            onClick = { onTimeoutChange(seconds) },
                            shape = SegmentedButtonDefaults.itemShape(index, TIMEOUT_OPTIONS.size),
                            label = { Text(stringResource(R.string.settings_timeout_option, seconds)) },
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = settings.autoRetry,
                        role = Role.Switch,
                        onValueChange = onAutoRetryChange,
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SettingText(
                    title = stringResource(R.string.settings_auto_retry),
                    description = stringResource(R.string.settings_auto_retry_desc),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                Switch(checked = settings.autoRetry, onCheckedChange = null)
            }

            SectionHeader(R.string.settings_adapters)
            adapterSources.forEach { source ->
                ActionRow(
                    title = stringResource(source.nameRes),
                    description = stringResource(R.string.settings_adapter_desc),
                    onClick = { onOpenAdapterSettings(source) },
                )
            }

            SectionHeader(R.string.settings_copy)
            ActionRow(
                title = stringResource(R.string.settings_copy_line),
                description = stringResource(
                    R.string.settings_copy_line_summary,
                    stringResource(if (settings.copyLineFormat == LineFormat.CSV) R.string.format_csv else R.string.format_tsv),
                    settings.copyLineFields.map { stringResource(it.labelRes) }.joinToString(", "),
                ),
                onClick = onOpenCopyLine,
            )
            ActionRow(
                title = stringResource(R.string.settings_copy_details),
                description = stringResource(
                    if (settings.copyDetailsTemplate == CopyFormatter.DEFAULT_DETAILS_TEMPLATE) {
                        R.string.settings_copy_details_default
                    } else {
                        R.string.settings_copy_details_custom
                    }
                ),
                onClick = onOpenCopyDetails,
            )

            SectionHeader(R.string.settings_local_data)
            ActionRow(
                title = stringResource(R.string.settings_clear_cache),
                description = stringResource(R.string.settings_clear_cache_desc),
                onClick = { pendingConfirm = PendingConfirm.CLEAR_CACHE },
            )
            ActionRow(
                title = stringResource(R.string.settings_clear_history),
                description = stringResource(R.string.settings_clear_history_desc),
                titleColor = MaterialTheme.colorScheme.error,
                onClick = { pendingConfirm = PendingConfirm.CLEAR_HISTORY },
            )

            SectionHeader(R.string.settings_about)
            SettingText(
                title = stringResource(R.string.settings_version),
                description = BuildConfig.VERSION_NAME,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
            SettingText(
                title = stringResource(R.string.settings_licenses),
                description = stringResource(R.string.settings_licenses_desc),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }

    pendingConfirm?.let { confirm ->
        val (title, body) = when (confirm) {
            PendingConfirm.CLEAR_CACHE ->
                R.string.settings_clear_cache_confirm to R.string.settings_clear_cache_confirm_body

            PendingConfirm.CLEAR_HISTORY ->
                R.string.settings_clear_history_confirm to R.string.settings_clear_history_confirm_body
        }
        AlertDialog(
            onDismissRequest = { pendingConfirm = null },
            title = { Text(stringResource(title)) },
            text = { Text(stringResource(body)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingConfirm = null
                    when (confirm) {
                        PendingConfirm.CLEAR_CACHE -> onClearCache()
                        PendingConfirm.CLEAR_HISTORY -> onClearHistory()
                    }
                }) {
                    Text(stringResource(R.string.action_clear), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingConfirm = null }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun SectionHeader(titleRes: Int) {
    Column {
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(titleRes).uppercase(),
            style = EyebrowTextStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun SettingText(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(modifier) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = titleColor)
        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ThemeOption(labelRes: Int, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(labelRes), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        RadioButton(selected = selected, onClick = null)
    }
}

@Composable
private fun ActionRow(
    title: String,
    description: String,
    onClick: () -> Unit,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    SettingText(
        title = title,
        description = description,
        titleColor = titleColor,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun SettingsScreenPreview() {
    DataToolTheme {
        SettingsScreen(
            settings = AppSettings(),
            adapterSources = listOf(DataSource.UNITY_ASSET_STORE, DataSource.AMAZON_BOOKS),
            onThemeModeChange = {},
            onParallelRequestsChange = {},
            onTimeoutChange = {},
            onAutoRetryChange = {},
            onClearCache = {},
            onClearHistory = {},
            onOpenCopyLine = {},
            onOpenCopyDetails = {},
            onOpenAdapterSettings = {},
        )
    }
}
