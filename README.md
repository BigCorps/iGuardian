# Guardian DEV — Android 0.1.20 — Guardian Web Observer v2

## Physical finding from 0.1.19

The Guardian Web service really connected on the Xiaomi/Android 16 device, but
the detector never reached a host:

- consent granted
- `WEB_SERVICE_CONNECTED` recorded
- tracking start recorded
- 0 browser rows
- no last browser
- no URL-bar id
- no detector timestamp

The user also observed the Guardian UI becoming unresponsive after visiting
Chrome and returning.

## Observer v2

The v1 service performed accessibility-tree queries, fallback traversal,
incognito-marker traversal and SQLite writes on the app main thread.

0.1.20 moves that work to a dedicated `HandlerThread` named
`GuardianWebObserver`.

The Accessibility callback now only records sanitized counters and schedules a
debounced worker sample.

Worker behavior:
- 5-second periodic sample
- 500 ms event debounce
- direct browser `url_bar` lookup first
- bounded 120-node fallback
- host-only persistence
- incognito/private marker check
- SQLite writes off the UI thread

## Better Android/Xiaomi status detection

The old status probe depended on one AccessibilityManager result. 0.1.20
combines:
- AccessibilityManager
- Android secure enabled-service setting
- recent service heartbeat

Diagnostics distinguish configured vs alive.

## Privacy-safe calibration

The app now records only technical counters/states:
- service connection count
- heartbeat
- accessibility event count
- sample count
- host-found/focused/missing/invalid/error counts
- last supported browser package
- last URL-bar resource id
- last extraction state/error class

It never logs the raw URL, path, query, search text, title or page content.

## Physical test

Install over 0.1.19. If Android disabled Accessibility during the update,
re-enable Guardian Web.

Then:
1. Chrome Dev normal: one site for ~20–30 seconds.
2. Chrome Dev incognito: a different site for ~20–30 seconds.
3. Return to Guardian Web. The screen must remain responsive.
4. Check the local diagnostic card.
5. Export one recommended validation JSON.

Expected final proof:
- `guardian_web_runtime_health` PASS
- normal rows > 0
- anonymous rows > 0
- >= 2 hosts
- `guardian_web_physical_validation` PASS
- `manual_test_required=false`
