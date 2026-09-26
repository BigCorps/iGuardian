# Local Intelligence v8 — 0.1.15

All comparison families are now history-maturity aware.

Protected comparisons:
- today vs yesterday
- last 24h vs previous 24h
- last 7d vs previous 7d
- calendar day vs day
- calendar range vs range

Minimums:
- 99% requested-history availability in both periods
- 90% classified-data coverage in both periods

Incomplete periods return `Histórico insuficiente...` instead of a misleading
numeric delta.

Questions remain local and non-persistent.
