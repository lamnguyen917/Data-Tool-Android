---
id: 1pondo-adapter-research
title: 1pondo adapter — live JSON research and Android implementation
status: implemented
updated: 2026-10-30
tags:
  - 1pondo
  - adapters
  - rust
  - json
  - localization
  - search
source: ../sources/1pondo-adapter-observations.md
---

# 1pondo adapter — live JSON research and Android implementation

This page defines a safe Android implementation contract for 1pondo. It combines a full review of the sibling Rust adapter with live verification of the current website, public product JSON, list feed, not-found response, robots policy, sitemap, SPA routes, and JavaScript data flow on 2026-10-30.

Related: [raw 1pondo observations](../sources/1pondo-adapter-observations.md) and [all Rust desktop adapters](./rust-desktop-adapters.md).

## Executive summary

| Item | Finding |
|---|---|
| Adapter key | `1pondo` (Rust alias `ippondo`) |
| Product identity | `MMDDYY_NNN`, treated as an opaque string |
| Canonical product URL | `https://www.1pondo.tv/movies/{sku}/` |
| Metadata endpoint | Public UTF-8 JSON by exact SKU |
| Product HTML | JavaScript SPA shell; not suitable for static parsing |
| Keyword search | SPA WebSocket protocol; do not implement natively in the first version |
| Search fallback | Use the existing per-adapter DuckDuckGo fallback when enabled |
| Languages | English top level plus Japanese localization |
| Not found | HTTP 404 HTML from the `.json` route |
| Live access | HTTP 200 JSON without challenge on 2026-10-30 |
| Rust status | Stub: fetches JSON but maps no fields |
| Android status | Implemented, registered, fixture-tested, and live endpoint-verified |

The JSON endpoint is the authoritative and simplest source. Do not execute the SPA to collect a known product. The main implementation risks are strict URL validation, mixed optional localization, HTML returned by a `.json` URL on 404, and accidentally retaining member-file URLs.

## Android implementation

The registered Android implementation includes:

- `DataSource.ONE_PONDO("1pondo")` with one strict resolver for raw SKUs, canonical movie URLs, and metadata URLs;
- `OnePondoAdapter`, which requests only the deterministic JSON endpoint, returns the public movie URL, marks native search unsupported, and attaches the movie page to blocked requests for serialized browser verification;
- `OnePondoParser`, which validates product identity and active status, maps English fields with Japanese fallback, builds `localized["ja"]`, formats duration, validates release dates and image hosts, preserves category order, and ignores member/sample files;
- DuckDuckGo fallback scope `1pondo.tv/movies` with exact trusted-host filtering;
- English source, short, SKU, and validation labels, performer display, copy/export name, adapter registration, and generic per-adapter settings/clear-data integration;
- sanitized JSON fixtures, resolver tests, parser tests, MockWebServer adapter tests, search fallback tests, and an ignored live smoke test.

The complete debug unit suite passed after implementation. The live endpoint and unknown-SKU behavior were verified separately during the research pass; the committed live test stays ignored by default.

## Recommended Android input contract

Accept exactly these forms:

```text
SKU          010121_001
Product URL  https://www.1pondo.tv/movies/010121_001/
Metadata URL https://www.1pondo.tv/dyn/phpauto/movie_details/movie_id/010121_001.json
```

Rules:

1. Trim the input.
2. Accept a raw SKU only when it matches `^[0-9]{6}_[0-9]{3}$`.
3. For URLs, require HTTPS, host `www.1pondo.tv`, no user information, and the default port or port 443.
4. Match the complete path case-sensitively against `/movies/{sku}/?` or `/dyn/phpauto/movie_details/movie_id/{sku}.json`.
5. Reject query strings and fragments on both supported URL forms.
6. Extract only the SKU, then rebuild the fixed metadata and canonical URLs. Never fetch a user-supplied host or path.
7. Preserve leading zeroes and keep the SKU as a string.

Do not validate calendar semantics from the first six digits. The format resembles a date, but the authoritative release value is the JSON `Release` field.

`DataSource.validate` and `OnePondoAdapter.fetch` must use one shared resolver so validation cannot accept an input that collection later rejects. Prefer the class and display spelling `1pondo`; use an identifier such as `OnePondoAdapter` where Kotlin identifiers cannot begin with a digit.

## Endpoint and fetch flow

