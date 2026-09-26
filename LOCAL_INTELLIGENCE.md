# Local Intelligence v8 + App Trend Engine v1 — 0.1.16

## History-safe comparisons
- today vs yesterday
- last 24h vs previous 24h
- last 7d vs previous 7d
- calendar day/range comparisons

## New per-app trend layer

AppTrendEngine compares sanitized APP aggregates across equivalent periods.

Supported:
- last 24h vs previous 24h
- last 7d vs previous 7d

Rules:
- requires HistoryReadiness
- ignores deltas below 1 minute
- top increases/decreases only
- no behavioral score or judgment
- no query persistence/network
