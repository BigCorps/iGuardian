package com.bigcorps.guardian.core

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

data class TrendDashboardSnapshot(
    val period: AppTrendPeriod,
    val title: String,
    val ready: Boolean,
    val currentHistoryPercent: Double,
    val previousHistoryPercent: Double,
    val currentCoveragePercent: Double,
    val previousCoveragePercent: Double,
    val generalTrendText: String,
    val appTrendText: String
)

class TrendDashboardEngine(
    private val context: Context
) {
    companion object {
        const val VERSION =
            1
    }

    fun snapshot(
        period: AppTrendPeriod,
        nowMs: Long =
            System.currentTimeMillis()
    ): TrendDashboardSnapshot {
        val appTrendEngine =
            AppTrendEngine(
                context
            )

        val appTrend =
            appTrendEngine.analyze(
                period,
                nowMs
            )

        val generalQuestion =
            when (
                period
            ) {
                AppTrendPeriod.LAST_24_HOURS ->
                    "Compare as últimas 24 horas com as 24 anteriores"

                AppTrendPeriod.LAST_7_DAYS ->
                    "Compare os últimos 7 dias com os 7 anteriores"
            }

        val generalTrend =
            LocalQuestionEngine(
                context
            ).answer(
                generalQuestion,
                nowMs
            ).text

        val title =
            when (
                period
            ) {
                AppTrendPeriod.LAST_24_HOURS ->
                    "Últimas 24 horas"

                AppTrendPeriod.LAST_7_DAYS ->
                    "Últimos 7 dias"
            }

        return TrendDashboardSnapshot(
            period =
                period,
            title =
                title,
            ready =
                appTrend.ready,
            currentHistoryPercent =
                appTrend.currentHistoryPercent,
            previousHistoryPercent =
                appTrend.previousHistoryPercent,
            currentCoveragePercent =
                appTrend.currentCoveragePercent,
            previousCoveragePercent =
                appTrend.previousCoveragePercent,
            generalTrendText =
                generalTrend,
            appTrendText =
                appTrendEngine.summaryText(
                    period,
                    nowMs
                )
        )
    }

    fun appDetailText(
        query: String,
        period: AppTrendPeriod,
        nowMs: Long =
            System.currentTimeMillis()
    ): String {
        val cleanQuery =
            query.trim()

        if (
            cleanQuery.isBlank()
        ) {
            return "Digite o nome de um aplicativo para ver a comparação."
        }

        val windows =
            windows(
                period,
                nowMs
            )

        val generator =
            ReportGenerator(
                context
            )

        val current =
            generator.periodJson(
                windows.currentStart,
                windows.currentEnd
            )

        val previous =
            generator.periodJson(
                windows.previousStart,
                windows.previousEnd
            )

        val currentTracking =
            current.getJSONObject(
                "tracking"
            )

        val previousTracking =
            previous.getJSONObject(
                "tracking"
            )

        val ready =
            HistoryReadiness.canCompare(
                currentTracking.optDouble(
                    "history_availability_percent",
                    0.0
                ),
                previousTracking.optDouble(
                    "history_availability_percent",
                    0.0
                ),
                currentTracking.optDouble(
                    "coverage_percent",
                    0.0
                ),
                previousTracking.optDouble(
                    "coverage_percent",
                    0.0
                )
            )

        if (
            !ready
        ) {
            return "Histórico insuficiente para comparar esse aplicativo em ${periodLabel(period)} com segurança. " +
                "Disponibilidade: ${
                    percentLabel(
                        currentTracking.optDouble(
                            "history_availability_percent",
                            0.0
                        )
                    )
                } vs ${
                    percentLabel(
                        previousTracking.optDouble(
                            "history_availability_percent",
                            0.0
                        )
                    )
                }."
        }

        val currentApps =
            appMap(
                current.getJSONArray(
                    "apps"
                )
            )

        val previousApps =
            appMap(
                previous.getJSONArray(
                    "apps"
                )
            )

        val packages =
            (
                currentApps.keys +
                    previousApps.keys
                )
                .toSortedSet()

        val normalizedQuery =
            LocalQuestionIntentParser
                .normalize(
                    cleanQuery
                )

        val selectedPackage =
            packages
                .firstOrNull {
                    pkg ->
                    val currentValue =
                        currentApps[
                            pkg
                        ]

                    val previousValue =
                        previousApps[
                            pkg
                        ]

                    val name =
                        currentValue
                            ?.name
                            ?: previousValue
                                ?.name
                            ?: pkg

                    val normalizedName =
                        LocalQuestionIntentParser
                            .normalize(
                                name
                            )

                    val normalizedPackage =
                        LocalQuestionIntentParser
                            .normalize(
                                pkg
                            )

                    normalizedName ==
                        normalizedQuery ||
                        normalizedPackage ==
                            normalizedQuery
                }
                ?: packages
                    .sortedByDescending {
                        pkg ->
                        (
                            currentApps[
                                pkg
                            ]
                                ?.name
                                ?: previousApps[
                                    pkg
                                ]
                                    ?.name
                                ?: pkg
                            )
                            .length
                    }
                    .firstOrNull {
                        pkg ->
                        val name =
                            currentApps[
                                pkg
                            ]
                                ?.name
                                ?: previousApps[
                                    pkg
                                ]
                                    ?.name
                                ?: pkg

                        val normalizedName =
                            LocalQuestionIntentParser
                                .normalize(
                                    name
                                )

                        normalizedName.contains(
                            normalizedQuery
                        ) ||
                            normalizedQuery.contains(
                                normalizedName
                            )
                    }

        if (
            selectedPackage ==
            null
        ) {
            return "Não encontrei “$cleanQuery” nos dois períodos comparados."
        }

        val currentValue =
            currentApps[
                selectedPackage
            ]

        val previousValue =
            previousApps[
                selectedPackage
            ]

        val name =
            currentValue
                ?.name
                ?: previousValue
                    ?.name
                ?: selectedPackage

        val currentMs =
            currentValue
                ?.milliseconds
                ?: 0L

        val previousMs =
            previousValue
                ?.milliseconds
                ?: 0L

        val delta =
            currentMs -
                previousMs

        val deltaText =
            when {
                delta >
                    0L ->
                    "+${
                        durationLabel(
                            delta
                        )
                    }"

                delta <
                    0L ->
                    "-${
                        durationLabel(
                            -delta
                        )
                    }"

                else ->
                    "sem mudança"
            }

        return "$name — ${periodLabel(period)}: ${
            durationLabel(
                currentMs
            )
        } agora vs ${
            durationLabel(
                previousMs
            )
        } no período anterior. Diferença: $deltaText."
    }

    fun asJson(
        period: AppTrendPeriod,
        nowMs: Long =
            System.currentTimeMillis()
    ): JSONObject {
        val snapshot =
            snapshot(
                period,
                nowMs
            )

        return JSONObject().apply {
            put(
                "engine_version",
                VERSION
            )
            put(
                "period",
                snapshot.period.name
            )
            put(
                "title",
                snapshot.title
            )
            put(
                "ready",
                snapshot.ready
            )
            put(
                "current_history_percent",
                snapshot.currentHistoryPercent
            )
            put(
                "previous_history_percent",
                snapshot.previousHistoryPercent
            )
            put(
                "current_coverage_percent",
                snapshot.currentCoveragePercent
            )
            put(
                "previous_coverage_percent",
                snapshot.previousCoveragePercent
            )
            put(
                "general_trend",
                snapshot.generalTrendText
            )
            put(
                "app_trend",
                snapshot.appTrendText
            )
        }
    }

    private data class PeriodWindows(
        val currentStart: Long,
        val currentEnd: Long,
        val previousStart: Long,
        val previousEnd: Long
    )

    private fun windows(
        period: AppTrendPeriod,
        nowMs: Long
    ): PeriodWindows {
        val duration =
            when (
                period
            ) {
                AppTrendPeriod.LAST_24_HOURS ->
                    24L *
                        60L *
                        60L *
                        1000L

                AppTrendPeriod.LAST_7_DAYS ->
                    7L *
                        24L *
                        60L *
                        60L *
                        1000L
            }

        val currentStart =
            nowMs -
                duration

        return PeriodWindows(
            currentStart =
                currentStart,
            currentEnd =
                nowMs,
            previousStart =
                currentStart -
                    duration,
            previousEnd =
                currentStart
        )
    }

    private data class AppValue(
        val name: String,
        val milliseconds: Long
    )

    private fun appMap(
        apps: JSONArray
    ): Map<String, AppValue> {
        val result =
            linkedMapOf<
                String,
                AppValue
                >()

        for (
            index in
            0 until apps.length()
        ) {
            val item =
                apps.getJSONObject(
                    index
                )

            val pkg =
                item.optString(
                    "package"
                )

            if (
                pkg.isBlank()
            ) {
                continue
            }

            result[
                pkg
            ] =
                AppValue(
                    name =
                        item.optString(
                            "name",
                            pkg
                        ),
                    milliseconds =
                        item.optLong(
                            "foreground_milliseconds",
                            item.optLong(
                                "foreground_seconds",
                                0L
                            ) *
                                1000L
                        )
                )
        }

        return result
    }

    private fun periodLabel(
        period: AppTrendPeriod
    ): String =
        when (
            period
        ) {
            AppTrendPeriod.LAST_24_HOURS ->
                "últimas 24 horas"

            AppTrendPeriod.LAST_7_DAYS ->
                "últimos 7 dias"
        }

    private fun percentLabel(
        value: Double
    ): String =
        String.format(
            Locale.forLanguageTag(
                "pt-BR"
            ),
            "%.1f%%",
            value
        )

    private fun durationLabel(
        milliseconds: Long
    ): String {
        val seconds =
            milliseconds /
                1000L

        if (
            seconds <
            60L
        ) {
            return "${seconds}s"
        }

        val minutes =
            seconds /
                60L

        if (
            minutes <
            60L
        ) {
            val rest =
                seconds %
                    60L

            return if (
                rest ==
                0L
            ) {
                "${minutes}m"
            } else {
                "${minutes}m ${rest}s"
            }
        }

        val hours =
            minutes /
                60L

        val restMinutes =
            minutes %
                60L

        return if (
            restMinutes ==
            0L
        ) {
            "${hours}h"
        } else {
            "${hours}h ${restMinutes}m"
        }
    }
}
