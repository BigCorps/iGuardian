package com.bigcorps.guardian.core

import org.junit.Assert.assertEquals
import org.junit.Test

class LocalQuestionIntentParserTest {
    @Test
    fun comparisonUsesReferenceToday() {
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
    fun keepsLast24HoursSpecialCase() {
        val plan =
            LocalQuestionIntentParser.plan(
                "Top 5 das últimas 24 horas"
            )

        assertEquals(
            LocalQuestionPeriod.LAST_24_HOURS,
            plan.period
        )
        assertEquals(
            24,
            plan.amount
        )
    }

    @Test
    fun keepsLastSevenDaysSpecialCase() {
        val plan =
            LocalQuestionIntentParser.plan(
                "Resumo dos últimos 7 dias"
            )

        assertEquals(
            LocalQuestionPeriod.LAST_7_DAYS,
            plan.period
        )
        assertEquals(
            7,
            plan.amount
        )
    }

    @Test
    fun parsesScreenOffNaturalPhrase() {
        assertEquals(
            LocalQuestionIntent.SCREEN_OFF,
            LocalQuestionIntentParser.parse(
                "Quanto tempo a tela ficou desligada?"
            )
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
