# Static validation — 0.1.24 Hybrid v3 + Bank Mode

Base runtime files match repository commit `090f339246f5c15a8edeffd83c9f5b9e77a3155e` before this patch.

## Actions #63 diagnosis

- failing step: privacy/project guard;
- failure occurred before validation lineage, signing, unit tests, Kotlin compile and APK build;
- repository still contained the old substring-based AccessibilityService guard;
- corrected guard uses real Android accessibilityservice imports and retains the narrow allowlist.

## Bank Mode structural checks

- `BrowserAccessibilityService.requestBankModeDisable()` present;
- real Android `disableSelf()` call present;
- current browser interval is stopped before disabling;
- Hybrid host/private state is reset;
- `WEB_BANK_MODE_DISABLE` technical event contains no banking content;
- Guardian Web UI confirms AccessibilityManager=false AND secureSetting=false before reporting success;
- recent heartbeat alone is deliberately not accepted as proof that Bank Mode is active;
- reactivation is explicit/user-controlled.

## Local compile validation

Modified BrowserAccessibilityService + BrowserWebActivity compile successfully in the existing Hybrid-v3 Android structural harness, including `disableSelf()`, main Looper/Handler and the Bank Mode verification loop. Only stub unused-parameter warnings were produced.

## Unchanged privacy contract

- host-only web persistence;
- no raw screenshot/OCR/tree text persistence;
- no INTERNET;
- no ACCESS_NETWORK_STATE;
- no QUERY_ALL_PACKAGES;
- no database schema expansion.

Authoritative Android SDK/Gradle/APK/merged-manifest validation remains GitHub Actions.
