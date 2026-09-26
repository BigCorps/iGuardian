package com.bigcorps.guardian.core

import android.content.Context
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ValidationPackGenerator {
    const val PACK_SCHEMA = 6

    fun generate(context: Context): JSONObject {
        val daily = ReportGenerator(context).todayJson()
        val diagnostic = DiagnosticsGenerator(context).generate()
        val validation = ValidationSuite.run(context, daily, diagnostic)
        val engine = LocalQuestionEngine(context)

        val autoInsights = JSONObject().apply {
            put("summary_today", engine.answer("Resumo de hoje").text)
            put("insights_today", engine.answer("Insights de hoje").text)
            put(
                "trend_last_24h",
                engine.answer("Compare as últimas 24 horas com as 24 anteriores").text
            )
            put(
                "trend_last_7d",
                engine.answer("Compare os últimos 7 dias com os 7 anteriores").text
            )
            put("generated_from_fixed_internal_prompts", true)
            put("user_query_content_stored", false)
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
                        System.currentTimeMillis() - dayMs,
                        System.currentTimeMillis()
                    )
                )
                put(
                    "previous_24h_history_percent",
                    availability(
                        System.currentTimeMillis() - 2L * dayMs,
                        System.currentTimeMillis() - dayMs
                    )
                )
                put(
                    "current_7d_history_percent",
                    availability(
                        System.currentTimeMillis() - 7L * dayMs,
                        System.currentTimeMillis()
                    )
                )
                put(
                    "previous_7d_history_percent",
                    availability(
                        System.currentTimeMillis() - 14L * dayMs,
                        System.currentTimeMillis() - 7L * dayMs
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
                        AppTrendPeriod.LAST_24_HOURS
                    )
                )
                put(
                    "last_7d",
                    appTrendEngine.asJson(
                        AppTrendPeriod.LAST_7_DAYS
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
                        AppTrendPeriod.LAST_24_HOURS
                    )
                )
                put(
                    "last_7d",
                    trendDashboard.asJson(
                        AppTrendPeriod.LAST_7_DAYS
                    )
                )
            }

        return JSONObject().apply {
            put("validation_pack_schema", PACK_SCHEMA)
            put(
                "generated_at",
                SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                    Locale.US
                ).format(Date())
            )
            put("validation", validation)
            put("auto_insights", autoInsights)
            put("app_trends", appTrends)
            put("trend_dashboard", trendDashboardJson)
            put("data_maturity", dataMaturity)
            put("daily_report", daily)
            put("diagnostic", diagnostic)
        }
    }
}
