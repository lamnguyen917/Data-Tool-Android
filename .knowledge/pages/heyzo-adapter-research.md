---
id: heyzo-adapter-research
title: Heyzo adapter — source research and Android migration guide
status: implemented
updated: 2026-10-29
tags:
  - heyzo
  - adapters
  - rust
  - html
  - localization
  - cloudflare
source: ../sources/heyzo-adapter-observations.md
---

# Heyzo adapter — source research and Android migration guide

This document records the Android Heyzo implementation derived from the sibling Rust adapter. The Rust source was reviewed in full. Live product-page access was blocked by Cloudflare on 2026-10-29, so current selectors remain source-reviewed and fixture-tested rather than live-verified.

Related: [raw Heyzo observations](../sources/heyzo-adapter-observations.md) and [all Rust desktop adapters](./rust-desktop-adapters.md).

## Executive summary

| Item | Finding |
|---|---|
| Rust adapter key | `heyzo` |
| Canonical identity | Four-digit SKU, preserving leading zeroes |
| Direct product URLs | Resolved to the four-digit SKU |
| Product endpoints | Fixed English and Japanese HTML URLs |
| Keyword search | Unsupported |
| Languages | English top-level plus Japanese localization |
| Rust parser evidence | Source-reviewed; no fixtures or Heyzo tests |
| Live status | Both language product routes returned Cloudflare HTTP 403 on 2026-10-29 |
| Android status | Implemented, registered, and fixture-tested; live access remains blocked |

The port is conceptually small because product URLs are deterministic and no search resolution is needed. The hard part is operational reliability: current access is blocked, the Rust selectors have no fixture coverage, and the Rust result mixes English and Japanese category text.

## Android implementation

The registered Android port includes:

- `DataSource.HEYZO` with strict four-digit SKU and trusted English/Japanese product-URL validation;
- deterministic English and Japanese URL construction that never fetches a user-controlled host;
- `HeyzoAdapter`, with English as the required top-level response and Japanese as best-effort localization;
- `HeyzoParser`, with stable-order deduplication, English/Japanese category separation, generic not-found detection, and Cloudflare/challenge-page classification;
- English UI labels, copy/export display name, performer labeling, source selection, and application registration;
- local English, Japanese, and Cloudflare fixtures;
- resolver, parser, adapter, HTTP error, localization fallback, and unsupported-search tests.

The fixtures encode the source-reviewed parser contract. They do not prove that current live Heyzo markup still matches because Cloudflare prevented product HTML verification.

## Recommended Android input contract

Accept exactly one of these forms:

```text
SKU          0123
Japanese URL https://www.heyzo.com/moviepages/0123/index.html
English URL  https://en.heyzo.com/moviepages/0123/index.html
```

Rules:

1. Trim the input.
2. Accept a raw SKU only when it matches `^[0-9]{4}$`.
3. For a URL, require HTTPS.
4. Require the exact host `www.heyzo.com` or `en.heyzo.com`.
5. Require the path `/moviepages/{fourDigits}/index.html`, allowing only a harmless trailing slash if the live site proves it valid.
6. Reject user information, non-default ports, unexpected path segments, and look-alike hosts.
7. Extract and store only the four-digit SKU.
8. Rebuild both canonical language URLs from the SKU; never fetch the supplied URL verbatim.

Do not convert the SKU to an integer because leading zeroes are part of the product identity.

### Why the Rust validation should not be copied

The Rust validator accepts any syntactically plausible HTTP(S) URL, while its parser only extracts `/moviepages/{fourDigits}/`. Android validation and adapter resolution should share the same strict resolver so the Collect screen cannot accept an input that fetch later rejects.

## Endpoint and fetch design

Canonical endpoints:

```text
English  https://en.heyzo.com/moviepages/{sku}/index.html
Japanese https://www.heyzo.com/moviepages/{sku}/index.html
```

Recommended flow:

```text
input
  -> resolve strict four-digit SKU
  -> construct English and Japanese URLs
  -> fetch and parse English page
  -> fetch Japanese page as optional localization
  -> return one ProductResult with localized["ja"] when available
```

Fetch English first because it supplies the top-level Android result. After a valid English result exists, a Japanese network, block, or parse failure should not discard it. Coroutine cancellation must still be rethrown.

The current live 403 response means the first implementation may correctly return `BLOCKED` until access conditions change or the user completes a supported browser verification flow. Do not add challenge automation, CAPTCHA solving, or Cloudflare bypass behavior.

## Source-reviewed parsing contract

### English product page

