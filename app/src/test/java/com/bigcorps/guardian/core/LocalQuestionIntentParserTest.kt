package com.bigcorps.guardian.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class LocalQuestionIntentParserTest {
    @Test
    fun comparisonTodayYesterdayUsesTodayReference() {
        val plan =
            LocalQuestionIntentParser.plan(
                "Compare hoje com ontem"
            )

        assertEquals(
            LocalQuestionIntent.COMPARE_TODAY_YESTERDAY,
            plan.intent
        )
        assertEquals(
            LocalQuestionPeriod.TODAY,
            plan.period
        )
    }

    @Test
    fun comparisonLast24HoursIsSpecialIntent() {
        val plan =
            LocalQuestionIntentParser.plan(
                "Compare as últimas 24 horas com as 24 anteriores"
            )

        assertEquals(
            LocalQuestionIntent.COMPARE_LAST_24H_PREVIOUS_24H,
            plan.intent
        )
        assertEquals(
            LocalQuestionPeriod.LAST_24_HOURS,
            plan.period
        )
    }

    @Test
    fun parsesCalendarDay() {
        val plan =
            LocalQuestionIntentParser.plan(
                "Resumo de 25/09/2026"
            )

        assertEquals(
            LocalQuestionIntent.SUMMARY,
            plan.intent
        )
        assertEquals(
            LocalQuestionPeriod.CALENDAR_DAY,
            plan.period
        )
        assertEquals(
            25,
            plan.calendarStart?.day
        )
        assertEquals(
            9,
            plan.calendarStart?.month
        )
        assertEquals(
            2026,
            plan.calendarStart?.year
        )
    }

    @Test
    fun parsesCalendarRange() {
        val plan =
            LocalQuestionIntentParser.plan(
                "Insights de 24/09 a 26/09"
            )

        assertEquals(
            LocalQuestionIntent.INSIGHTS,
            plan.intent
        )
        assertEquals(
            LocalQuestionPeriod.CALENDAR_RANGE,
            plan.period
        )
        assertNotNull(
            plan.calendarStart
        )
        assertNotNull(
            plan.calendarEnd
        )
    }

    @Test
    fun parsesSixRollingHours() {
        val plan =
            LocalQuestionIntentParser.plan(
                "Top 5 das últimas 6 horas"
            )

        assertEquals(
            LocalQuestionIntent.TOP_APPS,
            plan.intent
        )
        assertEquals(
            LocalQuestionPeriod.ROLLING_HOURS,
            plan.period
        )
        assertEquals(
            6,
            plan.amount
        )
    }

    @Test
    fun parsesThreeRollingDays() {
        val plan =
            LocalQuestionIntentParser.plan(
                "Insights dos últimos 3 dias"
            )

        assertEquals(
            LocalQuestionIntent.INSIGHTS,
            plan.intent
        )
        assertEquals(
            LocalQuestionPeriod.ROLLING_DAYS,
            plan.period
        )
        assertEquals(
            3,
            plan.amount
        )
    }

    @Test
    fun normalizesPortugueseAccents() {
        assertEquals(
            "navegacao do sistema",
            LocalQuestionIntentParser.normalize(
                "Navegação do sistema"
            )
        )
    }
}
