# Guardian DEV — Android 0.1.4

Codinome temporário do futuro produto BigCorps atualmente chamado de **iGuardian / Guardian**.

## PROJECT STATUS

- **Current version:** Android 0.1.4
- **Current phase:** Android Foundation / stability + fixed DEV signing
- **Repository:** `BigCorps/iGuardian`
- **Backend / cloud / login / external AI:** none
- **Internet permission:** intentionally absent
- **DEV package:** `com.bigcorps.guardian.dev`
- **Daily JSON schema:** v2
- **Diagnostic schema:** v2

## O que o teste prolongado do 0.1.3 comprovou

O teste real no Xiaomi/Redmi Android 16 manteve coleta consistente por mais de duas horas após o início do tracking.

Comprovado:
- Usage Access;
- uso por app;
- PRIVATE sanitizado;
- SCREEN_OFF;
- desbloqueios;
- SQLite;
- JSON diário;
- exportação verificada;
- ausência de Internet.

Foram encontrados dois pontos a melhorar:
1. Launcher/IntentResolver estavam poluindo o ranking de apps.
2. O JobScheduler foi aceito inicialmente, mas mais tarde deixou de aparecer como pendente.

## 0.1.4

### Assinatura DEV fixa

GitHub Actions passa a assinar todos os APKs DEV com a mesma chave privada guardada apenas em Actions Secrets.

**0.1.4 exige uma última desinstalação**, porque 0.1.3 foi assinado com chave de debug efêmera do runner. Depois que 0.1.4 estiver instalado, 0.1.5+ deve atualizar por cima preservando SQLite, preferências e histórico.

A chave privada NÃO fica neste repositório. Veja `SIGNING_DEV.md` e o pacote separado KEEP-PRIVATE.

### Segundos exatos

JSON v2 mantém segundos como valor autoritativo. A UI mostra `6s`, e não `0m`, quando o período tem menos de um minuto.

### SYSTEM separado

Launcher, IntentResolver, System UI e permission controller viram `SYSTEM`, sem package/label armazenado.

O launcher HOME é detectado dinamicamente para não depender de Xiaomi/Samsung/etc.

### Cobertura do tracking

JSON v2 adiciona:
- tracking start;
- effective period start;
- recorded seconds;
- unclassified seconds;
- coverage percent.

Assim uma IA não interpreta o período anterior à ativação como “tempo sem uso”.

### Background instrumentado

O DEV usa cadência de 30 minutos para validar rapidamente:
- schedule attempts;
- recovery count;
- job starts;
- job finishes;
- job stops;
- last scheduler check.

O job é rechecado no início do processo, resume da Activity, boot, package replacement e user unlock.

## Antes de subir 0.1.4

Cadastre os quatro GitHub Actions Secrets do pacote KEEP-PRIVATE:

- `GUARDIAN_DEV_KEYSTORE_BASE64`
- `GUARDIAN_DEV_STORE_PASSWORD`
- `GUARDIAN_DEV_KEY_ALIAS`
- `GUARDIAN_DEV_KEY_PASSWORD`

Se faltar qualquer secret, o Actions falha de propósito em vez de gerar outro APK incompatível.

## Teste 0.1.4

1. Guarde os JSONs do 0.1.3 que quiser.
2. Desinstale 0.1.3 uma última vez.
3. Instale o APK fixed-signed 0.1.4.
4. Reative restricted settings/Usage Access se necessário.
5. Use normalmente por 1–2 horas.
6. Nos primeiros ~40 min, evite abrir o Guardian repetidamente para dar chance ao job de fundo.
7. Depois abra e exporte JSON diário + diagnóstico.

Esperado:
- Launcher e IntentResolver não aparecem como APP.
- `SYSTEM` aparece no lugar.
- PRIVATE curto aparece em segundos.
- `scheduler.job_run_count` informa se o background realmente executou.
- a próxima 0.1.5 deverá instalar por cima sem desinstalar.
