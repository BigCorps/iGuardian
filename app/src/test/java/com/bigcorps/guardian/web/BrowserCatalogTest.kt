package com.bigcorps.guardian.web

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserCatalogTest {
    @Test
    fun operaStablePrefersUrlField() {
        val spec = BrowserCatalog.spec("com.opera.browser")!!
        assertEquals("com.opera.browser:id/url_field", spec.urlBarIds.first())
    }


    @Test
    fun operaMiniAlsoPrefersUrlField() {
        val spec = BrowserCatalog.spec("com.opera.mini.native")!!
        assertEquals("com.opera.mini.native:id/url_field", spec.urlBarIds.first())
    }

    @Test
    fun chromiumVariantsUseOwnUrlBarBeforeChromeFallback() {
        listOf(
            "com.chrome.beta",
            "com.chrome.dev",
            "com.chrome.canary",
            "com.brave.browser_beta",
            "com.brave.browser_nightly",
            "com.microsoft.emmx.beta",
            "com.microsoft.emmx.dev",
            "com.microsoft.emmx.canary",
            "com.vivaldi.browser.snapshot"
        ).forEach { pkg ->
            val spec = BrowserCatalog.spec(pkg)!!
            assertEquals("$pkg:id/url_bar", spec.urlBarIds.first())
        }
    }

    @Test
    fun firefoxVariantsHaveMozacAndLegacyCandidates() {
        listOf(
            "org.mozilla.firefox",
            "org.mozilla.firefox_beta",
            "org.mozilla.fenix",
            "org.mozilla.focus"
        ).forEach { pkg ->
            val ids = BrowserCatalog.spec(pkg)!!.urlBarIds
            assertTrue(ids.any { it.endsWith(":id/mozac_browser_toolbar_url_view") })
            assertTrue(ids.any { it.endsWith(":id/url_bar") })
        }
    }

    @Test
    fun xiaomiBrowserIsObservedWithoutGuessingResourceId() {
        val spec = BrowserCatalog.spec("com.android.browser")!!
        assertEquals("unknown_chromium", spec.family)
        assertTrue(spec.urlBarIds.isEmpty())
    }
}
