# Validation — Android 0.1.22

Physical target:
- Chrome Dev normal: youtube + uol.com.br + globo.com, ~20–30 s each
- Chrome Dev incognito: one different host, ~20–30 s
- Guardian Web UI remains responsive
- multiple distinct hosts stored
- normal duration materially exceeds one 5-second tick
- anonymous_milliseconds > 0
- guardian_web_physical_validation PASS
- full_url_stored remains false

CI also unit-tests UOL/Globo host sanitization and incognito heuristics.
