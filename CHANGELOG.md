# Changelog

## 0.1.1 — 2026-09-24

First physical-device runtime fix.

- Confirmed GitHub Actions run #3 builds successfully.
- Added Android 13+ sideload restricted-settings guidance for the DEV APK.
- Added direct App Info helper for `⋮ > Permitir configurações restritas`.
- Clarified that Usage Access is special access and does not appear as a normal granted permission.
- Reworked the main UI with cards, compact metrics and clearer status hierarchy.
- Added system-bar/safe-area handling for modern edge-to-edge Android.
- Reworked private-app selector UI.
- Fixed potentially empty 0 KB JSON exports by persisting the pending payload in app-private cache before opening the document picker.
- Added explicit output stream failure handling, flush and zero-byte validation.
- Added export success/error technical events.
- Bumped DEV build to versionCode 2 / versionName 0.1.1.

## 0.1.0 — 2026-09-24

Initial Android foundation.

- Added local-only native Android project.
- Added GitHub Actions build pipeline.
- Added UsageStats collection and SQLite storage.
- Added privacy-before-storage rule.
- Added generic PRIVATE intervals for protected contexts.
- Added distinct reserved ANONYMOUS_BROWSER type.
- Added screen-off timeline support.
- Added best-effort user-switch PRIVATE interval signaling.
- Added daily JSON and support diagnostic JSON exports.
- Added private-app selector.
- Added CI privacy invariant checks.
- Build Fix 01: removed obsolete Android SDK `tools` package request.
- Build Fix 02: corrected invalid Kotlin escape sequence.
