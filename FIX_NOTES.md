# Build Fix 02

O segundo GitHub Actions avançou além do setup do SDK e falhou em `Unit tests` durante `:app:compileDebugKotlin`.

Erro exato:
`GuardianDatabase.kt:57:62 Unsupported escape sequence.`

Correção:
`Regex("[^A-Z0-9_\\-]")`
foi substituído por:
`Regex("[^A-Z0-9_-]")`

Em uma classe de caracteres de regex, o hífen colocado no final não precisa de escape.
O escape `\-` dentro de uma string Kotlin comum era inválido.

Arquivos deste pacote:
- `app/src/main/java/com/bigcorps/guardian/core/GuardianDatabase.kt`
- `README.md`
- `CHANGELOG.md`
- `PROJECT_STATE.json`

Substitua esses arquivos mantendo os mesmos caminhos e faça commit na `main`.
