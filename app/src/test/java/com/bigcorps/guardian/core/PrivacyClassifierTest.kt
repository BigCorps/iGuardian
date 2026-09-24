package com.bigcorps.guardian.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivacyClassifierTest {
    @Test
    fun androidSettingsIsPrivate() {
        assertTrue(PrivacyClassifier.isPrivate("com.android.settings"))
    }

    @Test
    fun bankingLikePackageIsPrivate() {
        assertTrue(PrivacyClassifier.isPrivate("br.com.example.bank.mobile"))
        assertTrue(PrivacyClassifier.isPrivate("br.com.intermedium"))
    }

    @Test
    fun normalPackageIsNotPrivateByDefault() {
        assertFalse(PrivacyClassifier.isPrivate("com.example.social"))
    }

    @Test
    fun manualPrivatePackageWins() {
        assertTrue(PrivacyClassifier.isPrivate("com.example.social", setOf("com.example.social")))
    }

    @Test
    fun nonAppIntervalsCannotPersistIdentity() {
        listOf(
            IntervalType.PRIVATE,
            IntervalType.SCREEN_OFF,
            IntervalType.ANONYMOUS_BROWSER
        ).forEach { type ->
            val sanitized = StorageSanitizer.identityFor(type, "secret.package", "Secret App")
            assertNull(sanitized.packageName)
            assertNull(sanitized.appLabel)
        }
    }
}
