package com.bigcorps.guardian.web

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserPrivateModeHeuristicsTest {
    private val chrome =
        BrowserCatalog.spec(
            "com.chrome.dev"
        )!!

    @Test
    fun chromiumCurrentIncognitoLabelsDetectPrivateMode() {
        listOf(
            "Incognito mode",
            "Leave Incognito mode",
            "Selected Incognito Tab",
            "News, Incognito Tab",
            "Modo de navegação anônima",
            "Sair do modo de navegação anônima",
            "Guia anônima selecionada",
            "Notícias, guia anônima"
        ).forEach { label ->
            assertTrue(
                BrowserPrivateModeHeuristics.detect(
                    chrome,
                    listOf(
                        BrowserAccessibilityMarker(
                            resourceId =
                                "com.chrome.dev:id/toolbar",
                            text =
                                null,
                            contentDescription =
                                label
                        )
                    )
                ).isPrivate
            )
        }
    }

    @Test
    fun incognitoActionsAvailableFromNormalModeAreNotPrivateProof() {
        listOf(
            "Enter Incognito mode",
            "New Incognito tab",
            "Nova guia anônima",
            "Entrar no modo de navegação anônima"
        ).forEach { label ->
            assertFalse(
                BrowserPrivateModeHeuristics.detect(
                    chrome,
                    listOf(
                        BrowserAccessibilityMarker(
                            resourceId =
                                "com.chrome.dev:id/menu_item",
                            text =
                                null,
                            contentDescription =
                                label
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
                        resourceId =
                            "com.chrome.dev:id/url_bar",
                        text =
                            "example.com",
                        contentDescription =
                            "Address bar"
                    )
                )
            ).isPrivate
        )
    }

    @Test
    fun chromiumIncognitoBadgeResourceDetectsPrivateMode() {
        listOf(
            "com.chrome.dev:id/incognito_indicator",
            "com.chrome.dev:id/location_bar_incognito_badge"
        ).forEach { resource ->
            assertTrue(
                BrowserPrivateModeHeuristics.detect(
                    chrome,
                    listOf(
                        BrowserAccessibilityMarker(
                            resourceId =
                                resource,
                            text =
                                null,
                            contentDescription =
                                null
                        )
                    )
                ).isPrivate
            )
        }
    }
}
