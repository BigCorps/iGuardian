# Store Compliance Design Notes — 0.1.7

Current MVP:
- own-device local diagnostics/digital wellbeing;
- no Internet permission;
- no account/cloud;
- no Accessibility Service;
- no VPN inspection;
- no screen capture;
- no keyboard/input capture;
- no notification/message content;
- no QUERY_ALL_PACKAGES;
- explicit Usage Access;
- PRIVATE sanitized before storage;
- SYSTEM sanitized before storage;
- local questions use sanitized data and are not stored;
- background work uses standard Android JobScheduler only;
- boot recovery uses RECEIVE_BOOT_COMPLETED only for the local scheduled collector.

Final production submission still requires a current policy review.
