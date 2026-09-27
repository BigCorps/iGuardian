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

grep -Fq "package: name='com.bigcorps.guardian.dev'" <<<"$BADGING" \
  || fail "applicationId inesperado no APK"

grep -Fq "versionCode='$EXPECTED_CODE'" <<<"$BADGING" \
  || fail "versionCode do APK diverge do Gradle"

grep -Fq "versionName='$EXPECTED_NAME'" <<<"$BADGING" \
  || fail "versionName do APK diverge do Gradle"

PERMISSIONS="$("$AAPT" dump permissions "$APK")"
XMLTREE="$("$AAPT" dump xmltree "$APK" AndroidManifest.xml)"

for forbidden in \
  "android.permission.INTERNET" \
  "android.permission.QUERY_ALL_PACKAGES" \
  "android.permission.BIND_ACCESSIBILITY_SERVICE" \
  "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE" \
  "android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION"; do
  if grep -Fq "$forbidden" <<<"$PERMISSIONS"$'\n'"$XMLTREE"; then
    fail "APK final contém permissão/contrato proibido: $forbidden"
  fi
done

echo "Final APK contract OK"
echo "- package: com.bigcorps.guardian.dev"
echo "- versionName: $EXPECTED_NAME"
echo "- versionCode: $EXPECTED_CODE"
echo "- forbidden permissions/services: absent"
