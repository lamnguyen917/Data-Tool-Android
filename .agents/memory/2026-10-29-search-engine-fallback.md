# Search engine fallback

## Change summary

- Added `AdapterSettings.searchEngineFallbackEnabled` and stored adapter settings by `DataSource` in `AppSettings`. Each adapter defaults to `false` so product keywords are not sent to a third party unless the user opts in for that adapter.
- Persisted source-specific settings under `adapter_{sourceId}_search_engine_fallback` and added matching in-memory repository support for tests. The former global `search_engine_fallback` value is migrated to sources that do not yet have a source-specific key.
- Added a dedicated settings screen for each registered adapter with an English toggle and a privacy-oriented description that names DuckDuckGo.
- Added `DuckDuckGoSearchEngine`, which uses the HTML endpoint with a source-specific `site:` query.
- Updated `AdapterSearchRepository` to prefer native adapter results. The search engine runs only when native results are empty or native search returns `NOT_FOUND` or `UNSUPPORTED`.
- Native network, timeout, blocked, rate-limit, HTTP, and parse failures remain visible and do not trigger the fallback.
- Search-engine links are unwrapped, restricted to trusted source hosts, validated with `DataSource.validate`, deduplicated by product ID, and limited to 20 results.
- The fallback enables product search for adapters such as Amazon Books whose native keyword search is currently unsupported.

## Verification

- Added repository tests for enabled/disabled fallback, native-result precedence, unsupported native search, and operational error propagation.
- Added DuckDuckGo tests for scoped query construction, trusted-host filtering, deduplication, product URL validation, and redirect URL unwrapping.
- Added Settings ViewModel tests for source-specific updates and registered-adapter discovery.
