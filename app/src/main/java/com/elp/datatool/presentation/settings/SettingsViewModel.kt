package com.elp.datatool.presentation.settings

import androidx.lifecycle.ViewModel
import com.elp.datatool.domain.model.AppSettings
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
}
