---
id: rust-desktop-adapters
title: Rust desktop adapters — implementation reference
status: source-reviewed
updated: 2026-10-28
tags:
  - adapters
  - rust
  - html
  - search
  - localization
source: ../sources/rust-desktop-adapter-observations.md
---

# Rust desktop adapters — implementation reference

This page summarizes nine adapters registered by the sibling Rust desktop application. It is a migration reference for Android work, not evidence that these adapters are implemented in `DataToolAndroid` or that their target websites currently match the selectors.

Related: [source-review observations](../sources/rust-desktop-adapter-observations.md)

## Status matrix

| Adapter | Key | Accepted input | Candidate search | Languages | Source-reviewed status |
|---------|-----|----------------|------------------|-----------|------------------------|
| Ave | `ave` | SKU; `parse` also accepts URL | Yes | English + Japanese | Parser implemented; integration test for `RHJ-100` |
| AdultEmpire | `adult-empire` | Product URL | No | English | Parser implemented; age-confirmation flow required |
| AVFantasy | `avfantasy` | SKU, URL, or `!keyword` | Yes | English + partial Japanese | Parser implemented with localization caveat |
| Carib | `caribbeancom` | `MMDDYY-NNN` or movie URL | No | English + Japanese | Parser implemented |
| CaribPr | `caribbeancompr` | `MMDDYY_NNN` or URL | No | Japanese only | Partial; top-level English metadata is empty |
| Heyzo | `heyzo` | Four-digit SKU or movie URL | No | English + Japanese | Parser implemented |
| 1pondo | `1pondo` | `MMDDYY_NNN` or JSON URL | No | Declared Japanese | Stub; JSON is fetched but not mapped |
| ThirdWorldMedia | `third-world-media` | URL | No | English | Not implemented |
| Tokyo-Hot | `tokyo-hot` | `kNNNN`, `nNNNN`, URL, or keyword | Resolver only | English + Japanese | Parser implemented; integration test for `n0001` |

`Candidate search` means an adapter overrides the list-returning `search_items` operation. Tokyo-Hot can internally resolve a query to one product but does not expose a candidate list.

## Shared result mapping

The Rust `ItemResult` and Android `ProductResult` use different names. A port should use this explicit mapping:

| Rust field | Android field | Notes |
|------------|---------------|-------|
| `sku` | `productId` | Normalize before queue/cache lookup |
| `title` | `title` | Direct mapping |
| `description` | `description` | Direct mapping |
| `starring[]` | `creators[]` | Preserve source order and remove empty values |
| `category[]` | `categories[]` | Deduplicate while preserving order |
| `url` | `canonicalUrl` | Restrict accepted hosts before fetch |
| `studio` | `attributes[studio]` | Android does not yet declare this attribute key |
| `date` | `attributes[release_date]` | Keep the source format unless a date contract is added |
| `details` | `attributes` | Convert keys to stable English identifiers |
| `jp` | No current equivalent | Decide between localized fields, a locale-indexed model, or a separate result |

Do not copy Japanese detail keys such as `シリーズ` into Android attribute identifiers. Store stable English keys and keep localized display labels in the presentation layer.

## Resolution and parsing rules

### Ave

Dedicated research: [Ave adapter — source research and Android migration guide](./ave-adapter-research.md).

```text
SKU
  -> remove optional dvd1 prefix
  -> search English catalog
  -> exact normalized SKU match
  -> parse English product
  -> derive and parse Japanese product URL

URL
  -> parse directly
  -> derive Japanese URL when possible
```

Primary selectors and sources:

| Data | Selector / rule |
|------|-----------------|
| Search cards | `.single-slider-product` |
| Product link | `.single-slider-product__image a` |
| Search SKU | image filename, without `dvd1` |
| Search title | image `alt`, fallback `.product-title` |
| Title | `.section-title h3` |
| Description | `.product-description` |
| Structured blocks | `.additional-info` and `.single-info` |
| Japanese URL | `lang=2`, `culture=ja-JP`, or `/2/` |

The Rust validator does not accept URLs even though `parse` does. Align the two behaviors in an Android port.

### AdultEmpire

```text
product URL
  -> POST age confirmation with shared cookies
  -> GET product URL
  -> parse product metadata
```

