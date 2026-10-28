# Android project status

The application uses **Kotlin**, **Jetpack Compose**, and **Material 3**.

## Current configuration

| Item | Value |
|------|-------|
| Application ID | `com.elp.datatool` |
| Module | `app` |
| Min SDK | 24 |
| Target / Compile SDK | 37 |
| Java compatibility | 11 |

## Implementation status

The Compose UI follows [`.visualizations/wireframe.html`](../.visualizations/wireframe.html). It includes collection, product search, result cards and filters, multi-selection, result details, settings, navigation, and light/dark themes.

| Source | Search | Collection |
|--------|--------|------------|
| Unity Asset Store | Implemented through `/search?q=` and the RSC payload | Implemented through `/packages/unknown/{id}` and embedded state / JSON-LD |
| Amazon Books | Not supported | Implemented through `/dp/{ASIN}` and product HTML |
| [Ave](../.knowledge/pages/ave-adapter-research.md) | Implemented, but the site currently redirects automated search to VerifyBot/reCAPTCHA | Implemented for trusted product URLs, with English and Japanese metadata |
| Steam Store | Not supported | Not implemented; the UI reports that the source is not available yet |
| IGDB | Not supported | Not implemented; the UI reports that the source is not available yet |

The Android application currently registers `UnityAssetStoreAdapter`, `AmazonBookAdapter`, and `AveAdapter`. It also includes the OkHttp client, `DefaultCollectionQueue` with bounded concurrency, timeout, temporary-error retry, cancellation and manual retry, expiring response cache, and Coil image loading. Parser tests use local fixtures under `app/src/test/resources`.

The sibling Rust desktop project supplied the Ave reference and contains eight remaining adapters that are **implementation references only** and are not registered in the Android application:

| Adapter | Rust key | Current Rust capability |
|---------|----------|-------------------------|
| Tokyo-Hot | `tokyo-hot` | English and Japanese parsing for SKU or URL; keyword resolution is English-only |
| 1pondo | `1pondo` | JSON request only; metadata mapping is still a stub |
| Heyzo | `heyzo` | English and Japanese product parsing for a four-digit SKU or movie URL |
| AdultEmpire | `adult-empire` | Product-URL parsing with an age-confirmation request; no Japanese result |
| AVFantasy | `avfantasy` | SKU, URL, and title-keyword resolution with English and partial Japanese parsing |
| Carib | `caribbeancom` | English and Japanese parsing for a dated SKU or movie URL |
| CaribPr | `caribbeancompr` | Japanese-only parsing; the top-level English result remains mostly empty |
| ThirdWorldMedia | `third-world-media` | Registered but not implemented; every parse call returns an error |

See [Rust desktop adapter reference](../.knowledge/pages/rust-desktop-adapters.md) for input rules, endpoints, field mappings, selectors, and known limitations extracted from the Rust source.

Result copying supports **Copy line** with CSV/TSV and configurable field order, plus **Copy details** with placeholders such as `{title}` and `{sku}`. Configuration is available under Settings > Copy, and formatting logic lives in `domain/copy/CopyFormatter.kt`.

The database, DataStore persistence, durable WorkManager queue, and file export remain target architecture. Results and settings currently live in memory and are lost when the application process stops.

## Reading order

1. [`01-product-overview.md`](./01-product-overview.md) — product scope and primary workflows.
2. [`02-architecture.md`](./02-architecture.md) — layer boundaries and data contracts.
3. [`03-ui-design.md`](./03-ui-design.md) — mobile UI specification.
4. [`.knowledge/index/by-id.yaml`](../.knowledge/index/by-id.yaml) — source-specific crawl and adapter knowledge.

A feature is marked as implemented only when corresponding source code and tests exist in this Android project.
