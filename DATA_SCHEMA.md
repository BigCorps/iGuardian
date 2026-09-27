# Data Schema — report v5 / DB v8 / diagnostic v18 / validation-pack v8

DB v8 adds `browser_sessions` with only:
- start/end time
- sanitized host
- supported browser package
- private-mode boolean

No full URL column exists.

Report v5 adds a `browser` overlay with host aggregates and normal/anonymous milliseconds. The overlay is not additive to APP total.

Diagnostic v18 adds Guardian Web consent/service/storage audit evidence.

Validation pack v8 adds top-level `guardian_web` + the one-round physical test contract.
