# Privacy Model — Guardian 0.1.21

No privacy expansion.

Accessibility remains browser-package scoped. The copied event is used only to
resolve the browser UI tree and is discarded after processing. Persisted web
data remains host + browser + interval + private-mode boolean only.

## Offline merged-manifest enforcement

Bundled OCR dependencies may declare network-related permissions in their own
library manifests. Guardian explicitly removes INTERNET and ACCESS_NETWORK_STATE
during manifest merge, and CI verifies the final APK does not contain them.
