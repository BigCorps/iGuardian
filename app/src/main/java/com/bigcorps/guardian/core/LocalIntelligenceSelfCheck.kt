package com.bigcorps.guardian.core

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object LocalIntelligenceSelfCheck {
    private data class Expected(
        val id: String,
        val question: String,
        val intent: LocalQuestionIntent,
        val period: LocalQuestionPeriod,
        val amount: Int = 0
    )

    fun run(
        context: Context,
        nowMs: Long =
            System.currentTimeMillis()
    ): JSONObject {
        val format =
            SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.US
            )

        val yesterdayMs =
            Calendar
                .getInstance()
                .apply {
                    timeInMillis =
                        nowMs

                    add(
                        Calendar.DAY_OF_YEAR,
                        -1
                    )
                }
                .timeInMillis

        val yesterday =
            format.format(
                Date(
                    yesterdayMs
                )
            )

        val today =
            format.format(
                Date(
                    nowMs
                )
            )

        val expected =
            listOf(
                Expected(
                    "summary_today",
                    "Resumo de hoje",
                    LocalQuestionIntent.SUMMARY,
                    LocalQuestionPeriod.TODAY
                ),
                Expected(
                    "top_last_24h",
                    "Top 5 das últimas 24 horas",
                    LocalQuestionIntent.TOP_APPS,
                    LocalQuestionPeriod.LAST_24_HOURS,
                    24
                ),
                Expected(
                    "top_last_6h",
                    "Top 5 das últimas 6 horas",
                    LocalQuestionIntent.TOP_APPS,
                    LocalQuestionPeriod.ROLLING_HOURS,
                    6
                ),
                Expected(
                    "insights_last_3d",
                    "Insights dos últimos 3 dias",
                    LocalQuestionIntent.INSIGHTS,
                    LocalQuestionPeriod.ROLLING_DAYS,
                    3
                ),
                Expected(
                    "calendar_day",
                    "Resumo de $yesterday",
                    LocalQuestionIntent.SUMMARY,
                    LocalQuestionPeriod.CALENDAR_DAY
                ),
                Expected(
                    "calendar_range",
                    "Insights de $yesterday a $today",
                    LocalQuestionIntent.INSIGHTS,
                    LocalQuestionPeriod.CALENDAR_RANGE
                ),
                Expected(
                    "compare_today_yesterday",
                    "Compare hoje com ontem",
                    LocalQuestionIntent.COMPARE_TODAY_YESTERDAY,
                    LocalQuestionPeriod.TODAY
                ),
                Expected(
                    "compare_last_24h",
                    "Compare as últimas 24 horas com as 24 anteriores",
                    LocalQuestionIntent.COMPARE_LAST_24H_PREVIOUS_24H,
                    LocalQuestionPeriod.LAST_24_HOURS,
                    24
                ),
                Expected(
                    "compare_last_7d",
                    "Compare os últimos 7 dias com os 7 anteriores",
                    LocalQuestionIntent.COMPARE_LAST_7D_PREVIOUS_7D,
                    LocalQuestionPeriod.LAST_7_DAYS,
                    7
                ),
                Expected(
                    "compare_calendar_day",
                    "Compare $yesterday com $today",
                    LocalQuestionIntent.COMPARE_CALENDAR_PERIODS,
                    LocalQuestionPeriod.CALENDAR_DAY
                )
            )

        val checks =
            JSONArray()

        var passed =
            true

        expected.forEach {
            item ->
            val plan =
                LocalQuestionIntentParser
                    .plan(
                        item.question
                    )

            val answer =
                runCatching {
                    LocalQuestionEngine(
                        context
                    ).answer(
                        item.question,
                        nowMs
                    )
                }
                    .getOrNull()

            val amountPassed =
                item.amount ==
                    0 ||
                    plan.amount ==
                    item.amount

            val itemPassed =
                plan.intent ==
                    item.intent &&
                    plan.period ==
                    item.period &&
                    amountPassed &&
                    !answer
                        ?.text
                        .isNullOrBlank()

            if (
                !itemPassed
            ) {
                passed =
                    false
            }

            checks.put(
                JSONObject().apply {
                    put(
                        "check",
                        item.id
                    )

                    put(
                        "passed",
                        itemPassed
                    )
                }
            )
        }

        return JSONObject().apply {
            put(
                "passed",
                passed
            )

            put(
                "checks",
                checks
            )

            put(
                "user_query_content_stored",
                false
            )
        }
    }
}
