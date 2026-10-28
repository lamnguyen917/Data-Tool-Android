# Raw observations — Ave adapter

Evidence reviewed on 2026-10-28 from `DataTool/src-tauri/src/adapters/ave.rs`, the shared Rust adapter contract, result model, HTTP loader, adapter registry, and utility functions. This is implementation evidence only; the AV Entertainments website was not re-crawled during this review.

## Registration and contract

- Adapter key: `ave`.
- UI label: `Ave`.
- Registered in the Rust desktop adapter catalog.
- `has_lang_jp()` returns true.
- Implements both `parse()` and candidate-returning `search_items()`.
- Returns the shared `ItemResult` with an optional nested Japanese `ItemResult`.

## Endpoints

English search template:

```text
https://www.aventertainments.com/dvd/search?lang=1&cat=29&culture=en-US&keyword={0}
```

The keyword passed by `search_items()` is form URL-encoded. Product links are taken from the search response instead of being constructed from the SKU.

Japanese product URLs are derived from an English product URL by applying all of these replacements:

```text
lang=1        -> lang=2
culture=en-US -> culture=ja-JP
/1/           -> /2/
```

## Input validation and normalization

Before validation and comparison, a case-insensitive leading `dvd1` is removed.

Validation regular expression:

```regex
^\w{2,}\-{0,1}\w{0,1}\d{2,5}$
```

Observed implications:

- `RHJ-100`, `DVD1RHJ-100`, and `dvd1pt-101` normalize to SKU-like values.
- Comparison uses ASCII uppercase after removing `dvd1`.
- The expression allows an optional hyphen and one optional word character before the final 2–5 digits.
- Rust `\w` is Unicode-aware, so the accepted character set is broader than ASCII letters and digits.
- `parse()` explicitly accepts URLs, but `validate()` does not. A caller that always gates parsing through `validate()` cannot use the URL feature.
- URL validation is generic through `is_url`; `parse()` does not enforce an AV Entertainments host.

The unit test `strip_dvd1_handles_japanese_keyword` confirms that checking the four-byte prefix with `get(..4)` does not panic on a multibyte Japanese keyword.

## Search behavior

`search_items(keyword)` performs this pipeline:

1. Trim the keyword and remove an optional `dvd1` prefix.
2. Return an empty list when the cleaned value is empty.
3. Form URL-encode the keyword.
4. Request the English search endpoint.
5. Parse `.single-slider-product` cards.
6. Read the product link from `.single-slider-product__image a`.
7. Replace only `&amp;` in the link.
8. Deduplicate by the decoded link.
9. Derive SKU from the product image filename; fall back to the URL `pro` query value.
10. Read title from image `alt`; fall back to `.product-title`.

Search-card selectors:

| Purpose | Selector / source |
|---------|-------------------|
| Card | `.single-slider-product` |
| Product link | `.single-slider-product__image a` |
| Image | `img` |
| Image URL | `src`, fallback `data-src` |
| Title | image `alt`, fallback `.product-title` |

Image-derived SKU logic:

- Take the file stem of the image URL.
- Convert the stem to lowercase for prefix handling.
- Remove a leading `dvd1`.
- Return ASCII uppercase.

URL-derived fallback SKU logic:

```regex
[?&]pro=(\d+)
```

The fallback becomes `PRO{digits}` after normalization. It identifies the AV Entertainments product record, not necessarily the catalog item number displayed on the detail page.

`search(sku)` calls `search_items(sku)` and returns the first candidate whose normalized SKU exactly matches the normalized input. It does not use title similarity or partial matching.

## Product resolution

For a non-URL input:

1. Remove the `dvd1` prefix.
2. Search for candidates.
3. Select the first exact normalized SKU match.
4. Return `None` if no exact match exists.
5. Parse the English product page.
6. Parse the derived Japanese page when its URL differs.

For a URL input:

1. Use the URL directly.
2. Use `pro{digits}` as the cache identity when the query contains `pro=`.
3. Otherwise use the literal fallback cache identity `url`.
4. Parse English and derive Japanese as above.

## English and Japanese page parsing

Both languages use the same `parse_page()` routine. Labels determine which fields are populated.

Top-level selectors:

| Result field | Selector |
|--------------|----------|
| Title | `.section-title h3` |
| Description | `.product-description` |
| Grouped metadata | `.additional-info` |
| Group heading | `h4` |
| Flat metadata row | `.single-info` |
| Flat label | `.title` |
| Flat value | `.value` |

Grouped metadata headings:

| English | Japanese | Mapping |
|---------|----------|---------|
| `Product Details` | `商品詳細` | `details` map |
| `Starring` | `主演女優` | `starring[]` from links |
| `Category` | `カテゴリ` | `category[]` from links |
| `Series` | `シリーズ` | `details[Series]` or `details[シリーズ]` |
| `Features` | `DVD特典` | Explicitly ignored |

If an `.additional-info` block has an unknown or missing heading and `details` is still empty, the parser attempts to extract key/value detail text from that block. This supports newer page layouts that omit the `Product Details` heading.

Flat metadata labels:

