# Guardian DEV — Android 0.1.3

## PROJECT STATUS

- **Current version:** Android 0.1.3
- **Current phase:** Android Foundation / privacy correction from first real diagnostic
- **Repository:** `BigCorps/iGuardian`
- **Backend / cloud / login / external AI:** none
- **Internet permission:** intentionally absent
- **DEV package:** `com.bigcorps.guardian.dev`

## What the first real diagnostic proved

The 0.1.2 export path is working: real JSON files were successfully exported and shared.

It also revealed a critical classifier problem:
- Nubank package `com.nu.production` was stored as APP.
- InfinitePay package `io.cloudwalk.infinitepaydash` was stored as APP.
- `PRIVATE` count was zero.

This violated the intended privacy model even though no content, password or screen data was captured.

## 0.1.3 privacy correction

The automatic privacy classifier now evaluates:
1. exact known sensitive packages;
2. package-name tokens;
3. the app's visible label, resolved only in memory.

For a PRIVATE app, label and package are not written to SQLite.

The first confirmed false negatives are explicitly covered:
- `com.nu.production`
- `io.cloudwalk.infinitepaydash`

The visible labels `Nubank` and `InfinitePay` are also recognized, which makes the classifier more robust when package names do not reveal that an app is financial.

### Existing local history repair

On first launch of classifier v2:
- Guardian scans existing APP rows locally;
- rows now recognized as sensitive are converted to `PRIVATE`;
- package and label are erased from those rows;
- generated local report JSON files are deleted and today's report is regenerated from the repaired SQLite data;
- the migration version is stored so it runs only once.

This is necessary because privacy must be corrected at rest, not only for future events.

## Scheduler finding

The first diagnostic also reported `periodic_job_scheduled = false`.

0.1.3 changes scheduling to:
- retry each time the Guardian process starts;
- capture JobScheduler result and whether the job actually became pending;
- write a sanitized `JOB_SCHEDULE` technical event for the next diagnostic.

This will tell us whether the Xiaomi/Android 16 device accepts the periodic job or whether an OEM-specific fallback is needed.

## Still to validate

The first data sample was only a few minutes long and did not produce:
- SCREEN_OFF intervals;
- unlock events;
- user-switch PRIVATE intervals.

Do not conclude yet that those features are broken. They need a deliberate test after 0.1.3 is installed.

## Locked principles

- Privacy Engine before storage.
- Protected app identity never stored.
- One generic `PRIVATE` state for protected contexts.
- No screenshots/video.
- No keyboard/input capture.
- No notification/message content.
- No INTERNET permission in the local-only MVP.
- `ANONYMOUS_BROWSER` is distinct but never fabricated.
- Future browser data stores only main/registrable domain, never full URL.

## 0.1.3 test sequence

1. Install/update to 0.1.3.
2. Open **Revisar apps privados**.
3. Confirm Nubank and InfinitePay show **Protegido automaticamente**.
4. Open a normal app for ~30 seconds.
5. Open Nubank for ~15 seconds.
6. Open InfinitePay for ~15 seconds.
7. Open Android Settings for ~15 seconds.
8. Lock the screen for ~20 seconds.
9. Unlock and use another normal app.
10. Return to Guardian and tap **Atualizar dados locais**.
11. Export both JSONs and share them back.

Expected:
- Nubank and InfinitePay must NOT appear by package/name anywhere in the new report.
- Their intervals must contribute only to PRIVATE.
- Settings must contribute only to PRIVATE.
- exported diagnostic should show privacy classifier version 2 and repair version 2.
- scheduler status/technical event should tell us whether periodic background collection is accepted.
- screen-off/unlock should now be evaluable from the deliberate test.

## Next milestone

Only after this privacy test passes:
- refine transient system surfaces;
- improve screen/off and reboot/day-boundary handling if needed;
- daily/weekly local summaries;
- deterministic local question engine.
