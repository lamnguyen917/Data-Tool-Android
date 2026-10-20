---
id: igdb-crawl
title: IGDB — crawl
status: verified
updated: 2026-07-20
tags:
  - igdb
  - games
  - crawl
  - api
  - json
  - slug
source: ../sources/igdb-observations.md
---

# IGDB — crawl

How to fetch and extract game metadata from [IGDB](https://www.igdb.com). Site behavior only — no application-specific mapping.

Sample: [Army General](https://www.igdb.com/games/army-general) (`slug=army-general`).

Related: [raw observations](../sources/igdb-observations.md)

## Quick facts

| Key | Value |
|-----|-------|
| Host (web) | `https://www.igdb.com` |
| Host (API) | `https://api.igdb.com/v4` |
| Primary strategy | **IGDB API v4** (`POST /games` with Apicalypse body) |
| Web HTML | **Cloudflare-protected** — not usable with plain HTTP client |
| Product key | Numeric **game id** (API) + url-safe **slug** (public URL) |
| Auth | Twitch developer **Client-ID** + OAuth2 **Bearer** token |
| Rate limit | 4 requests / second |

## Do / Don't

| Do | Don't |
|----|-------|
| Use API v4 for all structured fields | Scrape `www.igdb.com` HTML with curl/reqwest alone |
| Extract slug from `/games/{slug}` URLs | Assume slug equals Steam AppID |
| Quote string filters: `where slug = "army-general";` | Use single quotes in Apicalypse strings |
| Cache by slug or game id | Hammer API (>4 rps → 429) |
| Map `involved_companies.developer/publisher` | Invent a Distributor field (not in IGDB schema) |
| Refresh Twitch token before expiry | Commit Client Secret to repo |

## URL rule (critical)

Public game pages:

```text
https://www.igdb.com/games/{slug}
```

Example:

```text
https://www.igdb.com/games/army-general
```

The path segment is the **slug**, not the numeric IGDB id. Resolve via API:

```text
where slug = "army-general";
```

Canonical API field: `url` → `https://www.igdb.com/games/army-general`

### Extract slug from URL

| URL shape | Extract |
|-----------|---------|
| `…/games/army-general` | `/games/([a-z0-9-]+)` |
| bare slug `army-general` | valid if matches `^[a-z0-9-]+$` |

Strip query (`?`) and fragment (`#`).

## Input → resolve

```text
INPUT
├─ full IGDB URL …/games/{slug}
│     → extract slug → API where slug = "{slug}"
├─ bare slug army-general
│     → API where slug = "army-general"
└─ numeric IGDB id (if known)
      → API where id = {id};
```

## Crawl pipeline

```text
resolve_input(input)
  → obtain Twitch access_token (client_credentials)
  → POST https://api.igdb.com/v4/games
      Body: fields …; where slug = "{slug}";
  → map JSON game object → Result
  → (optional) search via search "keyword"; fields id,name,slug;
```

### Authentication

1. Register Twitch developer app → Client ID + Client Secret.
2. Token:

```text
POST https://id.twitch.tv/oauth2/token
  ?client_id={CLIENT_ID}
  &client_secret={CLIENT_SECRET}
  &grant_type=client_credentials
```

3. IGDB request headers:

```text
Client-ID: {CLIENT_ID}
Authorization: Bearer {access_token}
```

Docs: [api-docs.igdb.com](https://api-docs.igdb.com/#authentication)

### Failure signals

| Signal | Meaning |
|--------|---------|
| `Authorization Failure` JSON | Missing/invalid Client-ID or Bearer token |
| HTTP 429 | Rate limit (>4 req/s) |
| Empty array `[]` | Unknown slug / no match |
| Cloudflare HTML on www | Expected for bots — use API instead |

## Field mapping (minimum + extras)

### Required / primary columns

| Column | API source | Notes |
|--------|------------|-------|
| **Title** | `name` | |
| **Developer(s)** | `involved_companies` where `developer = true` → `company.name` | Expand nested fields in one query |
| **Publisher(s)** | `involved_companies` where `publisher = true` → `company.name` | Multiple publishers allowed |
| **Release date(s)** | `release_dates.human` (+ platform/region) | Multiple rows per platform; or `first_release_date` unix |
| **Distributor(s)** | — | **Not in IGDB** → leave empty |
| **Series** | `franchise.name` / `franchises[].name` | Optional; often empty |
| **URL** | `url` | IGDB canonical page |

### Recommended Apicalypse query (single game)

```text
fields id,name,slug,summary,storyline,url,first_release_date,
  franchise.name,franchises.name,
  involved_companies.company.name,involved_companies.developer,involved_companies.publisher,
  release_dates.human,release_dates.date,release_dates.platform.name,
  genres.name,platforms.name,themes.name,game_modes.name,
  rating,total_rating,aggregated_rating,
  cover.url,screenshots.url,
  external_games.uid,external_games.url,external_games.external_game_source.name;
where slug = "army-general";
```

### Additional fields worth capturing

| Concept | Source |
|---------|--------|
| IGDB id | `id` |
| Slug | `slug` |
| Summary / description | `summary`, `storyline` |
| Genres / themes | `genres.name`, `themes.name` |
| Platforms | `platforms.name`, `release_dates.platform.name` |
| Cover / screenshots | `cover.url`, `screenshots.url` |
| Ratings | `rating`, `total_rating`, `aggregated_rating` |
| External store ids | `external_games` (`uid` + source — Steam, GOG, Epic, …) |
| First release unix | `first_release_date` (UTC epoch seconds) |
| Game type / status | `game_type`, `game_status` |

### External games (Steam cross-link)

`external_games` links IGDB games to other stores. Steam category id = **1** (`uid` = AppID string).

For Army General, Steam AppID **606950** is expected (cross-ref Steam Store — verify via API).

### Distributor vs Publisher

IGDB `involved_companies` roles: `developer`, `publisher`, `porting`, `supporting`.

**Publisher ≠ Distributor.** Do not copy publishers into Distributor.

## Keyword search — `search_items(keyword)`

Keyword search pattern:

```text
POST https://api.igdb.com/v4/games
Body:
search "Army General"; fields id,name,slug,first_release_date; limit 25;
```

Optional filter for base games only:

```text
search "Assassins Creed"; fields id,name,slug; where version_parent = null; limit 25;
```

Map hits:

| SearchHit | Source |
|-----------|--------|
| `sku` | `id` (IGDB game id) **or** `slug` if adapter resolves slug |
| `title` | `name` |
| `url` | `url` or `https://www.igdb.com/games/{slug}` |

Prefer returning **slug** as process input when adapter resolves via slug URL.

## Verified example — `army-general`

Cross-reference (Steam / developer site; API query pending credentials):

| Field | Value |
|-------|-------|
| Slug | `army-general` |
| Title | Army General |
| Developer(s) | Phobetor |
| Publisher(s) | Phobetor; Plug In Digital |
| Release date | Apr 26, 2017 |
| Distributor(s) | *(empty — not in IGDB)* |
| Series | *(often empty; check `franchise` via API)* |
| URL | https://www.igdb.com/games/army-general |
| Genres (typical) | Strategy, Turn-based |
| Steam (external) | AppID `606950` via `external_games` |

## Smoke checks

| Check | Expect |
|-------|--------|
| `GET www.igdb.com/games/army-general` (plain bot) | Cloudflare challenge / 403 — **not** product HTML |
| `POST /v4/games` without auth | `Authorization Failure` JSON |
| `where slug = "army-general";` (with token) | One game; `name` contains Army General |
| `search "Army General";` | Includes slug `army-general` |
| Slug URL vs API `url` field | Same slug segment |

## Change checklist

When IGDB crawl behavior changes:

1. Append evidence under [`sources/igdb-observations.md`](../sources/igdb-observations.md).
2. Promote into this page and refresh [`index/`](../index/).
3. Re-test slug `army-general` and one search keyword with valid API credentials.
