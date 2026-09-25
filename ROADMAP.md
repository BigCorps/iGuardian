# Guardian Roadmap

## Phase 1 — Android Foundation

Validated:
- UsageStats collection;
- privacy-before-storage;
- SQLite/local JSON;
- fixed signing and update-in-place;
- PRIVATE;
- system-surface separation;
- screen-off/unlocks;
- user/profile privacy boundary;
- non-overlapping timeline;
- overnight/day-boundary clipping;
- export verification;
- 99%+ long-run coverage on test device.

Current:
- scheduler v4 chained one-shot validation;
- reboot recovery;
- broader OEM hardening.

## Phase 2 — Local Intelligence — STARTED IN 0.1.7

Alpha:
- deterministic Portuguese intent parser;
- today summary;
- top app/top 5;
- named app time;
- unlock/screen-off/PRIVATE/SYSTEM/coverage questions;
- no query persistence;
- no external API.

Next:
- yesterday;
- last 7 days;
- comparisons/trends;
- custom periods;
- richer deterministic intent routing;
- sanitized JSON handoff for complex questions.

## Phase 3 — Android hardening

- additional OEM/device tests;
- timezone changes;
- power-management guidance;
- support diagnostics.

## Phase 4 — Android Store

- final brand/applicationId;
- production signing;
- landing/privacy/support;
- AAB / Play review.

## Phase 5 — Windows

- Windows collector using the same privacy contract.

## Phase 6 — Optional premium

Only after initial approvals:
- account;
- pairing;
- optional cloud/external AI;
- payment.
