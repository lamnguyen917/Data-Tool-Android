---
id: ave-adapter-research
title: Ave adapter — source research and Android migration guide
status: implemented
updated: 2026-10-28
tags:
  - ave
  - adapters
  - rust
  - html
  - search
  - localization
source: ../sources/ave-adapter-observations.md
---

# Ave adapter — source research and Android migration guide

This document describes the Ave adapter implemented in the sibling Rust desktop project and turns its behavior into an explicit Android migration plan. The findings come from source review, not a fresh live crawl of AV Entertainments.

Related: [raw Ave observations](../sources/ave-adapter-observations.md) and [all Rust desktop adapters](./rust-desktop-adapters.md).

## Executive summary

| Item | Finding |
|------|---------|
| Rust adapter key | `ave` |
| Target | AV Entertainments DVD catalog |
| Primary lookup | English keyword search followed by exact normalized SKU match |
| Rust direct product URL | Supported by `parse`, rejected by `validate` |
| Candidate search | Implemented |
| Languages | English and Japanese |
| Main page format | HTML |
| Android status | Implemented, tested, and registered |
| Evidence quality | Source-reviewed; `RHJ-101` live-verified on 2026-10-28 |

The Rust parser is one of the more complete references: it supports candidate search, exact SKU resolution, bilingual product parsing, multiple page layouts, and several SKU fallbacks. The Android implementation addresses the main migration risks with strict HTTPS host validation, URL-aware cache aliases, offline parser fixtures, stable English attribute keys, and locale-indexed metadata.

### Current live-search limitation

Live verification with `RHJ-101` on 2026-10-28 found that AV Entertainments redirects `/dvd/search` to `/cart/verifybot`, which requires Google reCAPTCHA. Browser-like headers, an `AVECustID` session cookie, and a same-origin referrer did not remove the challenge. The site also disallows `/dvd/search` in `robots.txt`, so the adapter reports `BLOCKED` and does not attempt a CAPTCHA bypass.

Direct product collection remains functional. The verified `RHJ-101` URL uses `pro=30719`; an Android live smoke run returned SKU `RHJ-101` and the expected `Red Hot Jam Vol.101` title.

## Android implementation

The Android port is registered as `DataSource.AVE` and includes:

- strict SKU and trusted AV Entertainments product-URL validation;
- candidate search with form URL encoding, image-derived SKU normalization, link deduplication, and exact SKU resolution;
- English product parsing for grouped and flat metadata layouts;
- Japanese page resolution and locale-indexed metadata under language tag `ja`;
- stable `studio`, `release_date`, and `series` attributes;
- block-page detection and explicit not-found/parse errors;
- request-key cache aliases for direct URL input;
- MockWebServer integration tests and local EN/JP/search fixtures.

## Source behavior

### Input forms

The Rust parser can process:

```text
catalog SKU        RHJ-100
prefixed SKU       DVD1RHJ-100
direct product URL https://www.aventertainments.com/dvd/detail?...&pro=30718
```

Normalization removes a case-insensitive leading `dvd1` and converts ASCII characters to uppercase for comparison and final SKU output.

The validator uses:

```regex
^\w{2,}\-{0,1}\w{0,1}\d{2,5}$
```

This rule is useful as implementation evidence but should not be copied unchanged. It is difficult to explain to users, includes Unicode `\w`, and disagrees with the direct-URL branch in `parse()`.

### Recommended Android input contract

Use an explicit resolver rather than a single permissive regex:

```text
INPUT
├─ AV Entertainments product URL
│    ├─ require HTTPS
│    ├─ require aventertainments.com host
│    └─ extract stable product URL and optional pro id
└─ SKU
     ├─ trim
     ├─ remove optional case-insensitive dvd1 prefix
     ├─ require a documented ASCII SKU pattern
     └─ uppercase with Locale.ROOT
```

Do not accept an arbitrary URL merely because it has a dot or resembles a product path.

## Search and product resolution

English search endpoint:

```text
GET https://www.aventertainments.com/dvd/search
    ?lang=1
    &cat=29
    &culture=en-US
    &keyword={form-url-encoded query}
```

### Candidate extraction

| Concept | Selector / rule |
|---------|-----------------|
| Result card | `.single-slider-product` |
| Product anchor | `.single-slider-product__image a` |
| Product image | anchor descendant `img` |
| Image source | `src`, fallback `data-src` |
| Candidate SKU | image filename without leading `dvd1` |
| SKU fallback | `pro` query parameter from product URL |
| Candidate title | image `alt`, fallback `.product-title` |
| Duplicate identity | decoded product URL |

