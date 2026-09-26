# Local Intelligence v4 — 0.1.11

100% local, deterministic, offline and non-persistent.

## Fixed periods
- today
- yesterday
- last 24 hours
- last 7 days

## Flexible rolling periods
- last N hours: 1–720
- last N days: 1–90

Examples:
- Top 5 das últimas 6 horas
- Insights dos últimos 3 dias
- Quanto tempo usei o ChatGPT nas últimas 4 horas?
- Qual a cobertura dos últimos 2 dias?

## Comparison fix

`Compare hoje com ontem` is a special two-period intent. The parser now marks TODAY as the comparison reference instead of allowing the word `ontem` to relabel the whole plan.

## Self-check

Runtime self-check covers:
- today summary;
- last 24h top apps;
- custom 6h;
- custom 3d;
- today-vs-yesterday comparison.

Only internal check IDs/pass-fail are exported.
User questions remain unlogged.
