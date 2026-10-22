package com.elp.datatool.presentation.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elp.datatool.R
import com.elp.datatool.data.sample.SampleData
import com.elp.datatool.domain.copy.CopyField
import com.elp.datatool.domain.copy.CopyFormatter
import com.elp.datatool.domain.copy.LineFormat
import com.elp.datatool.domain.model.ProductResult
import com.elp.datatool.presentation.components.BackButton
import com.elp.datatool.presentation.components.DtTopBar
import com.elp.datatool.presentation.components.FieldLabel
import com.elp.datatool.presentation.components.displayTitle
import com.elp.datatool.presentation.components.labelRes
import com.elp.datatool.ui.icons.DtIcons
import com.elp.datatool.ui.theme.CaptionTextStyle
import com.elp.datatool.ui.theme.DataToolTheme

// ---------------------------------------------------------------- Copy line

@Composable
fun CopyLineSettingsRoute(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    CopyLineSettingsScreen(
        format = settings.copyLineFormat,
        fields = settings.copyLineFields,
        preview = remember { viewModel.previewResult },
        onBack = onBack,
        onFormatChange = viewModel::onCopyLineFormatChange,
        onAdd = viewModel::onAddCopyField,
        onRemove = viewModel::onRemoveCopyField,
        onMove = viewModel::onMoveCopyField,
        onReset = viewModel::onResetCopyLine,
    )
}

@Composable
fun CopyLineSettingsScreen(
    format: LineFormat,
    fields: List<CopyField>,
    preview: ProductResult,
    onBack: () -> Unit,
    onFormatChange: (LineFormat) -> Unit,
    onAdd: (CopyField) -> Unit,
    onRemove: (CopyField) -> Unit,
    onMove: (CopyField, Int) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            DtTopBar(
                title = stringResource(R.string.copy_line_title),
                navigationIcon = { BackButton(onBack) },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            FieldLabel(stringResource(R.string.copy_line_format))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                LineFormat.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = format == option,
                        onClick = { onFormatChange(option) },
                        shape = SegmentedButtonDefaults.itemShape(index, LineFormat.entries.size),
                        label = {
                            Text(stringResource(if (option == LineFormat.CSV) R.string.format_csv else R.string.format_tsv))
                        },
                    )
                }
            }
            Text(
                text = stringResource(R.string.copy_line_format_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )

            Spacer(Modifier.height(20.dp))
            FieldLabel(stringResource(R.string.copy_line_columns))
            if (fields.isEmpty()) {
                Text(
                    text = stringResource(R.string.copy_line_columns_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            } else {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Column {
                        fields.forEachIndexed { index, field ->
                            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                            SelectedFieldRow(
                                position = index + 1,
                                field = field,
                                canMoveUp = index > 0,
                                canMoveDown = index < fields.lastIndex,
                                onMove = { offset -> onMove(field, offset) },
                                onRemove = { onRemove(field) },
                            )
                        }
                    }
                }
            }

            val available = CopyField.entries.filterNot { it in fields }
            if (available.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                FieldLabel(stringResource(R.string.copy_line_available))
                FieldChips(fields = available, showPlaceholder = false, icon = DtIcons.Plus, onClick = onAdd)
            }

            Spacer(Modifier.height(20.dp))
            PreviewBox(
                text = CopyFormatter.line(preview, fields, format)
                    .replace("\t", "  →  "),
                sampleTitle = preview.displayTitle(),
                singleLine = true,
            )

            Spacer(Modifier.height(16.dp))
            ResetButton(onReset)
        }
    }
}

@Composable
private fun SelectedFieldRow(
    position: Int,
    field: CopyField,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    val label = stringResource(field.labelRes)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = position.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 12.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(field.placeholder, style = CaptionTextStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = { onMove(-1) }, enabled = canMoveUp) {
            Icon(DtIcons.ChevronUp, stringResource(R.string.copy_line_move_up, label), Modifier.size(18.dp))
        }
        IconButton(onClick = { onMove(1) }, enabled = canMoveDown) {
            Icon(DtIcons.ChevronDown, stringResource(R.string.copy_line_move_down, label), Modifier.size(18.dp))
        }
        IconButton(onClick = onRemove) {
            Icon(DtIcons.Close, stringResource(R.string.copy_line_remove, label), Modifier.size(18.dp))
        }
    }
}

// ---------------------------------------------------------------- Copy details

