# 1pondo adapter observations

Evidence reviewed on 2026-10-30 from the sibling Rust desktop project and a limited set of requests to the official 1pondo website.

## Evidence scope

Source files reviewed:

- `DataTool/src-tauri/src/adapters/ippondo.rs`
- `DataTool/src-tauri/src/adapters/mod.rs`
- the current Android adapter, search fallback, product model, and settings contracts
- the current 1pondo SPA shell and public JavaScript bundles

Official requests used for live verification:

| Request | Result |
|---|---|
| `GET https://www.1pondo.tv/` | HTTP 200 SPA shell; product data is not embedded in the HTML |
| `GET https://www.1pondo.tv/movies/010121_001/` | HTTP 200 SPA shell; no product-specific metadata in the initial HTML |
| `GET https://www.1pondo.tv/dyn/phpauto/movie_details/movie_id/010121_001.json` | HTTP 200, `application/json; charset=utf-8`, valid product object |
| `GET https://www.1pondo.tv/dyn/phpauto/movie_details/movie_id/999999_999.json` | HTTP 404, `text/html; charset=utf-8`, bilingual 404 page |
| `GET https://www.1pondo.tv/dyn/phpauto/movie_lists/list_newest_0.json` | HTTP 200, JSON page with 50 rows |
| `GET https://www.1pondo.tv/robots.txt` | HTTP 200; `/pu/` and `/sitemap.xml.gz` are disallowed |
| `GET https://www.1pondo.tv/static-seo/sitemap-pages.xml` | HTTP 200; 4,883 URLs, including 4,871 `/movies/{sku}/` URLs |

No CAPTCHA, age gate, login redirect, or Cloudflare challenge was observed. The server is behind Cloudflare, so challenge handling is still required defensively.

## Rust adapter behavior

- Adapter key: `1pondo`; registry alias: `ippondo`.
- Japanese localization flag: enabled.
- Raw SKU pattern: `^\d{6}_\d{3}$`.
- URL extraction pattern: `movie_id/(\d{6}_\d{3})`.
- Fixed metadata endpoint: `https://www.1pondo.tv/dyn/phpauto/movie_details/movie_id/{sku}.json`.
- Keyword search: not implemented.
- The response body is downloaded but never deserialized. The returned result contains only the SKU and endpoint URL; all metadata fields are empty.

`validate()` accepts any syntactically recognized URL, even when it does not contain a usable SKU. `parse()` is stricter and fails unless the raw SKU or `movie_id/{sku}` fragment is present. The host, scheme, port, user information, and complete path are not validated.

## Current website architecture

The public website is a JavaScript SPA. The initial HTML for both the home page and a movie route contains the application shell, not movie metadata. The current router exposes:

```text
/movies/:movieId/
/search/?s={query}
```

The movie-detail bundle requests the same static JSON endpoint used by the Rust adapter:

```text
/dyn/phpauto/movie_details/movie_id/{movieId}.json
```

The search page calls `getFTS(keywords, offset, limit, getPPV)`. That operation is transported through the site's Bifrost/WebSocket infrastructure with message data resembling:

```text
AppID: 1
MsgID: 100
Data: { Offset, Limit, Keywords, GetPPV }
```

It is not a stable anonymous REST endpoint suitable for the first Android implementation.

The SPA also references static list endpoints such as:

```text
/dyn/phpauto/movie_lists/list_newest_{page}.json
/dyn/phpauto/movie_lists/list_oldest_{page}.json
/dyn/phpauto/movie_lists/list_weekly_{page}.json
/dyn/phpauto/movie_lists/list_monthly_{page}.json
/dyn/phpauto/movie_lists/list_yearly_{page}.json
```

These lists are browsing feeds, not keyword search. The first page includes `TotalRows`, `SplitSize`, and `Rows`; the observed page had 50 rows.

## Product JSON schema

The observed detail object and current list rows share the same product shape. Important fields are:

