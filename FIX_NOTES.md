# Guardian Android 0.1.3 — Privacy Fix

Replace/add these files preserving paths:

- app/src/main/java/com/bigcorps/guardian/core/PrivacyClassifier.kt
- app/src/main/java/com/bigcorps/guardian/core/UsageCollector.kt
- app/src/main/java/com/bigcorps/guardian/core/PrivacyRepair.kt (NEW)
- app/src/main/java/com/bigcorps/guardian/core/GuardianDatabase.kt
- app/src/main/java/com/bigcorps/guardian/core/LocalReportStore.kt
- app/src/main/java/com/bigcorps/guardian/core/GuardianScheduler.kt
- app/src/main/java/com/bigcorps/guardian/core/DiagnosticsGenerator.kt
- app/src/main/java/com/bigcorps/guardian/GuardianApplication.kt
- app/src/main/java/com/bigcorps/guardian/ui/PrivateAppsActivity.kt
- app/build.gradle.kts
- .github/workflows/android.yml
- README.md
- CHANGELOG.md
- PROJECT_STATE.json

Reason:
The first real exported diagnostic showed Nubank and InfinitePay as APP rather than PRIVATE.
This patch fixes future classification and repairs existing local rows/reports.
It also instruments the periodic JobScheduler failure reported by the diagnostic.
