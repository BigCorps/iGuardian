# Store Compliance — ConfIA.vc 0.1.27

A arquitetura ativa desta build não registra AccessibilityService e não usa VpnService, MediaProjection ou NotificationListenerService.

Permissões/recursos Android relevantes:

- `PACKAGE_USAGE_STATS` — acesso concedido explicitamente pelo usuário para tempo por aplicativo;
- `RECEIVE_BOOT_COMPLETED` — restaura o agendamento local de coleta;
- sem `INTERNET` efetivo nesta build Android;
- sem `ACCESS_NETWORK_STATE` efetivo;
- sem `QUERY_ALL_PACKAGES`.

A integração de domínio será feita por extensão do navegador compatível, com disclosure próprio e coleta minimizada para host-only.

O CI final falha se um AccessibilityService voltar a aparecer no Manifest empacotado.

## Nota de teste 0.1.28

A instalação local de XPI/CRX é somente um mecanismo de desenvolvimento. Produção dependerá dos canais oficiais de distribuição/assinatura de cada navegador. O APK continua sem AccessibilityService e VPN.

