# Guardian DEV — Android 0.1.2

Codinome temporário do futuro produto BigCorps hoje chamado de **iGuardian / Guardian**.

## PROJECT STATUS

- **Current version:** Android 0.1.2
- **Current phase:** Phase 1 — Android Foundation / physical-device validation
- **Repository:** `BigCorps/iGuardian`
- **Backend / cloud / login / external AI:** none
- **Internet permission:** intentionally absent
- **DEV package:** `com.bigcorps.guardian.dev`
- **CI:** GitHub Actions run #6 successfully built Android 0.1.1.

## Why 0.1.2 exists

The second physical-device test proved that the 0.1.1 exporter could still report the expected payload size while the selected Android document provider left a zero-byte destination file.

The old success toast could therefore be misleading when provider metadata did not return a real size.

### Export architecture changed

On Android 10+:

1. Guardian inserts the JSON directly into `MediaStore.Downloads`.
2. Destination is `Downloads/iGuardian`.
3. Guardian writes the exact UTF-8 bytes.
4. Guardian reopens the created destination.
5. Guardian reads every byte back.
6. Success is shown only when the read-back bytes exactly equal the generated payload.
7. The user can immediately share the verified file through Android's share sheet.

No storage permission is required for files the app itself creates in `MediaStore.Downloads` on Android 10+.

On Android 9 and lower, Storage Access Framework remains the fallback, but now it also performs byte-for-byte read-back verification before reporting success.

## Restricted settings during GitHub APK testing

`PACKAGE_USAGE_STATS` is a special access. Android can block this access for APKs installed manually.

This is Android security behavior, not a normal app permission dialog. For the DEV APK:

1. Guardian > **1. Abrir Informações do app**.
2. Remain on the main **Informações do app** screen.
3. Tap `⋮`.
4. Choose **Permitir configurações restritas**.
5. Return to Guardian.
6. Tap **2. Conceder acesso de uso**.

Do not use the ordinary **Permissões do app** screen for this step. It can correctly show no normal permissions even though Guardian declares Usage Access.

The app cannot and must not bypass this Android protection programmatically.

## Locked product principles

- Local-first.
- Privacy Engine before storage.
- No screenshots/video.
- No keyboard/input capture.
- No notification/message content.
- No INTERNET permission in the local-only MVP.
- `PRIVATE` hides the reason/package for protected contexts.
- `ANONYMOUS_BROWSER` remains a distinct schema type but is never fabricated.
- Future browser storage: registrable/main domain only, never full URL.
- Diagnostics are user-exported manually and privacy-safe.

## Current implemented foundation

- UsageStats collector.
- SQLite local timeline.
- Daily reports.
- `APP`, `PRIVATE`, `SCREEN_OFF` timeline.
- reserved `ANONYMOUS_BROWSER`.
- local device name.
- private-app picker.
- background best-effort JobScheduler.
- local diagnostic generator.
- robust direct Downloads export + read-back verification.
- share verified JSON via Android share sheet.
- CI privacy verification.
- GitHub Actions APK generation.

## Next physical test

1. Install 0.1.2.
2. If Usage Access is blocked, perform the restricted-settings flow above.
3. Confirm Guardian shows `Acesso de uso autorizado`.
4. Use several normal apps.
5. Open Android Settings.
6. Lock/unlock the phone.
7. Tap **Atualizar dados locais**.
8. Export diagnostic JSON.
9. Guardian should say `JSON salvo e verificado`.
10. Use **Compartilhar** in that dialog and send the file to ChatGPT, or select it from `Downloads/iGuardian`.

If byte-for-byte verification fails, Guardian must report an error instead of claiming a successful export.

## Next milestone after this test

- inspect real diagnostic JSON;
- validate timeline accuracy;
- refine OEM/background behavior;
- then begin daily/weekly summaries and the deterministic local question engine.

See `PROJECT_STATE.json`, `ROADMAP.md`, `PRIVACY_MODEL.md` and `DATA_SCHEMA.md`.
