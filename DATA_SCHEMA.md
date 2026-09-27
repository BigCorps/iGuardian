# Data Schema — 0.1.23

DB v8 and report v5 are unchanged.

Diagnostic v21 adds Guardian Web Visual counters:
- screenshot requests/success/failure
- last screenshot error code
- OCR runs
- OCR host detections
- private-mode visual probes
- pipeline error count/class
- last sanitized visual host

No image bytes or raw OCR strings are part of the schema.

Validation pack v11 carries the compact diagnostic evidence.
