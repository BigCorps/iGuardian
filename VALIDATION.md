# Validation — Android 0.1.11

## 0.1.10 physical result
PASS:
- report schema v3;
- millisecond fields;
- today coverage 99.4%;
- unique WorkManager present;
- 9 Worker runs / 9 successes;
- retry/failure/stopped = 0;
- no privacy/timeline regression.

Runtime intelligence self-check:
- summary_today PASS
- top_last_24h PASS
- insights_today PASS
- compare_today_yesterday FAIL

Root cause:
parser selected YESTERDAY because the comparison phrase contains `ontem`; the comparison answer route itself already handles both periods.

## 0.1.11 acceptance
- diagnostic schema 9;
- engine version 4;
- self-check overall PASS;
- custom rolling hours true;
- custom rolling days true;
- comparison check PASS;
- WorkManager remains unique/healthy;
- privacy invariants unchanged.
