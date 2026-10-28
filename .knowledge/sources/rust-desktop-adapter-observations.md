# Raw observations — Rust desktop adapters

Evidence reviewed on 2026-10-28 from the sibling `DataTool` Rust desktop project. This is a source-code review, not a live verification of the external websites.

Reviewed files:

- `DataTool/src-tauri/src/adapters/adult_empire.rs`
- `DataTool/src-tauri/src/adapters/ave.rs`
- `DataTool/src-tauri/src/adapters/avfantasy.rs`
- `DataTool/src-tauri/src/adapters/carib.rs`
- `DataTool/src-tauri/src/adapters/carib_pr.rs`
- `DataTool/src-tauri/src/adapters/heyzo.rs`
- `DataTool/src-tauri/src/adapters/ippondo.rs`
- `DataTool/src-tauri/src/adapters/third_world_media.rs`
- `DataTool/src-tauri/src/adapters/tokyo_hot.rs`
- `DataTool/src-tauri/src/adapters/http.rs`
- `DataTool/src-tauri/src/adapters/util.rs`
- `DataTool/src-tauri/src/adapters/mod.rs`

## Shared contract

- Every adapter exposes `name`, `has_lang_jp`, `validate`, and `parse`.
- Keyword candidate search is optional and defaults to an unsupported error.
- A parsed `ItemResult` can contain SKU, title, description, studio, date, URL, performers, categories, arbitrary details, and an optional nested Japanese result.
- The shared HTTP loader uses a browser-like user agent, a cookie store, a 45-second timeout, disk response caching, and charset detection from HTTP or HTML metadata.
- All nine adapters are registered in the Rust UI. Registration does not imply that parsing is complete.

## Adapter observations

### Ave

- Key: `ave`; Japanese flag: true.
- Search endpoint: `https://www.aventertainments.com/dvd/search?lang=1&cat=29&culture=en-US&keyword={0}`.
- Removes a case-insensitive `dvd1` prefix and compares normalized uppercase SKUs.
- Candidate cards come from `.single-slider-product`; SKU normally comes from the image filename and title from image `alt` or `.product-title`.
- Product parsing reads title, description, product details, performers, categories, series, studio, item number, and date. Japanese URLs are derived by changing language/culture parameters or `/1/` to `/2/`.
- Product URLs are accepted by `parse`, although `validate` only recognizes the SKU regular expression.
- A live integration test covers `RHJ-100`; keyword search has a network test marked ignored.

### AdultEmpire

- Key: `adult-empire`; Japanese flag: false.
- Accepts any syntactically valid HTTP(S) URL; it does not restrict the host.
- Posts `ageConfirmationClicked=true` to `https://www.adultdvdempire.com/Account/AgeConfirmation`, then fetches the target URL with the same cookie jar.
- Parses title, release date, Empire SKU, studio, UPC code, cast, and categories.
- Falls back to the input URL when no SKU is found.

### AVFantasy

- Key: `avfantasy`; Japanese flag: true.
- Accepts a direct URL, a normal SKU, or a title keyword prefixed with `!`.
- Normal SKU lookup compares the requested SKU with the search-card image filename after removing `dvd1`.
- English parsing reads title, item number, performers, studio, date, category, series, and play time.
- Japanese parsing is loaded by replacing `/1/` with `/2/` and reads title, performers, studio, category, and series.
- Known source behavior: Japanese categories are appended to the top-level result rather than the nested Japanese result.
- Candidate search is implemented separately through `.single-slider-product` cards.

### Carib

- Key: `caribbeancom`; Japanese flag: true.
- Accepts `MMDDYY-NNN` or a URL containing that value under `/moviepages/`.
- Loads both `en.caribbeancom.com/eng/moviepages/{sku}/index.html` and `www.caribbeancom.com/moviepages/{sku}/index.html`.
- Parses English and Japanese title, performers, and description; English also includes duration.
- Studio names are fixed to `Caribbeancom` and `カリビアンコム`.
- Release date is derived from the first six SKU digits with a two-digit-year pivot at 80.
- The top-level canonical URL is currently the Japanese page URL.

### CaribPr

- Key: `caribbeancompr`; Japanese flag: true.
- Accepts `MMDDYY_NNN` or any syntactically valid URL.
- Only the Japanese page is fetched and parsed. The top-level English result contains SKU, URL, and studio but no parsed metadata.
- Japanese parsing reads title, description, tags, and performers. Performer extraction scans table cells and splits the following cell on whitespace.
- There is no English endpoint or English parser in the current implementation.

### Heyzo

- Key: `heyzo`; Japanese flag: true.
- Accepts a four-digit SKU or extracts it from `/moviepages/{sku}/`; `validate` accepts any URL, but `parse` requires that URL shape.
- Loads both `www.heyzo.com/moviepages/{sku}/index.html` and `en.heyzo.com/moviepages/{sku}/index.html`.
- English parsing reads title and performers plus `Type` and `Sex Styles` categories.
- Japanese parsing reads title, performers, release date, actor-type links, and tag links.
- The top-level category list combines English and Japanese category text.

### 1pondo

- Key: `1pondo`; alias: `ippondo`; Japanese flag: true.
- Accepts `MMDDYY_NNN` or extracts it from a URL containing `movie_id/{sku}`.
- Fetches `https://www.1pondo.tv/dyn/phpauto/movie_details/movie_id/{sku}.json`.
- Metadata mapping is a stub: the response is fetched but ignored, and the result only contains SKU, URL, and an empty Japanese result.
- `validate` accepts any URL, while `parse` only accepts the documented JSON URL shape.

### ThirdWorldMedia

- Key: `third-world-media`; Japanese flag: false.
- Validation accepts a URL.
- Parsing is not implemented and always returns `ThirdWorldMedia adapter is not implemented`.

### Tokyo-Hot

- Key: `tokyo-hot`; Japanese flag: true.
- Accepts a lowercase `kNNNN` or `nNNNN` SKU, a URL, or any non-empty keyword.
- Search endpoint: `https://www.tokyo-hot.com/product/?q={0}&x=0&y=0`.
- Search resolution scans `li.detail`, requires `div.actor` text to contain the query, and takes the first link.
- English and Japanese product pages use `?lang=en` and `?lang=ja`.
- Parses title, description, performers, categories, series, label/studio, release date, duration, product ID, tags, and theme from the information list.
- SKU and URL paths load both languages. A free-form keyword loads only the English page.
- A live integration test covers `n0001` and checks the product URL plus Japanese metadata.

## Cross-adapter caveats

- Several validators accept any URL while the parser requires a specific host or path. Android ports should make validation and resolution agree and should restrict hosts.
- Search query interpolation is not consistently URL-encoded in every helper path.
- Live integration tests depend on mutable external pages and should not replace local parser fixtures.
- Japanese metadata is represented by a nested result in the Rust model. The Android `ProductResult` does not currently have an equivalent localization structure, so a port needs an explicit mapping decision.
