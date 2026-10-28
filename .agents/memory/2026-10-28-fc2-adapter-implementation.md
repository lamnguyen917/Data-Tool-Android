# FC2 adapter implementation

- Added `DataSource.FC2` with numeric article IDs, `FC2-PPV` aliases, strict HTTPS `adult.contents.fc2.com/article/{id}` URL validation, and canonical normalization to the numeric SKU.
- Implemented `Fc2Adapter` and `Fc2Parser` with deterministic article fetching, Product JSON-LD parsing, DOM seller/duration/release-date/device parsing, protocol-relative image fallback, soft HTTP-200 not-found detection, and explicit unsupported search behavior.
- Extended `HttpClient.get` with optional request headers while preserving shared defaults, allowing FC2 to use an adapter-specific desktop Chrome user agent. Live FC2 omits Product JSON-LD for the shared Android mobile user agent.
- Implemented session description loading from the iframe URL discovered on the current product page, with shared cookies, a `Referer` header, cancellation propagation, and non-fatal fallback to the JSON-LD description preview.
- Added description cleanup for scripts, styles, images, hidden anti-scraping markers, seller-related product cards, and FC2 recommendation sections.
- Added the stable `supported_devices` attribute and exposed FC2 seller and metadata through English UI labels.
- Registered FC2 in the application container, source selector, copied source names, result details, project status, and knowledge indexes.
- Added sanitized product, description, not-found, and login fixtures plus parser, adapter, validation, header, cleanup, fallback, and unsupported-search tests.
- Added a normally ignored live smoke test and temporarily enabled it to verify live article `4871322`, including seller `KEN REAL`, duration `42:40`, image, clean title, and full description.
- Verified the complete debug unit-test suite: 102 tests, zero failures, and three normally ignored live tests.
- `lintDebug` still reports the same 12 pre-existing Compose `LocalContextGetResourceValueCall` errors; the lint text report contains no FC2 finding.
