package com.elp.datatool.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.elp.datatool.domain.copy.CopyField
import com.elp.datatool.domain.copy.LineFormat
import com.elp.datatool.domain.model.AppSettings
import com.elp.datatool.domain.model.DataSource
import com.elp.datatool.domain.model.ThemeMode
import com.elp.datatool.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Persists app settings and the selected data source across process restarts. */
class PreferencesSettingsRepository(context: Context) : SettingsRepository {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val state = MutableStateFlow(preferences.readSettings())

    override val settings: StateFlow<AppSettings> = state.asStateFlow()

    override fun setThemeMode(mode: ThemeMode) = update { it.copy(themeMode = mode) }
    override fun setParallelRequests(value: Int) = update { it.copy(parallelRequests = value) }
    override fun setTimeoutSeconds(value: Int) = update { it.copy(timeoutSeconds = value) }
    override fun setAutoRetry(enabled: Boolean) = update { it.copy(autoRetry = enabled) }
    override fun setCopyLineFormat(format: LineFormat) = update { it.copy(copyLineFormat = format) }
    override fun setCopyLineFields(fields: List<CopyField>) = update { it.copy(copyLineFields = fields) }
    override fun setCopyDetailsTemplate(template: String) = update { it.copy(copyDetailsTemplate = template) }
    override fun setSelectedSource(source: DataSource) = update { it.copy(selectedSource = source) }

    @Synchronized
    private fun update(transform: (AppSettings) -> AppSettings) {
        val updated = transform(state.value)
        preferences.writeSettings(updated)
        state.value = updated
    }

    private fun SharedPreferences.readSettings(): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            themeMode = enumValueOrDefault(getString(KEY_THEME_MODE, null), defaults.themeMode),
            parallelRequests = getInt(KEY_PARALLEL_REQUESTS, defaults.parallelRequests),
            timeoutSeconds = getInt(KEY_TIMEOUT_SECONDS, defaults.timeoutSeconds),
            autoRetry = getBoolean(KEY_AUTO_RETRY, defaults.autoRetry),
            copyLineFormat = enumValueOrDefault(getString(KEY_COPY_LINE_FORMAT, null), defaults.copyLineFormat),
            copyLineFields = readCopyFields(defaults.copyLineFields),
            copyDetailsTemplate = getString(KEY_COPY_DETAILS_TEMPLATE, defaults.copyDetailsTemplate)
                ?: defaults.copyDetailsTemplate,
            selectedSource = DataSource.entries.firstOrNull { it.id == getString(KEY_SELECTED_SOURCE, null) }
                ?: defaults.selectedSource,
        )
    }

    private fun SharedPreferences.readCopyFields(default: List<CopyField>): List<CopyField> {
        if (!contains(KEY_COPY_LINE_FIELDS)) return default
        return getString(KEY_COPY_LINE_FIELDS, "")
            .orEmpty()
            .split(FIELD_SEPARATOR)
            .filter(String::isNotEmpty)
            .mapNotNull(CopyField::fromKey)
            .distinct()
    }

    private fun SharedPreferences.writeSettings(settings: AppSettings) {
        edit()
            .putString(KEY_THEME_MODE, settings.themeMode.name)
            .putInt(KEY_PARALLEL_REQUESTS, settings.parallelRequests)
            .putInt(KEY_TIMEOUT_SECONDS, settings.timeoutSeconds)
            .putBoolean(KEY_AUTO_RETRY, settings.autoRetry)
            .putString(KEY_COPY_LINE_FORMAT, settings.copyLineFormat.name)
            .putString(KEY_COPY_LINE_FIELDS, settings.copyLineFields.joinToString(FIELD_SEPARATOR) { it.key })
            .putString(KEY_COPY_DETAILS_TEMPLATE, settings.copyDetailsTemplate)
            .putString(KEY_SELECTED_SOURCE, settings.selectedSource.id)
            .apply()
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == value } ?: default

    private companion object {
        const val PREFERENCES_NAME = "app_settings"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_PARALLEL_REQUESTS = "parallel_requests"
        const val KEY_TIMEOUT_SECONDS = "timeout_seconds"
        const val KEY_AUTO_RETRY = "auto_retry"
        const val KEY_COPY_LINE_FORMAT = "copy_line_format"
        const val KEY_COPY_LINE_FIELDS = "copy_line_fields"
        const val KEY_COPY_DETAILS_TEMPLATE = "copy_details_template"
        const val KEY_SELECTED_SOURCE = "selected_source"
        const val FIELD_SEPARATOR = ","
    }
}
