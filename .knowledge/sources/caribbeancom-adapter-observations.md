# Caribbeancom adapter observations

Evidence reviewed on 2026-10-29 from the sibling Rust desktop project and a small number of live requests to the official Caribbeancom hosts.

## Evidence scope

Source files reviewed:

- `jdata-tool/src-tauri/src/adapters/carib.rs`
- `jdata-tool/src-tauri/src/adapters/util.rs`
- `jdata-tool/src-tauri/src/adapters/http.rs`
- `jdata-tool/src-tauri/src/adapters/mod.rs`
- `jdata-tool/src-tauri/src/core/cache.rs`

The Rust adapter has no Caribbeancom HTML fixtures and no Caribbeancom-specific automated tests.

Live requests (2026-10-29, desktop Chrome user agent, `Accept-Language: en-US,en;q=0.9,ja;q=0.8`):

| Request | Result |
|---|---|
| `GET https://en.caribbeancom.com/eng/moviepages/091317-498/index.html` | HTTP 200, `text/html; charset=UTF-8`, about 60 KB |
| `GET https://www.caribbeancom.com/moviepages/091317-498/index.html` | HTTP 200, `text/html; charset=EUC-JP`, about 140 KB |
| `GET https://www.caribbeancom.com/moviepages/999999-999/index.html` | HTTP 404, Japanese 404 page |
| `GET https://en.caribbeancom.com/eng/moviepages/999999-999/index.html` | **HTTP 200** soft not-found page |
| `GET https://www.caribbeancom.com/robots.txt` | HTTP 200 |

No Cloudflare challenge, CAPTCHA, age gate, or redirect was observed for these requests.

## Adapter identity

- Adapter key: `caribbeancom`; registry aliases `caribbeancom` and `carib`.
- Display name: `Carib`.
- Japanese localization flag: enabled.
- Cache prefix: `carib_` (the cache cleaner excludes `caribpr_` explicitly).
- Keyword search: not implemented (adapter contract default).

## Input handling

`validate()` accepts:

- a raw value matching `^\d{6}-\d{3}$`; or
- any value from which `extract_sku_from_carib_url()` captures `/moviepages/(\d{6}-\d{3})/`.

`parse()` uses the same rules through `resolve_sku()`, but its URL branch first requires the shared `is_url()` helper (`http://` or `https://` prefix and at least one dot).

Consequences:

- The host is never checked; any URL containing `/moviepages/MMDDYY-NNN/` is accepted.
- Surrounding whitespace is not trimmed before the raw SKU regex.
- The supplied URL is never fetched; only the SKU is kept and both canonical URLs are rebuilt.
- The Caribbeancom PR SKU form `MMDDYY_NNN` (underscore) is rejected, which correctly keeps the two sites apart.

## Fetch flow

Fixed endpoints:

```text
English  https://en.caribbeancom.com/eng/moviepages/{sku}/index.html
Japanese https://www.caribbeancom.com/moviepages/{sku}/index.html
```

The English page is loaded first, then the Japanese page. Both must succeed; a failure in either language fails the whole product.

Cache keys:

```text
carib_{sku}_en.html
carib_{sku}_jp.html
```

The shared loader uses a desktop Chrome user agent, a shared cookie store, a 45-second timeout, successful-status enforcement, charset detection from the HTTP header or HTML meta tag, and a disk cache. It does not classify soft not-found pages.

## Rust parsing behavior

### English page

| Result field | Rust rule |
|---|---|
| `title` | `h1[itemprop='name']` |
| `starring` | first element sibling after `span.spec-title` with text `Starring:`; its full text split on `,` |
| `description` | `p[itemprop='description']` |
| `details["Duration"]` | sibling after `span.spec-title` `Duration:`; `span[itemprop='duration']` text, else sibling text |
| `studio` | constant `Caribbeancom` |
| `date` | derived from the SKU, not the page |
| `url` | **Japanese** URL |

`cast_en()` builds an unused selector and discards it with `let _ = sel;`.

### Japanese page

| Result field | Rust rule |
|---|---|
| `jp.title` | `h1[itemprop='name']` |
| `jp.starring` | first `span` whose text contains `出演`, then the first following element sibling that yields `a span[itemprop='name'], span[itemprop='name'], a` |
| `jp.description` | `p[itemprop='description']` |
| `jp.studio` | constant `カリビアンコム` |
| `jp.url` | Japanese URL |

