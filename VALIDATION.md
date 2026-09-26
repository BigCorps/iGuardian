# Validation — Android 0.1.15

## 0.1.14 physical result

PASS:
- permissions/privacy contract
- report schema v4
- history availability math
- report totals
- timeline privacy/overlap
- app aggregate consistency
- Local Intelligence self-check
- product capabilities v7
- system surface exclusion
- WorkManager
- tracking coverage 99.4%

FAIL:
- comparison_history_guard
  - guard24=true / expected24=true
  - guard7=false / expected7=true

Data maturity:
- current 24h 100.0%
- previous 24h 92.0%
- current 7d 27.4%
- previous 7d 0.0%

## 0.1.15 acceptance

- version 0.1.15 / code 16
- diagnostic schema 14
- DB 7
- engine v8
- classifier v5
- ValidationSuite v4
- validation pack v4
- comparison_history_guard PASS for 24h/7d/calendar
- Wallpaper Carousel absent from APP aggregate
- existing privacy/report/WorkManager checks remain green
