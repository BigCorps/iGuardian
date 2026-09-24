# Runtime Fix 0.1.1

This package replaces only the files listed below.

## Files
- `app/src/main/java/com/bigcorps/guardian/MainActivity.kt`
- `app/src/main/java/com/bigcorps/guardian/ui/PrivateAppsActivity.kt`
- `app/src/main/res/values/themes.xml`
- `app/build.gradle.kts`
- `.github/workflows/android.yml`
- `README.md`
- `CHANGELOG.md`
- `PROJECT_STATE.json`

## Fixes
1. Android sideload/restricted-settings onboarding.
2. Main screen visual redesign.
3. Private-app screen visual redesign.
4. Android edge-to-edge safe areas.
5. 0 KB JSON export fix using app-private cache + validated output.
6. Version bump to 0.1.1 / versionCode 2.
7. Actions artifact renamed to `guardian-android-0.1.1-debug`.

Upload these files preserving their paths. A push to `main` will start GitHub Actions automatically.
