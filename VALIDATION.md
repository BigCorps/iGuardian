# Validation — Android 0.1.23

Expected build:
- versionName 0.1.23
- versionCode 24
- DB v8
- report v5
- diagnostic v21
- ValidationSuite v11
- validation pack v11

New checks:
- guardian_web_visual_privacy PASS
- guardian_web_runtime_health PASS after at least one successful screenshot + OCR
- guardian_web_physical_validation PASS only after normal + anonymous evidence and >=2 hosts

Physical target:
- UOL normal >= ~20 s
- Globo normal >= ~20 s
- one different incognito host >= ~20 s
- screenshot success count > 0
- OCR run count > 0
- visual OCR host count > 0
- anonymous milliseconds > 0
- no raw screenshot/OCR persistence
- manual_test_required=false when complete
