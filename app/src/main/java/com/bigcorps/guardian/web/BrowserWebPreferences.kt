package com.bigcorps.guardian.web

import android.content.Context

class BrowserWebPreferences(
    context: Context
) {
    private val prefs =
        context.getSharedPreferences(
            "guardian_web",
            Context.MODE_PRIVATE
        )

    fun consented(): Boolean =
        prefs.getBoolean(
            "consent",
            false
        )

    fun consentVersion(): Int =
        prefs.getInt(
            "consent_version",
            0
        )

    fun consentedAtMs(): Long =
        prefs.getLong(
            "consented_at_ms",
            0L
        )

    fun grantConsent(
        nowMs: Long =
            System.currentTimeMillis()
    ) {
        prefs.edit()
            .putBoolean(
                "consent",
                true
            )
            .putInt(
                "consent_version",
                CONSENT_VERSION
            )
            .putLong(
                "consented_at_ms",
                nowMs
            )
            .apply()
    }

    fun revokeConsent() {
        prefs.edit()
            .putBoolean(
                "consent",
                false
            )
            .apply()
    }

    fun trackingStartedAtMs(): Long =
        prefs.getLong(
            "tracking_started_at_ms",
            0L
        )

    fun markTrackingStartedIfMissing(
        nowMs: Long =
            System.currentTimeMillis()
    ) {
        if (
            trackingStartedAtMs() <=
            0L
        ) {
            prefs.edit()
                .putLong(
                    "tracking_started_at_ms",
                    nowMs
                )
                .apply()
        }
    }

    fun recordDetectorState(
        browserPackage: String,
        urlBarId: String?,
        privateMode: Boolean,
        privateReason: String,
        nowMs: Long =
            System.currentTimeMillis()
    ) {
        prefs.edit()
            .putString(
                "last_browser_package",
                browserPackage.take(
                    120
                )
            )
            .putString(
                "last_url_bar_id",
                urlBarId
                    ?.take(
                        160
                    )
            )
            .putBoolean(
                "last_private_mode",
                privateMode
            )
            .putString(
                "last_private_reason",
                privateReason.take(
                    80
                )
            )
            .putLong(
                "last_detection_at_ms",
                nowMs
            )
            .apply()
    }

    fun lastBrowserPackage(): String? =
        prefs.getString(
            "last_browser_package",
            null
        )

    fun lastUrlBarId(): String? =
        prefs.getString(
            "last_url_bar_id",
            null
        )

    fun lastPrivateMode(): Boolean =
        prefs.getBoolean(
            "last_private_mode",
            false
        )

    fun lastPrivateReason(): String? =
        prefs.getString(
            "last_private_reason",
            null
        )

    fun lastDetectionAtMs(): Long =
        prefs.getLong(
            "last_detection_at_ms",
            0L
        )

    fun clearRuntimeEvidence() {
        prefs.edit()
            .remove(
                "tracking_started_at_ms"
            )
            .remove(
                "last_browser_package"
            )
            .remove(
                "last_url_bar_id"
            )
            .remove(
                "last_private_mode"
            )
            .remove(
                "last_private_reason"
            )
            .remove(
                "last_detection_at_ms"
            )
            .apply()
    }

    companion object {
        const val CONSENT_VERSION =
            1
    }
}
