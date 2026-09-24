# Changelog

## 0.1.2 — 2026-09-24

Physical-device export reliability fix.

- Confirmed Actions run #6 successfully built 0.1.1.
- Replaced Android 10+ document-picker export with direct `MediaStore.Downloads` export.
- Exports now go to `Downloads/iGuardian`.
- Added byte-for-byte destination read-back verification.
- Success is impossible unless the destination can be reopened and matches the generated JSON exactly.
- Added immediate Android share-sheet option after verified export.
- Kept Storage Access Framework fallback for Android 9 and lower, also with read-back verification.
- Improved restricted-settings wording to distinguish App Info from normal App Permissions.
- Bumped to versionCode 3 / versionName 0.1.2.

## 0.1.1 — 2026-09-24

- Added sideload restricted-settings guidance.
- Redesigned foundation UI.
- Added safe-area handling.
- Added first cache-backed export attempt.

## 0.1.0 — 2026-09-24

Initial Android foundation.
