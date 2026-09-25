# Data Schema — export v2 / DB v4 / diagnostic v6

Timeline schema is unchanged:
APP | PRIVATE | SCREEN_OFF | SYSTEM | ANONYMOUS_BROWSER

Only APP may carry package/name.

Diagnostic v6 adds WorkManager state/telemetry.

Local Intelligence v2 creates no new persistent query/history table; answers are calculated from the existing sanitized timeline.
