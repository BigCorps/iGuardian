package com.bigcorps.guardian.core

object TimelineNormalizer {
    fun resolve(
        intervals: List<TimelineInterval>,
        startMs: Long,
        endMs: Long
    ): List<TimelineInterval> {
        if (endMs <= startMs) return emptyList()

        val clipped = intervals.mapNotNull { item ->
            val start = maxOf(item.startMs, startMs)
            val end = minOf(item.endMs, endMs)
            if (end <= start) null
            else item.copy(startMs = start, endMs = end)
        }

        if (clipped.isEmpty()) return emptyList()

        val boundaries = clipped
            .flatMap { listOf(it.startMs, it.endMs) }
            .distinct()
            .sorted()

        val atomic = mutableListOf<TimelineInterval>()

        for (index in 0 until boundaries.lastIndex) {
            val segmentStart = boundaries[index]
            val segmentEnd = boundaries[index + 1]
            if (segmentEnd <= segmentStart) continue

            val candidates = clipped.filter { item ->
                item.startMs < segmentEnd && item.endMs > segmentStart
            }

            if (candidates.isEmpty()) continue

            val chosen = candidates.maxWithOrNull(
                compareBy<TimelineInterval>(
                    { priority(it.type) },
                    { it.startMs },
                    { it.id }
                )
            ) ?: continue

            atomic += chosen.copy(
                startMs = segmentStart,
                endMs = segmentEnd
            )
        }

        val merged = mutableListOf<TimelineInterval>()

        atomic.forEach { current ->
            val previous = merged.lastOrNull()
            val sameIdentity = previous != null &&
                previous.type == current.type &&
                (
                    current.type != IntervalType.APP ||
                    previous.packageName == current.packageName
                )

            // Merge only true adjacency. Never bridge an unknown gap.
            if (sameIdentity && previous!!.endMs == current.startMs) {
                merged[merged.lastIndex] = previous.copy(
                    endMs = current.endMs,
                    appLabel = previous.appLabel ?: current.appLabel
                )
            } else {
                merged += current
            }
        }

        return merged
    }

    private fun priority(type: IntervalType): Int =
        when (type) {
            IntervalType.PRIVATE -> 50
            IntervalType.ANONYMOUS_BROWSER -> 40
            IntervalType.SCREEN_OFF -> 30
            IntervalType.SYSTEM -> 20
            IntervalType.APP -> 10
        }
}
