package com.bigcorps.guardian.core

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.OffsetDateTime
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

object ValidationSuite {
    private const val PASS = "PASS"
    private const val WARN = "WARN"
    private const val FAIL = "FAIL"
    private const val SUITE_VERSION = 5

    fun run(
        context: Context,
        daily: JSONObject,
        diagnostic: JSONObject
    ): JSONObject {
        val checks = JSONArray()

        fun add(id: String, status: String, detail: String) {
            checks.put(
                JSONObject().apply {
                    put("id", id)
                    put("status", status)
                    put("detail", detail)
                }
            )
        }

        validatePermissions(diagnostic, ::add)
        validateReportSchema(daily, ::add)
        validateHistoryAvailability(daily, ::add)
        validateReportTotals(daily, ::add)
        validateTimeline(daily, ::add)
        validateAppAggregates(daily, ::add)
        validateLocalIntelligence(diagnostic, ::add)
        validateProductCapabilities(diagnostic, ::add)
        validateComparisonHistoryGuard(context, ::add)
        validateAppTrendEngine(context, ::add)
        validateSystemSurfaceExclusion(daily, ::add)
        validateBackground(diagnostic, ::add)
        validateCoverage(daily, ::add)

        var passCount = 0
        var warnCount = 0
        var failCount = 0

        for (index in 0 until checks.length()) {
            when (checks.getJSONObject(index).getString("status")) {
                PASS -> passCount += 1
                WARN -> warnCount += 1
                FAIL -> failCount += 1
            }
        }

        return JSONObject().apply {
            put("suite_version", SUITE_VERSION)
            put("critical_passed", failCount == 0)
            put("manual_test_required", failCount > 0)
            put("pass_count", passCount)
            put("warning_count", warnCount)
            put("fail_count", failCount)
            put("checks", checks)
            put("user_query_content_stored", false)
        }
    }

    private fun validatePermissions(
        diagnostic: JSONObject,
        add: (String, String, String) -> Unit
    ) {
        val permissions = diagnostic.getJSONObject("permissions")
        val usage = permissions.optBoolean("usage_access", false)
        val internet = permissions.optBoolean("internet_declared", true)
        val queryAll = permissions.optBoolean("query_all_packages_declared", true)

        if (usage && !internet && !queryAll) {
            add(
                "permissions_privacy_contract",
                PASS,
                "Usage Access ativo; INTERNET e QUERY_ALL_PACKAGES ausentes."
            )
        } else {
            add(
                "permissions_privacy_contract",
                FAIL,
                "usage=$usage;internet=$internet;query_all=$queryAll"
            )
        }
    }

    private fun validateReportSchema(
        daily: JSONObject,
        add: (String, String, String) -> Unit
    ) {
        val schema = daily.optInt("schema_version", -1)
        val tracking = daily.getJSONObject("tracking")
        val precision = tracking.optString("aggregation_precision", "")
        val timeline = daily.getJSONArray("timeline")
        val hasMs = timeline.length() == 0 ||
            timeline.getJSONObject(0).has("duration_milliseconds")

        val historyAvailability =
            tracking.has(
                "history_availability_percent"
            )

        if (
            schema == 4 &&
            precision == "milliseconds" &&
            hasMs &&
            historyAvailability
        ) {
            add(
                "report_schema_v4",
                PASS,
                "Schema v4, milissegundos e disponibilidade de histórico ativos."
            )
        } else {
            add(
                "report_schema_v4",
                FAIL,
                "schema=$schema;precision=$precision;duration_ms=$hasMs;history_availability=$historyAvailability"
            )
        }
    }

