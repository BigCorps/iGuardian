# Data Schema — report v3 / DB v5 / diagnostic v8

Report v3 keeps v2 fields and adds millisecond precision to timeline intervals, app aggregates, summaries and tracking. Coverage is calculated from millisecond totals.

Only APP may carry package/name. PRIVATE/SYSTEM/SCREEN_OFF remain identity-free.

Diagnostic v8 adds WorkManager cadence telemetry and Local Intelligence runtime self-check. No user query history is stored.
