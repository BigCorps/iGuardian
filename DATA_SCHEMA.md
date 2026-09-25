# Data Schema — export v2 / DB v5 / diagnostic v7

Timeline:
APP | PRIVATE | SCREEN_OFF | SYSTEM | ANONYMOUS_BROWSER

Only APP may carry package/name.

DB v5:
- historical `com.google.android.photopicker` rows become SYSTEM;
- historical `com.mi.appfinder` rows become SYSTEM;
- package/label identity is cleared during migration.

Diagnostic v7 adds WorkManager stop telemetry.

Local Intelligence v3 remains computed-only and creates no query history table.
