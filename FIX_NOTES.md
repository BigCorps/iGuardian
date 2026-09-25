# Guardian 0.1.6 — Background Hardening R2

Important finding from the newest JSONs:
the phone was still running 0.1.5 (versionCode 6). The repository also remained at 0.1.5 before this upload.

The long 0.1.5 test was still valuable:
- 99.8% coverage today;
- overnight background scheduler ran repeatedly;
- fresh visitor/user switch became generic PRIVATE with no exposed guest identity;
- no timeline overlaps;
- 0 job stops.

This R2 keeps the 0.1.6 scheduler-v3 corrections and additionally prevents concurrent JSON export requests.

Upload these files preserving paths.
Do NOT uninstall 0.1.5.
Install the resulting 0.1.6 APK directly over it.

Before starting the timed test, confirm the diagnostic says:
- version 0.1.6
- versionCode 7
- diagnostic_schema 4
- scheduler logic_version 3
