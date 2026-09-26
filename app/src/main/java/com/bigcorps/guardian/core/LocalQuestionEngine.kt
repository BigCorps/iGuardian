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
    COMPARE_LAST_24H_PREVIOUS_24H,
    COMPARE_LAST_7D_PREVIOUS_7D,
    COMPARE_CALENDAR_PERIODS,
    HELP
}

enum class LocalQuestionPeriod {
    TODAY,
    YESTERDAY,
    LAST_24_HOURS,
    LAST_7_DAYS,
    ROLLING_HOURS,
    ROLLING_DAYS,
    CALENDAR_DAY,
    CALENDAR_RANGE
}

data class LocalCalendarDate(
    val day: Int,
    val month: Int,
    val year: Int? = null
)

data class LocalQuestionPlan(
    val intent: LocalQuestionIntent,
    val period: LocalQuestionPeriod,
    val amount: Int = 0,
    val calendarStart: LocalCalendarDate? = null,
    val calendarEnd: LocalCalendarDate? = null,
    val comparisonCalendarStart: LocalCalendarDate? = null,
    val comparisonCalendarEnd: LocalCalendarDate? = null
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

    private val calendarDateRegex =
        Regex(
            """(?<!\d)(\d{1,2})[/-](\d{1,2})(?:[/-](\d{2,4}))?(?!\d)"""
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

        val calendarDates =
            calendarDateRegex
                .findAll(raw)
                .mapNotNull {
                    match ->
                    parseCalendarDate(
                        match
                    )
                }
                .take(4)
                .toList()

        val compareWord =
            q.contains("compare") ||
                q.contains("comparar")

        if (
            compareWord &&
            calendarDates.size >= 2
        ) {
            return if (
                calendarDates.size >= 4
            ) {
                LocalQuestionPlan(
                    intent =
                        LocalQuestionIntent.COMPARE_CALENDAR_PERIODS,
                    period =
                        LocalQuestionPeriod.CALENDAR_RANGE,
                    calendarStart =
                        calendarDates[0],
                    calendarEnd =
                        calendarDates[1],
                    comparisonCalendarStart =
                        calendarDates[2],
                    comparisonCalendarEnd =
                        calendarDates[3]
                )
            } else {
                LocalQuestionPlan(
                    intent =
                        LocalQuestionIntent.COMPARE_CALENDAR_PERIODS,
                    period =
                        LocalQuestionPeriod.CALENDAR_DAY,
                    calendarStart =
                        calendarDates[0],
                    comparisonCalendarStart =
                        calendarDates[1]
                )
            }
        }

        val compareTodayYesterday =
            q.contains("compare hoje com ontem") ||
                q.contains("comparar hoje com ontem") ||
                q.contains("hoje ou ontem")

        if (compareTodayYesterday) {
            return LocalQuestionPlan(
                intent =
                    LocalQuestionIntent.COMPARE_TODAY_YESTERDAY,
                period =
                    LocalQuestionPeriod.TODAY
            )
        }

        val compareLast24 =
            (
                q.contains("24 horas") ||
                    q.contains("24h")
                ) &&
                (
                    q.contains("anteriores") ||
                        q.contains("anterior")
                    ) &&
                (
                    compareWord ||
                        q.contains("tendencia") ||
                        q.contains("mudou")
                    )

        if (compareLast24) {
            return LocalQuestionPlan(
                intent =
                    LocalQuestionIntent.COMPARE_LAST_24H_PREVIOUS_24H,
                period =
                    LocalQuestionPeriod.LAST_24_HOURS,
                amount =
                    24
            )
        }

        val compareLast7 =
            (
                q.contains("7 dias") ||
                    q.contains("sete dias")
                ) &&
                (
                    q.contains("anteriores") ||
                        q.contains("anterior")
                    ) &&
                (
                    compareWord ||
                        q.contains("tendencia") ||
                        q.contains("mudou")
                    )

        if (compareLast7) {
            return LocalQuestionPlan(
                intent =
                    LocalQuestionIntent.COMPARE_LAST_7D_PREVIOUS_7D,
                period =
                    LocalQuestionPeriod.LAST_7_DAYS,
                amount =
                    7
            )
        }

        val periodPlan =
            when {
                calendarDates.size >= 2 ->
                    LocalQuestionPlan(
                        intent =
                            LocalQuestionIntent.HELP,
                        period =
                            LocalQuestionPeriod.CALENDAR_RANGE,
                        calendarStart =
                            calendarDates[0],
                        calendarEnd =
                            calendarDates[1]
                    )

                calendarDates.size == 1 ->
                    LocalQuestionPlan(
                        intent =
                            LocalQuestionIntent.HELP,
                        period =
                            LocalQuestionPeriod.CALENDAR_DAY,
                        calendarStart =
                            calendarDates[0]
                    )

                else ->
                    parseRollingPeriod(
                        q
                    )
            }

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

                periodPlan.period ==
                    LocalQuestionPeriod.CALENDAR_DAY ||
                    periodPlan.period ==
                    LocalQuestionPeriod.CALENDAR_RANGE ->
                    LocalQuestionIntent.SUMMARY

                else ->
                    LocalQuestionIntent.HELP
            }

        return periodPlan.copy(
            intent =
                intent
        )
    }

    private fun parseCalendarDate(
        match: MatchResult
    ): LocalCalendarDate? {
        val day =
            match.groupValues
                .getOrNull(1)
                ?.toIntOrNull()
                ?: return null

        val month =
            match.groupValues
                .getOrNull(2)
                ?.toIntOrNull()
                ?: return null

        val rawYear =
            match.groupValues
                .getOrNull(3)
                .orEmpty()

        val year =
            when {
                rawYear.isBlank() ->
                    null

                rawYear.length == 2 ->
                    2000 +
                        (
                            rawYear.toIntOrNull()
                                ?: return null
                            )

                else ->
                    rawYear.toIntOrNull()
            }

        if (
            day !in 1..31 ||
            month !in 1..12
        ) {
            return null
        }

        return LocalCalendarDate(
            day =
                day,
            month =
                month,
            year =
                year
        )
    }

    private fun parseRollingPeriod(
        q: String
    ): LocalQuestionPlan {
        if (
            q.contains("ontem")
        ) {
            return LocalQuestionPlan(
                intent =
                    LocalQuestionIntent.HELP,
                period =
                    LocalQuestionPeriod.YESTERDAY
            )
        }

        val hours =
            rollingHoursRegex
                .find(q)
                ?.groupValues
                ?.getOrNull(1)
                ?.toIntOrNull()

        if (
            hours != null
        ) {
            if (
                hours ==
                24
            ) {
                return LocalQuestionPlan(
                    intent =
                        LocalQuestionIntent.HELP,
                    period =
                        LocalQuestionPeriod.LAST_24_HOURS,
                    amount =
                        24
                )
            }

            if (
                hours in
                1..720
            ) {
                return LocalQuestionPlan(
                    intent =
                        LocalQuestionIntent.HELP,
                    period =
                        LocalQuestionPeriod.ROLLING_HOURS,
                    amount =
                        hours
                )
            }
        }

        val days =
            rollingDaysRegex
                .find(q)
                ?.groupValues
                ?.getOrNull(1)
                ?.toIntOrNull()

        if (
            days != null
        ) {
            if (
                days ==
                7
            ) {
                return LocalQuestionPlan(
                    intent =
                        LocalQuestionIntent.HELP,
                    period =
                        LocalQuestionPeriod.LAST_7_DAYS,
                    amount =
                        7
                )
            }

            if (
                days in
                1..90
            ) {
                return LocalQuestionPlan(
                    intent =
                        LocalQuestionIntent.HELP,
                    period =
                        LocalQuestionPeriod.ROLLING_DAYS,
                    amount =
                        days
                )
            }
        }

        if (
            q.contains("24h") ||
            q.contains("24 horas")
        ) {
            return LocalQuestionPlan(
                intent =
                    LocalQuestionIntent.HELP,
                period =
                    LocalQuestionPeriod.LAST_24_HOURS,
                amount =
                    24
            )
        }

        if (
            q.contains("sete dias") ||
            q.contains("ultima semana") ||
            q.contains("essa semana")
        ) {
            return LocalQuestionPlan(
                intent =
                    LocalQuestionIntent.HELP,
                period =
                    LocalQuestionPeriod.LAST_7_DAYS,
                amount =
                    7
            )
        }

        return LocalQuestionPlan(
            intent =
                LocalQuestionIntent.HELP,
            period =
                LocalQuestionPeriod.TODAY
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
        nowMs: Long =
            System.currentTimeMillis()
    ): LocalQuestionAnswer {
        val plan =
            LocalQuestionIntentParser
                .plan(
                    question
                )

        if (
            plan.intent ==
            LocalQuestionIntent
                .COMPARE_TODAY_YESTERDAY
        ) {
            return compareTodayYesterday(
                nowMs
            )
        }

        if (
            plan.intent ==
            LocalQuestionIntent
                .COMPARE_LAST_24H_PREVIOUS_24H
        ) {
            return compareLast24Hours(
                nowMs
            )
        }

        if (
            plan.intent ==
            LocalQuestionIntent
                .COMPARE_LAST_7D_PREVIOUS_7D
        ) {
            return compareLast7Days(
                nowMs
            )
        }

        if (
            plan.intent ==
            LocalQuestionIntent
                .COMPARE_CALENDAR_PERIODS
        ) {
            return compareCalendarPeriods(
                plan,
                nowMs
            )
        }

        val range =
            runCatching {
                rangeFor(
                    plan,
                    nowMs
                )
            }
                .getOrElse {
                    return LocalQuestionAnswer(
                        intent =
                            plan.intent,
                        period =
                            plan.period,
                        text =
                            "Não consegui interpretar essa data. Use, por exemplo, 25/09 ou 25/09/2026."
                    )
                }

        if (
            range.endMs <=
            range.startMs
        ) {
            return LocalQuestionAnswer(
                intent =
                    plan.intent,
                period =
                    plan.period,
                text =
                    "O período solicitado ainda não possui tempo acompanhável."
            )
        }

        val report =
            ReportGenerator(
                context
            ).periodJson(
                range.startMs,
                range.endMs
            )

        val summary =
            report.getJSONObject(
                "summary"
            )

        val tracking =
            report.getJSONObject(
                "tracking"
            )

        val apps =
            report.getJSONArray(
                "apps"
            )

        val normalized =
            LocalQuestionIntentParser
                .normalize(
                    question
                )

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
                normalized.contains(
                    it
                )
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
                        matchingApp.getString(
                            "name"
                        )
                    } ficou registrado por ${
                        durationMillisecondsLabel(
                            matchingApp.optLong(
                                "foreground_milliseconds",
                                matchingApp.getLong(
                                    "foreground_seconds"
                                ) *
                                    1000L
                            )
                        )
                    } em primeiro plano."
            )
        }

        return when (
            plan.intent
        ) {
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
                        summary.getInt(
                            "unlock_count"
                        )
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

            LocalQuestionIntent
                .COMPARE_TODAY_YESTERDAY ->
                compareTodayYesterday(
                    nowMs
                )

            LocalQuestionIntent
                .COMPARE_LAST_24H_PREVIOUS_24H ->
                compareLast24Hours(
                    nowMs
                )

            LocalQuestionIntent
                .COMPARE_LAST_7D_PREVIOUS_7D ->
                compareLast7Days(
                    nowMs
                )

            LocalQuestionIntent
                .COMPARE_CALENDAR_PERIODS ->
                compareCalendarPeriods(
                    plan,
                    nowMs
                )
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
            startOfDay(
                nowMs
            )

        return when (
            plan.period
        ) {
            LocalQuestionPeriod.TODAY ->
                Range(
                    todayStart,
                    nowMs
                )

            LocalQuestionPeriod.YESTERDAY -> {
                val yesterdayStart =
                    Calendar
                        .getInstance()
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

            LocalQuestionPeriod.CALENDAR_DAY -> {
                val start =
                    calendarStartMs(
                        plan.calendarStart
                            ?: error(
                                "missing calendar day"
                            ),
                        nowMs
                    )

                val end =
                    minOf(
                        nextDayStart(
                            start
                        ),
                        nowMs
                    )

                Range(
                    start,
                    end
                )
            }

            LocalQuestionPeriod.CALENDAR_RANGE -> {
                var first =
                    calendarStartMs(
                        plan.calendarStart
                            ?: error(
                                "missing calendar range start"
                            ),
                        nowMs
                    )

                var second =
                    calendarStartMs(
                        plan.calendarEnd
                            ?: error(
                                "missing calendar range end"
                            ),
                        nowMs
                    )

                if (
                    second <
                    first
                ) {
                    val temp =
                        first
                    first =
                        second
                    second =
                        temp
                }

                Range(
                    first,
                    minOf(
                        nextDayStart(
                            second
                        ),
                        nowMs
                    )
                )
            }
        }
    }

    private fun calendarStartMs(
        value: LocalCalendarDate,
        nowMs: Long
    ): Long {
        val now =
            Calendar
                .getInstance()
                .apply {
                    timeInMillis =
                        nowMs
                }

        val resolvedYear =
            value.year
                ?: now.get(
                    Calendar.YEAR
                )

        val calendar =
            Calendar
                .getInstance()
                .apply {
                    isLenient =
                        false

                    clear()

                    set(
                        Calendar.YEAR,
                        resolvedYear
                    )
                    set(
                        Calendar.MONTH,
                        value.month -
                            1
                    )
                    set(
                        Calendar.DAY_OF_MONTH,
                        value.day
                    )
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

        return calendar
            .timeInMillis
    }

    private fun nextDayStart(
        startMs: Long
    ): Long =
        Calendar
            .getInstance()
            .apply {
                timeInMillis =
                    startMs

                add(
                    Calendar.DAY_OF_YEAR,
                    1
                )
            }
            .timeInMillis

    private fun compareLast24Hours(
        nowMs: Long
    ): LocalQuestionAnswer {
        val currentStart =
            nowMs -
                24L *
                60L *
                60L *
                1000L

        val previousStart =
            currentStart -
                24L *
                60L *
                60L *
                1000L

        val current =
            ReportGenerator(
                context
            ).periodJson(
                currentStart,
                nowMs
            )

        val previous =
            ReportGenerator(
                context
            ).periodJson(
                previousStart,
                currentStart
            )

        val currentSummary =
            current.getJSONObject(
                "summary"
            )

        val previousSummary =
            previous.getJSONObject(
                "summary"
            )

        val currentTracking =
            current.getJSONObject(
                "tracking"
            )

        val previousTracking =
            previous.getJSONObject(
                "tracking"
            )

        val currentApps =
            current.getJSONArray(
                "apps"
            )

        val previousApps =
            previous.getJSONArray(
                "apps"
            )

        val currentMs =
            summaryMilliseconds(
                currentSummary,
                "app_usage"
            )

        val previousMs =
            summaryMilliseconds(
                previousSummary,
                "app_usage"
            )

        val difference =
            currentMs -
                previousMs

        val differenceText =
            when {
                difference >
                    0L ->
                    "${
                        durationMillisecondsLabel(
                            difference
                        )
                    } a mais nas últimas 24h"

                difference <
                    0L ->
                    "${
                        durationMillisecondsLabel(
                            -difference
                        )
                    } a menos nas últimas 24h"

                else ->
                    "o mesmo tempo registrado"
            }

        val currentTop =
            topAppCompact(
                currentApps
            )

        val previousTop =
            topAppCompact(
                previousApps
            )

        return LocalQuestionAnswer(
            intent =
                LocalQuestionIntent
                    .COMPARE_LAST_24H_PREVIOUS_24H,
            period =
                LocalQuestionPeriod.LAST_24_HOURS,
            text =
                "Últimas 24h: ${
                    durationMillisecondsLabel(
                        currentMs
                    )
                } em apps; 24h anteriores: ${
                    durationMillisecondsLabel(
                        previousMs
                    )
                }. Diferença: $differenceText. " +
                    "Mais usado agora: $currentTop. Antes: $previousTop. " +
                    "Cobertura: ${
                        coverageLabel(
                            currentTracking
                        )
                    } agora e ${
                        coverageLabel(
                            previousTracking
                        )
                    } no período anterior."
        )
    }

    private fun compareLast7Days(
        nowMs: Long
    ): LocalQuestionAnswer {
        val currentStart =
            nowMs -
                7L *
                24L *
                60L *
                60L *
                1000L

        val previousStart =
            currentStart -
                7L *
                24L *
                60L *
                60L *
                1000L

        val current =
            ReportGenerator(
                context
            ).periodJson(
                currentStart,
                nowMs
            )

        val previous =
            ReportGenerator(
                context
            ).periodJson(
                previousStart,
                currentStart
            )

        return comparisonAnswer(
            intent =
                LocalQuestionIntent.COMPARE_LAST_7D_PREVIOUS_7D,
            period =
                LocalQuestionPeriod.LAST_7_DAYS,
            currentLabel =
                "Últimos 7 dias",
            previousLabel =
                "7 dias anteriores",
            current =
                current,
            previous =
                previous
        )
    }

    private fun compareCalendarPeriods(
        plan: LocalQuestionPlan,
        nowMs: Long
    ): LocalQuestionAnswer {
        val firstStart =
            plan.calendarStart
                ?: return LocalQuestionAnswer(
                    plan.intent,
                    plan.period,
                    "Não consegui interpretar o primeiro período."
                )

        val secondStart =
            plan.comparisonCalendarStart
                ?: return LocalQuestionAnswer(
                    plan.intent,
                    plan.period,
                    "Não consegui interpretar o segundo período."
                )

        val firstRange =
            calendarRange(
                firstStart,
                plan.calendarEnd,
                nowMs
            )

        val secondRange =
            calendarRange(
                secondStart,
                plan.comparisonCalendarEnd,
                nowMs
            )

        if (
            firstRange.endMs <=
            firstRange.startMs ||
            secondRange.endMs <=
            secondRange.startMs
        ) {
            return LocalQuestionAnswer(
                plan.intent,
                plan.period,
                "Um dos períodos solicitados ainda não possui tempo acompanhável."
            )
        }

        val first =
            ReportGenerator(
                context
            ).periodJson(
                firstRange.startMs,
                firstRange.endMs
            )

        val second =
            ReportGenerator(
                context
            ).periodJson(
                secondRange.startMs,
                secondRange.endMs
            )

        return comparisonAnswer(
            intent =
                LocalQuestionIntent.COMPARE_CALENDAR_PERIODS,
            period =
                plan.period,
            currentLabel =
                calendarPeriodLabel(
                    secondStart,
                    plan.comparisonCalendarEnd
                ),
            previousLabel =
                calendarPeriodLabel(
                    firstStart,
                    plan.calendarEnd
                ),
            current =
                second,
            previous =
                first
        )
    }

    private fun calendarRange(
        startDate: LocalCalendarDate,
        endDate: LocalCalendarDate?,
        nowMs: Long
    ): Range {
        var start =
            calendarStartMs(
                startDate,
                nowMs
            )

        var endStart =
            calendarStartMs(
                endDate
                    ?: startDate,
                nowMs
            )

        if (
            endStart <
            start
        ) {
            val temporary =
                start
            start =
                endStart
            endStart =
                temporary
        }

        return Range(
            start,
            minOf(
                nextDayStart(
                    endStart
                ),
                nowMs
            )
        )
    }

    private fun comparisonAnswer(
        intent: LocalQuestionIntent,
        period: LocalQuestionPeriod,
        currentLabel: String,
        previousLabel: String,
        current: JSONObject,
        previous: JSONObject
    ): LocalQuestionAnswer {
        val currentSummary =
            current.getJSONObject(
                "summary"
            )

        val previousSummary =
            previous.getJSONObject(
                "summary"
            )

        val currentTracking =
            current.getJSONObject(
                "tracking"
            )

        val previousTracking =
            previous.getJSONObject(
                "tracking"
            )

        val currentApps =
            current.getJSONArray(
                "apps"
            )

        val previousApps =
            previous.getJSONArray(
                "apps"
            )

        val currentMs =
            summaryMilliseconds(
                currentSummary,
                "app_usage"
            )

        val previousMs =
            summaryMilliseconds(
                previousSummary,
                "app_usage"
            )

        val difference =
            currentMs -
                previousMs

        val differenceText =
            when {
                difference > 0L ->
                    "${
                        durationMillisecondsLabel(
                            difference
                        )
                    } a mais"

                difference < 0L ->
                    "${
                        durationMillisecondsLabel(
                            -difference
                        )
                    } a menos"

                else ->
                    "o mesmo tempo registrado"
            }

        return LocalQuestionAnswer(
            intent =
                intent,
            period =
                period,
            text =
                "$currentLabel: ${
                    durationMillisecondsLabel(
                        currentMs
                    )
                } em apps; $previousLabel: ${
                    durationMillisecondsLabel(
                        previousMs
                    )
                }. Diferença: $differenceText. " +
                    "Mais usado: ${
                        topAppCompact(
                            currentApps
                        )
                    } agora e ${
                        topAppCompact(
                            previousApps
                        )
                    } antes. " +
                    "Desbloqueios: ${
                        currentSummary.optInt(
                            "unlock_count",
                            0
                        )
                    } vs ${
                        previousSummary.optInt(
                            "unlock_count",
                            0
                        )
                    }. Cobertura: ${
                        coverageLabel(
                            currentTracking
                        )
                    } vs ${
                        coverageLabel(
                            previousTracking
                        )
                    }."
        )
    }

    private fun calendarPeriodLabel(
        start: LocalCalendarDate,
        end: LocalCalendarDate?
    ): String =
        if (
            end ==
            null
        ) {
            calendarDateLabel(
                start
            )
        } else {
            "${
                calendarDateLabel(
                    start
                )
            } a ${
                calendarDateLabel(
                    end
                )
            }"
        }

    private fun insightsAnswer(
        plan: LocalQuestionPlan,
        report: JSONObject
    ): LocalQuestionAnswer {
        val summary =
            report.getJSONObject(
                "summary"
            )

        val tracking =
            report.getJSONObject(
                "tracking"
            )

        val apps =
            report.getJSONArray(
                "apps"
            )

        val timeline =
            report.getJSONArray(
                "timeline"
            )

        val appUsageMs =
            summaryMilliseconds(
                summary,
                "app_usage"
            )

        val topLine =
            if (
                apps.length() >
                0 &&
                appUsageMs >
                0L
            ) {
                val top =
                    apps.getJSONObject(
                        0
                    )

                val topMs =
                    top.optLong(
                        "foreground_milliseconds",
                        top.getLong(
                            "foreground_seconds"
                        ) *
                            1000L
                    )

                val share =
                    topMs.toDouble() *
                        100.0 /
                        appUsageMs.toDouble()

                "• ${
                    top.getString(
                        "name"
                    )
                } liderou o tempo em apps: ${
                    durationMillisecondsLabel(
                        topMs
                    )
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
                timeline.getJSONObject(
                    index
                )

            val durationMs =
                item.optLong(
                    "duration_milliseconds",
                    item.optLong(
                        "duration_seconds",
                        0L
                    ) *
                        1000L
                )

            when (
                item.optString(
                    "type"
                )
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
                longestAppMs >
                0L &&
                !longestAppName
                    .isNullOrBlank()
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
                longestScreenOffMs >
                0L
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
                        coverageLabel(
                            tracking
                        )
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
            ReportGenerator(
                context
            ).periodJson(
                todayRange.startMs,
                todayRange.endMs
            )

        val yesterday =
            ReportGenerator(
                context
            ).periodJson(
                yesterdayRange.startMs,
                yesterdayRange.endMs
            )

        val todaySummary =
            today.getJSONObject(
                "summary"
            )

        val yesterdaySummary =
            yesterday.getJSONObject(
                "summary"
            )

        val todayTracking =
            today.getJSONObject(
                "tracking"
            )

        val yesterdayTracking =
            yesterday.getJSONObject(
                "tracking"
            )

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

        val difference =
            todayMs -
                yesterdayMs

        val differenceText =
            when {
                difference >
                    0L ->
                    "${
                        durationMillisecondsLabel(
                            difference
                        )
                    } a mais hoje"

                difference <
                    0L ->
                    "${
                        durationMillisecondsLabel(
                            -difference
                        )
                    } a menos hoje"

                else ->
                    "o mesmo tempo registrado"
            }

        return LocalQuestionAnswer(
            intent =
                LocalQuestionIntent
                    .COMPARE_TODAY_YESTERDAY,
            period =
                LocalQuestionPeriod.TODAY,
            text =
                "Hoje há ${
                    durationMillisecondsLabel(
                        todayMs
                    )
                } em apps; ontem, ${
                    durationMillisecondsLabel(
                        yesterdayMs
                    )
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
            topAppCompact(
                apps
            )

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
                    coverageLabel(
                        tracking
                    )
                }."
        )
    }

    private fun topAppAnswer(
        plan: LocalQuestionPlan,
        apps: JSONArray
    ): LocalQuestionAnswer {
        if (
            apps.length() ==
            0
        ) {
            return LocalQuestionAnswer(
                LocalQuestionIntent.TOP_APP,
                plan.period,
                "${periodLabel(plan)}: ainda não há tempo suficiente de aplicativos registrado."
            )
        }

        val first =
            apps.getJSONObject(
                0
            )

        val firstMs =
            first.optLong(
                "foreground_milliseconds",
                first.getLong(
                    "foreground_seconds"
                ) *
                    1000L
            )

        return LocalQuestionAnswer(
            LocalQuestionIntent.TOP_APP,
            plan.period,
            "${periodLabel(plan)}: o app mais usado é ${
                first.getString(
                    "name"
                )
            }, com ${
                durationMillisecondsLabel(
                    firstMs
                )
            }."
        )
    }

    private fun topAppsAnswer(
        plan: LocalQuestionPlan,
        apps: JSONArray
    ): LocalQuestionAnswer {
        if (
            apps.length() ==
            0
        ) {
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
            mutableListOf<
                String
                >()

        for (
            index in
            0 until count
        ) {
            val item =
                apps.getJSONObject(
                    index
                )

            val millis =
                item.optLong(
                    "foreground_milliseconds",
                    item.getLong(
                        "foreground_seconds"
                    ) *
                        1000L
                )

            lines +=
                "${index + 1}. ${
                    item.getString(
                        "name"
                    )
                } — ${
                    durationMillisecondsLabel(
                        millis
                    )
                }"
        }

        return LocalQuestionAnswer(
            LocalQuestionIntent.TOP_APPS,
            plan.period,
            "${periodLabel(plan)} — mais usados:\n${
                lines.joinToString(
                    "\n"
                )
            }"
        )
    }

    private fun topAppCompact(
        apps: JSONArray
    ): String {
        if (
            apps.length() ==
            0
        ) {
            return "nenhum app com tempo suficiente"
        }

        val first =
            apps.getJSONObject(
                0
            )

        val millis =
            first.optLong(
                "foreground_milliseconds",
                first.getLong(
                    "foreground_seconds"
                ) *
                    1000L
            )

        return "${
            first.getString(
                "name"
            )
        } (${
            durationMillisecondsLabel(
                millis
            )
        })"
    }

    private fun coverageAnswer(
        plan: LocalQuestionPlan,
        tracking: JSONObject
    ): String =
        "${periodLabel(plan)}: cobertura ${
            coverageLabel(
                tracking
            )
        }, com ${
            unclassifiedDurationLabel(
                tracking
            )
        } não classificados em ${
            trackingDurationLabel(
                tracking
            )
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
            ) *
                1000L
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
                apps.getJSONObject(
                    index
                )

            val label =
                LocalQuestionIntentParser
                    .normalize(
                        item.getString(
                            "name"
                        )
                    )

            if (
                label.isNotBlank()
            ) {
                candidates +=
                    label to
                    item
            }
        }

        return candidates
            .sortedByDescending {
                it.first.length
            }
            .firstOrNull {
                normalizedQuestion
                    .contains(
                        it.first
                    )
            }
            ?.second
    }

    private fun periodLabel(
        plan: LocalQuestionPlan
    ): String =
        when (
            plan.period
        ) {
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

            LocalQuestionPeriod.CALENDAR_DAY ->
                "Em ${
                    calendarDateLabel(
                        plan.calendarStart
                    )
                }"

            LocalQuestionPeriod.CALENDAR_RANGE ->
                "De ${
                    calendarDateLabel(
                        plan.calendarStart
                    )
                } a ${
                    calendarDateLabel(
                        plan.calendarEnd
                    )
                }"
        }

    private fun periodLabelLower(
        plan: LocalQuestionPlan
    ): String =
        when (
            plan.period
        ) {
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

            LocalQuestionPeriod.CALENDAR_DAY ->
                calendarDateLabel(
                    plan.calendarStart
                )

            LocalQuestionPeriod.CALENDAR_RANGE ->
                "${
                    calendarDateLabel(
                        plan.calendarStart
                    )
                } a ${
                    calendarDateLabel(
                        plan.calendarEnd
                    )
                }"
        }

    private fun calendarDateLabel(
        value: LocalCalendarDate?
    ): String {
        if (
            value ==
            null
        ) {
            return "data"
        }

        val day =
            value.day
                .toString()
                .padStart(
                    2,
                    '0'
                )

        val month =
            value.month
                .toString()
                .padStart(
                    2,
                    '0'
                )

        return if (
            value.year !=
            null
        ) {
            "$day/$month/${value.year}"
        } else {
            "$day/$month"
        }
    }

    private fun helpText():
        String =
        "Tente: “Resumo de 25/09”, “Insights de 24/09 a 26/09”, “Compare 24/09 com 25/09”, “Compare 22/09 a 23/09 com 24/09 a 25/09”, “Compare as últimas 24 horas com as 24 anteriores” ou “Compare os últimos 7 dias com os 7 anteriores”. Perguntas não são salvas."

    private fun startOfDay(
        nowMs: Long
    ): Long =
        Calendar
            .getInstance()
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
        if (
            seconds <
            60L
        ) {
            return "${seconds}s"
        }

        val minutes =
            seconds /
                60L

        val remainingSeconds =
            seconds %
                60L

        if (
            minutes <
            60L
        ) {
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
