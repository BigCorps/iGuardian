# Data Schema — report v4 / DB v7 / diagnostic v16 / validation-pack v6

No report or database schema change.

Diagnostic v16 adds TrendDashboardEngine capabilities.

Validation pack v6 adds:
- trend_dashboard.last_24h
- trend_dashboard.last_7d

Dashboard JSON includes:
- readiness
- history availability
- coverage
- general trend text
- app trend text

User-entered app detail queries are not persisted.
