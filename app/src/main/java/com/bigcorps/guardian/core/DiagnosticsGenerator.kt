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
        val power = context.getSystemService(PowerManager::class.java)
        val activity = context.getSystemService(ActivityManager::class.java)
        val usage = context.getSystemService(UsageStatsManager::class.java)
        val user = context.getSystemService(UserManager::class.java)
        val requestedPermissions = packagePermissions()
        val reports = LocalReportStore(context)

        val technical = JSONArray()
        db.recentTechnical().forEach { event ->
            technical.put(JSONObject().apply {
                put("code", event.code)
                put("at", iso(event.tsMs))
                event.value?.let { put("value", it) }
            })
        }

        return JSONObject().apply {
            put("diagnostic_schema", 1)
            put("generated_at", iso(System.currentTimeMillis()))
            put("app", JSONObject().apply {
                put("version", versionName())
                put("version_code", versionCode())
                put("package", context.packageName)
                put("dev_build", context.packageName.endsWith(".dev"))
            })
            put("device", DeviceInfo.asJson(context))
            put("environment", JSONObject().apply {
                put("locale", Locale.getDefault().toLanguageTag())
                put("timezone", TimeZone.getDefault().id)
                put("user_unlocked", user?.isUserUnlocked ?: JSONObject.NULL)
                put("power_save_mode", power?.isPowerSaveMode ?: JSONObject.NULL)
                put("battery_optimization_ignored", power?.isIgnoringBatteryOptimizations(context.packageName) ?: false)
                if (android.os.Build.VERSION.SDK_INT >= 28) {
                    put("background_restricted", activity?.isBackgroundRestricted ?: JSONObject.NULL)
                }
                if (android.os.Build.VERSION.SDK_INT >= 28) {
                    put("app_standby_bucket", runCatching { usage?.appStandbyBucket }.getOrNull() ?: JSONObject.NULL)
                } else {
                    put("app_standby_bucket", JSONObject.NULL)
                }
            })
            put("permissions", JSONObject().apply {
                put("usage_access", UsageAccess.hasPermission(context))
                put("internet_declared", requestedPermissions.contains("android.permission.INTERNET"))
                put("query_all_packages_declared", requestedPermissions.contains("android.permission.QUERY_ALL_PACKAGES"))
            })
            put("runtime", JSONObject().apply {
                put("periodic_job_scheduled", GuardianScheduler.isScheduled(context))
                put("tracking_start_ms", state.trackingStartMs())
                put("tracking_start_at", state.trackingStartMs().takeIf { it > 0 }?.let(::iso) ?: JSONObject.NULL)
                put("usage_permission_was_active", state.usagePermissionWasActive())
                put("last_permission_baseline_at", state.lastPermissionBaselineMs().takeIf { it > 0 }?.let(::iso) ?: JSONObject.NULL)
                put("collector_cursor_ms", state.cursorMs())
                put("collector_cursor_at", state.cursorMs().takeIf { it > 0 }?.let(::iso) ?: JSONObject.NULL)
                put("private_override_open", state.privateOverrideStartMs() > 0L)
                put("database_size_bytes", db.databaseSizeBytes())
                put("local_daily_reports_count", reports.reportCount())
                put("local_daily_reports_bytes", reports.totalBytes())
                put("privacy_setup_complete", PrivatePreferences(context).setupComplete())
                put("manually_private_apps_count", PrivatePreferences(context).packages().size)
            })
            put("interval_counts", JSONObject().apply {
                put("APP", counts[IntervalType.APP] ?: 0)
                put("PRIVATE", counts[IntervalType.PRIVATE] ?: 0)
                put("SCREEN_OFF", counts[IntervalType.SCREEN_OFF] ?: 0)
                put("ANONYMOUS_BROWSER", counts[IntervalType.ANONYMOUS_BROWSER] ?: 0)
            })
            put("capabilities", JSONObject().apply {
                put("usage_stats", true)
                put("screen_events_mode", if (android.os.Build.VERSION.SDK_INT >= 28) "usage_stats" else "runtime_best_effort")
                put("unlock_detection_mode", if (android.os.Build.VERSION.SDK_INT >= 28) "usage_stats" else "runtime_best_effort")
                put("user_switch_runtime_signal", true)
                put("browser_domains", false)
                put("anonymous_browser_detection", false)
                put("anonymous_browser_schema_ready", true)
                put("network_transport", false)
            })
            put("recent_technical_events", technical)
            put("activity_snapshot_24h", ReportGenerator(context).periodJson(
                System.currentTimeMillis() - 24L * 60L * 60L * 1000L,
                System.currentTimeMillis()
            ))
            put("privacy_guarantees", JSONObject().apply {
                put("private_reason_exported", false)
                put("private_package_exported", false)
                put("full_url_exported", false)
                put("screen_or_input_content_exported", false)
            })
        }
    }

    private fun packagePermissions(): Set<String> {
        return try {
            val info = if (android.os.Build.VERSION.SDK_INT >= 33) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            }
            info.requestedPermissions?.toSet() ?: emptySet()
        } catch (_: Exception) {
            emptySet()
        }
    }

    private fun versionName(): String = try {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        info.versionName ?: "unknown"
    } catch (_: Exception) { "unknown" }

    private fun versionCode(): Long = try {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        if (android.os.Build.VERSION.SDK_INT >= 28) info.longVersionCode else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
    } catch (_: Exception) { 0L }

    private fun iso(ms: Long): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US).format(Date(ms))
}
