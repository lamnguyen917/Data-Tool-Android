---
id: amazon-book-crawl
title: Amazon — book product crawl
status: verified
updated: 2026-07-20
tags:
  - amazon
  - books
  - crawl
  - html
  - asin
  - isbn
source: ../sources/amazon-book-observations.md
---

# Amazon — book product crawl

How to fetch and extract metadata from an **Amazon.com book detail page**. Site behavior only — no application-specific mapping.

Sample product: [Flutter and Dart Cookbook](https://www.amazon.com/Flutter-Dart-Cookbook-Developing-Applications/dp/1098119517) (ISBN-10 / `/dp/` id `1098119517`).

Related: [raw observations](../sources/amazon-book-observations.md)

## Quick facts

| Key | Value |
|-----|-------|
| Host | `https://www.amazon.com` |
| Primary strategy | **HTML product page** (`GET /dp/{id}`) |
| Product id | ASIN — for books often **ISBN-10** (10 digits) in URL |
| Slug in URL | **Optional** — may be omitted |
| JSON-LD | Not observed on tested page (do not rely on `ld+json`) |
| Anti-bot | Browser UA + cookies; rate-limit; detect block pages |

## Do / Don't

| Do | Don't |
|----|-------|
| Normalize to `https://www.amazon.com/dp/{id}` for crawl | Assume slug-less and slug URLs redirect to each other |
| Accept full URL, `/dp/…`, `/gp/product/…`, or bare ISBN-10 / ASIN | Use ISBN-13 in `/dp/` as primary resolver (fragile / soft-block observed) |
| Parse `#productTitle`, `#bylineInfo`, `#detailBullets_feature_div` | Rely on `application/ld+json` |
| Strip Unicode bidi marks (`\u200f`, `\u200e`) and entities `&rlm;` / `&lrm;` from detail bullets | Hammer Amazon with back-to-back requests from one IP |
| Detect bot/block pages (small HTML, no `#productTitle`) | Treat HTTP 200 alone as success |

## URL rule (critical)

Amazon book links follow:

```text
https://www.amazon.com/{optional-slug}/dp/{ASIN}
```

The `{optional-slug}` segment (book title words) is **not required**. These are equivalent for crawl:

```text
https://www.amazon.com/Flutter-Dart-Cookbook-Developing-Applications/dp/1098119517
https://www.amazon.com/dp/1098119517
```

Also works:

```text
https://www.amazon.com/gp/product/1098119517
```

`rel=canonical` may reference the slug form; prefer storing **`https://www.amazon.com/dp/{id}`** as the normalized product URL unless you need the canonical slug URL.

## Input → seed URL

```text
INPUT
├─ full product URL (amazon.com … /dp/{id} or /gp/product/{id})
│     → extract id, normalize to https://www.amazon.com/dp/{id}
├─ path /dp/{id} or /gp/product/{id}
│     → prepend https://www.amazon.com
├─ host without scheme amazon.com/dp/…
└─ bare id
      ├─ /^\d{10}$/           → ISBN-10 (common for books) → /dp/{id}
      └─ /^B[A-Z0-9]{9}$/i    → Amazon ASIN → /dp/{id}
```

### Extract id from URL

| URL shape | Extract |
|-----------|---------|
| `…/dp/1098119517` | `/dp/([A-Z0-9]{10})` |
| `…/something-slug/dp/1098119517` | trailing `/dp/([A-Z0-9]{10})` |
| `…/gp/product/1098119517` | `/gp/product/([A-Z0-9]{10})` |

Strip query (`?`) and fragment (`#`) when normalizing (e.g. `?ref=…`).

## Crawl pipeline

```text
resolve_seed_url(input)
  → GET https://www.amazon.com/dp/{id}
  → follow redirects (usually stays on /dp/{id})
  → detect_block_page(html)
  → parse_book_html(html)
```

### HTTP client requirements

- Browser-like `User-Agent`
- `Accept-Language: en-US,en;q=0.9` (book metadata in English on amazon.com)
- Cookie jar enabled (session cookies help avoid early blocks)
- Reasonable delay between requests from same client
- Timeout ~45s; page size often **1–2 MB** HTML

### Block / captcha detection

Treat as **failure** (retry later / rotate session), not parseable product:

| Signal | Meaning |
|--------|---------|
| HTML ≪ 10 KB and no `#productTitle` | Soft block / automated-access page |
| Title `Robot Check` or captcha form | Hard block |
| Boilerplate mentioning `api-services-support@amazon.com` | Automated access warning |
| Missing `#productTitle` on otherwise small page | Not a product detail page |

## HTML sources (priority)

### Core fields

| Concept | Primary selector / source |
|---------|---------------------------|
| SKU / ASIN | URL `/dp/{id}`; cross-check `#detailBullets` **ISBN-10** |
| Title | `#productTitle` |
| Author(s) | `#bylineInfo` — links/text after “by”, drop “(Author)” suffix |
| Publisher | `#detailBullets_feature_div` li containing `Publisher` |
| Publication date | detail bullet `Publication date` |
| Language | detail bullet `Language` |
| Pages | detail bullet `Print length` |
| ISBN-10 | detail bullet `ISBN-10` |
| ISBN-13 | detail bullet `ISBN-13` |
| Categories | `#wayfinding-breadcrumbs_feature_div` anchor texts (skip “Books” if redundant) |
| Description | `#bookDescription_feature_div` (fallback `#productDescription`) — strip HTML |
| Cover image | `#landingImage` attribute `data-a-dynamic-image` (JSON → largest URL key) |
| Rating | `#acrPopover[title]` or text `(\d+(?:\.\d+)?) out of 5 stars` |
| Canonical URL | `link[rel=canonical][href]` |

### Detail bullets parsing

Structure: `#detailBullets_feature_div` → `ul` → `li` items like:

```text
Publisher ‏ : ‎ O'Reilly Media
ISBN-10 ‏ : ‎ 1098119517
```

Real HTML often uses named entities instead of raw Unicode:

```text
Publisher &rlm; : &lrm; Packt Publishing
```

- Labels and values may be wrapped with Unicode direction marks **U+200F / U+200E** or entities **`&rlm;` / `&lrm;`** — remove before split.
- Split on first `:` after label; value is remainder trimmed.
- Label and value may live in separate `<span>` siblings (bold label + value span).

### Embedded JSON notes

- `"parentAsin":"B0……"` may appear for edition families (paperback vs Kindle). **Use the URL `/dp/` token as primary id** unless you explicitly resolve edition variants.
- Do not assume `currentAsin` is present.

## Verified examples

### `1098119517` — Flutter and Dart Cookbook

| Field | Value |
|-------|-------|
| Title | Flutter and Dart Cookbook: Developing Full-Stack Applications for the Cloud |
| Author | Richard Rose |
| Publisher | O'Reilly Media |
| Publication date | January 24, 2023 |
| Print length | 307 pages |
| Language | English |
| ISBN-10 | 1098119517 |
| ISBN-13 | 978-1098119515 |
| Categories | Books › Computers & Technology › Programming › … |
| Rating | 4.1 out of 5 stars |

Seed URLs tested (all served product HTML):

```text
GET /dp/1098119517                                          → 200
GET /Flutter-Dart-Cookbook-Developing-Applications/dp/1098119517 → 200
GET /gp/product/1098119517                                  → 200
```

### `1800565992` — Flutter for Beginners

| Field | Value |
|-------|-------|
| Title | Flutter for Beginners: An introductory guide to building cross-platform mobile applications with Flutter 2.5 and Dart |
| Authors | Thomas Bailey, Alessandro Biessek |
| Publisher | Packt Publishing |
| Publication date | October 18, 2021 |
| Print length | 353 pages |
| Language | English |
| ISBN-10 | 1800565992 |
| ISBN-13 | 978-1800565999 |

```text
GET /Flutter-Beginners-introductory-cross-platform-applications/dp/1800565992 → 200
GET /dp/1800565992 → 200
```

## Smoke checks

| Check | Expect |
|-------|--------|
| GET `/dp/1098119517` | `#productTitle` present; title mentions Flutter |
| GET slug URL | Same title; id still `1098119517` |
| Normalize slug URL → `/dp/1098119517` | Still 200 + parseable |
| ASIN from slug URL | Returns `1098119517` / `1800565992` |
| Block page | Small HTML, no `#productTitle` → fail loudly |

## Change checklist

When Amazon book crawl behavior changes:

1. Append evidence under [`sources/amazon-book-observations.md`](../sources/amazon-book-observations.md).
2. Promote into this page and refresh [`index/`](../index/).
3. Re-test `/dp/{isbn10}` and one slug URL on amazon.com.
