# Raw — IGDB observations

Unedited working notes. Curated form: [IGDB crawl](../pages/igdb-crawl.md).

Sample URL verified **2026-07-20**: https://www.igdb.com/games/army-general

## Website HTML (blocked)

Plain `curl` / reqwest **without browser challenge** returns Cloudflare block, not game HTML:

| Signal | Observed |
|--------|----------|
| HTTP status | **403** |
| Body size | ~5.7 KB |
| Title | `Just a moment...` |
| Header | `cf-mitigated: challenge` |
| Content | Cloudflare JS challenge (`challenges.cloudflare.com`) |

No `#appHubAppName`-style markers — page is not parseable as product HTML.

> Do **not** treat HTTP 200/403 alone as success; require API JSON or post-challenge browser HTML.

## Public URL shape

```text
https://www.igdb.com/games/{slug}
https://www.igdb.com/games/army-general
```

- `{slug}` is url-safe lowercase (hyphens), **not** numeric id in the path.
- Resolve crawl input by extracting slug from `/games/{slug}` or querying API by slug.

## API v4 (works; auth required)

Base: `https://api.igdb.com/v4`

Unauthenticated POST returns:

```json
{
  "message": "Authorization Failure. Have you tried:",
  "Tip 1": "Ensure you are sending Authorization and Client-ID as headers.",
  ...
}
```

### Auth (Twitch OAuth2 client credentials)

```text
POST https://id.twitch.tv/oauth2/token?client_id={id}&client_secret={secret}&grant_type=client_credentials
→ { "access_token": "...", "expires_in": ..., "token_type": "bearer" }
```

Headers for IGDB requests:

```text
Client-ID: {client_id}
Authorization: Bearer {access_token}
Content-Type: text/plain
Accept: application/json
```

Rate limit: **4 requests / second** (429 if exceeded).

### Slug lookup (Apicalypse)

String values must use **double quotes**:

```text
POST https://api.igdb.com/v4/games
Body:
fields id,name,slug,summary,url,first_release_date,
  franchise.name,franchises.name,
  involved_companies.company.name,involved_companies.developer,involved_companies.publisher,
  release_dates.human,release_dates.date,release_dates.platform.name,
  genres.name,platforms.name,
  external_games.uid,external_games.url,external_games.external_game_source.name;
where slug = "army-general";
```

### Keyword search

```text
POST https://api.igdb.com/v4/games
Body:
search "Army General"; fields id,name,slug,first_release_date; limit 20;
```

Edition noise: add `where version_parent = null;` when you want base games only.

## Sample game — `army-general` (cross-ref, not live API pull)

Independent sources (Steam / MobyGames / developer site) for the same title:

| Field | Expected value |
|-------|----------------|
| Title | Army General |
| Slug | `army-general` |
| Developer(s) | Phobetor |
| Publisher(s) | Phobetor; Plug In Digital |
| Release | Apr 26, 2017 |
| Summary | Turn-based WW2 North Africa / Mediterranean strategy |
| Steam external id | `606950` (via `external_games`, category steam) |
| IGDB page | https://www.igdb.com/games/army-general |

Re-verify with authenticated API before treating numeric `id` as canonical.

## Distributor field

IGDB has **involved_companies** roles: `developer`, `publisher`, `porting`, `supporting`.

No Wikipedia-style **Distributor(s)** field observed in Game / involved_companies schema.

## Franchise / Series

Game object exposes `franchise` (single) and `franchises` (array). Use `franchise.name` when present for Series-like output.
