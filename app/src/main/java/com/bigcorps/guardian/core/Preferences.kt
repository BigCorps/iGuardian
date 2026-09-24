package com.bigcorps.guardian.core

import android.content.Context

class PrivatePreferences(context: Context) {
    private val prefs = context.getSharedPreferences("guardian_private_apps", Context.MODE_PRIVATE)
    fun packages(): Set<String> = prefs.getStringSet(KEY_PACKAGES, emptySet())?.toSet() ?: emptySet()
    fun isPrivate(packageName: String): Boolean = packages().contains(packageName)
    fun setPrivate(packageName: String, privateValue: Boolean) {
        val current = packages().toMutableSet()
        if (privateValue) current += packageName else current -= packageName
        prefs.edit().putStringSet(KEY_PACKAGES, current).apply()
    }
    fun setupComplete(): Boolean = prefs.getBoolean("setup_complete", false)
    fun markSetupComplete() = prefs.edit().putBoolean("setup_complete", true).apply()
    companion object { private const val KEY_PACKAGES = "packages" }
}

class CollectorStateStore(context: Context) {
    private val prefs = context.getSharedPreferences("guardian_collector_state", Context.MODE_PRIVATE)
    fun cursorMs(): Long = prefs.getLong("cursor_ms", 0L)
    fun setCursorMs(value: Long) = prefs.edit().putLong("cursor_ms", value).apply()
    fun trackingStartMs(): Long = prefs.getLong("tracking_start_ms", 0L)
    fun markTrackingStartIfMissing(nowMs: Long = System.currentTimeMillis()) {
        if (trackingStartMs() <= 0L) prefs.edit().putLong("tracking_start_ms", nowMs).apply()
    }
    fun usagePermissionWasActive(): Boolean = prefs.getBoolean("usage_permission_active", false)
    fun setUsagePermissionActive(active: Boolean) = prefs.edit().putBoolean("usage_permission_active", active).apply()
    fun lastPermissionBaselineMs(): Long = prefs.getLong("permission_baseline_ms", 0L)
    fun setLastPermissionBaselineMs(value: Long) = prefs.edit().putLong("permission_baseline_ms", value).apply()

    fun openState(): OpenState? {
        val typeName = prefs.getString("open_type", null) ?: return null
        val start = prefs.getLong("open_start", 0L)
        if (start <= 0L) return null
        val type = runCatching { IntervalType.valueOf(typeName) }.getOrNull() ?: return null
        return OpenState(
            type = type,
            startMs = start,
            packageName = if (type == IntervalType.APP) prefs.getString("open_package", null) else null,
            appLabel = if (type == IntervalType.APP) prefs.getString("open_label", null) else null
        )
    }

    fun setOpenState(state: OpenState?) {
        val edit = prefs.edit().remove("open_type").remove("open_start").remove("open_package").remove("open_label")
        if (state != null) {
            edit.putString("open_type", state.type.name).putLong("open_start", state.startMs)
            if (state.type == IntervalType.APP) {
                state.packageName?.let { edit.putString("open_package", it) }
                state.appLabel?.let { edit.putString("open_label", it) }
            }
        }
        edit.apply()
    }

    fun privateOverrideStartMs(): Long = prefs.getLong("private_override_start", 0L)
    fun setPrivateOverrideStartMs(value: Long) = prefs.edit().putLong("private_override_start", value).apply()
    fun clearPrivateOverride() = prefs.edit().remove("private_override_start").apply()
}

class SchedulerStateStore(context: Context) {
    private val prefs = context.getSharedPreferences("guardian_scheduler_state", Context.MODE_PRIVATE)

    fun recordScheduleAttempt(reason: String, result: Int, pending: Boolean, nowMs: Long = System.currentTimeMillis()) {
        prefs.edit()
            .putInt("schedule_attempt_count", scheduleAttemptCount() + 1)
            .putLong("last_schedule_attempt_ms", nowMs)
            .putString("last_schedule_reason", reason.take(40))
            .putInt("last_schedule_result", result)
            .putBoolean("last_schedule_pending", pending)
            .apply()
    }

    fun recordCheck(reason: String, pending: Boolean, nowMs: Long = System.currentTimeMillis()) {
        val edit = prefs.edit()
            .putLong("last_scheduler_check_ms", nowMs)
            .putString("last_scheduler_check_reason", reason.take(40))
            .putBoolean("last_scheduler_check_pending", pending)
        if (!pending) edit.putInt("recovery_count", recoveryCount() + 1)
        edit.apply()
    }

    fun recordJobStart(nowMs: Long = System.currentTimeMillis()) {
        prefs.edit().putInt("job_run_count", jobRunCount() + 1).putLong("last_job_start_ms", nowMs).apply()
    }
    fun recordJobFinish(nowMs: Long = System.currentTimeMillis()) = prefs.edit().putLong("last_job_finish_ms", nowMs).apply()
    fun recordJobStop(nowMs: Long = System.currentTimeMillis()) {
        prefs.edit().putInt("job_stop_count", jobStopCount() + 1).putLong("last_job_stop_ms", nowMs).apply()
    }

    fun scheduleAttemptCount(): Int = prefs.getInt("schedule_attempt_count", 0)
    fun recoveryCount(): Int = prefs.getInt("recovery_count", 0)
    fun jobRunCount(): Int = prefs.getInt("job_run_count", 0)
    fun jobStopCount(): Int = prefs.getInt("job_stop_count", 0)
    fun lastScheduleAttemptMs(): Long = prefs.getLong("last_schedule_attempt_ms", 0L)
    fun lastScheduleReason(): String? = prefs.getString("last_schedule_reason", null)
    fun lastScheduleResult(): Int = prefs.getInt("last_schedule_result", -1)
    fun lastSchedulePending(): Boolean = prefs.getBoolean("last_schedule_pending", false)
    fun lastSchedulerCheckMs(): Long = prefs.getLong("last_scheduler_check_ms", 0L)
    fun lastSchedulerCheckReason(): String? = prefs.getString("last_scheduler_check_reason", null)
    fun lastSchedulerCheckPending(): Boolean = prefs.getBoolean("last_scheduler_check_pending", false)
    fun lastJobStartMs(): Long = prefs.getLong("last_job_start_ms", 0L)
    fun lastJobFinishMs(): Long = prefs.getLong("last_job_finish_ms", 0L)
    fun lastJobStopMs(): Long = prefs.getLong("last_job_stop_ms", 0L)
}
