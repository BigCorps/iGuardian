# Privacy Model — Guardian 0.1.0

## Non-negotiable rule

**Privacy Engine precedes Storage.** If a context is protected, identifying fields are removed before any SQLite row or JSON field is created.

## Stored timeline types

### APP
A normal foreground app session. May store package name and best-effort human label.

### PRIVATE
Generic protected interval. Stored without package, label or reason. Banks, authentication/password tools, Android Settings, manually private apps and a detected switch to another Android user/profile all collapse to this same type.

### SCREEN_OFF
The system reported a non-interactive screen. No app identity is stored for that interval.

### ANONYMOUS_BROWSER
Reserved for a future reliable signal that the user is in an anonymous/private browser context. It is intentionally **different from PRIVATE** so a future report can say that anonymous browsing occurred without confusing it with bank/settings/another-user privacy.

Android 0.1.0 does not have a reliable UsageStats signal for incognito/private tabs, so this type is not automatically emitted yet. The product must never guess it.

## Never collected

- screenshots or video;
- typed text or keyboard events;
- passwords;
- message contents;
- notification contents;
- clipboard contents;
- banking content;
- Settings contents;
- another Android user's activity;
- full browser URLs;
- query strings, page titles or form contents.

## Private app rules

The Privacy Engine has conservative built-in package-name patterns for common settings, banking, wallet, authentication and password-manager packages. Users can also mark launchable apps as private locally.

A built-in heuristic is not considered proof that all financial apps are covered. Store/release onboarding must clearly offer a private-app selection step before the product is treated as production-ready.

## User switching

When Android emits `ACTION_USER_BACKGROUND`, Guardian starts a local generic PRIVATE interval. When it emits `ACTION_USER_FOREGROUND`, that interval closes. No other-user identity or activity is queried or stored.

Because those broadcasts are runtime-only, exact switch capture is best-effort if Android has killed the Guardian process. UsageStats remains user-scoped; Guardian must not infer missing data as another user's activity.

## Incognito/private browser mode

The product requirement distinguishes anonymous browsing from generic PRIVATE. However, Android's standard UsageStats data identifies the browser app, not a reliable incognito-tab state. Therefore 0.1.0 exposes capability `anonymous_browser_detection=false` rather than fabricating data.

A future implementation may enable `ANONYMOUS_BROWSER` only if there is a store-compatible, explicit and reliable signal.

## Support export

The diagnostic file is manually created by the device owner. To make OEM/collector debugging practical it may include a processed 24-hour snapshot of normal APP intervals, plus technical status. PRIVATE intervals remain generic and cannot reveal which protected app/context caused them. The file is never uploaded automatically.

## Consent baseline

Guardian does not intentionally backfill usage from before consent. The first successful collection after Usage Access becomes available establishes a local baseline at that moment instead of importing earlier history. When Guardian observes that Usage Access was removed, it marks the permission inactive; the next observed grant starts a new cursor baseline so the known permission-off period is not backfilled.
