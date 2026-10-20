# Raw — Steam Store observations

Unedited working notes. Curated form: [Steam Store crawl](../pages/steam-store-crawl.md).

Samples verified **2026-07-20** (browser User-Agent + English locale):

| App id | URL |
|--------|-----|
| `2543760` | https://store.steampowered.com/app/2543760/Shadow_of_the_Ninja__Reborn/ |
| `7510` | https://store.steampowered.com/app/7510/XBlades/ |

## URL shapes

All return **200** + store HTML (slug optional):

```text
https://store.steampowered.com/app/2543760
https://store.steampowered.com/app/2543760/
https://store.steampowered.com/app/2543760/Shadow_of_the_Ninja__Reborn/
https://store.steampowered.com/app/7510/XBlades/
```

- Slug is decorative; crawl can normalize to `https://store.steampowered.com/app/{id}/`.
- `rel=canonical` keeps the slug form when present.

## Store API (preferred)

```text
GET https://store.steampowered.com/api/appdetails?appids={id}&l=english
```

Returns JSON `{ "{id}": { "success": true, "data": { ... } } }`.

Verified keys useful for crawl: `name`, `steam_appid`, `developers`, `publishers`, `release_date`, `website`, `short_description`, `genres`, `categories`, `platforms`, `price_overview`, `header_image`, `metacritic`, `recommendations`, `supported_languages`, `type`.

**Not present** in API for tested apps: `distributor(s)`, `series`, `franchise` object.

## HTML that worked (fallback)

| Field | Selector / source |
|-------|-------------------|
| Title | `#appHubAppName` (also `og:title` → `"… on Steam"`) |
| Release date | `.release_date .date` |
| Developer(s) | `#developers_list a` / glance `dev_row` labeled `Developer:` |
| Publisher(s) | glance `dev_row` labeled `Publisher:` → one or more `<a>` |
| Details block | `#genresAndManufacturer .details_block` (Title / Genre / Developer / Publisher / Release Date) |
| Tags | `.app_tag` |
| Short description | `.game_description_snippet` |
| Reviews | `#userReviews` / `data-featuretarget="appreviews"` JSON props |
| Canonical URL | `link[rel=canonical]` |

## Sample parse — `2543760` Shadow of the Ninja - Reborn

| Field | Value |
|-------|-------|
| Title | Shadow of the Ninja - Reborn |
| Developer(s) | Winning Entertainment Group Inc. |
| Publisher(s) | Winning Entertainment Group Inc., Edigger, ININ |
| Release date (HTML + API) | 29 Aug, 2024 |
| Distributor(s) | *(not on Steam page/API)* |
| Series / Franchise | *(no Franchise glance row; only A/B recommend_franchise widget ids)* |
| URL | https://store.steampowered.com/app/2543760/Shadow_of_the_Ninja__Reborn/ |
| Genres | Action, Adventure, RPG |
| Platforms | Windows |
| Reviews | Mostly Positive (~591) |
| Price (locale VND) | 260.000₫ |

## Sample parse — `7510` X-Blades

| Field | Value |
|-------|-------|
| Title | X-Blades |
| Developer(s) | Gaijin Entertainment |
| Publisher(s) | Topware Interactive |
| Release date (HTML glance) | **10 Feb, 2009** |
| Release date (API `release_date.date`) | **30 Apr, 2009** |
| Release unix (`app_release_date`) | `1241121600` → 2009-04-30 UTC |
| Distributor(s) | *(not on Steam page/API)* |
| Series / Franchise | *(not present)* |
| URL | https://store.steampowered.com/app/7510/XBlades/ |
| Genres | Action, Adventure, RPG (+ content genres Nudity, Violent in API) |
| Platforms | Windows, Mac, Linux |
| Metacritic | 54 |
| Reviews | ~1035 recommendations |
| Price (locale VND) | 180.000₫ |

> **Release date mismatch** on older titles: prefer API `release_date.date` or unix `app_release_date` for a single source of truth; document HTML glance separately if needed.

## Age / region notes

- Mature titles may redirect to age check without cookies (`birthtime`, `wants_mature_content=1`). Both samples loaded without gate under those cookies.
- Price / currency follow store country (observed VND here).
- Prefer `l=english` on API for stable English metadata.

## Store search API

```text
GET https://store.steampowered.com/api/storesearch/?term={keyword}&l=english&cc=US
```

Verified `term=x-blades` (2026-07-20):

| id | name | type |
|----|------|------|
| 7510 | X-Blades | app |
| 604290 | X-Blades - Soundtrack | app |
| 3050910 | X-Blades - HD Upgrade | app |
| 604280 | X-Blades - Digital Deluxe Content | app |

Also works: `/search/suggest?term=…&f=games` (HTML snippet with `data-ds-appid`) — prefer JSON `storesearch` for adapters.

## Franchise / Series / Distributor

- Steam glance usually only shows **Developer** + **Publisher** + **Release Date**.
- Some titles link `/franchise/{name}` in recommendation widgets (`recommend_franchise`, `more_from_franchise`) — not a reliable “Series” field.
- **Distributor(s)** is a Wikipedia/box-style field; **not exposed** on Steam Store HTML or `appdetails` for tested apps → leave empty unless another source is added later.
