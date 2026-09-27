# Upload desta correção

Extraia o ZIP e envie os arquivos mantendo exatamente os caminhos relativos à raiz do repositório.

## Arquivo crítico do CI

Confirme especialmente que este arquivo foi substituído no GitHub:

`scripts/verify_project.py`

Antes desta correção, Actions #61 e #63 falharam porque a `main` ainda continha a versão antiga desse script.

## Arquivos funcionais do Modo Banco

- `app/src/main/java/com/bigcorps/guardian/web/BrowserAccessibilityService.kt`
- `app/src/main/java/com/bigcorps/guardian/web/BrowserWebActivity.kt`

A versão permanece 0.1.24 / versionCode 25 porque ainda não houve um APK 0.1.24 validado com sucesso.
