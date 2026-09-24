package com.bigcorps.guardian.core

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ReportGenerator(private val context: Context) {
    private val db = GuardianDatabase(context)
    private val state = CollectorStateStore(context)

    data class Summary(
        val appSeconds: Long,
        val privateSeconds: Long,
        val screenOffSeconds: Long,
        val anonymousSeconds: Long,
        val unlockCount: Int,
        val topApps: List<AppAggregate>
    )

    data class AppAggregate(
        val packageName: String,
        val name: String,
        val seconds: Long,
        val sessions: Int
    )

    fun todayJson(nowMs: Long = System.currentTimeMillis()): JSONObject {
        val start = startOfDay(nowMs)
        return periodJson(start, nowMs)
    }

    fun todaySummary(nowMs: Long = System.currentTimeMillis()): Summary {
        val start = startOfDay(nowMs)
        val intervals = normalizedIntervals(start, nowMs)
        return summarize(intervals, start, nowMs)
    }

    fun periodJson(startMs: Long, endMs: Long): JSONObject {
        val intervals = normalizedIntervals(startMs, endMs)
        val summary = summarize(intervals, startMs, endMs)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        val apps = JSONArray()
        summary.topApps.forEach { app ->
            apps.put(JSONObject().apply {
                put("package", app.packageName)
                put("name", app.name)
                put("foreground_seconds", app.seconds)
                put("sessions", app.sessions)
            })
        }

        val timeline = JSONArray()
        intervals.forEach { item ->
            timeline.put(JSONObject().apply {
                put("type", item.type.name)
                put("start_at", iso(item.startMs))
                put("end_at", iso(item.endMs))
                put("duration_seconds", (item.endMs - item.startMs) / 1000L)
                if (item.type == IntervalType.APP) {
                    put("package", item.packageName ?: JSONObject.NULL)
                    put("name", item.appLabel ?: item.packageName ?: "Unknown")
                }
            })
        }

        return JSONObject().apply {
            put("schema_version", 1)
            put("date", dateFormat.format(Date(startMs)))
            put("period_start", iso(startMs))
            put("period_end", iso(endMs))
            put("device", DeviceInfo.asJson(context))
            put("summary", JSONObject().apply {
                put("app_usage_minutes", summary.appSeconds / 60L)
                put("screen_off_minutes", summary.screenOffSeconds / 60L)
                put("private_minutes", summary.privateSeconds / 60L)
                put("anonymous_browser_minutes", JSONObject.NULL)
                put("unlock_count", summary.unlockCount)
            })
            put("capabilities", JSONObject().apply {
                put("app_usage", true)
                put("screen_events", true)
                put("screen_events_mode", if (android.os.Build.VERSION.SDK_INT >= 28) "usage_stats" else "runtime_best_effort")
                put("unlock_detection_mode", if (android.os.Build.VERSION.SDK_INT >= 28) "usage_stats" else "runtime_best_effort")
                put("browser_domains", false)
                put("anonymous_browser_detection", false)
                put("anonymous_browser_schema_ready", true)
            })
            put("apps", apps)
            put("timeline", timeline)
            put("privacy", JSONObject().apply {
                put("private_reason_stored", false)
                put("full_urls_stored", false)
                put("content_capture", false)
            })
        }
    }

    private fun summarize(intervals: List<TimelineInterval>, startMs: Long, endMs: Long): Summary {
        var appSeconds = 0L
        var privateSeconds = 0L
        var screenOffSeconds = 0L
        var anonymousSeconds = 0L
        data class MutableAgg(var name: String, var seconds: Long = 0, var sessions: Int = 0)
        val appMap = linkedMapOf<String, MutableAgg>()

        intervals.forEach { interval ->
            val seconds = ((interval.endMs - interval.startMs).coerceAtLeast(0L)) / 1000L
            when (interval.type) {
                IntervalType.APP -> {
                    appSeconds += seconds
                    val pkg = interval.packageName ?: return@forEach
                    val agg = appMap.getOrPut(pkg) { MutableAgg(interval.appLabel ?: pkg) }
                    agg.seconds += seconds
                    agg.sessions += 1
                    if (!interval.appLabel.isNullOrBlank()) agg.name = interval.appLabel
                }
                IntervalType.PRIVATE -> privateSeconds += seconds
                IntervalType.SCREEN_OFF -> screenOffSeconds += seconds
                IntervalType.ANONYMOUS_BROWSER -> anonymousSeconds += seconds
            }
        }

        val topApps = appMap.map { (pkg, agg) ->
            AppAggregate(pkg, agg.name, agg.seconds, agg.sessions)
        }.sortedByDescending { it.seconds }

        return Summary(
            appSeconds = appSeconds,
            privateSeconds = privateSeconds,
            screenOffSeconds = screenOffSeconds,
            anonymousSeconds = anonymousSeconds,
            unlockCount = db.technicalCount("UNLOCK", startMs, endMs),
            topApps = topApps
        )
    }

    private fun normalizedIntervals(startMs: Long, endMs: Long): List<TimelineInterval> {
        val all = db.intervalsBetween(startMs, endMs).toMutableList()
        state.openState()?.let { open ->
            val virtualEnd = endMs
            if (virtualEnd > open.startMs && open.startMs < endMs) {
                all += TimelineInterval(
                    startMs = open.startMs,
                    endMs = virtualEnd,
                    type = open.type,
                    packageName = open.packageName,
                    appLabel = open.appLabel
                )
            }
        }

        val clipped = all.mapNotNull { i ->
            val s = maxOf(i.startMs, startMs)
            val e = minOf(i.endMs, endMs)
            if (e <= s) null else i.copy(startMs = s, endMs = e)
        }.sortedBy { it.startMs }

        val merged = mutableListOf<TimelineInterval>()
        clipped.forEach { current ->
            val previous = merged.lastOrNull()
            val sameIdentity = previous != null && previous.type == current.type &&
                (current.type != IntervalType.APP || previous.packageName == current.packageName)
            val touches = previous != null && current.startMs <= previous.endMs + 2_000L

            if (sameIdentity && touches) {
                merged[merged.lastIndex] = previous!!.copy(
                    endMs = maxOf(previous.endMs, current.endMs),
                    appLabel = previous.appLabel ?: current.appLabel
                )
            } else {
                merged += current
            }
        }
        return merged
    }

    private fun startOfDay(nowMs: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = nowMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun iso(ms: Long): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US).format(Date(ms))
}
