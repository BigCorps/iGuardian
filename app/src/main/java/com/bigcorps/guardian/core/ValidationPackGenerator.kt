package com.bigcorps.guardian.core

import android.content.Context
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ValidationPackGenerator {
    const val PACK_SCHEMA = 2

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
            put("daily_report", daily)
            put("diagnostic", diagnostic)
        }
    }
}
