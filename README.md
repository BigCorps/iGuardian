# Guardian DEV — Android 0.1.0

Codinome temporário do futuro produto BigCorps hoje chamado de **iGuardian.me / Guardian**. Nome, logo e domínio definitivos ainda não estão congelados.

## PROJECT STATUS

- **Current version:** Android 0.1.0
- **Current phase:** Phase 1 — Android Foundation
- **Build model:** GitHub + GitHub Actions
- **Backend:** none
- **Cloud:** none
- **Login:** none
- **External AI API:** none
- **Internet permission:** intentionally absent
- **Primary package (DEV):** `com.bigcorps.guardian.dev`
- **CI status:** first SDK setup issue fixed; second run reached Kotlin compilation. Build Fix 02 corrects the Kotlin escape error in `GuardianDatabase.kt`.

### Implemented in 0.1.0

- Android native project in Kotlin using Android framework APIs only.
- Usage Access onboarding and permission check.
- Tracking baseline starts from the user's authorization flow; no silent pre-consent 24-hour backfill.
- Local UsageStats event collector.
- Local SQLite timeline database.
- Periodic best-effort collection with JobScheduler.
- Privacy Engine before storage.
- `PRIVATE` intervals for protected contexts.
- User-switch signal support through `ACTION_USER_BACKGROUND` / `ACTION_USER_FOREGROUND` while the process is alive.
- Screen non-interactive intervals as `SCREEN_OFF` (UsageStats on API 28+, runtime best-effort fallback on API 26–27).
- Automatic local daily JSON reports under the app's private `reports/YYYY/MM/YYYY-MM-DD.json` storage.
- Manual daily JSON export through Android's document picker.
- Local support diagnostic JSON.
- Manual selection of additional private apps from launchable apps.
- Device/runtime diagnostics.
- GitHub Actions workflow producing a debug APK artifact.
- Automated privacy invariant check in CI.

### Explicitly NOT implemented yet

- Supabase, Vercel or any server.
- Accounts/login.
- Payments.
- Remote monitoring.
- Automatic device-to-device sharing.
- OpenAI or other external AI APIs.
- On-device LLM.
- Accessibility Service.
- VPN traffic inspection.
- Screen capture, screenshots or video recording.
- Keyboard/input capture.
- Notification/message contents.
- Full browser URLs.
- Browser domains on Android.
- Reliable Android incognito/private-tab detection.
- `QUERY_ALL_PACKAGES`.

## Locked product principles

1. **Local first.** User history stays on the device unless the user manually exports it.
2. **Privacy Engine precedes storage.** Protected data must be discarded/sanitized before SQLite/JSON, never cleaned afterward.
3. **No content capture.** The app records metadata/time, not screen content, typed text, passwords, messages or notification contents.
4. **One generic `PRIVATE` state.** Banks, system Settings and another Android user/profile are not differentiated in stored data.
5. **`ANONYMOUS_BROWSER` is distinct from `PRIVATE`.** It has its own schema type, but Android 0.1.0 does not claim to detect it automatically because UsageStats does not provide a reliable incognito signal.
6. **Browser data, when added later, stores only the registrable/main domain**, never full URLs, paths, queries or tokens.
7. **No fake telemetry.** If Android cannot prove an event, Guardian does not infer it as fact.
8. **Support diagnostics remain privacy-safe.** Diagnostics expose health/capability/error information, not private app names or private reasons.

## How data flows

```text
Android UsageStats / system signals
            |
            v
       Privacy Engine
            |
       +----+------------------+
       |                       |
    PRIVATE?                 NORMAL
       |                       |
strip identifying          app metadata
fields BEFORE DB              |
       +-----------+-----------+
                   v
               SQLite
                   |
          daily aggregation
                   |
          local JSON report
```

## Build using GitHub Actions

1. Upload this folder to a GitHub repository.
2. Use `main` as the branch.
3. Open **Actions > Android APK > Run workflow** (or push to `main`).
4. The workflow installs Java 17, Android API 36 and Gradle 9.6, runs tests, verifies privacy invariants and builds the APK.
5. Download the `guardian-android-0.1.0-debug` artifact.

The workflow uses Android Gradle Plugin 9.4.0, Gradle 9.6.0 and JDK 17.

### CI fixes already applied

- Build Fix 01: stopped requesting the obsolete Android SDK package `tools`; the SDK setup now completes successfully.
- Build Fix 02: corrected an invalid Kotlin string escape in `GuardianDatabase.kt` (`\-` inside a regular Kotlin string). The regex now uses a literal hyphen at the end of the character class.

## First real-device test

1. Install the debug APK.
2. Open Guardian.
3. Open **Escolher apps privados**, review the installed launchable apps and tap **Concluir revisão de privacidade**.
4. Tap **Conceder acesso de uso** and authorize Guardian in Android's Usage Access screen.
5. Use a few normal apps.
6. Open Android Settings for a while.
7. Lock/unlock the screen.
8. If the device supports multiple users, switch away and return.
9. Return to Guardian and tap **Atualizar agora**.
10. Export **JSON do dia** and **JSON de diagnóstico**.

Expected behavior:

- normal apps may appear by app/package and duration;
- Settings must become only `PRIVATE` time and must not be stored by name;
- a detected user-switch interval must also become only `PRIVATE`;
- screen-off time must be separate;
- diagnostic JSON must not disclose why a `PRIVATE` interval happened;
- incognito browser time is **not automatically detectable in this release** and therefore must never be fabricated.

## Support JSON

The support export is intentionally separate from the user's daily activity report. It contains build/device/API/permission/collector/database/capability information, sanitized technical events, and a 24-hour processed activity snapshot so BigCorps can diagnose timing/OEM issues. Normal APP names/packages may therefore appear when the user explicitly exports this support file; PRIVATE intervals still contain no app, reason or content. Nothing is sent automatically.

## Compatibility strategy

Physical testing on every Android model is not a release gate. Guardian targets broad compatibility through official Android APIs, API-level guards, graceful fallbacks and sanitized support diagnostics. Available real devices are tested before release; device-specific problems discovered later are handled through BigCorps support and subsequent compatibility fixes.

## Next planned milestone — 0.2.0

- Improve first-run onboarding and explanations.
- Improve app-name resolution and private-app picker UX.
- Daily/weekly local summaries.
- More robust interval merging and reboot/day-boundary handling.
- Local question engine for deterministic questions such as top app, screen time, app comparison and trends.
- Expand diagnostics for OEM background restrictions without collecting private data.

See `PROJECT_STATE.json`, `ROADMAP.md`, `PRIVACY_MODEL.md` and `DATA_SCHEMA.md` before changing architecture.

### Password/login limitation in 0.1.0

Guardian deliberately does not inspect UI fields, Accessibility trees or keyboard input, so it cannot reliably detect that an arbitrary normal app has just opened a password/login form. The safe MVP rule is to protect the entire app through the private-app list. Browser login pages are likewise not detectable until a separate future browser-domain design is validated.
