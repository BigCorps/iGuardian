# iGuardian 0.1.7 — Build Fix 01

GitHub Actions run #21 reached unit tests successfully.

Passed:
- checkout / Java / Android SDK
- project/privacy invariant verification
- fixed DEV signing preparation
- Kotlin compilation

Failure:
- `LocalQuestionIntentParserTest > parsesScreenOff`
- 13 tests executed, 1 failed

Root cause:
The parser only recognized the literal contiguous phrase `tela desligada`.
The test uses the natural phrase `Quanto tempo a tela ficou desligada?`,
which normalizes to `quanto tempo a tela ficou desligada`.

Fix:
- SCREEN_OFF intent now recognizes `tela` + `deslig...`, even with words in between.
- Also accepts `tela apagada` and `screen off`.
- Expanded unit tests from one screen-off phrasing to four variants.
- Replaced deprecated `Locale("pt", "BR")` constructor with `Locale.forLanguageTag("pt-BR")`.

No version bump:
the 0.1.7 APK was never built/uploaded because CI stopped at unit tests.
Keep `versionName 0.1.7` / `versionCode 8`.
