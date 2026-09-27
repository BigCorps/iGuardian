# Store Compliance — Guardian 0.1.24

Guardian Web remains optional, browser-package scoped and gated by explicit
in-app consent. It uses Android Accessibility only for the disclosed Guardian Web
function: reading browser UI needed to reduce the current address to a host and
to detect private-browsing signals. When the tree is insufficient, it may take a
transient local screenshot for bundled OCR; image and raw OCR are not stored.

The AccessibilityService description now states both tree/address-bar reading
and transient screenshot/OCR behavior. `android:isAccessibilityTool` remains
false.

Hybrid v3 expands only the browser package allowlist. It does not add network
transport, QUERY_ALL_PACKAGES, page-content storage or a new backend.

The final merged APK must continue to be CI-checked to ensure transitive OCR
dependencies do not reintroduce INTERNET or ACCESS_NETWORK_STATE.
