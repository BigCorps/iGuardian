package com.bigcorps.guardian.web

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserPrivateModeHeuristicsTest {
    private val chrome =
        BrowserCatalog.spec("com.chrome.dev")!!

    @Test
    fun chromiumStrongAccessibilityLabelsDetectIncognito() {
        listOf(
            "Leave Incognito mode",
            "Selected Incognito Tab",
            "Sair do modo de navegação anônima",
            "Guia anônima selecionada"
        ).forEach { label ->
            assertTrue(
                BrowserPrivateModeHeuristics.detect(
                    chrome,
                    listOf(
                        BrowserAccessibilityMarker(
                            resourceId = "com.chrome.dev:id/incognito_switch",
                            text = null,
                            contentDescription = label
                        )
                    )
                ).isPrivate
            )
        }
    }

    @Test
    fun normalToolbarDoesNotBecomePrivate() {
        assertFalse(
            BrowserPrivateModeHeuristics.detect(
                chrome,
                listOf(
                    BrowserAccessibilityMarker(
                        resourceId = "com.chrome.dev:id/url_bar",
                        text = "example.com",
                        contentDescription = "Address bar"
                    )
                )
            ).isPrivate
        )
    }

    @Test
    fun resourceIndicatorCanDetectPrivateMode() {
        assertTrue(
            BrowserPrivateModeHeuristics.detect(
                chrome,
                listOf(
                    BrowserAccessibilityMarker(
                        resourceId = "com.chrome.dev:id/incognito_indicator",
                        text = null,
                        contentDescription = null
                    )
                )
            ).isPrivate
        )
    }
}
