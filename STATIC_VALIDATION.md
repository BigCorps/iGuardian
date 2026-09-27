# Static validation — 0.1.24 Hybrid v3

Prepared against repository HEAD `aaad03f8527ee2ce3a19a58ee2cfb17f166a327c`
(the 0.1.23 state whose Android APK Actions #59 passed).

Changed contract:
- version 0.1.24 / code 25;
- tree-first URL extraction with no visibility requirement for address bars;
- bounded worker-tree fallback;
- OCR remains fallback;
- API 34+ per-window screenshot diagnostic;
- resource-ID-only tree telemetry;
- expanded browser allowlist/catalog;
- corrected private-probe counters;
- positive private → normal Chromium transition evidence and separate normal/private marker telemetry;
- same-timestamp BrowserWebAccess snapshot reuse.

Unchanged privacy contract:
- host-only browser storage;
- no raw screenshot/OCR/tree text persistence;
- no INTERNET;
- no ACCESS_NETWORK_STATE;
- no QUERY_ALL_PACKAGES;
- no database schema expansion.

Local structural Kotlin validation completed for the Hybrid service/extractor/
preferences/access/report/Guardian Web UI group using Android API stubs, and
BrowserVisualOcr was compiled separately against ML Kit stubs. Behavioral harnesses
also passed for: hidden-toolbar URL extraction, focused-field rejection, bounded
fallback extraction, positive Chromium normal-mode transition evidence, deep private OCR after a host is already known, corrected
private-probe counters, per-browser resource-ID grouping, secure-window telemetry,
and same-timestamp accessibility status snapshot reuse. Browser catalog runtime
checks and JSON/lineage/XML/workflow invariants also passed. This process caught
and corrected implementation errors before packaging.

The authoritative Android SDK/Gradle/APK/merged-manifest check remains GitHub
Actions because this artifact environment has no networked Android/Gradle dependency
resolution.