```text
canonical page  https://www.1pondo.tv/movies/{sku}/
metadata JSON   https://www.1pondo.tv/dyn/phpauto/movie_details/movie_id/{sku}.json
```

Recommended flow:

```text
input
  -> resolve strict SKU
  -> build the fixed metadata endpoint
  -> GET one JSON document
  -> classify status and content type
  -> decode and require MovieID == requested SKU
  -> map English plus localized Japanese metadata
  -> return the public /movies/{sku}/ URL as canonicalUrl
```

Use the app's normal desktop user agent, timeout, cookie jar, retry policy, and response cache. The verified endpoint was cacheable for ten minutes and exposed an ETag and Last-Modified header, but the Android completed-result cache is sufficient for the first version.

Do not fetch the public product page during normal collection. It returned HTTP 200 but contained no product-specific metadata until JavaScript runs.

## JSON decoding contract

Use a dedicated JSON decoding layer with explicit field access and ignore unknown fields. The implementation parses a `JsonObject` rather than deserializing directly into `ProductResult`, because localization and privacy filtering require deliberate mapping.

Minimum useful fields:

```text
MovieID
Title, TitleEn
Desc, DescEn
ActressesJa, ActressesEn, ActressesList
Release, Duration
Series, SeriesEn
AvgRating
UC, UCNAME, UCNAMEEn, UcNameList
MovieThumb, ThumbHigh, ThumbMed, ThumbLow, ThumbUltra
Status, NoListDisplay
```

Treat missing, null, blank, and type-incompatible optional values as absent. `MovieID` is required and must exactly equal the resolved SKU. A valid response whose `MovieID` differs from the request is a `PARSE` failure, not a redirect or alias.

Never map or persist `MemberFiles`, authentication data, or member download URLs. The parser does not read those properties. `SampleFiles`, gallery flags, streaming flags, and PPV price are outside the current `ProductResult` contract and are also ignored.

## Android field mapping

| JSON value | Android destination | Rule |
|---|---|---|
| `MovieID` | `productId` | Required exact SKU |
| `TitleEn`, then `Title` | `title` | Require a non-blank final value |
| `DescEn`, then `Desc` | `description` | Field-level fallback; omit when both are blank |
| `ActressesEn`, then English names in `ActressesList`, then `ActressesJa` | `creators` | Trim, remove blanks, preserve order, distinct |
| English names from `UC` + `UcNameList`, then `UCNAMEEn`, then Japanese names | `categories` | Preserve `UC` order and distinct values |
| public movie route | `canonicalUrl` | Always rebuild `https://www.1pondo.tv/movies/{sku}/` |
| `ThumbHigh`, then `MovieThumb`, then other thumb fields | `imageUrl` | Require an HTTPS URL on `www.1pondo.tv` |
| `Release` | `attributes[release_date]` | Accept only a real ISO `YYYY-MM-DD` date |
| `Duration` | `attributes[duration]` | Convert non-negative seconds to display-ready `HH:mm:ss` |
| `SeriesEn`, then `Series` | `attributes[series]` | Omit when blank |
| `AvgRating` | `attributes[rating]` | Preserve a finite non-negative numeric value; no rating count |
| constant `1pondo` | `attributes[studio]` | Optional source/studio display value |

`Duration=3654` should become `01:00:54`. Keep the conversion deterministic and test zero, values over one hour, and invalid negatives.

The site does not provide an observed rating count. Never infer one or copy another numeric field into `rating_count`.

### Image validation

Observed image fields are absolute URLs under paths such as `/moviepages/{sku}/images/`. Before returning one:

- require HTTPS;
- require exact host `www.1pondo.tv`;
- reject user information and non-default ports;
- prefer `ThumbHigh`, then `MovieThumb`, then medium/ultra/low only when the earlier values are absent.

Do not assume the misleading size names always correspond to larger images; use the verified fallback order and keep it easy to change.

## Localization policy

Return one English-oriented top-level result and a Japanese projection in `localized["ja"]`.

| Japanese destination | Source |
|---|---|
| `title` | `Title` |
| `description` | `Desc` |
| `creators` | `ActressesJa`, then `ActressesList[*].NameJa` |
| `categories` | iterate `UC` and read `UcNameList[id].NameJa`, then fall back to `UCNAME` |
| `canonicalUrl` | same public movie URL |
| `attributes[series]` | `Series` |
| shared attributes | validated release date and formatted duration when the UI expects them in localized details |

