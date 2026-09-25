# Validation — Android 0.1.9

## 0.1.8 result

PASS:
- 0.1.8 / versionCode 9 installed in place.
- Usage Access/history/device name preserved.
- WorkManager unique periodic work present.
- BOOT_COMPLETED captured.
- background work executed after reboot.
- next periodic run occurred about 30 minutes later.
- no extra enqueue/duplicate unique work.
- no WorkManager retry/failure recorded by Guardian's result path.
- privacy/timeline invariants remained clean.

OBSERVATION:
A third Worker start occurred ~21 seconds after the second, with attempt=1 and the same unique work UUID. This is consistent with WorkManager re-attempting work after a stop/process event, but 0.1.8 did not export stopReason.

## 0.1.9 acceptance

Background:
- unique periodic work remains present;
- worker_stopped_count / stop_reason explain any attempt>0;
- no second unique work UUID is created;
- collector integrity remains duplicate-safe.

System cleanup:
- Photo Picker and Xiaomi App Finder no longer appear as APP.

Local intelligence:
- last 24h queries work;
- deterministic insights match report data;
- queries remain absent from exports/logs.

Core:
- no timeline overlap;
- no PRIVATE/SYSTEM identity;
- no INTERNET / QUERY_ALL_PACKAGES.
