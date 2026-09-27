# Store Compliance Design Notes — 0.1.19

Guardian Web uses AccessibilityService as an optional non-accessibility-tool feature.

Requirements before public Play release:
- complete the AccessibilityService declaration in Play Console;
- prominent in-app disclosure before consent;
- affirmative consent;
- explain web browsing history access and host-only local processing;
- do not present the app as hidden monitoring/stalkerware;
- keep Guardian Web for the device owner only.

Technical guardrails:
- `android:isAccessibilityTool=false`
- browser package allowlist in service config
- BIND_ACCESSIBILITY_SERVICE protects service binding
- no INTERNET / QUERY_ALL_PACKAGES / VPN / MediaProjection / NotificationListener / IME / Clipboard capture
- focused URL bar ignored
- host-only persistence before SQLite
