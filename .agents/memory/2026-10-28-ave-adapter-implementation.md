# Ave adapter implementation

- Added `DataSource.AVE` with normalized `dvd1` SKU handling and strict HTTPS AV Entertainments product-URL validation.
- Implemented `AveAdapter` and `AveParser` with candidate search, exact normalized SKU resolution, grouped and flat product metadata parsing, block detection, and English/Japanese page loading.
- Added locale-indexed metadata to `ProductResult`; Ave stores Japanese metadata under `localized["ja"]` and the result-detail UI displays it with English labels.
- Added stable `studio`, `release_date`, and `series` attributes and corresponding detail labels.
- Registered Ave in the application container, source selector labels, copied source names, and project documentation.
- Updated the in-memory product cache to store an alias for the original request key so direct URL inputs can reuse results even when the parsed SKU becomes the result identity.
- Added local search, English product, and Japanese product fixtures plus parser, adapter, URL normalization, block-page, and data-source validation tests.
- Verified the complete debug unit-test suite with `gradlew.bat testDebugUnitTest`.
- `lintDebug` still reports 12 pre-existing Compose `LocalContextGetResourceValueCall` errors in Collect, Results, Result Detail, and Settings screens; the lint report contains no Ave adapter error.
