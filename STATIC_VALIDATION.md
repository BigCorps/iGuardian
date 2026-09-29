# Static validation — ConfIA.vc Android 0.1.27

Baseline: GitHub Actions #69 green on commit `658e60b82b4f2e64bafedd5495cb742f213f8c10`.

Static contract for this patch:

- launcher is `ConfiaMainActivity`;
- AndroidManifest contains no Accessibility service component;
- ConfIA Web UI contains Firefox/Edge compatibility and no legacy activation/Modo Banco path;
- INTERNET and ACCESS_NETWORK_STATE remain removed from final merge;
- VpnService / MediaProjection remain forbidden;
- root and Android asset validation-contract manifests are identical;
- workflow artifact is `confia-android-0.1.27-fixed-signed-debug`;
- launcher/adaptive icons use the clean ConfIA mark with safe padding.

Authoritative Android compile remains GitHub Actions.
