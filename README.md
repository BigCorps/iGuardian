# Guardian DEV — Android 0.1.21 — Guardian Web Event Snapshot v3

## What 0.1.20 proved

The observer no longer freezes the UI, so the off-main-thread change worked.
The service is enabled, connected, alive and receives Chrome Dev events.

Physical evidence from 0.1.20:
- Accessibility enabled: true
- Accessibility alive: true
- 780 browser accessibility events received
- last event browser: com.chrome.dev
- 24 samples
- 0 errors
- 0 hosts
- every sample failed before tree inspection with ROOT_NULL

This isolates the next problem: `rootInActiveWindow` is null when queried later
from the worker thread on this Xiaomi/Android 16 device.

## Observer v3

Android's Accessibility API explicitly supports retrieving window content from
`AccessibilityEvent.source`, and the callback event must be copied if it will be
used after the callback returns. 0.1.21 does exactly that:

1. Chrome Dev event arrives on the Accessibility callback.
2. Guardian immediately copies the event.
3. The copied event is moved to `GuardianWebObserver`.
4. The worker obtains the event source.
5. It resolves the source window root (or walks parents as fallback).
6. It extracts only the sanitized host and incognito marker.

The service also requests interactive windows and non-important browser toolbar
views, while remaining package-scoped to supported browsers.

## Accurate duration without reading other apps through Accessibility

The 5-second heartbeat uses the already-authorized UsageStats API only to check
which app is foreground. If the same browser remains foreground, the sanitized
host interval is banked. When the user leaves the browser, the interval stops.

## New calibration evidence

Validation now records counts only:
- event source available / null
- event window root resolved
- host found
- extraction state

No raw URL or page content is added.

## UI polish

The Guardian Web screen now respects status/navigation bar insets on Android 16
so its heading no longer sits underneath the status bar.

## Physical test

Install 0.1.21 over 0.1.20. Ensure Guardian Web remains enabled.

1. Open a normal Chrome Dev site for 20–30 seconds.
2. Open a different incognito site for 20–30 seconds.
3. Return to Guardian Web.
4. Confirm the screen stays responsive and the diagnostic shows `fontes` and
   `raízes` above zero.
5. Export one validation JSON.
