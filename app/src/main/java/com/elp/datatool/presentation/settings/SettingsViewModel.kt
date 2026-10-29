package com.elp.datatool.presentation.settings

import androidx.lifecycle.ViewModel
import com.elp.datatool.data.sample.SampleData
import com.elp.datatool.domain.copy.CopyField
import com.elp.datatool.domain.copy.CopyFormatter
import com.elp.datatool.domain.copy.LineFormat
import com.elp.datatool.domain.model.AdapterSettings
import com.elp.datatool.domain.model.AppSettings
import com.elp.datatool.domain.model.DataSource
import com.elp.datatool.domain.model.ProductResult
import com.elp.datatool.domain.model.ResultStatus
import com.elp.datatool.domain.model.ThemeMode
import com.elp.datatool.domain.repository.CollectionQueue
import com.elp.datatool.domain.repository.ResponseCache
import com.elp.datatool.domain.repository.ResultRepository
import com.elp.datatool.domain.repository.SettingsRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow

sealed interface SettingsEvent {
    data object CacheCleared : SettingsEvent
    data object HistoryCleared : SettingsEvent
    data class AdapterDataCleared(val source: DataSource) : SettingsEvent
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val resultRepository: ResultRepository,
    private val responseCache: ResponseCache,
    collectionQueue: CollectionQueue,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
    val adapterSources: List<DataSource> = DataSource.entries.filter(collectionQueue::isSupported)

    private val _events = Channel<SettingsEvent>(Channel.BUFFERED)
    val events: Flow<SettingsEvent> = _events.receiveAsFlow()

    /** The newest collected result, or sample data when nothing has been collected yet. */
    val previewResult: ProductResult
        get() = resultRepository.results.value
            .filter { it.status == ResultStatus.COMPLETED }
            .maxByOrNull { it.collectedAtMillis }
            ?: SampleData.results.first()

    fun previewResult(source: DataSource): ProductResult = resultRepository.results.value
        .filter { it.source == source && it.status == ResultStatus.COMPLETED }
        .maxByOrNull { it.collectedAtMillis }
        ?: SampleData.results.firstOrNull { it.source == source }
        ?: SampleData.results.first().copy(source = source)

    fun onThemeModeChange(mode: ThemeMode) = settingsRepository.setThemeMode(mode)

    fun onParallelRequestsChange(value: Int) = settingsRepository.setParallelRequests(value)

    fun onTimeoutChange(seconds: Int) = settingsRepository.setTimeoutSeconds(seconds)

    fun onAutoRetryChange(enabled: Boolean) = settingsRepository.setAutoRetry(enabled)

    fun onSearchEngineFallbackChange(source: DataSource, enabled: Boolean) = updateAdapterSettings(source) {
        it.copy(searchEngineFallbackEnabled = enabled)
    }

    fun onCustomCopySettingsChange(source: DataSource, enabled: Boolean) = updateAdapterSettings(source) {
        it.copy(customCopyEnabled = enabled)
    }

    fun onAdapterCopyLineFormatChange(source: DataSource, format: LineFormat) = updateAdapterSettings(source) {
        it.copy(customCopyEnabled = true, copyLineFormat = format)
    }

    fun onAddAdapterCopyField(source: DataSource, field: CopyField) {
        val fields = settings.value.copyLineFieldsFor(source)
        if (field !in fields) updateAdapterSettings(source) {
            it.copy(customCopyEnabled = true, copyLineFields = fields + field)
        }
    }

    fun onRemoveAdapterCopyField(source: DataSource, field: CopyField) {
        val fields = settings.value.copyLineFieldsFor(source)
        updateAdapterSettings(source) { it.copy(customCopyEnabled = true, copyLineFields = fields - field) }
    }

    fun onMoveAdapterCopyField(source: DataSource, field: CopyField, offset: Int) {
        val fields = settings.value.copyLineFieldsFor(source).toMutableList()
        val from = fields.indexOf(field)
        val to = from + offset
        if (from < 0 || to !in fields.indices) return
        fields.add(to, fields.removeAt(from))
        updateAdapterSettings(source) { it.copy(customCopyEnabled = true, copyLineFields = fields) }
    }

    fun onResetAdapterCopyLine(source: DataSource) = updateAdapterSettings(source) {
        it.copy(copyLineFormat = null, copyLineFields = null)
    }

    fun onAdapterDetailsTemplateChange(source: DataSource, template: String) = updateAdapterSettings(source) {
        it.copy(customCopyEnabled = true, copyDetailsTemplate = template)
    }

    fun onResetAdapterDetailsTemplate(source: DataSource) = updateAdapterSettings(source) {
        it.copy(copyDetailsTemplate = null)
    }

    fun onClearAdapterData(source: DataSource) {
        responseCache.clear(source)
        resultRepository.clear(source)
        _events.trySend(SettingsEvent.AdapterDataCleared(source))
    }

    fun onClearCache() {
        responseCache.clear()
        _events.trySend(SettingsEvent.CacheCleared)
    }

    fun onClearHistory() {
        resultRepository.clear()
        _events.trySend(SettingsEvent.HistoryCleared)
    }

    fun onCopyLineFormatChange(format: LineFormat) = settingsRepository.setCopyLineFormat(format)

    fun onAddCopyField(field: CopyField) {
        val fields = settings.value.copyLineFields
        if (field !in fields) settingsRepository.setCopyLineFields(fields + field)
    }

    fun onRemoveCopyField(field: CopyField) =
        settingsRepository.setCopyLineFields(settings.value.copyLineFields - field)

    /** Moves a column by [offset] positions (-1 = up, +1 = down). */
    fun onMoveCopyField(field: CopyField, offset: Int) {
        val fields = settings.value.copyLineFields.toMutableList()
        val from = fields.indexOf(field)
        val to = from + offset
        if (from < 0 || to !in fields.indices) return
        fields.add(to, fields.removeAt(from))
        settingsRepository.setCopyLineFields(fields)
    }

    fun onResetCopyLine() {
        settingsRepository.setCopyLineFormat(CopyFormatter.DEFAULT_LINE_FORMAT)
        settingsRepository.setCopyLineFields(CopyFormatter.DEFAULT_LINE_FIELDS)
    }

    fun onDetailsTemplateChange(template: String) = settingsRepository.setCopyDetailsTemplate(template)

    fun onResetDetailsTemplate() = settingsRepository.setCopyDetailsTemplate(CopyFormatter.DEFAULT_DETAILS_TEMPLATE)

    private fun updateAdapterSettings(source: DataSource, transform: (AdapterSettings) -> AdapterSettings) {
        settingsRepository.setAdapterSettings(source, transform(settings.value.settingsFor(source)))
    }
}
