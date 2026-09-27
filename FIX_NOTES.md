# Guardian Android 0.1.23 — offline merged-manifest correction

GitHub Actions #57 progressed much further than #55:

- project/privacy guard: PASS
- validation lineage: PASS
- unit tests: PASS
- Kotlin compilation: PASS
- APK build: PASS
- final APK contract: FAIL

Exact failure:
`APK final contém permissão/contrato proibido: android.permission.INTERNET`

Cause:
the bundled ML Kit OCR dependency contributes INTERNET through a transitive
library manifest even though the Guardian app manifest itself does not request it.

Correction:
- add `tools:node="remove"` for `android.permission.INTERNET`
- also remove `android.permission.ACCESS_NETWORK_STATE`
- source guard now permits those names only as merger-removal directives
- final APK guard now explicitly verifies both are absent from the built APK

No distributable APK was uploaded by #57, so version remains:
- versionName 0.1.23
- versionCode 24

Guardian Web Visual v1 remains otherwise unchanged.

Expected artifact:
guardian-android-0.1.23-fixed-signed-debug
