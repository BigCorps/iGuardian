package com.bigcorps.guardian.web

import android.content.Context

class BrowserWebPreferences(context: Context) {
    private val prefs =
        context.getSharedPreferences(
            "guardian_web",
            Context.MODE_PRIVATE
        )

    fun consented(): Boolean =
        prefs.getBoolean(
            "consent",
            false
        ) &&
            consentVersion() >=
            CONSENT_VERSION

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

    fun revokeConsent() =
        prefs.edit()
            .putBoolean(
                "consent",
                false
            )
            .apply()

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

    fun recordServiceConnected(
        nowMs: Long =
            System.currentTimeMillis()
    ) {
        prefs.edit()
            .putLong(
                "service_connected_at_ms",
                nowMs
            )
            .putLong(
                "last_heartbeat_ms",
                nowMs
            )
            .putInt(
                "service_connection_count",
                serviceConnectionCount() +
                    1
            )
            .apply()
    }

    fun recordHeartbeat(
        nowMs: Long =
            System.currentTimeMillis()
    ) =
        prefs.edit()
            .putLong(
                "last_heartbeat_ms",
                nowMs
            )
            .apply()

    fun recordAccessibilityEvent(
        browserPackage: String?,
        eventType: Int,
        nowMs: Long =
            System.currentTimeMillis()
    ) {
        val safePackage =
            browserPackage
                ?.takeIf {
                    BrowserCatalog.isSupported(
                        it
                    )
                }

        val edit =
            prefs.edit()
                .putInt(
                    "accessibility_event_count",
                    accessibilityEventCount() +
                        1
                )
                .putInt(
                    "last_event_type",
                    eventType
                )
                .putLong(
                    "last_event_at_ms",
                    nowMs
                )

        if (
            safePackage ==
            null
        ) {
            edit.remove(
                "last_event_browser_package"
            )
        } else {
            edit.putString(
                "last_event_browser_package",
                safePackage
            )
        }

        edit.apply()
    }

    fun recordEventSourceResult(
        browserPackage: String?,
        sourceAvailable: Boolean,
        rootResolved: Boolean,
        nowMs: Long =
            System.currentTimeMillis()
    ) {
        val safePackage =
            browserPackage
                ?.takeIf {
                    BrowserCatalog.isSupported(
                        it
                    )
                }

        val edit =
            prefs.edit()
                .putLong(
                    "last_event_source_at_ms",
                    nowMs
                )

        if (
            safePackage ==
            null
        ) {
            edit.remove(
                "last_event_source_browser_package"
            )
        } else {
            edit.putString(
                "last_event_source_browser_package",
                safePackage
            )
        }

        if (
            sourceAvailable
        ) {
            edit.putInt(
                "event_source_count",
                eventSourceCount() +
                    1
            )
        } else {
            edit.putInt(
                "event_source_null_count",
                eventSourceNullCount() +
                    1
            )
        }

        if (
            rootResolved
        ) {
            edit.putInt(
                "event_root_resolved_count",
                eventRootResolvedCount() +
                    1
            )
        }

        edit.apply()
    }

    fun recordSample(
        browserPackage: String?,
        state: String,
        hostFound: Boolean,
        nowMs: Long =
            System.currentTimeMillis()
    ) {
        val safePackage =
            browserPackage
                ?.takeIf {
                    BrowserCatalog.isSupported(
                        it
                    )
                }

        val edit =
            prefs.edit()
                .putInt(
                    "sample_count",
                    sampleCount() +
                        1
                )
                .putString(
                    "last_sample_state",
                    state.take(
                        48
                    )
                )
                .putLong(
                    "last_sample_at_ms",
                    nowMs
                )

        if (
            safePackage ==
            null
        ) {
            edit.remove(
                "last_root_browser_package"
            )
        } else {
            edit.putString(
                "last_root_browser_package",
                safePackage
            )
        }

        when (
            state
        ) {
            "FOUND",
            "FOUND_FALLBACK" ->
                if (
                    hostFound
                ) {
                    edit.putInt(
                        "host_found_count",
                        hostFoundCount() +
                            1
                    )
                }

            "FOCUSED" ->
                edit.putInt(
                    "focused_skip_count",
                    focusedSkipCount() +
                        1
                )

            "MISSING",
            "MISSING_DIRECT",
            "ROOT_NULL",
            "UNSUPPORTED_WINDOW" ->
                edit.putInt(
                    "missing_count",
                    missingCount() +
                        1
                )

            "INVALID" ->
                edit.putInt(
                    "invalid_count",
                    invalidCount() +
                        1
                )
        }

        edit.apply()
    }

