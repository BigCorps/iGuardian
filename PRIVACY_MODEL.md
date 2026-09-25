# Privacy Model — Guardian 0.1.6

## Non-negotiable rule

**Privacy Engine precedes Storage.**

## Types

- `APP`: normal app; package/label may be stored.
- `PRIVATE`: bank/settings/authenticator/password manager/manual private app/other Android user; no identity.
- `SCREEN_OFF`: screen non-interactive; no identity.
- `SYSTEM`: launcher/system chooser/permission/package-installer/document-picker/technical system surface; no identity.
- `ANONYMOUS_BROWSER`: reserved; never inferred without a reliable signal.

## Priority

`PRIVATE > ANONYMOUS_BROWSER > SCREEN_OFF > SYSTEM > APP`

Privacy therefore wins even when Android emits overlapping technical events.

## Other Android users/profiles

On owner-user background:
- owner history is collected only up to the switch boundary;
- an anonymous PRIVATE interval begins.

On owner-user foreground:
- PRIVATE is closed;
- collector cursor advances to the return boundary;
- UsageStats from the other profile are not replayed into owner history.

## Never collected

Screenshots, video, keyboard input, passwords, messages, notification content, clipboard, banking content, Settings content, full URLs, URL query strings, form content, or identifiable activity from another Android user/profile.

## Historical sanitation

DB migrations may make privacy stricter. DB v4 reclassifies known historical technical/system APP rows to sanitized SYSTEM and removes stored package/label identity.
