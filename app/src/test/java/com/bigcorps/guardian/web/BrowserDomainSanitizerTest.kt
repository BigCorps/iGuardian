package com.bigcorps.guardian.web

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserDomainSanitizerTest {
    @Test
    fun fullUrlBecomesHostOnly() {
        assertEquals(
            "google.com",
            BrowserDomainSanitizer.hostFromRaw(
                "https://www.google.com/search?q=segredo#resultado"
            )
        )
        assertEquals(
            "mail.google.com",
            BrowserDomainSanitizer.hostFromRaw(
                "mail.google.com/mail/u/0/#inbox"
            )
        )
        assertEquals(
            "example.com",
            BrowserDomainSanitizer.hostFromRaw(
                "https://user:pass@example.com/private?token=1"
            )
        )
    }

    @Test
    fun browserInternalAndSearchTextAreRejected() {
        assertNull(BrowserDomainSanitizer.hostFromRaw("chrome://settings"))
        assertNull(BrowserDomainSanitizer.hostFromRaw("about:blank"))
        assertNull(BrowserDomainSanitizer.hostFromRaw("pesquisa com espaços"))
        assertNull(BrowserDomainSanitizer.hostFromRaw("javascript:alert(1)"))
    }

    @Test
    fun storedValueMustAlreadyBeSanitized() {
        assertTrue(BrowserDomainSanitizer.isSanitizedHost("youtube.com"))
        assertTrue(BrowserDomainSanitizer.isSanitizedHost("mail.google.com"))
    }
}
