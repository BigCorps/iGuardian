# Privacy Model — Guardian 0.1.19

## Guardian Web exception

The general prohibition on AccessibilityService now has one explicit narrow exception: the optional Guardian Web browser observer.

It is allowed only because:
- separate in-app consent is required;
- Android user activation is required;
- XML packageNames restricts events to supported browsers;
- the code only extracts the browser address field and strong private-mode browser UI markers;
- the value is reduced to host before database insertion;
- focused address fields are ignored to avoid typed/search text;
- no page content/title/path/query/fragment is persisted;
- no network transport exists.

PRIVATE app identity remains unavailable. Guardian Web does not inspect non-browser apps.
