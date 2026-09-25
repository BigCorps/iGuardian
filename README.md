# Guardian DEV — Android 0.1.8

## Current direction

0.1.8 advances two tracks in one build:

1. background maintenance migrates from hand-written JobScheduler recurrence to **AndroidX WorkManager 2.12.0**;
2. local intelligence expands from today-only to **today, yesterday and the last 7 days**.

No cloud, login, external AI or Internet permission is added.

## 0.1.7 findings

The physical diagnostic confirms:
- version 0.1.7 / versionCode 8;
- diagnostic schema 5;
- Usage Access active;
- no INTERNET;
- no QUERY_ALL_PACKAGES;
- tracking history preserved;
- today coverage 99.5%;
- 24h snapshot coverage 99.3%;
- no timeline overlap;
- no PRIVATE/SYSTEM identity leak found in analysis.

Today at export:
- app use: 1h55m53s;
- screen off: 10h45m13s;
- PRIVATE: 4m11s;
- SYSTEM: 14m27s;
- unlocks: 51.

Top apps:
1. WhatsApp Business — 41m05s
2. Chrome Dev — 32m50s
3. ChatGPT — 15m50s
4. Claude — 8m23s
5. Manus — 5m30s

## Scheduler v4 conclusion

At 11:02 job 41001 was scheduled successfully.

At 13:03 it finally ran and scheduled 41002 with `present=true`.

Seconds later the diagnostic showed no managed IDs and Android 16 returned reason `-2` for both IDs, meaning those jobs did not exist anymore.

Therefore Guardian stops hand-building recurrence with JobScheduler.

This is not a data-loss blocker: UsageStats collection catches up retrospectively from the stored cursor whenever Guardian or background work next runs.

## Background v5 — WorkManager

Guardian now uses stable AndroidX WorkManager 2.12.0.

DEV schedule:
- unique periodic work;
- 30 minute repeat;
- 10 minute flex;
- one unique work chain only;
- WorkManager owns system/reboot rescheduling.

Migration:
- legacy JobScheduler IDs 41001/41002 are cancelled;
- legacy GuardianJobService is removed from the app manifest.

Diagnostics now include:
- WorkInfo state;
- work UUID;
- generation;
- run attempts;
- next eligible schedule;
- worker run/success/retry/failure counters;
- boot/update/user-unlock signals.

## Local Intelligence v2

Supported periods:
- today;
- yesterday;
- last 7 days.

Supported questions:
- summary;
- top app / top 5;
- named app usage;
- unlocks;
- screen-off;
- PRIVATE;
- SYSTEM;
- coverage;
- compare today with yesterday.

Examples:
- `Resumo de ontem`
- `Top 5 dos últimos 7 dias`
- `Quanto tempo usei o Chrome Dev ontem?`
- `Qual a cobertura de ontem?`
- `Compare hoje com ontem`

Questions are never persisted.

## Combined test

Install directly over 0.1.7; do not uninstall.

Immediately test:
1. Resumo de hoje
2. Resumo de ontem
3. Top 5 dos últimos 7 dias
4. Quanto tempo usei o Chrome Dev hoje?
5. Compare hoje com ontem

Then leave Guardian mostly closed for 60–90 minutes.

If convenient, reboot once during the period. At the end export daily + diagnostic JSON.

Success:
- scheduler logic 5;
- engine `androidx_workmanager`;
- periodic work ENQUEUED/RUNNING;
- worker executes without bursts;
- update preserves name/history/Usage Access;
- local answers agree with period reports.
