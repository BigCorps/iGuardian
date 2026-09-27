# Privacy Model — Guardian 0.1.24

No new persisted user-data category is introduced by Hybrid v3.

Guardian Web may transiently inspect the accessibility tree of allowlisted
browsers and may transiently screenshot the active browser window/display for
local bundled OCR. The processing order is tree first, visual fallback.

Persisted browser history remains strictly:
- sanitized host;
- supported browser package;
- time interval;
- private-mode boolean.

Never persisted/exported as user content:
- full URL/path/query/fragment;
- browser search text or typed text;
- page title/content;
- screenshot pixels;
- raw OCR text;
- accessibility node text/contentDescription.

The DEV calibration diagnostic may persist/export browser accessibility
`viewIdResourceName` values only (for example `com.chrome.dev:id/url_bar`). No
node text accompanies those IDs. Bounded resource-ID inventories may be grouped
by browser package and by normal/private state when that private state was
independently detected.

`ERROR_TAKE_SCREENSHOT_SECURE_WINDOW` is diagnostic evidence about Android's
window security state, not proof of incognito by itself.

Bundled OCR dependencies may declare network-related permissions in library
manifests. Guardian explicitly removes INTERNET and ACCESS_NETWORK_STATE during
manifest merge and CI verifies they are absent from the final APK.


## Banking compatibility

Modo Banco does not try to hide Guardian from financial apps and does not inspect
banking app screens. It explicitly disables the Guardian Web AccessibilityService
with Android `disableSelf()` before the user opens a financial app. The regular
Usage Access collector remains independent and continues to record app-level usage.

Guardian does not auto-enable Accessibility afterwards; reactivation remains an
explicit user action in Android Settings.
