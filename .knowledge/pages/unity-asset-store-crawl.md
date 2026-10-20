---
id: unity-crawl
title: Unity Asset Store — crawl
status: verified
updated: 2026-07-20
tags:
  - unity
  - asset-store
  - crawl
  - graphql
  - html
  - redirect
source: ../sources/unity-asset-store-observations.md
---

# Unity Asset Store — crawl

How to fetch and extract package metadata from [Unity Asset Store](https://assetstore.unity.com). Site behavior only — no application-specific mapping.

Related: [raw observations](../sources/unity-asset-store-observations.md)

## Quick facts

| Key | Value |
|-----|-------|
| Host | `https://assetstore.unity.com` |
| Primary strategy | **HTML product page** (follow redirects) |
| GraphQL | **Do not use** (CSRF / session required) |
| Package id example | `36050` → Azure[Sky] Dynamic Skybox |

## Do / Don't

| Do | Don't |
|----|-------|
| Resolve bare package id via seed URL `/packages/unknown/{id}` | Rely on DuckDuckGo as the primary id resolver |
| Follow HTTP redirects to the canonical product URL | Assume the seed URL is the final product URL |
| Parse embedded product JSON + `application/ld+json` | Call `/api/graphql/batch` from a plain HTTP client |
| Take canonical URL from `og:url` or `offers.url` | Use `/packages/{id}` alone (missing middle segment → wrong/404 routing) |
| Use any non-empty category token in the seed path (`unknown`, `x`, …) | |

## Input → seed URL

```text
INPUT
├─ full package URL  → use as-is (strip ?# if normalizing)
│     https://assetstore.unity.com/packages/{cat…}/{slug}-{id}
├─ path /packages/…  → prepend https://assetstore.unity.com
├─ host without scheme assetstore.unity.com/packages/…
└─ bare id /^\d{3,}$/ → https://assetstore.unity.com/packages/unknown/{id}
```

### Seed URL rule (critical)

```text
https://assetstore.unity.com/packages/{categoryToken}/{packageId}
```

- `{categoryToken}`: **required**; any non-empty token works for redirect (`unknown` is a convenient convention).
- Without the middle segment, Asset Store does **not** resolve the package reliably.

### Redirect chain (verified)

Example id `36050`:

```text
GET /packages/unknown/36050
  → 301 /packages/unknown/azure-sky-dynamic-skybox-36050
  → 301 /packages/tools/particles-effects/azure-sky-dynamic-skybox-36050
  → 200 HTML product page
```

Canonical URL example:

```text
https://assetstore.unity.com/packages/tools/particles-effects/azure-sky-dynamic-skybox-36050
```

## Crawl pipeline

```text
resolve_seed_url(input)
  → GET seed (follow redirects)
  → read final URL + HTML body
  → canonical_url_from_html   # prefer og:url / ld+json offers.url
  → extract fields from HTML
```

### Package id extraction from URL

| URL shape | Extract |
|-----------|---------|
| `…/packages/…/slug-36050` | trailing `-(\d+)` |
| `…/packages/unknown/36050` | bare `/(\d+)/` under `/packages/` |

### HTML sources (priority)

1. **Embedded product JSON** in page body: object containing `"id":"{packageId}"`  
   - Rich fields: `name`, `description`, `slug`, `productId`, `firstPublishedDate`, `currentVersion.name`, `originalPrice`, `rating`, `category` (publisher often id-ref only).
2. **`application/ld+json`**
   - `@type: Product` → `name`, `description`, `brand.name` (publisher when embedded publisher is only an id-ref), `offers.price` / `offers.url`, `aggregateRating`
   - `@type: BreadcrumbList` → category path (typically drop `Home` and the last crumb = product title)

### Useful fields on the product page

| Concept | Typical source |
|---------|----------------|
| Package id | URL trailing digits / embedded `"id"` |
| Title | `name` |
| Categories | Breadcrumb names **or** `category.longName` / `category.name` |
| Author / publisher | `brand.name` (ld+json); embedded `publisher` may be `{type:id,…}` only |
| Canonical URL | `og:url` or `offers.url` |
| Price | `originalPrice.finalPrice` + currency / `offers.price` |
| Rating | average (+ count) |
| Version | `currentVersion.name` |
| Slug / productId | slug / productId |
| First published | `firstPublishedDate` (or version published date) |

## Keyword search (secondary)

- Method: DuckDuckGo HTML `site:assetstore.unity.com/packages {keyword}`
- Yields `/packages/…-{id}` links → package id + URL (+ slug as a title hint)
- Fragile; prefer seed URL + redirects when you already have a numeric package id

## GraphQL — dead end (do not retry without new evidence)

| Item | Detail |
|------|--------|
| Endpoint | `POST https://assetstore.unity.com/api/graphql/batch` |
| Observed | `csrf token mismatch` (400) or empty body + `302` → `/errors/unexpected` |
| Need | Browser session cookie `_csrf` **and** matching CSRF header/policy that plain HTTP clients do not satisfy reliably |

Only revisit GraphQL if someone documents a working anonymous or token-based auth flow.

## HTTP client requirements

- Browser-like `User-Agent`
- Cookie jar enabled
- Follow redirects
- Prefer caching by **package id**, not by seed URL path (seed and canonical URLs differ)

## Smoke checks

| Check | Expect |
|-------|--------|
| GET `…/packages/unknown/36050` (follow redirects) | Title/name mentions Azure; final URL contains `36050` |
| GET full canonical package URL | Same product fields; no seed hop required |
| Package id from URL | Works for both `…/slug-36050` and `…/unknown/36050` |

## Change checklist

When Asset Store crawl behavior changes:

1. Append evidence under [`sources/unity-asset-store-observations.md`](../sources/unity-asset-store-observations.md).
2. Promote into this page and refresh [`index/`](../index/) if id/tags/summary change.
