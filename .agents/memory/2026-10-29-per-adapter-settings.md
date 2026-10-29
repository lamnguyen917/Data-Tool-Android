# Per-adapter settings

## Change summary

- Added the extensible `AdapterSettings` model and an `AppSettings.adapterSettings` map keyed by `DataSource`. Adapter settings now include search fallback, optional copy-line format and fields, and an optional copy-details template.
- Changed the search-engine fallback setting from one global value to an independent value for every adapter.
- Added migration from the former global SharedPreferences key. Its value becomes the default for each source without an existing source-specific key, then future writes use only source-specific keys.
- Added an Adapters section to the main Settings screen. It lists only adapters registered in `CollectionQueue`, so unavailable sources are not presented as configurable adapters.
- Added the `settings/adapter/{sourceId}` navigation route and a dedicated adapter settings screen.
- Moved the DuckDuckGo fallback toggle into the adapter settings screen. Changing Unity Asset Store no longer changes Amazon Books.
- Added adapter-scoped Copy line and Copy details editors. Missing overrides dynamically inherit the global copy settings; resetting an adapter editor removes its override instead of copying a snapshot of the global value.
- Added an adapter-scoped Clear local data action with confirmation. It removes only cached products and collected results belonging to the selected source.
- Kept all new labels and comments in English.

## Verification

- Updated repository tests to construct source-specific settings.
- Updated Settings ViewModel tests to verify adapter isolation, registered-adapter filtering, copy override/reset behavior, and source-specific local-data clearing.
- Ran the targeted adapter-search and Settings ViewModel unit tests successfully on 2026-10-29.
