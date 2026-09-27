package com.bigcorps.guardian.web

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserVisualTextParserTest {
    @Test
    fun portalHostsAreExtractedWithoutPathsOrQueries() {
        assertEquals(
            "uol.com.br",
            BrowserVisualTextParser.parse(
                toolbarTexts =
                    listOf(
                        "www.uol.com.br"
                    ),
                modeProbeText =
                    ""
            ).host
        )

        assertEquals(
            "globo.com",
            BrowserVisualTextParser.parse(
                toolbarTexts =
                    listOf(
                        "https://www.globo.com/noticia?q=segredo"
                    ),
                modeProbeText =
                    ""
            ).host
        )
    }

    @Test
    fun ocrSpacingAroundUrlPunctuationIsRecovered() {
        assertEquals(
            "uol.com.br",
            BrowserVisualTextParser.parse(
                toolbarTexts =
                    listOf(
                        "https : / / www . uol . com . br"
                    ),
                modeProbeText =
                    ""
            ).host
        )
    }

    @Test
    fun normalToolbarDoesNotBecomePrivate() {
        val parsed =
            BrowserVisualTextParser.parse(
                toolbarTexts =
                    listOf(
                        "m.youtube.com"
                    ),
                modeProbeText =
                    "YouTube"
            )

        assertEquals(
            "m.youtube.com",
            parsed.host
        )

        assertFalse(
            parsed.privateModeDetected
        )
    }

    @Test
    fun incognitoStartPageCanLatchPrivateModeWithoutHost() {
        val parsed =
            BrowserVisualTextParser.parse(
                toolbarTexts =
                    emptyList(),
                modeProbeText =
                    "Você entrou no modo de navegação anônima"
            )

        assertNull(
            parsed.host
        )

        assertTrue(
            parsed.privateModeDetected
        )
    }

    @Test
    fun currentPortugueseIncognitoRedesignTextIsDetected() {
        assertTrue(
            BrowserVisualTextParser.detectsPrivateModeText(
                "Agora você pode navegar com privacidade. Outras pessoas que usarem este dispositivo não verão sua atividade."
            )
        )
    }

    @Test
    fun currentEnglishIncognitoRedesignTextIsDetected() {
        assertTrue(
            BrowserVisualTextParser.detectsPrivateModeText(
                "Now you can browse privately, and other people who use this device won't see your activity."
            )
        )
    }

    @Test
    fun newIncognitoTabActionAloneIsNotCurrentPrivateProof() {
        assertFalse(
            BrowserVisualTextParser.detectsPrivateModeText(
                "Nova guia anônima"
            )
        )
        assertFalse(
            BrowserVisualTextParser.detectsPrivateModeText(
                "New incognito tab"
            )
        )
    }

    @Test
    fun noisyToolbarWordsAreNotStoredAsHosts() {
        listOf(
            "kit",
            "fallback",
            "https",
            "app",
            "quiser",
            "midia.prosu"
        ).forEach {
            assertNull(
                BrowserVisualTextParser.parse(
                    toolbarTexts =
                        listOf(
                            it
                        ),
                    modeProbeText =
                        ""
                ).host
            )
        }
    }

    @Test
    fun emailLikeTextIsNotStoredAsHost() {
        listOf(
            "usuario@example.com",
            "usuario+tag@example.com",
            "contato:usuario@example.com"
        ).forEach {
            assertNull(
                BrowserVisualTextParser.parse(
                    toolbarTexts =
                        listOf(
                            it
                        ),
                    modeProbeText =
                        ""
                ).host
            )
        }

        assertEquals(
            "example.com",
            BrowserVisualTextParser.parse(
                toolbarTexts =
                    listOf(
                        "site example.com"
                    ),
                modeProbeText =
                    ""
            ).host
        )
    }
}
