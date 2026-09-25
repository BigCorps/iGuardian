package com.bigcorps.guardian.core

import android.content.Context

class PrivatePreferences(context: Context) {
    private val prefs =
        context.getSharedPreferences("guardian_private_apps", Context.MODE_PRIVATE)

    fun packages(): Set<String> =
        prefs.getStringSet(KEY_PACKAGES, emptySet())?.toSet() ?: emptySet()

    fun isPrivate(packageName: String): Boolean =
        packages().contains(packageName)

    fun setPrivate(packageName: String, privateValue: Boolean) {
        val current = packages().toMutableSet()
        if (privateValue) current += packageName
        else current -= packageName
        prefs.edit().putStringSet(KEY_PACKAGES, current).apply()
    }

    fun setupComplete(): Boolean =
        prefs.getBoolean("setup_complete", false)

    fun markSetupComplete() =
        prefs.edit().putBoolean("setup_complete", true).apply()

    companion object {
        private const val KEY_PACKAGES = "packages"
    }
}

class CollectorStateStore(context: Context) {
    private val prefs =
        context.getSharedPreferences("guardian_collector_state", Context.MODE_PRIVATE)

    fun cursorMs(): Long = prefs.getLong("cursor_ms", 0L)
    fun setCursorMs(value: Long) = prefs.edit().putLong("cursor_ms", value).apply()

    fun trackingStartMs(): Long = prefs.getLong("tracking_start_ms", 0L)

    fun markTrackingStartIfMissing(nowMs: Long = System.currentTimeMillis()) {
        if (trackingStartMs() <= 0L) {
            prefs.edit().putLong("tracking_start_ms", nowMs).apply()
        }
    }

    fun usagePermissionWasActive(): Boolean =
        prefs.getBoolean("usage_permission_active", false)

    fun setUsagePermissionActive(active: Boolean) =
        prefs.edit().putBoolean("usage_permission_active", active).apply()

    fun lastPermissionBaselineMs(): Long =
        prefs.getLong("permission_baseline_ms", 0L)

    fun setLastPermissionBaselineMs(value: Long) =
        prefs.edit().putLong("permission_baseline_ms", value).apply()

    fun openState(): OpenState? {
        val typeName = prefs.getString("open_type", null) ?: return null
        val start = prefs.getLong("open_start", 0L)
        if (start <= 0L) return null

        val type = runCatching {
            IntervalType.valueOf(typeName)
        }.getOrNull() ?: return null

        return OpenState(
            type = type,
            startMs = start,
            packageName =
                if (type == IntervalType.APP) prefs.getString("open_package", null)
                else null,
            appLabel =
                if (type == IntervalType.APP) prefs.getString("open_label", null)
                else null
        )
    }

    fun setOpenState(state: OpenState?) {
        val edit = prefs.edit()
            .remove("open_type")
            .remove("open_start")
            .remove("open_package")
            .remove("open_label")

        if (state != null) {
            edit.putString("open_type", state.type.name)
                .putLong("open_start", state.startMs)

            if (state.type == IntervalType.APP) {
                state.packageName?.let { edit.putString("open_package", it) }
                state.appLabel?.let { edit.putString("open_label", it) }
            }
        }
        edit.apply()
    }

    fun privateOverrideStartMs(): Long =
        prefs.getLong("private_override_start", 0L)

    fun setPrivateOverrideStartMs(value: Long) =
        prefs.edit().putLong("private_override_start", value).apply()

    fun clearPrivateOverride() =
        prefs.edit().remove("private_override_start").apply()
}

class SchedulerStateStore(context: Context) {
    private val prefs =
        context.getSharedPreferences(
            "guardian_scheduler_state",
            Context.MODE_PRIVATE
        )

    fun ensureLogicVersion(
        version: Int,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        if (logicVersion() == version) return false

        prefs.edit()
            .clear()
            .putInt("logic_version", version)
            .putLong("stats_since_ms", nowMs)
            .apply()

        return true
    }

    fun recordEnsure(
        reason: String,
        createdNewWork: Boolean,
        workId: String?,
        nowMs: Long = System.currentTimeMillis()
    ) {
        val edit = prefs.edit()
            .putInt("ensure_count", ensureCount() + 1)
            .putLong("last_ensure_ms", nowMs)
            .putString("last_ensure_reason", reason.take(60))
            .putBoolean("last_ensure_created_new", createdNewWork)

        if (createdNewWork) {
            edit.putInt("enqueue_count", enqueueCount() + 1)
        }

        if (workId == null) {
            edit.remove("last_work_id")
        } else {
            edit.putString("last_work_id", workId.take(80))
        }

        edit.apply()
    }

    fun recordWorkerStart(
        workId: String,
        attempt: Int,
        nowMs: Long = System.currentTimeMillis()
    ) {
        val history =
            (recentWorkerStartsMs() + nowMs)
                .takeLast(12)
                .joinToString(",")

        prefs.edit()
            .putInt("worker_run_count", workerRunCount() + 1)
            .putLong("last_worker_start_ms", nowMs)
            .putString("last_worker_id", workId.take(80))
            .putInt("last_worker_attempt", attempt)
            .putString("worker_start_history_ms", history)
            .apply()
    }

