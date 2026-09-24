# Store Compliance Design Notes

- no INTERNET permission in MVP;
- no Accessibility Service;
- no VPN interception;
- no screen/media capture;
- no keyboard/input capture;
- no notification/message content;
- no QUERY_ALL_PACKAGES;
- Usage Access explicit;
- PRIVATE sanitized before storage;
- SYSTEM identity sanitized before storage;
- export only by explicit user action;
- background work uses Android JobScheduler.

DEV signing is separate from future production signing.
