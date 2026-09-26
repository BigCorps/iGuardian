# Guardian DEV — Android 0.1.15 — Complete History Guard

## 0.1.14 physical result

The 0.1.14 validation pack proved that the automated validation strategy is
working: it caught one real semantic regression without manual reproduction.

Result:
- 11 PASS
- 0 WARN
- 1 FAIL
- only failure: `comparison_history_guard`

24h behaved correctly:
- current 24h history availability: 100.0%
- previous 24h: 92.0%
- Guardian blocked the comparison.

7d did not:
- current 7d availability: 27.4%
- previous 7d: 0.0%
- Guardian still produced a numeric delta.

Root cause:
the 24h and today/yesterday methods had explicit maturity guards, while the
generic comparison path used by 7d/calendar comparisons did not.

## 0.1.15

### Local Intelligence v8

History readiness is now enforced centrally on the generic comparison path.

This protects:
- last 7d vs previous 7d
- calendar day vs calendar day
- calendar range vs calendar range

The engine refuses a delta until both compared periods have:
- >= 99% requested-history availability
- >= 90% classified-data coverage

### AutoTest Suite v4

The history-guard check now validates:
- 24h
- 7d
- calendar comparison using dates guaranteed to precede tracking start

A single validation pack remains sufficient.

### System cleanup

`com.miui.android.fashiongallery` is the Xiaomi lock-screen Wallpaper Carousel.
It is now classified as SYSTEM rather than APP.

DB v7 repairs already stored rows and removes their identity.

### Unchanged

- no INTERNET
- no QUERY_ALL_PACKAGES
- no backend/cloud
- WorkManager scheduling unchanged
- report schema remains v4

## Test

Install directly over 0.1.14.

No manual question testing is needed.

Use normally for ~60–90 minutes and export one
`guardian-validacao-*.json`.

Expected result:
`critical_passed=true` and `manual_test_required=false`.
