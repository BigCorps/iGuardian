# ConfIA.vc 0.1.27 — correção visual + arquitetura web

Os prints físicos da 0.1.26 mostraram dois problemas:

1. o ícone tinha ruído/recorte inadequado no launcher Xiaomi;
2. a UI continuava descrevendo e oferecendo a arquitetura abandonada de Accessibility + screenshot/OCR + Modo Banco.

A 0.1.27 corrige ambos e torna a ausência do AccessibilityService um contrato de CI/APK.

O backend `confia.*` aplicado no Supabase foi verificado em modo leitura e está isolado dos demais apps.

## 0.1.28

Esta rodada não tenta instalar extensões silenciosamente em outros apps. O ConfIA apenas entrega os arquivos localmente e explica o fluxo suportado de teste em Firefox Nightly e Edge Canary/Beta.

