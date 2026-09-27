# Guardian — Modo Banco 0.1.25

## Goal

Some financial apps can treat an enabled AccessibilityService as a device-risk
signal. Guardian therefore removes the Guardian Web capability before a protected
financial launch instead of trying to hide it from the bank.

## Financial classification

`FinancialAppCatalog` is a narrower subset of the broader PRIVATE policy.
Confirmed packages include:

- Inter Empresas: `br.com.Inter.CDPro`;
- Inter: `br.com.intermedium`;
- Nubank: `com.nu.production`;
- InfinitePay: `io.cloudwalk.infinitepaydash`.

Additional banks/wallets can be recognized by conservative package/label patterns.
Financial apps are also classified as PRIVATE by PrivacyClassifier v3.

## Protected launch — preferred path

1. Guardian Web enumerates launcher-visible financial apps already installed.
2. User taps **Abrir <banco> com proteção**.
3. Guardian stops/banks the current web interval and clears transient Hybrid state.
4. Guardian calls Android `AccessibilityService.disableSelf()`.
5. Guardian polls both AccessibilityManager and Android's secure enabled-service setting.
6. Only when both report Guardian Web OFF does Guardian launch the selected bank.
7. Normal app-level monitoring continues through Usage Access.
8. Guardian never silently re-enables Accessibility afterwards.

## Direct-launch automatic failsafe

If the user opens a recognized financial app directly, Guardian Web checks the
foreground package through UsageStats and requests `disableSelf()` when the bank
is observed. This is **best effort only** because the financial process has already
started by the time UsageStats can report it. It is not a replacement for
**Abrir com proteção**.

## Privacy boundary

- Guardian Web remains package-scoped to browsers and does not inspect bank screens.
- Financial classification uses package/launcher label only.
- `WEB_BANK_MODE_DISABLE` may record only a generic reason such as manual/protected
  launch or automatic financial foreground; it never records account, screen,
  credential, Pix or transaction content.
- No new network permission is introduced.

## Physical test

First test `Abrir Inter Empresas com proteção`. Confirm the bank opens only after
Guardian Web is OFF. If desired, perform one separate direct-open test after
reactivating Guardian Web to observe whether the automatic failsafe disables it.
Do not repeatedly perform banking transactions merely to exercise the failsafe.
