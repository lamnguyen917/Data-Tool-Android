# Raw — Amazon book observations

Unedited working notes. Curated form: `processed/amazon-book.md`.

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
