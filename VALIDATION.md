# Validation — Android 0.1.0

Validation performed before packaging on 2026-09-24.

## Passed locally

- XML parsing: AndroidManifest + resources.
- YAML parsing: GitHub Actions workflow.
- `PROJECT_STATE.json` parsing.
- Pure Kotlin compilation for privacy model / storage sanitizer sources.
- CI privacy-invariant script.
- Confirmed Manifest does not declare `android.permission.INTERNET`.
- Confirmed Manifest does not declare `android.permission.QUERY_ALL_PACKAGES`.
- Confirmed no AccessibilityService declaration.
- Confirmed handoff docs are present.
- Confirmed `PRIVATE`, `SCREEN_OFF` and `ANONYMOUS_BROWSER` storage sanitizer removes package/app identity for non-APP intervals.

## Build validation path

The packaging environment used to create this ZIP does not include an Android SDK, so the full Android Gradle compile cannot be truthfully claimed here. The repository includes a GitHub Actions workflow that installs Android API 36 + Build Tools 36.0.0 + Gradle 9.6.0, runs unit tests and builds `app-debug.apk`. That workflow is the first definitive Android compiler/build validation after upload to GitHub.

If GitHub Actions reports a compiler/OEM issue, keep the log and update this document, `CHANGELOG.md` and `PROJECT_STATE.json` in the next ZIP.
