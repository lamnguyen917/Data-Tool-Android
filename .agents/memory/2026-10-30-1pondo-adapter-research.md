# 1pondo adapter research

## Request

Research 1pondo and create implementation documentation for a future Android adapter.

## Changes

- Added `.knowledge/sources/1pondo-adapter-observations.md` with Rust source review and live evidence from the official SPA, product JSON endpoint, list feed, 404 response, robots policy, sitemap, and JavaScript search flow.
- Added `.knowledge/pages/1pondo-adapter-research.md` with the strict input contract, deterministic fetch flow, DTO and field mapping, English/Japanese localization policy, search fallback, serialized browser-verification behavior, error policy, cache design, privacy rules, fixtures, tests, implementation sequence, and acceptance criteria.
- Added the new page to `.knowledge/README.md`, `.knowledge/index/by-id.yaml`, and the relevant entries in `.knowledge/index/by-tag.yaml`.
- Updated `.knowledge/pages/rust-desktop-adapters.md` so its 1pondo stub points to the dedicated live research.

## Verification

- Verified `https://www.1pondo.tv/movies/010121_001/` returns an HTTP 200 SPA shell without embedded product metadata.
- Verified `https://www.1pondo.tv/dyn/phpauto/movie_details/movie_id/010121_001.json` returns HTTP 200 UTF-8 JSON with localized fields.
- Verified an unknown metadata SKU returns HTTP 404 HTML.
- Verified the newest list endpoint returns paged JSON with 50 rows.
- Verified the official page sitemap contains 4,871 movie routes on 2026-10-30.
- Confirmed no application source code was changed; implementation remains pending.
