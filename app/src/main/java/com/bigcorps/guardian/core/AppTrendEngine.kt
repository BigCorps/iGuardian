package com.bigcorps.guardian.core

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.abs

enum class AppTrendPeriod {
    LAST_24_HOURS,
    LAST_7_DAYS
}

data class AppTrendDelta(
    val packageName: String,
    val name: String,
    val currentMilliseconds: Long,
    val previousMilliseconds: Long,
    val deltaMilliseconds: Long
)

data class AppTrendResult(
    val period: AppTrendPeriod,
    val ready: Boolean,
    val currentHistoryPercent: Double,
    val previousHistoryPercent: Double,
    val currentCoveragePercent: Double,
    val previousCoveragePercent: Double,
    val increases: List<AppTrendDelta>,
    val decreases: List<AppTrendDelta>,
    val changedAppsCount: Int
)

class AppTrendEngine(
    private val context: Context
) {
    companion object {
        const val VERSION =
            1

        const val MIN_MEANINGFUL_DELTA_MS =
            60_000L
    }

    fun analyze(
        period: AppTrendPeriod,
        nowMs: Long =
            System.currentTimeMillis()
    ): AppTrendResult {
        val duration =
            when (period) {
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

        val previousStart =
            currentStart -
                duration

        val generator =
            ReportGenerator(
                context
            )

        val current =
            generator.periodJson(
                currentStart,
                nowMs
            )

        val previous =
            generator.periodJson(
                previousStart,
                currentStart
            )

        val currentTracking =
            current.getJSONObject(
                "tracking"
            )

        val previousTracking =
            previous.getJSONObject(
                "tracking"
            )

        val currentHistory =
            currentTracking.optDouble(
                "history_availability_percent",
                0.0
            )

        val previousHistory =
            previousTracking.optDouble(
                "history_availability_percent",
                0.0
            )

        val currentCoverage =
            currentTracking.optDouble(
                "coverage_percent",
                0.0
            )

        val previousCoverage =
            previousTracking.optDouble(
                "coverage_percent",
                0.0
            )

        val ready =
            HistoryReadiness.canCompare(
                currentHistory,
                previousHistory,
                currentCoverage,
                previousCoverage
            )

        if (
            !ready
        ) {
            return AppTrendResult(
                period =
                    period,
                ready =
                    false,
                currentHistoryPercent =
                    currentHistory,
                previousHistoryPercent =
                    previousHistory,
                currentCoveragePercent =
                    currentCoverage,
                previousCoveragePercent =
                    previousCoverage,
                increases =
                    emptyList(),
                decreases =
                    emptyList(),
                changedAppsCount =
                    0
            )
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

        val deltas =
            packages
                .map {
                    pkg ->
                    val currentItem =
                        currentApps[
                            pkg
                        ]

                    val previousItem =
                        previousApps[
                            pkg
                        ]

                    val currentMs =
                        currentItem
                            ?.milliseconds
                            ?: 0L

                    val previousMs =
                        previousItem
                            ?.milliseconds
                            ?: 0L

                    AppTrendDelta(
                        packageName =
                            pkg,
                        name =
                            currentItem
                                ?.name
                                ?: previousItem
                                    ?.name
                                ?: pkg,
                        currentMilliseconds =
                            currentMs,
                        previousMilliseconds =
                            previousMs,
                        deltaMilliseconds =
                            currentMs -
                                previousMs
                    )
                }
                .filter {
                    abs(
                        it.deltaMilliseconds
                    ) >=
                        MIN_MEANINGFUL_DELTA_MS
                }

        val increases =
            deltas
                .filter {
                    it.deltaMilliseconds >
                        0L
                }
                .sortedByDescending {
                    it.deltaMilliseconds
                }

        val decreases =
            deltas
                .filter {
                    it.deltaMilliseconds <
                        0L
                }
                .sortedBy {
                    it.deltaMilliseconds
                }

        return AppTrendResult(
            period =
                period,
            ready =
                true,
            currentHistoryPercent =
                currentHistory,
            previousHistoryPercent =
                previousHistory,
            currentCoveragePercent =
                currentCoverage,
            previousCoveragePercent =
                previousCoverage,
            increases =
                increases,
            decreases =
                decreases,
            changedAppsCount =
                deltas.size
        )
    }

    fun summaryText(
        period: AppTrendPeriod,
        nowMs: Long =
            System.currentTimeMillis()
    ): String {
        val result =
            analyze(
                period,
                nowMs
            )

        val label =
            when (period) {
                AppTrendPeriod.LAST_24_HOURS ->
                    "Tendências por app — últimas 24h"

                AppTrendPeriod.LAST_7_DAYS ->
                    "Tendências por app — últimos 7 dias"
            }

        if (
            !result.ready
        ) {
            return "$label: histórico insuficiente para comparar com segurança. " +
                "Disponibilidade: ${
                    percentLabel(
                        result.currentHistoryPercent
                    )
                } vs ${
                    percentLabel(
                        result.previousHistoryPercent
                    )
                }."
        }

        if (
            result.changedAppsCount ==
            0
        ) {
            return "$label: nenhuma mudança de pelo menos 1 minuto foi detectada entre os períodos."
        }

        val rise =
            result.increases
                .take(
                    3
                )
                .joinToString(
                    "; "
                ) {
                    "${it.name} +${
                        durationLabel(
                            it.deltaMilliseconds
                        )
                    }"
                }
                .ifBlank {
                    "nenhuma alta relevante"
                }

        val fall =
            result.decreases
                .take(
                    3
                )
                .joinToString(
                    "; "
                ) {
                    "${it.name} -${
                        durationLabel(
                            -it.deltaMilliseconds
                        )
                    }"
                }
                .ifBlank {
                    "nenhuma queda relevante"
                }

        return "$label: em alta: $rise. Em queda: $fall."
    }

    fun asJson(
        period: AppTrendPeriod,
        nowMs: Long =
            System.currentTimeMillis()
    ): JSONObject {
        val result =
            analyze(
                period,
                nowMs
            )

        fun arrayOf(
            values: List<AppTrendDelta>
        ): JSONArray =
            JSONArray().apply {
                values.forEach {
                    item ->
                    put(
                        JSONObject().apply {
                            put(
                                "package",
                                item.packageName
                            )
                            put(
                                "name",
                                item.name
                            )
                            put(
                                "current_milliseconds",
                                item.currentMilliseconds
                            )
                            put(
                                "previous_milliseconds",
                                item.previousMilliseconds
                            )
                            put(
                                "delta_milliseconds",
                                item.deltaMilliseconds
                            )
                        }
                    )
                }
            }

        return JSONObject().apply {
            put(
                "engine_version",
                VERSION
            )
            put(
                "period",
                period.name
            )
            put(
                "ready",
                result.ready
            )
            put(
                "current_history_percent",
                result.currentHistoryPercent
            )
            put(
                "previous_history_percent",
                result.previousHistoryPercent
            )
            put(
                "current_coverage_percent",
                result.currentCoveragePercent
            )
            put(
                "previous_coverage_percent",
                result.previousCoveragePercent
            )
            put(
                "changed_apps_count",
                result.changedAppsCount
            )
            put(
                "increases",
                arrayOf(
                    result.increases
                )
            )
            put(
                "decreases",
                arrayOf(
                    result.decreases
                )
            )
            put(
                "summary",
                summaryText(
                    period,
                    nowMs
                )
            )
        }
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
