#!/usr/bin/env bash
set -euo pipefail

APK="app/build/outputs/apk/debug/app-debug.apk"
SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"

fail() {
  echo "::error::$1"
  exit 1
}

[[ -f "$APK" ]] || fail "APK final não encontrado: $APK"
[[ -n "$SDK_ROOT" ]] || fail "Android SDK root ausente"

AAPT="$(
  find "$SDK_ROOT/build-tools" -maxdepth 2 -type f -name aapt -print \
    | sort -V \
    | tail -n 1
)"
[[ -n "$AAPT" && -x "$AAPT" ]] || fail "aapt não encontrado"

EXPECTED_NAME="$(
  sed -nE 's/^[[:space:]]*versionName[[:space:]]*=[[:space:]]*"([^"]+)".*/\1/p' \
    app/build.gradle.kts | head -n 1
)"
EXPECTED_CODE="$(
  sed -nE 's/^[[:space:]]*versionCode[[:space:]]*=[[:space:]]*([0-9]+).*/\1/p' \
    app/build.gradle.kts | head -n 1
)"

[[ -n "$EXPECTED_NAME" ]] || fail "versionName não encontrado"
[[ -n "$EXPECTED_CODE" ]] || fail "versionCode não encontrado"

BADGING="$("$AAPT" dump badging "$APK")"
PERMISSIONS="$("$AAPT" dump permissions "$APK")"
XMLTREE="$("$AAPT" dump xmltree "$APK" AndroidManifest.xml)"

grep -Fq "package: name='com.bigcorps.guardian.dev'" <<<"$BADGING" \
  || fail "applicationId inesperado no APK"
grep -Fq "versionCode='$EXPECTED_CODE'" <<<"$BADGING" \
  || fail "versionCode do APK diverge do Gradle"
grep -Fq "versionName='$EXPECTED_NAME'" <<<"$BADGING" \
  || fail "versionName do APK diverge do Gradle"
grep -Fq "application-label:'ConfIA.vc'" <<<"$BADGING" \
  || fail "label ConfIA.vc ausente no APK"

for forbidden in \
  "android.permission.INTERNET" \
  "android.permission.ACCESS_NETWORK_STATE" \
  "android.permission.QUERY_ALL_PACKAGES" \
  "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE" \
  "android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION"; do
  if grep -Fq "$forbidden" <<<"$PERMISSIONS"$'\n'"$XMLTREE"; then
    fail "APK final contém permissão/contrato proibido: $forbidden"
  fi
done

for forbidden_accessibility in \
  "com.bigcorps.guardian.web.BrowserAccessibilityService" \
  "android.permission.BIND_ACCESSIBILITY_SERVICE" \
  "android.accessibilityservice.AccessibilityService"; do
  if grep -Fq "$forbidden_accessibility" <<<"$XMLTREE"; then
    fail "APK final ainda registra arquitetura de Acessibilidade: $forbidden_accessibility"
  fi
done

grep -Fq "com.bigcorps.guardian.ConfiaMainActivity" <<<"$XMLTREE" \
  || fail "ConfiaMainActivity ausente no APK final"

APK_LIST="$(unzip -l "$APK")"
for required_asset in \
  "assets/confia-web-firefox-poc-0.1.1.xpi" \
  "assets/confia-web-edge-poc-0.1.1.crx"; do
  grep -Fq "$required_asset" <<<"$APK_LIST" \
    || fail "Extensão POC ausente do APK final: $required_asset"
done

echo "Final APK contract OK"
echo "- package: com.bigcorps.guardian.dev"
echo "- label: ConfIA.vc"
echo "- versionName: $EXPECTED_NAME"
echo "- versionCode: $EXPECTED_CODE"
echo "- forbidden network/high-risk permissions: absent"
echo "- AccessibilityService registration: absent"
echo "- ConfIA launcher: present"
echo "- bundled Firefox XPI + Edge CRX: present"
