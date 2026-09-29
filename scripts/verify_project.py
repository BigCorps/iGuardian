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
    "VALIDATION_CONTRACTS.json",
    "BANK_MODE.md",
]

errors = []

internet_permission_tags = re.findall(
    r"<uses-permission\b[^>]*android:name=[\"']android\.permission\.INTERNET[\"'][^>]*>",
    manifest,
    flags=re.IGNORECASE,
)
for tag in internet_permission_tags:
    if 'tools:node="remove"' not in tag and "tools:node='remove'" not in tag:
        errors.append(
            "Android POC must not effectively declare INTERNET; extension/backend are separate"
        )

network_state_tags = re.findall(
    r"<uses-permission\b[^>]*android:name=[\"']android\.permission\.ACCESS_NETWORK_STATE[\"'][^>]*>",
    manifest,
    flags=re.IGNORECASE,
)
for tag in network_state_tags:
    if 'tools:node="remove"' not in tag and "tools:node='remove'" not in tag:
        errors.append("Android POC must not effectively declare ACCESS_NETWORK_STATE")

if "android.permission.QUERY_ALL_PACKAGES" in manifest:
    errors.append("POC must not declare QUERY_ALL_PACKAGES")

# 0.1.27 retires the old Accessibility/OCR architecture from the installed app.
for forbidden_manifest_fragment in [
    ".web.BrowserAccessibilityService",
    "android.permission.BIND_ACCESSIBILITY_SERVICE",
    "android.accessibilityservice.AccessibilityService",
    "@xml/guardian_web_accessibility",
]:
    if forbidden_manifest_fragment in manifest:
        errors.append(
            "ConfIA Web 0.1.27 must not register legacy Accessibility component: "
            + forbidden_manifest_fragment
        )

if ".ConfiaMainActivity" not in manifest:
    errors.append("ConfIA launcher activity missing from manifest")
if ".MainActivity" in manifest:
    errors.append("Legacy Guardian MainActivity must not remain registered")

if "android.useAndroidX=true" not in gradle_properties:
    errors.append("android.useAndroidX=true missing")
if "androidx.work:work-runtime:2.12.0" not in app_gradle:
    errors.append("Required WorkManager 2.12.0 dependency missing")
if ".core.GuardianJobService" in manifest:
    errors.append("Legacy direct GuardianJobService must not remain in manifest")

kotlin_files = list((root / "app/src/main/java").rglob("*.kt"))
source = "\n".join(
    p.read_text(encoding="utf-8", errors="ignore")
    for p in kotlin_files
)

for forbidden in [
    "VpnService",
    "MediaProjection",
    "NotificationListenerService",
    "ClipboardManager",
    "InputMethodService",
]:
    if forbidden in source:
        errors.append(f"Forbidden POC API found in source: {forbidden}")

confia_main_path = root / "app/src/main/java/com/bigcorps/guardian/ConfiaMainActivity.kt"
web_activity_path = root / "app/src/main/java/com/bigcorps/guardian/web/BrowserWebActivity.kt"

if not confia_main_path.exists():
    errors.append("ConfiaMainActivity.kt missing")
else:
    confia_main = confia_main_path.read_text(encoding="utf-8", errors="ignore")
    for marker in [
        "ConfIA.vc",
        "Sem Acessibilidade",
        "sem screenshot",
        "sem VPN",
        "Firefox Android",
        "Edge Android",
    ]:
        if marker not in confia_main:
            errors.append(f"ConfIA launcher copy missing required marker: {marker}")

if not web_activity_path.exists():
    errors.append("BrowserWebActivity.kt missing")
else:
    web_activity = web_activity_path.read_text(encoding="utf-8", errors="ignore")
    for marker in [
        "Firefox Android",
        "Edge Android",
        "Chrome / Chrome Dev",
        "Sem Acessibilidade",
        "Sem screenshot ou OCR",
        "Sem VPN",
        "schema confia",
    ]:
        if marker not in web_activity:
            errors.append(f"ConfIA Web compatibility UI missing required marker: {marker}")
    for forbidden_runtime in [
        "BrowserWebAccess",
        "requestBankModeDisable",
        "openAccessibility",
        "BrowserAccessibilityService",
        "android.accessibilityservice",
    ]:
        if forbidden_runtime in web_activity:
            errors.append(
                "ConfIA Web compatibility UI must not call legacy runtime path: "
                + forbidden_runtime
            )

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
else:
    state_code = int(state.get("version_code", -1))
    if state_code != int(code_match.group(1)):
        errors.append(
            f"PROJECT_STATE version_code ({state_code}) must match Gradle ({code_match.group(1)})"
        )

workflow_path = root / ".github/workflows/android.yml"
workflow = workflow_path.read_text(encoding="utf-8")
if version_match is not None:
    expected_artifact = f"confia-android-{version_match.group(1)}-fixed-signed-debug"
    if expected_artifact not in workflow:
        errors.append(
            "Workflow artifact name must match current ConfIA versionName: "
            + expected_artifact
        )

for required_script in [
    "scripts/verify_validation_contracts.py",
    "scripts/verify_apk_contract.sh",
]:
    if not (root / required_script).exists():
        errors.append(f"Missing required CI validation script: {required_script}")

asset_contract = root / "app/src/main/assets/validation-contracts.json"
if not asset_contract.exists():
    errors.append("Missing runtime validation lineage asset")

for name in required_docs:
    if not (root / name).exists():
        errors.append(f"Missing required project handoff file: {name}")

required_sources = [
    "app/src/main/java/com/bigcorps/guardian/ConfiaMainActivity.kt",
    "app/src/main/java/com/bigcorps/guardian/core/ValidationSuite.kt",
    "app/src/main/java/com/bigcorps/guardian/core/ValidationPackGenerator.kt",
    "app/src/main/java/com/bigcorps/guardian/core/LocalIntelligenceSelfCheck.kt",
    "app/src/main/java/com/bigcorps/guardian/core/HistoryReadiness.kt",
    "app/src/main/java/com/bigcorps/guardian/core/ValidationLineage.kt",
    "app/src/main/java/com/bigcorps/guardian/web/BrowserWebActivity.kt",
]
for name in required_sources:
    if not (root / name).exists():
        errors.append(f"Missing required validation source: {name}")

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
print("- no effective INTERNET/ACCESS_NETWORK_STATE permission")
print("- no QUERY_ALL_PACKAGES")
print("- legacy AccessibilityService is NOT registered in AndroidManifest")
print("- ConfIA launcher replaces legacy Guardian MainActivity")
print("- ConfIA Web UI is extension/UsageStats based")
print("- no VPN / MediaProjection / notification listener path")
print("- WorkManager local collection remains present")
print("- validation lineage + final APK guards remain present")
print("- no signing private key committed")
if version_match:
    print(f"- PROJECT_STATE/app version synchronized: {version_match.group(1)}")
if code_match:
    print(f"- Android versionCode synchronized: {code_match.group(1)}")
