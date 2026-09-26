package com.bigcorps.guardian.core

object HistoryReadiness {
    const val MIN_HISTORY_PERCENT =
        99.0

    const val MIN_COVERAGE_PERCENT =
        90.0

    fun canCompare(
        currentHistoryPercent: Double,
        previousHistoryPercent: Double,
        currentCoveragePercent: Double,
        previousCoveragePercent: Double
    ): Boolean =
        currentHistoryPercent >=
            MIN_HISTORY_PERCENT &&
            previousHistoryPercent >=
            MIN_HISTORY_PERCENT &&
            currentCoveragePercent >=
            MIN_COVERAGE_PERCENT &&
            previousCoveragePercent >=
            MIN_COVERAGE_PERCENT
}
