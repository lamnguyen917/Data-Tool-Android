package com.elp.datatool.presentation.settings

import com.elp.datatool.data.repository.InMemoryResultRepository
import com.elp.datatool.data.repository.InMemorySettingsRepository
import com.elp.datatool.data.repository.ProductCache
import com.elp.datatool.domain.copy.CopyField
import com.elp.datatool.domain.copy.CopyFormatter
import com.elp.datatool.domain.copy.LineFormat
import com.elp.datatool.domain.model.AdapterSettings
import com.elp.datatool.domain.model.DataSource
import com.elp.datatool.data.sample.SampleData
import com.elp.datatool.presentation.FakeCollectionQueue
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsViewModelTest {

    private val settings = InMemorySettingsRepository()
    private val results = InMemoryResultRepository()
    private val cache = ProductCache()
    private val supportedSources = setOf(DataSource.UNITY_ASSET_STORE, DataSource.AMAZON_BOOKS)
    private val viewModel = SettingsViewModel(
        settings,
        results,
        cache,
        FakeCollectionQueue(supportedSources),
    )

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

    @Test
    fun searchEngineFallback_changeUpdatesSettings() {
        viewModel.onSearchEngineFallbackChange(DataSource.UNITY_ASSET_STORE, true)

        assertEquals(true, settings.settings.value.settingsFor(DataSource.UNITY_ASSET_STORE).searchEngineFallbackEnabled)
        assertEquals(false, settings.settings.value.settingsFor(DataSource.AMAZON_BOOKS).searchEngineFallbackEnabled)
    }

    @Test
    fun adapterSources_includeOnlyRegisteredAdapters() {
        assertEquals(supportedSources.toList(), viewModel.adapterSources)
    }

    @Test
    fun adapterCopySettings_overrideGlobalValuesAndResetToGlobal() {
        val source = DataSource.UNITY_ASSET_STORE
        viewModel.onAdapterCopyLineFormatChange(source, LineFormat.CSV)
        viewModel.onRemoveAdapterCopyField(source, CopyField.CATEGORIES)
        viewModel.onAdapterDetailsTemplateChange(source, "{sku}: {title}")

        val configured = settings.settings.value
        assertEquals(LineFormat.CSV, configured.copyLineFormatFor(source))
        assertEquals(false, CopyField.CATEGORIES in configured.copyLineFieldsFor(source))
        assertEquals("{sku}: {title}", configured.copyDetailsTemplateFor(source))
        assertEquals(AdapterSettings(), configured.settingsFor(DataSource.AMAZON_BOOKS))

        viewModel.onCustomCopySettingsChange(source, false)
        val disabled = settings.settings.value
        assertEquals(disabled.copyLineFormat, disabled.copyLineFormatFor(source))
        assertEquals(disabled.copyDetailsTemplate, disabled.copyDetailsTemplateFor(source))
        assertEquals(LineFormat.CSV, disabled.settingsFor(source).copyLineFormat)

        viewModel.onCustomCopySettingsChange(source, true)
        assertEquals(LineFormat.CSV, settings.settings.value.copyLineFormatFor(source))
        assertEquals("{sku}: {title}", settings.settings.value.copyDetailsTemplateFor(source))

        viewModel.onResetAdapterCopyLine(source)
        viewModel.onResetAdapterDetailsTemplate(source)

        val reset = settings.settings.value
        assertEquals(reset.copyLineFormat, reset.copyLineFormatFor(source))
        assertEquals(reset.copyLineFields, reset.copyLineFieldsFor(source))
        assertEquals(reset.copyDetailsTemplate, reset.copyDetailsTemplateFor(source))
    }

    @Test
    fun clearAdapterData_keepsOtherAdapterResultsAndCache() {
        val unity = SampleData.results.first()
        val amazon = unity.copy(source = DataSource.AMAZON_BOOKS, productId = "1098119517")
        results.upsert(unity)
        results.upsert(amazon)
        cache.put(unity)
        cache.put(amazon)

        viewModel.onClearAdapterData(DataSource.UNITY_ASSET_STORE)

        assertEquals(listOf(amazon), results.results.value)
        assertEquals(null, cache.get(unity.key))
        assertEquals(amazon, cache.get(amazon.key))
    }
}
