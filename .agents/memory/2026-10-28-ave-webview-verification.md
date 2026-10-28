# Ave search verification in WebView

- Added a user-completed verification flow for AV Entertainments VerifyBot (Google reCAPTCHA). The app never solves or automates the CAPTCHA; the user completes it in an in-app WebView.
- `FetchException` gained an optional `challengeUrl`. `AveAdapter.loadCandidates` sets it to the search URL when the search is `BLOCKED`.
- Added domain type `VerifiedPage` (URL, HTML, browser cookie header) and interface `BrowserVerifiedSearch`. `ProductSearchRepository.searchFromVerifiedPage` defaults to `UNSUPPORTED`.
- `AveAdapter` implements `BrowserVerifiedSearch`: it accepts only trusted `/dvd/search` pages, parses candidates, and stores an in-memory `normalized SKU → product URL` map. `fetch(sku)` checks this map before searching.
- `AdapterSearchRepository` forwards verified pages to the adapter and imports browser cookies through the new `HttpClient.importBrowserCookies`.
- `SearchUiState.challengeUrl` drives a "Verification required" state with a "Verify in browser" action on the Search screen.
- New `BrowserVerificationScreen` (route `collect/search/verify`) hosts the WebView. It uses `HttpClient.USER_AGENT`, enables JavaScript, DOM storage, and third-party cookies, restricts top-level navigation to HTTPS on the challenge host, and captures `outerHTML` and cookies when the challenge redirects back to the search path.
- Added adapter tests for the challenge URL, verified-page parsing with fetch skipping search, and foreign-host rejection, plus a ViewModel test for the full challenge → verified-page flow.
- Fixed two `CollectViewModelTest` constructor calls that were missing the `settingsRepository` argument.
- Documented the flow in `.knowledge/pages/ave-adapter-research.md`.
