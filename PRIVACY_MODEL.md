# Privacy Model — Guardian 0.1.4

## Regra não negociável

**Privacy Engine precedes Storage.**

## Tipos

- `APP`: app normal; pode ter package/label.
- `PRIVATE`: banco, Settings, autenticador, password manager, app manualmente privado e outro usuário detectado; sem identidade.
- `SCREEN_OFF`: tela não interativa; sem identidade.
- `SYSTEM`: launcher/system chooser/System UI/permission surfaces; sem identidade.
- `ANONYMOUS_BROWSER`: reservado, nunca inferido sem sinal confiável.

## Nunca coletado

Screenshots, vídeo, teclado, senha, mensagens, notificações, clipboard, conteúdo bancário, conteúdo de Settings, atividade de outro usuário, URL completa, query, título ou formulário.

## Classificadores

PRIVATE sempre tem prioridade sobre SYSTEM.

SYSTEM existe para tirar navegação técnica do ranking de apps sem perder cobertura temporal.

O launcher padrão é resolvido dinamicamente.
