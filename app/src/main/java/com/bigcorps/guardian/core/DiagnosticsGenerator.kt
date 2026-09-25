package com.bigcorps.guardian.core

import android.app.ActivityManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.PowerManager
import android.os.UserManager
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class DiagnosticsGenerator(private val context: Context) {
    fun generate(): JSONObject {
        val db = GuardianDatabase(context)
        val counts = db.intervalCounts()
        val state = CollectorStateStore(context)
        val schedulerState = SchedulerStateStore(context)
        val power = context.getSystemService(PowerManager::class.java)
        val activity = context.getSystemService(ActivityManager::class.java)
        val usage = context.getSystemService(UsageStatsManager::class.java)
        val user = context.getSystemService(UserManager::class.java)
        val requestedPermissions = packagePermissions()
        val reports = LocalReportStore(context)

        val technical = JSONArray()
        db.recentTechnical(limit = 60).forEach { event ->
            technical.put(
                JSONObject().apply {
                    put("code", event.code)
                    put("at", iso(event.tsMs))
                    event.value?.let { put("value", it) }
                }
            )
        }

        return JSONObject().apply {
            put("diagnostic_schema", 8)
            put("generated_at", iso(System.currentTimeMillis()))

            put(
                "app",
                JSONObject().apply {
                    put("version", versionName())
                    put("version_code", versionCode())
                    put("package", context.packageName)
                    put("dev_build", context.packageName.endsWith(".dev"))
                }
            )

            put("device", DeviceInfo.asJson(context))

            put(
                "environment",
                JSONObject().apply {
                    put("locale", Locale.getDefault().toLanguageTag())
                    put("timezone", TimeZone.getDefault().id)
                    put("user_unlocked", user?.isUserUnlocked ?: JSONObject.NULL)
                    put("power_save_mode", power?.isPowerSaveMode ?: JSONObject.NULL)
                    put(
                        "battery_optimization_ignored",
                        power?.isIgnoringBatteryOptimizations(context.packageName)
                            ?: false
                    )

                    if (android.os.Build.VERSION.SDK_INT >= 28) {
                        put(
                            "background_restricted",
                            activity?.isBackgroundRestricted ?: JSONObject.NULL
                        )
                        put(
                            "app_standby_bucket",
                            runCatching { usage?.appStandbyBucket }.getOrNull()
                                ?: JSONObject.NULL
                        )
                    } else {
                        put("app_standby_bucket", JSONObject.NULL)
                    }
                }
            )

            put(
                "permissions",
                JSONObject().apply {
                    put("usage_access", UsageAccess.hasPermission(context))
                    put(
                        "internet_declared",
                        requestedPermissions.contains("android.permission.INTERNET")
                    )
                    put(
                        "query_all_packages_declared",
                        requestedPermissions.contains(
                            "android.permission.QUERY_ALL_PACKAGES"
                        )
                    )
                }
            )

            put(
                "runtime",
                JSONObject().apply {
                    put(
                        "periodic_job_scheduled",
                        GuardianScheduler.isScheduled(context)
                    )
                    put(
                        "background_work_scheduled",
                        GuardianScheduler.isScheduled(context)
                    )
                    put(
                        "background_engine",
                        GuardianScheduler.engine()
                    )
                    put("tracking_start_ms", state.trackingStartMs())
                    put(
                        "tracking_start_at",
                        state.trackingStartMs()
                            .takeIf { it > 0L }
                            ?.let(::iso)
                            ?: JSONObject.NULL
                    )
                    put(
                        "usage_permission_was_active",
                        state.usagePermissionWasActive()
                    )
                    put(
                        "last_permission_baseline_at",
                        state.lastPermissionBaselineMs()
                            .takeIf { it > 0L }
                            ?.let(::iso)
                            ?: JSONObject.NULL
                    )
                    put("collector_cursor_ms", state.cursorMs())
                    put(
                        "collector_cursor_at",
                        state.cursorMs()
                            .takeIf { it > 0L }
                            ?.let(::iso)
                            ?: JSONObject.NULL
                    )
                    put(
                        "private_override_open",
                        state.privateOverrideStartMs() > 0L
                    )
                    put("database_size_bytes", db.databaseSizeBytes())
                    put("local_daily_reports_count", reports.reportCount())
                    put("local_daily_reports_bytes", reports.totalBytes())
                    put(
                        "privacy_setup_complete",
                        PrivatePreferences(context).setupComplete()
                    )
                    put(
                        "manually_private_apps_count",
                        PrivatePreferences(context).packages().size
                    )
                    put(
                        "privacy_classifier_version",
                        PrivacyClassifier.CLASSIFIER_VERSION
                    )
                    put(
                        "privacy_repair_applied_version",
                        PrivacyRepair.appliedVersion(context)
                    )
                }
            )

            val workSnapshot =
                GuardianScheduler.snapshot(
                    context
                )

            put(
                "scheduler",
                JSONObject().apply {
                    put(
                        "logic_version",
                        schedulerState.logicVersion()
                    )
                    put(
                        "engine",
                        GuardianScheduler.engine()
                    )
                    put(
                        "workmanager_version",
                        GuardianScheduler.WORKMANAGER_VERSION
                    )
                    put(
                        "unique_work_name",
                        GuardianScheduler.UNIQUE_WORK_NAME
                    )
                    put(
                        "repeat_minutes",
                        GuardianScheduler.repeatMinutes()
                    )
                    put(
                        "flex_minutes",
                        GuardianScheduler.flexMinutes()
                    )
                    put(
                        "stats_since_at",
                        schedulerState.statsSinceMs()
                            .takeIf { it > 0L }
                            ?.let(::iso)
                            ?: JSONObject.NULL
                    )
                    put(
                        "work_snapshot_available",
                        workSnapshot.available
                    )
                    put(
                        "work_snapshot_error",
                        workSnapshot.error
                            ?: JSONObject.NULL
                    )
                    put(
                        "periodic_work_present_now",
                        GuardianScheduler.isScheduled(
                            context
                        )
                    )
                    put(
                        "work_infos",
                        JSONArray().apply {
                            workSnapshot.infos.forEach { item ->
                                put(
                                    JSONObject().apply {
                                        put("id", item.id)
                                        put("state", item.state)
                                        put(
                                            "generation",
                                            item.generation
                                        )
                                        put(
                                            "run_attempt_count",
                                            item.runAttemptCount
                                        )
                                        put(
                                            "next_schedule_at",
                                            item.nextScheduleTimeMs
                                                .takeIf { it > 0L }
                                                ?.let(::iso)
                                                ?: JSONObject.NULL
                                        )
                                        put(
                                            "stop_reason",
                                            item.stopReason
                                        )
                                        put(
                                            "worker_class_name",
                                            item.workerClassName
                                                ?: JSONObject.NULL
                                        )
                                    }
                                )
                            }
                        }
                    )
                    put(
                        "ensure_count",
                        schedulerState.ensureCount()
                    )
                    put(
                        "enqueue_count",
                        schedulerState.enqueueCount()
                    )
                    put(
                        "last_ensure_at",
                        schedulerState.lastEnsureMs()
                            .takeIf { it > 0L }
                            ?.let(::iso)
                            ?: JSONObject.NULL
                    )
                    put(
                        "last_ensure_reason",
                        schedulerState.lastEnsureReason()
                            ?: JSONObject.NULL
                    )
                    put(
                        "last_ensure_created_new",
                        schedulerState.lastEnsureCreatedNew()
                    )
                    put(
                        "last_work_id",
                        schedulerState.lastWorkId()
                            ?: JSONObject.NULL
                    )
                    put(
                        "worker_run_count",
                        schedulerState.workerRunCount()
                    )
                    put(
                        "worker_success_count",
                        schedulerState.workerSuccessCount()
                    )
                    put(
                        "worker_retry_count",
                        schedulerState.workerRetryCount()
                    )
                    put(
                        "worker_failure_count",
                        schedulerState.workerFailureCount()
                    )
                    put(
                        "last_worker_start_at",
                        schedulerState.lastWorkerStartMs()
                            .takeIf { it > 0L }
                            ?.let(::iso)
                            ?: JSONObject.NULL
                    )
                    put(
                        "last_worker_finish_at",
                        schedulerState.lastWorkerFinishMs()
                            .takeIf { it > 0L }
                            ?.let(::iso)
                            ?: JSONObject.NULL
                    )
                    put(
                        "last_worker_id",
                        schedulerState.lastWorkerId()
                            ?: JSONObject.NULL
                    )
                    put(
                        "last_worker_attempt",
                        schedulerState.lastWorkerAttempt()
                    )
                    put(
                        "last_worker_outcome",
                        schedulerState.lastWorkerOutcome()
                            ?: JSONObject.NULL
                    )
                    put(
                        "last_worker_events",
                        schedulerState.lastWorkerEvents()
                    )
                    put(
                        "last_worker_note",
                        schedulerState.lastWorkerNote()
                            ?: JSONObject.NULL
                    )
                    put(
                        "worker_stopped_count",
                        schedulerState.workerStoppedCount()
                    )
                    put(
                        "last_worker_stopped_at",
                        schedulerState.lastWorkerStoppedMs()
                            .takeIf { it > 0L }
                            ?.let(::iso)
                            ?: JSONObject.NULL
                    )
                    put(
                        "last_worker_stop_reason",
                        schedulerState.lastWorkerStopReason()
                    )
                    put(
                        "last_worker_stopped_attempt",
                        schedulerState.lastWorkerStoppedAttempt()
                    )
                    val recentStarts =
                        schedulerState.recentWorkerStartsMs()

                    put(
                        "recent_worker_starts_at",
                        JSONArray().apply {
                            recentStarts.forEach { value -> put(iso(value)) }
                        }
                    )
                    put(
                        "recent_worker_intervals_seconds",
                        JSONArray().apply {
                            recentStarts.zipWithNext().forEach { (before, after) ->
                                put((after - before).coerceAtLeast(0L) / 1000L)
                            }
                        }
                    )
                    put(
                        "last_worker_finish_age_seconds",
                        schedulerState.lastWorkerFinishMs()
                            .takeIf { it > 0L }
                            ?.let {
                                (System.currentTimeMillis() - it)
                                    .coerceAtLeast(0L) / 1000L
                            }
                            ?: JSONObject.NULL
                    )
                    put(
                        "reschedule_signal_count",
                        schedulerState.rescheduleSignalCount()
                    )
                    put(
                        "last_reschedule_signal",
                        schedulerState.lastRescheduleSignal()
                            ?: JSONObject.NULL
                    )
                    put(
                        "last_reschedule_signal_at",
                        schedulerState.lastRescheduleSignalMs()
                            .takeIf { it > 0L }
                            ?.let(::iso)
                            ?: JSONObject.NULL
                    )
                    put(
                        "legacy_jobs_cancelled_at",
                        schedulerState.legacyJobsCancelledMs()
                            .takeIf { it > 0L }
                            ?.let(::iso)
                            ?: JSONObject.NULL
                    )
                }
            )

            put(
                "interval_counts",
                JSONObject().apply {
                    put("APP", counts[IntervalType.APP] ?: 0)
                    put("PRIVATE", counts[IntervalType.PRIVATE] ?: 0)
                    put("SCREEN_OFF", counts[IntervalType.SCREEN_OFF] ?: 0)
                    put("SYSTEM", counts[IntervalType.SYSTEM] ?: 0)
                    put(
                        "ANONYMOUS_BROWSER",
                        counts[IntervalType.ANONYMOUS_BROWSER] ?: 0
                    )
                }
            )

            put(
                "capabilities",
                JSONObject().apply {
                    put("usage_stats", true)
                    put(
                        "screen_events_mode",
                        if (android.os.Build.VERSION.SDK_INT >= 28)
                            "usage_stats"
                        else
                            "runtime_best_effort"
                    )
                    put(
                        "unlock_detection_mode",
                        if (android.os.Build.VERSION.SDK_INT >= 28)
                            "usage_stats_distinct_timestamp"
                        else
                            "runtime_best_effort"
                    )
                    put("user_switch_runtime_signal", true)
                    put("system_surface_separation", true)
                    put("timeline_overlap_resolution", true)
                    put("serialized_collection", true)
                    put("process_start_scheduler_guard", false)
                    put("millisecond_summary_aggregation", true)
                    put("report_schema_version", 3)
                    put("chained_one_shot_scheduler", false)
                    put("workmanager_background", true)
                    put("workmanager_unique_periodic", true)
                    put("background_exact_periodicity_guaranteed", false)
                    put("background_catch_up_collection", true)
                    put("workmanager_version", GuardianScheduler.WORKMANAGER_VERSION)
                    put("local_question_engine", true)
                    put("local_question_engine_version", 3)
                    put("local_question_period_today", true)
                    put("local_question_period_yesterday", true)
                    put("local_question_period_last_24_hours", true)
                    put("local_question_period_last_7_days", true)
                    put("local_question_compare_today_yesterday", true)
                    put("local_question_deterministic_insights", true)
                    put("local_question_persists_queries", false)
                    put("browser_domains", false)
                    put("anonymous_browser_detection", false)
                    put("anonymous_browser_schema_ready", true)
                    put("network_transport", false)
                }
            )

            put(
                "local_intelligence_self_check",
                runCatching {
                    LocalIntelligenceSelfCheck.run(context)
                }.getOrElse {
                    JSONObject().apply {
                        put("passed", false)
                        put("error", it::class.java.simpleName.take(60))
                        put("user_query_content_stored", false)
                    }
                }
            )

            put("recent_technical_events", technical)

            put(
                "activity_snapshot_24h",
                ReportGenerator(context).periodJson(
                    System.currentTimeMillis() - 24L * 60L * 60L * 1000L,
                    System.currentTimeMillis()
                )
            )

            put(
                "privacy_guarantees",
                JSONObject().apply {
                    put("private_reason_exported", false)
                    put("private_package_exported", false)
                    put("system_package_exported", false)
                    put("full_url_exported", false)
                    put("screen_or_input_content_exported", false)
                }
            )
        }
    }

    private fun packagePermissions(): Set<String> = try {
        val info =
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(
                        PackageManager.GET_PERMISSIONS.toLong()
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_PERMISSIONS
                )
            }

        info.requestedPermissions?.toSet() ?: emptySet()
    } catch (_: Exception) {
        emptySet()
    }

    private fun versionName(): String = try {
        @Suppress("DEPRECATION")
        context.packageManager
            .getPackageInfo(context.packageName, 0)
            .versionName ?: "unknown"
    } catch (_: Exception) {
        "unknown"
    }

    private fun versionCode(): Long = try {
        @Suppress("DEPRECATION")
        val info =
            context.packageManager.getPackageInfo(context.packageName, 0)

        if (android.os.Build.VERSION.SDK_INT >= 28)
            info.longVersionCode
        else
            info.versionCode.toLong()
    } catch (_: Exception) {
        0L
    }

    private fun iso(ms: Long): String =
        SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            Locale.US
        ).format(Date(ms))
}
