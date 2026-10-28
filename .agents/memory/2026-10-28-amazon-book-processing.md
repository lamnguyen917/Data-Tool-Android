# Amazon book processing

- Added `AmazonBookAdapter` and registered it in the application container so Amazon Books can be processed by the collection queue.
- Normalized collection requests to `https://www.amazon.com/dp/{ASIN}` and serialized them with a one-second interval to reduce Amazon block risk.
- Reused the shared browser headers, English locale, cookies, redirects, timeout handling, and HTTP error mapping.
- Added `AmazonBookParser` for title, authors, publisher, publication date, language, print length, ISBN-10, ISBN-13, categories, description, cover image, rating, and rating count.
- Added captcha, automated-access warning, and small soft-block page detection mapped to `FetchErrorKind.BLOCKED`.
- Extended Amazon input validation to accept `/dp/{id}` and `/gp/product/{id}` paths in addition to bare IDs and full URLs, while restricting IDs to ISBN-10 or `B`-prefixed ASIN formats.
- Added source-specific Authors labeling and display labels for Amazon book attributes on the result detail screen.
- Kept Amazon keyword search explicitly unsupported because the verified crawl knowledge only defines product detail collection.
- Added parser, adapter, input validation, and HTML fixture tests.
