from pathlib import Path
import json
import re
import sys

root = Path(__file__).resolve().parents[1]
manifest_path = root / "app/src/main/AndroidManifest.xml"
state_path = root / "PROJECT_STATE.json"
app_gradle_path = root / "app/build.gradle.kts"
gradle_properties_path = root / "gradle.properties"

manifest = manifest_path.read_text(encoding="utf-8")
state = json.loads(state_path.read_text(encoding="utf-8"))
app_gradle = app_gradle_path.read_text(encoding="utf-8")
gradle_properties = gradle_properties_path.read_text(encoding="utf-8")

required_docs = [
    "README.md",
    "PRIVACY_MODEL.md",
    "DATA_SCHEMA.md",
    "ROADMAP.md",
    "STORE_COMPLIANCE.md",
    "CHANGELOG.md",
    "VALIDATION.md",
    "PROJECT_STATE.json",
    "SIGNING_DEV.md",
    "LOCAL_INTELLIGENCE.md",
]

errors = []

if "android.permission.INTERNET" in manifest:
    errors.append("MVP must not declare INTERNET permission")
if "android.permission.QUERY_ALL_PACKAGES" in manifest:
    errors.append("MVP must not declare QUERY_ALL_PACKAGES")
if "BIND_ACCESSIBILITY_SERVICE" in manifest:
    errors.append("MVP must not implement AccessibilityService")
if "android.useAndroidX=true" not in gradle_properties:
    errors.append("0.1.8+ requires android.useAndroidX=true")
if "androidx.work:work-runtime:2.12.0" not in app_gradle:
    errors.append("Required WorkManager 2.12.0 dependency missing")
if ".core.GuardianJobService" in manifest:
    errors.append("Legacy direct GuardianJobService must not remain in manifest")

source = "\n".join(
    p.read_text(encoding="utf-8", errors="ignore")
    for p in (root / "app/src/main/java").rglob("*.kt")
)

for forbidden in [
    "AccessibilityService",
    "VpnService",
    "MediaProjection",
    "NotificationListenerService",
    "ClipboardManager",
    "InputMethodService",
]:
    if forbidden in source:
        errors.append(f"Forbidden MVP API found in source: {forbidden}")

version_match = re.search(
    r'versionName\s*=\s*"([^"]+)"',
    app_gradle
)
code_match = re.search(
    r'versionCode\s*=\s*(\d+)',
    app_gradle
)

if version_match is None:
    errors.append("Could not read versionName from app/build.gradle.kts")
else:
    gradle_version = version_match.group(1)
    state_version = str(state.get("version", "")).strip()
    if state_version != gradle_version:
        errors.append(
            f"PROJECT_STATE version ({state_version or 'missing'}) "
            f"must match app versionName ({gradle_version})"
        )

if code_match is None:
    errors.append("Could not read versionCode from app/build.gradle.kts")

for name in required_docs:
    if not (root / name).exists():
        errors.append(f"Missing required project handoff file: {name}")

required_sources = [
    "app/src/main/java/com/bigcorps/guardian/core/ValidationSuite.kt",
    "app/src/main/java/com/bigcorps/guardian/core/ValidationPackGenerator.kt",
    "app/src/main/java/com/bigcorps/guardian/core/LocalIntelligenceSelfCheck.kt",
]

for name in required_sources:
    if not (root / name).exists():
        errors.append(f"Missing required validation source: {name}")

secret_suffixes = {".jks", ".keystore", ".p12", ".pfx"}
for path in root.rglob("*"):
    if path.is_file() and path.suffix.lower() in secret_suffixes:
        errors.append(
            f"Signing key material must not be committed: "
            f"{path.relative_to(root)}"
        )

if errors:
    print("Privacy/project verification FAILED:")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print("Privacy/project verification OK")
print("- no INTERNET permission")
print("- no QUERY_ALL_PACKAGES")
print("- no AccessibilityService")
print("- WorkManager background architecture present")
print("- comprehensive local validation suite present")
print("- one-file validation pack generator present")
print("- no legacy direct GuardianJobService in app manifest")
print("- no signing private key committed")
print("- handoff documentation present")
if version_match:
    print(
        "- PROJECT_STATE/app version synchronized: "
        f"{version_match.group(1)}"
    )
if code_match:
    print(
        "- Android versionCode found: "
        f"{code_match.group(1)}"
    )
