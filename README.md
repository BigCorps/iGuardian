# Guardian DEV — Android 0.1.16 — App Trends AutoTest

This build intentionally supersedes the failed 0.1.15 build.

## Action #39 fix

The 0.1.15 source accidentally left generic placeholders (`intent`, `period`,
`currentLabel`, `previousLabel`) inside the dedicated 24-hour comparison
method. Kotlin compilation therefore failed before tests could run.

0.1.16 fixes that exact method and keeps the generic history guard only in
the generic comparison path.

## Product advance in the same build

### Per-app trend engine v1

The Guardian can now compare application usage between:
- last 24h vs previous 24h
- last 7d vs previous 7d

It reports only meaningful changes of at least 1 minute.

When history is mature it can show up to:
- top 3 increases
- top 3 decreases

When history is incomplete it shows `histórico insuficiente` instead of
inventing a delta.

### Automatic dashboard

The existing automatic insights card now also shows per-app trends for:
- 24h
- 7d

No manual question is required.

### AutoTest Suite v5

The suite now validates the app-trend engine itself:
- readiness must match report history/coverage
- incomplete trends must contain no deltas
- increases must be positive
- decreases must be negative
- ordering must be deterministic

### Validation pack v5

The one-file validation export now includes:
- `app_trends.last_24h`
- `app_trends.last_7d`

with readiness, history percentages, coverage and app deltas.

## Existing protections kept

- history guard across 24h/7d/calendar comparisons
- report schema v4
- DB v7 system cleanup
- Xiaomi Wallpaper Carousel classified as SYSTEM
- no INTERNET
- no QUERY_ALL_PACKAGES
- no backend/cloud
- WorkManager unchanged

## Test flow

Upload this package.

After GitHub Actions passes, install directly over 0.1.14.
There is no 0.1.15 APK to install because its Action failed.

Use normally for ~60–90 minutes and export only the recommended
`guardian-validacao-*.json`.

No question-by-question testing is required.