Top-level English fields should fall back individually to Japanese so a product remains usable when only some translations are missing. Still preserve Japanese metadata separately. For example, `DescEn` was null on the verified detail while the Japanese description was populated.

For actresses and categories, prefer the ordered arrays/identifier list first. Use the keyed objects to resolve localized names. Remove blanks and duplicates without sorting.

## Availability and required markers

A successful result requires all of the following:

- HTTP 200;
- a JSON media type, or a body that safely decodes as a JSON object when the media type is missing;
- non-blank `MovieID` equal to the requested SKU;
- a non-blank title after the English-to-Japanese fallback;
- `Status` not explicitly false.

If `NoListDisplay` is true but the exact JSON endpoint returns a valid active product, keep the exact-SKU result; the flag controls listing visibility rather than product identity. If future evidence shows hidden products should not be exposed, revise this policy with a fixture.

Unknown SKU behavior is unambiguous: the metadata route returned HTTP 404 and an HTML 404 page. Classify the status before JSON parsing so this becomes `NOT_FOUND`.

## Search policy

Return `FetchErrorKind.UNSUPPORTED` from native `search(query)` in the first implementation.

The official `/search/?s={query}` page is an SPA. Its full-text search uses the site's Bifrost/WebSocket protocol rather than a stable public REST route. Reimplementing that protocol would add coupling, session behavior, and maintenance risk for no benefit to exact-SKU collection.

Integrate with the existing optional search-engine fallback:

- add the search scope `1pondo.tv/movies` to `DuckDuckGoSearchEngine`;
- trust only `www.1pondo.tv` results;
- pass every result URL through the strict 1pondo resolver;
- deduplicate by SKU;
- use the link title only as candidate display text; authoritative metadata still comes from the JSON endpoint;
- invoke fallback only when the adapter setting is enabled and native search is unsupported or empty, following `AdapterSearchRepository` behavior.

The official sitemap is a possible future source for a local product index. On 2026-10-30 it contained 4,871 movie routes and was roughly 630 KB. If a native index is later added, download it on a bounded refresh schedule, honor cache headers, and join it with paged list JSON offline. Do not download or scan the sitemap for each keystroke.

## Block and browser-verification policy

No challenge was observed, but the host uses Cloudflare. Detect challenge/access-denied responses before JSON decoding:

- HTTP 401 or 403;
- HTTP 200 with HTML rather than JSON and recognizable Cloudflare or CAPTCHA markers;
- redirects to a verification page;
- a JSON endpoint response that is an HTML challenge rather than the known 404 page.

Map these conditions to `BLOCKED` and attach the canonical product page as `challengeUrl`. Reuse the existing serialized browser-verification flow: if several items encounter a challenge concurrently, only one WebView may be active and the other items wait for its result. Never automate CAPTCHA solving.

After successful browser verification, import cookies into the shared HTTP client and retry the JSON endpoint. A verified SPA page is not itself product metadata; do not parse its shell as the product.

## Error policy

| Condition | Android result |
|---|---|
| Invalid SKU or untrusted URL | Validation failure before queueing |
| DNS/connectivity failure | `NETWORK` |
| Request timeout | `TIMEOUT` |
| HTTP 404 | `NOT_FOUND` |
| HTTP 429 | `RATE_LIMITED` |
| HTTP 401/403 or challenge document | `BLOCKED` with browser challenge |
| Other non-success status | `HTTP` |
| HTTP 200 non-JSON without challenge markers | `PARSE` |
| Invalid JSON or missing/mismatched `MovieID` | `PARSE` |
| Explicit inactive `Status=false` | `NOT_FOUND` |
| Missing title after localization fallback | `PARSE` |
| Native keyword search | `UNSUPPORTED` |

Do not log the full response body. A future schema variation could include access-oriented URLs even when the current DTO ignores them.

## Cache design

Use the existing completed-result key:

```text
1pondo:{sku}
```

If raw JSON caching is added, include a schema version:

```text
1pondo:{schemaVersion}:{sku}:detail
```

Never cache HTML challenge pages, the HTML 404 body, or member-file data as product metadata. The per-adapter clear-local-data action must remove both completed 1pondo results and any 1pondo response-cache entries without affecting other sources.

## Fixture and test plan

