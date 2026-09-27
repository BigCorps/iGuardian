# Guardian Android 0.1.24 — Hybrid v3 implementation notes

Base: 0.1.23 repository HEAD `aaad03f8527ee2ce3a19a58ee2cfb17f166a327c`.
That base's Android APK workflow #59 completed successfully after the bundled OCR
manifest-merger network-permission correction.

Why 0.1.24 exists:
- physical 0.1.23 evidence showed screenshots/OCR work technically;
- host-shaped OCR reads were much more frequent than persisted host rows;
- private visual probing could remain at zero because the mode probe was disabled
  after a normal host observation existed;
- accessibility extractor code still existed but was disconnected from the
  running service pipeline.

Hybrid v3 reconnects the tree as the primary source without returning heavy work
to the Accessibility callback. Direct known IDs are read synchronously; bounded
fallback, resource-ID inventory, OCR and SQLite remain off the callback hot path.

Important deliberate choices:
- URL bar visibility is not required, focus/typing is still rejected;
- a missing toolbar never clears the last valid host while UsageStats says the
  same browser is foreground;
- tree hosts are strong, OCR hosts still need two consecutive reads;
- secure-window screenshot error is not automatically incognito;
- private → normal transition requires a positive Chromium standard-mode accessibility marker;
- a window-screenshot failure falls back to the 0.1.23 display-screenshot path;
- Mi Browser is observed without inventing a resource ID;
- Device Owner is not introduced into normal Guardian onboarding.

Expected GitHub Actions artifact after upload:
`guardian-android-0.1.24-fixed-signed-debug`.
