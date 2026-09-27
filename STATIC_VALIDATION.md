# Static validation — Guardian Android 0.1.25

Baseline: Actions #64 green on commit `1e196d5fd32d4e25e73ad1ffe85db8e90b623959`.

## Changed contracts

- versionName 0.1.25 / versionCode 26;
- stricter OCR-only host acceptance;
- current Chromium private-tab pt-BR/en visual markers;
- host OCR crop 16% + private visual crop 90%;
- deterministic clean web-test reset;
- FinancialAppCatalog + PrivacyClassifier v3;
- protected financial launcher after confirmed Accessibility shutdown;
- best-effort automatic financial foreground shutdown;
- validation lineage now includes `privacy_classification_core`.

## Unchanged privacy/security contracts

- no INTERNET permission in merged APK;
- no ACCESS_NETWORK_STATE permission in merged APK;
- no QUERY_ALL_PACKAGES;
- no full URL/path/query/title/page-content persistence;
- no screenshot or raw OCR persistence;
- no bank/account/Pix/transaction content persistence;
- database schema remains v8.

## Local pre-package checks

- BrowserDomainSanitizer + BrowserVisualTextParser + FinancialAppCatalog compile with Kotlin JVM compiler;
- behavior harness passes for OCR junk rejection, URL spacing repair, Chromium pt-BR/en private markers and Inter/Pinterest/Internet-label classification separation;
- modified Guardian Web Activity + financial catalog compile in Android structural harness;
- modified service differs from the Actions-#64-compiled service only in localized financial-failsafe/reset code plus one pure-Kotlin catalog import; no new Kotlin parser errors were introduced;
- root and Android asset validation contracts are byte-identical;
- workflow artifact name matches 0.1.25;
- Python CI guards compile syntactically.

The authoritative Android SDK, unit-test, merged-manifest, signing and APK checks remain GitHub Actions.
