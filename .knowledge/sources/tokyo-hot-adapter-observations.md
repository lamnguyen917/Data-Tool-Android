# Tokyo-Hot adapter observations

Observed on 2026-10-28 against the live site and the sibling Rust adapter.

## Live endpoints

- Search: `GET https://www.tokyo-hot.com/product/?q={query}&x=0&y=0`
- English product page: `GET {productUrl}?lang=en`
- Japanese product page: `GET {productUrl}?lang=ja`
- Verified query: `n0001`
- Verified product URL: `https://www.tokyo-hot.com/product/5000/`

## Search HTML

- Result cards use `li.detail`.
- The product link is the first `a[href]` inside the card.
- The visible title is under `.description2 .title`.
- The product ID appears under `.description2 .actor` as `(Product ID: n0001)`.
- The card image uses `img[src]`.
- A live search for `n0001` places `HTN0001` before the exact `n0001` result.

The Rust resolver checks whether actor text contains the query. Therefore, `n0001` can resolve to `HTN0001`. The Android implementation extracts the Product ID and compares it case-insensitively for exact equality.

## Product HTML

- The title is under `#main .contents h2` on both verified language pages.
- The description is under `.contents .sentence`.
- The main preview image is in `[data-poster]` on the sample player shell.
- Metadata is represented by adjacent `dt` and `dd` elements under `div.infowrapper dl.info`.

English labels observed:

- `Model`
- `Play`
- `Tags`
- `Theme`
- `Label`
- `Release Date`
- `Duration`
- `Product ID`

Japanese labels observed:

- `出演者`
- `プレイ内容`
- `タグ`
- `シリーズ`
- `レーベル`
- `配信開始日`
- `収録時間`
- `作品番号`

## Verified n0001 values

- English title: `Office Lady`
- Japanese title: `OL無残社内ナマ中出し`
- English performer: `Mai Kuramoto`
- Japanese performer: `倉本舞`
- Release date: `2003/09/15`
- Duration: `00:41:26`
- English label: `Tokyo-Hot`
- Japanese label: `東熱`
- Preview image: `https://my.cdn.tokyo-hot.com/media/5000/list_image/20170830023247/820x462_default.jpg`

## Operational notes

- The site returned HTTP 200 for the verified search and both product languages.
- Search results can contain IDs beyond the classic `kNNNN` and `nNNNN` forms, so Android validation accepts current alphanumeric, underscore, and hyphen IDs.
- Direct URLs are restricted to HTTPS Tokyo-Hot hosts and `/product/{slug}/` paths.
- A missing Japanese page does not discard a successfully parsed English result.
- Access-denied, Cloudflare, CAPTCHA, and human-verification pages are classified as blocked responses.
