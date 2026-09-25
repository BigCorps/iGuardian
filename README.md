# Guardian DEV — Android 0.1.9

## What 0.1.8 proved

The WorkManager migration passed the main background/reboot test.

Diagnostic at 14:59 showed:
- 0.1.8 / versionCode 9;
- scheduler logic 5;
- WorkManager 2.12.0;
- unique periodic work still ENQUEUED;
- one unique work UUID;
- reboot signal BOOT_COMPLETED captured;
- three worker starts and three local collector completions;
- no recorded worker retry/failure in Guardian telemetry;
- next schedule remained populated;
- legacy JobScheduler IDs were cancelled.

The first run after reboot occurred at 14:18. The next periodic run began about 30 minutes later at 14:49.

One additional WorkManager attempt started about 21 seconds later with attempt=1. It used the same unique work UUID and did not create a second enqueue. Data integrity was protected by the serialized/cursor-based collector, but 0.1.9 adds stop-reason telemetry so the next diagnostic can identify why WorkManager retried that attempt.

## Data/privacy

0.1.8 retained:
- 99.5% coverage today;
- 99.3% 24h snapshot coverage;
- no timeline overlap found;
- no PRIVATE/SYSTEM identity leak found;
- no sensitive bank/settings identity found.

Two tiny OEM/system surfaces still appeared as user apps:
- Xiaomi Localizador de apps;
- Android/Google Photo Picker.

0.1.9 moves them to sanitized SYSTEM and DB v5 repairs the historical rows.

## WorkManager telemetry

0.1.9 keeps scheduler logic 5. It does not reset the counters, so the next test extends the same WorkManager observation window.

New telemetry:
- WorkInfo.stopReason;
- Worker class name;
- onStopped count;
- last stop reason;
- stopped attempt number.

This is diagnostic only; scheduling behavior itself is unchanged.

## Local Intelligence v3

New:
- exact rolling last 24 hours;
- deterministic factual insights.

Examples:
- `Top 5 das últimas 24 horas`
- `Insights de hoje`
- `Insights de ontem`
- `Insights dos últimos 7 dias`

Insights remain descriptive only:
- leading app and share of app-use time;
- longest continuous app session;
- longest continuous screen-off interval;
- coverage.

No behavioral judgment, score or health claim is produced.

## Combined validation

Install 0.1.9 over 0.1.8 without uninstalling.

Immediately try:
1. `Top 5 das últimas 24 horas`
2. `Insights de hoje`
3. `Insights de ontem`
4. `Compare hoje com ontem`

Then use the phone normally and leave Guardian mostly closed.

For background validation, 60–90 minutes is sufficient. A reboot is optional this time because BOOT_COMPLETED + WorkManager persistence already passed in 0.1.8.

At the end export daily + diagnostic JSON.

The next diagnostic should let us distinguish a normal WorkManager/system stop from an app-generated retry if another attempt=1 occurs.
