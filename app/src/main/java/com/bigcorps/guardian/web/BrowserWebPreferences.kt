package com.bigcorps.guardian.web

import android.content.Context

class BrowserWebPreferences(context: Context) {
    private val prefs =
        context.getSharedPreferences("guardian_web", Context.MODE_PRIVATE)

    fun consented(): Boolean =
        prefs.getBoolean("consent", false) &&
            consentVersion() >= CONSENT_VERSION
    fun consentVersion(): Int = prefs.getInt("consent_version", 0)
    fun consentedAtMs(): Long = prefs.getLong("consented_at_ms", 0L)

    fun grantConsent(nowMs: Long = System.currentTimeMillis()) {
        prefs.edit()
            .putBoolean("consent", true)
            .putInt("consent_version", CONSENT_VERSION)
            .putLong("consented_at_ms", nowMs)
            .apply()
    }

    fun revokeConsent() =
        prefs.edit().putBoolean("consent", false).apply()

    fun trackingStartedAtMs(): Long =
        prefs.getLong("tracking_started_at_ms", 0L)

    fun markTrackingStartedIfMissing(nowMs: Long = System.currentTimeMillis()) {
        if (trackingStartedAtMs() <= 0L) {
            prefs.edit().putLong("tracking_started_at_ms", nowMs).apply()
        }
    }

    fun recordServiceConnected(nowMs: Long = System.currentTimeMillis()) {
        prefs.edit()
            .putLong("service_connected_at_ms", nowMs)
            .putLong("last_heartbeat_ms", nowMs)
            .putInt("service_connection_count", serviceConnectionCount() + 1)
            .apply()
    }

    fun recordHeartbeat(nowMs: Long = System.currentTimeMillis()) =
        prefs.edit().putLong("last_heartbeat_ms", nowMs).apply()

    fun recordAccessibilityEvent(
        browserPackage: String?,
        eventType: Int,
        nowMs: Long = System.currentTimeMillis()
    ) {
        val safePackage =
            browserPackage?.takeIf { BrowserCatalog.isSupported(it) }

        val edit = prefs.edit()
            .putInt("accessibility_event_count", accessibilityEventCount() + 1)
            .putInt("last_event_type", eventType)
            .putLong("last_event_at_ms", nowMs)

        if (safePackage == null) edit.remove("last_event_browser_package")
        else edit.putString("last_event_browser_package", safePackage)
        edit.apply()
    }

    fun recordEventSourceResult(
        browserPackage: String?,
        sourceAvailable: Boolean,
        rootResolved: Boolean,
        nowMs: Long = System.currentTimeMillis()
    ) {
        val safePackage =
            browserPackage?.takeIf {
                BrowserCatalog.isSupported(it)
            }

        val edit =
            prefs.edit()
                .putLong(
                    "last_event_source_at_ms",
                    nowMs
                )

        if (safePackage == null) {
            edit.remove(
                "last_event_source_browser_package"
            )
        } else {
            edit.putString(
                "last_event_source_browser_package",
                safePackage
            )
        }

        if (sourceAvailable) {
            edit.putInt(
                "event_source_count",
                eventSourceCount() + 1
            )
        } else {
            edit.putInt(
                "event_source_null_count",
                eventSourceNullCount() + 1
            )
        }

        if (rootResolved) {
            edit.putInt(
                "event_root_resolved_count",
                eventRootResolvedCount() + 1
            )
        }

        edit.apply()
    }

    fun recordSample(
        browserPackage: String?,
        state: String,
        hostFound: Boolean,
        nowMs: Long = System.currentTimeMillis()
    ) {
        val safePackage =
            browserPackage?.takeIf { BrowserCatalog.isSupported(it) }

        val edit = prefs.edit()
            .putInt("sample_count", sampleCount() + 1)
            .putString("last_sample_state", state.take(48))
            .putLong("last_sample_at_ms", nowMs)

        if (safePackage == null) edit.remove("last_root_browser_package")
        else edit.putString("last_root_browser_package", safePackage)

        when (state) {
            "FOUND", "FOUND_FALLBACK" ->
                if (hostFound) edit.putInt("host_found_count", hostFoundCount() + 1)
            "FOCUSED" ->
                edit.putInt("focused_skip_count", focusedSkipCount() + 1)
            "MISSING", "ROOT_NULL", "UNSUPPORTED_WINDOW" ->
                edit.putInt("missing_count", missingCount() + 1)
            "INVALID" ->
                edit.putInt("invalid_count", invalidCount() + 1)
        }
        edit.apply()
    }

    fun recordSampleError(
        errorClass: String,
        nowMs: Long = System.currentTimeMillis()
    ) {
        prefs.edit()
            .putInt("sample_error_count", sampleErrorCount() + 1)
            .putString("last_sample_error", errorClass.take(80))
            .putLong("last_sample_error_at_ms", nowMs)
            .apply()
    }

