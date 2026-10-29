# GOG adapter documentation

Date: 2026-10-29

## Request

Analyze `https://www.gog.com/game/alien_nations` and document the expected GOG adapter behavior.

## Changes

- Added `.knowledge/pages/gog-crawl.md` with the GOG adapter source contract, URL rules, resolution pipeline, search behavior, field mapping, failure handling, and smoke checks.
- Added `.knowledge/sources/gog-alien-nations-observations.md` with evidence captured from the GOG Catalog API, Product API, English product page, JSON-LD, and missing-product behavior.
- Added `gog-crawl` to the knowledge README and the by-id/by-tag indexes.

## Verified findings

- The locale-free product URL can redirect to a localized page. The adapter should normalize public links to `https://www.gog.com/en/game/{slug}`.
- Product id `1207658717` and slug `alien_nations` identify Alien Nations.
- The public Catalog API supports keyword search and returns the original release date, GOG store release date, developer, publisher, platform, genres, tags, price, and covers.
- The public Product API returns description HTML, languages, compatibility, links, images, and screenshots after resolution by numeric product id.
- Product API and page JSON-LD release dates represent the GOG store release date for the sample, not the original game release date.
- An unknown product page can redirect to `/en/games` with HTTP 200, so success requires exact identity or canonical-path validation.
- Catalog `reviewsCount` matched the JSON-LD rating count, not the written review count, for the sample.

## Validation scope

- Confirmed the live sample through anonymous HTTP requests on 2026-10-29.
- Confirmed an impossible catalog query returns an empty `products` array.
- Confirmed an impossible page slug finishes at the catalog page with HTTP 200.
- No application code was changed for this documentation task.

