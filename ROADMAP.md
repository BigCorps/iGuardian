# Guardian Roadmap

## Android — current: 0.1.24 Hybrid v3

The Android core is now in the final Guardian Web hardware-validation stage.
0.1.24 combines accessibility-tree URL extraction, UsageStats duration banking
and screenshot/OCR fallback instead of betting on one browser signal.

### Gate A — 0.1.24 physical validation

Must prove on the Redmi/Android 16 device:

- two different normal Chrome Dev hosts are stored;
- a third incognito host is stored as anonymous;
- returning to a normal Chromium tab closes anonymous banking only from positive normal-mode evidence;
- last valid host continues accruing when the toolbar disappears;
- tree direct/fallback counters explain which source worked;
- `visual_private_probe_count` counts attempts and private detection is observable;
- API 34+ window screenshot result/secure error is observable without assuming
  secure == incognito;
- resource-ID-only diagnostic identifies Chrome normal/incognito IDs and, when
  tested, Mi Browser IDs;
- UI stays responsive;
- final APK remains fully offline and stores host-only web history.

### Banking compatibility gate

Before public release, validate **Modo Banco** with Inter/Inter Empresas and a
small representative set of other financial apps. Acceptance: Guardian Web must
be truly OFF in Android Accessibility before the banking app is opened, while
normal Guardian Usage Access collection remains active. Repeat the test later
with a Play-distributed build to separate sideload risk from Accessibility risk.

### Gate B — calibration follow-up only if the JSON requires it

Pin any newly proven Mi Browser/variant URL-bar IDs from physical evidence. Do
not guess IDs. Tighten private-mode transition heuristics only from observed
resource/class/window evidence.

### Gate C — Android release cleanup

After Gate A (and B if necessary): final UX copy, Play disclosure/data-safety
review, long-run OEM/battery test, clean validation pack, signing/release prep.

## Managed / Family / Enterprise later

Device Owner / managed Chrome policy can be evaluated as a separate Guardian
Managed product path. It is not part of normal Guardian onboarding and must not
be required to make the consumer app work.

## Windows

After Android Guardian Web passes hardware validation, reuse the validated
product contract: app usage, host-only web activity, normal/private browsing,
privacy-before-storage, local trends/intelligence and compact validation.
