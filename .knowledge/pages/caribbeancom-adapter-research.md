---
id: caribbeancom-adapter-research
title: Caribbeancom adapter — source research and Android migration guide
status: implemented
updated: 2026-10-29
tags:
  - carib
  - adapters
  - rust
  - html
  - localization
  - charset
source: ../sources/caribbeancom-adapter-observations.md
---

# Caribbeancom adapter — source research and Android migration guide

This page records the Android Caribbeancom implementation derived from the sibling Rust `caribbeancom` adapter. The Rust source was reviewed in full, and both language product pages for `091317-498` were live-verified on 2026-10-29, including an Android live smoke run.

Related: [raw Caribbeancom observations](../sources/caribbeancom-adapter-observations.md), [all Rust desktop adapters](./rust-desktop-adapters.md), and the structurally similar [Heyzo adapter](./heyzo-adapter-research.md).

## Executive summary

| Item | Finding |
|---|---|
| Rust adapter key | `caribbeancom` (alias `carib`) |
| Canonical identity | `MMDDYY-NNN`, for example `091317-498` |
| Direct product URLs | Resolved to the SKU; canonical URLs are rebuilt |
| Product endpoints | Fixed English and Japanese HTML URLs |
| Keyword search | Not implemented in Rust |
| Languages | English top level plus Japanese localization |
| Japanese charset | **EUC-JP** |
| Live status | Both product pages HTTP 200 without challenge on 2026-10-29 |
| robots.txt | No disallowed paths; three sitemaps published |
| Rust test evidence | None; no fixtures |
| Android status | Implemented, registered, fixture-tested, and live smoke-tested |

The port is small: identity is deterministic, no search is needed, and the site is currently reachable. The main risks are the English soft not-found page (HTTP 200), the EUC-JP Japanese page, and Rust field mapping gaps (no English description, ignored tags and series, Japanese URL at the top level).

## Android implementation

The registered Android port includes:

- `DataSource.CARIBBEANCOM("caribbeancom")` with a strict resolver (`caribbeancomProductId`) shared by validation and `CaribbeancomAdapter.fetch`;
- `CaribbeancomAdapter`, which builds both canonical URLs from the SKU, requires the English page, treats the Japanese page as best-effort localization, wraps English `BLOCKED` errors with `withBrowserChallenge`, and returns `UNSUPPORTED` for keyword search;
- `CaribbeancomParser`, with label-based spec rows, comma-split English performers, English tags, duration, Japanese description with line breaks, Japanese tags and series, English soft not-found detection, Japanese 404 detection, and challenge detection;
- `CaribbeancomParser.releaseDateFromSku`, which validates month, day, and leap years and omits impossible dates;
- English UI labels (`Caribbeancom`, short label `CARIB`, `SKU`), copy/export display name, performer labeling, DuckDuckGo fallback scope and trusted hosts, and `AppContainer` registration;
- fixtures in `app/src/test/resources/caribbeancom/`, with Japanese fixtures stored as raw EUC-JP bytes;
- resolver, parser, date, adapter (MockWebServer with an EUC-JP `Content-Type`), and an ignored live smoke test.

The live smoke test for `091317-498` passed on 2026-10-29 and confirmed that `HttpClient` decodes the EUC-JP Japanese page correctly. The poster image is not mapped yet because its availability without a session has not been verified.

## Recommended Android input contract

Accept exactly one of these forms:

```text
SKU          091317-498
Japanese URL https://www.caribbeancom.com/moviepages/091317-498/index.html
English URL  https://en.caribbeancom.com/eng/moviepages/091317-498/index.html
```

Rules:

1. Trim the input.
2. Accept a raw SKU only when it matches `^[0-9]{6}-[0-9]{3}$`.
3. For a URL, require HTTPS.
4. Require host `www.caribbeancom.com` with path `/moviepages/{sku}/index.html`, or host `en.caribbeancom.com` with path `/eng/moviepages/{sku}/index.html`. A trailing `/` instead of `/index.html` may be accepted.
5. Reject user information, non-default ports, look-alike hosts, and unexpected path segments.
6. Store only the SKU and rebuild both canonical URLs; never fetch the supplied URL.
7. Keep the SKU as a string.

Do not accept the Caribbeancom PR form `MMDDYY_NNN`; it belongs to a different site (`caribbeancompr`) with different endpoints.

Like Heyzo, `DataSource.validate` and the adapter must share one resolver so the Collect screen cannot accept an input that fetch later rejects.

## Endpoint and fetch design

```text
English  https://en.caribbeancom.com/eng/moviepages/{sku}/index.html
Japanese https://www.caribbeancom.com/moviepages/{sku}/index.html
```

Recommended flow:

