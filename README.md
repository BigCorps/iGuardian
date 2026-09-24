# Guardian DEV — Android 0.1.5

## Status

0.1.5 is the first deliberate **in-place update test** over the fixed-signed 0.1.4.

Do NOT uninstall 0.1.4 before installing 0.1.5.

Expected preserved data:
- local device name;
- Usage Access authorization;
- privacy review/preferences;
- SQLite history;
- tracking start;
- daily reports.

## Findings from the 0.1.4 physical test

The 0.1.4 test proved:
- fixed-signed APK installed and runs;
- Usage Access active;
- JSON v2 export works;
- system surfaces separated from user app ranking;
- PRIVATE time visible in seconds/minutes;
- screen-off collection works;
- user/profile switch signal works and becomes PRIVATE;
- no guest-profile app identity appeared in exports.

It also exposed three correctness issues:

1. **Concurrent collectors.**
   JobService and Activity could read the same UsageStats range simultaneously. This inflated raw interval counts and duplicated at least one unlock event.

2. **Scheduler self-rescheduling.**
   `jobFinished()` was followed by a manual `ensureScheduled()`, and Activity resume also called `ensureScheduled()`. The test showed 12 job runs in about one hour, so this was not a clean 30-minute periodic-background result.

3. **PRIVATE / SCREEN_OFF overlap on user switch.**
   The visitor-profile test correctly raised PRIVATE, but a previously queued screen-off UsageStats interval overlapped part of that PRIVATE interval. Reports therefore needed deterministic precedence.

## 0.1.5 corrections

### Serialized collection
All `UsageCollector.collect()` calls share one process-wide lock.

### Scheduler logic v2
- periodic job remains owned by Android JobScheduler;
- no manual re-schedule after every job finish;
- no scheduler replacement on every Activity resume;
- scheduler counters reset once for logic v2 so the next diagnostic measures only the corrected behavior.

### Other-user/profile privacy boundary
When the owner user goes background:
- collect owner history only up to the switch timestamp;
- close the current interval;
- start generic PRIVATE.

When owner returns:
- close PRIVATE;
- advance the collector cursor to the return timestamp;
- do not replay UsageStats events from the other profile.

### Non-overlapping reports
Timeline normalization now uses precedence:

`PRIVATE > ANONYMOUS_BROWSER > SCREEN_OFF > SYSTEM > APP`

Every millisecond can belong to at most one exported timeline type. Unknown gaps are no longer bridged automatically.

### Data repair
SQLite DB v3 removes exact duplicate interval rows and duplicate unlock timestamps produced by the 0.1.4 concurrency bug.

### System noise cleanup
Additional system-only packages are mapped to sanitized SYSTEM:
- Google Play Services;
- Android/Google package installer;
- Xiaomi Security Center / system resource plugin.

### Visual safe area
The main ScrollView now clips content to system-bar padding so section headings/buttons do not scroll underneath the status/navigation bars.

## Test

Install 0.1.5 directly over 0.1.4.

First confirm:
- Android offers **Atualizar**, not uninstall/install;
- device name remains `ith cel2`;
- Usage Access remains authorized;
- previous history still exists.

Then leave Guardian mostly closed for at least 70 minutes and use the phone normally.

Afterward:
1. open Guardian;
2. export daily JSON;
3. export diagnostic JSON;
4. send both back.

For scheduler logic v2, approximately 1–3 runs in 70–90 minutes can be normal because Android may delay jobs. A rapid burst of many runs is not.

Also perform one visitor-profile switch again. In the new JSON there must be no overlapping PRIVATE/SCREEN_OFF segments and no identity from the visitor profile.