Because the Japanese selector matches nested elements at several levels, the same performer can be collected more than once (the `a` and its inner `span[itemprop='name']`). The Rust code does not deduplicate.

### Date derivation

`parse_date_from_sku(sku, '-')` reads the six digits before the separator as `MMDDYY` and maps `YY >= 80` to `19YY`, otherwise `20YY`. Output is `YYYY-MM-DD`. The month and day are not range-checked, so `999999-999` becomes `2099-99-99`.

## Live markup on 2026-10-29 (`091317-498`)

### English page

```html
<h1 itemprop="name">The Undisclosed: Scolding By Chie Aoi And Kurumi Chino</h1>
<ul itemscope itemtype="http://schema.org/VideoObject">
  <li class="movie-detail__spec">
    <span class="spec-title">Starring:</span>
    <span class="spec-content">
      <a class="spec__tag" itemprop="actor" ... href="/eng/search_act/7192/1.html">
        <span itemprop="name">Chie Aoi, Kurumi Chino</span></a>
    </span>
  </li>
  <li class="movie-detail__spec">
    <span class="spec-title">Tags:</span>
    <span class="spec-content">
      <a class="spec__tag" ... itemprop="url">original</a>
      <a class="spec__tag" itemprop="genre" ...>tittyfuck</a> ...
    </span>
  </li>
  <li class="movie-detail__spec">
    <span class="spec-title">Duration:</span>
    <span class="spec-content"><span itemprop="duration" content="T00H19M54S">00:19:54</span></span>
  </li>
</ul>
```

Observations:

- Multiple English performers share **one** `itemprop="actor"` link whose name is comma-separated, so the comma split in Rust is required.
- There is **no** `p[itemprop='description']`; English description is always empty for this product. `meta[name=description]` is a single space.
- Tags exist under `Tags:` but the Rust adapter ignores them. Tag links use either `itemprop="url"` or `itemprop="genre"`.
- No release date, no `link[rel=canonical]`, and no JSON-LD.
- The HTML `<title>` is `{performers} {title} - Caribbeancom.com`.

### Japanese page

- Served as **EUC-JP**. Search links percent-encode EUC-JP bytes (for example `/search/%B0%AA%C0%E9%B7%C3/1.html`).
- `h1[itemprop='name']` holds the Japanese title.
- `p[itemprop='description']` holds a multi-line description.
- Spec rows use `li.movie-spec` with these labels: `出演`, `再生時間`, `シリーズ`, `タグ`, `ユーザー評価`.
- Each Japanese performer has its own `a[itemprop='actor'] span[itemprop='name']`.
- Series is a single link, for example `/series/187/index.html` → `THE 未公開`.
- Tags are `a.spec-item` links (`オリジナル動画`, `美乳`, `パイズリ`, ...).
- An inline script defines `var Movie = {...}` with `movie_id`, `movie_seq`, sample video URLs, and membership flags.
- Poster paths are built in scripts as `/moviepages/{sku}/images/l_l.jpg` and `/moviepages/{sku}/images/poster_en.jpg`.

### Not-found behavior

- Japanese: HTTP 404 with title `404 ページが見つかりません - カリビアンコム` and `h1.error404-heading`. The existing `HttpClient` maps this to `NOT_FOUND`.
- English: **HTTP 200** with title `  - Caribbeancom.com`, an empty `<h1 itemprop="name"></h1>`, and empty spec rows. Rust would return a successful result with an empty English title.

### robots.txt

```text
User-agent:*
Disallow:
Sitemap: https://www.caribbeancom.com/static-seo/sitemap-pages.xml
Sitemap: https://www.caribbeancom.com/static-seo/sitemap-images.xml
Sitemap: https://www.caribbeancom.com/static-seo/sitemap-videos.xml
```

Nothing is disallowed for generic crawlers.

## Risks and migration findings

- URL validation accepts foreign hosts; only the extracted SKU is used, but the input contract is misleading.
- The English soft not-found page returns HTTP 200 and would become a blank completed result.
- English description is not available on the observed page; the Rust field stays empty.
- Japanese performer extraction can produce duplicates.
- The top-level `url` is the Japanese URL while the top-level fields are English.
- The SKU-derived date has no range validation and may not equal the actual publication date.
- Tags and series are present on the live pages but not mapped by Rust.
- The Japanese page is EUC-JP; any port must decode using the response charset, never assume UTF-8.
- A Japanese failure discards an otherwise valid English result.