    fun recordSampleError(
        errorClass: String,
        nowMs: Long =
            System.currentTimeMillis()
    ) {
        prefs.edit()
            .putInt(
                "sample_error_count",
                sampleErrorCount() +
                    1
            )
            .putString(
                "last_sample_error",
                errorClass.take(
                    80
                )
            )
            .putLong(
                "last_sample_error_at_ms",
                nowMs
            )
            .apply()
    }

    fun recordTreeProbe(
        browserPackage: String,
        state: String,
        source: String,
        urlBarId: String?,
        hostFound: Boolean,
        privateDetected: Boolean,
        privateReason: String,
        normalDetected: Boolean = false,
        nowMs: Long =
            System.currentTimeMillis()
    ) {
        if (
            !BrowserCatalog.isSupported(
                browserPackage
            )
        ) {
            return
        }

        val edit =
            prefs.edit()
                .putInt(
                    "tree_probe_count",
                    treeProbeCount() +
                        1
                )
                .putString(
                    "tree_last_state",
                    state.take(
                        48
                    )
                )
                .putString(
                    "tree_last_source",
                    source.take(
                        48
                    )
                )
                .putString(
                    "tree_last_browser_package",
                    browserPackage.take(
                        120
                    )
                )
                .putString(
                    "tree_last_url_bar_id",
                    urlBarId?.take(
                        180
                    )
                )
                .putBoolean(
                    "tree_last_private_detected",
                    privateDetected
                )
                .putBoolean(
                    "tree_last_normal_detected",
                    normalDetected
                )
                .putString(
                    "tree_last_private_reason",
                    privateReason.take(
                        80
                    )
                )
                .putLong(
                    "tree_last_probe_ms",
                    nowMs
                )

        if (
            hostFound
        ) {
            edit.putInt(
                "tree_host_found_count",
                treeHostFoundCount() +
                    1
            )

            if (
                state ==
                "FOUND_FALLBACK"
            ) {
                edit.putInt(
                    "tree_fallback_host_count",
                    treeFallbackHostCount() +
                        1
                )
            } else {
                edit.putInt(
                    "tree_direct_host_count",
                    treeDirectHostCount() +
                        1
                )
            }
        } else if (
            state.startsWith(
                "MISSING"
            ) ||
            state ==
                "ROOT_NULL"
        ) {
            edit.putInt(
                "tree_missing_count",
                treeMissingCount() +
                    1
            )
        }

        if (
            privateDetected
        ) {
            edit.putInt(
                "tree_private_marker_count",
                treePrivateMarkerCount() +
                    1
            )
        }

        if (
            normalDetected
        ) {
            edit.putInt(
                "tree_normal_marker_count",
                treeNormalMarkerCount() +
                    1
            )
        }

        edit.apply()
    }

    fun recordTreeResourceIds(
        browserPackage: String,
        ids: List<String>,
        privateMode: Boolean,
        nowMs: Long =
            System.currentTimeMillis()
    ) {
        if (
            !BrowserCatalog.isSupported(
                browserPackage
            )
        ) {
            return
        }

        val safeIds =
            ids.asSequence()
                .filter {
                    it.contains(
                        ":id/"
                    )
                }
                .map {
                    it.take(
                        180
                    )
                }
                .distinct()
                .take(
                    96
                )
                .toList()

        val modeKey =
            treeResourceModeKey(
                browserPackage,
                privateMode
            )

        val existingModeIds =
            prefs.getString(
                modeKey,
                null
            )
                .orEmpty()
                .lineSequence()
                .map {
                    it.trim()
                }
                .filter {
                    it.contains(
                        ":id/"
                    )
                }
                .toList()

        val modeIds =
            (existingModeIds + safeIds)
                .asSequence()
                .distinct()
                .take(
                    128
                )
                .toList()

        prefs.edit()
            .putInt(
                "tree_resource_probe_count",
                treeResourceProbeCount() +
                    1
            )
            .putString(
                "tree_resource_browser_package",
                browserPackage.take(
                    120
                )
            )
            .putInt(
                "tree_resource_id_count",
                safeIds.size
            )
            .putString(
                "tree_resource_ids",
                safeIds.joinToString(
                    "\n"
                )
            )
            .putLong(
                "tree_resource_probe_ms",
                nowMs
            )
            .putString(
                modeKey,
                modeIds.joinToString(
                    "\n"
                )
            )
            .putInt(
                "${modeKey}_count",
                modeIds.size
            )
            .apply()
    }

