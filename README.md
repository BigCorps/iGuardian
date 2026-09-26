# Guardian DEV — Android 0.1.11

## 0.1.10 validation result

The 0.1.10 long round ran for more than three hours and validated:

- version 0.1.10 / versionCode 11;
- report schema v3;
- millisecond precision;
- no INTERNET or QUERY_ALL_PACKAGES;
- WorkManager unique periodic work still present;
- same unique work UUID;
- 9 cumulative Worker runs;
- 9 successes;
- 0 retry;
- 0 failure;
- 0 stopped Worker;
- current WorkInfo ENQUEUED;
- data/privacy pipeline still clean.

The recorded Worker intervals remain irregular. This is expected for best-effort WorkManager scheduling on the tested Xiaomi device. Retrospective UsageStats catch-up remains the continuity mechanism.

## Self-check finding

The 0.1.10 runtime self-check passed:
- summary today;
- top last 24h;
- insights today.

Only `compare_today_yesterday` failed.

The comparison engine itself was not the failing part. The parser saw the word `ontem` and labeled the plan as YESTERDAY, while the self-check expected TODAY as the comparison reference. The answer route already bypassed that period for comparison.

0.1.11 fixes the plan semantics: comparison is explicitly a two-period intent with TODAY as the reference period.

## Local Intelligence v4

New flexible rolling periods:

- last N hours, 1–720;
- last N days, 1–90.

Examples:
- `Top 5 das últimas 6 horas`
- `Insights dos últimos 3 dias`
- `Quanto tempo usei o Chrome Dev nas últimas 2 horas?`
- `Qual a cobertura das últimas 12 horas?`

Existing fixed periods remain:
- today;
- yesterday;
- last 24 hours;
- last 7 days.

The runtime self-check now also verifies:
- custom 6-hour period;
- custom 3-day period;
- comparison semantics.

Questions remain local and are never persisted.

## Background

No WorkManager scheduling behavior was changed in 0.1.11.

That is deliberate: the current architecture is stable and duplicate-safe. Background exact periodicity is not a product guarantee.

## Next combined test

Install 0.1.11 directly over 0.1.10.

Test:
1. `Top 5 das últimas 6 horas`
2. `Insights dos últimos 3 dias`
3. `Quanto tempo usei o Chrome Dev nas últimas 2 horas?`
4. `Compare hoje com ontem`

Use normally for at least 60–90 minutes if convenient.

Then export daily + diagnostic JSON.

The diagnostic self-check should now be fully green and will validate custom rolling periods without storing any user query.
