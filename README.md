# Guardian DEV — Android 0.1.13 — Trends AutoTest

## 0.1.12 AutoTest result

The one-file validation pack passed completely:

- critical_passed=true
- manual_test_required=false
- 8 PASS / 0 WARN / 0 FAIL
- coverage 99.7%
- report schema v3
- exact millisecond arithmetic
- zero timeline overlap
- zero non-APP identity leak
- exact APP aggregate sum
- Local Intelligence self-check PASS
- one active unique WorkManager
- 29 Worker runs / 29 successes / 0 retry / 0 failure / 0 stopped

## Local Intelligence v6

New:
- last 7 days vs previous 7 days
- calendar day vs calendar day
- calendar range vs calendar range

Examples:
- `Compare os últimos 7 dias com os 7 anteriores`
- `Compare 24/09 com 25/09`
- `Compare 22/09 a 23/09 com 24/09 a 25/09`

Comparisons report factual differences in app-use duration, leading app,
unlocks and coverage.

## Automatic insights

Home now calculates automatically:
1. today's deterministic insights
2. last 24h vs previous 24h
3. last 7d vs previous 7d

No manual question is required.

## AutoTest Suite v2

Keeps all v1 checks and adds a product-capability regression check for
Intelligence v6.

Validation pack schema v2 includes the automatic 7-day trend.

## Test

Install over 0.1.12 without uninstalling.

No feature-by-feature manual test is needed.

Use normally for ~60–90 minutes, then export only:

`guardian-validacao-*.json`

Manual reproduction is required only if the pack returns a critical failure.
