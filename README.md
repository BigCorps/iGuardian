# Guardian DEV — Android 0.1.14 — Data Maturity + AutoTest v3

## Why this build exists

0.1.13 passed all automated checks, but the new trend cards revealed an
important product-correctness issue: a comparison can be mathematically valid
over the data that exists while still being unfair if part of the requested
period predates Guardian tracking.

Example observed on the physical device:
- last 7d had data;
- previous 7d had no historical data;
- the old card still showed a numeric delta.

0.1.14 fixes that class of problem.

## Report schema v4

Tracking now distinguishes:
- requested period;
- history actually available for that requested period;
- classification coverage inside the available history.

New fields:
- requested_period_milliseconds/seconds
- history_available_milliseconds/seconds
- history_availability_percent

`coverage_percent` keeps its existing meaning: classified data divided by
available history.

## Local Intelligence v7

Comparisons require:
- >=99% history availability in both periods;
- >=90% classification coverage in both periods.

Otherwise Guardian returns `Histórico insuficiente...` instead of a misleading
numeric difference.

This applies to:
- today vs yesterday;
- 24h vs previous 24h;
- 7d vs previous 7d;
- calendar-period comparisons.

## System-surface cleanup

`com.android.providers.downloads.ui` is now treated as SYSTEM rather than a
user APP. DB version 6 repairs already-stored historical rows and removes
identity, matching other Android document/download surfaces.

## AutoTest Suite v3

Adds:
- report-v4 history availability arithmetic;
- dynamic comparison history guard;
- known system-surface exclusion.

Validation pack schema v3 includes `data_maturity`.

## Test

Install over 0.1.13 without uninstalling.
No manual question testing is needed.

Use normally for ~60–90 minutes, export one recommended validation pack and
send only that JSON.
