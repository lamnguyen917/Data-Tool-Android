# DataTool knowledge base

Agent-facing crawl knowledge. **Start at [`index/`](./index/)** to locate a page, then open the linked file under [`pages/`](./pages/).

```text
.knowledge/
  README.md      # this file
  index/         # fast lookup (by id / tag) → pages/
  pages/         # curated wiki articles
  sources/       # raw observations / evidence dumps
```

## Workflow

1. Open `index/by-id.yaml` or `index/by-tag.yaml`.
2. Read the linked page under `pages/`.
3. New evidence → append under `sources/`, then promote into `pages/` and refresh `index/`.

The superseded v1 layout is preserved under [`archive/legacy-v1/`](./archive/legacy-v1/) for traceability. Do not add new entries there.

## Topics

| Id | Page | When |
|----|------|------|
| `unity-crawl` | [Unity Asset Store crawl](./pages/unity-asset-store-crawl.md) | package id resolve, Asset Store HTML, GraphQL CSRF |
| `amazon-book-crawl` | [Amazon book crawl](./pages/amazon-book-crawl.md) | ASIN / ISBN, `/dp/` URL, Amazon HTML selectors |
| `steam-crawl` | [Steam Store crawl](./pages/steam-store-crawl.md) | AppID resolve, storesearch, Steam API / HTML |
| `igdb-crawl` | [IGDB crawl](./pages/igdb-crawl.md) | slug / game id, IGDB API v4, involved_companies |
| `rust-desktop-adapters` | [Rust desktop adapters](./pages/rust-desktop-adapters.md) | migration reference for Ave, AdultEmpire, AVFantasy, Carib, CaribPr, Heyzo, 1pondo, ThirdWorldMedia, and Tokyo-Hot |
| `ave-adapter-research` | [Ave adapter research](./pages/ave-adapter-research.md) | Ave input resolution, search, bilingual parsing, cache risks, Android mapping, and test plan |
| `tokyo-hot-adapter-research` | [Tokyo-Hot adapter research](./pages/tokyo-hot-adapter-research.md) | live search selectors, exact `n0001` resolution, bilingual parsing, Android mapping, and verification |
| `fc2-adapter-research` | [FC2 adapter research](./pages/fc2-adapter-research.md) | numeric article SKU, Product JSON-LD, description iframe, soft not-found behavior, search limitation, and implementation plan |
