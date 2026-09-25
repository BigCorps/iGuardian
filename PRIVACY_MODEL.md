# Privacy Model — Guardian 0.1.9

Privacy Engine precedes Storage.

APP may contain non-private identity.
PRIVATE and SYSTEM never contain identity.
Other Android users remain generic PRIVATE.
ANONYMOUS_BROWSER is never guessed.

Local Intelligence v3 reads only sanitized report data.
Questions are not stored or transmitted.
Insights are deterministic descriptions of stored metrics and do not infer sensitive content.

DB v5 additionally sanitizes Android Photo Picker and Xiaomi App Finder historical rows as SYSTEM.

WorkManager telemetry stores only technical execution state/reason codes, never user content.