| Android destination | Source-reviewed selector or rule |
|---|---|
| `title` | first `h1` |
| `creators` | links in the cell after a table cell containing `Actress(es)` |
| `categories` | links in rows whose first-cell label equals `Type` or `Sex Styles` |
| `attributes[studio]` | constant `Heyzo` |
| `canonicalUrl` | canonical English product URL |

For links, prefer normalized visible text. Use an anchor `title` attribute only as a fallback and only after a fixture proves it contains the desired human-readable value.

### Japanese product page

| Android destination | Source-reviewed selector or rule |
|---|---|
| `localized["ja"].title` | first `h1` |
| `localized["ja"].creators` | `tr.table-actor a, tr[class*='table-actor'] a` |
| `localized["ja"].categories` | `tr.table-actor-type a, tr[class*='table-actor-type'] a`, plus `ul.tag-keyword-list li a` |
| `localized["ja"].canonicalUrl` | canonical Japanese product URL |
| `attributes[release_date]` | value cell following the label `公開日` |
| `attributes[studio]` | constant `Heyzo` |

Treat release date as shared product metadata and expose it under the existing stable English key `ProductAttributes.RELEASE_DATE`. Preserve the source value until the observed format is verified; normalize to ISO `YYYY-MM-DD` only for known formats.

### Required parser validation

A successful HTTP response is not enough. The parser should require at least:

- a non-empty product title;
- a resolved four-digit SKU from the request;
- no block-page signals.

Once accessible fixtures exist, add a page-specific product marker so a generic homepage or age gate cannot be accepted merely because it contains an `h1`.

## Language and category policy

The Rust adapter appends English Type, English Sex Styles, Japanese actor types, and Japanese tags into one top-level category list. Android should preserve language boundaries:

```text
ProductResult.categories
  <- English Type + English Sex Styles

ProductResult.localized["ja"].categories
  <- Japanese actor type + Japanese keyword tags
```

Within each language:

- trim and normalize whitespace;
- remove blank values;
- deduplicate while preserving document order;
- do not translate source values in the parser.

The same normalization and stable-order deduplication should be applied to performers.

## Android field mapping

| Rust Heyzo value | Android destination | Notes |
|---|---|---|
| adapter key | `DataSource.HEYZO("heyzo")` | New source entry required |
| `sku` | `productId` | Keep as a four-character string |
| English title | `title` | Required |
| English performers | `creators` | Stable-order distinct values |
| English Type and Sex Styles | `categories` | Do not mix Japanese text |
| fixed `Heyzo` studio | `attributes[studio]` | Existing key |
| Japanese release date | `attributes[release_date]` | Existing key; preserve raw value initially |
| English URL | `canonicalUrl` | Must match the top-level language |
| Japanese title and performers | `localized["ja"]` | Existing localized model |
| Japanese actor types and tags | `localized["ja"].categories` | Unlike the Rust top-level merge |
| Japanese URL | `localized["ja"].canonicalUrl` | Canonical Japanese URL |

The current source provides no trustworthy mapping for description, image, duration, series, price, rating, or a page-displayed SKU. Leave these fields empty until fixtures or live evidence establish selectors.

## Search policy

Keyword search should initially return `FetchErrorKind.UNSUPPORTED`.

Reasons:

- the Rust adapter does not implement search;
- a deterministic product URL already exists for a known SKU;
- no current live search endpoint or result-card selector has been verified;
- current Cloudflare behavior makes anonymous search research unreliable.

Search can be added later only after its endpoint, query encoding, pagination, exact SKU mapping, block behavior, and local fixtures are documented.

## Error policy

| Condition | Android result |
|---|---|
| Invalid SKU or untrusted URL | Input validation failure before queueing |
| Network failure | `NETWORK` |
| Request timeout | `TIMEOUT` |
| HTTP 401 or 403 | `BLOCKED` |
| HTTP 429 | `RATE_LIMITED` |
| HTTP 404 | `NOT_FOUND` |
| HTTP 200 Cloudflare, CAPTCHA, access-denied, or human-verification page | `BLOCKED` |
| Verified Heyzo not-found marker | `NOT_FOUND` |
| Unknown HTTP 200 page without required product markers | `PARSE` |
| Japanese failure after English success | Keep English result and omit `localized["ja"]` |

The current `HttpClient` maps HTTP 403 to `BLOCKED`, and the Heyzo parser detects soft blocks in HTTP 200 responses containing Cloudflare or challenge text.

Do not retry `BLOCKED` responses as ordinary transient failures. The processing queue may retry once after the user completes the browser verification flow described below.

## Browser verification decision

