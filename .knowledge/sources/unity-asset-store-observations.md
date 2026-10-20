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
