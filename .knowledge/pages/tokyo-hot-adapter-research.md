# Tokyo-Hot adapter research

Status: implemented and live-verified on 2026-10-28.

Source evidence: [`../sources/tokyo-hot-adapter-observations.md`](../sources/tokyo-hot-adapter-observations.md)

## Objective

Port the sibling Rust Tokyo-Hot adapter to Android while preserving bilingual metadata and making search results usable from the existing Collect workflow.

## Input contract

The Android source accepts:

- classic IDs such as `n0001` and `k0123`;
- current alphanumeric IDs returned by Tokyo-Hot search;
- trusted HTTPS product URLs on `tokyo-hot.com` or `www.tokyo-hot.com` with a `/product/{slug}/` path.

Classic `kNNNN` and `nNNNN` IDs are normalized to lowercase. Direct product URLs keep their original value through validation and are normalized by the adapter before loading language pages.

## Resolution flow

For a product ID:

1. Request `/product/?q={encodedId}&x=0&y=0`.
2. Parse every `li.detail` card into Product ID, URL, title, and image.
3. Compare the parsed Product ID with the requested ID using exact case-insensitive equality.
4. Load the matched product URL with `lang=en` and then `lang=ja`.

For a direct product URL, skip search and load both language variants directly.

Exact ID comparison is mandatory. The live `n0001` result page currently includes `HTN0001` before `n0001`; substring matching selects the wrong product.

## Search mapping

| Android field | HTML source |
|---|---|
| `SearchHit.productId` | Product ID extracted from `.description2 .actor` |
| `SearchHit.title` | `.description2 .title` |
| `SearchHit.imageUrl` | card `img[src]` or `img[data-src]` |
| Resolution URL | first card `a[href]` |

Cards are deduplicated by absolute product URL.

## Product mapping

| Tokyo-Hot label | Android field |
|---|---|
| `Model` / `出演者` | `creators` |
| `Play` / `プレイ内容` | `categories` |
| `Tags` / `タグ` | `attributes[tags]` |
| `Theme` | `attributes[theme]` |
| `Series` / `シリーズ` | `attributes[series]` |
| `Label` / `レーベル` | `attributes[studio]` |
| `Release`, `Release Date`, `配信開始日` | `attributes[release_date]` |
| `Duration` / `収録時間` | `attributes[duration]` |
| `Product ID` / `作品番号` | result identity |

The English page supplies the top-level `ProductResult`. Japanese metadata is stored in `localized["ja"]`. The detail screen exposes localized title, performers, categories, studio, release date, series, duration, tags, theme, description, and URL using English UI labels.

## Error policy

- HTTP behavior is mapped by the shared `HttpClient`.
- Known challenge-page signals produce `FetchErrorKind.BLOCKED`.
- Missing exact search matches produce `FetchErrorKind.NOT_FOUND`.
- Missing required product title or identity produces `FetchErrorKind.PARSE`.
- Japanese-page failure is non-fatal after the English page succeeds.
- Coroutine cancellation is always rethrown.

## Verification

Local fixtures cover:

- current search-card parsing and URL deduplication;
- the `HTN0001` versus `n0001` exact-match regression;
- English field mapping;
- Japanese localized field mapping;
- poster extraction;
- challenge-page classification;
- ID normalization and direct-URL validation.

The ignored live smoke test can be run manually with:

```text
gradlew.bat testDebugUnitTest --tests com.elp.datatool.data.adapter.tokyohot.TokyoHotLiveSmokeTest
```

It was enabled temporarily and passed against live `n0001` on 2026-10-28.
