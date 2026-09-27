package com.bigcorps.guardian.web

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object BrowserReport {
    fun todayJson(
        context: Context,
        nowMs: Long = System.currentTimeMillis(),
        accessStatusSnapshot: BrowserWebAccessStatus? = null
    ): JSONObject =
        periodJson(
            context,
            startOfDay(nowMs),
            nowMs,
            accessStatusSnapshot
        )

    fun periodJson(
        context: Context,
        startMs: Long,
        endMs: Long,
        accessStatusSnapshot: BrowserWebAccessStatus? = null
    ): JSONObject {
        val db = com.bigcorps.guardian.core.GuardianDatabase(context)
        val rows = db.browserSessionsBetween(startMs, endMs)

        data class Aggregate(
            var totalMs: Long = 0L,
            var normalMs: Long = 0L,
            var privateMs: Long = 0L,
            var sessions: Int = 0
        )

        val byHost = linkedMapOf<String, Aggregate>()
        var totalMs = 0L
        var normalMs = 0L
        var privateMs = 0L

        rows.forEach { row ->
            val clippedStart = maxOf(row.startMs, startMs)
            val clippedEnd = minOf(row.endMs, endMs)
            val duration = (clippedEnd - clippedStart).coerceAtLeast(0L)
            if (duration <= 0L) return@forEach

            totalMs += duration
            if (row.privateMode) privateMs += duration else normalMs += duration

            val aggregate = byHost.getOrPut(row.host) { Aggregate() }
            aggregate.totalMs += duration
            if (row.privateMode) aggregate.privateMs += duration else aggregate.normalMs += duration
            aggregate.sessions += 1
        }

        val domains = JSONArray().apply {
            byHost.entries
                .sortedByDescending { it.value.totalMs }
                .forEach { (host, aggregate) ->
                    put(JSONObject().apply {
                        put("host", host)
                        put("foreground_milliseconds", aggregate.totalMs)
                        put("foreground_seconds", aggregate.totalMs / 1000L)
                        put("normal_milliseconds", aggregate.normalMs)
                        put("anonymous_milliseconds", aggregate.privateMs)
                        put("sessions", aggregate.sessions)
                    })
                }
        }

        val prefs = BrowserWebPreferences(context)
        val accessStatus = accessStatusSnapshot ?: BrowserWebAccess.status(context, endMs)

        return JSONObject().apply {
            put("schema_version", 1)
            put("period_start", iso(startMs))
            put("period_end", iso(endMs))
            put(
                "tracking_started_at",
                prefs.trackingStartedAtMs().takeIf { it > 0L }?.let(::iso) ?: JSONObject.NULL
            )
            put("consent_granted", prefs.consented())
            put("accessibility_service_enabled", accessStatus.enabled)
            put("accessibility_service_manager_reported", accessStatus.managerReported)
            put("accessibility_service_secure_setting_reported", accessStatus.secureSettingReported)
            put("accessibility_service_alive", accessStatus.alive)
            put("accessibility_status_snapshot_timestamp_ms", endMs)
            put("accessibility_status_same_timestamp_contract", true)

            put("tree_probe_count", prefs.treeProbeCount())
            put("tree_host_found_count", prefs.treeHostFoundCount())
            put("tree_direct_host_count", prefs.treeDirectHostCount())
            put("tree_fallback_host_count", prefs.treeFallbackHostCount())
            put("tree_missing_count", prefs.treeMissingCount())
            put("tree_private_marker_count", prefs.treePrivateMarkerCount())
            put("tree_normal_marker_count", prefs.treeNormalMarkerCount())
            put("tree_last_normal_detected", prefs.treeLastNormalDetected())
            put("tree_last_state", prefs.treeLastState() ?: JSONObject.NULL)
            put("tree_last_source", prefs.treeLastSource() ?: JSONObject.NULL)
            put("tree_last_url_bar_id", prefs.treeLastUrlBarId() ?: JSONObject.NULL)
            put("tree_resource_probe_count", prefs.treeResourceProbeCount())
            put("tree_resource_id_count", prefs.treeResourceIdCount())
            put("tree_resource_browser_package", prefs.treeResourceBrowserPackage() ?: JSONObject.NULL)
            put(
                "browser_tree_resource_ids",
                JSONArray().apply {
                    prefs.treeResourceIds().forEach { id -> put(id) }
                }
            )
            put(
                "browser_tree_resource_ids_by_package",
                JSONArray().apply {
                    BrowserCatalog.supported.forEach { spec ->
                        val normalIds =
                            prefs.treeResourceIds(
                                spec.packageName,
                                false
                            )

                        val privateIds =
                            prefs.treeResourceIds(
                                spec.packageName,
                                true
                            )

                        if (
                            normalIds.isNotEmpty() ||
                            privateIds.isNotEmpty()
                        ) {
                            put(
                                JSONObject().apply {
                                    put(
                                        "package",
                                        spec.packageName
                                    )
                                    put(
                                        "normal",
                                        JSONArray().apply {
                                            normalIds.forEach { id -> put(id) }
                                        }
                                    )
                                    put(
                                        "private_when_detected",
                                        JSONArray().apply {
                                            privateIds.forEach { id -> put(id) }
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            )

            put("visual_screenshot_request_count", prefs.visualScreenshotRequestCount())
            put("visual_screenshot_success_count", prefs.visualScreenshotSuccessCount())
            put("visual_screenshot_failure_count", prefs.visualScreenshotFailureCount())
            put("visual_last_screenshot_error", prefs.visualLastScreenshotError())
            put("visual_window_screenshot_request_count", prefs.visualWindowScreenshotRequestCount())
            put("visual_window_screenshot_success_count", prefs.visualWindowScreenshotSuccessCount())
            put("visual_window_screenshot_failure_count", prefs.visualWindowScreenshotFailureCount())
            put("visual_last_window_screenshot_error", prefs.visualLastWindowScreenshotError())
            put("secure_browser_window", prefs.secureBrowserWindow())
            put("secure_browser_window_count", prefs.secureBrowserWindowCount())
            put("visual_ocr_run_count", prefs.visualOcrRunCount())
            put("visual_ocr_host_count", prefs.visualOcrHostCount())
            put("visual_private_probe_count", prefs.visualPrivateProbeCount())
            put("visual_private_detected_count", prefs.visualPrivateDetectedCount())
            put("last_detection_source", prefs.lastDetectionSource() ?: JSONObject.NULL)

            put("total_milliseconds", totalMs)
            put("normal_milliseconds", normalMs)
            put("anonymous_milliseconds", privateMs)
            put("domain_count", byHost.size)
            put("domains", domains)
            put("storage_policy", "host_only_no_path_query_fragment_title_content")
            put("browser_overlay_not_additive_to_app_total", true)
        }
    }

    private fun startOfDay(nowMs: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = nowMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun iso(ms: Long): String =
        SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            Locale.US
        ).format(Date(ms))
}
