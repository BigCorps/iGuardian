# Guardian Android 0.1.25 — physical-evidence correction

Base: repository commit `1e196d5fd32d4e25e73ad1ffe85db8e90b623959`.
GitHub Actions #64 is green and is the build baseline for this round.

## What the 0.1.24 JSON proved

The validation exported at 2026-09-27 16:26:19 -03 showed:

- tree probes: 618;
- tree host hits: 0;
- event source available: 0;
- screenshots: 78 successful / 82 requested;
- OCR runs: 78;
- host-shaped OCR results: 53;
- private probes: 4;
- private detections: 0;
- anonymous browser rows/time: 0;
- last persisted web detection: 14:06:20, well before the 16:26 export;
- Bank Mode disable event observed at 16:23:17;
- real Inter Empresas package observed as `br.com.Inter.CDPro`.

The visible list contained older rows from previous versions, including low-quality
OCR artifacts. Therefore the next test must begin from a web-only clean baseline.

## 0.1.25 corrections

1. **OCR host gate**
   - tree URLs continue through the general sanitizer;
   - visual OCR goes through `hostFromVisualOcr()`;
   - single words and implausible long-TLD OCR noise are rejected.

2. **Incognito visual detection**
   - recognizes current Chromium pt-BR text such as `Agora você pode navegar com privacidade`;
   - recognizes the current English redesign context;
   - keeps `Nova guia anônima` / `New incognito tab` as action-only, not proof of current private mode;
   - host crop reduced from 22% to 16% to avoid page-body false positives; deep visual probe increased from 62% to 90% of the display.

3. **Clean physical test**
   - Guardian Web UI adds `Iniciar teste limpo`;
   - current browser interval is banked/stopped first;
   - in-memory host/private state is reset;
   - queued final browser write completes before the browser table is cleared;
   - only web history/telemetry is reset; normal app history remains intact.

4. **Financial protection**
   - new `FinancialAppCatalog`;
   - Inter Empresas `br.com.Inter.CDPro` and Inter `br.com.intermedium` are recognized;
   - PrivacyClassifier v3 makes financial apps PRIVATE;
   - installed financial launcher apps appear as `Abrir <app> com proteção`;
   - Guardian calls `disableSelf()`, confirms AccessibilityManager=false and secure setting=false, then launches the bank;
   - direct opening of a recognized financial app also triggers a best-effort UsageStats failsafe, but this cannot guarantee pre-launch ordering.

No database schema migration and no network expansion.
