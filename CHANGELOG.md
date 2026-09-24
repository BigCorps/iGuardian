# Changelog

## 0.1.5 — 2026-09-24

- First in-place update validation build after fixed DEV signing.
- Serialized UsageStats collection process-wide.
- Removed manual periodic-job re-schedule after each JobService run.
- Removed scheduler replacement on every Activity resume.
- Added scheduler logic version 2 with fresh diagnostics counters.
- User/profile switch now creates a hard privacy boundary and skips UsageStats replay from the private interval.
- Added deterministic non-overlapping timeline resolution.
- PRIVATE takes precedence over SCREEN_OFF/SYSTEM/APP.
- Unknown timeline gaps are no longer silently bridged.
- DB schema bumped to v3.
- DB upgrade removes exact duplicate intervals and duplicate unlock timestamps from 0.1.4.
- Unlock summary counts distinct UsageStats timestamps.
- Expanded SYSTEM classifier for Play Services/package installers/Xiaomi system security components.
- Fixed main-screen content scrolling under system bars.
- Added TimelineNormalizer unit tests.
- versionCode 6 / versionName 0.1.5.
