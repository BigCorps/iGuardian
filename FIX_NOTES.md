# Guardian Android 0.1.24 — Actions #63 + Modo Banco

Current repository failure inspected on 2026-09-27:

- GitHub Actions run: #63
- commit: `090f339246f5c15a8edeffd83c9f5b9e77a3155e`
- failed step: `Verify privacy/project invariants`
- Gradle/unit tests/APK were never reached.

Exact failure:

`AccessibilityService usage must be isolated to Guardian Web only: BrowserAccessibilityExtractor.kt,BrowserAccessibilityService.kt,BrowserWebAccess.kt`

Root cause:

The `scripts/verify_project.py` file in `main` was still the old version. It searched every Kotlin file for the literal text `AccessibilityService`. Hybrid v3 legitimately mentions `BrowserAccessibilityService` in an extractor comment, so the guard produced a false positive even though the extractor does not import the Android AccessibilityService API.

Correction in this package:

- detect only actual `import android.accessibilityservice.*` lines;
- keep the allowed API surface restricted to BrowserAccessibilityService + BrowserWebAccess;
- add Modo Banco static invariants;
- include `scripts/verify_project.py` again in this self-contained ZIP so the fix cannot depend on a previous patch being applied.

Modo Banco:

- user activates it before opening a bank;
- Guardian Web banks/stops the current web interval, clears the Hybrid session and calls `disableSelf()`;
- Guardian Web UI waits until both AccessibilityManager and secure settings report the service OFF;
- only then does the UI say it is safe to open the banking app;
- Usage Access/app monitoring is not disabled;
- Guardian Web must be manually re-enabled after banking.

No attempt is made to inspect banking screens or to hide an enabled AccessibilityService from a bank.
