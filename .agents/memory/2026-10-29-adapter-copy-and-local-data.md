# Adapter copy settings and local data

## Change summary

- Extended `AdapterSettings` with `customCopyEnabled` plus nullable `copyLineFormat`, `copyLineFields`, and `copyDetailsTemplate` overrides.
- Added effective-value helpers on `AppSettings`. Disabled custom copy always follows global settings. When enabled, a null individual override still follows the corresponding current global setting.
- Persisted adapter copy overrides with source-specific SharedPreferences keys. Empty field lists and empty templates remain valid custom values, while absent keys mean “use global settings.”
- Added a Custom copy settings switch and dedicated adapter Copy line and Copy details routes that reuse the existing editors and preview behavior. Turning the switch off preserves custom values for later re-enabling.
- Updated result copy actions and result-detail copy text to select effective settings from the result source. Mixed-source multi-copy applies each result’s adapter format and fields.
- Added source-scoped `clear` operations to `ResultRepository` and `ResponseCache`.
- Added a destructive Clear local data row and confirmation dialog to each adapter settings screen. It deletes only that adapter’s collected results and cached products, then shows an English confirmation snackbar.
- Adapter settings themselves are retained when local data is cleared.

## Verification

- Added Settings ViewModel coverage for adapter copy overrides, reset-to-global behavior, and selective result/cache deletion.
- Added Results ViewModel coverage proving copy line and details actions use the result adapter’s overrides.
- Ran the targeted Settings and Results ViewModel tests successfully on 2026-10-29.