    private fun validateHistoryAvailability(
        daily: JSONObject,
        add: (String, String, String) -> Unit
    ) {
        val tracking =
            daily.getJSONObject(
                "tracking"
            )

        val requested =
            tracking.optLong(
                "requested_period_milliseconds",
                -1L
            )

        val available =
            tracking.optLong(
                "history_available_milliseconds",
                -1L
            )

        val percent =
            tracking.optDouble(
                "history_availability_percent",
                -1.0
            )

        val expected =
            if (
                requested > 0L
            ) {
                kotlin.math.round(
                    available.toDouble() *
                        1000.0 /
                        requested.toDouble()
                ) /
                    10.0
            } else {
                0.0
            }

        if (
            requested >=
            0L &&
            available in 0L..requested &&
            kotlin.math.abs(
                expected -
                    percent
            ) <=
            0.1
        ) {
            add(
                "history_availability_math",
                PASS,
                "Disponibilidade do histórico fecha com o período solicitado."
            )
        } else {
            add(
                "history_availability_math",
                FAIL,
                "requested=$requested;available=$available;percent=$percent;expected=$expected"
            )
        }
    }

    private fun validateReportTotals(
        daily: JSONObject,
        add: (String, String, String) -> Unit
    ) {
        val summary = daily.getJSONObject("summary")
        val tracking = daily.getJSONObject("tracking")

        val categories =
            summary.getLong("app_usage_milliseconds") +
                summary.getLong("screen_off_milliseconds") +
                summary.getLong("private_milliseconds") +
                summary.getLong("system_milliseconds")

        val recorded = tracking.getLong("recorded_milliseconds")
        val effective = tracking.getLong("effective_period_milliseconds")
        val unclassified = tracking.getLong("unclassified_milliseconds")

        if (categories == recorded && effective - recorded == unclassified) {
            add(
                "report_total_consistency",
                PASS,
                "Totais categorizados, recorded e unclassified fecham em milissegundos."
            )
        } else {
            add(
                "report_total_consistency",
                FAIL,
                "categories=$categories;recorded=$recorded;effective=$effective;unclassified=$unclassified"
            )
        }
    }

    private fun validateTimeline(
        daily: JSONObject,
        add: (String, String, String) -> Unit
    ) {
        val timeline = daily.getJSONArray("timeline")
        var previousEnd: Long? = null
        var overlapCount = 0
        var identityLeakCount = 0
        var badDurationCount = 0

        for (index in 0 until timeline.length()) {
            val item = timeline.getJSONObject(index)
            val start = runCatching {
                OffsetDateTime.parse(item.getString("start_at"))
                    .toInstant().toEpochMilli()
            }.getOrNull()
            val end = runCatching {
                OffsetDateTime.parse(item.getString("end_at"))
                    .toInstant().toEpochMilli()
            }.getOrNull()

            if (start == null || end == null) {
                badDurationCount += 1
            } else {
                val duration = item.optLong("duration_milliseconds", -1L)
                if (duration != (end - start).coerceAtLeast(0L)) {
                    badDurationCount += 1
                }

                val previous = previousEnd
                if (previous != null && start < previous) {
                    overlapCount += 1
                }
                previousEnd = maxOf(previous ?: end, end)
            }

            val type = item.optString("type")
            if (type != "APP" && (item.has("package") || item.has("name"))) {
                identityLeakCount += 1
            }
        }

        if (overlapCount == 0 && identityLeakCount == 0 && badDurationCount == 0) {
            add(
                "timeline_privacy_and_overlap",
                PASS,
                "${timeline.length()} intervalos; sem sobreposição, identidade não-APP ou duração inconsistente."
            )
        } else {
            add(
                "timeline_privacy_and_overlap",
                FAIL,
                "overlaps=$overlapCount;identity_leaks=$identityLeakCount;bad_durations=$badDurationCount"
            )
        }
    }

