# Store Compliance Design Notes

Current Android foundation:
- no INTERNET permission;
- no Accessibility Service;
- no VPN interception;
- no screen/media capture;
- no keyboard/input capture;
- no notification/message content;
- no QUERY_ALL_PACKAGES;
- Usage Access is explicit;
- PRIVATE is sanitized before storage;
- SYSTEM identity is sanitized before storage;
- other-user/profile activity is represented generically as PRIVATE;
- export occurs only by explicit user action;
- background work uses Android JobScheduler;
- no remote monitoring or hidden collection path exists.

DEV signing is separate from future production signing.

Background reliability work must not introduce covert persistence, Accessibility, VPN or content capture.
