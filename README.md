# Guardian DEV — Android 0.1.23 — Guardian Web Visual v1

0.1.22 confirmed that Accessibility events arrive on the physical Xiaomi/Android 16
device, but the Chrome accessibility tree/event source is not reliable enough for
continuous domain capture. Only one old `m.youtube.com` interval of about 5 seconds
remained while Chrome Dev had much more foreground time.

0.1.23 stops treating the browser accessibility tree as the primary URL source.

## Guardian Web Visual

When a supported browser is confirmed in the foreground through UsageStats, the
opt-in AccessibilityService can request a temporary screenshot.

Privacy flow:

1. Screenshot exists only in memory.
2. OCR first analyzes only the upper toolbar band.
3. Raw OCR text is never stored/exported.
4. Candidate URL text is immediately reduced to a sanitized host.
5. A host must appear in two consecutive visual reads before persistence.
6. Only host + browser package + time + private boolean enter SQLite.
7. Screenshot/bitmap is recycled immediately after processing.

For incognito detection, when no host is known the service may temporarily inspect a
larger upper portion of the screenshot to detect explicit incognito start-page text.
UsageStats activity class evidence is also accepted when Chrome exposes an incognito
launcher/activity.

The OCR model is bundled in the APK (`com.google.mlkit:text-recognition:16.0.1`);
no model download or INTERNET permission is needed.

## Physical test

After Actions passes:
- install over 0.1.22;
- accept the new Guardian Web Visual disclosure (consent v2);
- confirm Accessibility is enabled;
- normal Chrome Dev: `uol.com.br` ~20 s, then `globo.com` ~20 s;
- return to Guardian for a few seconds;
- open a new incognito tab, wait ~4 s on its start page, then visit another host ~20 s;
- return to Guardian Web and export one validation JSON.

If screenshot capture itself is blocked, diagnostics will expose the Android
takeScreenshot error code so we can decide immediately whether this path is viable.