```text
input
  -> resolve strict SKU
  -> fetch and parse English page (required)
  -> fetch Japanese page (best effort)
  -> return one ProductResult with localized["ja"] when available
```

Differences from Rust:

- A Japanese failure must not discard a valid English result. Coroutine cancellation must still be rethrown.
- If the Japanese page succeeds but the English page is a soft not-found, return `NOT_FOUND`. Do not promote Japanese data to the top level.

### Charset

The Japanese page is served with `Content-Type: text/html; charset=EUC-JP`. The current `HttpClient` reads `ResponseBody.string()`, which honors the header charset, and Android supports `EUC-JP`. Add a fixture test that stores raw EUC-JP bytes and decodes them through the HTTP layer so a regression to forced UTF-8 is caught. Do not commit UTF-8 converted fixtures only.

## Parsing contract

### English page (live-verified)

| Android destination | Selector or rule |
|---|---|
| `title` | `h1[itemprop=name]`; required and non-blank |
| `creators` | `li.movie-detail__spec` whose `span.spec-title` is `Starring:` → each `[itemprop=actor] [itemprop=name]` text split on `,`, trimmed, distinct |
| `categories` | `Tags:` row → `.spec-content a` text, distinct, in order |
| `attributes[duration]` | `Duration:` row → `[itemprop=duration]` text (`00:19:54`) |
| `attributes[studio]` | constant `Caribbeancom` |
| `attributes[release_date]` | derived from SKU (see below) |
| `canonicalUrl` | canonical English URL |
| `description` | `p[itemprop=description]` if present; currently absent, so usually empty |

Match spec rows by their `span.spec-title` label, not by position. Labels end with `:` on the English page.

### Japanese page (live-verified)

| Android destination | Selector or rule |
|---|---|
| `localized["ja"].title` | `h1[itemprop=name]` |
| `localized["ja"].description` | `p[itemprop=description]`, preserving line breaks |
| `localized["ja"].creators` | `出演` row → `[itemprop=actor] [itemprop=name]`, distinct |
| `localized["ja"].categories` | `タグ` row → `.spec-content a` |
| `localized["ja"].attributes[series]` | `シリーズ` row → link text |
| `localized["ja"].canonicalUrl` | canonical Japanese URL |

Use one selector level for performers (`[itemprop=actor] [itemprop=name]`) to avoid the duplicate names the Rust selector can produce. Ignore `ユーザー評価` (star glyphs, not a numeric rating).

The Japanese description is the only description available for observed products. Keep it under `localized["ja"]`; do not copy it into the English top level.

### Release date

The page shows no date. Rust derives one from the SKU (`MMDDYY`, `YY >= 80` → 1900s). Android should:

1. Parse `MMDDYY` and validate the result as a real calendar date (`java.time` is not available below API 26 without desugaring; use a manual month/day check or `GregorianCalendar` with `isLenient = false`).
2. Store a valid date as ISO `YYYY-MM-DD` in `attributes[release_date]`.
3. Omit the attribute when the date is invalid instead of producing `2099-99-99`.

Document in UI copy or attribute naming that this is the catalog date encoded in the SKU, not a verified publication date.

### Image

The poster is built by script, not by a static `img` tag: `/moviepages/{sku}/images/l_l.jpg` on `www.caribbeancom.com`. It may be used for `imageUrl` once its availability without a session is verified. Do not scrape sample-video URLs from `var Movie`.

### Required product markers

A successful HTTP response is not enough. Require:

- a non-blank `h1[itemprop=name]`;
- at least one `li.movie-detail__spec` (English) or `li.movie-spec` (Japanese).

The English not-found page returns **HTTP 200** with an empty `h1[itemprop=name]` and title `  - Caribbeancom.com`. Classify it as `NOT_FOUND`, not `PARSE`, when the title is blank and the HTML `<title>` starts with ` - Caribbeancom.com` after trimming leading whitespace. The Japanese not-found page returns HTTP 404 and is already mapped to `NOT_FOUND` by `HttpClient`.

## Android field mapping

| Rust value | Android destination | Notes |
|---|---|---|
| adapter key | `DataSource.CARIBBEANCOM("caribbeancom")` | New source entry |
| `sku` | `productId` | `MMDDYY-NNN` string |
| English title | `title` | Required |
| English starring | `creators` | Comma split, distinct |
| — (ignored by Rust) | `categories` | English tags |
| `details["Duration"]` | `attributes[duration]` | Existing `ProductAttributes.DURATION` |
| `studio` | `attributes[studio]` | `Caribbeancom` |
| `date` | `attributes[release_date]` | SKU-derived, validated |
| `url` (Japanese in Rust) | `canonicalUrl` | Use the English URL to match top-level language |
| `jp.title`, `jp.description`, `jp.starring` | `localized["ja"]` | Existing localized model |
| — (ignored by Rust) | `localized["ja"].categories`, `localized["ja"].attributes[series]` | Japanese tags and series |
| `jp.studio` `カリビアンコム` | `localized["ja"].attributes[studio]` | Optional |

