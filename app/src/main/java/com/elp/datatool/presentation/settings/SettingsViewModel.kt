package com.elp.datatool.presentation.settings

import androidx.lifecycle.ViewModel
import com.elp.datatool.data.sample.SampleData
import com.elp.datatool.domain.copy.CopyField
import com.elp.datatool.domain.copy.CopyFormatter
import com.elp.datatool.domain.copy.LineFormat
import com.elp.datatool.domain.model.AppSettings
import com.elp.datatool.domain.model.ProductResult
import com.elp.datatool.domain.model.ResultStatus
import com.elp.datatool.domain.model.ThemeMode
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
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val resultRepository: ResultRepository,
    private val responseCache: ResponseCache,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings

    private val _events = Channel<SettingsEvent>(Channel.BUFFERED)
    val events: Flow<SettingsEvent> = _events.receiveAsFlow()

    /** The newest collected result, or sample data when nothing has been collected yet. */
    val previewResult: ProductResult
        get() = resultRepository.results.value
            .filter { it.status == ResultStatus.COMPLETED }
            .maxByOrNull { it.collectedAtMillis }
            ?: SampleData.results.first()

    fun onThemeModeChange(mode: ThemeMode) = settingsRepository.setThemeMode(mode)

    fun onParallelRequestsChange(value: Int) = settingsRepository.setParallelRequests(value)

    fun onTimeoutChange(seconds: Int) = settingsRepository.setTimeoutSeconds(seconds)

    fun onAutoRetryChange(enabled: Boolean) = settingsRepository.setAutoRetry(enabled)

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
}
