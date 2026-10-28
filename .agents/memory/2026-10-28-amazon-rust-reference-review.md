# Amazon Rust reference review

- Reviewed `DataTool/src-tauri/src/adapters/amazon_book.rs` and compared its fixtures and parser rules with the Android implementation and crawl knowledge.
- Updated the Android author fallback to read every link inside `#bylineInfo`, including links without Amazon CSS classes.
- Made known detail-bullet label lookup case-insensitive after bidi cleanup and ignored nested `detail-bullets-wrapper` entries.
- Preserved a valid Amazon product `rel=canonical` URL for display while retaining the normalized `/dp/{id}` URL for requests; foreign canonical URLs fall back to the normalized URL.
- Aligned conservative soft-block detection with the Rust adapter's 20 KB threshold.
- Rejected absolute non-Amazon URLs that merely contain an Amazon-like `/dp/{id}` path; this intentionally avoids the over-broad behavior exposed by the Rust reference tests.
- Added tests for classless author links, case-insensitive bullet labels, canonical URLs, the 20 KB block threshold, Amazon country domains, and foreign-host rejection.
- Added explicit reference-implementation findings to the raw Amazon observations and promoted the actionable rules into the curated crawl page.
