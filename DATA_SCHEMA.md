# Data Schema — ConfIA.vc 0.1.27

## SQLite Android

Sem migration nesta rodada. O database local permanece versão 8 para preservar histórico já coletado.

As antigas sessões web podem ser apagadas manualmente pela nova tela ConfIA Web; elas são tratadas como legado do método OCR/Acessibilidade.

## Supabase minhAi

Schema remoto: `confia`.

Tabelas:

- `accounts`
- `installations`
- `browser_sources`
- `browser_events`
- `foreground_intervals`
- `domain_sessions`

As tabelas foram aplicadas e verificadas antes desta build. Não há colunas para conteúdo de página ou URL completa.

## POC de navegador 0.1.28

Firefox e Edge usam `browser_sources` distintos ligados à mesma `installation`. `browser_events` recebe somente host/timestamps/estado privado quando disponível.

