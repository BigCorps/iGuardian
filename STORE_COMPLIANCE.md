# Store Compliance — Guardian 0.1.25

Guardian Web remains optional, browser-package scoped, local-only and gated by
explicit disclosure/consent. `android:isAccessibilityTool` remains false.

Accessibility is used only for the disclosed web-monitoring function: address-bar
host extraction, browser private-mode signals and transient local screenshot/OCR
fallback. Screenshot/raw OCR/tree text are not persisted. The final APK remains
CI-guarded against INTERNET, ACCESS_NETWORK_STATE and QUERY_ALL_PACKAGES.

## Financial-app compatibility

0.1.25 does not inspect financial-app UI. The protected launcher:

1. classifies launcher-visible apps locally from package/label;
2. disables Guardian Web with Android `disableSelf()`;
3. waits until Android reports the Accessibility service OFF;
4. launches the selected financial app.

The broader installed-app list was already required by Guardian's existing
privacy/app-selection experience through launcher visibility; no QUERY_ALL_PACKAGES
permission is added. The direct-open UsageStats failsafe is only a supplemental
shutdown mechanism and is disclosed as best-effort.

Guardian never spoofs the Accessibility state and never silently re-enables it.
