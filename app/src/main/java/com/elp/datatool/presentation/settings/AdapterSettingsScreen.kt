package com.elp.datatool.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.elp.datatool.R
import com.elp.datatool.domain.model.AdapterSettings
import com.elp.datatool.domain.model.DataSource
import com.elp.datatool.presentation.components.BackButton
import com.elp.datatool.presentation.components.DtTopBar
import com.elp.datatool.presentation.components.nameRes
import com.elp.datatool.ui.theme.DataToolTheme
import com.elp.datatool.ui.theme.EyebrowTextStyle

@Composable
fun AdapterSettingsRoute(
    viewModel: SettingsViewModel,
    source: DataSource,
    onBack: () -> Unit,
    onOpenCopyLine: () -> Unit,
    onOpenCopyDetails: () -> Unit,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            if (event is SettingsEvent.AdapterDataCleared && event.source == source) {
                snackbarHostState.showSnackbar(
                    context.getString(
                        R.string.settings_adapter_data_cleared,
                        context.getString(source.nameRes),
                    )
                )
            }
        }
    }
    AdapterSettingsScreen(
        source = source,
        settings = settings.settingsFor(source),
        onBack = onBack,
        onSearchEngineFallbackChange = { enabled ->
            viewModel.onSearchEngineFallbackChange(source, enabled)
        },
        onCustomCopySettingsChange = { enabled ->
            viewModel.onCustomCopySettingsChange(source, enabled)
        },
        onOpenCopyLine = onOpenCopyLine,
        onOpenCopyDetails = onOpenCopyDetails,
        onClearLocalData = { viewModel.onClearAdapterData(source) },
        snackbarHostState = snackbarHostState,
    )
}

@Composable
fun AdapterSettingsScreen(
    source: DataSource,
    settings: AdapterSettings,
    onBack: () -> Unit,
    onSearchEngineFallbackChange: (Boolean) -> Unit,
    onCustomCopySettingsChange: (Boolean) -> Unit,
    onOpenCopyLine: () -> Unit,
    onOpenCopyDetails: () -> Unit,
    onClearLocalData: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    var confirmClearData by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            DtTopBar(
                title = stringResource(R.string.adapter_settings_title),
                subtitle = stringResource(source.nameRes),
                navigationIcon = { BackButton(onBack) },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.settings_search).uppercase(),
                style = EyebrowTextStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = settings.searchEngineFallbackEnabled,
                        role = Role.Switch,
                        onValueChange = onSearchEngineFallbackChange,
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_search_engine),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = stringResource(R.string.settings_search_engine_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(checked = settings.searchEngineFallbackEnabled, onCheckedChange = null)
            }

            SectionHeader(R.string.settings_copy)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = settings.customCopyEnabled,
                        role = Role.Switch,
                        onValueChange = onCustomCopySettingsChange,
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_adapter_custom_copy),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = stringResource(R.string.settings_adapter_custom_copy_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(checked = settings.customCopyEnabled, onCheckedChange = null)
            }
            if (settings.customCopyEnabled) {
                ActionRow(
                    title = stringResource(R.string.settings_copy_line),
                    description = stringResource(R.string.settings_adapter_copy_custom),
                    onClick = onOpenCopyLine,
                )
                ActionRow(
                    title = stringResource(R.string.settings_copy_details),
                    description = stringResource(R.string.settings_adapter_copy_custom),
                    onClick = onOpenCopyDetails,
                )
            }

            SectionHeader(R.string.settings_local_data)
            ActionRow(
                title = stringResource(R.string.settings_adapter_clear_data),
                description = stringResource(R.string.settings_adapter_clear_data_desc),
                titleColor = MaterialTheme.colorScheme.error,
                onClick = { confirmClearData = true },
            )
        }
    }

    if (confirmClearData) {
        val sourceName = stringResource(source.nameRes)
        AlertDialog(
            onDismissRequest = { confirmClearData = false },
            title = { Text(stringResource(R.string.settings_adapter_clear_data_confirm)) },
            text = {
                Text(stringResource(R.string.settings_adapter_clear_data_confirm_body, sourceName))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClearData = false
                        onClearLocalData()
                    }
                ) {
                    Text(stringResource(R.string.action_clear), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearData = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun SectionHeader(titleRes: Int) {
    Spacer(Modifier.height(12.dp))
    Text(
        text = stringResource(titleRes).uppercase(),
        style = EyebrowTextStyle,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
}

@Composable
private fun ActionRow(
    title: String,
    description: String,
    onClick: () -> Unit,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = titleColor)
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AdapterSettingsScreenPreview() {
    DataToolTheme {
        AdapterSettingsScreen(
            source = DataSource.UNITY_ASSET_STORE,
            settings = AdapterSettings(searchEngineFallbackEnabled = true),
            onBack = {},
            onSearchEngineFallbackChange = {},
            onCustomCopySettingsChange = {},
            onOpenCopyLine = {},
            onOpenCopyDetails = {},
            onClearLocalData = {},
        )
    }
}
