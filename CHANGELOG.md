# Changelog

## 0.1.25 — 2026-09-27

### CI correction after Actions #66

- Actions #66 passed privacy/project invariants, validation lineage, DEV signing, main Kotlin compilation and unit-test compilation.
- The run stopped at `:app:testDebugUnitTest`: 49 tests executed, 1 failed (`BrowserVisualTextParserTest.emailLikeTextIsNotStoredAsHost`).
- Root cause: the token path rejected `usuario@example.com`, but the secondary domain regex extracted the substring `example.com` from inside the e-mail address.
- `DOMAIN_LIKE_REGEX` now requires a non-email/domain boundary before a visual domain candidate, so e-mail text cannot leak a host while ordinary `example.com`, `https://example.com` and spaced OCR URLs remain supported.
- Regression coverage now includes plain, plus-tag and prefixed e-mail forms plus a positive ordinary-domain control.

- versionCode 26 / versionName 0.1.25; base Actions #64 green.
- Added strict OCR-only host acceptance. Visual noise such as `kit`, `fallback`,
  `https`, `app`, `quiser` and implausible long-TLD artifacts such as
  `midia.prosu` are rejected before persistence. Tree-derived values keep the
  general URL sanitizer.
- Visual parser now recognizes current Chromium private-tab redesign evidence in
  pt-BR (`Agora você pode navegar com privacidade...`) and English, while
  `Nova guia anônima` / `New incognito tab` remain action-only and do not prove
  that the current tab is private.
- Host OCR crop reduced from 22% to 16% of the display to stay inside browser chrome; private OCR probe enlarged from 62% to 90% of the display.
- Added **Iniciar teste limpo**: web-only history/telemetry and in-memory Hybrid
  state are reset in deterministic order; normal app history is untouched.
- Added `FinancialAppCatalog`; confirmed Inter packages include
  `br.com.Inter.CDPro` (Inter Empresas) and `br.com.intermedium` (Inter).
- `PrivacyClassifier` v3 treats financial apps as PRIVATE and triggers the
  existing privacy repair on upgrade.
- Modo Banco now discovers installed financial launcher apps and offers
  **Abrir <banco> com proteção**. Guardian confirms Accessibility is OFF before
  launching the selected financial app.
- Added a best-effort UsageStats financial foreground failsafe for direct bank
  launches. It is secondary to protected launch because it reacts only after the
  financial process becomes foreground.
- No DB schema change, no new network permission, no bank/account/transaction
  content storage.


## 0.1.24 — 2026-09-27

### Guardian Web Hybrid v3

- versionCode 25 / versionName 0.1.24.
- Accessibility tree first, screenshot/OCR fallback.
- Direct known URL-bar IDs are read while AccessibilityEvent/source is valid.
- URL-bar host extraction no longer requires `isVisibleToUser`; focused typing remains rejected.
- Bounded tree fallback/resource-ID-only diagnostics remain off the callback hot path.
- Last valid host continues accruing while UsageStats confirms the same browser remains foreground.
- Tree hosts are strong signals; visual fallback still needs two consecutive host reads.
- Private visual probing can run after a normal host is known.
- Private-to-normal Chromium transitions require positive standard-mode evidence.
- `visual_private_probe_count` counts attempts; `visual_private_detected_count` counts positives.
- API 34+ prefers `takeScreenshotOfWindow()` and falls back to display screenshot on failure.
- secure-window error is diagnostic evidence only, never automatic incognito.
- Expanded browser catalog/AccessibilityService allowlist; Opera uses `url_field` and Mi Browser is calibrated without guessed IDs.
- Same-timestamp BrowserWebAccess snapshot reuse prevents contradictory validation flags.

### Bank compatibility mode

- Added preventive **Modo Banco** to Guardian Web.
- Modo Banco requests a real Accessibility shutdown with `AccessibilityService.disableSelf()`.
- Before confirming success, Guardian verifies both AccessibilityManager and Android secure settings no longer report Guardian Web enabled.
- The normal Guardian app-usage collector remains active through Usage Access.
- Guardian does not silently re-enable Accessibility after banking; the user reactivates Guardian Web explicitly in Android Settings.
- The mode is preventive: it must be activated before opening the financial app rather than reacting after the bank has already started its security checks.
- Technical event `WEB_BANK_MODE_DISABLE` records the local mode transition without banking content.

### CI guard correction — Actions #61 / #63

- Actions #61 and #63 stopped at `Verify privacy/project invariants` before Gradle.
- Root cause: `verify_project.py` treated any plain-text occurrence of `AccessibilityService` as API usage. A harmless Hybrid-v3 comment in `BrowserAccessibilityExtractor.kt` therefore triggered a false positive.
- Guard now detects real `import android.accessibilityservice.*` declarations instead of comments/strings.
- Allowed API use remains restricted to `BrowserAccessibilityService.kt` and `BrowserWebAccess.kt`; the privacy boundary is not relaxed.
- Added static Modo Banco guard for `requestBankModeDisable()`, `disableSelf()` and the Guardian Web UI entrypoint.

### Privacy contract unchanged

- No database schema change.
- No INTERNET or ACCESS_NETWORK_STATE in the merged APK contract.
- No screenshot/OCR/tree text persistence.
- Browser history remains host-only.

## 0.1.23 — 2026-09-27

- Guardian Web Visual v1: transient screenshot + bundled ML Kit OCR.
- Two consecutive visual host reads before persistence.
- UsageStats foreground confirmation for continuous duration banking.
- Explicitly remove transitive INTERNET and ACCESS_NETWORK_STATE permissions.
- Final APK guard verifies the merged artifact remains offline.

## 0.1.22 — 2026-09-27

- Guardian Web v4 capture path.
- Copied AccessibilityEvent source before interactive-window fallback.
- Browser tree work kept off the app UI thread.
- UsageStats duration banking and stronger incognito heuristics.
