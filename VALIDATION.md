# Validation — Android 0.1.10

## 0.1.9 physical result

PASS: 0.1.9/versionCode 10; WorkManager unique work present; run/success count 5; retry/failure/stopped 0; current WorkInfo ENQUEUED; privacy/data invariants clean; today coverage 99.4%; 24h coverage 99.3%; system-surface cleanup passed.

OBSERVATION: Worker execution is not exact-periodic on this Xiaomi. Accepted as best-effort because collection catches up retrospectively.

## 0.1.10 acceptance

- report schema_version 3;
- millisecond fields present;
- diagnostic self-check passed=true;
- same unique WorkManager remains active;
- recent Worker-start history begins populating;
- no privacy regression.
