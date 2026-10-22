package com.elp.datatool.presentation.results

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elp.datatool.domain.model.ProductResult
import com.elp.datatool.domain.copy.CopyFormatter
import com.elp.datatool.domain.repository.ResultRepository
import com.elp.datatool.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ResultDetailViewModel(
    savedStateHandle: SavedStateHandle,
    repository: ResultRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    /** Text for "Copy details", built from the template in Settings. */
    fun detailsText(result: ProductResult): String =
        CopyFormatter.details(result, settings.settings.value.copyDetailsTemplate)

    private val key: String = checkNotNull(savedStateHandle[KEY_ARG])

    /** The result, or null when it no longer exists. */
    val result: StateFlow<ProductResult?> = repository.results
        .map { list -> list.find { it.key == key } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = repository.results.value.find { it.key == key },
        )

    companion object {
        const val KEY_ARG = "key"
    }
}
