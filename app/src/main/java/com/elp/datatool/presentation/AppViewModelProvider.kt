package com.elp.datatool.presentation

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.elp.datatool.AppContainer
import com.elp.datatool.DataToolApplication
import com.elp.datatool.presentation.collect.CollectViewModel
import com.elp.datatool.presentation.results.ResultDetailViewModel
import com.elp.datatool.presentation.results.ResultsViewModel
import com.elp.datatool.presentation.settings.SettingsViewModel

/** Creates every ViewModel with dependencies from [AppContainer]. */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            CollectViewModel(container().productSearchRepository, container().collectionQueue)
        }
        initializer { ResultsViewModel(container().resultRepository, container().collectionQueue) }
        initializer { ResultDetailViewModel(createSavedStateHandle(), container().resultRepository) }
        initializer {
            val container = container()
            SettingsViewModel(container.settingsRepository, container.resultRepository, container.responseCache)
        }
    }

    private fun CreationExtras.container(): AppContainer =
        (this[APPLICATION_KEY] as DataToolApplication).container
}
