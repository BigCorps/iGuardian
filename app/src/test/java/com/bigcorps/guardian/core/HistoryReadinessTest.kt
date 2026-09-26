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
    @Test
    fun zeroHistoryCannotCompare() {
        assertFalse(
            HistoryReadiness.canCompare(
                27.4,
                0.0,
                99.3,
                0.0
            )
        )
    }

    @Test
    fun readinessBoundaryRequiresNinetyNinePercentHistory() {
        assertFalse(
            HistoryReadiness.canCompare(
                98.9,
                100.0,
                99.0,
                99.0
            )
        )

        assertTrue(
            HistoryReadiness.canCompare(
                99.0,
                99.0,
                90.0,
                90.0
            )
        )
    }

}
