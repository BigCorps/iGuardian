package com.bigcorps.guardian.core

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.Normalizer
import java.util.Calendar
import java.util.Locale

enum class LocalQuestionIntent {
    SUMMARY,
    TOP_APP,
    TOP_APPS,
    APP_USAGE,
    SCREEN_OFF,
    UNLOCKS,
    PRIVATE,
    SYSTEM,
    COVERAGE,
    INSIGHTS,
    COMPARE_TODAY_YESTERDAY,
    HELP
}

enum class LocalQuestionPeriod {
    TODAY,
    YESTERDAY,
    LAST_24_HOURS,
    LAST_7_DAYS,
    ROLLING_HOURS,
    ROLLING_DAYS
}

data class LocalQuestionPlan(
    val intent: LocalQuestionIntent,
    val period: LocalQuestionPeriod,
    val amount: Int = 0
)

object LocalQuestionIntentParser {
    private val rollingHoursRegex =
        Regex(
            """(?:ultimas|ultimos)\s+(\d{1,3})\s*(?:h|hora|horas)\b"""
        )

    private val rollingDaysRegex =
        Regex(
            """(?:ultimos|ultimas)\s+(\d{1,2})\s*(?:dia|dias)\b"""
        )

    fun normalize(value: String): String {
        val noAccents =
            Normalizer.normalize(
                value,
                Normalizer.Form.NFD
            ).replace(
                Regex("\\p{M}+"),
                ""
            )

        return noAccents
            .lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
    }

    fun parse(raw: String): LocalQuestionIntent =
        plan(raw).intent

    fun plan(raw: String): LocalQuestionPlan {
        val q = normalize(raw)

        val isCompare =
            q.contains("compare") ||
                q.contains("comparar") ||
                q.contains("hoje ou ontem") ||
                q.contains("hoje com ontem")

        // Comparison is a two-period intent. TODAY is the reference period
        // rather than allowing the word "ontem" to relabel the whole plan.
        if (isCompare) {
            return LocalQuestionPlan(
                intent =
                    LocalQuestionIntent.COMPARE_TODAY_YESTERDAY,
                period =
                    LocalQuestionPeriod.TODAY
            )
        }

        val periodPlan =
            parsePeriod(q)

        val intent =
            when {
                q.isBlank() ->
                    LocalQuestionIntent.HELP

                q.contains("insight") ||
                    q.contains("destaque") ||
                    q.contains("resumo inteligente") ->
                    LocalQuestionIntent.INSIGHTS

                q.contains("top 5") ||
                    q.contains("top cinco") ||
                    q.contains("mais usados") ||
                    q.contains("apps mais usados") ->
                    LocalQuestionIntent.TOP_APPS

                (
                    q.contains("qual app") ||
                        q.contains("qual aplicativo")
                    ) &&
                    (
                        q.contains("mais") ||
                            q.contains("maior")
                        ) ->
                    LocalQuestionIntent.TOP_APP

                q.contains("mais usei") &&
                    (
                        q.contains("app") ||
                            q.contains("aplicativo")
                        ) ->
                    LocalQuestionIntent.TOP_APP

                q.contains("desbloq") ->
                    LocalQuestionIntent.UNLOCKS

                (
                    q.contains("tela") &&
                        (
                            q.contains("deslig") ||
                                q.contains("apag")
                            )
                    ) ||
                    q.contains("screen off") ->
                    LocalQuestionIntent.SCREEN_OFF

                q.contains("private") ||
                    q.contains("privado") ->
                    LocalQuestionIntent.PRIVATE

                q.contains("navegacao do sistema") ||
                    q.contains("tempo do sistema") ->
                    LocalQuestionIntent.SYSTEM

                q.contains("cobertura") ||
                    q.contains("quanto foi registrado") ||
                    q.contains("dados confiaveis") ->
                    LocalQuestionIntent.COVERAGE

                q.contains("tempo em apps") ||
                    q.contains("tempo de uso") ||
                    q.contains("quanto usei o celular") ||
                    q.contains("quanto usei os apps") ->
                    LocalQuestionIntent.APP_USAGE

                q.contains("resumo") ||
                    q == "hoje" ||
                    q == "ontem" ||
                    q.contains("como foi meu dia") ||
                    q.contains("o que fiz hoje") ||
                    q.contains("o que fiz ontem") ->
                    LocalQuestionIntent.SUMMARY

                else ->
                    LocalQuestionIntent.HELP
            }

        return LocalQuestionPlan(
            intent = intent,
            period = periodPlan.first,
            amount = periodPlan.second
        )
    }

