# Tokyo-Hot adapter implementation

- Added `DataSource.TOKYO_HOT` with normalization for classic `kNNNN` and `nNNNN` IDs, support for current alphanumeric Product IDs, and strict HTTPS Tokyo-Hot product-URL validation.
- Implemented `TokyoHotAdapter` and `TokyoHotParser` with current search-card parsing, exact Product ID resolution, English and Japanese page loading, challenge-page detection, and non-fatal localized-page failure.
- Fixed the Rust reference behavior that could resolve `n0001` to the earlier `HTN0001` result through substring matching.
- Mapped performers, play categories, tags, theme, series, studio, release date, duration, description, preview image, and canonical URL into the Android product model.
- Added stable `duration` and `theme` product attributes and exposed them in both primary and Japanese result details.
- Registered Tokyo-Hot in the application container, source selector labels, copied source names, and project status documentation.
- Added local search and bilingual product fixtures plus parser, adapter, block-page, URL, normalization, and exact-match regression tests.
- Added a normally ignored live smoke test and temporarily enabled it to verify search and bilingual fetch for live `n0001` on 2026-10-28.
- Verified the complete debug unit-test suite with `gradlew.bat testDebugUnitTest` and the live smoke test with its dedicated Gradle test filter.
- `lintDebug` still reports the same 12 pre-existing Compose `LocalContextGetResourceValueCall` errors; the lint text report contains no Tokyo-Hot adapter finding.
