# Store Compliance Design Notes

This file is a living engineering checklist, not legal advice.

## MVP positioning

Guardian is a local self-diagnostic, digital-usage and privacy tool for the device owner. Initial public release must not be marketed as spouse/partner surveillance or covert monitoring.

## Technical choices made to simplify review

- no INTERNET permission in MVP;
- no account or cloud history;
- no Accessibility Service;
- no VPN traffic interception;
- no screen/media capture;
- no keyboard/input capture;
- no notification/message content access;
- no `QUERY_ALL_PACKAGES`;
- Usage Access is explicit and user-granted;
- protected contexts are sanitized before storage;
- exports happen only after an explicit user action through the Android document picker.

## Before production submission

Re-check current Google Play policy at submission time, especially around usage access, monitoring/behavior tracking disclosures, financial/sensitive information, package visibility, billing and background execution.

Final production identity must include:

- final product name;
- production applicationId;
- icon/logo;
- support URL;
- privacy policy URL;
- clear in-app explanation of Usage Access and collected metadata.
