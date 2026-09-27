# Data Schema — report v4 / DB v7 / diagnostic v17 / validation-pack v7

Report schema remains v4.
Database remains v7.

Diagnostic v17 can omit the heavy activity snapshot when used as a validation
source. Explicit full diagnostic export still includes it.

Validation pack v7 adds:
- `pack_meta`
- `generation_metrics`
- `validation_lineage`
- compact report timeline evidence on successful builds
- automatic full evidence on failed builds
