# Guardian Roadmap

## Phase 1 — Android Foundation

Core data/privacy: validated on the primary Xiaomi Android 16 device.

Background:
- direct JobScheduler v1-v4 retired;
- WorkManager v5 passed first periodic + reboot persistence validation in 0.1.8;
- 0.1.9 adds stop-reason telemetry for OEM/system retry diagnosis.

## Phase 2 — Local Intelligence — ACTIVE

0.1.7: today.
0.1.8: yesterday, 7 days, today-vs-yesterday.
0.1.9: rolling 24h + deterministic factual insights.

Next:
- explicit custom date ranges;
- week-over-week trends once enough history exists;
- daily/weekly local insight cards;
- sanitized handoff for genuinely complex questions.

## Phase 3 — hardening
Additional OEMs, timezone/date changes, battery guidance, support diagnostics.

## Phase 4 — Play Store
Final brand/applicationId/signing/privacy/support/AAB.

## Phase 5 — Windows
Same privacy contract with Windows-native collector.

## Phase 6 — optional premium
Only after approvals: account, pairing, optional cloud AI, payments.
