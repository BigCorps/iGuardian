# Privacy Model — Guardian 0.1.23

Guardian Web Visual is explicit opt-in and requires consent v2.

Transient processing:
- screenshot requested only while a supported browser is foreground;
- full screenshot exists only in RAM;
- toolbar crop is OCR'd locally;
- a larger upper crop may be OCR'd only to detect explicit incognito start-page text
  while private mode is unresolved;
- bitmap and OCR objects are discarded after processing.

Persisted:
- sanitized host only
- allowlisted browser package
- interval
- private/normal boolean
- technical counters/error codes without raw content

Never persisted/exported:
- screenshot
- raw OCR text
- full URL
- path/query/search/fragment
- page title/content
- typed text/passwords

No network transport is added.
