# Local Intelligence v2 — 0.1.8

100% local and deterministic.

No LLM, server, API or query logging.

Periods:
- today;
- yesterday;
- last 7 days.

Intents:
- summary;
- top app;
- top 5;
- named app time;
- total app usage;
- screen-off;
- unlocks;
- PRIVATE;
- SYSTEM;
- coverage;
- today-vs-yesterday comparison.

Comparisons include each period's effective tracked duration and coverage so a partial historical day is not silently treated as a complete day.

Source: sanitized ReportGenerator output over local SQLite.
