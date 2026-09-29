# ConfIA.vc Roadmap

## Android — atual: 0.1.27

### Gate A — retirar a arquitetura de Acessibilidade do APK

- `BrowserAccessibilityService` não pode estar registrado no Manifest final;
- `BIND_ACCESSIBILITY_SERVICE` não pode aparecer no APK;
- nenhum fluxo de UI deve pedir Acessibilidade;
- Modo Banco deixa de ser necessário;
- Inter e outros bancos devem abrir normalmente pelo ícone original.

### Gate B — POC Firefox + Supabase

1. criar endpoint server-only de ingestão;
2. gerar pairing/token por `confia.bootstrap_test_pair`;
3. extensão Firefox observa aba/navegação;
4. URL é reduzida localmente para host;
5. somente host + timestamps chegam a `confia.browser_events`;
6. validar navegação normal e privativa;
7. cruzar com UsageStats do Android.

### Gate C — Edge Android

Portar o mesmo protocolo para Edge, validar instalação/distribuição móvel e comparar eventos de `tabs`/`webNavigation`.

### Gate D — navegadores sem extensão comprovada

Manter somente tempo por app. Não inferir domínio por DNS, OCR ou heurística como se fosse página ativa.

## Depois do POC

- restaurar/modernizar os painéis avançados dentro da nova UI ConfIA;
- landing `confia.vc`;
- login/pagamentos como app filho da minhAi;
- múltiplos dispositivos por conta master;
- Windows.