    fun recordWorkerFinish(
        outcome: String,
        events: Int,
        note: String,
        nowMs: Long = System.currentTimeMillis()
    ) {
        val value = outcome.take(24)
        val edit = prefs.edit()
            .putLong("last_worker_finish_ms", nowMs)
            .putString("last_worker_outcome", value)
            .putInt("last_worker_events", events)
            .putString("last_worker_note", note.take(80))

        when (value) {
            "success" ->
                edit.putInt(
                    "worker_success_count",
                    workerSuccessCount() + 1
                )
            "retry" ->
                edit.putInt(
                    "worker_retry_count",
                    workerRetryCount() + 1
                )
            "failure" ->
                edit.putInt(
                    "worker_failure_count",
                    workerFailureCount() + 1
                )
        }

        edit.apply()
    }

    fun recordWorkerStopped(
        reason: Int,
        attempt: Int,
        nowMs: Long = System.currentTimeMillis()
    ) {
        prefs.edit()
            .putInt(
                "worker_stopped_count",
                workerStoppedCount() + 1
            )
            .putLong(
                "last_worker_stopped_ms",
                nowMs
            )
            .putInt(
                "last_worker_stop_reason",
                reason
            )
            .putInt(
                "last_worker_stopped_attempt",
                attempt
            )
            .apply()
    }

    fun recordRescheduleSignal(
        action: String,
        nowMs: Long = System.currentTimeMillis()
    ) {
        prefs.edit()
            .putInt(
                "reschedule_signal_count",
                rescheduleSignalCount() + 1
            )
            .putString("last_reschedule_signal", action.take(50))
            .putLong("last_reschedule_signal_ms", nowMs)
            .apply()
    }

    fun recordLegacyJobsCancelled(
        nowMs: Long = System.currentTimeMillis()
    ) {
        prefs.edit()
            .putLong("legacy_jobs_cancelled_ms", nowMs)
            .apply()
    }

    fun logicVersion(): Int = prefs.getInt("logic_version", 0)
    fun statsSinceMs(): Long = prefs.getLong("stats_since_ms", 0L)
    fun ensureCount(): Int = prefs.getInt("ensure_count", 0)
    fun enqueueCount(): Int = prefs.getInt("enqueue_count", 0)
    fun lastEnsureMs(): Long = prefs.getLong("last_ensure_ms", 0L)
    fun lastEnsureReason(): String? =
        prefs.getString("last_ensure_reason", null)
    fun lastEnsureCreatedNew(): Boolean =
        prefs.getBoolean("last_ensure_created_new", false)
    fun lastWorkId(): String? =
        prefs.getString("last_work_id", null)

    fun recentWorkerStartsMs(): List<Long> =
        prefs.getString("worker_start_history_ms", "")
            .orEmpty()
            .split(",")
            .mapNotNull { it.toLongOrNull() }
            .filter { it > 0L }
            .takeLast(12)

    fun workerRunCount(): Int = prefs.getInt("worker_run_count", 0)
    fun workerSuccessCount(): Int =
        prefs.getInt("worker_success_count", 0)
    fun workerRetryCount(): Int =
        prefs.getInt("worker_retry_count", 0)
    fun workerFailureCount(): Int =
        prefs.getInt("worker_failure_count", 0)
    fun lastWorkerStartMs(): Long =
        prefs.getLong("last_worker_start_ms", 0L)
    fun lastWorkerFinishMs(): Long =
        prefs.getLong("last_worker_finish_ms", 0L)
    fun lastWorkerId(): String? =
        prefs.getString("last_worker_id", null)
    fun lastWorkerAttempt(): Int =
        prefs.getInt("last_worker_attempt", 0)
    fun lastWorkerOutcome(): String? =
        prefs.getString("last_worker_outcome", null)
    fun lastWorkerEvents(): Int =
        prefs.getInt("last_worker_events", 0)
    fun lastWorkerNote(): String? =
        prefs.getString("last_worker_note", null)

    fun workerStoppedCount(): Int =
        prefs.getInt(
            "worker_stopped_count",
            0
        )

    fun lastWorkerStoppedMs(): Long =
        prefs.getLong(
            "last_worker_stopped_ms",
            0L
        )

    fun lastWorkerStopReason(): Int =
        prefs.getInt(
            "last_worker_stop_reason",
            -1
        )

    fun lastWorkerStoppedAttempt(): Int =
        prefs.getInt(
            "last_worker_stopped_attempt",
            0
        )

    fun rescheduleSignalCount(): Int =
        prefs.getInt("reschedule_signal_count", 0)
    fun lastRescheduleSignal(): String? =
        prefs.getString("last_reschedule_signal", null)
    fun lastRescheduleSignalMs(): Long =
        prefs.getLong("last_reschedule_signal_ms", 0L)
    fun legacyJobsCancelledMs(): Long =
        prefs.getLong("legacy_jobs_cancelled_ms", 0L)
}
