from pathlib import Path
import json
import sys

root = Path(__file__).resolve().parents[1]
manifest = (root / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
state = json.loads((root / "PROJECT_STATE.json").read_text(encoding="utf-8"))
required_docs = [
    "README.md", "PRIVACY_MODEL.md", "DATA_SCHEMA.md",
    "ROADMAP.md", "STORE_COMPLIANCE.md", "CHANGELOG.md", "VALIDATION.md", "PROJECT_STATE.json"
]

errors = []
if "android.permission.INTERNET" in manifest:
    errors.append("MVP must not declare INTERNET permission")
if "android.permission.QUERY_ALL_PACKAGES" in manifest:
    errors.append("MVP must not declare QUERY_ALL_PACKAGES")
if "BIND_ACCESSIBILITY_SERVICE" in manifest:
    errors.append("MVP must not implement AccessibilityService")

source = "\n".join(p.read_text(encoding="utf-8", errors="ignore") for p in (root / "app/src/main/java").rglob("*.kt"))
for forbidden in ["AccessibilityService", "VpnService", "MediaProjection", "NotificationListenerService", "ClipboardManager", "InputMethodService"]:
    if forbidden in source:
        errors.append(f"Forbidden MVP API found in source: {forbidden}")
if state.get("version") != "0.1.0":
    errors.append("PROJECT_STATE version must be 0.1.0")
for name in required_docs:
    if not (root / name).exists():
        errors.append(f"Missing required project handoff file: {name}")

if errors:
    print("Privacy/project verification FAILED:")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print("Privacy/project verification OK")
print("- no INTERNET permission")
print("- no QUERY_ALL_PACKAGES")
print("- no AccessibilityService")
print("- handoff documentation present")
