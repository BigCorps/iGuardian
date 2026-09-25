# Validation — Android 0.1.7

## 0.1.6 evidence

Latest diagnostic:
- version 0.1.6 / versionCode 7;
- diagnostic schema 4;
- Usage Access true;
- no Internet permission;
- no QUERY_ALL_PACKAGES;
- today coverage 99.8%;
- 24h snapshot coverage 99.4%;
- no public sensitive-app identity leak detected;
- no non-APP identity fields;
- no timeline overlap detected;
- known system-installer packages absent from current app aggregate;
- no new post-0.1.6 export failure.

Scheduler logic v3:
- one process start;
- one recovery/schedule;
- one job run;
- zero job stops;
- no second run after approximately one hour;
- managed periodic job absent at diagnostic time.

Conclusion:
privacy/data pipeline is strong; periodic background scheduling remains the active foundation issue.

## Why scheduler v4 uses chained one-shots

Official Android JobScheduler supports one-time jobs with minimum latency and persisted jobs. 0.1.7 alternates two IDs so scheduling the next job cannot replace the currently running one.

## 0.1.7 acceptance

### Update
- installs over 0.1.6;
- name, permission, tracking start and history preserved.

### Local intelligence
Queries return values consistent with exported daily JSON.
No raw question appears in SQLite/diagnostic/export.

### Scheduler
After install and a reboot:
- `scheduler.logic_version = 4`
- `scheduler.mode = chained_one_shot`
- managed job exists between runs;
- `job_run_count >= 2` over a sufficient test window;
- `job_stop_count = 0` ideally;
- schedule attempts grow roughly one per completed chained job, not in bursts;
- alternate job IDs are visible;
- next target/deadline are populated;
- Android 16 pending reasons are captured.

### Reboot
Diagnostic must show a persisted reschedule signal such as BOOT_COMPLETED or USER_UNLOCKED and the chain must continue afterward.