    fun recordDetectorState(
        browserPackage: String,
        urlBarId: String?,
        privateMode: Boolean,
        privateReason: String,
        nowMs: Long = System.currentTimeMillis()
    ) {
        if (!BrowserCatalog.isSupported(browserPackage)) return
        prefs.edit()
            .putString("last_browser_package", browserPackage.take(120))
            .putString("last_url_bar_id", urlBarId?.take(160))
            .putBoolean("last_private_mode", privateMode)
            .putString("last_private_reason", privateReason.take(80))
            .putLong("last_detection_at_ms", nowMs)
            .apply()
    }

    fun serviceConnectedAtMs(): Long = prefs.getLong("service_connected_at_ms", 0L)
    fun lastHeartbeatMs(): Long = prefs.getLong("last_heartbeat_ms", 0L)
    fun serviceConnectionCount(): Int = prefs.getInt("service_connection_count", 0)
    fun accessibilityEventCount(): Int = prefs.getInt("accessibility_event_count", 0)
    fun lastEventType(): Int = prefs.getInt("last_event_type", 0)
    fun lastEventAtMs(): Long = prefs.getLong("last_event_at_ms", 0L)
    fun lastEventBrowserPackage(): String? = prefs.getString("last_event_browser_package", null)
    fun eventSourceCount(): Int = prefs.getInt("event_source_count", 0)
    fun eventSourceNullCount(): Int = prefs.getInt("event_source_null_count", 0)
    fun eventRootResolvedCount(): Int = prefs.getInt("event_root_resolved_count", 0)
    fun lastEventSourceAtMs(): Long = prefs.getLong("last_event_source_at_ms", 0L)
    fun lastEventSourceBrowserPackage(): String? =
        prefs.getString("last_event_source_browser_package", null)

    fun sampleCount(): Int = prefs.getInt("sample_count", 0)
    fun hostFoundCount(): Int = prefs.getInt("host_found_count", 0)
    fun focusedSkipCount(): Int = prefs.getInt("focused_skip_count", 0)
    fun missingCount(): Int = prefs.getInt("missing_count", 0)
    fun invalidCount(): Int = prefs.getInt("invalid_count", 0)
    fun sampleErrorCount(): Int = prefs.getInt("sample_error_count", 0)
    fun lastSampleState(): String? = prefs.getString("last_sample_state", null)
    fun lastSampleAtMs(): Long = prefs.getLong("last_sample_at_ms", 0L)
    fun lastRootBrowserPackage(): String? = prefs.getString("last_root_browser_package", null)
    fun lastSampleError(): String? = prefs.getString("last_sample_error", null)
    fun lastSampleErrorAtMs(): Long = prefs.getLong("last_sample_error_at_ms", 0L)
    fun lastBrowserPackage(): String? = prefs.getString("last_browser_package", null)
    fun lastUrlBarId(): String? = prefs.getString("last_url_bar_id", null)
    fun lastPrivateMode(): Boolean = prefs.getBoolean("last_private_mode", false)
    fun lastPrivateReason(): String? = prefs.getString("last_private_reason", null)
    fun lastDetectionAtMs(): Long = prefs.getLong("last_detection_at_ms", 0L)

    fun recordScreenshotRequested(
        nowMs: Long =
            System.currentTimeMillis()
    ) {
        prefs.edit()
            .putInt(
                "visual_screenshot_request_count",
                visualScreenshotRequestCount() +
                    1
            )
            .putLong(
                "visual_last_screenshot_request_ms",
                nowMs
            )
            .apply()
    }

    fun recordScreenshotSuccess(
        nowMs: Long =
            System.currentTimeMillis()
    ) {
        prefs.edit()
            .putInt(
                "visual_screenshot_success_count",
                visualScreenshotSuccessCount() +
                    1
            )
            .putLong(
                "visual_last_screenshot_success_ms",
                nowMs
            )
            .remove(
                "visual_last_screenshot_error"
            )
            .apply()
    }

    fun recordScreenshotFailure(
        errorCode: Int,
        nowMs: Long =
            System.currentTimeMillis()
    ) {
        prefs.edit()
            .putInt(
                "visual_screenshot_failure_count",
                visualScreenshotFailureCount() +
                    1
            )
            .putInt(
                "visual_last_screenshot_error",
                errorCode
            )
            .putLong(
                "visual_last_screenshot_failure_ms",
                nowMs
            )
            .apply()
    }

