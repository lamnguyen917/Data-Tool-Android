# FC2 adapter research

Status: researched from live article `4871322`; not implemented in the Android application.

Source evidence: [`../sources/fc2-adapter-observations.md`](../sources/fc2-adapter-observations.md)

Live reference: `https://adult.contents.fc2.com/article/4871322/`

## Adapter identity

- Proposed source ID: `fc2`
- Proposed display name: `FC2`
- Canonical SKU: the numeric article ID only, for example `4871322`
- Product URL template: `https://adult.contents.fc2.com/article/{sku}/?lang=ja`
- Search: unsupported for anonymous requests
- Separate Japanese localization: not required; the product content is seller-authored and remains Japanese across FC2 interface languages

## Input rules

Accept either:

- a numeric SKU matching `^\d+$`;
- a trusted HTTPS URL on `adult.contents.fc2.com` whose path matches `^/article/(\d+)/?$`.

Extract and store only the numeric path segment as the result identity. The supplied URL therefore resolves to SKU `4871322`.

Optional convenience aliases such as `FC2-PPV-4871322` or `FC2 PPV 4871322` may be normalized by extracting the final digit sequence, but the stored SKU must remain `4871322`.

Reject look-alike hosts, non-HTTPS URLs, search URLs, embed URLs, and article paths with extra segments.

## Fetch flow

1. Normalize the input to a numeric SKU.
2. Request `https://adult.contents.fc2.com/article/{sku}/?lang=ja` with the shared browser-like HTTP client and cookie jar.
3. Parse the Product JSON-LD object.
4. If no Product JSON-LD exists, check the page title/body for the FC2 not-found marker and return `NOT_FOUND`; otherwise return `PARSE`.
5. Parse supplemental DOM fields such as seller, duration, sale date, supported devices, and sample images.
6. Optionally request the description iframe with the same cookie session and product-page referrer.
7. Return a single top-level result; do not duplicate the same seller-authored content into `localized["ja"]`.

## Field mapping

| FC2 source | Android field |
|---|---|
| JSON-LD `sku` or `productID` | `productId` |
| JSON-LD `name` | `title` |
| Cleaned iframe `div[data-container]` | `description` |
| JSON-LD `image.url` | `imageUrl` |
| JSON-LD `offers.url` | `canonicalUrl` |
| `[data-article-seller-name]` | `creators` and `attributes[studio]` |
| Sale Day / `販売日` | `attributes[release_date]` |
| `.items_article_info` | `attributes[duration]` |
| JSON-LD `offers.price` | `attributes[price]`, formatted as FC2 points |
| JSON-LD `aggregateRating.ratingValue` | `attributes[rating]` |
| JSON-LD `aggregateRating.reviewCount` | `attributes[rating_count]` |
| Supported-device list | source-specific `supported_devices` attribute if added |
| `a[data-pdp-sample-thumbnail]` | optional sample-image list if the product model is extended |

Do not use JSON-LD `brand.name` as the seller for the verified page because it is `null`. The DOM seller element is the authoritative fallback.

## Parsing priorities

### Title

1. Product JSON-LD `name`.
2. Main thumbnail `alt`.
3. Main thumbnail `title`.

Avoid whole-heading `.text()` because the live heading includes hidden noise.

### Description

1. Full description iframe, requested from the session-specific `src` found on the current product page.
2. JSON-LD `description` as a degraded preview.

For the iframe, parse `div[data-container]`, remove scripts, styles, images, hidden marker nodes, `.sample_block`, and `.kJhxnUI`, then normalize whitespace. Never construct or cache the iframe access token independently.

### Price and rating

Prefer JSON-LD values because the DOM can contain original price, discounted price, promotion dates, and unrelated recommendation-card prices at the same time. Preserve the point-based unit in display formatting and treat all values as time-sensitive.

## Error classification

| Condition | Result |
|---|---|
| HTTP 404 | `NOT_FOUND` |
| HTTP 401 or 403 | `BLOCKED` |
| HTTP 429 | `RATE_LIMITED` |
| HTTP 200 with FC2 not-found title and no Product JSON-LD | `NOT_FOUND` |
| Login/reCAPTCHA page returned from search | search unsupported, or `BLOCKED` if encountered during product fetch |
| Product JSON-LD missing on an otherwise unknown page | `PARSE` |
| Description iframe failure after product parsing | keep the product and use JSON-LD description fallback |

## Search policy

Although FC2 publishes a GET search form using parameter `q`, anonymous search currently resolves to the FC2 login page. The first implementation should expose no keyword search and should fetch directly from the deterministic numeric article URL.

Search can be revisited only if FC2 provides an anonymous result page or an authorized API that fits the application's account and credential policy. Do not automate login or CAPTCHA handling.

## Fixture and test plan

Create sanitized local fixtures for:

- the verified product page with Product JSON-LD and hidden heading noise;
- the description iframe with hidden marker nodes and recommendation blocks;
- the soft HTTP-200 not-found page;
- the login/reCAPTCHA response returned by anonymous search.

Required tests:

- URL and numeric-SKU normalization to `4871322`;
- rejection of look-alike hosts and non-article paths;
- JSON-LD title, SKU, image, price, rating, and canonical URL mapping;
- DOM seller, duration, release-date, and sample-image extraction;
- full-description cleanup without hidden tokens or recommendation titles;
- fallback to truncated JSON-LD description when iframe loading fails;
- HTTP-200 not-found classification;
- protocol-relative image normalization;
- explicit unsupported search behavior.
