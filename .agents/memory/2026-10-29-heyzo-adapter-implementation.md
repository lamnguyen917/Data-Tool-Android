# Heyzo adapter implementation

- Added `DataSource.HEYZO` with strict four-digit SKU validation and trusted HTTPS product URL parsing for `www.heyzo.com` and `en.heyzo.com`.
- Preserved leading zeroes and rejected foreign hosts, non-default ports, queries, fragments, and unexpected paths.
- Added and registered `HeyzoAdapter` with deterministic English and Japanese product URLs and explicit unsupported keyword search.
- Added `HeyzoParser` for English title, performers, Type, Sex Styles, Japanese title, performers, actor types, keyword tags, and release date.
- Kept English categories in the top-level result and Japanese categories in `localized["ja"]`.
- Made Japanese requests best-effort after a successful English result while preserving coroutine cancellation.
- Added Cloudflare, CAPTCHA, age-gate, access-denied, generic not-found, and malformed product-page classification.
- Added English UI source labels, copy/export display name, performer detail labeling, and Collect source integration.
- Added local English, Japanese, and Cloudflare fixtures plus resolver, parser, adapter, HTTP error, localization fallback, and unsupported-search tests.
- Updated the Heyzo research page, knowledge indexes, and project documentation to reflect the registered implementation and current live Cloudflare block.
- Verified with `gradlew.bat testDebugUnitTest`; the build and all unit tests passed.
