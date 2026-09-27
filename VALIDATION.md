# Validation — Android 0.1.25

## CI/static gate

Expected before installing:

- versionName 0.1.25 / versionCode 26;
- privacy/project guard PASS;
- validation lineage PASS including `privacy_classification_core`;
- unit tests PASS;
- Kotlin/APK build PASS;
- final APK contains neither INTERNET nor ACCESS_NETWORK_STATE;
- fixed DEV certificate remains unchanged;
- artifact: `guardian-android-0.1.25-fixed-signed-debug`.

## Clean Guardian Web physical test — Redmi / Android 16

1. Install over 0.1.24 and re-enable Guardian Web if Bank Mode left it OFF.
2. Open Guardian Web and tap **Iniciar teste limpo**.
3. Confirm the site list is empty/reset.
4. Chrome Dev normal: `uol.com.br` for ~20 seconds.
5. Chrome Dev normal: `globo.com` for ~20 seconds.
6. Open a new incognito tab and remain on its start page for ~8 seconds.
7. Still incognito, visit `github.com` for ~20 seconds.
8. Return to Guardian Web.

Acceptance:

- no legacy/junk rows such as `kit`, `fallback`, `https`, `app`, `quiser`;
- new normal hosts appear after the clean reset;
- `visual_private_probe_count > 0`;
- `visual_private_detected_count > 0` OR another strong private signal is present;
- anonymous browser milliseconds/rows > 0;
- screenshot/OCR pipeline has no persistent/raw content;
- if the tree remains 0 hits on this Xiaomi, the report must state that clearly rather than blocking visual fallback.

## Financial protection test

1. With Guardian Web ON, confirm **Inter Empresas** appears under Modo Banco.
2. Tap **Abrir Inter Empresas com proteção**.
3. Confirm Guardian waits until AccessibilityManager=false and secure-setting=false.
4. Confirm Inter opens only after that state.
5. Return to Guardian: normal Usage Access tracking should still exist; Guardian Web should remain OFF.
6. Re-enable Guardian Web manually only after banking is finished.
7. Optional single failsafe test: open Inter directly after re-enabling Guardian Web and confirm the service disables itself shortly afterwards.

## Export

After the web test and bank test, export one validation JSON. The JSON should
contain only data generated after the clean web reset for Guardian Web evidence.