    private fun validateAppAggregates(
        daily: JSONObject,
        add: (String, String, String) -> Unit
    ) {
        val summary = daily.getJSONObject("summary")
        val apps = daily.getJSONArray("apps")
        val seen = mutableSetOf<String>()
        var duplicates = 0
        var orderErrors = 0
        var aggregateMs = 0L
        var previousMs: Long? = null

        for (index in 0 until apps.length()) {
            val app = apps.getJSONObject(index)
            val pkg = app.optString("package")
            val millis = app.optLong("foreground_milliseconds", -1L)

            if (!seen.add(pkg)) duplicates += 1
            if (millis > 0L) aggregateMs += millis

            val previous = previousMs
            if (previous != null && millis > previous) orderErrors += 1
            previousMs = millis
        }

        val expected = summary.getLong("app_usage_milliseconds")
        val delta = abs(aggregateMs - expected)

        if (duplicates == 0 && orderErrors == 0 && delta == 0L) {
            add(
                "app_aggregate_consistency",
                PASS,
                "Apps únicos, ordenados e soma agregada igual ao total APP."
            )
        } else {
            add(
                "app_aggregate_consistency",
                FAIL,
                "duplicates=$duplicates;order_errors=$orderErrors;aggregate=$aggregateMs;summary=$expected;delta=$delta"
            )
        }
    }

    private fun validateLocalIntelligence(
        diagnostic: JSONObject,
        add: (String, String, String) -> Unit
    ) {
        val check = diagnostic.optJSONObject("local_intelligence_self_check")
        val passed = check?.optBoolean("passed", false) == true
        val storesUserQuery = check?.optBoolean("user_query_content_stored", true) == true

        if (passed && !storesUserQuery) {
            add(
                "local_intelligence_self_check",
                PASS,
                "Parser/engine automático passou e perguntas do usuário não são persistidas."
            )
        } else {
            add(
                "local_intelligence_self_check",
                FAIL,
                "passed=$passed;user_query_content_stored=$storesUserQuery"
            )
        }
    }

    private fun validateProductCapabilities(
        diagnostic: JSONObject,
        add: (String, String, String) -> Unit
    ) {
        val capabilities =
            diagnostic.getJSONObject(
                "capabilities"
            )

        val engineVersion =
            capabilities.optInt(
                "local_question_engine_version",
                0
            )

        val required =
            listOf(
                "local_question_calendar_day",
                "local_question_calendar_range",
                "local_question_compare_today_yesterday",
                "local_question_compare_last_24h_previous_24h",
                "local_question_compare_last_7d_previous_7d",
                "local_question_compare_calendar_periods",
                "local_question_history_readiness_guard",
                "automatic_local_insight_cards",
                "validation_pack_export"
            )

        val missing =
            required.filter {
                !capabilities.optBoolean(
                    it,
                    false
                )
            }

        if (
            engineVersion >=
            7 &&
            missing.isEmpty()
        ) {
            add(
                "product_capabilities_v7",
                PASS,
                "Engine v$engineVersion com comparações protegidas por maturidade do histórico."
            )
        } else {
            add(
                "product_capabilities_v7",
                FAIL,
                "engine=$engineVersion;missing=${missing.joinToString(",")}"
            )
        }
    }

    private fun validateComparisonHistoryGuard(
        context: Context,
        add: (String, String, String) -> Unit
    ) {
        val now =
            System.currentTimeMillis()

        fun report(
            start: Long,
            end: Long
        ): JSONObject =
            ReportGenerator(
                context
            ).periodJson(
                start,
                end
            )

        val day =
            24L *
                60L *
                60L *
                1000L

        val current24 =
            report(
                now - day,
                now
            )

        val previous24 =
            report(
                now - 2L * day,
                now - day
            )

        val current7 =
            report(
                now - 7L * day,
                now
            )

        val previous7 =
            report(
                now - 14L * day,
                now - 7L * day
            )

        fun ready(
            first: JSONObject,
            second: JSONObject
        ): Boolean {
            val a =
                first.getJSONObject(
                    "tracking"
                )

            val b =
                second.getJSONObject(
                    "tracking"
                )

            return HistoryReadiness.canCompare(
                a.optDouble(
                    "history_availability_percent",
                    0.0
                ),
                b.optDouble(
                    "history_availability_percent",
                    0.0
                ),
                a.optDouble(
                    "coverage_percent",
                    0.0
                ),
                b.optDouble(
                    "coverage_percent",
                    0.0
                )
            )
        }

        val answer24 =
            LocalQuestionEngine(
                context
            ).answer(
                "Compare as últimas 24 horas com as 24 anteriores",
                now
            ).text

        val answer7 =
            LocalQuestionEngine(
                context
            ).answer(
                "Compare os últimos 7 dias com os 7 anteriores",
                now
            ).text

        val guard24 =
            answer24.contains(
                "Histórico insuficiente"
            )

        val guard7 =
            answer7.contains(
                "Histórico insuficiente"
            )

        val trackingStart =
            runCatching {
                context
                    .getSharedPreferences(
                        "guardian_collector",
                        Context.MODE_PRIVATE
                    )
                    .getLong(
                        "tracking_start_ms",
                        0L
                    )
            }.getOrDefault(0L)

        val calendarReference =
            if (
                trackingStart >
                0L
            ) {
                trackingStart
            } else {
                now
            }

        val dateFormat =
            SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.US
            )

