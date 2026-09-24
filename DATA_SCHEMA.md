# Data Schema — v2 / DB v3

Timeline types:
- APP
- PRIVATE
- SCREEN_OFF
- SYSTEM
- ANONYMOUS_BROWSER

Only APP may persist package/name.

## Export timeline precedence

When raw technical intervals overlap, exported reports resolve them deterministically:

1. PRIVATE
2. ANONYMOUS_BROWSER
3. SCREEN_OFF
4. SYSTEM
5. APP

An exported millisecond belongs to only one category.

Unknown gaps stay unknown; adjacent entries are merged only when they truly touch.

## DB v3 migration

The upgrade from DB v2 to v3 removes:
- exact duplicate interval rows;
- duplicate UNLOCK technical rows sharing the same UsageStats timestamp.

This repairs artifacts created when 0.1.4 manual/background collectors ran concurrently.
