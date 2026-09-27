# Validation — Android 0.1.19

## 0.1.18 physical baseline

- compact_success ~26 KB
- ValidationSuite v7
- 14 PASS / 1 WARN / 0 FAIL
- critical_passed=true
- lineage PASS
- 1232 clean timeline intervals
- coverage 99.1%
- WorkManager active unique work; 3 historical stops after reboot, last reason 13, followed by successful attempt and no failure/retry

## 0.1.19 acceptance

CI:
- privacy/project guard allows Accessibility only in Guardian Web
- browser sanitizer/private-mode unit tests PASS
- final APK has no INTERNET/QUERY_ALL and contains the protected Guardian Web service
- validation lineage hashes PASS

Runtime:
- report schema v5
- DB v8
- diagnostic v18
- ValidationSuite v8
- validation pack v8
- `guardian_web_privacy_contract=PASS`
- `guardian_web_physical_validation=PASS` after normal + incognito Chrome Dev test
- scheduler PASS if old stops are followed by a successful completion
- no FAIL
