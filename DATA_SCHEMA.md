# Data Schema — Guardian Android 0.1.25

## Persistent database

- database version: **8** — unchanged;
- daily/report schema: **5** — unchanged;
- no SQL migration required from 0.1.24;
- browser history remains only sanitized host + supported browser package + interval + private-mode boolean.

No full URL, path, query, fragment, title, page content, screenshot, raw OCR,
accessibility node text, bank screen, credentials, account data or transaction
content is added to persistence.

## Diagnostic / validation schemas

- diagnostic schema: **21** — unchanged;
- validation pack schema: **11** — unchanged;
- ValidationSuite version: **11** — unchanged.

0.1.25 changes acceptance/flow rather than database shape:

- visual OCR uses a stricter public-host plausibility gate;
- clean-test reset clears only existing Guardian Web rows/telemetry;
- FinancialAppCatalog uses package/launcher label classification only;
- PrivacyClassifier version becomes **3**, triggering the existing privacy repair
  so previously stored financial APP rows can be reclassified PRIVATE;
- `WEB_TEST_RESET` and `WEB_BANK_MODE_DISABLE` are technical events without web
  page text or banking content.
