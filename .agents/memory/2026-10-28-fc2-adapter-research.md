# FC2 adapter research

- Analyzed live FC2 Content Market article `4871322` and defined the canonical SKU as the numeric article ID `4871322`.
- Documented strict trusted-URL validation, the deterministic article URL, Product JSON-LD mapping, supplemental DOM selectors, protocol-relative image normalization, and HTTP-200 soft not-found detection.
- Identified hidden noise inside the visible product heading; the implementation guide requires JSON-LD `name` or thumbnail attributes instead of whole-heading text.
- Verified that the complete description is loaded through a session-specific iframe token and documented cookie/referrer reuse plus removal of hidden markers and recommendation blocks.
- Verified that anonymous `/search/?q=4871322` returns the generic FC2 login/reCAPTCHA page, so the initial adapter should declare keyword search unsupported and resolve numeric SKUs directly.
- Added the FC2 research page, raw observations, knowledge indexes, and project-status link. No FC2 application code was added.
