package com.bigcorps.guardian.core

import android.content.Context
import com.bigcorps.guardian.web.BrowserReport
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ValidationPackGenerator {
    const val PACK_SCHEMA =
        10

    fun generate(
        context: Context
    ): JSONObject {
        val now =
            System.currentTimeMillis()

        val generationStarted =
            android.os.SystemClock.elapsedRealtime()

        val daily =
            ReportGenerator(
                context
            ).todayJson(
                now
            )

        val dailyReadyAt =
            android.os.SystemClock.elapsedRealtime()

        val diagnosticCore =
            DiagnosticsGenerator(
                context
            ).generate(
                includeActivitySnapshot =
                    false,
                nowMs =
                    now
            )

        val diagnosticReadyAt =
            android.os.SystemClock.elapsedRealtime()

        val validation =
            ValidationSuite.run(
                context,
                daily,
                diagnosticCore
            )

        val validationReadyAt =
            android.os.SystemClock.elapsedRealtime()

        val criticalPassed =
            validation.optBoolean(
                "critical_passed",
                false
            )

        val engine =
            LocalQuestionEngine(
                context
            )

        val autoInsights =
            JSONObject().apply {
                put(
                    "summary_today",
                    engine.answer(
                        "Resumo de hoje",
                        now
                    ).text
                )
                put(
                    "insights_today",
                    engine.answer(
                        "Insights de hoje",
                        now
                    ).text
                )
                put(
                    "trend_last_24h",
                    engine.answer(
                        "Compare as últimas 24 horas com as 24 anteriores",
                        now
                    ).text
                )
                put(
                    "trend_last_7d",
                    engine.answer(
                        "Compare os últimos 7 dias com os 7 anteriores",
                        now
                    ).text
                )
                put(
                    "generated_from_fixed_internal_prompts",
                    true
                )
                put(
                    "user_query_content_stored",
                    false
                )
            }

        val dayMs =
            24L *
                60L *
                60L *
                1000L

        fun availability(
            start: Long,
            end: Long
        ): Double =
            ReportGenerator(
                context
            ).periodJson(
                start,
                end
            )
                .getJSONObject(
                    "tracking"
                )
                .optDouble(
                    "history_availability_percent",
                    0.0
                )

        val dataMaturity =
            JSONObject().apply {
                put(
                    "current_24h_history_percent",
                    availability(
                        now - dayMs,
                        now
                    )
                )
                put(
                    "previous_24h_history_percent",
                    availability(
                        now - 2L * dayMs,
                        now - dayMs
                    )
                )
                put(
                    "current_7d_history_percent",
                    availability(
                        now - 7L * dayMs,
                        now
                    )
                )
                put(
                    "previous_7d_history_percent",
                    availability(
                        now - 14L * dayMs,
                        now - 7L * dayMs
                    )
                )
                put(
                    "comparison_min_history_percent",
                    HistoryReadiness.MIN_HISTORY_PERCENT
                )
            }

        val appTrendEngine =
            AppTrendEngine(
                context
            )

        val appTrends =
            JSONObject().apply {
                put(
                    "last_24h",
                    appTrendEngine.asJson(
                        AppTrendPeriod.LAST_24_HOURS,
                        now
                    )
                )
                put(
                    "last_7d",
                    appTrendEngine.asJson(
                        AppTrendPeriod.LAST_7_DAYS,
                        now
                    )
                )
            }

        val trendDashboard =
            TrendDashboardEngine(
                context
            )

        val trendDashboardJson =
            JSONObject().apply {
                put(
                    "last_24h",
                    trendDashboard.asJson(
                        AppTrendPeriod.LAST_24_HOURS,
                        now
                    )
                )
                put(
                    "last_7d",
                    trendDashboard.asJson(
                        AppTrendPeriod.LAST_7_DAYS,
                        now
                    )
                )
            }

        val diagnostic =
            if (
                criticalPassed
            ) {
                compactDiagnostic(
                    diagnosticCore
                )
            } else {
                DiagnosticsGenerator(
                    context
                ).generate(
                    includeActivitySnapshot =
                        true,
                    nowMs =
                        now
                )
            }

        val exportedDaily =
            if (
                criticalPassed
            ) {
                compactReport(
                    daily
                )
            } else {
                daily
            }

        val sourceTimelineCount =
            daily.optJSONArray(
                "timeline"
            )
                ?.length()
                ?: 0

        val packMeta =
            JSONObject().apply {
                put(
                    "mode",
                    if (
                        criticalPassed
                    ) {
                        "compact_success"
                    } else {
                        "full_failure_evidence"
                    }
                )
                put(
                    "compact_success_enabled",
                    true
                )
                put(
                    "full_evidence_on_failure",
                    true
                )
                put(
                    "full_daily_timeline_embedded",
                    !criticalPassed
                )
                put(
                    "full_24h_snapshot_embedded",
                    !criticalPassed
                )
                put(
                    "source_daily_timeline_intervals",
                    sourceTimelineCount
                )
                put(
                    "single_snapshot_timestamp",
                    true
                )
            }

        val timings =
            JSONObject().apply {
                put(
                    "daily_report_ms",
                    dailyReadyAt -
                        generationStarted
                )
                put(
                    "diagnostic_core_ms",
                    diagnosticReadyAt -
                        dailyReadyAt
                )
                put(
                    "validation_suite_ms",
                    validationReadyAt -
                        diagnosticReadyAt
                )
                put(
                    "pre_export_total_ms",
                    android.os.SystemClock.elapsedRealtime() -
                        generationStarted
                )
            }

        val result =
            JSONObject().apply {
                put(
                    "validation_pack_schema",
                    PACK_SCHEMA
                )
                put(
                    "generated_at",
                    SimpleDateFormat(
                        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                        Locale.US
                    ).format(
                        Date(
                            now
                        )
                    )
                )
                put(
                    "pack_meta",
                    packMeta
                )
                put(
                    "generation_metrics",
                    timings
                )
                put(
                    "validation_lineage",
                    runCatching {
                        ValidationLineage.snapshot(
                            context
                        )
                    }.getOrElse {
                        JSONObject().apply {
                            put(
                                "error",
                                it::class.java.simpleName
                            )
                        }
                    }
                )
                put(
                    "guardian_web",
                    BrowserReport.todayJson(
                        context,
                        now
                    )
                )
                put(
                    "guardian_web_test",
                    JSONObject().apply {
                        put("required_this_round", true)
                        put("normal_domain_seconds_target", 20)
                        put("anonymous_domain_seconds_target", 20)
                        put("recommended_browser", "Chrome Dev")
                        put("full_url_stored", false)
                    }
                )
                put(
                    "validation",
                    validation
                )
                put(
                    "auto_insights",
                    autoInsights
                )
                put(
                    "app_trends",
                    appTrends
                )
                put(
                    "trend_dashboard",
                    trendDashboardJson
                )
                put(
                    "data_maturity",
                    dataMaturity
                )
                put(
                    "daily_report",
                    exportedDaily
                )
                put(
                    "diagnostic",
                    diagnostic
                )
            }

        packMeta.put(
            "estimated_compact_json_bytes",
            result.toString()
                .toByteArray(
                    Charsets.UTF_8
                )
                .size
        )

        return result
    }

    private fun compactReport(
        source: JSONObject
    ): JSONObject {
        val compact =
            JSONObject()

        listOf(
            "schema_version",
            "date",
            "period_start",
            "period_end",
            "tracking",
            "device",
            "summary",
            "capabilities",
            "apps",
            "browser"
        ).forEach {
            key ->
            if (
                source.has(
                    key
                )
            ) {
                compact.put(
                    key,
                    source.get(
                        key
                    )
                )
            }
        }

        val timeline =
            source.optJSONArray(
                "timeline"
            )

        if (
            timeline !=
            null
        ) {
            compact.put(
                "timeline_evidence",
                timelineEvidence(
                    timeline
                )
            )
        }

        compact.put(
            "full_timeline_embedded",
            false
        )

        return compact
    }

    private fun compactDiagnostic(
        source: JSONObject
    ): JSONObject {
        val compact =
            JSONObject()

        listOf(
            "diagnostic_schema",
            "generated_at",
            "app",
            "device",
            "environment",
            "permissions",
            "runtime",
            "scheduler",
            "interval_counts",
            "capabilities",
            "browser_web",
            "local_intelligence_self_check",
            "recent_technical_events",
            "privacy_guarantees"
        ).forEach {
            key ->
            if (
                source.has(
                    key
                )
            ) {
                compact.put(
                    key,
                    source.get(
                        key
                    )
                )
            }
        }

        compact.put(
            "activity_snapshot_24h_included",
            false
        )

        return compact
    }

    private fun timelineEvidence(
        timeline: JSONArray
    ): JSONObject {
        val counts =
            linkedMapOf(
                "APP" to 0,
                "PRIVATE" to 0,
                "SCREEN_OFF" to 0,
                "SYSTEM" to 0,
                "ANONYMOUS_BROWSER" to 0
            )

        var totalMs =
            0L

        var firstStart: String? =
            null

        var lastEnd: String? =
            null

        for (
            index in
            0 until timeline.length()
        ) {
            val item =
                timeline.getJSONObject(
                    index
                )

            val type =
                item.optString(
                    "type"
                )

            if (
                counts.containsKey(
                    type
                )
            ) {
                counts[
                    type
                ] =
                    (
                        counts[
                            type
                        ]
                            ?: 0
                        ) +
                        1
            }

            totalMs +=
                item.optLong(
                    "duration_milliseconds",
                    0L
                )

            if (
                index ==
                0
            ) {
                firstStart =
                    item.optString(
                        "start_at"
                    )
            }

            lastEnd =
                item.optString(
                    "end_at"
                )
        }

        return JSONObject().apply {
            put(
                "interval_count",
                timeline.length()
            )
            put(
                "total_interval_milliseconds",
                totalMs
            )
            put(
                "first_start_at",
                firstStart
                    ?: JSONObject.NULL
            )
            put(
                "last_end_at",
                lastEnd
                    ?: JSONObject.NULL
            )
            put(
                "category_interval_counts",
                JSONObject().apply {
                    counts.forEach {
                        (key, value) ->
                        put(
                            key,
                            value
                        )
                    }
                }
            )
            put(
                "full_timeline_embedded",
                false
            )
        }
    }
}
