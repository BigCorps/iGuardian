# Changelog

## 0.1.24 — 2026-09-27

- versionCode 25 / versionName 0.1.24.
- Guardian Web Hybrid v3: accessibility tree first, screenshot/OCR fallback.
- Direct known URL-bar IDs are read while AccessibilityEvent/source is valid.
- URL-bar host extraction no longer requires `isVisibleToUser`; focused typing
  remains rejected.
- Bounded tree fallback and resource-ID-only diagnostics run off the callback
  hot path, with bounded per-browser normal/private inventories when mode is known.
- Last valid host continues accruing while UsageStats confirms the same browser
  remains foreground.
- Tree-derived sanitized hosts are strong signals; visual fallback still needs
  two consecutive host reads.
- Visual private-mode probing is no longer disabled merely because a normal host
  is already known.
- Private-to-normal Chromium transitions now require a positive standard-mode
  accessibility marker; absence of a private marker never clears the private latch.
- Fixed telemetry semantics: `visual_private_probe_count` now counts attempts;
  `visual_private_detected_count` counts positive detections.
- API 34+ prefers `takeScreenshotOfWindow()` when a browser window ID is known.
- Per-window screenshot failures are recorded, then the proven display screenshot
  path is retried as visual fallback.
- `ERROR_TAKE_SCREENSHOT_SECURE_WINDOW` is exported as a diagnostic signal only;
  it never automatically marks browsing as incognito.
- Expanded supported browser/package catalog and AccessibilityService allowlist.
- Opera stable prefers `com.opera.browser:id/url_field`.
- Mi Browser (`com.android.browser`) is allowlisted without a guessed URL-bar ID
  so physical resource-ID diagnostics can calibrate it safely.
- Repeated browser-access status calls using the validation pack's single
  timestamp reuse one immutable snapshot, eliminating contradictory alive flags.
- No database schema change, no new persisted user-content category, no network
  transport.

## 0.1.23 — 2026-09-27

- Guardian Web Visual v1: transient screenshot + bundled ML Kit OCR.
- Two consecutive visual host reads before persistence.
- UsageStats foreground confirmation for continuous duration banking.
- Explicitly remove transitive INTERNET and ACCESS_NETWORK_STATE permissions
  introduced by bundled OCR dependencies.
- Final APK guard verifies the merged artifact remains offline.

## 0.1.22 — 2026-09-27

- versionCode 23 / versionName 0.1.22.
- Guardian Web v4 capture path.
- Copied AccessibilityEvent source before interactive-window fallback.
- Browser tree work kept off the app UI thread.
- UsageStats duration banking and stronger incognito heuristics.
