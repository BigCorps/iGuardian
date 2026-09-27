#!/usr/bin/env python3
from pathlib import Path
import hashlib
import json
import re

ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "VALIDATION_CONTRACTS.json"
ASSET = ROOT / "app/src/main/assets/validation-contracts.json"
GRADLE = ROOT / "app/build.gradle.kts"
CURRENT_SENTINEL = "__CURRENT_BUILD__"

def fail(message: str) -> None:
    print(f"::error::{message}")
    raise SystemExit(1)

def group_digest(paths: list[str]) -> str:
    h = hashlib.sha256()
    for rel in sorted(paths):
        path = ROOT / rel
        if not path.is_file():
            fail(f"validation contract file missing: {rel}")
        h.update(rel.encode("utf-8"))
        h.update(b"\0")
        h.update(path.read_bytes())
        h.update(b"\0")
    return h.hexdigest()

if not MANIFEST.is_file():
    fail("VALIDATION_CONTRACTS.json missing")
if not ASSET.is_file():
    fail("runtime validation-contracts.json asset missing")

root_manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
asset_manifest = json.loads(ASSET.read_text(encoding="utf-8"))

if root_manifest != asset_manifest:
    fail("root validation manifest and Android asset differ")

if root_manifest.get("schema") != 1:
    fail("unsupported validation-contract schema")

gradle = GRADLE.read_text(encoding="utf-8")
m = re.search(r'versionName\s*=\s*"([^"]+)"', gradle)
if not m:
    fail("could not read Android versionName")
version = m.group(1)

if root_manifest.get("build_version") != version:
    fail(
        f"validation manifest build_version={root_manifest.get('build_version')} "
        f"does not match versionName={version}"
    )

contracts = root_manifest.get("contracts")
if not isinstance(contracts, list) or not contracts:
    fail("validation contracts list is empty")

seen = set()
actual_digests = {}

for contract in contracts:
    cid = contract.get("id")
    if not cid or cid in seen:
        fail(f"invalid/duplicate validation contract id: {cid}")
    seen.add(cid)

    files = contract.get("files")
    if not isinstance(files, list) or not files:
        fail(f"{cid}: files list is empty")

    actual = group_digest(files)
    actual_digests[cid] = actual
    expected = contract.get("sha256")
    mode = contract.get("validation_mode")
    validated_in = contract.get("validated_in")

    if expected == CURRENT_SENTINEL:
        if mode == "inherited":
            fail(f"{cid}: inherited groups must have a pinned SHA-256")
        if validated_in != version:
            fail(
                f"{cid}: current-build sentinel requires validated_in={version}, "
                f"got {validated_in}"
            )
        continue

    if not isinstance(expected, str) or not re.fullmatch(r"[0-9a-f]{64}", expected):
        fail(f"{cid}: invalid SHA-256 contract")

    if actual != expected:
        fail(
            f"{cid}: source digest changed. "
            f"expected={expected} actual={actual}. "
            "Update the validation contract intentionally and choose the correct validation mode."
        )

required_modes = {
    "usage_collection_core": "inherited",
    "background_scheduler_core": "inherited",
    "local_intelligence_core": "inherited",
    "database_report_browser_overlay": "runtime_autotest",
    "privacy_classification_core": "runtime_autotest",
    "guardian_web_core": "runtime_plus_physical",
    "validation_export_ui": "runtime_autotest",
    "build_pipeline": "ci_only",
}

by_id = {c["id"]: c for c in contracts}
for cid, mode in required_modes.items():
    if cid not in by_id:
        fail(f"required validation contract missing: {cid}")
    if by_id[cid].get("validation_mode") != mode:
        fail(
            f"{cid}: expected validation_mode={mode}, "
            f"got {by_id[cid].get('validation_mode')}"
        )

print("Validation lineage contracts OK")
print(f"- build_version: {version}")
print(f"- base physical validation: {root_manifest.get('base_physically_validated_version')}")
for cid in required_modes:
    c = by_id[cid]
    digest = actual_digests[cid]
    print(
        f"- {cid}: {c.get('validation_mode')} "
        f"(validated_in={c.get('validated_in')}; sha256={digest})"
    )
