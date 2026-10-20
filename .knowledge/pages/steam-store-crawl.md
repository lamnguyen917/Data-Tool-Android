---
id: steam-crawl
title: Steam Store — crawl
status: verified
updated: 2026-07-20
tags:
  - steam
  - store
  - crawl
  - html
  - json
  - appid
source: ../sources/steam-store-observations.md
---

# Steam Store — crawl

How to fetch and extract game metadata from [Steam Store](https://store.steampowered.com). Site behavior only — no application-specific mapping.

Samples: [Shadow of the Ninja - Reborn](https://store.steampowered.com/app/2543760/Shadow_of_the_Ninja__Reborn/) (`2543760`), [X-Blades](https://store.steampowered.com/app/7510/XBlades/) (`7510`).

Related: [raw observations](../sources/steam-store-observations.md)

## Quick facts

| Key | Value |
|-----|-------|
| Host | `https://store.steampowered.com` |
| Primary strategy | **Store API JSON** `GET /api/appdetails?appids={id}` |
| Search | **Store search JSON** `GET /api/storesearch/?term={keyword}` |
| Fallback | HTML product page `#appHubAppName` + glance `dev_row` |
| Product id | Numeric **AppID** (e.g. `7510`, `2543760`) |
| Slug in URL | **Optional** |
| Series / Distributor | **Usually absent** on Steam (see below) |

## Do / Don't

| Do | Don't |
|----|-------|
| Normalize to `https://store.steampowered.com/app/{id}/` | Require the title slug segment |
| Prefer `appdetails` JSON for developers / publishers / release | Assume HTML glance release date always matches API (older titles can differ) |
| Send browser-like UA; set `l=english` on API | Ignore age-gate cookies for mature apps |
| Leave Series / Distributor empty when not on page | Invent Series from franchise A/B widget ids |
| Detect failed API (`success: false`) or age-check HTML | Treat HTTP 200 alone as parseable product |

## URL rule (critical)

```text
https://store.steampowered.com/app/{appId}/{optional-slug}/
```

Slug is **not required**. Equivalent for crawl:

```text
https://store.steampowered.com/app/2543760
https://store.steampowered.com/app/2543760/
https://store.steampowered.com/app/2543760/Shadow_of_the_Ninja__Reborn/
```

`rel=canonical` may keep the slug form; store **`https://store.steampowered.com/app/{id}/`** as normalized URL unless you need the canonical slug.

### Extract id from URL

| URL shape | Extract |
|-----------|---------|
| `…/app/2543760` | `/app/(\d+)` |
| `…/app/2543760/Shadow_of_the_Ninja__Reborn/` | same |
| bare id | `/^\d+$/` |

Strip query (`?`) and fragment (`#`).

## Input → seed

```text
INPUT
├─ full store URL …/app/{id}/…  → extract id → API + optional HTML
├─ path /app/{id}…              → prepend https://store.steampowered.com
└─ bare AppID /^\d+$/           → https://store.steampowered.com/app/{id}/
```

## Crawl pipeline

```text
resolve_app_id(input)
  → GET /api/appdetails?appids={id}&l=english
  → if success: map fields from data
  → else / for HTML-only fields: GET /app/{id}/ (cookies for age gate)
  → detect age-check page → fail loudly
```

### HTTP client requirements

- Browser-like `User-Agent`
- Cookie jar; for mature content set age cookies (e.g. `birthtime`, `wants_mature_content=1`)
- Prefer `Accept-Language: en-US` / API `l=english`
- Price/currency follow Steam country/IP — do not hard-code locale

### Failure signals

| Signal | Meaning |
|--------|---------|
| API `success: false` | Unknown id / region-blocked / unavailable |
| HTML age gate (`agecheck`) without product title | Need age cookies / confirm age |
| Missing `#appHubAppName` on store HTML | Not a product page |

## Field mapping (minimum + extras)

### Required / primary columns

| Column | API (`data`) | HTML fallback |
|--------|--------------|---------------|
| **Title** | `name` | `#appHubAppName` |
| **Developer(s)** | `developers[]` | `#developers_list a` / glance row `Developer:` |
| **Publisher(s)** | `publishers[]` | glance row `Publisher:` → `<a>` list |
| **Release date(s)** | `release_date.date` (+ `coming_soon`) | `.release_date .date` |
| **Distributor(s)** | — | **Not on Steam Store** for tested apps → empty |
| **Series** | — | Optional: `/franchise/{name}` links in widgets if present; else empty |
| **URL** | build `https://store.steampowered.com/app/{steam_appid}/` | `link[rel=canonical]` |

### Additional fields worth capturing

| Concept | Source |
|---------|--------|
| AppID | `steam_appid` / URL |
| Type | `type` (`game`, `dlc`, …) |
| Short description | `short_description` / `.game_description_snippet` |
| Genres | `genres[].description` / `#genresAndManufacturer` |
| Categories / features | `categories[].description` (Single-player, Steam Achievements, …) |
| Tags | HTML `.app_tag` (not in basic `appdetails`) |
| Platforms | `platforms.windows/mac/linux` |
| Price | `price_overview` (`final_formatted`, currency, discount) |
| Reviews count | `recommendations.total` / review summary rows |
| Metacritic | `metacritic.score` + `url` (when present) |
| Header image | `header_image` |
| Official website | `website` |
| Languages | `supported_languages` (HTML string) |
| Free? | `is_free` |

### Series / Distributor (important)

Steam Store is **not** Wikipedia. For both verified samples:

| App | Series | Distributor |
|-----|--------|-------------|
| `2543760` | empty | empty |
| `7510` | empty | empty |

- **Publisher ≠ Distributor.** Do not copy publishers into Distributor.
- **Franchise** widgets (`recommend_franchise` / `more_from_franchise`) are A/B experiment ids, not a Series title — only use an explicit `/franchise/…` link text/name if you need a Series-like value.

### Release date caveat

On **X-Blades** (`7510`):

| Source | Date |
|--------|------|
| HTML glance `.date` | 10 Feb, 2009 |
| API `release_date.date` | 30 Apr, 2009 |
| Embedded `app_release_date` unix | 2009-04-30 |

Prefer **API** (or unix) as canonical; keep HTML as secondary if you need “store page displayed date”.

## Keyword search — `search_items(keyword)`

Keyword search uses Steam’s official store search JSON (not HTML scrape):

```text
GET https://store.steampowered.com/api/storesearch/?term={keyword}&l=english&cc=US
```

### Response shape

```json
{
  "total": 4,
  "items": [
    { "type": "app", "name": "X-Blades", "id": 7510, "price": { ... }, "tiny_image": "..." }
  ]
}
```

| Map to `SearchHit` | Source |
|--------------------|--------|
| `sku` | `id` (AppID) |
| `title` | `name` |
| `url` | `https://store.steampowered.com/app/{id}/` |

### Rules

- URL-encode `term` (UTF-8).
- Keep items with `type == "app"` (games, DLC, soundtracks, …). Skip `bundle` / `package` for Process.
- Deduplicate by AppID (API can repeat).
- Cache key: `steam_search_{sanitized}.json`
- Resolve / Process still uses AppID via `appdetails` — search only supplies candidates.

### Verified example

`term=x-blades` → includes AppID `7510` (“X-Blades”), plus soundtrack / DLC app ids.

## Verified examples

### `2543760` — Shadow of the Ninja - Reborn

| Field | Value |
|-------|-------|
| Title | Shadow of the Ninja - Reborn |
| Developer(s) | Winning Entertainment Group Inc. |
| Publisher(s) | Winning Entertainment Group Inc.; Edigger; ININ |
| Release date | 29 Aug, 2024 |
| Distributor(s) | *(empty)* |
| Series | *(empty)* |
| URL | https://store.steampowered.com/app/2543760/Shadow_of_the_Ninja__Reborn/ |
| Genres | Action, Adventure, RPG |
| Platforms | Windows |

### `7510` — X-Blades

| Field | Value |
|-------|-------|
| Title | X-Blades |
| Developer(s) | Gaijin Entertainment |
| Publisher(s) | Topware Interactive |
| Release date (API) | 30 Apr, 2009 |
| Distributor(s) | *(empty)* |
| Series | *(empty)* |
| URL | https://store.steampowered.com/app/7510/XBlades/ |
| Genres | Action, Adventure, RPG |
| Platforms | Windows, Mac, Linux |
| Metacritic | 54 |

## Smoke checks

| Check | Expect |
|-------|--------|
| `GET /api/appdetails?appids=2543760&l=english` | `success: true`; `name` contains Shadow of the Ninja |
| `GET /app/2543760/` | `#appHubAppName` present; developers_list non-empty |
| Slug vs bare AppID URL | Same product; id still `2543760` |
| `GET /api/appdetails?appids=7510` | publishers include Topware Interactive |
| `GET /api/storesearch/?term=x-blades` | `items` contains app id `7510` named X-Blades |
| Missing Series/Distributor | Fields empty — not an error |

## Change checklist

When Steam Store crawl behavior changes:

1. Append evidence under [`sources/steam-store-observations.md`](../sources/steam-store-observations.md).
2. Promote into this page and refresh [`index/`](../index/).
3. Re-test AppIDs `2543760` and `7510` (API + one HTML page).
4. Re-test store search: `term=x-blades` still returns AppID `7510`.
