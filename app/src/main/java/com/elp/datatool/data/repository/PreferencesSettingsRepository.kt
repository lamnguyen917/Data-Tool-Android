package com.elp.datatool.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.elp.datatool.domain.copy.CopyField
import com.elp.datatool.domain.copy.CopyFormatter
import com.elp.datatool.domain.copy.LineFormat
import com.elp.datatool.domain.model.AdapterSettings
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
    override fun setAdapterSettings(source: DataSource, adapterSettings: AdapterSettings) = update { settings ->
        settings.copy(adapterSettings = settings.adapterSettings + (source to adapterSettings))
    }
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
            adapterSettings = readAdapterSettings(),
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

    /** Migrates the former global fallback value when a source-specific key is absent. */
    private fun SharedPreferences.readAdapterSettings(): Map<DataSource, AdapterSettings> {
        val legacyFallback = getBoolean(KEY_SEARCH_ENGINE_FALLBACK, false)
        return DataSource.entries.associateWith { source ->
            val hasExistingCopyOverride = contains(adapterCopyLineFormatKey(source)) ||
                contains(adapterCopyLineFieldsKey(source)) ||
                contains(adapterCopyDetailsTemplateKey(source))
            AdapterSettings(
                searchEngineFallbackEnabled = getBoolean(
                    adapterSearchEngineKey(source),
                    legacyFallback,
                ),
                customCopyEnabled = getBoolean(
                    adapterCustomCopyEnabledKey(source),
                    hasExistingCopyOverride,
                ),
                copyLineFormat = getString(adapterCopyLineFormatKey(source), null)
                    ?.let { enumValueOrDefault<LineFormat>(it, CopyFormatter.DEFAULT_LINE_FORMAT) },
                copyLineFields = readAdapterCopyFields(source),
                copyDetailsTemplate = getString(adapterCopyDetailsTemplateKey(source), null),
            )
        }
    }

    private fun SharedPreferences.readAdapterCopyFields(source: DataSource): List<CopyField>? {
        val key = adapterCopyLineFieldsKey(source)
        if (!contains(key)) return null
        return getString(key, "")
            .orEmpty()
            .split(FIELD_SEPARATOR)
            .filter(String::isNotEmpty)
            .mapNotNull(CopyField::fromKey)
            .distinct()
    }

    private fun SharedPreferences.writeSettings(settings: AppSettings) {
        val editor = edit()
            .putString(KEY_THEME_MODE, settings.themeMode.name)
            .putInt(KEY_PARALLEL_REQUESTS, settings.parallelRequests)
            .putInt(KEY_TIMEOUT_SECONDS, settings.timeoutSeconds)
            .putBoolean(KEY_AUTO_RETRY, settings.autoRetry)
            .putString(KEY_COPY_LINE_FORMAT, settings.copyLineFormat.name)
            .putString(KEY_COPY_LINE_FIELDS, settings.copyLineFields.joinToString(FIELD_SEPARATOR) { it.key })
            .putString(KEY_COPY_DETAILS_TEMPLATE, settings.copyDetailsTemplate)
            .putString(KEY_SELECTED_SOURCE, settings.selectedSource.id)
            .remove(KEY_SEARCH_ENGINE_FALLBACK)
        DataSource.entries.forEach { source ->
            val adapterSettings = settings.settingsFor(source)
            editor.putBoolean(
                adapterSearchEngineKey(source),
                adapterSettings.searchEngineFallbackEnabled,
            )
            editor.putBoolean(adapterCustomCopyEnabledKey(source), adapterSettings.customCopyEnabled)
            editor.putOrRemove(adapterCopyLineFormatKey(source), adapterSettings.copyLineFormat?.name)
            editor.putOrRemove(
                adapterCopyLineFieldsKey(source),
                adapterSettings.copyLineFields?.joinToString(FIELD_SEPARATOR) { it.key },
            )
            editor.putOrRemove(adapterCopyDetailsTemplateKey(source), adapterSettings.copyDetailsTemplate)
        }
        editor.apply()
    }

    private fun SharedPreferences.Editor.putOrRemove(key: String, value: String?) = apply {
        if (value == null) remove(key) else putString(key, value)
    }

    private fun adapterSearchEngineKey(source: DataSource) =
        "adapter_${source.id}_search_engine_fallback"

    private fun adapterCopyLineFormatKey(source: DataSource) = "adapter_${source.id}_copy_line_format"

    private fun adapterCustomCopyEnabledKey(source: DataSource) = "adapter_${source.id}_custom_copy_enabled"

    private fun adapterCopyLineFieldsKey(source: DataSource) = "adapter_${source.id}_copy_line_fields"

    private fun adapterCopyDetailsTemplateKey(source: DataSource) = "adapter_${source.id}_copy_details_template"

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == value } ?: default

    private companion object {
        const val PREFERENCES_NAME = "app_settings"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_PARALLEL_REQUESTS = "parallel_requests"
        const val KEY_TIMEOUT_SECONDS = "timeout_seconds"
        const val KEY_AUTO_RETRY = "auto_retry"
        const val KEY_SEARCH_ENGINE_FALLBACK = "search_engine_fallback"
        const val KEY_COPY_LINE_FORMAT = "copy_line_format"
        const val KEY_COPY_LINE_FIELDS = "copy_line_fields"
        const val KEY_COPY_DETAILS_TEMPLATE = "copy_details_template"
        const val KEY_SELECTED_SOURCE = "selected_source"
        const val FIELD_SEPARATOR = ","
    }
}
