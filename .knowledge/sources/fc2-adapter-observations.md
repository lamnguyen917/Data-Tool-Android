# FC2 adapter observations

Observed on 2026-10-28 against the supplied live page:

- Product URL: `https://adult.contents.fc2.com/article/4871322/`
- Canonical SKU: `4871322`
- Canonical URL pattern: `https://adult.contents.fc2.com/article/{sku}/`

## Response behavior

- The product URL returned HTTP 200 without an age-confirmation redirect.
- The response set a PHP session cookie plus `contents_mode=digital` and `contents_func_mode=buy` cookies.
- `?lang=en` and `?lang=ja` translate FC2 interface labels, but the seller-authored product title remains Japanese.
- A nonexistent article, tested with `9999999999`, also returned HTTP 200. Its title indicated that the product was not found and it did not contain Product JSON-LD.

## Structured metadata

The page contains a `script[type="application/ld+json"]` object with `@type: Product`.

Verified fields for `4871322`:

- `sku`: `4871322`
- `productID`: `4871322`
- `name`: the seller-authored product title
- `image.url`: `https://storage200000.contents.fc2.com/file/333/33225285/1774767489.87.png`
- `aggregateRating.ratingValue`: `5`
- `aggregateRating.reviewCount`: `196`
- `offers.price`: `1639`
- `offers.url`: `https://adult.contents.fc2.com/article/4871322/`
- `offers.availability`: `OnlineOnly`
- `potentialAction.target.urlTemplate`: the cart URL for article `4871322`

The JSON-LD description is locale-dependent and incomplete. It was empty with `lang=en` and contained only a short truncated preview with `lang=ja`.

## HTML metadata

- Product header: `.items_article_header`
- Main thumbnail: `.items_article_MainitemThumb img`
- Duration: `.items_article_MainitemThumb .items_article_info` (`42:40`)
- Seller: `[data-article-seller-name]` (`KEN REAL`)
- Seller profile: `https://adult.contents.fc2.com/users/1365/`
- Sale date container: `.items_article_softDevice p`
  - English text: `Sale Day : 2026/04/17`
  - Japanese text: `販売日 : 2026/04/17`
- Displayed product identifier: `FC2 PPV 4871322`
- Sample images: `a[data-pdp-sample-thumbnail]`
- Description iframe: `iframe[data-iframe="description"]`

The visible product-heading element contains a hidden, random-looking text span. A parser that calls `.text()` on the entire heading will contaminate the title. JSON-LD `name` is the preferred source; the main image `alt` or `title` is a safer fallback.

## Full description

The complete description is loaded from a session-specific iframe URL resembling:

`/widget/article/4871322/description?ac={token}`

The token must be read from the current product HTML. The iframe request succeeded when it reused the product-page cookie session and sent the product URL as the referrer.

The description content starts inside `div[data-container]`, but the same container can include seller-authored related-product blocks and FC2 recommendation markup. It also contains hidden anti-scraping marker text. Before normalizing text, remove:

- `script`, `style`, and image elements;
- `.kJhxnUI` recommendation sections;
- `.sample_block` related-product cards;
- `[hidden]` elements;
- elements whose inline styles hide content with `display: none`, `opacity: 0`, or `height:0`;
- empty marker nodes such as `div[data-id]`.

If the iframe request fails, the JSON-LD description can be used as a degraded fallback, but it must be treated as a preview rather than the complete description.

## Search behavior

The public header exposes this form:

- Action: `https://adult.contents.fc2.com/search/`
- Method: `GET`
- Query parameter: `q`

An anonymous request to `/search/?q=4871322` returned the generic FC2 login page with reCAPTCHA, even after first loading the product page in the same cookie session. Search should therefore be reported as unsupported for the initial adapter. Numeric SKU input does not require search because its product URL is deterministic.

## Risks

- HTTP 200 does not prove that a product exists.
- Product heading text is intentionally polluted by hidden content.
- Description iframe tokens are session-specific and must not be cached as stable URLs.
- JSON-LD descriptions can be empty or truncated depending on interface language.
- Image and sample links can be protocol-relative and must be normalized to HTTPS.
- Price and review values are live data and can change between requests.