    fun recordVisualOcr(
        host: String?,
        privateDetected: Boolean,
        privateReason: String,
        nowMs: Long =
            System.currentTimeMillis()
    ) {
        val safeHost =
            host?.takeIf {
                BrowserDomainSanitizer.isValidStoredHost(
                    it
                )
            }

        val edit =
            prefs.edit()
                .putInt(
                    "visual_ocr_run_count",
                    visualOcrRunCount() +
                        1
                )
                .putBoolean(
                    "visual_last_private_detected",
                    privateDetected
                )
                .putString(
                    "visual_last_private_reason",
                    privateReason.take(
                        80
                    )
                )
                .putLong(
                    "visual_last_ocr_ms",
                    nowMs
                )

        if (
            safeHost ==
            null
        ) {
            edit.remove(
                "visual_last_host"
            )
        } else {
            edit.putString(
                "visual_last_host",
                safeHost
            )
            edit.putInt(
                "visual_ocr_host_count",
                visualOcrHostCount() +
                    1
            )
        }

        if (
            privateDetected
        ) {
            edit.putInt(
                "visual_private_probe_count",
                visualPrivateProbeCount() +
                    1
            )
        }

        edit.apply()
    }

    fun recordVisualPipelineError(
        errorClass: String,
        nowMs: Long =
            System.currentTimeMillis()
    ) {
        prefs.edit()
            .putInt(
                "visual_pipeline_error_count",
                visualPipelineErrorCount() +
                    1
            )
            .putString(
                "visual_last_pipeline_error",
                errorClass.take(
                    80
                )
            )
            .putLong(
                "visual_last_pipeline_error_ms",
                nowMs
            )
            .apply()
    }

    fun visualScreenshotRequestCount(): Int =
        prefs.getInt(
            "visual_screenshot_request_count",
            0
        )

    fun visualScreenshotSuccessCount(): Int =
        prefs.getInt(
            "visual_screenshot_success_count",
            0
        )

    fun visualScreenshotFailureCount(): Int =
        prefs.getInt(
            "visual_screenshot_failure_count",
            0
        )

    fun visualLastScreenshotError(): Int =
        prefs.getInt(
            "visual_last_screenshot_error",
            0
        )

    fun visualOcrRunCount(): Int =
        prefs.getInt(
            "visual_ocr_run_count",
            0
        )

    fun visualOcrHostCount(): Int =
        prefs.getInt(
            "visual_ocr_host_count",
            0
        )

    fun visualPrivateProbeCount(): Int =
        prefs.getInt(
            "visual_private_probe_count",
            0
        )

    fun visualPipelineErrorCount(): Int =
        prefs.getInt(
            "visual_pipeline_error_count",
            0
        )

    fun visualLastHost(): String? =
        prefs.getString(
            "visual_last_host",
            null
        )

    fun visualLastPrivateDetected(): Boolean =
        prefs.getBoolean(
            "visual_last_private_detected",
            false
        )

    fun visualLastPrivateReason(): String? =
        prefs.getString(
            "visual_last_private_reason",
            null
        )

    fun visualLastPipelineError(): String? =
        prefs.getString(
            "visual_last_pipeline_error",
            null
        )

    fun clearRuntimeEvidence() {
        prefs.edit()
            .remove("tracking_started_at_ms")
            .remove("last_browser_package")
            .remove("last_url_bar_id")
            .remove("last_private_mode")
            .remove("last_private_reason")
            .remove("last_detection_at_ms")
            .remove("service_connected_at_ms")
            .remove("last_heartbeat_ms")
            .remove("service_connection_count")
            .remove("accessibility_event_count")
            .remove("last_event_type")
            .remove("last_event_at_ms")
            .remove("last_event_browser_package")
            .remove("event_source_count")
            .remove("event_source_null_count")
            .remove("event_root_resolved_count")
            .remove("last_event_source_at_ms")
            .remove("last_event_source_browser_package")
            .remove("sample_count")
            .remove("host_found_count")
            .remove("focused_skip_count")
            .remove("missing_count")
            .remove("invalid_count")
            .remove("visual_screenshot_request_count")
            .remove("visual_last_screenshot_request_ms")
            .remove("visual_screenshot_success_count")
            .remove("visual_last_screenshot_success_ms")
            .remove("visual_screenshot_failure_count")
            .remove("visual_last_screenshot_error")
            .remove("visual_last_screenshot_failure_ms")
            .remove("visual_ocr_run_count")
            .remove("visual_ocr_host_count")
            .remove("visual_private_probe_count")
            .remove("visual_pipeline_error_count")
            .remove("visual_last_host")
            .remove("visual_last_private_detected")
            .remove("visual_last_private_reason")
            .remove("visual_last_ocr_ms")
            .remove("visual_last_pipeline_error")
            .remove("visual_last_pipeline_error_ms")
            .remove("sample_error_count")
            .remove("last_sample_state")
            .remove("last_sample_at_ms")
            .remove("last_root_browser_package")
            .remove("last_sample_error")
            .remove("last_sample_error_at_ms")
            .apply()
    }

    companion object {
        const val CONSENT_VERSION = 2
    }
}
