# Guardian DEV — Android 0.1.12 — AutoTest Bundle

This package supersedes the earlier not-yet-uploaded 0.1.12 ZIP. Version remains `0.1.12 / 13`.

## Purpose
Reduce manual test rounds. A single validation export runs the automatic suite and packages the evidence in one JSON.

## Included product advances
- calendar day and inclusive calendar ranges;
- last 24h vs previous 24h factual trend;
- automatic local insight card;
- automatic 24h trend card;
- existing question UI remains available.

## AutoTest Suite v1
Checks automatically:
1. privacy/permission contract;
2. report schema v3 and millisecond precision;
3. report arithmetic consistency;
4. timeline overlap, duration and non-APP identity;
5. app aggregate uniqueness/order/total;
6. Local Intelligence runtime self-check;
7. WorkManager unique periodic state and health;
8. tracking coverage.

Returns PASS/WARN/FAIL plus `manual_test_required`.

## One-file validation
Tap **Exportar pacote de validação (recomendado)**.

It creates `guardian-validacao-YYYYMMDD-HHmmss.json` containing:
- validation suite;
- fixed automatic insights;
- daily report;
- diagnostic.

Existing daily and diagnostic exports remain available.

## Test workflow
Install over 0.1.11, use normally for ~60–90 minutes, export the validation pack and send that single JSON. Manual feature-by-feature testing is only needed if `manual_test_required=true`.
