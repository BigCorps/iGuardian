# Data Schema — export v2 / DB v4

## Timeline types

- `APP`: normal user-facing application; identity may be stored.
- `PRIVATE`: sensitive/private context; identity is never stored.
- `SCREEN_OFF`: non-interactive screen; no identity.
- `SYSTEM`: launcher, system chooser, package installer, system picker and other technical surfaces; no identity.
- `ANONYMOUS_BROWSER`: reserved for a future reliable signal; never guessed.

## Export precedence

When raw technical intervals overlap, reports resolve them deterministically:

`PRIVATE > ANONYMOUS_BROWSER > SCREEN_OFF > SYSTEM > APP`

Each exported millisecond belongs to at most one timeline category.

## Duration precision

0.1.6 aggregates category/app duration in milliseconds first, then converts the final aggregate to seconds. This avoids losing sub-second fragments on every individual interval.

Zero-second aggregate apps are omitted from the visible ranking.

## DB v4 migration

In addition to previous duplicate repair, DB v4 reclassifies historical APP rows belonging to known Android/OEM system surfaces as `SYSTEM` and clears package/app-label identity.

This includes known package installers, permission/system UI surfaces, Google Play Services, Xiaomi system-security components and DocumentsUI.
