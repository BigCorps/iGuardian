# Guardian DEV — Android 0.1.18 — Lean Validation + Lineage

## 0.1.17 physical result

The one-file AutoTest passed completely:

- `critical_passed=true`
- `manual_test_required=false`
- 14 PASS
- 0 WARN
- 0 FAIL
- coverage 99.2%
- 1173 timeline intervals validated
- 24h trend/dashboard ready with 100% vs 100% history
- 7d trend correctly blocked at 29.9% vs 0%
- WorkManager 38/38 successes, zero retry/failure/stopped

## Why 0.1.18 exists

The validation JSON had grown to roughly 768 KB because it embedded:
1. the full daily timeline; and
2. another full 24h activity snapshot inside diagnostics.

Those large sections were useful while stabilizing the timeline, but they are
redundant on a green build because ValidationSuite has already checked them.

## Lean validation pack v7

Recommended export now uses two modes:

### `compact_success`
When all AutoTests pass:
- full timelines are omitted;
- report summaries/apps/tracking remain;
- timeline evidence/counts remain;
- scheduler, permissions, capabilities, privacy guarantees and self-checks remain;
- app trends/dashboard remain;
- validation lineage remains.

Expected size is tens of KB rather than hundreds of KB.

### `full_failure_evidence`
If any critical check fails:
- full daily timeline is kept automatically;
- full 24h diagnostic snapshot is kept automatically.

No user decision is required.

## Export feedback

The recommended export button now contains a circular indeterminate loader while
the package is being processed and is disabled until the operation finishes.

## Validation lineage

0.1.18 adopts a pattern already proven in the other BigCorps repositories:
stable subsystems can inherit physical validation only when their source hashes
are unchanged.

Inherited from physically validated 0.1.17:
- privacy/collection core
- database/report core
- background scheduler core
- local intelligence core

Retested automatically in 0.1.18:
- validation/export/UI

CI-only validation:
- build pipeline

`scripts/verify_validation_contracts.py` fails the build if an inherited source
changes without an intentional contract update.

## Final APK guard

After Gradle builds the APK, CI now checks the finished artifact itself:
- applicationId
- versionName
- versionCode
- absence of prohibited permissions/services

The fixed signing certificate verification remains separate and unchanged.

## Test flow

Upload 0.1.18.

If Actions is green, install directly over 0.1.17.

Use normally for ~60–90 minutes and export only the recommended validation JSON.

The loader should be visible during processing, and a green pack should be much
smaller/faster to save and share.
