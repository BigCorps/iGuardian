# Store Compliance — 0.1.23

Guardian Web Visual uses AccessibilityService only after prominent disclosure and
explicit consent. The service remains package-scoped to supported browsers.

The AccessibilityService now declares `canTakeScreenshot=true`. Screenshot processing
is local and transient; images and raw OCR text are not stored or transmitted.

Still absent:
- INTERNET
- QUERY_ALL_PACKAGES
- VPN
- MediaProjection
- notification listener
- input method
- clipboard capture

A public Play release will require the appropriate AccessibilityService declaration,
prominent disclosure, consent flow and policy review.
