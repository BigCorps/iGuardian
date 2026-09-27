# Guardian Android 0.1.23 — corrected Visual build

GitHub Actions run #55 failed before APK generation at `compileDebugKotlin`.

Exact compiler error:
`BrowserWebPreferences.kt:272:40 Unresolved reference 'isValidStoredHost'`

Cause:
the new visual telemetry referenced a helper name that does not exist.
The canonical BrowserDomainSanitizer API is:

`BrowserDomainSanitizer.isSanitizedHost(...)`

This corrected package keeps:
- versionName 0.1.23
- versionCode 24
- Guardian Web Visual v1
- transient Accessibility screenshot
- bundled local ML Kit OCR
- two consecutive host confirmations
- UsageStats foreground confirmation
- visual/private mode probing
- no screenshot/raw OCR persistence

A CI source guard now rejects the stale helper symbol if it ever reappears.

Expected artifact:
guardian-android-0.1.23-fixed-signed-debug