For `fetch(sku)`, the Rust implementation searches and selects the first candidate where:

```text
normalize(candidate.sku) == normalize(requested.sku)
```

This exact comparison prevents a partial title match from silently resolving to the wrong product. Preserve that property in Android.

### Search result caveat

When SKU extraction falls back to `pro={digits}`, the candidate SKU becomes `PRO{digits}`. That value is an internal product record identity and may not match the catalog item number shown on the detail page. Android search results should keep these as separate concepts:

```text
SearchHit.productId  = displayed or image-derived SKU when available
SearchHit.url        = product URL
attributes[recordId] = pro query value when useful
```

## Product-page parsing

The same parser handles English and Japanese HTML.

### Core fields

| ProductResult concept | Primary source |
|-----------------------|----------------|
| Title | `.section-title h3` |
| Description | `.product-description` |
| Creators / performers | `Starring` or `主演女優` metadata links |
| Categories | `Category` or `カテゴリ` metadata links |
| Publisher / studio | `Studio` or `スタジオ` flat metadata |
| Product ID | Item Number / Item# / SKU / Japanese item-number labels |
| Publication date | `Date` or `発売日` |
| Series | `Series` or `シリーズ` |

### Grouped layout

Grouped metadata uses `.additional-info` blocks and an `h4` heading.

| Heading | Behavior |
|---------|----------|
| `Product Details` / `商品詳細` | Parse key/value details |
| `Starring` / `主演女優` | Collect all link text |
| `Category` / `カテゴリ` | Collect all link text |
| `Series` / `シリーズ` | Join link text into a detail value |
| `Features` / `DVD特典` | Ignore |
| Unknown or missing | Try detail extraction only while details are empty |

### Flat layout

Newer or alternate layouts use `.single-info`, with `.title` and `.value` descendants. The parser recognizes studio, performers, categories, series, item number, and date in this form. It only uses several flat-layout values when the grouped layout has not already populated the field.

### Date handling

The parser prefers the first date matching:

```regex
\d{1,2}/\d{1,2}/\d{4}
```

Otherwise it strips `(Discontinue)` and `(Available)` and retains the remaining text. This preserves data but does not produce a stable date format.

Recommended Android handling:

1. Preserve the original source value in an attribute for traceability.
2. Parse known `MM/DD/YYYY` values into an ISO `YYYY-MM-DD` field.
3. Represent availability status separately instead of deleting it.

## SKU fallback strategy

The current Rust fallback order is:

```text
explicit item-number label
  -> known key in details map
  -> first matching image filename in the document
  -> requested SKU for non-URL input
```

For Android, use the requested normalized SKU before scanning unrelated images. Image scanning should be limited to product-gallery selectors, not every `img[src]` in the document.

Recommended order:

```text
explicit product field
  -> requested normalized SKU
  -> product-image filename
  -> fail with PARSE when no trustworthy identity exists
```

## Japanese localization

Japanese product URLs are derived by replacing English language markers:

```text
lang=1        -> lang=2
culture=en-US -> culture=ja-JP
/1/           -> /2/
```

The Rust result stores Japanese metadata in `ItemResult.jp`. Android now stores translations in `ProductResult.localized`, keyed by BCP 47 language tag. The implementation uses `localized["ja"]` for Ave. These were the alternatives considered:

| Option | Benefit | Cost |
|--------|---------|------|
| Locale-indexed fields | One product identity with explicit translations | Requires a domain-model migration |
| Separate localized result | Minimal conceptual change from Rust | Duplicate queue/cache/result identity concerns |
| English-only first phase | Fastest safe implementation | Defers a documented capability |

The selected locale-indexed model keeps Japanese values separate from English lists and displays them in a dedicated result-detail section.

Japanese request failures should not create a result that looks successful but contains only SKU and URL. Record a localization warning or omit the Japanese variant while keeping the English result successful.

## Android mapping

