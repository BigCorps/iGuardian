# Data Schema — v2

## SQLite

`intervals.type`:
`APP | PRIVATE | SCREEN_OFF | SYSTEM | ANONYMOUS_BROWSER`

Somente `APP` pode armazenar `package_name` e `app_label`.

`PRIVATE`, `SCREEN_OFF`, `SYSTEM` e `ANONYMOUS_BROWSER` devem persistir identidade nula.

## Daily JSON v2

Seconds are authoritative:

```json
{
  "tracking": {
    "tracking_started_at": "...",
    "effective_period_start": "...",
    "effective_period_seconds": 8000,
    "recorded_seconds": 7870,
    "unclassified_seconds": 130,
    "coverage_percent": 98.4
  },
  "summary": {
    "app_usage_seconds": 2610,
    "app_usage_minutes": 43,
    "screen_off_seconds": 5224,
    "screen_off_minutes": 87,
    "private_seconds": 6,
    "private_minutes": 0,
    "system_seconds": 176,
    "system_minutes": 2,
    "anonymous_browser_seconds": null,
    "anonymous_browser_minutes": null,
    "unlock_count": 13
  }
}
```

`SYSTEM` representa navegação técnica do Android sem identidade exportada.

## Future browser rule

Quando houver suporte, guardar apenas domínio principal/registrável. Nunca URL completa, path, query, fragment, título, formulário ou token.
