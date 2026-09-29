# Upload — ConfIA.vc Android 0.1.27

Base esperada no GitHub: commit `658e60b82b4f2e64bafedd5495cb742f213f8c10` / Actions #69 verde.

Suba TODO o conteúdo deste ZIP preservando os caminhos.

Pontos principais para conferir no GitHub antes de esperar o Actions:

- `app/build.gradle.kts`: 0.1.27 / versionCode 28;
- `app/src/main/AndroidManifest.xml`: launcher `.ConfiaMainActivity` e NENHUM `BrowserAccessibilityService`;
- `app/src/main/java/com/bigcorps/guardian/ConfiaMainActivity.kt` existe;
- `app/src/main/java/com/bigcorps/guardian/web/BrowserWebActivity.kt` cita Firefox/Edge e não possui Modo Banco/Acessibilidade;
- workflow artifact: `confia-android-0.1.27-fixed-signed-debug`;
- os arquivos `ic_launcher*` foram substituídos.

Não rode novamente o SQL de fundação apenas por causa deste ZIP. O schema `confia` já foi aplicado e verificado.

Depois do Actions verde, instale a 0.1.27 por cima da 0.1.26.

## Upload 0.1.28

Suba todos os arquivos deste ZIP preservando caminhos. Os dois arquivos em `app/src/main/assets/` são obrigatórios para o APK conseguir exportar as extensões no celular. Artefato esperado: `confia-android-0.1.28-fixed-signed-debug`.

