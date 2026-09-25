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
    COMPARE_TODAY_YESTERDAY,
    HELP
}

enum class LocalQuestionPeriod {
    TODAY,
    YESTERDAY,
    LAST_7_DAYS
}

data class LocalQuestionPlan(
    val intent: LocalQuestionIntent,
    val period: LocalQuestionPeriod
)

object LocalQuestionIntentParser {
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

    fun period(raw: String): LocalQuestionPeriod {
        val q = normalize(raw)

        return when {
            q.contains("ontem") ->
                LocalQuestionPeriod.YESTERDAY

            q.contains("ultimos 7") ||
                q.contains("ultimas 7") ||
                q.contains("7 dias") ||
                q.contains("sete dias") ||
                q.contains("ultima semana") ||
                q.contains("essa semana") ->
                LocalQuestionPeriod.LAST_7_DAYS

            else ->
                LocalQuestionPeriod.TODAY
        }
    }

    fun parse(raw: String): LocalQuestionIntent =
        plan(raw).intent

    fun plan(raw: String): LocalQuestionPlan {
        val q = normalize(raw)
        val selectedPeriod = period(raw)

        val intent =
            when {
                q.isBlank() ->
                    LocalQuestionIntent.HELP

                q.contains("compare") ||
                    q.contains("comparar") ||
                    q.contains("hoje ou ontem") ||
                    q.contains("hoje com ontem") ->
                    LocalQuestionIntent.COMPARE_TODAY_YESTERDAY

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
            period = selectedPeriod
        )
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
            rangeFor(plan.period, nowMs)

        val report =
            ReportGenerator(context).periodJson(
                range.startMs,
                range.endMs
            )

        val summary = report.getJSONObject("summary")
        val tracking = report.getJSONObject("tracking")
        val apps = report.getJSONArray("apps")
        val normalized =
            LocalQuestionIntentParser.normalize(question)

        val matchingApp =
            findNamedApp(normalized, apps)

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
                intent = LocalQuestionIntent.APP_USAGE,
                period = plan.period,
                text =
                    "${periodLabel(plan.period)}: ${
                        matchingApp.getString("name")
                    } ficou registrado por ${
                        durationLabel(
                            matchingApp.getLong(
                                "foreground_seconds"
                            )
                        )
                    } em primeiro plano."
            )
        }

        return when (plan.intent) {
            LocalQuestionIntent.SUMMARY ->
                summaryAnswer(
                    plan.period,
                    summary,
                    tracking,
                    apps
                )

            LocalQuestionIntent.TOP_APP ->
                topAppAnswer(
                    plan.period,
                    apps
                )

            LocalQuestionIntent.TOP_APPS ->
                topAppsAnswer(
                    plan.period,
                    apps
                )

            LocalQuestionIntent.APP_USAGE ->
                LocalQuestionAnswer(
                    plan.intent,
                    plan.period,
                    "${periodLabel(plan.period)}: ${
                        durationLabel(
                            summary.getLong(
                                "app_usage_seconds"
                            )
                        )
                    } de uso registrado em aplicativos."
                )

            LocalQuestionIntent.SCREEN_OFF ->
                LocalQuestionAnswer(
                    plan.intent,
                    plan.period,
                    "${periodLabel(plan.period)}: a tela ficou desligada por ${
                        durationLabel(
                            summary.getLong(
                                "screen_off_seconds"
                            )
                        )
                    } no período acompanhado."
                )

            LocalQuestionIntent.UNLOCKS ->
                LocalQuestionAnswer(
                    plan.intent,
                    plan.period,
                    "${periodLabel(plan.period)}: foram detectados ${
                        summary.getInt("unlock_count")
                    } desbloqueios."
                )

            LocalQuestionIntent.PRIVATE ->
                LocalQuestionAnswer(
                    plan.intent,
                    plan.period,
                    "${periodLabel(plan.period)}: houve ${
                        durationLabel(
                            summary.getLong(
                                "private_seconds"
                            )
                        )
                    } classificados como PRIVATE. O Guardian não guarda qual app ou motivo gerou esses períodos."
                )

            LocalQuestionIntent.SYSTEM ->
                LocalQuestionAnswer(
                    plan.intent,
                    plan.period,
                    "${periodLabel(plan.period)}: a navegação técnica do sistema somou ${
                        durationLabel(
                            summary.getLong(
                                "system_seconds"
                            )
                        )
                    }. Ela não entra no ranking de aplicativos."
                )

            LocalQuestionIntent.COVERAGE ->
                LocalQuestionAnswer(
                    plan.intent,
                    plan.period,
                    coverageAnswer(
                        plan.period,
                        tracking
                    )
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
        period: LocalQuestionPeriod,
        nowMs: Long
    ): Range {
        val todayStart =
            startOfDay(nowMs)

        return when (period) {
            LocalQuestionPeriod.TODAY ->
                Range(
                    todayStart,
                    nowMs
                )

            LocalQuestionPeriod.YESTERDAY -> {
                val yesterdayStart =
                    Calendar.getInstance()
                        .apply {
                            timeInMillis = todayStart
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

            LocalQuestionPeriod.LAST_7_DAYS -> {
                val start =
                    Calendar.getInstance()
                        .apply {
                            timeInMillis = todayStart
                            add(
                                Calendar.DAY_OF_YEAR,
                                -6
                            )
                        }
                        .timeInMillis

                Range(
                    start,
                    nowMs
                )
            }
        }
    }

    private fun compareTodayYesterday(
        nowMs: Long
    ): LocalQuestionAnswer {
        val todayRange =
            rangeFor(
                LocalQuestionPeriod.TODAY,
                nowMs
            )

        val yesterdayRange =
            rangeFor(
                LocalQuestionPeriod.YESTERDAY,
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

        val todaySeconds =
            todaySummary.getLong(
                "app_usage_seconds"
            )
        val yesterdaySeconds =
            yesterdaySummary.getLong(
                "app_usage_seconds"
            )

        val difference =
            todaySeconds - yesterdaySeconds

        val differenceText =
            when {
                difference > 0 ->
                    "${durationLabel(difference)} a mais hoje"
                difference < 0 ->
                    "${durationLabel(-difference)} a menos hoje"
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
                    durationLabel(todaySeconds)
                } em apps; ontem, ${
                    durationLabel(yesterdaySeconds)
                }. Diferença: $differenceText. " +
                    "Período acompanhado: hoje ${
                        durationLabel(
                            todayTracking.getLong(
                                "effective_period_seconds"
                            )
                        )
                    } (${coverageLabel(todayTracking)}); ontem ${
                        durationLabel(
                            yesterdayTracking.getLong(
                                "effective_period_seconds"
                            )
                        )
                    } (${coverageLabel(yesterdayTracking)})."
        )
    }

    private fun summaryAnswer(
        period: LocalQuestionPeriod,
        summary: JSONObject,
        tracking: JSONObject,
        apps: JSONArray
    ): LocalQuestionAnswer {
        val top =
            if (apps.length() > 0) {
                val first = apps.getJSONObject(0)
                "${
                    first.getString("name")
                } (${
                    durationLabel(
                        first.getLong(
                            "foreground_seconds"
                        )
                    )
                })"
            } else {
                "nenhum app com tempo suficiente"
            }

        return LocalQuestionAnswer(
            intent = LocalQuestionIntent.SUMMARY,
            period = period,
            text =
                "${periodLabel(period)}: ${
                    durationLabel(
                        summary.getLong(
                            "app_usage_seconds"
                        )
                    )
                } em apps, ${
                    durationLabel(
                        summary.getLong(
                            "screen_off_seconds"
                        )
                    )
                } de tela desligada, ${
                    durationLabel(
                        summary.getLong(
                            "private_seconds"
                        )
                    )
                } em PRIVATE e ${
                    summary.getInt("unlock_count")
                } desbloqueios. Mais usado: $top. Cobertura ${
                    coverageLabel(tracking)
                }."
        )
    }

    private fun topAppAnswer(
        period: LocalQuestionPeriod,
        apps: JSONArray
    ): LocalQuestionAnswer {
        if (apps.length() == 0) {
            return LocalQuestionAnswer(
                LocalQuestionIntent.TOP_APP,
                period,
                "${periodLabel(period)}: ainda não há tempo suficiente de aplicativos registrado."
            )
        }

        val first = apps.getJSONObject(0)

        return LocalQuestionAnswer(
            LocalQuestionIntent.TOP_APP,
            period,
            "${periodLabel(period)}: o app mais usado é ${
                first.getString("name")
            }, com ${
                durationLabel(
                    first.getLong(
                        "foreground_seconds"
                    )
                )
            }."
        )
    }

    private fun topAppsAnswer(
        period: LocalQuestionPeriod,
        apps: JSONArray
    ): LocalQuestionAnswer {
        if (apps.length() == 0) {
            return LocalQuestionAnswer(
                LocalQuestionIntent.TOP_APPS,
                period,
                "${periodLabel(period)}: ainda não há aplicativos com tempo suficiente registrado."
            )
        }

        val count =
            minOf(5, apps.length())
        val lines =
            mutableListOf<String>()

        for (index in 0 until count) {
            val item =
                apps.getJSONObject(index)

            lines +=
                "${index + 1}. ${
                    item.getString("name")
                } — ${
                    durationLabel(
                        item.getLong(
                            "foreground_seconds"
                        )
                    )
                }"
        }

        return LocalQuestionAnswer(
            LocalQuestionIntent.TOP_APPS,
            period,
            "${periodLabel(period)} — mais usados:\n${
                lines.joinToString("\n")
            }"
        )
    }

    private fun coverageAnswer(
        period: LocalQuestionPeriod,
        tracking: JSONObject
    ): String =
        "${periodLabel(period)}: cobertura ${
            coverageLabel(tracking)
        }, com ${
            tracking.getLong(
                "unclassified_seconds"
            )
        } segundos não classificados em ${
            durationLabel(
                tracking.getLong(
                    "effective_period_seconds"
                )
            )
        } efetivamente acompanhados."

    private fun coverageLabel(
        tracking: JSONObject
    ): String =
        String.format(
            Locale.forLanguageTag("pt-BR"),
            "%.1f%%",
            tracking.getDouble(
                "coverage_percent"
            )
        )

    private fun findNamedApp(
        normalizedQuestion: String,
        apps: JSONArray
    ): JSONObject? {
        val candidates =
            mutableListOf<
                Pair<String, JSONObject>
                >()

        for (index in 0 until apps.length()) {
            val item =
                apps.getJSONObject(index)

            val label =
                LocalQuestionIntentParser.normalize(
                    item.getString("name")
                )

            if (label.isNotBlank()) {
                candidates += label to item
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
        period: LocalQuestionPeriod
    ): String =
        when (period) {
            LocalQuestionPeriod.TODAY ->
                "Hoje"
            LocalQuestionPeriod.YESTERDAY ->
                "Ontem"
            LocalQuestionPeriod.LAST_7_DAYS ->
                "Nos últimos 7 dias"
        }

    private fun helpText(): String =
        "Tente: “Resumo de hoje”, “Resumo de ontem”, “Top 5 dos últimos 7 dias”, “Quanto tempo usei o Chrome Dev ontem?”, “Quantas vezes desbloqueei hoje?”, “Qual a cobertura de ontem?” ou “Compare hoje com ontem”. Perguntas não são salvas."

    private fun startOfDay(
        nowMs: Long
    ): Long =
        Calendar.getInstance()
            .apply {
                timeInMillis = nowMs
                set(
                    Calendar.HOUR_OF_DAY,
                    0
                )
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            .timeInMillis

    private fun durationLabel(
        seconds: Long
    ): String {
        if (seconds < 60L) {
            return "${seconds}s"
        }

        val minutes = seconds / 60L
        val remainingSeconds = seconds % 60L

        if (minutes < 60L) {
            return if (remainingSeconds == 0L) {
                "${minutes}m"
            } else {
                "${minutes}m ${remainingSeconds}s"
            }
        }

        val hours = minutes / 60L
        val remainingMinutes = minutes % 60L

        return if (remainingMinutes == 0L) {
            "${hours}h"
        } else {
            "${hours}h ${remainingMinutes}m"
        }
    }
}
