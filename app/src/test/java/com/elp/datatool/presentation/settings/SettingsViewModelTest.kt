package com.elp.datatool.presentation.settings

import com.elp.datatool.data.repository.InMemoryResultRepository
import com.elp.datatool.data.repository.InMemorySettingsRepository
import com.elp.datatool.data.repository.ProductCache
import com.elp.datatool.domain.copy.CopyField
import com.elp.datatool.domain.copy.CopyFormatter
import com.elp.datatool.domain.copy.LineFormat
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsViewModelTest {

    private val settings = InMemorySettingsRepository()
    private val viewModel = SettingsViewModel(settings, InMemoryResultRepository(), ProductCache())

    @Test
    fun copyLineFields_addMoveRemoveAndReset() {
        viewModel.onAddCopyField(CopyField.PRICE)
        viewModel.onAddCopyField(CopyField.PRICE)
        viewModel.onMoveCopyField(CopyField.PRICE, -1)
        viewModel.onRemoveCopyField(CopyField.SKU)
        viewModel.onCopyLineFormatChange(LineFormat.CSV)

        assertEquals(
            listOf(CopyField.TITLE, CopyField.CATEGORIES, CopyField.AUTHOR, CopyField.PRICE, CopyField.URL),
            settings.settings.value.copyLineFields,
        )

        viewModel.onMoveCopyField(CopyField.TITLE, -1)
        assertEquals(CopyField.TITLE, settings.settings.value.copyLineFields.first())

        viewModel.onResetCopyLine()
        assertEquals(CopyFormatter.DEFAULT_LINE_FIELDS, settings.settings.value.copyLineFields)
        assertEquals(CopyFormatter.DEFAULT_LINE_FORMAT, settings.settings.value.copyLineFormat)
    }

    @Test
    fun detailsTemplate_changeAndReset() {
        viewModel.onDetailsTemplateChange("{title} — {url}")
        assertEquals("{title} — {url}", settings.settings.value.copyDetailsTemplate)

        viewModel.onResetDetailsTemplate()
        assertEquals(CopyFormatter.DEFAULT_DETAILS_TEMPLATE, settings.settings.value.copyDetailsTemplate)
    }
}
