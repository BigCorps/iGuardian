# Changelog

## 0.1.3 — 2026-09-24

Critical privacy correction from first real diagnostic.

- Automatic private detection now considers app label in memory, not only package name.
- Added confirmed private packages `com.nu.production` and `io.cloudwalk.infinitepaydash`.
- Added classifier versioning.
- Added one-time repair of existing sensitive APP rows to PRIVATE.
- Repair erases package/label from existing sensitive rows.
- Derived local JSON reports are purged after privacy repair and today's report is regenerated.
- Added privacy classifier/repair versions to diagnostic.
- Added JobScheduler result logging and retry on process start.
- Bumped internal SQLite version to 2.
- Bumped app to versionCode 4 / versionName 0.1.3.

## 0.1.2 — 2026-09-24

- Direct Downloads/iGuardian MediaStore export.
- Byte-for-byte export read-back verification.
- Verified JSON sharing.

## 0.1.1 — 2026-09-24

- Restricted-settings onboarding.
- Visual redesign.
- First export reliability fix.

## 0.1.0 — 2026-09-24

Initial Android foundation.