Capture sanitized JSON fixtures for:

- `010121_001`, which has both titles, a Japanese description, `DescEn=null`, rating, duration, categories, and one performer;
- one current listing/detail item whose English description is populated and rating is null;
- a minimal valid product with optional arrays and objects absent;
- a product with duplicate names and category identifiers;
- the observed HTML 404 response;
- a sanitized Cloudflare/CAPTCHA HTML response for block detection.

Remove `MemberFiles`, `SampleFiles`, media URLs not required by the parser, and any access-sensitive values from committed fixtures.

### Resolver tests

- Accept `010121_001` and preserve the leading zero.
- Trim outer whitespace.
- Accept the canonical product and exact metadata URLs.
- Accept the product URL with and without its trailing slash if that is the documented contract.
- Reject `010121-001`, short segments, extra suffixes, HTTP, foreign/look-alike hosts, user information, non-default ports, wrong paths, and encoded path separators.
- Ensure validation and adapter fetch resolve the same SKU.

### Parser tests

- Prefer English title, description, performer, category, and series values when present.
- Fall back field-by-field to Japanese.
- Build `localized["ja"]` from Japanese values.
- Iterate `UC` and resolve category names through `UcNameList` in order.
- Fall back to `UCNAME`/`UCNAMEEn` when keyed names are absent.
- Format `3654` seconds as `01:00:54`.
- Parse a valid ISO release date and reject impossible dates.
- Prefer `ThumbHigh`, validate the host, and fall back safely.
- Preserve rating without inventing a rating count.
- Reject missing or mismatched `MovieID`, inactive status, and missing title.
- Confirm ignored member/sample fields cannot appear in the result.

### Adapter integration tests with MockWebServer

- Request only `/dyn/phpauto/movie_details/movie_id/{sku}.json`.
- Never request the URL supplied by the user.
- Return the public `/movies/{sku}/` route as `canonicalUrl`.
- Map HTTP 404 HTML to `NOT_FOUND` without attempting JSON parsing.
- Map HTTP 429, 403, challenge HTML, malformed JSON, and timeouts correctly.
- Attach a browser challenge for `BLOCKED` responses.
- Return `UNSUPPORTED` for native keyword search.

### Search fallback tests

- Build a scoped query for `site:1pondo.tv/movies`.
- Accept only exact `www.1pondo.tv/movies/{sku}/` results.
- Reject homepage, search, image, JSON, look-alike-host, and malformed SKU links unless the resolver explicitly supports the JSON URL.
- Deduplicate product and metadata URLs that identify the same SKU.

Keep any live smoke test ignored by default and rate-limited. One known SKU request is enough; do not enumerate the feed in automated tests.

## Implemented sequence

1. Added `DataSource.ONE_PONDO("1pondo")`, English labels, strict shared input resolution, search scope, and trusted-host rules.
2. Added a sanitized detail fixture plus inline not-found and block cases.
3. Added `OnePondoParser` with explicit localization, validation, image filtering, and privacy exclusions.
4. Added `OnePondoAdapter` with the deterministic JSON endpoint, canonical public URL, error classification, browser challenge, and unsupported native search.
5. Registered the adapter in `AppContainer`; existing generic UI paths provide per-adapter settings and clear-local-data behavior.
6. Added resolver, parser, adapter, search fallback, and live smoke coverage.
7. Ran the complete debug unit suite and kept the live smoke test ignored by default.
8. Recorded the implementation in `.agents/memory`.

## Acceptance criteria

- Raw SKU, public product URL, and metadata URL resolve through one strict trusted resolver.
- Collection makes one deterministic JSON request and never parses the SPA shell.
- `MovieID` must exactly match the requested SKU.
- English top-level metadata and Japanese localized metadata are separated without losing field-level fallbacks.
- Release date, duration, rating, series, categories, and trusted image URL are mapped deterministically.
- Member/sample file records are never stored or logged.
- HTTP 404 HTML maps to `NOT_FOUND`; challenge HTML maps to `BLOCKED` and uses the serialized WebView flow.
- Native search stays unsupported until the WebSocket protocol is deliberately researched.
- Optional DuckDuckGo fallback is scoped to trusted 1pondo movie URLs and controlled by the adapter setting.
- Fixtures cover null translations, category ordering, malformed data, 404, and block behavior.
- All UI text, labels, comments, and error messages are in English.

