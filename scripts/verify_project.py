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
            "MVP must not effectively declare INTERNET permission; "
            "only tools:node=remove is allowed for transitive dependency cleanup"
        )

network_state_tags = re.findall(
    r"<uses-permission\b[^>]*android:name=[\"']android\.permission\.ACCESS_NETWORK_STATE[\"'][^>]*>",
    manifest,
    flags=re.IGNORECASE,
)
for tag in network_state_tags:
    if 'tools:node="remove"' not in tag and "tools:node='remove'" not in tag:
        errors.append(
            "Guardian offline build must not effectively declare ACCESS_NETWORK_STATE"
        )

if "android.permission.QUERY_ALL_PACKAGES" in manifest:
    errors.append("MVP must not declare QUERY_ALL_PACKAGES")

web_service_name = ".web.BrowserAccessibilityService"
web_accessibility_xml = root / "app/src/main/res/xml/guardian_web_accessibility.xml"

if manifest.count("android.permission.BIND_ACCESSIBILITY_SERVICE") != 1:
    errors.append(
        "Guardian Web must declare exactly one BIND_ACCESSIBILITY_SERVICE service permission"
    )
if web_service_name not in manifest:
    errors.append("Guardian Web accessibility service missing from manifest")
if 'android:exported="true"' not in manifest:
    errors.append("Guardian Web service must be exported for Android system binding")

if not web_accessibility_xml.exists():
    errors.append("Guardian Web accessibility config missing")
else:
    web_xml = web_accessibility_xml.read_text(encoding="utf-8")
    if 'android:isAccessibilityTool="false"' not in web_xml:
        errors.append("Guardian Web must declare isAccessibilityTool=false")
    if 'com.chrome.dev' not in web_xml:
        errors.append(
            "Guardian Web accessibility package allowlist must include Chrome Dev"
        )
    if 'android:packageNames=' not in web_xml:
        errors.append("Guardian Web accessibility package allowlist missing")
    if 'flagRetrieveInteractiveWindows' not in web_xml:
        errors.append(
            "Guardian Web must request flagRetrieveInteractiveWindows for browser-window fallback"
        )
    if 'android:canTakeScreenshot="true"' not in web_xml:
        errors.append("Guardian Web Visual must declare canTakeScreenshot=true")

if "android.useAndroidX=true" not in gradle_properties:
    errors.append("0.1.8+ requires android.useAndroidX=true")
if "androidx.work:work-runtime:2.12.0" not in app_gradle:
    errors.append("Required WorkManager 2.12.0 dependency missing")
if "com.google.mlkit:text-recognition:16.0.1" not in app_gradle:
    errors.append(
        "Guardian Web Visual requires bundled ML Kit text-recognition 16.0.1"
    )
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
        errors.append(f"Forbidden MVP API found in source: {forbidden}")

if "BrowserDomainSanitizer.isValidStoredHost" in source:
    errors.append(
        "Stale Guardian Web sanitizer helper reference: "
        "use BrowserDomainSanitizer.isSanitizedHost"
    )

# IMPORTANT: detect actual android.accessibilityservice imports, not the plain
# text "AccessibilityService". Hybrid v3 documents BrowserAccessibilityService
# inside BrowserAccessibilityExtractor comments; the old substring guard treated
# that harmless comment as API usage and made Actions #61 fail before Gradle.
# Keeping this import-based guard preserves the intended isolation contract while
# avoiding comment/string false positives.
accessibility_sources = []
for path in kotlin_files:
    content = path.read_text(encoding="utf-8", errors="ignore")
    if re.search(
        r"^\s*import\s+android\.accessibilityservice\.",
        content,
        flags=re.MULTILINE,
    ):
        accessibility_sources.append(path.relative_to(root).as_posix())

allowed_accessibility_sources = {
    "app/src/main/java/com/bigcorps/guardian/web/BrowserAccessibilityService.kt",
    "app/src/main/java/com/bigcorps/guardian/web/BrowserWebAccess.kt",
}
if set(accessibility_sources) != allowed_accessibility_sources:
    errors.append(
        "android.accessibilityservice imports must be isolated to Guardian Web only: "
        + ",".join(accessibility_sources)
    )

web_service_source = (
    root / "app/src/main/java/com/bigcorps/guardian/web/BrowserAccessibilityService.kt"
).read_text(encoding="utf-8", errors="ignore")
web_activity_source = (
    root / "app/src/main/java/com/bigcorps/guardian/web/BrowserWebActivity.kt"
).read_text(encoding="utf-8", errors="ignore")

if "fun requestBankModeDisable(): Boolean" not in web_service_source:
    errors.append("Guardian Web bank mode disable entrypoint missing")
if "disableSelf()" not in web_service_source:
    errors.append("Guardian Web bank mode must disable the AccessibilityService via disableSelf()")
if '"Modo Banco"' not in web_activity_source:
    errors.append("Guardian Web bank mode UI missing")

version_match = re.search(
    r'versionName\s*=\s*"([^"]+)"',
    app_gradle,
)
code_match = re.search(
    r'versionCode\s*=\s*(\d+)',
    app_gradle,
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

workflow_path = root / ".github/workflows/android.yml"
workflow = workflow_path.read_text(encoding="utf-8")

if version_match is not None:
    expected_artifact = (
        f"guardian-android-{version_match.group(1)}-fixed-signed-debug"
    )
    if expected_artifact not in workflow:
        errors.append(
            "Workflow artifact name must match current versionName: "
            f"{expected_artifact}"
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
    "app/src/main/java/com/bigcorps/guardian/core/ValidationSuite.kt",
    "app/src/main/java/com/bigcorps/guardian/core/ValidationPackGenerator.kt",
    "app/src/main/java/com/bigcorps/guardian/core/LocalIntelligenceSelfCheck.kt",
    "app/src/main/java/com/bigcorps/guardian/core/HistoryReadiness.kt",
    "app/src/main/java/com/bigcorps/guardian/core/ValidationLineage.kt",
    "app/src/main/java/com/bigcorps/guardian/web/BrowserDomainSanitizer.kt",
    "app/src/main/java/com/bigcorps/guardian/web/BrowserAccessibilityService.kt",
    "app/src/main/java/com/bigcorps/guardian/web/BrowserReport.kt",
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
print("- no effective INTERNET permission (transitive declaration removed at manifest merge)")
print("- no QUERY_ALL_PACKAGES")
print("- android.accessibilityservice imports isolated to Guardian Web service/status code")
print("- Guardian Web Visual screenshot capability + bundled OCR contract present")
print("- Guardian Web accessibility package allowlist present")
print("- Guardian Web bank mode disableSelf contract present")
print("- WorkManager background architecture present")
print("- comprehensive local validation suite present")
print("- one-file validation pack generator present")
print("- validation lineage manifest + CI guard present")
print("- final APK contract guard present")
print("- history-readiness guard source present")
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
