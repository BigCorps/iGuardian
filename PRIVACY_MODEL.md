# Privacy Model — ConfIA.vc 0.1.27

## Android

O APK mantém coleta local de tempo por aplicativo via Usage Access.

A build ativa não registra AccessibilityService, não usa screenshot/OCR e não usa VPN.

Apps financeiros continuam classificados como PRIVATE pelo classificador local, mas não existe mais Modo Banco: como não há serviço de Acessibilidade registrado, o usuário abre o banco normalmente.

## ConfIA Web POC

O navegador compatível será a fonte do domínio.

Antes de qualquer transmissão, a extensão deve transformar a URL completa em apenas host, por exemplo:

`https://github.com/BigCorps/iGuardian/issues?x=1` → `github.com`

O backend não possui colunas para URL completa, path, query, fragment, título, HTML, senha ou texto digitado.

## Supabase

O schema `confia` é server-only no POC. `anon` e `authenticated` não possuem grants diretos nas tabelas. Tokens de ingestão são armazenados somente como SHA-256.

## POC 0.1.28

O APK não recebe URL nem domínio pela rede. Os tokens temporários ficam somente dentro dos pacotes binários XPI/CRX desta rodada e serão revogados após o teste. A extensão reduz URL para host antes de transmitir; path, query, título, HTML e texto digitado não fazem parte do payload.

