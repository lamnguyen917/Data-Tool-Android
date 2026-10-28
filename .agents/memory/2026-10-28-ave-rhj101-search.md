# Ave RHJ-101 search investigation

- Reproduced `RHJ-101` against the live AV Entertainments search endpoint. HTTP 200 followed a redirect to `/cart/verifybot`, which contained Google reCAPTCHA and no product cards.
- Confirmed that visiting the DVD index first, retaining `AVECustID`, and using complete desktop browser navigation headers did not avoid VerifyBot. The site's `robots.txt` explicitly disallows `/dvd/search`, so no CAPTCHA bypass was implemented.
- Updated Ave block detection for the VerifyBot URL, form, `.g-recaptcha`, and `I am not a robot` text. Search now reports `FetchErrorKind.BLOCKED` instead of an empty result list.
- Preserved search error kinds in `SearchUiState` and display the existing source-specific error label and description on the Search screen.
- Verified the direct live product URL with `pro=30719`. The first Android smoke run exposed that live pages use `.tag-title` for `Item#`; the parser now supports `.value`, `.tag-title`, and `.value-category`.
- Reran the Android live smoke test successfully: the adapter returned SKU `RHJ-101` and the expected `Red Hot Jam Vol.101` title. The live test is ignored by default.
- Added deterministic VerifyBot regression fixtures and tests for Ave parser, adapter redirects, and search error propagation.