| Data | Selector / rule |
|------|-----------------|
| Title | `h1` |
| Date, SKU, studio, UPC | `ul.list-unstyled.m-b-2 li` label text |
| Cast | `a.PerformerName`, preferably below the `Cast` heading |
| Categories | links in the `ul` after the `Categories` heading |

Require an Adult DVD Empire host when porting. The Rust adapter currently accepts any HTTP(S) URL before sending the age-confirmation request to Adult DVD Empire.

### AVFantasy

```text
URL        -> parse directly
!keyword   -> title search -> first matching title rule
SKU        -> search cards -> image-derived exact SKU match
```

Product metadata is organized by `span.title` labels. English labels include `Item#`, `Starring`, `Studio`, `Date`, `Category`, `Series`, and `Play Time`. Japanese labels include `主演女優`, `スタジオ`, `カテゴリ`, and `シリーズ`.

The current Japanese category branch appends values to the top-level category list. Treat this as observed behavior, not a desired Android mapping.

### Carib

```text
MMDDYY-NNN or matching movie URL
  -> load English page
  -> load Japanese page
  -> derive YYYY-MM-DD from MMDDYY
```

| Data | Selector / rule |
|------|-----------------|
| Title | `h1[itemprop='name']` |
| Description | `p[itemprop='description']` |
| English performers | element after `span.spec-title` equal to `Starring:` |
| Japanese performers | sibling content after a span containing `出演` |
| Duration | content after `span.spec-title` equal to `Duration:` |

The year rule maps `80..99` to 1980..1999 and `00..79` to 2000..2079.

### CaribPr

Only the Japanese page is parsed:

| Data | Selector / rule |
|------|-----------------|
| Title | `div.heading h1` |
| Description | first `div.movie-info div.section.is-wide p` |
| Categories | links in an `li` whose text contains `タグ` |
| Performers | table cell after a cell containing `出演` |

Do not mark this adapter bilingual until an English endpoint, parser, and field mapping exist.

### Heyzo

```text
four-digit SKU or /moviepages/{sku}/ URL
  -> load Japanese page
  -> load English page
  -> merge category text into top-level result
```

| Data | English | Japanese |
|------|---------|----------|
| Title | `h1` | `h1` |
| Performers | table cell after `Actress(es)` | `tr.table-actor a` |
| Date | Uses Japanese result | table cell after `公開日` |
| Categories | rows labeled `Type` and `Sex Styles` | `tr.table-actor-type a`, `ul.tag-keyword-list li a` |

### 1pondo

The adapter only resolves the SKU and downloads:

```text
https://www.1pondo.tv/dyn/phpauto/movie_details/movie_id/{sku}.json
```

No JSON fields are deserialized or mapped. A port must first define the JSON schema, add fixtures, implement block/not-found handling, and map both shared and localized fields.

### ThirdWorldMedia

There are no endpoint or selector rules in the current source. The adapter stores the HTTP loader but never uses it, and `parse` always returns an implementation error.

### Tokyo-Hot

```text
kNNNN / nNNNN -> search -> product URL -> EN + JP pages
URL           -> EN + JP pages
keyword       -> search -> product URL -> EN page only
```

| Data | Selector / label |
|------|------------------|
| Search items | `li.detail` |
| Search match | query contained in `div.actor` text |
| English title | `div.contents h2` |
| Japanese title | `h1`, fallback `div.contents h2` |
| Description | `div.sentence` |
| Information rows | `div.infowrapper dl.info dt` plus the next element sibling |
| Performers | `Model` or `出演者` |
| Categories | `Play` or `プレイ内容` |
| Studio | `Label` or `レーベル` |
| Date | `Release`, `Release Date`, or `配信開始日` |
| Details | Series, duration, tags, and theme labels |

The SKU regular expression is lowercase-only. Normalize user input or deliberately preserve that restriction in a port.

## Porting priorities

1. Keep incomplete adapters disabled in the Android source selector until they return meaningful metadata.
2. Add a `DataSource` entry, stable English labels, and attribute keys before registering an adapter.
3. Make input validation and URL resolution use the same host/path rules.
4. Use local HTML or JSON fixtures for parsing tests; keep live tests optional.
5. Define the localized-result model before porting bilingual adapters.
6. Add age-gate, charset, block-page, timeout, and cancellation tests where relevant.
7. Record new live-site evidence under `.knowledge/sources` before updating selectors in this curated page.
