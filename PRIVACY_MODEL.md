# Privacy Model — Guardian 0.1.25

Guardian remains local-first and the final APK remains offline.

## Guardian Web

- Accessibility is scoped to supported browser packages.
- Tree-derived address values are sanitized to host before storage.
- Visual OCR uses a stricter host gate than tree values because OCR is noisy.
- Screenshots live only in memory and are recycled.
- Raw OCR and accessibility node text are not persisted.
- Browser storage contains only host, supported browser package, interval and private boolean.
- No path, query, fragment, search, title, page content or typed input is stored.

## Clean validation reset

`Iniciar teste limpo` clears only Guardian Web browser sessions and technical web
runtime evidence after the active browser interval has been stopped. Normal app
history is not erased.

## Financial apps

FinancialAppCatalog is a local package/label classifier. Financial apps are a
subset of PRIVATE and are not exported as normal identifiable APP usage after
PrivacyClassifier v3 repair.

Protected launch does not inspect or intercept bank UI. It disables the Guardian
Web AccessibilityService and waits for Android to report that service OFF before
starting the chosen financial app. The automatic direct-open failsafe uses only
the foreground package reported by UsageStats; it does not read financial screens.

No account number, Pix data, bank screen content, credential or transaction detail
is stored by these features.
