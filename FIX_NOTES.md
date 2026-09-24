# Guardian Android 0.1.5

Replace/add files preserving paths.

Important: DO NOT uninstall 0.1.4.
This build is specifically intended to prove that fixed DEV signing now allows in-place updates while preserving local data.

New files:
- app/src/main/java/com/bigcorps/guardian/core/TimelineNormalizer.kt
- app/src/test/java/com/bigcorps/guardian/core/TimelineNormalizerTest.kt

Changed:
- MainActivity.kt
- GuardianApplication.kt
- GuardianDatabase.kt
- UsageCollector.kt
- GuardianPrivacyOverride.kt
- GuardianJobService.kt
- GuardianScheduler.kt
- Preferences.kt
- SystemSurfaceClassifier.kt
- ReportGenerator.kt
- DiagnosticsGenerator.kt
- app/build.gradle.kts
- .github/workflows/android.yml
- README.md
- CHANGELOG.md
- DATA_SCHEMA.md
- PROJECT_STATE.json
