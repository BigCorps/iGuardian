# Upload — ConfIA.vc Android 0.1.26

Base expected in GitHub before upload:

- Actions #67 green;
- commit `0ed697b66132603effe03b24038cc80ce339094f`;
- version 0.1.25 / code 26.

Upload **all files from this ZIP preserving their paths**.

This patch intentionally keeps:

- namespace `com.bigcorps.guardian`;
- applicationId `com.bigcorps.guardian.dev`;
- existing signing certificate;
- current local SQLite/database continuity.

Visible branding changes:

- launcher/system app name becomes `ConfIA.vc`;
- launcher/round icon uses the symbol from the supplied ConfIA.vc logo;
- Android 12+ splash shows the ConfIA symbol;
- base system surfaces use the orange/purple brand palette.

Expected build:

- versionName `0.1.26`;
- versionCode `27`;
- Actions artifact `guardian-android-0.1.26-fixed-signed-debug`.

Backend:

- `backend/confia-supabase-foundation-poc.sql` is for the existing minhAi
  Supabase project and is **not executed by GitHub Actions**.
- Apply it manually only after the Android upload/build is confirmed.
- The current APK remains offline; the future extension/backend POC will be a
  separate next step.
