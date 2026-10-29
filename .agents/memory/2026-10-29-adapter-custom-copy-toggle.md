# Adapter custom copy toggle

## Change summary

- Added `customCopyEnabled` to `AdapterSettings` and persisted it with `adapter_{sourceId}_custom_copy_enabled`.
- Added a Custom copy settings switch to each adapter settings screen.
- Copy line and Copy details editor links are shown only while custom copy is enabled.
- Disabling custom copy makes all copy actions use global settings without deleting the adapter's saved format, fields, or template. Re-enabling restores those values.
- Editing an adapter copy option enables custom copy defensively, including access through a retained or deep-linked editor route.
- Existing persisted adapter copy overrides migrate as enabled when the new boolean key is absent.

## Verification

- Updated Settings ViewModel tests to verify disabled fallback to global settings, preservation of custom values, and restoration after re-enabling.
- Updated Results ViewModel test fixtures to opt into custom copy explicitly.
