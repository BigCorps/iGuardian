from pathlib import Path
import json
import re
import sys

root = Path(__file__).resolve().parents[1]
manifest_path = root / "app/src/main/AndroidManifest.xml"
state_path = root / "PROJECT_STATE.json"
app_gradle_path = root / "app/build.gradle.kts"

manifest = manifest_path.read_text(encoding="utf-8")
state = json.loads(state_path.read_text(encoding="utf-8"))
app_gradle = app_gradle_path.read_text(encoding="utf-8")

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
]

errors = []

if "android.permission.INTERNET" in manifest:
    errors.append("MVP must not declare INTERNET permission")
if "android.permission.QUERY_ALL_PACKAGES" in manifest:
    errors.append("MVP must not declare QUERY_ALL_PACKAGES")
if "BIND_ACCESSIBILITY_SERVICE" in manifest:
    errors.append("MVP must not implement AccessibilityService")

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

version_match = re.search(r'versionName\s*=\s*"([^"]+)"', app_gradle)
code_match = re.search(r'versionCode\s*=\s*(\d+)', app_gradle)

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

secret_suffixes = {".jks", ".keystore", ".p12", ".pfx"}
for path in root.rglob("*"):
    if path.is_file() and path.suffix.lower() in secret_suffixes:
        errors.append(f"Signing key material must not be committed: {path.relative_to(root)}")

if errors:
    print("Privacy/project verification FAILED:")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print("Privacy/project verification OK")
print("- no INTERNET permission")
print("- no QUERY_ALL_PACKAGES")
print("- no AccessibilityService")
print("- no signing private key committed")
print("- handoff documentation present")
if version_match:
    print(f"- PROJECT_STATE/app version synchronized: {version_match.group(1)}")
if code_match:
    print(f"- Android versionCode found: {code_match.group(1)}")
