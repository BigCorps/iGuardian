# CHANGELOG

## 0.1.28 — POC sem USB: Firefox + Edge

- Base: Actions #71 verde (`320cdb8…`).
- Incorpora `confia-web-firefox-poc-0.1.1.xpi` e `confia-web-edge-poc-0.1.1.crx` dentro do APK.
- ConfIA Web salva os dois arquivos em `Downloads/ConfIA` sem computador, cabo ou ADB.
- Firefox: teste local em Firefox Nightly com menu de desenvolvimento/instalação por arquivo.
- Edge: teste local em Edge Canary/Beta via `Extension install by crx`.
- Extensões desta rodada já vêm pareadas com tokens temporários separados e apontando para `confia-web-ingest`.
- APK Android continua sem INTERNET, AccessibilityService, screenshot/OCR ativo ou VPN.
- Depois do teste, os tokens POC devem ser revogados no Supabase.

# Changelog

## 0.1.27 — 2026-09-29

- versionCode 28 / versionName 0.1.27; base Actions #69 verde.
- Corrigido ícone ConfIA.vc: nova extração limpa do símbolo original e padding seguro para launcher/adaptive icon.
- Nova `ConfiaMainActivity` passa a ser o launcher; a antiga `MainActivity` não é mais registrada.
- Interface principal alinhada à arquitetura real: sem Acessibilidade, screenshot ou VPN.
- `BrowserWebActivity` reescrita para mostrar compatibilidade por navegador:
  - Firefox Android: POC principal por extensão;
  - Edge Android: compatível / próxima validação;
  - Chrome/Chrome Dev e demais: tempo do navegador apenas nesta fase.
- `BrowserAccessibilityService` removido do AndroidManifest.
- Modo Banco aposentado da arquitetura ativa.
- CI/APK contract agora falham se AccessibilityService/BIND_ACCESSIBILITY_SERVICE reaparecerem no pacote final.
- Histórico web antigo pode ser apagado pela nova tela.
- Exportações novas vão para `Downloads/ConfIA`.
- Schema `confia` no Supabase minhAi verificado: RLS em todas as tabelas, zero grants de cliente e zero colunas de conteúdo/URL completa.
