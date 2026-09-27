# Guardian DEV — Android 0.1.25

## Current baseline

0.1.24 is confirmed green in GitHub Actions #64. Its physical JSON on the
Redmi/Android 16 device showed that accessibility-tree extraction still produced
618 probes and zero host hits, while screenshot/OCR completed 78 times and
produced 53 host-shaped readings. The same JSON produced zero private/incognito
detections and preserved browser rows from earlier rounds, making the on-screen
list unsuitable as clean evidence.

0.1.25 responds directly to that physical evidence instead of adding another tree
strategy.

## Guardian Web v4 — hardened visual fallback

The hybrid order remains tree first, OCR fallback, but the Xiaomi result now has
an explicit interpretation: zero tree hits do not block the proven visual path.

Visual-only host acceptance is stricter than tree acceptance. OCR candidates must
look like plausible public hosts, so single words and implausible suffix artifacts
cannot be persisted. Tree values continue to use the general URL sanitizer.

Private-mode OCR now recognizes Chromium's current redesigned incognito start-page
text in pt-BR and English. Host OCR is limited to the upper 16% to avoid page-body words; the deep private probe covers 90% of the display while
the host crop remains small.

## Clean test button

Guardian Web now has **Iniciar teste limpo**. It stops/banks the active browser
interval, resets transient host/private state, lets the queued final write finish,
then clears only Guardian Web sessions and runtime evidence. Normal app history
is untouched.

Use this before every physical web-validation round so old versions cannot pollute
the result.

## Financial apps / Modo Banco

0.1.25 adds `FinancialAppCatalog` and PrivacyClassifier v3. Confirmed Inter packages:

- Inter Empresas: `br.com.Inter.CDPro`;
- Inter: `br.com.intermedium`.

Guardian Web lists recognized financial apps installed on the device. **Abrir
<banco> com proteção** performs this order:

1. stop Guardian Web collection;
2. call Android `AccessibilityService.disableSelf()`;
3. wait until AccessibilityManager and secure settings both report Guardian Web OFF;
4. only then launch the selected financial app.

If a recognized financial app is opened directly, a UsageStats failsafe requests
shutdown as soon as that foreground package is observed. This is supplemental,
not equivalent to protected launch, because direct detection necessarily happens
after the financial process has started.

Normal app-usage collection remains independent through Usage Access. Android
requires explicit user action to re-enable Guardian Web afterwards.

## Privacy / offline contract

- no INTERNET permission;
- no ACCESS_NETWORK_STATE permission;
- no QUERY_ALL_PACKAGES;
- no screenshot/raw OCR persistence;
- no full URL/page/input persistence;
- no bank/account/Pix/transaction-content persistence;
- database schema remains v8.

## Next physical validation

After Actions builds 0.1.25:

1. install over 0.1.24;
2. re-enable Guardian Web if needed;
3. tap **Iniciar teste limpo**;
4. Chrome Dev normal: `uol.com.br` ~20s, then `globo.com` ~20s;
5. open incognito and wait ~8s on the start page;
6. while still incognito, visit `github.com` ~20s;
7. verify only fresh hosts appear and anonymous time is > 0;
8. use **Abrir Inter Empresas com proteção**;
9. return and export one validation JSON.
