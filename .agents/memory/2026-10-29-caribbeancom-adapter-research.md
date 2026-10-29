# Caribbeancom adapter research documentation

- Reviewed the sibling Rust `caribbeancom` adapter (`jdata-tool/src-tauri/src/adapters/carib.rs`) and the shared URL, date, HTTP, registry, and cache helpers. The Rust code has no Caribbeancom fixtures or tests.
- Live-checked SKU `091317-498` on 2026-10-29: English and Japanese product pages returned HTTP 200 with no challenge. The Japanese page is served as EUC-JP.
- Found that the English not-found page returns HTTP 200 with an empty `h1[itemprop=name]`, while the Japanese not-found page returns HTTP 404.
- Confirmed live selectors for title, performers (English names are comma-separated in one element), tags, duration, Japanese description, and series. The English page has no description.
- Confirmed `robots.txt` disallows nothing and publishes three sitemaps.
- Added raw evidence in `.knowledge/sources/caribbeancom-adapter-observations.md`.
- Added the Android migration guide in `.knowledge/pages/caribbeancom-adapter-research.md`: strict resolver, endpoint design, parsing contract, validated SKU-derived date, error policy, cache, fixtures, tests, implementation order, and acceptance criteria.
- Registered the page in the knowledge README, `index/by-id.yaml`, and `index/by-tag.yaml` (new `charset` tag), and linked it from the Rust adapters overview.
- No application code was changed.
