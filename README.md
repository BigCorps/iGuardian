# ConfIA.vc — Android 0.1.27

## Base

Esta rodada parte do GitHub Actions #69 verde (`658e60b82b4f2e64bafedd5495cb742f213f8c10`).

O objetivo da 0.1.27 é encerrar a arquitetura Android baseada em AccessibilityService/screenshot/OCR no APK instalado e alinhar a interface ao novo POC:

- Android `UsageStats` continua medindo tempo por aplicativo;
- o domínio será fornecido pelo próprio navegador via extensão compatível;
- Supabase minhAi / schema `confia` recebe somente host + timestamps + origem;
- sem VPN;
- sem captura de tela;
- sem AccessibilityService registrado;
- bancos deixam de depender de qualquer “Modo Banco”.

## ConfIA Web — compatibilidade

### Firefox Android

É o navegador do POC principal. A extensão pode observar mudança de aba/navegação e reduzir a URL a somente o host antes de transmitir.

### Edge Android

As APIs móveis de extensão relevantes são a próxima validação. A UI marca Edge como compatível/próxima etapa, sem prometer ainda distribuição Android concluída.

### Chrome / Chrome Dev / Brave / Opera / Samsung Internet / Mi Browser

Nesta fase o Android mede o tempo do navegador como aplicativo, mas não atribui um domínio sem uma fonte tecnicamente comprovada.

## Backend já preparado

O schema `confia` no Supabase da minhAi foi aplicado e verificado em modo leitura:

- 6 tabelas;
- RLS ligado em todas;
- zero grants de tabela para `anon` / `authenticated`;
- funções acessíveis somente por `service_role`;
- zero colunas de URL completa, path, query, HTML, senha ou texto digitado;
- tabelas inicialmente vazias.

A build Android 0.1.27 ainda não envia dados ao backend. O próximo passo é o endpoint server-only + extensão Firefox.

## Branding

O ícone foi refeito a partir do símbolo original fornecido pelo usuário. A extração anterior deixava ruído do fundo branco; a nova versão usa máscara baseada em cor/saturação e padding seguro para launcher/adaptive icon.

O package interno permanece `com.bigcorps.guardian.dev` para preservar atualização, assinatura e histórico local.
