# Guardian DEV — Android 0.1.6

## PROJECT STATUS

- **Current code to upload:** Android 0.1.6
- **Last physically validated build:** Android 0.1.5
- **Repository currently observed before this upload:** Android 0.1.5
- **Current phase:** Android Foundation / background hardening
- **Backend / cloud / login / external AI:** none
- **Internet permission:** intentionally absent
- **DEV package:** `com.bigcorps.guardian.dev`
- **Daily JSON schema:** v2
- **Diagnostic schema:** v4
- **SQLite DB:** v4

## Long 0.1.5 validation — 2026-09-25

The long test was still running Android 0.1.5 (versionCode 6). It was valuable and validated the current foundation before 0.1.6 is installed.

Confirmed:
- update-in-place from 0.1.4 to 0.1.5 preserved state/history;
- device name and Usage Access remained preserved;
- tracking start remained from the previous build;
- daily coverage reached 99.8% from midnight to 09:03;
- 24 periodic jobs completed with 0 job stops in scheduler logic v2;
- the visible overnight cadence was approximately 27–35 minutes, centered near the 30-minute DEV period;
- timeline export had no overlaps;
- fresh other-user/profile switch was detected;
- the other-user interval was stored generically as PRIVATE;
- no guest app/package identity appeared inside the protected interval.

Fresh switch boundary:
- PRIVATE_STARTED: 09:01:57.624
- PRIVATE_ENDED: 09:02:38.664
- exported interval: PRIVATE for approximately 41 seconds.

This closes the main privacy-boundary question for the current Android approach.

## Why 0.1.6 is still needed

The uploaded diagnostic is 0.1.5 / scheduler logic v2, so scheduler logic v3 has not yet been physically tested.

0.1.5 also showed:
- `pending_now=false` shortly after a successful periodic run;
- two historical process-start recoveries from logic v2;
- old SYSTEM/package-installer rows still present as APP in historical data;
- zero-second app transition artifacts;
- one export preparation IOException immediately after a successful export, consistent with overlapping export requests.

## 0.1.6 changes

### Scheduler logic v3
When Android starts a dead Guardian process to execute JobScheduler, Application.onCreate() may run before JobService.onStartJob(). A short grace window prevents this normal lifecycle from being mistaken for a missing periodic job.

### Historical SYSTEM sanitation
DB v4 converts known old technical APP rows to sanitized SYSTEM and clears identity.

### Millisecond-first aggregation
Durations are summed in milliseconds and converted only after aggregation.

### Zero-second app cleanup
Sub-second transition artifacts are omitted from the user-facing app ranking.

### Export single-flight
Only one JSON export may run at a time. This avoids two rapid taps racing against MediaStore/file verification.

### Dynamic version label
The UI reads versionName from the installed APK instead of a hard-coded string.

## Next validation

1. Upload this 0.1.6 package to the repository preserving paths.
2. Wait for the signed Actions artifact `guardian-android-0.1.6-fixed-signed-debug`.
3. Install directly over 0.1.5. Do not uninstall.
4. Confirm the UI says 0.1.6 and the diagnostic reports:
   - version 0.1.6
   - versionCode 7
   - diagnostic_schema 4
   - scheduler logic_version 3
5. Leave mostly closed for about 60–90 minutes.
6. Export daily + diagnostic JSON.

The visitor-profile test does not need to be repeated immediately; the fresh 0.1.5 test already validated the privacy boundary.
