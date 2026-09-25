# Changelog

## 0.1.6 — 2026-09-24

- versionCode 7 / versionName 0.1.6.
- Scheduler logic v3.
- Added 5-second process-start grace before recovering a missing periodic job.
- Detects recent JobService launch and skips false rescheduling.
- Added scheduler process-start/skip diagnostics.
- Diagnostic schema v4.
- SQLite DB v4.
- DB v4 reclassifies historical known system/package-installer rows to SYSTEM and removes identity.
- Added Xiaomi global package installer and DocumentsUI to SYSTEM classification.
- Added system-app-only generic OEM installer/document picker detection.
- Summary aggregation now accumulates milliseconds before converting to seconds.
- Zero-second transition artifacts are omitted from the user app ranking.
- UI reads versionName dynamically instead of hard-coding it.
- Added single-flight export protection after a rapid-export race produced one IOException in 0.1.5.

## 0.1.5
- In-place update validation build.
- Serialized collection.
- Scheduler logic v2.
- Non-overlapping timeline with privacy precedence.
- DB v3 duplicate repair.