| Rust Ave value | Android destination | Required model work |
|----------------|---------------------|---------------------|
| `sku` | `productId` | Existing field |
| `title` | `title` | Existing field |
| `description` | `description` | Existing field |
| `starring` | `creators` | Existing field |
| `category` | `categories` | Existing field |
| `url` | `canonicalUrl` | Existing field |
| `studio` | `attributes[studio]` | Add stable `STUDIO` key |
| `date` | `attributes[release_date]` | Add stable `RELEASE_DATE` key |
| `Series` / `シリーズ` | `attributes[series]` | Add stable `SERIES` key |
| Other `details` | normalized attributes | Define allowlist and English identifiers |
| `jp` | `localized["ja"]` | Implemented with `LocalizedProductMetadata` |

Source-language labels must never become Android attribute keys. Map both English and Japanese labels to stable English identifiers.

## Cache design

The Rust implementation uses:

```text
ave_{sanitized-query}_search.html
ave_{cache-sku}en.html
ave_{cache-sku}jp.html
```

Two collision cases matter:

- different search queries can become the same sanitized filename;
- every direct URL without `pro=` uses `url` as its cache identity.

Android should key responses by a cryptographic hash of the canonical request URL plus source and locale:

```text
ave:{locale}:{sha256(canonicalRequestUrl)}
```

Keep the normalized product ID as metadata, not as the only HTTP cache key.

## Error semantics

Map failures explicitly:

| Condition | Android error |
|-----------|---------------|
| Invalid SKU or foreign URL | Input validation failure before queueing |
| Search request timeout | `TIMEOUT` |
| Search/product HTTP failure | `HTTP`, `RATE_LIMITED`, or `NOT_FOUND` as applicable |
| No exact SKU candidate | `NOT_FOUND` |
| Product page without trustworthy title or SKU | `PARSE` |
| Block or challenge page | `BLOCKED` |
| Japanese page failure after English success | Localization warning; keep English result |

The Rust code does not contain Ave-specific block-page detection. An Android port should add it before treating HTTP 200 as a product page.

## Test plan

Do not rely on the current live `RHJ-100` test as the parser regression suite. Capture sanitized local fixtures for each supported layout.

### Resolver tests

- Accept a canonical Ave SKU.
- Remove uppercase and lowercase `dvd1` prefixes.
- Normalize ASCII case with `Locale.ROOT`.
- Accept a valid AV Entertainments HTTPS product URL.
- Reject foreign hosts and malformed URLs.
- Reject empty and ambiguous SKU input.

### Search fixture tests

- Parse multiple `.single-slider-product` cards.
- Prefer image `alt` and fall back to `.product-title`.
- Read `src` and `data-src`.
- Remove `dvd1` from image filenames.
- Fall back to `pro` without treating it as a confirmed display SKU.
- Decode HTML entities in links.
- Deduplicate repeated product URLs.
- Require an exact normalized SKU when resolving a fetch.

### Product fixture tests

- Parse the grouped English layout.
- Parse the flat English layout.
- Parse the Japanese layout.
- Extract explicit item number before any fallback.
- Restrict image SKU fallback to product images.
- Parse `MM/DD/YYYY` and preserve availability text separately.
- Map English and Japanese series labels to one stable attribute key.
- Return `PARSE` for a non-product or challenge page.

### Localization tests

- Derive Japanese URL from each supported English URL shape.
- Keep English success when Japanese fetch fails.
- Never present an empty Japanese object as a successfully parsed translation.

### Cache tests

- Distinct direct URLs never share cache entries.
- Unicode queries with similar sanitized forms never collide.
- English and Japanese responses use distinct keys.

## Implemented sequence

1. Define `DataSource.AVE`, English display labels, and strict input resolution.
2. Decide the localized-result model before implementing Japanese parsing.
3. Add stable `STUDIO`, `RELEASE_DATE`, and `SERIES` attribute keys.
4. Capture local search, English product, Japanese product, not-found, and block-page fixtures.
5. Implement candidate search and exact SKU resolution.
6. Implement English parsing with both grouped and flat layouts.
7. Add Japanese parsing using the selected localization model.
8. Use URL-hash cache identities and explicit error classification.
9. Keep an optional rate-limited live smoke test outside the default unit-test suite.

## Acceptance criteria

The registered Android Ave adapter is expected to continue meeting these criteria:

- validation and fetch accept the same documented input forms;
- only trusted AV Entertainments hosts are fetched;
- search resolves an exact normalized SKU;
- English parser fixtures cover both metadata layouts;
- Japanese behavior has an explicit domain mapping;
- direct URLs cannot collide in cache;
- block pages and missing products do not become completed blank results;
- parser tests run without network access;
- all user-facing labels and internal attribute identifiers are English.
