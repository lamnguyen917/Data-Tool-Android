# Heyzo adapter observations

Evidence reviewed on 2026-10-29 from the sibling Rust desktop project and limited live requests to the official Heyzo hosts.

## Evidence scope

Source files reviewed:

- `DataTool/src-tauri/src/adapters/heyzo.rs`
- `DataTool/src-tauri/src/adapters/util.rs`
- `DataTool/src-tauri/src/adapters/http.rs`
- `DataTool/src-tauri/src/core/adapter.rs`
- `DataTool/src-tauri/src/core/result.rs`
- `DataTool/src-tauri/src/adapters/mod.rs`

The sibling project has no Heyzo HTML fixtures and no Heyzo-specific automated tests. The current selectors are therefore implementation evidence, not a verified description of the latest product-page markup.

## Adapter identity

- Adapter key: `heyzo`
- Display name: `Heyzo`
- Japanese localization flag: enabled
- Canonical SKU shape: exactly four ASCII digits
- Keyword search: unsupported by the adapter contract default

The registry exposes the adapter under `heyzo` and associates cached files with the `heyzo_` prefix.

## Input handling

The Rust validator accepts either:

- a raw value matching `^\d{4}$`; or
- any value accepted by the shared `is_url()` helper.

The shared URL helper only requires an `http://` or `https://` prefix and at least one dot. It does not restrict the host.

The parser then resolves the SKU using:

```regex
/moviepages/(\d{4})/
```

Consequences:

- validation and parsing disagree for URLs that do not contain the expected path;
- a foreign host with a matching `/moviepages/{sku}/` path is accepted;
- surrounding whitespace is not removed before matching a raw SKU;
- an accepted supplied URL is never fetched directly; only its four-digit SKU is retained and canonical Heyzo URLs are rebuilt.

The last behavior prevents the current parser from requesting the foreign input host, but the input contract remains misleading and should be made strict in Android.

## Fetch flow

For a resolved SKU, the Rust adapter constructs two fixed URLs:

```text
Japanese https://www.heyzo.com/moviepages/{sku}/index.html
English  https://en.heyzo.com/moviepages/{sku}/index.html
```

The Japanese page is loaded first, followed by the English page. Both requests must succeed before parsing starts. A failure in either language fails the complete product request.

Cache keys are deterministic and language-specific:

```text
heyzo_{sku}_jp.html
heyzo_{sku}_en.html
```

The shared loader uses a desktop Chrome user agent, a shared cookie store, a 45-second timeout, successful-status enforcement, charset detection, and a disk cache. It does not classify Cloudflare, CAPTCHA, or soft not-found HTML.

## Rust parsing behavior

Both pages are parsed as HTML.

### English page

| Result data | Selector or rule |
|---|---|
| Title | first `h1` |
| Performers | links in the table cell following a cell whose text contains `Actress(es)` |
| Type categories | links in the second cell of a `tr` whose first-cell text equals `Type` |
| Sex-style categories | links in the second cell of a `tr` whose first-cell text equals `Sex Styles` |

For performer and category links, the shared `link_texts()` helper prefers the anchor `title` attribute when non-empty and otherwise uses normalized visible text.

### Japanese page

| Result data | Selector or rule |
|---|---|
| Title | first `h1` |
| Performers | `tr.table-actor a, tr[class*='table-actor'] a` |
| Release date | text in the table cell following a cell whose text contains `公開日` |
| Actor-type categories | `tr.table-actor-type a, tr[class*='table-actor-type'] a` |
| Keyword categories | `ul.tag-keyword-list li a` |

The performer and category selectors normalize visible anchor text but do not use the anchor `title` fallback applied to the English page.

## Result construction

The top-level result represents the English page and contains:

- the normalized four-digit SKU;
- English title and performers;
- studio fixed to `Heyzo`;
- release date copied from the Japanese page;
- canonical URL set to the Japanese URL;
- one category list containing English Type, English Sex Styles, Japanese actor-type values, and Japanese keyword values.

The nested Japanese result contains:

- the same SKU;
- Japanese title and performers;
- Japanese release date;
- studio fixed to `Heyzo`;
- Japanese canonical URL.

Japanese categories are not stored in the nested Japanese result. They are appended to the top-level category list instead. Neither category list nor performer list is deduplicated.

The adapter does not parse description, image, duration, series, price, rating, or a product-page SKU field.

## Live access check on 2026-10-29

The official `https://www.heyzo.com/` homepage returned an age-confirmation page and identified the site as Heyzo.

The following representative product routes were probed:

```text
https://www.heyzo.com/moviepages/3000/index.html
https://en.heyzo.com/moviepages/3000/index.html
```

Both `HEAD` requests and browser-like `GET` requests returned:

- HTTP `403 Forbidden`;
- `Server: cloudflare`;
- a small Cloudflare response body;
- no redirect to a product or age-confirmation route.

The requests used a current desktop Chrome user agent and English/Japanese language preferences. This check confirms current Cloudflare blocking from the test environment, but it does not confirm whether SKU `3000` exists or whether the Rust selectors still match an accessible product page.

No attempt was made to bypass Cloudflare, automate a browser challenge, log in, or solve a CAPTCHA.

## Risks and migration findings

- The current live environment blocks direct product requests before useful HTML is returned.
- There are no local Heyzo fixtures or parser unit tests in the Rust project.
- Validation accepts unrelated URLs while parsing requires one specific path shape.
- The parser trusts any host that embeds the expected path, although it later rebuilds official URLs.
- A four-digit raw SKU is not trimmed before matching.
- Both language pages are mandatory, so an optional localization failure discards a usable English result.
- The Japanese page is fetched before the top-level English page.
- Top-level canonical URL and top-level language do not agree: English metadata points to the Japanese URL.
- English and Japanese categories are mixed into one list, losing language boundaries.
- Categories and performers can contain duplicates.
- `td_following_label()` uses substring matching across every table cell and can select an unintended row.
- The adapter does not distinguish blocked, missing, and structurally changed pages beyond HTTP success or failure.
- A cached historical response can hide a current live-site failure or selector change.
- Search is not implemented, and current evidence does not justify adding anonymous keyword search.

## Android implications

An Android port should:

- accept a trimmed four-digit SKU or a strict HTTPS product URL on an approved Heyzo host;
- extract the SKU and rebuild canonical language URLs instead of fetching arbitrary input URLs;
- fetch English as the required top-level result and Japanese as optional localization;
- map Japanese metadata to `localized["ja"]` rather than mixing languages;
- keep English categories at the top level and Japanese categories in localized metadata;
- map the release date to the stable English key `release_date`;
- classify HTTP 401/403 and Cloudflare/challenge HTML as `BLOCKED`;
- distinguish a known not-found page from an unknown parser failure when fixtures become available;
- use sanitized local fixtures for parser tests and keep any live smoke test ignored by default;
- avoid browser-challenge automation or any attempt to bypass access controls.
