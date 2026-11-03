# 1pondo adapter implementation

## Request

Implement the Android 1pondo adapter from the live-verified research document.

## Application changes

- Added `DataSource.ONE_PONDO("1pondo")` and a strict shared resolver for `MMDDYY_NNN`, `https://www.1pondo.tv/movies/{sku}/`, and the official movie-details JSON URL.
- Added `OnePondoAdapter`, which fetches the deterministic public JSON endpoint, returns the public movie URL, reports native keyword search as unsupported, and attaches the product page to blocked requests for the existing serialized WebView verification flow.
- Added `OnePondoParser`, which validates `MovieID` and active status; maps English metadata with Japanese field fallback; creates `localized["ja"]`; preserves category order through `UC` and `UcNameList`; validates dates and image hosts; formats duration seconds; and omits member/sample file data.
- Registered the adapter in `AppContainer`.
- Added the DuckDuckGo scope `1pondo.tv/movies` and strict `www.1pondo.tv` result filtering.
- Added English source, short, SKU, and invalid-input labels; performer display; and the copy/export display name.
- Existing generic adapter settings automatically provide search fallback, custom copy settings, and source-scoped local-data clearing for 1pondo.

## Tests

- Added a sanitized product JSON fixture without real member or sample URLs.
- Added parser coverage for localization, fallback behavior, category ordering, duplicate removal, duration, date, rating, trusted images, privacy exclusions, identity mismatch, inactive products, and HTML challenges.
- Added MockWebServer coverage for deterministic requests, headers, 404 handling, HTTP/HTML blocks, challenge URLs, and unsupported native search.
- Added strict resolver and DuckDuckGo fallback tests.
- Added an ignored live smoke test for `010121_001`.
- Ran `gradlew testDebugUnitTest`: 167 tests completed, with 162 passed and 5 live tests skipped.
- Ran `gradlew assembleDebug`: the debug APK built successfully.

## Documentation

- Updated the 1pondo research page and knowledge indexes to `implemented`.
- Updated the Rust adapter overview to point to the completed Android implementation.
