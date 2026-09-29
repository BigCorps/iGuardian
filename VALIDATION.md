# Validation — ConfIA.vc Android 0.1.27

## CI esperado

- versionName `0.1.27` / versionCode `28`;
- privacy/project guard PASS;
- validation lineage PASS;
- unit tests PASS;
- assembleDebug PASS;
- APK contract PASS;
- assinatura DEV fixa PASS;
- artifact `confia-android-0.1.27-fixed-signed-debug`.

## Contrato crítico desta rodada

No APK final devem estar AUSENTES:

- `com.bigcorps.guardian.web.BrowserAccessibilityService` no Manifest;
- `android.permission.BIND_ACCESSIBILITY_SERVICE`;
- `android.accessibilityservice.AccessibilityService` como componente registrado;
- INTERNET efetivo;
- VPN/MediaProjection.

Deve estar presente `com.bigcorps.guardian.ConfiaMainActivity` como launcher.

## Teste físico

1. instalar 0.1.27 por cima da 0.1.26;
2. confirmar novo ícone limpo;
3. abrir ConfIA.vc e confirmar textos “Sem Acessibilidade • sem screenshot • sem VPN”;
4. abrir ConfIA Web e confirmar Firefox/Edge + navegadores “tempo do app”;
5. abrir Inter diretamente pelo ícone original;
6. confirmar que não há necessidade de Modo Banco/religar Acessibilidade;
7. exportar pacote de validação.

## Teste físico 0.1.28

Firefox Nightly: `uol.com.br` e `github.com`, ~15 s cada. Edge Canary/Beta: `google.com` e `wikipedia.org`, ~15 s cada. Depois exportar o pacote de validação. O backend é verificado separadamente por `confia.browser_events`.

