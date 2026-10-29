# Caribbeancom adapter implementation

- Added `DataSource.CARIBBEANCOM("caribbeancom")` and a strict resolver: raw `MMDDYY-NNN`, or HTTPS `www.caribbeancom.com/moviepages/{sku}/[index.html]` and `en.caribbeancom.com/eng/moviepages/{sku}/[index.html]` without user info, custom ports, query, or fragment.
- Added `CaribbeancomAdapter`: fixed English and Japanese URLs rebuilt from the SKU, English required (wrapped with `withBrowserChallenge`), Japanese optional and stored in `localized["ja"]`, keyword search `UNSUPPORTED`.
- Added `CaribbeancomParser`: label-based spec rows, comma-split English performers, English tags as categories, duration, studio, SKU-derived release date with calendar validation, Japanese description with preserved line breaks, Japanese performers, tags, and series. English HTTP 200 soft not-found and Japanese 404 pages map to `NOT_FOUND`; challenge pages map to `BLOCKED`.
- Registered the source in `AppContainer`, `SourceLabels`, `CopyFormatter.displayName`, `ResultDetailScreen` performer label, `DuckDuckGoSearchEngine` scope and trusted hosts, and English string resources.
- Added sanitized fixtures from the live `091317-498` pages; Japanese fixtures are stored as raw EUC-JP bytes.
- Added parser, adapter (MockWebServer with EUC-JP `Content-Type`), validation, and ignored live smoke tests. The full unit suite (155 tests) and `assembleDebug` pass; the live smoke test passed once against the real site.
- Updated `.knowledge/pages/caribbeancom-adapter-research.md`, `index/by-id.yaml`, and `pages/rust-desktop-adapters.md` to status implemented.
