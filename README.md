# Guardian DEV — Android 0.1.19 — Guardian Web v1

## 0.1.18 physical result

The compact validation architecture passed. The recommended pack dropped from ~768 KB to ~26 KB while retaining all green evidence. Validation lineage also passed.

The only warning was historical WorkManager stop telemetry after BOOT_COMPLETED. Android stop reason 13 is `STOP_REASON_USER`; the same unique work later completed successfully, with no retry/failure and remained ENQUEUED. 0.1.19 therefore keeps the telemetry but classifies a later successful completion as recovered.

## Guardian Web v1

0.1.19 intentionally introduces one narrow exception to the previous no-Accessibility rule. Guardian Web is optional, requires a separate prominent disclosure/consent, and the service is limited in XML to supported browser packages.

It observes only browser chrome needed to read the address bar and strong private/incognito UI indicators. Before any persistence, a full URL is reduced to a sanitized host.

Example:

`https://www.google.com/search?q=segredo#x` -> `google.com`

Never stored:
- path
- query/search terms
- fragment
- page title
- page content
- typed text/passwords
- data from non-browser apps

The app still has no INTERNET permission.

### Initial browser adapters

- Chrome
- Chrome Dev
- Brave
- Microsoft Edge
- Vivaldi
- Opera
- Firefox
- Samsung Internet
- DuckDuckGo

Chrome Dev is the physical validation target for this round. Other adapters remain best-effort until individually exercised.

### Anonymous/incognito

Private mode is never inferred from color or network traffic. It is marked only when strong browser-owned accessibility labels/resource indicators are present. The host remains local and is marked anonymous in the separate browser overlay.

Browser data is an overlay and is not added to APP time a second time.

## Validation

ValidationSuite v8 adds:
- Guardian Web host-only privacy contract
- browser arithmetic/storage audit
- physical normal + anonymous evidence check
- recovered WorkManager stop semantics

A successful physical test should produce no FAIL and `guardian_web_physical_validation=PASS`.

## Physical test

1. Open Guardian Web and consent.
2. If sideload restrictions appear, allow restricted settings for Guardian.
3. Enable Guardian Web in Accessibility.
4. In Chrome Dev normal mode, visit one domain for ~20 seconds.
5. In Chrome Dev incognito mode, visit a different domain for ~20 seconds.
6. Return to Guardian Web; both hosts should appear and the second should show anonymous time.
7. Use the phone normally for another 20–30 minutes and export one recommended validation JSON.
