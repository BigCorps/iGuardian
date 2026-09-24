# Data Schema — v1

## SQLite

### `intervals`

- `id INTEGER PRIMARY KEY`
- `start_ms INTEGER`
- `end_ms INTEGER`
- `type TEXT`: `APP | PRIVATE | SCREEN_OFF | ANONYMOUS_BROWSER`
- `package_name TEXT NULL`
- `app_label TEXT NULL`

Storage invariant:

- `APP` may contain `package_name` and `app_label`.
- all other interval types must have both identifying columns set to `NULL`.

### `technical_events`

Sanitized operational telemetry only:

- timestamp;
- code such as `COLLECT_OK`, `COLLECT_NO_PERMISSION`, `UNLOCK`, `PRIVATE_STARTED`, `PRIVATE_ENDED`;
- optional coarse value.

It must never contain a private package name, URL or reason for a PRIVATE interval.

## Daily JSON

Representative shape:

```json
{
  "schema_version": 1,
  "date": "2026-09-24",
  "device": {
    "name": "Meu aparelho",
    "manufacturer": "Samsung",
    "model": "...",
    "android_version": "...",
    "api_level": 36,
    "ram_total_mb": 8192,
    "storage_total_mb": 256000,
    "storage_free_mb": 80000
  },
  "summary": {
    "tracked_minutes": 312,
    "screen_off_minutes": 700,
    "private_minutes": 31,
    "anonymous_browser_minutes": null,
    "unlock_count": 42
  },
  "capabilities": {
    "app_usage": true,
    "screen_events": true,
    "browser_domains": false,
    "anonymous_browser_detection": false
  },
  "apps": [
    {
      "package": "com.example.app",
      "name": "Example",
      "foreground_seconds": 1200,
      "sessions": 4
    }
  ]
}
```

There is intentionally no field that identifies the reason or app behind PRIVATE time.

## Future domain rule

If browser-domain support is added, store only the main/registrable domain (for example `youtube.com`). Never store full URL, path, query, fragment, page title, form value or token.

## Internal report files

Daily processed reports are also written automatically inside the app-private storage using `reports/YYYY/MM/YYYY-MM-DD.json`. They are derived from SQLite and can be regenerated. The app does not require broad storage permission; manual exports use Android's document picker.