| JSON field | Observed type | Meaning or migration note |
|---|---|---|
| `MovieID` | string | Canonical SKU; must exactly equal the requested SKU |
| `Title` / `TitleEn` | string | Japanese and English titles |
| `Desc` / `DescEn` | string or null | Japanese and English descriptions; English may be absent |
| `Actor` | string | Legacy display text; less structured than the actress arrays |
| `ActressesJa` / `ActressesEn` | string arrays | Ordered localized performer names |
| `ActressesList` | object keyed by actress ID | Localized names plus optional sizes and age |
| `Release` | string | ISO date, for example `2021-01-01` |
| `Duration` | integer | Duration in seconds |
| `Series` / `SeriesEn` | string or null | Localized series names |
| `AvgRating` | decimal or null | Average rating; no rating count was observed |
| `UC` | string/number array | Ordered category identifiers |
| `UcNameList` | object keyed by category ID | `NameJa` and `NameEn` for each category |
| `UCNAME` / `UCNAMEEn` | string arrays | Parallel localized category names; useful only as fallback |
| `MovieThumb`, `ThumbHigh`, `ThumbMed`, `ThumbLow`, `ThumbUltra` | URL strings | Product images |
| `Status`, `NoListDisplay`, `CanStream` | booleans | Availability and visibility signals |
| `MemberFiles` | array | Member download records; do not persist or expose |
| `SampleFiles` | array | Public sample media records; not needed for metadata collection |
| `PPVPrice` | object | Regular, discount, and campaign price values |

The verified `010121_001` response had `MovieID=010121_001`, `Release=2021-01-01`, `Duration=3654`, `AvgRating=2.9`, populated Japanese and English titles, a Japanese description, `DescEn=null`, one localized performer, category data, and absolute image URLs. A current listing row showed that `DescEn` can also be a non-null string and `AvgRating` can be null, so both fields must remain optional.

`ActressesList` entries expose `NameJa` and `NameEn`. `UcNameList` entries expose `NameJa` and `NameEn`. Category order is best preserved by iterating `UC` and looking up each identifier in `UcNameList`; use `UCNAME` and `UCNAMEEn` only when the keyed object is incomplete.

## URL and not-found behavior

Current canonical product URL:

```text
https://www.1pondo.tv/movies/{sku}/
```

Current metadata URL:

```text
https://www.1pondo.tv/dyn/phpauto/movie_details/movie_id/{sku}.json
```

The product page itself returns only the SPA shell and cannot be parsed without executing JavaScript. Unknown metadata URLs return HTTP 404 with an HTML document whose title is `1pondo | 404 Page Not Found`. A parser must inspect status and content type before attempting JSON decoding.

The public sitemap lists canonical movie URLs and marks them with `changefreq=daily`. It is a useful inventory for future indexing, but downloading and scanning thousands of entries for every user query would be wasteful.

## Localization findings

- Japanese is the source/default metadata language.
- English title, performer, category, description, and series variants are present in the JSON model, but individual English values may be null or blank.
- `Release`, `Duration`, rating, and image identity are shared fields.
- The SKU resembles `MMDDYY_NNN`, but release date must come from `Release`; deriving a date from the SKU is unnecessary and can be wrong.
- The top-level Android result should prefer English values, with field-level fallback to Japanese when English is missing. Preserve a complete Japanese projection in `localized["ja"]`.

## Search and index observations

- Rust provides no keyword search.
- The website search UI depends on JavaScript and a WebSocket protocol.
- The HTML search route is useful to a human browser but not a verified native parsing endpoint.
- The official page sitemap contained 4,871 movie routes on 2026-10-30.
- The current Android DuckDuckGo fallback can support this adapter after adding the scope `1pondo.tv/movies` and strict trusted-host validation for `www.1pondo.tv`.

## Risks

- Rust validation and parsing disagree, and Rust URL validation trusts arbitrary hosts.
- Treating the product page as static HTML yields no metadata.
- A 404 body is HTML, so blind JSON deserialization produces a misleading parse error.
- English fields vary between null, blank, and populated values.
- Parallel arrays can drift; keyed actress/category objects are safer.
- `MemberFiles` can contain access-oriented URLs and must not be stored in results, fixtures, or logs.
- Cloudflare may introduce a challenge even though none was observed.
- Native site search is coupled to undocumented SPA/WebSocket internals.

