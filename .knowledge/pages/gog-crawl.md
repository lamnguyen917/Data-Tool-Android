---
id: gog-crawl
title: GOG - game crawl
status: verified
updated: 2026-10-29
tags:
  - gog
  - games
  - crawl
  - api
  - json
  - html
  - slug
  - search
source: ../sources/gog-alien-nations-observations.md
---

# GOG - game crawl

How to resolve, search, and extract game metadata from [GOG](https://www.gog.com). This page describes the adapter contract and source behavior; it does not document an implementation already present in the app.

Sample: [Alien Nations](https://www.gog.com/en/game/alien_nations) (`id=1207658717`, `slug=alien_nations`).

Related: [raw observations](../sources/gog-alien-nations-observations.md)

## Quick facts

| Key | Value |
|-----|-------|
| Public page | `https://www.gog.com/en/game/{slug}` |
| Catalog API | `https://catalog.gog.com/v1/catalog` |
| Product API | `https://api.gog.com/products/{id}` |
| Primary identity | Numeric GOG product id |
| Public identity | Product slug |
| Authentication | None observed for public metadata |
| Primary strategy | Catalog JSON -> exact product -> Product API JSON |
| HTML role | Fallback for requirements and language roles |

## Do / Don't

| Do | Don't |
|----|-------|
| Force `/en/game/{slug}` for stable English content | Depend on locale-free `/game/{slug}` staying English |
| Resolve/search with Catalog API and require an exact hit | Treat the first fuzzy search result as the requested product |
| Keep original and GOG store release dates separate | Map API/JSON-LD `release_date` to the original game release |
| Parse price as amount plus currency | Store a formatted price string without currency semantics |
| Verify final/canonical product path | Treat HTTP 200 as proof that a slug exists |
| Sanitize HTML descriptions before producing plain text | Expose raw description HTML directly in copied text |
| Ignore catalog-level `filters` dictionaries | Mix global filter values into the product's tags or genres |

## URL and input rules

Accepted public URL shapes:

```text
https://www.gog.com/game/{slug}
https://www.gog.com/{locale}/game/{slug}
```

Normalize both to:

```text
https://www.gog.com/en/game/{slug}
```

Input handling:

```text
INPUT
|- trusted GOG product URL
|    -> extract final path segment as slug
|- bare slug, for example alien_nations
|    -> resolve exact slug through Catalog API
|- numeric GOG product id
|    -> Product API -> obtain canonical slug
`- title or general keywords
     -> Catalog API search -> return selectable search hits
```

URL validation requirements:

- Require HTTPS and an exact host of `gog.com` or `www.gog.com`.
- Accept only a single non-empty product slug segment after `/game/`.
- Decode once, reject embedded slashes, traversal segments, controls, query-derived slugs, and host suffix spoofing.
- Strip query and fragment before identity comparison.
- Preserve GOG's returned slug instead of generating one from the title.

Do not restrict slugs to underscores only. Use a conservative URL-segment allowlist such as ASCII letters, digits, underscore, and hyphen, then confirm the value against GOG data.

## Recommended pipeline

```text
resolve_input(input)
  -> parse trusted URL, id, slug, or keyword
  -> Catalog API query for slug/title inputs
  -> select exact id or exact normalized slug
  -> GET Product API by numeric id with required expansions
  -> merge Catalog + Product API fields
  -> optionally GET /en/game/{slug} for missing fields
  -> validate identity across id, slug, title, and canonical URL
  -> map to adapter result
```

Suggested product expansion:

```text
GET https://api.gog.com/products/{id}
  ?expand=description,screenshots,videos,related_products,changelog
```

The adapter should not need a WebView for the verified JSON endpoints. If GOG later returns a bot challenge for the HTML fallback, keep the JSON-derived result and either omit HTML-only fields or use the app's serialized challenge flow.

## Catalog search

Request:

```text
GET https://catalog.gog.com/v1/catalog?query={urlEncodedQuery}&limit=20
```

Useful response fields:

| Purpose | JSON field |
|---------|------------|
| Result count | `productCount` |
| Product list | `products[]` |
| Product id | `products[].id` |
| Slug | `products[].slug` |
| Title | `products[].title` |
| Canonical URL | `products[].storeLink` |
| Original release | `products[].releaseDate` |
| GOG store release | `products[].storeReleaseDate` |
| Developer / publisher | `developers[]`, `publishers[]` |
| Platforms | `operatingSystems[]` |
| Genres / tags | `genres[]`, `tags[]` |
| Modes/features | `features[]` |
| Cover | `coverVertical` or `coverHorizontal` |
| Price | `price.finalMoney.amount`, `price.finalMoney.currency` |

Search result mapping:

| Search hit field | Source |
|------------------|--------|
| `sku` | `id` |
| `title` | `title` |
| `url` | `storeLink`, otherwise `/en/game/{slug}` |

Notes:

- Query matching is search behavior, not guaranteed exact lookup. Prefer exact `id`, then exact case-insensitive `slug`, then exact normalized title.
- Do not silently process a fuzzy hit when resolving a product URL or bare slug.
- `dreamlistGames` describes games requested by the community, not purchasable catalog products. Exclude it from normal adapter results.
- Ignore the top-level `filters`, `fullGenresList`, and `fullTagsList`; they describe the entire catalog.
- No result is `productCount == 0` or an empty `products` array, even though the response can still be large.

## Product API

After resolving a numeric id, the Product API contributes:

| Concept | JSON source |
|---------|-------------|
| Product id | `id` |
| Slug / title | `slug`, `title` |
| Description | `description.full`, fallback `description.lead` |
| Supported systems | `content_system_compatibility` |
| Languages | `languages` |
| Product type | `game_type` |
| Images | `images` |
| Screenshots | `screenshots[]` |
| Related content | `dlcs[]`, `related_products[]` |
| Store/support/forum links | `links` |

Description values contain HTML. Convert `<br>` and block/list boundaries to whitespace/newlines, decode entities, remove scripts/styles, and sanitize before storing or copying plain text.

Some image values are protocol-relative (`//images-...`). Normalize them to HTTPS before use.

## HTML fallback

Fetch only the explicit English page:

```text
GET https://www.gog.com/en/game/{slug}
```

Preferred extraction order:

1. `script[type="application/ld+json"]` containing `Product` / `VideoGame`.
2. Canonical and Open Graph metadata.
3. Server-rendered product sections for system requirements and audio/text language roles.
4. DOM selectors only as a last resort.

The HTML includes unresolved Angular placeholders from the global site shell. Select the JSON-LD node by type and matching `sku`/URL; do not parse every visible-looking title or price in the document.

### Missing slug trap

An unknown `/en/game/{slug}` can redirect to `/en/games` and finish with HTTP 200. Declare success only when at least one identity check passes:

- Catalog returned the exact id or slug.
- Product API returned the expected id/slug.
- Final and canonical page URLs still match `/en/game/{expectedSlug}`.

## Date semantics

GOG exposes two different dates:

| Meaning | Preferred source | Alien Nations example |
|---------|------------------|-----------------------|
| Original game release | Catalog `releaseDate` | `1999.07.01` |
| GOG store release | Catalog `storeReleaseDate` | `2014.12.19` |

Product API `release_date` and page JSON-LD `releaseDate` matched the GOG store release for the sample. They must not overwrite the original release date.

Store dates can include time-zone offsets. Parse them as offset date-times, then store the semantic date needed by the app without shifting it through an unrelated local time zone.

## Rating and review semantics

For Alien Nations:

| Value | Meaning |
|-------|---------|
| `aggregateRating.ratingValue = 3.5` | Score on a 1-5 scale |
| `aggregateRating.ratingCount = 405` | Number of ratings |
| `aggregateRating.reviewCount = 105` | Number of written reviews |
| Catalog `reviewsRating = 35` | Encoded score corresponding to 3.5 |
| Catalog `reviewsCount = 405` | Rating count despite the field name |

Prefer JSON-LD when both rating count and written-review count are required. Do not label Catalog `reviewsCount` as written reviews without independent confirmation.

## Price semantics

Price is live commerce data. Store:

- numeric final amount;
- ISO currency code;
- optional base amount and discount;
- observation timestamp if cached.

Never hard-code the sample price. Locale, country, cookies, promotions, ownership, and time can change the value. If amount or currency is missing, omit price rather than guessing.

## Recommended adapter field mapping

| Adapter concept | Preferred source | Notes |
|-----------------|------------------|-------|
| SKU / stable id | Catalog `id` | Keep as a string to avoid numeric assumptions |
| Title | Catalog `title` | Verify against Product API title |
| Canonical URL | Catalog `storeLink` | Normalize to HTTPS `/en/game/{slug}` |
| Description | Product API `description.full` | Sanitize HTML |
| Image | Catalog `coverVertical` | Product/API images are fallback |
| Developer | Catalog `developers[]` | Multiple values allowed |
| Publisher | Catalog `publishers[]` | Do not relabel as distributor |
| Publication date | Catalog `releaseDate` | Original game release |
| Categories | Catalog `genres[].name` | Preserve source order, deduplicate |
| Tags | Catalog `tags[].name` | Keep separate from genres |
| Platforms | Catalog `operatingSystems[]` | Product API compatibility can verify |
| Languages | Product API `languages` | HTML is needed for audio/text roles |
| Features | Catalog `features[].name` | For example single-player/multi-player |
| Price | Catalog `price.finalMoney` | Live and locale-sensitive |
| Rating | Page JSON-LD `aggregateRating` | Catalog score needs scale conversion |

Series, distributor, and detailed system requirements do not have reliable first-class fields in the verified JSON responses. Leave absent fields empty unless the HTML provides an unambiguous value; do not infer distributor from publisher.

## Verified sample

| Field | Value |
|-------|-------|
| Product id | `1207658717` |
| Slug | `alien_nations` |
| Title | Alien Nations |
| Original release | 1999-07-01 |
| GOG release | 2014-12-19 |
| Developer | Neo Software Produktions |
| Publisher | THQ Nordic GmbH |
| Platform | Windows |
| Languages | English, German, French |
| Genres | Strategy, Real-time, Fantasy |
| Tags | Fantasy, Strategy, Classic, Real-Time |
| Features | Multi-player, Single-player |
| Canonical URL | https://www.gog.com/en/game/alien_nations |

Price and review counts are intentionally not fixed in this table because they are live values.

## Failure handling

| Signal | Adapter behavior |
|--------|------------------|
| Catalog `products: []` | Not found |
| Exact slug absent but fuzzy hits exist | Return search choices; do not auto-process |
| Product page redirects to `/en/games` | Not found |
| JSON API 429 | Honor retry delay and back off |
| API 5xx / network failure | Retry within policy; keep partial data only if identity is verified |
| HTML challenge / access denial | Use JSON result; invoke serialized challenge flow only for required HTML fields |
| Product API id differs from resolved id | Reject as identity mismatch |

## Smoke checks

| Check | Expected result |
|-------|-----------------|
| Catalog query `alien nations` | Exact product id `1207658717`, slug `alien_nations` |
| Product API id `1207658717` | Title `Alien Nations`, same slug |
| English product page | Canonical `/en/game/alien_nations`, matching JSON-LD SKU |
| Original vs store release | 1999-07-01 vs 2014-12-19 |
| Impossible catalog query | `productCount: 0`, empty `products` |
| Impossible page slug | Redirect/final URL `/en/games`; must not be accepted |

## Change checklist

When GOG behavior changes:

1. Append dated evidence under [`sources/gog-alien-nations-observations.md`](../sources/gog-alien-nations-observations.md).
2. Re-test Catalog search, Product API, English page metadata, and a missing slug.
3. Confirm original/store date semantics and rating counters before changing mappings.
4. Promote verified behavior into this page and refresh [`index/`](../index/).

