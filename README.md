# Guardian DEV — Android 0.1.10

## 0.1.9 result

The core Android data/privacy path remains stable.

Confirmed on the physical Xiaomi/Android 16 test device:
- 0.1.9 / versionCode 10;
- Usage Access and history preserved;
- no INTERNET permission;
- no QUERY_ALL_PACKAGES;
- WorkManager unique periodic work still present;
- same unique work UUID retained;
- 5 Worker runs / 5 successful results;
- 0 retry / 0 failure / 0 stopped Worker;
- current WorkInfo ENQUEUED;
- prior BOOT_COMPLETED signal preserved;
- today coverage 99.4%;
- rolling 24h diagnostic coverage 99.3%;
- full JSON analysis found no timeline overlaps;
- full JSON analysis found no identity on PRIVATE/SYSTEM;
- Photo Picker and Xiaomi App Finder no longer appear as APP.

## Background decision

The visible Worker starts show that Android/Xiaomi may defer a 30-minute periodic request substantially.

Guardian therefore treats WorkManager as best-effort maintenance, not an exact timer. UsageStats collection catches up retrospectively from the stored cursor whenever a Worker or the app runs. Exact timing is not a Phase 2 blocker.

## Report schema v3

0.1.10 adds millisecond precision while keeping seconds/minutes for compatibility.

New fields:
- timeline `duration_milliseconds`;
- app `foreground_milliseconds`;
- summary millisecond totals;
- tracking effective/recorded/unclassified milliseconds.

Coverage is calculated directly from milliseconds. Sub-second transitions no longer look like unexplained `0s` entries.

## Local Intelligence runtime self-check

The engine remains v3. Diagnostic schema v8 now runs fixed built-in local checks for summary today, top 5 last 24h, insights today and today-vs-yesterday comparison.

Only check IDs and pass/fail are exported. User-entered questions remain non-persistent.

## WorkManager cadence telemetry

Scheduling behavior is unchanged. New diagnostics keep recent Worker start timestamps, intervals between starts and age of last Worker finish.

## Next test

Install 0.1.10 over 0.1.9. No reboot is required.

Use the phone normally and leave Guardian installed for 2–3 hours if convenient, then export daily + diagnostic JSON.
