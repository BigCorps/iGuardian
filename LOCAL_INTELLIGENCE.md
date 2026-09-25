# Local Intelligence Alpha — 0.1.7

The first local question engine is deterministic and offline.

It does NOT call an LLM, OpenAI, a server or the Internet.

Source of truth:
- sanitized local report generated from SQLite;
- APP identities only for non-private apps;
- PRIVATE and SYSTEM never regain identity.

Questions are processed in memory and are not persisted.

Supported today:
- summary of today;
- most used app;
- top 5 apps;
- time for a named non-private app present in today's report;
- total app usage;
- screen-off time;
- unlock count;
- PRIVATE total;
- SYSTEM total;
- tracking coverage.

Unsupported-period questions such as yesterday/week/month return an explicit limitation instead of fabricated data.

Future:
- yesterday / 7-day / custom periods;
- trend comparisons;
- richer local intent classification;
- sanitized export fallback for complex questions.
