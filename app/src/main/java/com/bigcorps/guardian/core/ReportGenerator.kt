package com.bigcorps.guardian.core

import android.content.Context
import com.bigcorps.guardian.web.BrowserReport
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.round

class ReportGenerator(private val context: Context) {
    private val db = GuardianDatabase(context)
    private val state = CollectorStateStore(context)

    data class Summary(
        val appMilliseconds: Long,
        val privateMilliseconds: Long,
        val screenOffMilliseconds: Long,
        val systemMilliseconds: Long,
        val anonymousMilliseconds: Long,
        val appSeconds: Long,
        val privateSeconds: Long,
        val screenOffSeconds: Long,
        val systemSeconds: Long,
        val anonymousSeconds: Long,
        val unlockCount: Int,
        val topApps: List<AppAggregate>
    )

    data class AppAggregate(
        val packageName: String,
        val name: String,
        val milliseconds: Long,
        val seconds: Long,
        val sessions: Int
    )

    fun todayJson(nowMs: Long = System.currentTimeMillis()): JSONObject =
        periodJson(startOfDay(nowMs), nowMs)

    fun todaySummary(nowMs: Long = System.currentTimeMillis()): Summary {
        val start = startOfDay(nowMs)
        return summarize(normalizedIntervals(start, nowMs), start, nowMs)
    }

