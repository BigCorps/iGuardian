# Store Compliance Design Notes — 0.1.18

No permission expansion.

CI now verifies the final built APK in addition to source-level checks.

The APK guard confirms:
- expected applicationId/version
- no INTERNET
- no QUERY_ALL_PACKAGES
- no Accessibility binding permission
- no Notification Listener binding permission
- no MediaProjection foreground-service permission

Existing fixed DEV certificate verification remains active.