        val calendarFirst =
            dateFormat.format(
                Date(
                    calendarReference -
                        3L * day
                )
            )

        val calendarSecond =
            dateFormat.format(
                Date(
                    calendarReference -
                        2L * day
                )
            )

        val answerCalendar =
            LocalQuestionEngine(
                context
            ).answer(
                "Compare $calendarFirst com $calendarSecond",
                now
            ).text

        val guardCalendar =
            answerCalendar.contains(
                "Histórico insuficiente"
            )

        val expected24 =
            !ready(
                current24,
                previous24
            )

        val expected7 =
            !ready(
                current7,
                previous7
            )

        if (
            guard24 ==
            expected24 &&
            guard7 ==
            expected7 &&
            guardCalendar
        ) {
            add(
                "comparison_history_guard",
                PASS,
                "Comparações 24h/7d/calendário bloqueiam deltas quando o histórico solicitado ainda está incompleto."
            )
        } else {
            add(
                "comparison_history_guard",
                FAIL,
                "guard24=$guard24;expected24=$expected24;guard7=$guard7;expected7=$expected7;guard_calendar=$guardCalendar"
            )
        }
    }

    private fun validateAppTrendEngine(
        context: Context,
        add: (String, String, String) -> Unit
    ) {
        val engine =
            AppTrendEngine(
                context
            )

        val now =
            System.currentTimeMillis()

        fun expectedReady(
            durationMs: Long
        ): Boolean {
            val generator =
                ReportGenerator(
                    context
                )

            val current =
                generator.periodJson(
                    now - durationMs,
                    now
                )
                    .getJSONObject(
                        "tracking"
                    )

            val previous =
                generator.periodJson(
                    now - 2L * durationMs,
                    now - durationMs
                )
                    .getJSONObject(
                        "tracking"
                    )

            return HistoryReadiness.canCompare(
                current.optDouble(
                    "history_availability_percent",
                    0.0
                ),
                previous.optDouble(
                    "history_availability_percent",
                    0.0
                ),
                current.optDouble(
                    "coverage_percent",
                    0.0
                ),
                previous.optDouble(
                    "coverage_percent",
                    0.0
                )
            )
        }

        val dayMs =
            24L *
                60L *
                60L *
                1000L

        val result24 =
            engine.analyze(
                AppTrendPeriod.LAST_24_HOURS,
                now
            )

        val result7 =
            engine.analyze(
                AppTrendPeriod.LAST_7_DAYS,
                now
            )

        fun orderingValid(
            result: AppTrendResult
        ): Boolean {
            if (
                !result.ready
            ) {
                return result.increases.isEmpty() &&
                    result.decreases.isEmpty() &&
                    result.changedAppsCount == 0
            }

            val positive =
                result.increases.all {
                    it.deltaMilliseconds >
                        0L
                }

            val negative =
                result.decreases.all {
                    it.deltaMilliseconds <
                        0L
                }

            val increasesSorted =
                result.increases
                    .zipWithNext()
                    .all {
                        (a, b) ->
                        a.deltaMilliseconds >=
                            b.deltaMilliseconds
                    }

            val decreasesSorted =
                result.decreases
                    .zipWithNext()
                    .all {
                        (a, b) ->
                        a.deltaMilliseconds <=
                            b.deltaMilliseconds
                    }

            return positive &&
                negative &&
                increasesSorted &&
                decreasesSorted &&
                result.changedAppsCount ==
                    result.increases.size +
                    result.decreases.size
        }

        val expected24 =
            expectedReady(
                dayMs
            )

        val expected7 =
            expectedReady(
                7L * dayMs
            )

        val pass =
            result24.ready ==
                expected24 &&
                result7.ready ==
                    expected7 &&
                orderingValid(
                    result24
                ) &&
                orderingValid(
                    result7
                )

        if (
            pass
        ) {
            add(
                "app_trend_engine_v1",
                PASS,
                "Tendências por app respeitam maturidade do histórico, sinal do delta e ordenação."
            )
        } else {
            add(
                "app_trend_engine_v1",
                FAIL,
                "ready24=${result24.ready};expected24=$expected24;ready7=${result7.ready};expected7=$expected7"
            )
        }
    }

    private fun validateSystemSurfaceExclusion(
        daily: JSONObject,
        add: (String, String, String) -> Unit
    ) {
        val blocked =
            setOf(
                "com.android.documentsui",
                "com.google.android.documentsui",
                "com.google.android.photopicker",
                "com.mi.appfinder",
                "com.android.providers.downloads.ui",
                "com.miui.android.fashiongallery",
                "com.android.packageinstaller",
                "com.google.android.packageinstaller",
                "com.miui.global.packageinstaller"
            )

        val apps =
            daily.getJSONArray(
                "apps"
            )

        val found =
            mutableListOf<String>()

        for (
            index in
            0 until apps.length()
        ) {
            val pkg =
                apps.getJSONObject(
                    index
                )
                    .optString(
                        "package"
                    )

            if (
                pkg in
                blocked
            ) {
                found +=
                    pkg
            }
        }

        if (
            found.isEmpty()
        ) {
            add(
                "system_surface_exclusion",
                PASS,
                "Superfícies técnicas conhecidas não aparecem como APP."
            )
        } else {
            add(
                "system_surface_exclusion",
                FAIL,
                "found=${found.joinToString(",")}"
            )
        }
    }

    private fun validateBackground(
        diagnostic: JSONObject,
        add: (String, String, String) -> Unit
    ) {
        val scheduler = diagnostic.getJSONObject("scheduler")
        val present = scheduler.optBoolean("periodic_work_present_now", false)
        val infos = scheduler.optJSONArray("work_infos") ?: JSONArray()
        var active = 0

        for (index in 0 until infos.length()) {
            val state = infos.getJSONObject(index).optString("state")
            if (state == "ENQUEUED" || state == "RUNNING" || state == "BLOCKED") {
                active += 1
            }
        }

        val failures = scheduler.optInt("worker_failure_count", 0)
        val retries = scheduler.optInt("worker_retry_count", 0)
        val stopped = scheduler.optInt("worker_stopped_count", 0)

        if (present && active == 1) {
            val status = if (failures == 0 && retries == 0 && stopped == 0) PASS else WARN
            add(
                "workmanager_unique_periodic",
                status,
                "active=$active;failure=$failures;retry=$retries;stopped=$stopped"
            )
        } else {
            add(
                "workmanager_unique_periodic",
                FAIL,
                "present=$present;active=$active"
            )
        }
    }

    private fun validateCoverage(
        daily: JSONObject,
        add: (String, String, String) -> Unit
    ) {
        val coverage = daily.getJSONObject("tracking")
            .optDouble("coverage_percent", 0.0)

        val status = if (coverage >= 95.0) PASS else WARN
        add("tracking_coverage", status, "coverage=$coverage%")
    }
}
