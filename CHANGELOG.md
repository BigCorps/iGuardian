# Changelog

## 0.1.22 — 2026-09-27

- versionCode 23 / versionName 0.1.22.
- Guardian Web v4 capture path.
- Uses copied AccessibilityEvent source before falling back to interactive windows.
- Browser tree work remains off the app UI thread.
- Domain duration no longer depends on rootInActiveWindow succeeding every 5 seconds.
- UsageStats confirms which app remains foreground before banking browser time.
- Direct Chrome/Chrome Dev incognito badge detection added.
- Current-incognito labels expanded without treating “New/Enter Incognito” actions as proof.
- Added UOL/Globo sanitizer unit cases.
- No DB/report/privacy/network expansion.

### Corrected 0.1.23 build
- Explicitly remove transitive INTERNET and ACCESS_NETWORK_STATE permissions
  introduced by bundled OCR dependencies.
- Final APK guard verifies the merged artifact remains offline.
