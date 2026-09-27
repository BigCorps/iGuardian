# Data Schema — Guardian Android 0.1.24

## Persistent database

- database version: **8** — unchanged;
- daily/report schema: **5** — unchanged;
- no migration is required from 0.1.23;
- browser-history persistence remains only:
  - sanitized host;
  - supported browser package;
  - start/end interval;
  - private-mode boolean.

No full URL, path, query, fragment, title, page content, screenshot, raw OCR or
accessibility node text is added to the database.

## Diagnostic / validation schemas

- diagnostic schema: **21** — unchanged from 0.1.23;
- validation pack schema: **11** — unchanged;
- ValidationSuite version: **11** — unchanged.

Hybrid v3 extends Guardian Web runtime telemetry without changing the database
schema. New/clarified SharedPreferences diagnostics include:

- tree probe/direct/fallback/missing/private-marker/normal-marker counters;
- last tree source/state/url-bar resource ID;
- bounded resource-ID-only inventories, including per-browser normal/private
  groups when private mode has been independently detected;
- per-window screenshot request/success/failure/error counters;
- `secure_browser_window` evidence and count;
- `visual_private_probe_count` = probe attempts;
- `visual_private_detected_count` = positive visual private detections;
- last detector source.

These are technical compatibility diagnostics. Resource-ID inventories never
contain node text, URL values or page content.

## Bank Mode — 0.1.24

No database migration and no new persisted user-content category.
`WEB_BANK_MODE_DISABLE` is a technical event only; it contains no bank name,
account information, screen content, credentials or transaction data.