When a Heyzo product request is blocked, the adapter attaches the trusted product URL and its desktop user agent to the error. The processing queue then opens the in-app browser and waits for the user to complete the challenge. The app does not interact with, automate, or solve the challenge.

Only one processing-verification WebView may be active at a time. If multiple concurrent items become blocked, they wait for the same verification result. After the browser reaches the exact trusted product path and the page no longer looks like a challenge, its cookies are imported into the shared HTTP client and every waiting item retries. A per-source verification generation prevents a late stale blocked response from opening a second WebView immediately after successful verification.

Each item may wait for browser verification only once per collection attempt. Closing the WebView releases all waiting items as `BLOCKED`; it does not leave queue jobs suspended. Login credentials, challenge automation, and paid-session bypasses are outside this flow.

## Cache design

The Rust file keys are collision-free for the current four-digit identity and language pair:

```text
heyzo_{sku}_en.html
heyzo_{sku}_jp.html
```

Android currently has a parsed-product memory cache rather than a disk response cache. Use the existing product key `heyzo:{sku}` for completed normalized results. If raw response caching is added later, include source, SKU, language, and a parser/schema version:

```text
heyzo:{schemaVersion}:{sku}:en
heyzo:{schemaVersion}:{sku}:ja
```

Never cache a Cloudflare or age-gate response as a successful product page.

## Fixture and test plan

Do not implement the parser without sanitized local fixtures. Acquire fixtures only through normal authorized access and remove session identifiers, account data, analytics payloads, and explicit media assets that are unnecessary for parser tests.

### Resolver tests

- Accept `0001` and preserve all four digits.
- Trim surrounding whitespace from a raw SKU.
- Accept canonical HTTPS English and Japanese URLs.
- Extract the same SKU from both language hosts.
- Reject HTTP, foreign hosts, suffix look-alike hosts, non-default ports, and unexpected paths.
- Reject three-digit, five-digit, signed, decimal, and non-ASCII-digit inputs.

### English fixture tests

- Parse the required title.
- Parse multiple performers.
- Parse Type and Sex Styles independently.
- Prefer visible link text and use a verified `title` fallback when needed.
- Normalize whitespace and deduplicate while preserving order.
- Reject an HTML page without product markers.

### Japanese fixture tests

- Parse the Japanese title and performers.
- Parse actor-type and keyword categories.
- Parse and preserve the release date.
- Keep Japanese categories out of top-level English categories.
- Normalize whitespace and deduplicate while preserving order.

### Error fixtures

- Cloudflare 403 is mapped by the HTTP layer to `BLOCKED`.
- HTTP 200 Cloudflare or challenge HTML returns `BLOCKED`.
- A verified soft not-found page returns `NOT_FOUND`.
- An age gate or generic homepage is not accepted as a product.
- Japanese failure after English success keeps the English result.
- English failure prevents a completed result.

### Adapter integration tests

- Request the canonical English and Japanese URLs for a SKU.
- Never request a supplied foreign or user-controlled URL.
- Return `UNSUPPORTED` for keyword search.
- Store Japanese data under language tag `ja`.
- Keep cancellation behavior intact.

Any live smoke test must be ignored by default, rate-limited, and expected to report `BLOCKED` while the current Cloudflare behavior remains.

## Implemented sequence

1. Added `DataSource.HEYZO`, English UI resources, source labels, and strict validation.
2. Added deterministic URL construction and explicit unsupported search behavior.
3. Added local English, Japanese, and Cloudflare fixtures representing the reviewed source contract.
4. Implemented required product markers, generic not-found handling, and soft-block detection.
5. Mapped English fields to the top level and Japanese fields to `localized["ja"]`.
6. Made Japanese fetch and parsing non-fatal after English success.
7. Registered the adapter in `AppContainer` and added resolver, parser, adapter, HTTP error, and display integration coverage.
8. Deferred a live smoke test until an accessible stable SKU can be verified without bypassing Cloudflare.

## Acceptance criteria

The Android Heyzo adapter is expected to continue meeting these criteria:

- raw SKU and URL validation share one strict resolver;
- only approved HTTPS Heyzo product hosts and paths are accepted;
- leading zeroes are preserved;
- the adapter never fetches a user-controlled host;
- English data remains separate from Japanese localization;
- HTTP and soft block pages are classified as `BLOCKED`;
- missing product markers cannot produce a false completed result;
- Japanese failure does not discard valid English metadata;
- parser behavior is covered by sanitized local fixtures;
- search remains unsupported until separately researched;
- comments, UI labels, attribute keys, and error text are in English.