@Composable
fun CopyDetailsSettingsRoute(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    CopyDetailsSettingsScreen(
        template = settings.copyDetailsTemplate,
        preview = remember { viewModel.previewResult },
        onBack = onBack,
        onTemplateChange = viewModel::onDetailsTemplateChange,
        onReset = viewModel::onResetDetailsTemplate,
    )
}

@Composable
fun CopyDetailsSettingsScreen(
    template: String,
    preview: ProductResult,
    onBack: () -> Unit,
    onTemplateChange: (String) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Local value keeps the cursor so placeholders are inserted where the user is typing.
    var value by remember { mutableStateOf(TextFieldValue(template, TextRange(template.length))) }
    // Re-sync only when the template changes from outside (e.g. reset), not while it catches up with typing.
    var lastTemplate by remember { mutableStateOf(template) }
    if (template != lastTemplate) {
        lastTemplate = template
        if (template != value.text) value = TextFieldValue(template, TextRange(template.length))
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            DtTopBar(
                title = stringResource(R.string.copy_details_title),
                navigationIcon = { BackButton(onBack) },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            FieldLabel(stringResource(R.string.copy_details_template))
            OutlinedTextField(
                value = value,
                onValueChange = {
                    value = it
                    onTemplateChange(it.text)
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 8,
                shape = MaterialTheme.shapes.small,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
            )
            Text(
                text = stringResource(R.string.copy_details_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )

            Spacer(Modifier.height(16.dp))
            FieldLabel(stringResource(R.string.copy_details_insert))
            FieldChips(fields = CopyField.entries, showPlaceholder = true, icon = DtIcons.Plus) { field ->
                val selection = value.selection
                val text = value.text.replaceRange(selection.min, selection.max, field.placeholder)
                val cursor = selection.min + field.placeholder.length
                value = TextFieldValue(text, TextRange(cursor))
                onTemplateChange(text)
            }

            Spacer(Modifier.height(20.dp))
            PreviewBox(
                text = CopyFormatter.details(preview, value.text),
                sampleTitle = preview.displayTitle(),
                singleLine = false,
            )

            Spacer(Modifier.height(16.dp))
            ResetButton(onReset)
        }
    }
}

// ---------------------------------------------------------------- Shared parts

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FieldChips(
    fields: List<CopyField>,
    showPlaceholder: Boolean,
    icon: ImageVector,
    onClick: (CopyField) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        fields.forEach { field ->
            val label = stringResource(field.labelRes)
            AssistChip(
                onClick = { onClick(field) },
                label = { Text(if (showPlaceholder) field.placeholder else label) },
                leadingIcon = { Icon(icon, stringResource(R.string.copy_line_add, label), Modifier.size(14.dp)) },
                shape = MaterialTheme.shapes.small,
            )
        }
    }
}

@Composable
private fun PreviewBox(text: String, sampleTitle: String, singleLine: Boolean) {
    Row(verticalAlignment = Alignment.Bottom) {
        FieldLabel(stringResource(R.string.copy_preview), Modifier.weight(1f))
        Text(
            text = stringResource(R.string.copy_preview_sample, sampleTitle),
            style = CaptionTextStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(bottom = 6.dp, start = 8.dp),
        )
    }
    val textModifier = if (singleLine) Modifier.horizontalScroll(rememberScrollState()) else Modifier
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
        softWrap = !singleLine,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
            .padding(12.dp)
            .then(textModifier),
    )
}

@Composable
private fun ResetButton(onReset: () -> Unit) {
    OutlinedButton(
        onClick = onReset,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(DtIcons.Retry, contentDescription = null, modifier = Modifier.size(15.dp))
        Spacer(Modifier.size(6.dp))
        Text(stringResource(R.string.action_reset))
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun CopyLineSettingsPreview() {
    DataToolTheme {
        CopyLineSettingsScreen(
            format = CopyFormatter.DEFAULT_LINE_FORMAT,
            fields = CopyFormatter.DEFAULT_LINE_FIELDS,
            preview = SampleData.results.first(),
            onBack = {},
            onFormatChange = {},
            onAdd = {},
            onRemove = {},
            onMove = { _, _ -> },
            onReset = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun CopyDetailsSettingsPreview() {
    DataToolTheme {
        CopyDetailsSettingsScreen(
            template = CopyFormatter.DEFAULT_DETAILS_TEMPLATE,
            preview = SampleData.results.first(),
            onBack = {},
            onTemplateChange = {},
            onReset = {},
        )
    }
}
