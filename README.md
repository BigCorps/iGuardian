# Guardian DEV — Android 0.1.7

## PROJECT STATUS

- **Current version:** Android 0.1.7
- **Phase:** Android Foundation gate + Local Intelligence Alpha
- **Repository:** `BigCorps/iGuardian`
- **Backend / cloud / login / external AI:** none
- **Internet permission:** intentionally absent
- **DEV package:** `com.bigcorps.guardian.dev`
- **Daily JSON schema:** v2
- **Diagnostic schema:** v5
- **SQLite DB:** v4
- **Scheduler logic:** v4

## 0.1.6 physical result

The 0.1.6 JSONs proved several corrections:

- actual installed build is 0.1.6 / versionCode 7;
- Usage Access remains active;
- local history still begins at 0.1.4/0.1.5 tracking start;
- today's coverage is 99.8%;
- the 24-hour diagnostic snapshot coverage is 99.4%;
- no INTERNET or QUERY_ALL_PACKAGES permission;
- no sensitive bank/settings identity appeared in the public day report;
- no non-APP interval exposed package/name;
- no timeline overlaps were found;
- 0.1.6 system cleanup removed known package-installer/system packages from the user app aggregate after upgrade;
- export single-flight produced successful new exports and no new post-0.1.6 export error.

The remaining issue is background scheduling.

### Scheduler v3 finding

In approximately one hour:
- process started once;
- one schedule/recovery was created;
- the job ran once immediately after scheduling;
- one hour later there was no managed periodic job and no second run.

This shows the periodic model is still not robust enough on the tested Xiaomi/Android 16 device.

## 0.1.7 scheduler v4

The DEV scheduler now uses an alternating **chained one-shot** strategy instead of `setPeriodic()`.

Two job IDs alternate:

`41001 → 41002 → 41001 ...`

Each next job:
- is persisted across reboot;
- cannot run before 30 minutes;
- has a DEV deadline window of 45 minutes;
- is scheduled by the finishing job before that job reports completion.

This avoids replacing a currently running job with the same ID.

Process-start recovery still exists, but it now checks all Guardian-managed jobs and waits 8 seconds before deciding the chain was lost.

Android 16 diagnostics also export the raw `getPendingJobReasons()` values for both job IDs.

## Reboot diagnostics

Boot/package-replaced/user-unlocked signals are persisted in scheduler diagnostics:
- signal count;
- last signal;
- timestamp.

This lets one test validate update + background + reboot recovery together.

## Local Intelligence Alpha

0.1.7 also begins Phase 2 without waiting for another round.

A new **Perguntar ao Guardian** card answers deterministic questions locally, for example:

- Qual app mais usei hoje?
- Top 5 apps
- Quanto tempo usei o WhatsApp?
- Quantas vezes desbloqueei?
- Quanto tempo a tela ficou desligada?
- Quanto tempo ficou PRIVATE?
- Qual a cobertura de hoje?
- Resumo de hoje

Important:
- no API;
- no Internet;
- no LLM call;
- no hallucinated numbers;
- questions are not stored;
- answers use the sanitized local report only;
- periods other than today explicitly return “not supported yet”.

See `LOCAL_INTELLIGENCE.md`.

## Combined next test

Install 0.1.7 directly over 0.1.6. Do not uninstall.

### Immediately
Confirm:
- version shows 0.1.7;
- device name/history remain;
- Usage Access remains enabled.

Ask at least these:
1. `Resumo de hoje`
2. `Qual app mais usei hoje?`
3. `Quanto tempo usei o Chrome Dev?`
4. `Quantas vezes desbloqueei?`
5. `Qual a cobertura de hoje?`
6. `Qual app mais usei ontem?`

The first five must return exact local calculations. The last one must state that older periods are not supported yet.

### Background + reboot
1. Close Guardian.
2. Leave it mostly closed for about 35–45 minutes.
3. Reboot the phone once.
4. After reboot, do not open Guardian immediately.
5. Leave another 45–60 minutes.
6. Open Guardian, ask `Resumo de hoje` again.
7. Export daily + diagnostic JSON.

This single round validates:
- another in-place update;
- local Q&A;
- first chained job;
- reboot persistence/receiver;
- post-reboot chained job;
- scheduler pending reasons;
- no job bursts/stops;
- JSON/export after reboot.

If scheduler v4 + reboot pass, the Android foundation can move out of the critical blocking path and Phase 2 can expand while OEM hardening continues in parallel.
