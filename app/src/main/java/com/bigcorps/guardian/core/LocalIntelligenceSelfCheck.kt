package com.bigcorps.guardian.core

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object LocalIntelligenceSelfCheck {
    private data class Expected(
        val id: String,
        val question: String,
        val intent: LocalQuestionIntent,
        val period: LocalQuestionPeriod
    )

    fun run(
        context: Context,
        nowMs: Long = System.currentTimeMillis()
    ): JSONObject {
        val expected = listOf(
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
                LocalQuestionPeriod.LAST_24_HOURS
            ),
            Expected(
                "insights_today",
                "Insights de hoje",
                LocalQuestionIntent.INSIGHTS,
                LocalQuestionPeriod.TODAY
            ),
            Expected(
                "compare_today_yesterday",
                "Compare hoje com ontem",
                LocalQuestionIntent.COMPARE_TODAY_YESTERDAY,
                LocalQuestionPeriod.TODAY
            )
        )

        val checks = JSONArray()
        var passed = true

        expected.forEach { item ->
            val plan = LocalQuestionIntentParser.plan(item.question)
            val answer = runCatching {
                LocalQuestionEngine(context).answer(item.question, nowMs)
            }.getOrNull()

            val itemPassed =
                plan.intent == item.intent &&
                    plan.period == item.period &&
                    !answer?.text.isNullOrBlank()

            if (!itemPassed) passed = false

            checks.put(
                JSONObject().apply {
                    put("check", item.id)
                    put("passed", itemPassed)
                }
            )
        }

        return JSONObject().apply {
            put("passed", passed)
            put("checks", checks)
            put("user_query_content_stored", false)
        }
    }
}
