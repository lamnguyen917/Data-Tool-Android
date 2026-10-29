# Processing browser verification

## Change summary

- Added a queue-level `ProcessingVerification` state containing the source, product ID, trusted challenge URL, and request user agent.
- Added one shared verification gate to `DefaultCollectionQueue`. Concurrent items that encounter a supported browser challenge wait on the same deferred result instead of opening multiple WebViews.
- Added a per-source verification generation so a late blocked response from an in-flight request retries without opening a second WebView after another item has already completed verification. The generation check and WebView registration share the same lock, which closes the completion-to-registration race.
- If an item from another source waits behind the active WebView, it retries without consuming its own verification allowance. It may open the next WebView if its source is still blocked.
- Imported the verified browser cookies into the shared HTTP client and retried every waiting item after successful verification.
- Released all waiting items as `BLOCKED` when the user closes the verification screen or cancels the queue.
- Limited each item to one browser verification wait per collection attempt.
- Added challenge metadata wrapping to the Amazon, Unity, Ave, Tokyo-Hot, FC2, and Heyzo product adapters. Adapter-specific user agents are preserved.
- Added an application-level processing verification route. It opens automatically while processing and remains available even if the user navigates away from the collection screen.
- Added challenge-page HTML detection so same-URL CAPTCHA and Cloudflare pages are not submitted as successful product pages.

## Verification

- Added queue tests for shared concurrent verification, cancellation, and late stale blocked responses.
- Added browser verification HTML detection tests.
- Updated the Heyzo blocked-response test to verify its trusted challenge URL and desktop user agent.
- Ran `testDebugUnitTest` successfully on 2026-10-29.