| English labels | Japanese labels | Mapping |
|----------------|-----------------|---------|
| `Studio` | `スタジオ` | `studio` when still empty |
| `Starring` | `主演女優` | linked performers when still empty |
| `Category` | `カテゴリ` | `.value-category a, .value a` when still empty |
| `Series` | `シリーズ` | series detail when not already present |
| `Item Number`, `Item#`, `Item #`, `SKU` | `品番`, `商品番号` | `sku` and same entry in `details` |
| `Date` | `発売日` | `date` |

Date parsing first searches for `\d{1,2}/\d{1,2}/\d{4}`. If no match exists, it removes the exact English suffixes `(Discontinue)` and `(Available)` and keeps the remaining text.

SKU fallback order after label parsing:

1. Known item-number keys already stored in `details`.
2. First `img[src]` whose image-derived filename matches `^[A-Z0-9]{2,}[-_]?[A-Z0-9]*\d{2,5}$`.
3. For a non-URL request only, the normalized requested SKU.

The final SKU is normalized by removing `dvd1` and applying ASCII uppercase.

## Japanese-result behavior

- Japanese parsing runs only when `to_jp_url(url) != url`.
- A successful Japanese page becomes `result.jp`.
- If the Japanese SKU is missing, it inherits the English SKU.
- If English SKU is missing but Japanese SKU exists, English inherits the normalized Japanese SKU.
- If the Japanese request or parse fails, `result.jp` still exists with only SKU and Japanese URL populated.
- If the supplied URL already looks Japanese and no replacement changes it, no nested Japanese result is created.

## HTTP and cache behavior

The shared `HttpLoader` provides:

- browser-like user agent;
- shared cookie store;
- 45-second request timeout;
- successful-status enforcement;
- response decoding from HTTP charset, HTML meta charset, or UTF-8 fallback;
- disk response cache;
- rejection of cached text that appears badly decoded.

Ave cache keys:

| Request | Cache key |
|---------|-----------|
| Search | `ave_{sanitized-keyword}_search.html` |
| English product | `ave_{cache_sku}en.html` |
| Japanese product | `ave_{cache_sku}jp.html` |

The search cache replaces non-ASCII-alphanumeric, non-hyphen, and non-underscore characters with `_`. Distinct keywords can therefore collide after sanitization.

Direct URLs without a `pro=` query all use `cache_sku = "url"`, so unrelated direct URLs collide at `ave_urlen.html` and `ave_urljp.html`.

## Tests in the Rust source

| Test | Type | Coverage |
|------|------|----------|
| `ave_parses_rhj_100` | Live Tokio integration test | Validation, exact search, English fields, Japanese URL and localized fields for `RHJ-100` |
| `strip_dvd1_handles_japanese_keyword` | Unit test | Multibyte-safe prefix handling and case-insensitive `dvd1` removal |
| `ave_search_mami_aikawa` | Ignored live network test | Candidate search for a Japanese performer keyword |

There are no local HTML fixtures in `ave.rs`. The main integration test is not marked ignored and therefore depends on the current external website, network, and product availability.

## Risks and migration findings

- Validation and parsing disagree about direct URLs.
- Direct URL handling accepts foreign hosts.
- Direct URLs without `pro=` share cache files.
- Sanitized search keywords can share a cache file.
- The `pro` fallback is not guaranteed to be the displayed product SKU.
- SKU fallback scans every image and can select an unrelated asset filename that happens to match the regex.
- `get_details()` only accepts `.title` text that itself contains `:`; layouts with separate label and value nodes depend on the later `.single-info` pass.
- HTML entity decoding for product links only handles `&amp;` explicitly.
- Japanese fetch failure is represented by a non-empty but mostly blank nested result, which can be mistaken for successful localization.
- Date output is not normalized to ISO 8601.
- Live network tests are fragile and do not isolate parser regressions.
- The Android model initially had no localized-result structure corresponding to Rust `jp`; the Ave port added locale-indexed metadata.

## Live verification — RHJ-101 (2026-10-28)

- `GET /dvd/search?...&keyword=RHJ-101` returned HTTP 200 only after redirecting to `/cart/verifybot`.
- The verification page contains a Google reCAPTCHA form, `.g-recaptcha`, `I am not a robot`, an antiforgery token, and the original search URL as `referralLink`.
- A session that first visited `/dvd/index`, received `AVECustID`, used desktop browser headers, and supplied a same-origin referrer was still redirected to VerifyBot.
- The public `robots.txt` explicitly disallows `/dvd/search`; the Android adapter must not attempt to bypass the CAPTCHA.
- Direct product record `pro=30719` is accessible and returns SKU `RHJ-101` with title `Red Hot Jam Vol.101 Attack of Hard Cocked Samurai in LA (**no cover, disc only**)`.
- Live product markup stores the item number in `.single-info .tag-title`, not `.single-info .value`. The Android parser now accepts `.value`, `.tag-title`, and `.value-category`.
- A temporary Android live smoke run fetched `pro=30719` through `AveAdapter` and verified the returned SKU and title. The test remains ignored by default so the normal suite stays deterministic.
