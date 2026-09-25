# Validation — Android 0.1.6

## Long 0.1.5 real-device evidence

Diagnostic generated 2026-09-25 09:03:33 on Xiaomi/Redmi Android 16 / API 36.

Build actually under test:
- version 0.1.5
- versionCode 6
- diagnostic schema 3
- scheduler logic 2

Observed:
- Usage Access: true
- no INTERNET permission
- no QUERY_ALL_PACKAGES
- tracking start preserved from 2026-09-24 18:41:20
- today's coverage: 99.8%
- 24 scheduler job runs total in logic v2
- 0 scheduler job stops
- recent overnight cadence roughly 27–35 minutes
- timeline overlaps: 0

## Fresh other-user/profile validation

Technical events:
- PRIVATE_STARTED: 09:01:57.624
- PRIVATE_ENDED: 09:02:38.664

Daily timeline:
- generic PRIVATE interval from 09:01:57.624 to 09:02:38.664
- no guest application/package identity inside the interval
- no overlap with neighboring timeline entries

Result: privacy boundary PASSED for the current Android approach.

## Export observation

One `EXPORT_PREPARE_ERROR: IOException` appeared immediately after an `EXPORT_OK` during rapid export activity. The final daily and diagnostic files were both successfully produced.

0.1.6 adds single-flight export protection to prevent overlapping requests.

## 0.1.6 acceptance

The next files must prove the new build was actually installed:
- app.version = 0.1.6
- app.version_code = 7
- diagnostic_schema = 4
- scheduler.logic_version = 3

Then validate for 60–90 minutes:
- periodic jobs continue without bursts;
- job_stop_count remains 0;
- process-start guard records skips instead of false recoveries;
- schedule_attempt_count does not grow merely because JobScheduler launched the process;
- historical package installers/DocumentsUI no longer pollute the app ranking;
- zero-second app artifacts disappear;
- export succeeds without overlapping-export IOException.
