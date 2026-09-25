# Store Compliance Design Notes — 0.1.8

- own-device diagnostics/digital wellbeing;
- no INTERNET;
- no account/cloud;
- no AccessibilityService;
- no VPN;
- no screen/media capture;
- no keyboard/input capture;
- no notification/message content;
- no QUERY_ALL_PACKAGES;
- explicit Usage Access;
- privacy sanitization before storage;
- local queries are offline and not stored;
- WorkManager is used for ordinary deferrable background maintenance;
- RECEIVE_BOOT_COMPLETED remains for background continuity and diagnostic signal.

Final Play submission requires a current policy review.
