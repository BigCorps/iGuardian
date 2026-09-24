# Changelog

## 0.1.0 — 2026-09-24

Initial Android foundation ZIP.

- Added local-only native Android project.
- Added GitHub Actions build pipeline.
- Added UsageStats collection and SQLite storage.
- Added privacy-before-storage rule.
- Added generic PRIVATE intervals for protected contexts.
- Added distinct reserved ANONYMOUS_BROWSER type without pretending Android can currently detect incognito reliably.
- Added screen-off timeline support.
- Added best-effort user-switch PRIVATE interval signaling.
- Added daily JSON and privacy-safe support diagnostic JSON exports.
- Added private-app selector.
- Added CI privacy invariant checks.
- Build Fix 01: corrected Android SDK setup by removing obsolete `tools` package request and updating Java setup action.
- Build Fix 02: corrected invalid Kotlin escape sequence in `GuardianDatabase.kt`.
