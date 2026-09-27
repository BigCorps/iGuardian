# Validation — Android 0.1.24 Hybrid v3

## CI/static gate

Expected before installing:

- versionName 0.1.24 / versionCode 25;
- unit tests pass, including BrowserCatalog variants;
- Kotlin compilation and APK build pass;
- validation lineage passes;
- final APK contains neither INTERNET nor ACCESS_NETWORK_STATE;
- Guardian Web accessibility config retains screenshot + interactive-window
  capabilities and the expanded package allowlist.

## Physical target — Redmi / Android 16

1. Chrome Dev normal: `uol.com.br`, ~20 seconds.
2. Chrome Dev normal: `globo.com`, ~20 seconds.
3. Confirm toolbar can collapse/scroll while the last host keeps accruing.
4. Open Chrome Dev incognito and wait 4–6 seconds on the incognito start page.
5. Visit a third host in incognito, ~20 seconds.
6. Return to a normal Chrome Dev tab for ~10 seconds to validate private → normal transition.
7. Optional but valuable: repeat normal/private in Mi Browser.
8. Return to Guardian and export one validation JSON.

## Acceptance evidence

- `tree_probe_count > 0`;
- at least one of `tree_direct_host_count` / `tree_fallback_host_count` > 0 on
  browsers that expose a usable tree;
- visual OCR remains available as fallback;
- `visual_private_probe_count > 0` after incognito test;
- `visual_private_detected_count` reports positive visual detections separately;
- `tree_private_marker_count` and `tree_normal_marker_count` expose positive mode transitions separately;
- window screenshot request/success/failure metrics are present on API 34+;
- `secure_browser_window`/error 6, if observed, is treated only as a signal;
- `browser_tree_resource_ids` and `browser_tree_resource_ids_by_package` contain
  IDs only and no URL/page text;
- normal rows > 0, anonymous rows > 0 and distinct hosts >= 2;
- no invalid hosts/bad durations;
- full URL/content/screenshot/raw OCR remain unpersisted;
- exported access-status fields agree for the same validation snapshot.