    fun treeResourceIds(
        browserPackage: String,
        privateMode: Boolean
    ): List<String> =
        prefs.getString(
            treeResourceModeKey(
                browserPackage,
                privateMode
            ),
            null
        )
            .orEmpty()
            .lineSequence()
            .map {
                it.trim()
            }
            .filter {
                it.contains(
                    ":id/"
                )
            }
            .distinct()
            .take(
                128
            )
            .toList()

    private fun treeResourceModeKey(
        browserPackage: String,
        privateMode: Boolean
    ): String {
        val safePackage =
            browserPackage
                .replace(
                    Regex(
                        "[^A-Za-z0-9_]"
                    ),
                    "_"
                )
                .take(
                    100
                )

        val mode =
            if (
                privateMode
            ) {
                "private"
            } else {
                "normal"
            }

        return "tree_resource_ids_${safePackage}_$mode"
    }

    fun recordDetectorState(
        browserPackage: String,
        urlBarId: String?,
        privateMode: Boolean,
        privateReason: String,
        detectionSource: String =
            "UNKNOWN",
        nowMs: Long =
            System.currentTimeMillis()
    ) {
        if (
            !BrowserCatalog.isSupported(
                browserPackage
            )
        ) {
            return
        }

        prefs.edit()
            .putString(
                "last_browser_package",
                browserPackage.take(
                    120
                )
            )
            .putString(
                "last_url_bar_id",
                urlBarId?.take(
                    180
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
            .putString(
                "last_detection_source",
                detectionSource.take(
                    48
                )
            )
            .putLong(
                "last_detection_at_ms",
                nowMs
            )
            .apply()
    }

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

    fun recordWindowScreenshotRequested(
        windowId: Int,
        nowMs: Long =
            System.currentTimeMillis()
    ) {
        prefs.edit()
            .putInt(
                "visual_window_screenshot_request_count",
                visualWindowScreenshotRequestCount() +
                    1
            )
            .putInt(
                "visual_last_window_id",
                windowId
            )
            .putLong(
                "visual_last_window_screenshot_request_ms",
                nowMs
            )
            .apply()
    }

    fun recordWindowScreenshotSuccess(
        windowId: Int,
        nowMs: Long =
            System.currentTimeMillis()
    ) {
        prefs.edit()
            .putInt(
                "visual_window_screenshot_success_count",
                visualWindowScreenshotSuccessCount() +
                    1
            )
            .putInt(
                "visual_last_window_id",
                windowId
            )
            .putBoolean(
                "secure_browser_window",
                false
            )
            .remove(
                "visual_last_window_screenshot_error"
            )
            .putLong(
                "visual_last_window_screenshot_success_ms",
                nowMs
            )
            .apply()
    }

    fun recordWindowScreenshotFailure(
        windowId: Int,
        errorCode: Int,
        secureWindow: Boolean,
        nowMs: Long =
            System.currentTimeMillis()
    ) {
        val edit =
            prefs.edit()
                .putInt(
                    "visual_window_screenshot_failure_count",
                    visualWindowScreenshotFailureCount() +
                        1
                )
                .putInt(
                    "visual_last_window_id",
                    windowId
                )
                .putInt(
                    "visual_last_window_screenshot_error",
                    errorCode
                )
                .putBoolean(
                    "secure_browser_window",
                    secureWindow
                )
                .putLong(
                    "visual_last_window_screenshot_failure_ms",
                    nowMs
                )

        if (
            secureWindow
        ) {
            edit.putInt(
                "secure_browser_window_count",
                secureBrowserWindowCount() +
                    1
            )
        }

        edit.apply()
    }

    fun recordVisualOcr(
        host: String?,
        privateDetected: Boolean,
        privateReason: String,
        privateProbeAttempted: Boolean,
        nowMs: Long =
            System.currentTimeMillis()
    ) {
        val safeHost =
            host?.takeIf {
                BrowserDomainSanitizer.isSanitizedHost(
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
            privateProbeAttempted
        ) {
            // 0.1.24 corrects the old semantic bug: this key now means
            // actual probe attempts, not only positive detections.
            edit.putInt(
                "visual_private_probe_count",
                visualPrivateProbeCount() +
                    1
            )
        }

        if (
            privateDetected
        ) {
            edit.putInt(
                "visual_private_detected_count",
                visualPrivateDetectedCount() +
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

    fun serviceConnectedAtMs(): Long =
        prefs.getLong(
            "service_connected_at_ms",
            0L
        )

    fun lastHeartbeatMs(): Long =
        prefs.getLong(
            "last_heartbeat_ms",
            0L
        )

    fun serviceConnectionCount(): Int =
        prefs.getInt(
            "service_connection_count",
            0
        )

    fun accessibilityEventCount(): Int =
        prefs.getInt(
            "accessibility_event_count",
            0
        )

    fun lastEventType(): Int =
        prefs.getInt(
            "last_event_type",
            0
        )

    fun lastEventAtMs(): Long =
        prefs.getLong(
            "last_event_at_ms",
            0L
        )

    fun lastEventBrowserPackage(): String? =
        prefs.getString(
            "last_event_browser_package",
            null
        )

    fun eventSourceCount(): Int =
        prefs.getInt(
            "event_source_count",
            0
        )

    fun eventSourceNullCount(): Int =
        prefs.getInt(
            "event_source_null_count",
            0
        )

    fun eventRootResolvedCount(): Int =
        prefs.getInt(
            "event_root_resolved_count",
            0
        )

    fun lastEventSourceAtMs(): Long =
        prefs.getLong(
            "last_event_source_at_ms",
            0L
        )

    fun lastEventSourceBrowserPackage(): String? =
        prefs.getString(
            "last_event_source_browser_package",
            null
        )

    fun sampleCount(): Int =
        prefs.getInt(
            "sample_count",
            0
        )

    fun hostFoundCount(): Int =
        prefs.getInt(
            "host_found_count",
            0
        )

    fun focusedSkipCount(): Int =
        prefs.getInt(
            "focused_skip_count",
            0
        )

    fun missingCount(): Int =
        prefs.getInt(
            "missing_count",
            0
        )

    fun invalidCount(): Int =
        prefs.getInt(
            "invalid_count",
            0
        )

    fun sampleErrorCount(): Int =
        prefs.getInt(
            "sample_error_count",
            0
        )

    fun lastSampleState(): String? =
        prefs.getString(
            "last_sample_state",
            null
        )

    fun lastSampleAtMs(): Long =
        prefs.getLong(
            "last_sample_at_ms",
            0L
        )

    fun lastRootBrowserPackage(): String? =
        prefs.getString(
            "last_root_browser_package",
            null
        )

    fun lastSampleError(): String? =
        prefs.getString(
            "last_sample_error",
            null
        )

    fun lastSampleErrorAtMs(): Long =
        prefs.getLong(
            "last_sample_error_at_ms",
            0L
        )

    fun treeProbeCount(): Int =
        prefs.getInt(
            "tree_probe_count",
            0
        )

    fun treeHostFoundCount(): Int =
        prefs.getInt(
            "tree_host_found_count",
            0
        )

    fun treeDirectHostCount(): Int =
        prefs.getInt(
            "tree_direct_host_count",
            0
        )

    fun treeFallbackHostCount(): Int =
        prefs.getInt(
            "tree_fallback_host_count",
            0
        )

    fun treeMissingCount(): Int =
        prefs.getInt(
            "tree_missing_count",
            0
        )

    fun treePrivateMarkerCount(): Int =
        prefs.getInt(
            "tree_private_marker_count",
            0
        )

    fun treeNormalMarkerCount(): Int =
        prefs.getInt(
            "tree_normal_marker_count",
            0
        )

    fun treeLastState(): String? =
        prefs.getString(
            "tree_last_state",
            null
        )

    fun treeLastSource(): String? =
        prefs.getString(
            "tree_last_source",
            null
        )

    fun treeLastBrowserPackage(): String? =
        prefs.getString(
            "tree_last_browser_package",
            null
        )

    fun treeLastUrlBarId(): String? =
        prefs.getString(
            "tree_last_url_bar_id",
            null
        )

    fun treeLastPrivateDetected(): Boolean =
        prefs.getBoolean(
            "tree_last_private_detected",
            false
        )

    fun treeLastNormalDetected(): Boolean =
        prefs.getBoolean(
            "tree_last_normal_detected",
            false
        )

    fun treeLastPrivateReason(): String? =
        prefs.getString(
            "tree_last_private_reason",
            null
        )

    fun treeResourceProbeCount(): Int =
        prefs.getInt(
            "tree_resource_probe_count",
            0
        )

    fun treeResourceIdCount(): Int =
        prefs.getInt(
            "tree_resource_id_count",
            0
        )

    fun treeResourceBrowserPackage(): String? =
        prefs.getString(
            "tree_resource_browser_package",
            null
        )

    fun treeResourceIds(): List<String> =
        prefs.getString(
            "tree_resource_ids",
            ""
        )
            .orEmpty()
            .lineSequence()
            .map {
                it.trim()
            }
            .filter {
                it.contains(
                    ":id/"
                )
            }
            .distinct()
            .take(
                96
            )
            .toList()

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

    fun lastDetectionSource(): String? =
        prefs.getString(
            "last_detection_source",
            null
        )

    fun lastDetectionAtMs(): Long =
        prefs.getLong(
            "last_detection_at_ms",
            0L
        )

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

    fun visualWindowScreenshotRequestCount(): Int =
        prefs.getInt(
            "visual_window_screenshot_request_count",
            0
        )

    fun visualWindowScreenshotSuccessCount(): Int =
        prefs.getInt(
            "visual_window_screenshot_success_count",
            0
        )

    fun visualWindowScreenshotFailureCount(): Int =
        prefs.getInt(
            "visual_window_screenshot_failure_count",
            0
        )

    fun visualLastWindowScreenshotError(): Int =
        prefs.getInt(
            "visual_last_window_screenshot_error",
            0
        )

    fun secureBrowserWindow(): Boolean =
        prefs.getBoolean(
            "secure_browser_window",
            false
        )

    fun secureBrowserWindowCount(): Int =
        prefs.getInt(
            "secure_browser_window_count",
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

    fun visualPrivateDetectedCount(): Int =
        prefs.getInt(
            "visual_private_detected_count",
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
        val dynamicResourceEdit =
            prefs.edit()

        BrowserCatalog.supported.forEach { spec ->
            listOf(
                false,
                true
            ).forEach { privateMode ->
                val key =
                    treeResourceModeKey(
                        spec.packageName,
                        privateMode
                    )

                dynamicResourceEdit
                    .remove(
                        key
                    )
                    .remove(
                        "${key}_count"
                    )
            }
        }

        dynamicResourceEdit.apply()

        prefs.edit()
            .remove("tracking_started_at_ms")
            .remove("last_browser_package")
            .remove("last_url_bar_id")
            .remove("last_private_mode")
            .remove("last_private_reason")
            .remove("last_detection_source")
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
            .remove("sample_error_count")
            .remove("last_sample_state")
            .remove("last_sample_at_ms")
            .remove("last_root_browser_package")
            .remove("last_sample_error")
            .remove("last_sample_error_at_ms")
            .remove("tree_probe_count")
            .remove("tree_host_found_count")
            .remove("tree_direct_host_count")
            .remove("tree_fallback_host_count")
            .remove("tree_missing_count")
            .remove("tree_private_marker_count")
            .remove("tree_normal_marker_count")
            .remove("tree_last_state")
            .remove("tree_last_source")
            .remove("tree_last_browser_package")
            .remove("tree_last_url_bar_id")
            .remove("tree_last_private_detected")
            .remove("tree_last_normal_detected")
            .remove("tree_last_private_reason")
            .remove("tree_last_probe_ms")
            .remove("tree_resource_probe_count")
            .remove("tree_resource_browser_package")
            .remove("tree_resource_id_count")
            .remove("tree_resource_ids")
            .remove("tree_resource_probe_ms")
            .remove("visual_screenshot_request_count")
            .remove("visual_last_screenshot_request_ms")
            .remove("visual_screenshot_success_count")
            .remove("visual_last_screenshot_success_ms")
            .remove("visual_screenshot_failure_count")
            .remove("visual_last_screenshot_error")
            .remove("visual_last_screenshot_failure_ms")
            .remove("visual_window_screenshot_request_count")
            .remove("visual_window_screenshot_success_count")
            .remove("visual_window_screenshot_failure_count")
            .remove("visual_last_window_id")
            .remove("visual_last_window_screenshot_error")
            .remove("visual_last_window_screenshot_request_ms")
            .remove("visual_last_window_screenshot_success_ms")
            .remove("visual_last_window_screenshot_failure_ms")
            .remove("secure_browser_window")
            .remove("secure_browser_window_count")
            .remove("visual_ocr_run_count")
            .remove("visual_ocr_host_count")
            .remove("visual_private_probe_count")
            .remove("visual_private_detected_count")
            .remove("visual_pipeline_error_count")
            .remove("visual_last_host")
            .remove("visual_last_private_detected")
            .remove("visual_last_private_reason")
            .remove("visual_last_ocr_ms")
            .remove("visual_last_pipeline_error")
            .remove("visual_last_pipeline_error_ms")
            .apply()
    }

    companion object {
        // Consent v2 already explicitly covers transient screenshots + local OCR.
        // Hybrid v3 adds tree-first extraction but no new stored data category.
        const val CONSENT_VERSION =
            2
    }
}
