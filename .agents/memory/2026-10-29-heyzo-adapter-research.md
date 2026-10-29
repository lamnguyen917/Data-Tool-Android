# Heyzo adapter research documentation

- Reviewed the complete sibling Rust Heyzo adapter, shared URL utilities, HTTP loader, result model, registry, and adapter contract.
- Verified that the Rust implementation has no Heyzo-specific fixtures or automated tests.
- Probed the official English and Japanese product routes on 2026-10-29; both returned HTTP 403 from Cloudflare with browser-like request headers.
- Added raw evidence under `.knowledge/sources/heyzo-adapter-observations.md`.
- Added an Android migration guide under `.knowledge/pages/heyzo-adapter-research.md`.
- Documented strict four-digit and trusted-URL resolution, bilingual field separation, error classification, cache guidance, fixture tests, implementation order, and acceptance criteria.
- Registered the new research page in the knowledge README and both knowledge indexes.
- No application code was changed.
