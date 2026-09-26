# Local Intelligence v7 — 0.1.14

Comparisons are now history-aware.

A numeric comparison is returned only when both requested periods have enough
historical availability (>=99%) and classification coverage (>=90%).

Otherwise Guardian reports that history is insufficient rather than treating
missing pre-install history as zero usage.

All processing remains local and user questions are not persisted.
