# Upload — Guardian Android 0.1.25

Base expected in GitHub before upload:

- commit `1e196d5fd32d4e25e73ad1ffe85db8e90b623959` or descendant without conflicting changes;
- Actions #64 green.

Upload every file in this ZIP to the repository root while preserving its path.
New files such as `FinancialAppCatalog.kt` and `FinancialAppCatalogTest.kt` must
be created; existing files must be replaced.

After upload, confirm in GitHub before waiting for Actions:

- `app/build.gradle.kts` says 0.1.25 / versionCode 26;
- workflow artifact says `guardian-android-0.1.25-fixed-signed-debug`;
- `FinancialAppCatalog.kt` exists;
- `BrowserWebActivity.kt` contains `Abrir ${app.label} com proteção` and `Iniciar teste limpo`;
- `BrowserAccessibilityService.kt` contains `AUTO_FINANCIAL_FOREGROUND` and `requestValidationReset()`;
- root `VALIDATION_CONTRACTS.json` and Android asset are identical.

Do not install unless the new Actions run is green.

## Actions #66 corrective patch

This follow-up remains version 0.1.25 / code 26. Upload every file from the corrective ZIP preserving paths. In particular replace both:

- `app/src/main/java/com/bigcorps/guardian/web/BrowserVisualTextParser.kt`
- `app/src/test/java/com/bigcorps/guardian/web/BrowserVisualTextParserTest.kt`

Actions #66 already proved the main Android/Kotlin source compiles; this patch fixes the single failing unit-test boundary case.