    private fun parsePeriod(
        q: String
    ): Pair<LocalQuestionPeriod, Int> {
        if (q.contains("ontem")) {
            return LocalQuestionPeriod.YESTERDAY to 0
        }

        val hours =
            rollingHoursRegex.find(q)
                ?.groupValues
                ?.getOrNull(1)
                ?.toIntOrNull()

        if (hours != null) {
            if (hours == 24) {
                return LocalQuestionPeriod.LAST_24_HOURS to 24
            }

            if (hours in 1..720) {
                return LocalQuestionPeriod.ROLLING_HOURS to hours
            }
        }

        val days =
            rollingDaysRegex.find(q)
                ?.groupValues
                ?.getOrNull(1)
                ?.toIntOrNull()

        if (days != null) {
            if (days == 7) {
                return LocalQuestionPeriod.LAST_7_DAYS to 7
            }

            if (days in 1..90) {
                return LocalQuestionPeriod.ROLLING_DAYS to days
            }
        }

        if (
            q.contains("24h") ||
            q.contains("24 horas")
        ) {
            return LocalQuestionPeriod.LAST_24_HOURS to 24
        }

        if (
            q.contains("sete dias") ||
            q.contains("ultima semana") ||
            q.contains("essa semana")
        ) {
            return LocalQuestionPeriod.LAST_7_DAYS to 7
        }

        return LocalQuestionPeriod.TODAY to 0
    }
}

data class LocalQuestionAnswer(
    val intent: LocalQuestionIntent,
    val period: LocalQuestionPeriod,
    val text: String
)

