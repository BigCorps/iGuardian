# Guardian DEV — Android 0.1.17 — Selectable Trend Dashboard

## 0.1.16 physical result

The full AutoTest Suite v5 passed:

- critical_passed=true
- manual_test_required=false
- 13 PASS
- 0 WARN
- 0 FAIL
- coverage 99.4%
- report schema v4
- history-availability math PASS
- timeline privacy/overlap PASS
- app aggregate consistency PASS
- history guards PASS for 24h/7d/calendar
- AppTrendEngine v1 PASS
- technical system-surface exclusion PASS
- WorkManager active=1 with no failures/retries/stops

App trends are now real:
- last 24h has mature history and produces deterministic deltas
- last 7d is still immature and correctly refuses a comparison

## 0.1.17 product advance

### Selectable Trend Dashboard

New UI section:
**Painel de tendências**

The user can switch between:
- 24 hours
- 7 days

The selected view shows:
- general period comparison
- app trend summary
- readiness-aware output

### Per-app detail

The same dashboard lets the user type an app name, for example:

`ChatGPT`

and compare that app in the selected period.

When history is mature, the result shows:
- current usage
- previous-period usage
- signed difference

When history is incomplete, it returns `Histórico insuficiente`.

### TrendDashboardEngine v1

A dedicated local engine now owns dashboard composition and app detail.

No cloud, API or external LLM is used.

### AutoTest Suite v6

New automatic check:
`trend_dashboard_engine_v1`

It validates:
- selected 24h/7d readiness matches AppTrendEngine
- incomplete periods never show false trends
- general/app dashboard text follows readiness
- per-app detail is guarded by the same maturity rules

### Validation Pack v6

The single exported JSON now includes:

- `trend_dashboard.last_24h`
- `trend_dashboard.last_7d`

No manual dashboard test is required.

## Test flow

Install over 0.1.16.

Use normally for about 60–90 minutes.

Export only:
`guardian-validacao-*.json`

If AutoTest is green, no manual period/app-detail test is required.
