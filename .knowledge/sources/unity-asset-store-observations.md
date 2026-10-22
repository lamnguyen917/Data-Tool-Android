# Raw — Unity Asset Store observations

Unedited working notes. Curated form: [Unity Asset Store crawl](../pages/unity-asset-store-crawl.md).

## Seed URL

- Bare package id needs middle path segment: `/packages/{token}/{id}`
- Token may be `unknown` (or any non-empty string)
- Example: `https://assetstore.unity.com/packages/unknown/36050`

## Redirects (36050)

```
301 → /packages/unknown/azure-sky-dynamic-skybox-36050
301 → /packages/tools/particles-effects/azure-sky-dynamic-skybox-36050
200 HTML
```

## GraphQL

- `POST /api/graphql/batch` from a plain HTTP client: CSRF mismatch or 302 `/errors/unexpected`
- Needs a browser session CSRF cookie + matching header policy; not usable anonymously with curl/reqwest alone

## Parse sources that work

- Embedded `"id":"{packageId}"` JSON blob in HTML
- `application/ld+json` Product + BreadcrumbList
- Author/publisher often from `brand.name` (embedded `publisher` can be id-ref only)

## Search

- DuckDuckGo `site:assetstore.unity.com/packages {keyword}` can list package URLs
- Fragile; not a reliable primary resolver for bare package ids (prefer seed URL + redirects)

## 2026-10-22 — product state, unknown ids, first-party search

- Product page embeds a normalized entity cache as the first argument of
  `__component__.ReactDOMrender({...}, document.getElementById("Product/ProductDetailController"))`.
  Path: `data.ENTITY` with maps `Product`, `ProductPublisher`, `ProductVersion`, `Category`, `ProductTag`.
  References look like `{"type":"id","id":["ProductPublisher","13035"]}`.
- `Product[id]` (36050): `name`, HTML `description`, `rating {average,count}`, `originalPrice {finalPrice,currency,isFree}`,
  `mainImage {big,small,icon}` (protocol-relative `//assetstorev1-prd-cdn.unity3d.com/...`), `firstPublishedDate`,
  `supportedUnityVersions`, `downloadSize` (bytes, string), `assetCount`, `popularTags` (refs).
- `Category[108].longName` = `Tools/Particles & Effects`; `ProductPublisher[13035].name` = `7stars`;
  `ProductVersion[...]` = `{name: "8.0.8", publishedDate}`.
- Unknown id: `GET /packages/unknown/999999999` → **200**, no redirect, store home page
  (`og:url` = `https://assetstore.unity.com`, no Product state, no ld+json Product). Not a 404.
- Small ids are real packages: `/packages/unknown/1` → Detonator Explosion Framework.
- `/search?q={keyword}` is a server-rendered Next.js page. Its RSC payload
  (`self.__next_f.push([1,"..."])` string chunks, concatenated) contains the Coveo response:
  `"results":[{title, uri:"assetstore://product/{id}", clickUri, raw:{permanentid, ec_name, publisher_name,
  ec_thumbnails, ec_price, ec_rating, ec_category, ...}}]`, `totalCount`. Page size observed: 96.
  Featured results (e.g. "Behavior Designer Pro 3" for "terrain") are mixed in at the top, same as the website.
- Client-side search uses Coveo with a short-lived access token from the page; calling Coveo directly is not needed.
