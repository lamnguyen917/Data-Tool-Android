# Raw — Amazon book observations

Unedited working notes. Curated form: [Amazon book crawl](../pages/amazon-book-crawl.md).

Sample: Flutter and Dart Cookbook — ASIN / ISBN-10 `1098119517`

## URL shapes (verified 2026-07-17)

All return **200** + full product HTML with browser User-Agent:

```
https://www.amazon.com/dp/1098119517
https://www.amazon.com/Flutter-Dart-Cookbook-Developing-Applications/dp/1098119517
https://www.amazon.com/gp/product/1098119517
```

- Slug segment before `/dp/` is **optional** (decorative). Bare `/dp/{id}` works.
- `GET /dp/1098119517` final URL stayed `/dp/1098119517` (no forced redirect to slug).
- `rel=canonical` on slug URL points to slug form, but crawl can normalize to bare `/dp/{id}`.

## Product id

- Books commonly use **ISBN-10** as the `/dp/` token (`1098119517`, 10 digits).
- Page also embeds `"parentAsin":"B0BQ9VH43G"` (Kindle/edition parent) — URL id remains ISBN-10 for this paperback sample.
- `/dp/B0BQ9VH43G` on rapid follow-up → small **Robot Check / automated access** page (~3.8 KB, no `#productTitle`).

## HTML that worked

- `#productTitle` → title
- `#bylineInfo` → author (`Richard Rose (Author)`)
- `#detailBullets_feature_div ul li` → Publisher, Publication date, Language, Print length, ISBN-10, ISBN-13, …
  - List items contain Unicode bidi marks (`\u200f`, `\u200e`) around labels — strip when parsing.
  - Real pages also use named entities `&rlm;` / `&lrm;` (e.g. `Publisher &rlm; : &lrm; Packt Publishing`).
- `#bookDescription_feature_div` → description (fallback `#productDescription`)
- `#wayfinding-breadcrumbs_feature_div` → Books › Computers & Technology › …
- `#landingImage[data-a-dynamic-image]` → JSON map of cover image URLs
- `#acrPopover[title]` → `4.1 out of 5 stars`
- `<meta name="title">`, `<meta name="description">` → usable fallbacks

## Did not work / fragile

- `application/ld+json` — **0 blocks** on tested book page
- `/dp/9781098119515` (ISBN-13) — HTTP 200 but **soft-block page** after rapid sequential requests (no `#productTitle`, api-services-support boilerplate)
- Plain `curl` without browser UA / cookies — risk of block increases with volume

## Detail bullets (sample parse)

```
Publisher : O'Reilly Media
Publication date : January 24, 2023
Language : English
Print length : 307 pages
ISBN-10 : 1098119517
ISBN-13 : 978-1098119515
```

## Second sample (`1800565992`, verified 2026-07-17)

```
https://www.amazon.com/Flutter-Beginners-introductory-cross-platform-applications/dp/1800565992
```

| Field | Value |
|-------|-------|
| Title | Flutter for Beginners: … Flutter 2.5 and Dart |
| Authors | Thomas Bailey, Alessandro Biessek |
| Publisher | Packt Publishing |
| Publication date | October 18, 2021 |
| Print length | 353 pages |
| ISBN-10 | 1800565992 |
| ISBN-13 | 978-1800565999 |

## Reference implementation review (2026-10-28)

Evidence source: `DataTool/src-tauri/src/adapters/amazon_book.rs` and its unit fixtures. This is implementation evidence, not a new live Amazon crawl.

- The fixtures cover both `#bylineInfo .author a` markup and a plain `#bylineInfo a` without Amazon CSS classes. Author parsing must support both shapes.
- Detail-bullet lookup cleans bidi markers and compares known labels case-insensitively.
- The reference parser uses `link[rel=canonical][href]` as the displayed product URL, while fetches and cache keys continue to use the normalized `/dp/{id}` seed.
- Its conservative soft-block rule treats HTML below 20 KB without `#productTitle` as blocked.
- Tests cover both single-author (`1098119517`) and multi-author (`1800565992`) pages.
- The reference URL validation is over-broad: its generic path fallback accepts `https://example.com/dp/1098119517`, and `validate()` accepts any URL before `resolve_seed_url()` rejects it. Consumers should require an Amazon host for absolute URLs and only apply the generic `/dp/` or `/gp/product/` rule to path-only input.
