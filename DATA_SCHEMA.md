# Data Schema — export v2 / DB v4

No privacy schema expansion in 0.1.7.

Timeline types:
- APP
- PRIVATE
- SCREEN_OFF
- SYSTEM
- ANONYMOUS_BROWSER

Only APP may contain package/name.

Export precedence:
`PRIVATE > ANONYMOUS_BROWSER > SCREEN_OFF > SYSTEM > APP`

Local Intelligence Alpha reads the already-sanitized generated report. It does not create a second identity store.

Questions typed by the user are not persisted.

Diagnostic schema v5 adds scheduler-chain telemetry and local-question-engine capability flags.
