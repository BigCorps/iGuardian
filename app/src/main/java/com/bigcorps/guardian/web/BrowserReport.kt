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
        nowMs: Long =
            System.currentTimeMillis()
    ): JSONObject =
        periodJson(
            context,
            startOfDay(
                nowMs
            ),
            nowMs
        )

    fun periodJson(
        context: Context,
        startMs: Long,
        endMs: Long
    ): JSONObject {
        val db =
            com.bigcorps.guardian.core.GuardianDatabase(
                context
            )

        val rows =
            db.browserSessionsBetween(
                startMs,
                endMs
            )

        data class Aggregate(
            var totalMs: Long =
                0L,
            var normalMs: Long =
                0L,
            var privateMs: Long =
                0L,
            var sessions: Int =
                0
        )

        val byHost =
            linkedMapOf<
                String,
                Aggregate
                >()

        var totalMs =
            0L

        var normalMs =
            0L

        var privateMs =
            0L

        rows.forEach {
            row ->
            val clippedStart =
                maxOf(
                    row.startMs,
                    startMs
                )

            val clippedEnd =
                minOf(
                    row.endMs,
                    endMs
                )

            val duration =
                (
                    clippedEnd -
                        clippedStart
                    )
                    .coerceAtLeast(
                        0L
                    )

            if (
                duration <=
                0L
            ) {
                return@forEach
            }

            totalMs +=
                duration

            if (
                row.privateMode
            ) {
                privateMs +=
                    duration
            } else {
                normalMs +=
                    duration
            }

            val aggregate =
                byHost.getOrPut(
                    row.host
                ) {
                    Aggregate()
                }

            aggregate.totalMs +=
                duration

            if (
                row.privateMode
            ) {
                aggregate.privateMs +=
                    duration
            } else {
                aggregate.normalMs +=
                    duration
            }

            aggregate.sessions +=
                1
        }

        val domains =
            JSONArray().apply {
                byHost
                    .entries
                    .sortedByDescending {
                        it.value.totalMs
                    }
                    .forEach {
                        (host, aggregate) ->
                        put(
                            JSONObject().apply {
                                put(
                                    "host",
                                    host
                                )
                                put(
                                    "foreground_milliseconds",
                                    aggregate.totalMs
                                )
                                put(
                                    "foreground_seconds",
                                    aggregate.totalMs /
                                        1000L
                                )
                                put(
                                    "normal_milliseconds",
                                    aggregate.normalMs
                                )
                                put(
                                    "anonymous_milliseconds",
                                    aggregate.privateMs
                                )
                                put(
                                    "sessions",
                                    aggregate.sessions
                                )
                            }
                        )
                    }
            }

        val prefs =
            BrowserWebPreferences(
                context
            )

        return JSONObject().apply {
            put(
                "schema_version",
                1
            )
            put(
                "period_start",
                iso(
                    startMs
                )
            )
            put(
                "period_end",
                iso(
                    endMs
                )
            )
            put(
                "tracking_started_at",
                prefs.trackingStartedAtMs()
                    .takeIf {
                        it >
                        0L
                    }
                    ?.let {
                        iso(
                            it
                        )
                    }
                    ?: JSONObject.NULL
            )
            put(
                "consent_granted",
                prefs.consented()
            )
            val accessStatus =
                BrowserWebAccess.status(
                    context,
                    endMs
                )

            put("accessibility_service_enabled", accessStatus.enabled)
            put("accessibility_service_manager_reported", accessStatus.managerReported)
            put("accessibility_service_secure_setting_reported", accessStatus.secureSettingReported)
            put("accessibility_service_alive", accessStatus.alive)
            put(
                "total_milliseconds",
                totalMs
            )
            put(
                "normal_milliseconds",
                normalMs
            )
            put(
                "anonymous_milliseconds",
                privateMs
            )
            put(
                "domain_count",
                byHost.size
            )
            put(
                "domains",
                domains
            )
            put(
                "storage_policy",
                "host_only_no_path_query_fragment_title_content"
            )
            put(
                "browser_overlay_not_additive_to_app_total",
                true
            )
        }
    }

    private fun startOfDay(
        nowMs: Long
    ): Long =
        Calendar.getInstance().apply {
            timeInMillis =
                nowMs

            set(
                Calendar.HOUR_OF_DAY,
                0
            )

            set(
                Calendar.MINUTE,
                0
            )

            set(
                Calendar.SECOND,
                0
            )

            set(
                Calendar.MILLISECOND,
                0
            )
        }.timeInMillis

    private fun iso(
        ms: Long
    ): String =
        SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            Locale.US
        ).format(
            Date(
                ms
            )
        )
}
