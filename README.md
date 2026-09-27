# Guardian DEV — Android 0.1.21 — Guardian Web Observer v3

## What 0.1.20 proved

The service is enabled and alive on the Xiaomi/Android 16 device:
- manager=true
- secure setting=true
- heartbeat alive
- 780 Chrome Dev accessibility events received
- 24 samples
- 0 errors

But every sample ended before a browser tree was available: 24 missing samples, last state `ROOT_NULL`.

That isolates the problem: 0.1.20 asked `rootInActiveWindow` from a HandlerThread. On this device the service connection is valid, but the active-root lookup from that delayed worker path never produced the Chrome tree.

## Observer v3

0.1.21 follows the proven browser-watcher pattern more closely:
- active window/root lookup occurs in the AccessibilityService callback/ticker context;
- URL-bar extraction is throttled to ~700 ms for content-change floods;
- timer verification remains 5 s;
- `flagIncludeNotImportantViews` is enabled;
- fallback scan is limited to 120 nodes and at most once every 3 s;
- private/incognito scan is at most once every 3 s;
- SQLite browser writes remain on a dedicated `GuardianWebStorage` thread.

So the operation that must stay tied to the live accessibility window is performed immediately, while persistence remains off the UI path.

## Physical test

Install over 0.1.20. Keep Guardian Web enabled.

1. Open one normal Chrome Dev site for ~20–30 s.
2. Open a different incognito site for ~20–30 s.
3. Return to Guardian Web.
4. It must stay responsive.
5. Diagnostic should move from ROOT_NULL to FOUND/FOUND_FALLBACK and show host count > 0.
6. Export one validation JSON.
