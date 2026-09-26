package com.bigcorps.guardian.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryReadinessTest {
    @Test
    fun completePeriodsCanCompare() {
        assertTrue(
            HistoryReadiness.canCompare(
                100.0,
                100.0,
                99.0,
                98.0
            )
        )
    }

    @Test
    fun incompletePreviousPeriodCannotCompare() {
        assertFalse(
            HistoryReadiness.canCompare(
                100.0,
                83.8,
                99.0,
                99.0
            )
        )
    }

    @Test
    fun insufficientCoverageCannotCompare() {
        assertFalse(
            HistoryReadiness.canCompare(
                100.0,
                100.0,
                99.0,
                80.0
            )
        )
    }
}
