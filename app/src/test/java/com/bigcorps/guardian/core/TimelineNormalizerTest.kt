package com.bigcorps.guardian.core

import org.junit.Assert.assertEquals
import org.junit.Test

class TimelineNormalizerTest {
    @Test
    fun privateWinsOverScreenOffWithoutDoubleCounting() {
        val result = TimelineNormalizer.resolve(
            listOf(
                TimelineInterval(
                    startMs = 0,
                    endMs = 100,
                    type = IntervalType.SCREEN_OFF
                ),
                TimelineInterval(
                    startMs = 20,
                    endMs = 80,
                    type = IntervalType.PRIVATE
                )
            ),
            startMs = 0,
            endMs = 100
        )

        assertEquals(3, result.size)
        assertEquals(IntervalType.SCREEN_OFF, result[0].type)
        assertEquals(0L, result[0].startMs)
        assertEquals(20L, result[0].endMs)

        assertEquals(IntervalType.PRIVATE, result[1].type)
        assertEquals(20L, result[1].startMs)
        assertEquals(80L, result[1].endMs)

        assertEquals(IntervalType.SCREEN_OFF, result[2].type)
        assertEquals(80L, result[2].startMs)
        assertEquals(100L, result[2].endMs)
    }

    @Test
    fun duplicateAppIntervalsCollapse() {
        val app = TimelineInterval(
            startMs = 10,
            endMs = 30,
            type = IntervalType.APP,
            packageName = "example.app",
            appLabel = "Example"
        )

        val result = TimelineNormalizer.resolve(
            listOf(app, app.copy(id = 2)),
            startMs = 0,
            endMs = 40
        )

        assertEquals(1, result.size)
        assertEquals(10L, result[0].startMs)
        assertEquals(30L, result[0].endMs)
    }

    @Test
    fun unknownGapIsNotFilled() {
        val result = TimelineNormalizer.resolve(
            listOf(
                TimelineInterval(
                    startMs = 0,
                    endMs = 10,
                    type = IntervalType.SYSTEM
                ),
                TimelineInterval(
                    startMs = 12,
                    endMs = 20,
                    type = IntervalType.SYSTEM
                )
            ),
            startMs = 0,
            endMs = 20
        )

        assertEquals(2, result.size)
        assertEquals(10L, result[0].endMs)
        assertEquals(12L, result[1].startMs)
    }
}
