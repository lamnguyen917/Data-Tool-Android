# Persist settings and selected adapter

- Replaced the production in-memory settings repository with a `SharedPreferences`-backed repository.
- Persisted theme, processing, retry, copy-line, and copy-details settings across process restarts.
- Added the selected data source to `AppSettings` and persisted it with the other settings.
- Initialized the Collect screen from the saved data source and saved every source selection change.
- Kept `InMemorySettingsRepository` as a configurable test implementation.
- Added Collect ViewModel coverage for saving and restoring the selected data source.
- Verified with `gradlew.bat testDebugUnitTest`; the build and all unit tests passed.