class LocalQuestionEngine(
    private val context: Context
) {
    fun answer(
        question: String,
        nowMs: Long = System.currentTimeMillis()
    ): LocalQuestionAnswer {
        val plan =
            LocalQuestionIntentParser.plan(question)

        if (
            plan.intent ==
            LocalQuestionIntent.COMPARE_TODAY_YESTERDAY
        ) {
            return compareTodayYesterday(nowMs)
        }

        val range =
            rangeFor(plan, nowMs)

        val report =
            ReportGenerator(context).periodJson(
                range.startMs,
                range.endMs
            )

        val summary =
            report.getJSONObject("summary")
        val tracking =
            report.getJSONObject("tracking")
        val apps =
            report.getJSONArray("apps")
        val normalized =
            LocalQuestionIntentParser.normalize(question)

        val matchingApp =
            findNamedApp(
                normalized,
                apps
            )

        val asksForTime =
            listOf(
                "quanto",
                "tempo",
                "usei",
                "ficou"
            ).any {
                normalized.contains(it)
            }

        if (
            matchingApp != null &&
            asksForTime
        ) {
            return LocalQuestionAnswer(
                intent =
                    LocalQuestionIntent.APP_USAGE,
                period =
                    plan.period,
                text =
                    "${periodLabel(plan)}: ${
                        matchingApp.getString("name")
                    } ficou registrado por ${
                        durationMillisecondsLabel(
                            matchingApp.optLong(
                                "foreground_milliseconds",
                                matchingApp.getLong(
                                    "foreground_seconds"
                                ) * 1000L
                            )
                        )
                    } em primeiro plano."
            )
        }

        return when (plan.intent) {
            LocalQuestionIntent.SUMMARY ->
                summaryAnswer(
                    plan,
                    summary,
                    tracking,
                    apps
                )

            LocalQuestionIntent.TOP_APP ->
                topAppAnswer(
                    plan,
                    apps
                )

            LocalQuestionIntent.TOP_APPS ->
                topAppsAnswer(
                    plan,
                    apps
                )

            LocalQuestionIntent.APP_USAGE ->
                LocalQuestionAnswer(
                    plan.intent,
                    plan.period,
                    "${periodLabel(plan)}: ${
                        durationMillisecondsLabel(
                            summaryMilliseconds(
                                summary,
                                "app_usage"
                            )
                        )
                    } de uso registrado em aplicativos."
                )

            LocalQuestionIntent.SCREEN_OFF ->
                LocalQuestionAnswer(
                    plan.intent,
                    plan.period,
                    "${periodLabel(plan)}: a tela ficou desligada por ${
                        durationMillisecondsLabel(
                            summaryMilliseconds(
                                summary,
                                "screen_off"
                            )
                        )
                    } no período acompanhado."
                )

            LocalQuestionIntent.UNLOCKS ->
                LocalQuestionAnswer(
                    plan.intent,
                    plan.period,
                    "${periodLabel(plan)}: foram detectados ${
                        summary.getInt("unlock_count")
                    } desbloqueios."
                )

            LocalQuestionIntent.PRIVATE ->
                LocalQuestionAnswer(
                    plan.intent,
                    plan.period,
                    "${periodLabel(plan)}: houve ${
                        durationMillisecondsLabel(
                            summaryMilliseconds(
                                summary,
                                "private"
                            )
                        )
                    } classificados como PRIVATE. O Guardian não guarda qual app ou motivo gerou esses períodos."
                )

            LocalQuestionIntent.SYSTEM ->
                LocalQuestionAnswer(
                    plan.intent,
                    plan.period,
                    "${periodLabel(plan)}: a navegação técnica do sistema somou ${
                        durationMillisecondsLabel(
                            summaryMilliseconds(
                                summary,
                                "system"
                            )
                        )
                    }. Ela não entra no ranking de aplicativos."
                )

            LocalQuestionIntent.COVERAGE ->
                LocalQuestionAnswer(
                    plan.intent,
                    plan.period,
                    coverageAnswer(
                        plan,
                        tracking
                    )
                )

            LocalQuestionIntent.INSIGHTS ->
                insightsAnswer(
                    plan,
                    report
                )

            LocalQuestionIntent.HELP ->
                LocalQuestionAnswer(
                    plan.intent,
                    plan.period,
                    helpText()
                )

            LocalQuestionIntent.COMPARE_TODAY_YESTERDAY ->
                compareTodayYesterday(nowMs)
        }
    }

    private data class Range(
        val startMs: Long,
        val endMs: Long
    )

    private fun rangeFor(
        plan: LocalQuestionPlan,
        nowMs: Long
    ): Range {
        val todayStart =
            startOfDay(nowMs)

        return when (plan.period) {
            LocalQuestionPeriod.TODAY ->
                Range(
                    todayStart,
                    nowMs
                )

            LocalQuestionPeriod.YESTERDAY -> {
                val yesterdayStart =
                    Calendar.getInstance()
                        .apply {
                            timeInMillis =
                                todayStart
                            add(
                                Calendar.DAY_OF_YEAR,
                                -1
                            )
                        }
                        .timeInMillis

                Range(
                    yesterdayStart,
                    todayStart
                )
            }

            LocalQuestionPeriod.LAST_24_HOURS ->
                Range(
                    nowMs -
                        24L *
                        60L *
                        60L *
                        1000L,
                    nowMs
                )

            LocalQuestionPeriod.LAST_7_DAYS ->
                Range(
                    nowMs -
                        7L *
                        24L *
                        60L *
                        60L *
                        1000L,
                    nowMs
                )

            LocalQuestionPeriod.ROLLING_HOURS ->
                Range(
                    nowMs -
                        plan.amount
                            .coerceIn(
                                1,
                                720
                            )
                            .toLong() *
                        60L *
                        60L *
                        1000L,
                    nowMs
                )

            LocalQuestionPeriod.ROLLING_DAYS ->
                Range(
                    nowMs -
                        plan.amount
                            .coerceIn(
                                1,
                                90
                            )
                            .toLong() *
                        24L *
                        60L *
                        60L *
                        1000L,
                    nowMs
                )
        }
    }

    private fun insightsAnswer(
        plan: LocalQuestionPlan,
        report: JSONObject
    ): LocalQuestionAnswer {
        val summary =
            report.getJSONObject("summary")
        val tracking =
            report.getJSONObject("tracking")
        val apps =
            report.getJSONArray("apps")
        val timeline =
            report.getJSONArray("timeline")

        val appUsageMs =
            summaryMilliseconds(
                summary,
                "app_usage"
            )

        val topLine =
            if (
                apps.length() > 0 &&
                appUsageMs > 0L
            ) {
                val top =
                    apps.getJSONObject(0)

                val topMs =
                    top.optLong(
                        "foreground_milliseconds",
                        top.getLong(
                            "foreground_seconds"
                        ) * 1000L
                    )

                val share =
                    topMs.toDouble() *
                        100.0 /
                        appUsageMs.toDouble()

                "• ${
                    top.getString("name")
                } liderou o tempo em apps: ${
                    durationMillisecondsLabel(topMs)
                } (${percentLabel(share)} do uso de apps)."
            } else {
                "• Ainda não há uso de apps suficiente para destacar um líder."
            }

        var longestScreenOffMs =
            0L
        var longestAppMs =
            0L
        var longestAppName:
            String? =
            null

        for (
            index in
            0 until timeline.length()
        ) {
            val item =
                timeline.getJSONObject(index)

            val durationMs =
                item.optLong(
                    "duration_milliseconds",
                    item.optLong(
                        "duration_seconds",
                        0L
                    ) * 1000L
                )

            when (
                item.optString("type")
            ) {
                "SCREEN_OFF" -> {
                    if (
                        durationMs >
                        longestScreenOffMs
                    ) {
                        longestScreenOffMs =
                            durationMs
                    }
                }

                "APP" -> {
                    if (
                        durationMs >
                        longestAppMs
                    ) {
                        longestAppMs =
                            durationMs
                        longestAppName =
                            item.optString(
                                "name",
                                "App"
                            )
                    }
                }
            }
        }

        val appLine =
            if (
                longestAppMs > 0L &&
                !longestAppName.isNullOrBlank()
            ) {
                "• Maior intervalo contínuo de app registrado: $longestAppName por ${
                    durationMillisecondsLabel(
                        longestAppMs
                    )
                }."
            } else {
                "• Nenhum intervalo contínuo de app foi registrado."
            }

        val screenLine =
            if (
                longestScreenOffMs > 0L
            ) {
                "• Maior período contínuo com a tela desligada: ${
                    durationMillisecondsLabel(
                        longestScreenOffMs
                    )
                }."
            } else {
                "• Nenhum período de tela desligada foi registrado."
            }

        return LocalQuestionAnswer(
            intent =
                LocalQuestionIntent.INSIGHTS,
            period =
                plan.period,
            text =
                "Insights de ${periodLabelLower(plan)}:\n" +
                    "$topLine\n" +
                    "$appLine\n" +
                    "$screenLine\n" +
                    "• Cobertura do período: ${
                        coverageLabel(tracking)
                    }."
        )
    }

    private fun compareTodayYesterday(
        nowMs: Long
    ): LocalQuestionAnswer {
        val todayPlan =
            LocalQuestionPlan(
                intent =
                    LocalQuestionIntent.SUMMARY,
                period =
                    LocalQuestionPeriod.TODAY
            )

        val yesterdayPlan =
            LocalQuestionPlan(
                intent =
                    LocalQuestionIntent.SUMMARY,
                period =
                    LocalQuestionPeriod.YESTERDAY
            )

        val todayRange =
            rangeFor(
                todayPlan,
                nowMs
            )
        val yesterdayRange =
            rangeFor(
                yesterdayPlan,
                nowMs
            )

        val today =
            ReportGenerator(context).periodJson(
                todayRange.startMs,
                todayRange.endMs
            )
        val yesterday =
            ReportGenerator(context).periodJson(
                yesterdayRange.startMs,
                yesterdayRange.endMs
            )

        val todaySummary =
            today.getJSONObject("summary")
        val yesterdaySummary =
            yesterday.getJSONObject("summary")
        val todayTracking =
            today.getJSONObject("tracking")
        val yesterdayTracking =
            yesterday.getJSONObject("tracking")

        val todayMs =
            summaryMilliseconds(
                todaySummary,
                "app_usage"
            )
        val yesterdayMs =
            summaryMilliseconds(
                yesterdaySummary,
                "app_usage"
            )

        val differenceMs =
            todayMs -
                yesterdayMs

        val differenceText =
            when {
                differenceMs > 0L ->
                    "${
                        durationMillisecondsLabel(
                            differenceMs
                        )
                    } a mais hoje"

                differenceMs < 0L ->
                    "${
                        durationMillisecondsLabel(
                            -differenceMs
                        )
                    } a menos hoje"

                else ->
                    "o mesmo tempo registrado"
            }

        return LocalQuestionAnswer(
            intent =
                LocalQuestionIntent.COMPARE_TODAY_YESTERDAY,
            period =
                LocalQuestionPeriod.TODAY,
            text =
                "Hoje há ${
                    durationMillisecondsLabel(todayMs)
                } em apps; ontem, ${
                    durationMillisecondsLabel(yesterdayMs)
                }. Diferença: $differenceText. " +
                    "Período acompanhado: hoje ${
                        trackingDurationLabel(
                            todayTracking
                        )
                    } (${coverageLabel(todayTracking)}); ontem ${
                        trackingDurationLabel(
                            yesterdayTracking
                        )
                    } (${coverageLabel(yesterdayTracking)})."
        )
    }

    private fun summaryAnswer(
        plan: LocalQuestionPlan,
        summary: JSONObject,
        tracking: JSONObject,
        apps: JSONArray
    ): LocalQuestionAnswer {
        val top =
            if (apps.length() > 0) {
                val first =
                    apps.getJSONObject(0)

                val firstMs =
                    first.optLong(
                        "foreground_milliseconds",
                        first.getLong(
                            "foreground_seconds"
                        ) * 1000L
                    )

                "${
                    first.getString("name")
                } (${
                    durationMillisecondsLabel(firstMs)
                })"
            } else {
                "nenhum app com tempo suficiente"
            }

        return LocalQuestionAnswer(
            intent =
                LocalQuestionIntent.SUMMARY,
            period =
                plan.period,
            text =
                "${periodLabel(plan)}: ${
                    durationMillisecondsLabel(
                        summaryMilliseconds(
                            summary,
                            "app_usage"
                        )
                    )
                } em apps, ${
                    durationMillisecondsLabel(
                        summaryMilliseconds(
                            summary,
                            "screen_off"
                        )
                    )
                } de tela desligada, ${
                    durationMillisecondsLabel(
                        summaryMilliseconds(
                            summary,
                            "private"
                        )
                    )
                } em PRIVATE e ${
                    summary.getInt(
                        "unlock_count"
                    )
                } desbloqueios. Mais usado: $top. Cobertura ${
                    coverageLabel(tracking)
                }."
        )
    }

    private fun topAppAnswer(
        plan: LocalQuestionPlan,
        apps: JSONArray
    ): LocalQuestionAnswer {
        if (apps.length() == 0) {
            return LocalQuestionAnswer(
                LocalQuestionIntent.TOP_APP,
                plan.period,
                "${periodLabel(plan)}: ainda não há tempo suficiente de aplicativos registrado."
            )
        }

        val first =
            apps.getJSONObject(0)

        val firstMs =
            first.optLong(
                "foreground_milliseconds",
                first.getLong(
                    "foreground_seconds"
                ) * 1000L
            )

        return LocalQuestionAnswer(
            LocalQuestionIntent.TOP_APP,
            plan.period,
            "${periodLabel(plan)}: o app mais usado é ${
                first.getString("name")
            }, com ${
                durationMillisecondsLabel(firstMs)
            }."
        )
    }

    private fun topAppsAnswer(
        plan: LocalQuestionPlan,
        apps: JSONArray
    ): LocalQuestionAnswer {
        if (apps.length() == 0) {
            return LocalQuestionAnswer(
                LocalQuestionIntent.TOP_APPS,
                plan.period,
                "${periodLabel(plan)}: ainda não há aplicativos com tempo suficiente registrado."
            )
        }

        val count =
            minOf(
                5,
                apps.length()
            )
        val lines =
            mutableListOf<String>()

        for (
            index in
            0 until count
        ) {
            val item =
                apps.getJSONObject(index)

            val millis =
                item.optLong(
                    "foreground_milliseconds",
                    item.getLong(
                        "foreground_seconds"
                    ) * 1000L
                )

            lines +=
                "${index + 1}. ${
                    item.getString("name")
                } — ${
                    durationMillisecondsLabel(millis)
                }"
        }

        return LocalQuestionAnswer(
            LocalQuestionIntent.TOP_APPS,
            plan.period,
            "${periodLabel(plan)} — mais usados:\n${
                lines.joinToString("\n")
            }"
        )
    }

    private fun coverageAnswer(
        plan: LocalQuestionPlan,
        tracking: JSONObject
    ): String =
        "${periodLabel(plan)}: cobertura ${
            coverageLabel(tracking)
        }, com ${
            unclassifiedDurationLabel(tracking)
        } não classificados em ${
            trackingDurationLabel(tracking)
        } efetivamente acompanhados."

    private fun summaryMilliseconds(
        summary: JSONObject,
        keyPrefix: String
    ): Long =
        if (
            summary.has(
                "${keyPrefix}_milliseconds"
            )
        ) {
            summary.getLong(
                "${keyPrefix}_milliseconds"
            )
        } else {
            summary.getLong(
                "${keyPrefix}_seconds"
            ) * 1000L
        }

    private fun trackingDurationLabel(
        tracking: JSONObject
    ): String =
        if (
            tracking.has(
                "effective_period_milliseconds"
            )
        ) {
            durationMillisecondsLabel(
                tracking.getLong(
                    "effective_period_milliseconds"
                )
            )
        } else {
            durationLabel(
                tracking.getLong(
                    "effective_period_seconds"
                )
            )
        }

    private fun unclassifiedDurationLabel(
        tracking: JSONObject
    ): String =
        if (
            tracking.has(
                "unclassified_milliseconds"
            )
        ) {
            durationMillisecondsLabel(
                tracking.getLong(
                    "unclassified_milliseconds"
                )
            )
        } else {
            durationLabel(
                tracking.getLong(
                    "unclassified_seconds"
                )
            )
        }

    private fun coverageLabel(
        tracking: JSONObject
    ): String =
        String.format(
            Locale.forLanguageTag(
                "pt-BR"
            ),
            "%.1f%%",
            tracking.getDouble(
                "coverage_percent"
            )
        )

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

    private fun findNamedApp(
        normalizedQuestion: String,
        apps: JSONArray
    ): JSONObject? {
        val candidates =
            mutableListOf<
                Pair<String, JSONObject>
                >()

        for (
            index in
            0 until apps.length()
        ) {
            val item =
                apps.getJSONObject(index)
            val label =
                LocalQuestionIntentParser.normalize(
                    item.getString("name")
                )

            if (label.isNotBlank()) {
                candidates +=
                    label to item
            }
        }

        return candidates
            .sortedByDescending {
                it.first.length
            }
            .firstOrNull {
                normalizedQuestion.contains(
                    it.first
                )
            }
            ?.second
    }

    private fun periodLabel(
        plan: LocalQuestionPlan
    ): String =
        when (plan.period) {
            LocalQuestionPeriod.TODAY ->
                "Hoje"
            LocalQuestionPeriod.YESTERDAY ->
                "Ontem"
            LocalQuestionPeriod.LAST_24_HOURS ->
                "Nas últimas 24 horas"
            LocalQuestionPeriod.LAST_7_DAYS ->
                "Nos últimos 7 dias"
            LocalQuestionPeriod.ROLLING_HOURS ->
                "Nas últimas ${plan.amount} horas"
            LocalQuestionPeriod.ROLLING_DAYS ->
                "Nos últimos ${plan.amount} dias"
        }

    private fun periodLabelLower(
        plan: LocalQuestionPlan
    ): String =
        when (plan.period) {
            LocalQuestionPeriod.TODAY ->
                "hoje"
            LocalQuestionPeriod.YESTERDAY ->
                "ontem"
            LocalQuestionPeriod.LAST_24_HOURS ->
                "últimas 24 horas"
            LocalQuestionPeriod.LAST_7_DAYS ->
                "últimos 7 dias"
            LocalQuestionPeriod.ROLLING_HOURS ->
                "últimas ${plan.amount} horas"
            LocalQuestionPeriod.ROLLING_DAYS ->
                "últimos ${plan.amount} dias"
        }

    private fun helpText(): String =
        "Tente: “Resumo de hoje”, “Top 5 das últimas 6 horas”, “Insights dos últimos 3 dias”, “Quanto tempo usei o Chrome Dev nas últimas 2 horas?”, “Compare hoje com ontem” ou “Qual a cobertura das últimas 24 horas?”. Perguntas não são salvas."

    private fun startOfDay(
        nowMs: Long
    ): Long =
        Calendar.getInstance()
            .apply {
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
            }
            .timeInMillis

    private fun durationMillisecondsLabel(
        milliseconds: Long
    ): String =
        durationLabel(
            milliseconds /
                1000L
        )

    private fun durationLabel(
        seconds: Long
    ): String {
        if (seconds < 60L) {
            return "${seconds}s"
        }

        val minutes =
            seconds /
                60L
        val remainingSeconds =
            seconds %
                60L

        if (minutes < 60L) {
            return if (
                remainingSeconds ==
                0L
            ) {
                "${minutes}m"
            } else {
                "${minutes}m ${remainingSeconds}s"
            }
        }

        val hours =
            minutes /
                60L
        val remainingMinutes =
            minutes %
                60L

        return if (
            remainingMinutes ==
            0L
        ) {
            "${hours}h"
        } else {
            "${hours}h ${remainingMinutes}m"
        }
    }
}
