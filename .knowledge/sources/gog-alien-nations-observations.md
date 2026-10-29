# GOG Alien Nations observations

Observed on 2026-10-29 from the public GOG page and public JSON endpoints. No account, API key, or browser session was required.

## Sample input

```text
https://www.gog.com/game/alien_nations
```

The locale-free URL redirected according to site locale. During browser inspection it resolved to a localized path, while the explicit English URL remained stable:

```text
https://www.gog.com/en/game/alien_nations
```

The canonical link and `og:url` on the English page both pointed to the explicit English URL.

## Product identity

| Field | Observed value |
|-------|----------------|
| GOG product id | `1207658717` |
| Slug | `alien_nations` |
| Canonical URL | `https://www.gog.com/en/game/alien_nations` |
| Title | `Alien Nations` |

The numeric id appeared in the page JSON-LD as `sku`, in page scripts as `product_id`, and in both JSON APIs as `id`.

## Catalog API

Request:

```text
GET https://catalog.gog.com/v1/catalog?query=alien%20nations&limit=20
```

The response was JSON with `productCount: 1` and one entry under `products`. Relevant product fields:

```json
{
  "id": "1207658717",
  "slug": "alien_nations",
  "title": "Alien Nations",
  "releaseDate": "1999.07.01",
  "storeReleaseDate": "2014.12.19",
  "developers": ["Neo Software Produktions"],
  "publishers": ["THQ Nordic GmbH"],
  "operatingSystems": ["windows"],
  "features": [
    {"name": "Multi-player", "slug": "multi"},
    {"name": "Single-player", "slug": "single"}
  ],
  "genres": ["Strategy", "Real-time", "Fantasy"],
  "tags": ["Fantasy", "Strategy", "Classic", "Real-Time"],
  "storeLink": "https://www.gog.com/en/game/alien_nations"
}
```

The live response also included cover images, screenshots, product type, price, age rating, review counters, and large global filter dictionaries. The filter dictionaries are catalog metadata, not product metadata, and should be ignored by the adapter.

The price observed in this request was USD 5.99. Price and currency are market-, cookie-, and time-dependent and must not be treated as a static product fact.

An intentionally impossible query returned HTTP 200 with:

```json
{
  "pages": 0,
  "currentlyShownProductCount": 0,
  "productCount": 0,
  "products": [],
  "dreamlistGames": []
}
```

The response still contained the large `filters` object.

## Product API

Request:

```text
GET https://api.gog.com/products/1207658717?expand=description,screenshots,videos,related_products,changelog
```

Relevant fields returned:

```json
{
  "id": 1207658717,
  "title": "Alien Nations",
  "slug": "alien_nations",
  "content_system_compatibility": {
    "windows": true,
    "osx": false,
    "linux": false
  },
  "languages": {
    "de": "Deutsch",
    "en": "English",
    "fr": "français"
  },
  "game_type": "game",
  "release_date": "2014-12-19T12:00:00+0100",
  "description": {
    "lead": "HTML description omitted from this evidence excerpt",
    "full": "HTML description omitted from this evidence excerpt"
  }
}
```

The API also returned protocol-relative image URLs, screenshot variants, purchase/support/forum links, DLCs, related products, and changelog HTML.

Important: `release_date` from this API is the GOG store release date. It is not the original 1999 game release date.

## Product page metadata

The English HTML page was approximately 1 MB uncompressed. It contained a large site shell with unresolved Angular expressions such as `{{ product.title }}`, but the product content and metadata were still server-rendered.

The page exposed:

- `<link rel="canonical">` and Open Graph title, description, image, URL, and type.
- A `Product` / `VideoGame` JSON-LD object.
- Server-rendered title, description, genres, tags, supported OS, company, features, languages, requirements, rating, and review content.
- Images hosted under `images.gog-statics.com` and numbered GOG static hosts.

Selected JSON-LD values:

```json
{
  "@type": ["Product", "VideoGame", "SoftwareApplication"],
  "url": "https://www.gog.com/en/game/alien_nations",
  "name": "Alien Nations",
  "sku": "1207658717",
  "brand": {"name": "THQ Nordic GmbH"},
  "author": {"name": "Neo Software Produktions"},
  "publisher": {"name": "THQ Nordic GmbH"},
  "operatingSystem": "Windows",
  "releaseDate": "2014-12-19T12:00:00+01:00",
  "aggregateRating": {
    "bestRating": 5,
    "ratingCount": 405,
    "ratingValue": 3.5,
    "reviewCount": 105
  },
  "offers": [{
    "price": "5.99",
    "priceCurrency": "USD"
  }]
}
```

Again, JSON-LD `releaseDate` represented the GOG store date. The product details section separately embedded the original release timestamp `1999-07-01T00:00:00+02:00`.

The UI showed 3.5/5 and 105 reviews. Catalog fields named `reviewsRating` and `reviewsCount` were `35` and `405`; the JSON-LD clarified that 405 is the rating count while 105 is the review count.

## Missing product behavior

Requesting an intentionally unknown English product slug followed redirects and ended at:

```text
HTTP 200
https://www.gog.com/en/games
```

Therefore, HTTP 200 alone cannot prove that a product exists. The adapter must require an exact catalog slug/id match or verify that the final/canonical URL still has the expected `/game/{slug}` path.

## Interpretation

1. Prefer Catalog API for search, exact slug resolution, original release date, developer, publisher, genres, tags, platform, price, and covers.
2. Use Product API after obtaining the numeric id for full description, languages, compatibility, and media.
3. Use English product HTML only for fields missing from the APIs, such as detailed system requirements or audio/text language roles.
4. Treat product page JSON-LD as a fallback, not the only source, because its release date is the store release date and the page contains a large dynamic shell.
5. Always retain both the numeric id and slug. Use the id for API/cache identity and the slug for the public canonical URL.

