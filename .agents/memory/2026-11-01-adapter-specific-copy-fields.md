# Adapter-specific copy fields

- Problem: adapter custom copy settings offered one fixed, Unity-oriented `CopyField` list, fell back to the global columns and template, and previewed Unity sample data for every adapter.
- Added `CopyField` entries for metadata other adapters collect: `publisher`, `publication_date`, `gog_release_date`, `language`, `print_length`, `isbn_10`, `isbn_13`, `platforms`, `features`.
- Added `DataSource.copyFields` (in `CopyFormatter.kt`): common fields plus the fields each adapter actually produces (Unity, Amazon Books, GOG; Steam and IGDB have a provisional list).
- Added `CopyFormatter.defaultLineFields(source)` and `CopyFormatter.defaultDetailsTemplate(source)` with adapter-specific columns and templates.
- `AppSettings.copyLineFieldsFor` / `copyDetailsTemplateFor`: when custom copy is enabled, missing overrides now fall back to the adapter defaults instead of the global settings, and stored columns are filtered to the adapter's fields. Disabled custom copy still uses the global settings. Adapter reset now returns to the adapter defaults.
- `SettingsViewModel.onAddAdapterCopyField` ignores fields the adapter does not collect. `previewResult(source)` uses the newest result of that source, then `SampleData.copyPreviews[source]` (new samples for Unity, Amazon, and GOG).
- Adapter copy-line and copy-details screens only list the adapter's fields and use adapter-specific labels via `CopyField.labelRes(source)` (for example `{author}` is "Publisher" on Unity, "Author" on Amazon, "Developer" on GOG; `{sku}` uses the adapter ID label). Placeholder chips show the label and the placeholder. The global screens still list every field with neutral labels.
- Fixed a bug where `CopyFormatter.details` did not replace placeholders containing digits (`{isbn_10}`, `{isbn_13}`); the placeholder regex now allows `[a-z0-9_]`.
- Added English string resources for the new field labels.
- Updated `SettingsViewModelTest` (reset now expects adapter defaults) and added tests for adapter field filtering, stored-field filtering, per-source preview, default template/field validity for every source, and Amazon/GOG default output.
