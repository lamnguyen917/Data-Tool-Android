# GOG adapter implementation

Date: 2026-10-29

## Request

Implement the GOG adapter from the verified GOG crawl documentation.

## Changes

- Added `DataSource.GOG` with validation for numeric product ids, bare slugs, locale-free product URLs, and localized product URLs.
- Added `GogAdapter`, which searches through the public Catalog API and fetches product details through the public Product API.
- Added `GogParser`, which parses catalog/product JSON, validates product identity, sanitizes description HTML, and merges the responses into `ProductResult`.
- Registered `GogAdapter` in `AppContainer`, making collection, native search, adapter settings, cache clearing, and custom copy settings available for GOG.
- Added GOG source labels and validation messages to the UI.
- Added GOG to the DuckDuckGo search fallback with strict trusted-host and product-path validation.
- Added GOG-specific result attributes for GOG release date, platforms, and features. Original release date remains mapped to `PUBLICATION_DATE`.
- Updated result details so GOG creators are labeled as developers while publishers remain a separate attribute.
- Updated the GOG knowledge page to point to the implementation.

## Mapping decisions

- Search hits and completed results use the numeric GOG product id as the stable SKU.
- Catalog `releaseDate` maps to the original publication date.
- Catalog `storeReleaseDate`, with Product API `release_date` as fallback, maps to the separate GOG release date.
- Catalog developers map to `ProductResult.creators`; publishers map to the publisher attribute.
- Catalog `reviewsCount` maps to rating count, matching the verified page JSON-LD semantics.
- Product API description HTML is converted to plain text before storage.
- Catalog filter dictionaries are ignored and never mixed into product tags or genres.

## Tests

- Added Catalog and Product API fixtures for Alien Nations.
- Added parser tests for search mapping, HTML sanitization, metadata mapping, and separate original/store dates.
- Added adapter tests for search URL encoding, slug resolution, numeric id resolution, and exact-slug not-found behavior.
- Added data-source validation tests for trusted and untrusted GOG URLs.
- Added DuckDuckGo parser coverage for localized GOG product URLs.
- Ran `gradlew.bat clean testDebugUnitTest --no-daemon` with the available local JDK.
- Result: `BUILD SUCCESSFUL`.

