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
    fun emailLikeTextIsNotStoredAsHost() {
        assertNull(
            BrowserVisualTextParser.parse(
                toolbarTexts =
                    listOf(
                        "usuario@example.com"
                    ),
                modeProbeText =
                    ""
            ).host
        )
    }
}