    fun periodJson(startMs: Long, endMs: Long): JSONObject {
        val intervals = normalizedIntervals(startMs, endMs)
        val summary = summarize(intervals, startMs, endMs)
        val browser =
            BrowserReport.periodJson(
                context,
                startMs,
                endMs
            )

        val apps = JSONArray()
        summary.topApps.forEach { app ->
            apps.put(
                JSONObject().apply {
                    put("package", app.packageName)
                    put("name", app.name)
                    put("foreground_milliseconds", app.milliseconds)
                    put("foreground_seconds", app.seconds)
                    put("sessions", app.sessions)
                }
            )
        }

        val timeline = JSONArray()
        intervals.forEach { item ->
            timeline.put(
                JSONObject().apply {
                    put("type", item.type.name)
                    put("start_at", iso(item.startMs))
                    put("end_at", iso(item.endMs))
                    val durationMs =
                        (item.endMs - item.startMs).coerceAtLeast(0L)
                    put("duration_milliseconds", durationMs)
                    put("duration_seconds", durationMs / 1000L)

                    if (item.type == IntervalType.APP) {
                        put("package", item.packageName ?: JSONObject.NULL)
                        put(
                            "name",
                            item.appLabel ?: item.packageName ?: "Unknown"
                        )
                    }
                }
            )
        }

        val requestedPeriodMilliseconds =
            (endMs - startMs).coerceAtLeast(0L)

        val effectiveStart = effectiveTrackingStart(startMs, endMs)
        val effectivePeriodMilliseconds =
            (endMs - effectiveStart).coerceAtLeast(0L)

        val historyAvailabilityPercent =
            if (requestedPeriodMilliseconds > 0L) {
                round(
                    effectivePeriodMilliseconds.toDouble() *
                        1000.0 /
                        requestedPeriodMilliseconds.toDouble()
                ) / 10.0
            } else {
                0.0
            }

        val recordedMilliseconds =
            summary.appMilliseconds +
                summary.privateMilliseconds +
                summary.screenOffMilliseconds +
                summary.systemMilliseconds +
                summary.anonymousMilliseconds

        val unclassifiedMilliseconds =
            (effectivePeriodMilliseconds - recordedMilliseconds).coerceAtLeast(0L)

        val effectivePeriodSeconds = effectivePeriodMilliseconds / 1000L
        val recordedSeconds = recordedMilliseconds / 1000L
        val unclassifiedSeconds = unclassifiedMilliseconds / 1000L

        val coveragePercent =
            if (effectivePeriodMilliseconds > 0L) {
                round(
                    recordedMilliseconds.toDouble() *
                        1000.0 /
                        effectivePeriodMilliseconds.toDouble()
                ) / 10.0
            } else {
                0.0
            }

        return JSONObject().apply {
            put("schema_version", 5)
            put(
                "date",
                SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(startMs))
            )
            put("period_start", iso(startMs))
            put("period_end", iso(endMs))

            put(
                "tracking",
                JSONObject().apply {
                    put(
                        "tracking_started_at",
                        state.trackingStartMs()
                            .takeIf { it > 0L }
                            ?.let(::iso)
                            ?: JSONObject.NULL
                    )
                    put("effective_period_start", iso(effectiveStart))
                    put("requested_period_milliseconds", requestedPeriodMilliseconds)
                    put("requested_period_seconds", requestedPeriodMilliseconds / 1000L)
                    put("history_available_milliseconds", effectivePeriodMilliseconds)
                    put("history_available_seconds", effectivePeriodMilliseconds / 1000L)
                    put("history_availability_percent", historyAvailabilityPercent)
                    put("effective_period_milliseconds", effectivePeriodMilliseconds)
                    put("effective_period_seconds", effectivePeriodSeconds)
                    put("recorded_milliseconds", recordedMilliseconds)
                    put("recorded_seconds", recordedSeconds)
                    put("unclassified_milliseconds", unclassifiedMilliseconds)
                    put("unclassified_seconds", unclassifiedSeconds)
                    put("coverage_percent", coveragePercent)
                    put("overlap_resolution", "privacy_precedence_v1")
                    put("aggregation_precision", "milliseconds")
                }
            )

            put("device", DeviceInfo.asJson(context))

            put(
                "summary",
                JSONObject().apply {
                    put("app_usage_milliseconds", summary.appMilliseconds)
                    put("app_usage_seconds", summary.appSeconds)
                    put("app_usage_minutes", summary.appSeconds / 60L)
                    put("screen_off_milliseconds", summary.screenOffMilliseconds)
                    put("screen_off_seconds", summary.screenOffSeconds)
                    put("screen_off_minutes", summary.screenOffSeconds / 60L)
                    put("private_milliseconds", summary.privateMilliseconds)
                    put("private_seconds", summary.privateSeconds)
                    put("private_minutes", summary.privateSeconds / 60L)
                    put("system_milliseconds", summary.systemMilliseconds)
                    put("system_seconds", summary.systemSeconds)
                    put("system_minutes", summary.systemSeconds / 60L)
                    val anonymousBrowserMs =
                        browser.optLong(
                            "anonymous_milliseconds",
                            0L
                        )

                    put(
                        "anonymous_browser_milliseconds",
                        anonymousBrowserMs
                    )
                    put(
                        "anonymous_browser_seconds",
                        anonymousBrowserMs /
                            1000L
                    )
                    put(
                        "anonymous_browser_minutes",
                        anonymousBrowserMs /
                            60_000L
                    )
                    put(
                        "browser_total_milliseconds",
                        browser.optLong(
                            "total_milliseconds",
                            0L
                        )
                    )
                    put(
                        "browser_overlay_not_additive",
                        true
                    )
                    put("unlock_count", summary.unlockCount)
                }
            )

            put(
                "capabilities",
                JSONObject().apply {
                    put("app_usage", true)
                    put("screen_events", true)
                    put(
                        "screen_events_mode",
                        if (android.os.Build.VERSION.SDK_INT >= 28)
                            "usage_stats"
                        else
                            "runtime_best_effort"
                    )
                    put(
                        "unlock_detection_mode",
                        if (android.os.Build.VERSION.SDK_INT >= 28)
                            "usage_stats_distinct_timestamp"
                        else
                            "runtime_best_effort"
                    )
                    put("duration_precision", "milliseconds+seconds")
                    put("history_availability_tracking", true)
                    put("session_definition", "normalized_foreground_intervals")
                    put("system_surface_separation", true)
                    put("browser_domains", true)
                    put("browser_domains_storage", "host_only")
                    put("anonymous_browser_detection", true)
                    put("anonymous_browser_schema_ready", true)
                    put("browser_overlay_not_additive_to_app_total", true)
                }
            )

            put("apps", apps)
            put("browser", browser)
            put("timeline", timeline)

            put(
                "privacy",
                JSONObject().apply {
                    put("private_reason_stored", false)
                    put("full_urls_stored", false)
                    put("content_capture", false)
                    put("system_surface_identity_stored", false)
                }
            )
        }
    }

    private fun summarize(
        intervals: List<TimelineInterval>,
        startMs: Long,
        endMs: Long
    ): Summary {
        var appMs = 0L
        var privateMs = 0L
        var screenOffMs = 0L
        var systemMs = 0L
        var anonymousMs = 0L

        data class MutableAgg(
            var name: String,
            var millis: Long = 0L,
            var sessions: Int = 0
        )

        val appMap = linkedMapOf<String, MutableAgg>()

        intervals.forEach { interval ->
            val millis =
                (interval.endMs - interval.startMs).coerceAtLeast(0L)

            when (interval.type) {
                IntervalType.APP -> {
                    appMs += millis
                    val pkg = interval.packageName ?: return@forEach
                    val agg = appMap.getOrPut(pkg) {
                        MutableAgg(interval.appLabel ?: pkg)
                    }
                    agg.millis += millis
                    agg.sessions += 1

                    if (!interval.appLabel.isNullOrBlank()) {
                        agg.name = interval.appLabel
                    }
                }

                IntervalType.PRIVATE -> privateMs += millis
                IntervalType.SCREEN_OFF -> screenOffMs += millis
                IntervalType.SYSTEM -> systemMs += millis
                IntervalType.ANONYMOUS_BROWSER -> anonymousMs += millis
            }
        }

        val topApps = appMap.mapNotNull { (pkg, agg) ->
            val seconds = agg.millis / 1000L

            if (agg.millis <= 0L) {
                null
            } else {
                AppAggregate(
                    packageName = pkg,
                    name = agg.name,
                    milliseconds = agg.millis,
                    seconds = seconds,
                    sessions = agg.sessions
                )
            }
        }.sortedByDescending { it.milliseconds }

        return Summary(
            appMilliseconds = appMs,
            privateMilliseconds = privateMs,
            screenOffMilliseconds = screenOffMs,
            systemMilliseconds = systemMs,
            anonymousMilliseconds = anonymousMs,
            appSeconds = appMs / 1000L,
            privateSeconds = privateMs / 1000L,
            screenOffSeconds = screenOffMs / 1000L,
            systemSeconds = systemMs / 1000L,
            anonymousSeconds = anonymousMs / 1000L,
            unlockCount = db.technicalCount("UNLOCK", startMs, endMs),
            topApps = topApps
        )
    }

    private fun normalizedIntervals(
        startMs: Long,
        endMs: Long
    ): List<TimelineInterval> {
        val all = db.intervalsBetween(startMs, endMs).toMutableList()

        state.openState()?.let { open ->
            if (endMs > open.startMs && open.startMs < endMs) {
                all += TimelineInterval(
                    startMs = open.startMs,
                    endMs = endMs,
                    type = open.type,
                    packageName = open.packageName,
                    appLabel = open.appLabel
                )
            }
        }

        return TimelineNormalizer.resolve(
            intervals = all,
            startMs = startMs,
            endMs = endMs
        )
    }

    private fun effectiveTrackingStart(
        periodStartMs: Long,
        periodEndMs: Long
    ): Long {
        val trackingStart = state.trackingStartMs()
        if (trackingStart <= 0L) return periodStartMs

        return minOf(
            periodEndMs,
            maxOf(periodStartMs, trackingStart)
        )
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
