# Guardian — Modo Banco

## Purpose

Some financial apps may treat an enabled AccessibilityService as a device-risk signal even when Guardian is package-scoped to browsers and never reads the banking app. Modo Banco removes that capability before the user opens a financial app.

## Behavior

1. User opens Guardian Web and taps **Ativar Modo Banco**.
2. Guardian stops/banks the active browser interval and clears transient Hybrid state.
3. Guardian Web calls Android `AccessibilityService.disableSelf()`.
4. UI checks the Android AccessibilityManager and secure accessibility setting.
5. Success is shown only when both no longer report Guardian Web enabled.
6. The user can then open the financial app normally.
7. App-level Guardian monitoring through Usage Access remains active.
8. After banking, the user explicitly re-enables Guardian Web in Android Accessibility settings if web monitoring is desired again.

## Why preventive

Guardian does not attempt to wait until a bank is already foreground. A bank may evaluate device risk immediately during startup, so disabling Accessibility after launch can be too late.

## Security/privacy boundary

- Modo Banco does not inspect banking-app screens.
- It does not spoof or hide Guardian from another app.
- It actually disables the Guardian Web AccessibilityService.
- It does not persist bank credentials, bank screen content or banking activity details.
- It does not disable the independent Usage Access collector.

## Physical validation

Test first with Inter/Inter Empresas because that device produced the real warning. Verify Android shows Guardian Web OFF before starting the bank. Repeat later with a Play-distributed build to distinguish sideload-related risk from Accessibility-related risk.