## Search policy

Return `FetchErrorKind.UNSUPPORTED` for keyword search in the first version:

- Rust does not implement search;
- the SKU fully determines both product URLs;
- no search endpoint, encoding (EUC-JP percent-encoding on the Japanese site), or result-card selector has been verified.

If search is added later, the published sitemaps (`/static-seo/sitemap-pages.xml`, `sitemap-videos.xml`) are an allowed starting point for an offline SKU index; research their size and update frequency first.

## Error policy

| Condition | Android result |
|---|---|
| Invalid SKU or untrusted URL | Validation failure before queueing |
| Network failure / timeout | `NETWORK` / `TIMEOUT` |
| HTTP 401 or 403 | `BLOCKED` |
| HTTP 429 | `RATE_LIMITED` |
| HTTP 404 (Japanese not-found) | `NOT_FOUND` |
| English HTTP 200 soft not-found | `NOT_FOUND` |
| Challenge, CAPTCHA, or access-denied HTML | `BLOCKED` |
| HTTP 200 without product markers | `PARSE` |
| Japanese failure after English success | Keep English result; omit `localized["ja"]` |

No challenge was observed. If one appears later, reuse the existing processing browser-verification flow (`ProcessingVerification`, `BrowserChallenge`) as Heyzo does. Never automate or solve a challenge.

## Cache design

Rust cache keys are collision-free for one SKU and language:

```text
carib_{sku}_en.html
carib_{sku}_jp.html
```

On Android, use the existing product key `caribbeancom:{sku}` for completed results. If raw response caching is added, include a schema version and language (`caribbeancom:{schemaVersion}:{sku}:en`). Never cache a soft not-found page as a product.

## Fixture and test plan

Capture sanitized fixtures for `091317-498` (English UTF-8, Japanese **raw EUC-JP bytes**), the English soft not-found page, and the Japanese 404 page. Remove analytics scripts, session data, and media URLs that tests do not need.

### Resolver tests

- Accept `091317-498` and preserve leading zeroes.
- Trim whitespace.
- Accept both canonical HTTPS URLs and extract the same SKU.
- Reject `091317_498`, `91317-498`, `091317-4980`, HTTP, foreign or look-alike hosts, ports, and wrong paths (for example `/moviepages/` on the English host without `/eng/`).

### English parser tests

- Parse title, duration, and tags.
- Split `Chie Aoi, Kurumi Chino` into two creators.
- Leave description empty when `p[itemprop=description]` is absent.
- Classify the soft not-found fixture as `NOT_FOUND`.
- Reject a page without product markers as `PARSE`.

### Japanese parser tests

- Decode the EUC-JP fixture correctly (`葵千恵`, `千野くるみ`).
- Parse title, multi-line description, distinct performers, tags, and series.
- Keep Japanese tags out of top-level categories.

### Date tests

- `091317-498` → `2017-09-13`.
- `010180-001` → `1980-01-01`; `123179-001` → `2079-12-31`.
- `999999-999` and `023017-001` → no `release_date`.

### Adapter integration tests (MockWebServer)

- Request only the two canonical paths for a SKU.
- Never request a host from the user input.
- English success plus Japanese failure keeps the English result.
- English soft not-found returns `NOT_FOUND` even when Japanese succeeds.
- Search returns `UNSUPPORTED`.

An optional live smoke test for `091317-498` must be ignored by default and rate-limited.

## Implementation sequence

1. Add `DataSource.CARIBBEANCOM`, English UI labels, source label, and the strict resolver shared by validation and the adapter.
2. Add fixtures, including raw EUC-JP bytes.
3. Implement `CaribbeancomParser` with required markers, soft not-found detection, and block detection.
4. Implement `CaribbeancomAdapter` with deterministic URLs, English-required and Japanese-optional fetching, and unsupported search.
5. Add the SKU-date helper with calendar validation.
6. Register the adapter in `AppContainer`.
7. Add resolver, parser, date, adapter, and display tests.
8. Update this page status to `implemented` and record the change in `.agents/memory`.

## Acceptance criteria

- Raw SKU and URL validation share one strict resolver.
- Only the two canonical HTTPS hosts and paths are fetched.
- The EUC-JP Japanese page decodes correctly.
- The English soft not-found page never becomes a completed result.
- English data stays separate from Japanese localization; `canonicalUrl` is the English URL.
- Invalid SKU dates are omitted, not emitted.
- Japanese failure does not discard valid English metadata.
- Parser behavior is covered by local fixtures.
- Search is `UNSUPPORTED` until separately researched.
- Comments, UI labels, attribute keys, and error text are in English.
