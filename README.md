# Guardian DEV — Android 0.1.24 — Guardian Web Hybrid v3

## Current state

0.1.23 proved that the visual pipeline itself works on the Xiaomi/Android 16 test
device: screenshots and bundled ML Kit OCR ran successfully and produced many
host-shaped readings, but only one host reached persisted browser history. The
next problem is therefore extraction/acceptance strategy rather than permission,
screenshot capability or the OCR model.

0.1.24 changes Guardian Web from visual-first to a hybrid pipeline.

## Hybrid v3 pipeline

1. A supported browser emits an AccessibilityEvent.
2. While the event/source is still valid, Guardian performs only cheap direct
   `findAccessibilityNodeInfosByViewId()` lookups for known address-bar IDs.
3. A valid sanitized host from the tree is accepted immediately.
4. A bounded worker-thread tree pass can use fallback IDs/markers and records a
   diagnostic inventory of resource IDs only.
5. Only when the tree cannot supply enough evidence does screenshot + bundled
   local OCR act as the host fallback.
6. Visual hosts still require two consecutive reads before replacing history.
7. UsageStats confirms which app remains foreground and keeps banking the last
   valid host even when a browser hides/collapses its toolbar.

No BFS, OCR or SQLite work is performed in the Accessibility callback hot path.

## Hidden toolbar behavior

The URL bar no longer has to satisfy `isVisibleToUser`. Browsers may keep the
address node in the accessibility tree while the toolbar is collapsed after a
scroll. The focused/typing guard is retained, so partial user input is never
accepted as history.

## Private/incognito evidence

Private-mode detection combines independent signals:

- accessibility resource/label markers;
- UsageStats activity/class hints;
- local visual text when available;
- API 34+ window-screenshot failure diagnostics.

`ERROR_TAKE_SCREENSHOT_SECURE_WINDOW` is stored only as `secure_browser_window`
evidence. If a per-window screenshot fails, Guardian retains that evidence and
falls back to the display screenshot path already proven on 0.1.23. It is never treated by itself as proof of incognito because enterprise
or other secure-window policies can produce the same Android error.

Private state is also reversible without guessing: when Chromium exposes a
strong standard-tab accessibility marker such as `Enter incognito mode`, Guardian
closes the private interval and resumes normal banking. A missing marker is never
used to clear private mode.

The 0.1.23 telemetry ambiguity is also fixed:

- `visual_private_probe_count` = actual probe attempts;
- `visual_private_detected_count` = positive visual detections.

## Browser calibration

The browser catalog and AccessibilityService allowlist now include stable/beta/
dev/canary variants for major Chromium browsers, Firefox variants, Samsung Beta,
Opera variants and others. Opera stable prefers `url_field`.

`com.android.browser` (Mi Browser / Android Browser) is deliberately included
without a guessed URL-bar resource ID. Hybrid v3 exports only resource IDs from
the browser accessibility tree so the Redmi itself can tell us which ID should
be pinned later. The validation report also keeps bounded per-browser `normal`
and `private_when_detected` ID inventories, so one export can preserve both
states when private mode was actually detected.

## Privacy / offline contract

Guardian remains local-only:

- no INTERNET permission in the final APK;
- no ACCESS_NETWORK_STATE permission in the final APK;
- screenshots are memory-only and recycled;
- raw OCR is not persisted;
- tree diagnostic stores resource IDs only, never node text;
- only sanitized host + browser + interval + private boolean reach browser
  history;
- no path, query, fragment, title, page content or typed text is stored.

## Bank compatibility mode

0.1.24 now includes a preventive **Modo Banco** inside Guardian Web. Before
opening a banking/financial app, the user can ask Guardian Web to call Android
`disableSelf()`. Guardian then waits for the Accessibility service to disappear
from both AccessibilityManager and the secure setting before confirming that the
mode is active.

Only Guardian Web is disabled. App-usage monitoring through Usage Access keeps
working. Android does not allow Guardian to silently re-enable Accessibility
afterwards, so the user must explicitly reactivate Guardian Web in Accessibility
settings when banking is finished.

This is deliberately preventive rather than trying to detect a bank after it has
already opened; a financial app may evaluate device risk immediately at startup.

## Next physical validation

After GitHub Actions builds 0.1.24:

1. Install over 0.1.23 and keep Guardian Web enabled/consented.
2. Chrome Dev normal: visit `uol.com.br` ~20s.
3. Chrome Dev normal: visit `globo.com` ~20s.
4. Open incognito and remain on its start page ~4–6s.
5. Visit a third host in incognito ~20s.
6. Optionally repeat normal/private in Mi Browser.
7. Return to Guardian and export one validation JSON.

Expected evidence: tree probes/hits, multiple distinct hosts, normal and anonymous
rows, nonzero private-probe attempts, resource-ID inventory, and no persisted raw
screen/OCR data.
