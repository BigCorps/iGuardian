# iGuardian Android 0.1.2 — Export Fix

Replace these files preserving paths:

- `app/src/main/java/com/bigcorps/guardian/MainActivity.kt`
- `app/src/main/java/com/bigcorps/guardian/core/ExportStorage.kt` (new)
- `app/build.gradle.kts`
- `.github/workflows/android.yml`
- `README.md`
- `CHANGELOG.md`
- `PROJECT_STATE.json`

The core fix is that Android 10+ no longer depends on ACTION_CREATE_DOCUMENT for export.
Guardian writes directly to MediaStore.Downloads/Downloads/iGuardian and then reopens the
destination and compares it byte-for-byte with the generated JSON before showing success.
