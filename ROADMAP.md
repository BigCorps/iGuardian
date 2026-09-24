# Guardian Roadmap

## Phase 1 — Android Foundation (0.1.x) — CURRENT

- local collector;
- privacy engine;
- SQLite;
- daily JSON;
- diagnostic JSON;
- private-app selector;
- GitHub Actions APK build;
- compatibility feedback loop.

## Phase 2 — Local intelligence

- deterministic question parser;
- SQL/local calculations;
- supported questions and evidence-based answers;
- trend comparisons;
- no external API;
- complex-question fallback offering a user-selected JSON export period for the user's preferred AI app.

## Phase 3 — Android hardening

- broader OEM documentation-based compatibility;
- battery/background diagnostics;
- day/reboot/timezone edge cases;
- support workflow;
- store-compliance review;
- final branding/applicationId/domain only when core behavior is approved internally.

## Phase 4 — Android Store

- final name/logo/domain;
- privacy/support landing pages;
- final onboarding;
- production signing/AAB;
- Google Play submission.

## Phase 5 — Windows

- separate collector implementation;
- same conceptual privacy/data schema;
- EXE for tests, packaged store build for release;
- Windows Store submission.

## Phase 6 — Premium, only after initial store approvals

- optional account/login;
- optional device-to-device sharing with explicit user authorization;
- optional in-app cloud AI analysis;
- web payment/account management;
- evaluate reusing BigCorps/minhAi Supabase Auth/payment infrastructure while keeping Guardian in a separate repository;
- no automatic remote surveillance.
