package com.bigcorps.guardian.core

import org.junit.Assert.assertEquals
import org.junit.Test

class LocalQuestionIntentParserTest {
    @Test
    fun parsesTopAppToday() {
        val plan =
            LocalQuestionIntentParser.plan(
                "Qual app mais usei hoje?"
            )

        assertEquals(
            LocalQuestionIntent.TOP_APP,
            plan.intent
        )
        assertEquals(
            LocalQuestionPeriod.TODAY,
            plan.period
        )
    }

    @Test
    fun parsesTopAppYesterday() {
        val plan =
            LocalQuestionIntentParser.plan(
                "Qual app mais usei ontem?"
            )

        assertEquals(
            LocalQuestionIntent.TOP_APP,
            plan.intent
        )
        assertEquals(
            LocalQuestionPeriod.YESTERDAY,
            plan.period
        )
    }

    @Test
    fun parsesLastSevenDays() {
        val plan =
            LocalQuestionIntentParser.plan(
                "Top 5 dos últimos 7 dias"
            )

        assertEquals(
            LocalQuestionIntent.TOP_APPS,
            plan.intent
        )
        assertEquals(
            LocalQuestionPeriod.LAST_7_DAYS,
            plan.period
        )
    }

    @Test
    fun parsesUnlocks() {
        assertEquals(
            LocalQuestionIntent.UNLOCKS,
            LocalQuestionIntentParser.parse(
                "Quantas vezes desbloqueei?"
            )
        )
    }

    @Test
    fun parsesNaturalScreenOffPhrase() {
        assertEquals(
            LocalQuestionIntent.SCREEN_OFF,
            LocalQuestionIntentParser.parse(
                "Quanto tempo a tela ficou desligada?"
            )
        )
    }

    @Test
    fun parsesComparison() {
        assertEquals(
            LocalQuestionIntent.COMPARE_TODAY_YESTERDAY,
            LocalQuestionIntentParser.parse(
                "Compare hoje com ontem"
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
