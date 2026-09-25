package com.bigcorps.guardian.core

import org.junit.Assert.assertEquals
import org.junit.Test

class LocalQuestionIntentParserTest {
    @Test
    fun parsesTopApp() {
        assertEquals(
            LocalQuestionIntent.TOP_APP_TODAY,
            LocalQuestionIntentParser.parse(
                "Qual app mais usei hoje?"
            )
        )
    }

    @Test
    fun parsesUnlocksWithAccent() {
        assertEquals(
            LocalQuestionIntent.UNLOCKS_TODAY,
            LocalQuestionIntentParser.parse(
                "Quantas vezes desbloqueei?"
            )
        )
    }

    @Test
    fun parsesNaturalScreenOffPhrase() {
        assertEquals(
            LocalQuestionIntent.SCREEN_OFF_TODAY,
            LocalQuestionIntentParser.parse(
                "Quanto tempo a tela ficou desligada?"
            )
        )
    }

    @Test
    fun parsesDirectScreenOffPhrase() {
        assertEquals(
            LocalQuestionIntent.SCREEN_OFF_TODAY,
            LocalQuestionIntentParser.parse(
                "Tempo de tela desligada hoje"
            )
        )
    }

    @Test
    fun parsesScreenOffEnglishTerm() {
        assertEquals(
            LocalQuestionIntent.SCREEN_OFF_TODAY,
            LocalQuestionIntentParser.parse(
                "Quanto tempo de screen off?"
            )
        )
    }

    @Test
    fun parsesScreenApagadaPhrase() {
        assertEquals(
            LocalQuestionIntent.SCREEN_OFF_TODAY,
            LocalQuestionIntentParser.parse(
                "Quanto tempo fiquei com a tela apagada?"
            )
        )
    }

    @Test
    fun rejectsOlderPeriodForNow() {
        assertEquals(
            LocalQuestionIntent.UNSUPPORTED_PERIOD,
            LocalQuestionIntentParser.parse(
                "Qual app mais usei ontem?"
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
