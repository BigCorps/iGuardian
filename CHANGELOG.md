# Changelog

## 0.1.8 — 2026-09-25
- versionCode 9 / versionName 0.1.8.
- Enabled AndroidX.
- Added stable WorkManager 2.12.0.
- Replaced direct JobScheduler recurrence with unique periodic work.
- Cancel legacy job IDs 41001/41002 during migration.
- Removed legacy GuardianJobService from app manifest.
- Added WorkInfo/worker telemetry, diagnostic schema v6.
- Expanded local intelligence to yesterday and last 7 days.
- Added today-vs-yesterday comparison with coverage/effective-period context.
- Queries remain local and non-persistent.
