# Changelog

## 0.1.7 — 2026-09-25

### Background scheduler v4
- Replaced periodic DEV JobScheduler job with alternating chained one-shot jobs.
- Job IDs 41001/41002 alternate to avoid replacing a currently running job.
- Each DEV job uses 30-minute minimum latency and 45-minute deadline.
- Next one-shot is scheduled before the current job finishes.
- Process-start recovery checks all managed jobs after an 8-second grace window.
- Scheduler logic counters reset once for v4.
- Added persisted reboot/package/user-unlock signal diagnostics.
- Added Android 16 pending-job-reasons diagnostics.
- Added job ID and stop reason diagnostics.

### Local Intelligence Alpha
- Added offline deterministic question engine.
- Added main-screen “Perguntar ao Guardian” card.
- Supports today's summary, top app/top 5, named-app time, app-use total, screen-off, unlocks, PRIVATE, SYSTEM and coverage.
- Unsupported periods are explicit rather than guessed.
- Questions are never persisted.
- No network/API/LLM call.

### Version
- versionCode 8
- versionName 0.1.7
- diagnostic schema 5

## 0.1.6
- scheduler logic v3;
- DB v4 SYSTEM sanitation;
- millisecond-first aggregate durations;
- single-flight JSON export;
- dynamic UI version.
