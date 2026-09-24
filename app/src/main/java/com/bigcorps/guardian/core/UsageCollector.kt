package com.bigcorps.guardian.core

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.UserManager

class UsageCollector(private val context: Context) {
    private val db = GuardianDatabase(context)
    private val state = CollectorStateStore(context)
    private val privatePreferences = PrivatePreferences(context)
    private val systemSurfaceClassifier = SystemSurfaceClassifier(context)

    data class Result(
        val eventsRead: Int,
        val lastEventMs: Long,
        val permission: Boolean,
        val note: String
    )

    fun collect(nowMs: Long = System.currentTimeMillis()): Result =
        synchronized(COLLECT_LOCK) {
            try {
                collectInternal(nowMs)
            } catch (t: Throwable) {
                val errorClass = t::class.java.simpleName.take(80)
                runCatching { db.logTechnical("COLLECT_ERROR", errorClass) }
                Result(
                    0,
                    state.cursorMs(),
                    UsageAccess.hasPermission(context),
                    "error:$errorClass"
                )
            }
        }

    private fun collectInternal(nowMs: Long): Result {
        if (!UsageAccess.hasPermission(context)) {
            state.setUsagePermissionActive(false)
            state.setOpenState(null)
            db.logTechnical("COLLECT_NO_PERMISSION")
            return Result(0, state.cursorMs(), false, "usage_access_required")
        }

        if (!state.usagePermissionWasActive()) {
            state.setUsagePermissionActive(true)
            state.markTrackingStartIfMissing(nowMs)
            state.setLastPermissionBaselineMs(nowMs)
            state.setCursorMs(nowMs)
            state.setOpenState(null)
            db.logTechnical("PERMISSION_BASELINE_RESET")
            LocalReportStore(context).writeToday(nowMs)
            return Result(0, nowMs, true, "permission_baseline_initialized")
        }

        if (state.privateOverrideStartMs() > 0L) {
            db.logTechnical("COLLECT_PRIVATE_ACTIVE")
            return Result(0, state.cursorMs(), true, "private_active")
        }

        val userManager = context.getSystemService(UserManager::class.java)
        if (userManager != null && !userManager.isUserUnlocked) {
            db.logTechnical("COLLECT_USER_LOCKED")
            return Result(0, state.cursorMs(), true, "user_locked")
        }

        val usageStats = context.getSystemService(UsageStatsManager::class.java)
            ?: return Result(0, state.cursorMs(), true, "usage_service_unavailable")

        val cursor = state.cursorMs()
        if (cursor <= 0L) {
            state.markTrackingStartIfMissing(nowMs)
            state.setCursorMs(nowMs)
            db.logTechnical("COLLECT_BASELINE_INITIALIZED")
            LocalReportStore(context).writeToday(nowMs)
            return Result(0, nowMs, true, "baseline_initialized")
        }

        val begin = cursor + 1L
        if (begin >= nowMs) {
            LocalReportStore(context).writeToday(nowMs)
            return Result(0, cursor, true, "up_to_date")
        }

        val events = usageStats.queryEvents(begin, nowMs)
        val event = UsageEvents.Event()
        var read = 0
        var lastTs = cursor

        while (events != null && events.hasNextEvent()) {
            events.getNextEvent(event)
            read++
            lastTs = maxOf(lastTs, event.timeStamp)

            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED ->
                    onActivityResumed(event.packageName, event.timeStamp)

                UsageEvents.Event.SCREEN_NON_INTERACTIVE ->
                    onScreenOff(event.timeStamp)

                UsageEvents.Event.SCREEN_INTERACTIVE ->
                    onScreenInteractive(event.timeStamp)

                UsageEvents.Event.KEYGUARD_HIDDEN ->
                    db.logTechnical("UNLOCK", tsMs = event.timeStamp)
            }
        }

        if (lastTs > cursor) state.setCursorMs(lastTs)

        LocalReportStore(context).writeDaysIntersecting(begin, nowMs)
        db.logTechnical("COLLECT_OK", "events=$read")

        return Result(read, lastTs, true, "ok")
    }

    private fun onActivityResumed(packageName: String?, tsMs: Long) {
        if (packageName.isNullOrBlank()) return

        closeOpen(tsMs)

        if (packageName == context.packageName) {
            state.setOpenState(null)
            return
        }

        val appLabel = resolveLabel(packageName)
        val manual = privatePreferences.packages()

        if (PrivacyClassifier.isPrivate(packageName, manual, appLabel)) {
            state.setOpenState(OpenState(IntervalType.PRIVATE, tsMs))
            return
        }

        if (systemSurfaceClassifier.isSystemSurface(packageName)) {
            state.setOpenState(OpenState(IntervalType.SYSTEM, tsMs))
            return
        }

        state.setOpenState(
            OpenState(
                type = IntervalType.APP,
                startMs = tsMs,
                packageName = packageName,
                appLabel = appLabel
            )
        )
    }

    private fun onScreenOff(tsMs: Long) {
        closeOpen(tsMs)
        state.setOpenState(OpenState(IntervalType.SCREEN_OFF, tsMs))
    }

    private fun onScreenInteractive(tsMs: Long) {
        if (state.openState()?.type == IntervalType.SCREEN_OFF) {
            closeOpen(tsMs)
        }
    }

    fun closeOpen(endMs: Long) {
        val current = state.openState() ?: return

        if (endMs > current.startMs) {
            db.insertInterval(
                TimelineInterval(
                    startMs = current.startMs,
                    endMs = endMs,
                    type = current.type,
                    packageName = current.packageName,
                    appLabel = current.appLabel
                )
            )
        }

        state.setOpenState(null)
    }

    private fun resolveLabel(packageName: String): String = try {
        val pm = context.packageManager
        val info = if (android.os.Build.VERSION.SDK_INT >= 33) {
            pm.getApplicationInfo(
                packageName,
                PackageManager.ApplicationInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            pm.getApplicationInfo(packageName, 0)
        }

        pm.getApplicationLabel(info).toString().take(120)
    } catch (_: Exception) {
        packageName
    }

    companion object {
        // JobService and Activity may request collection at the same time.
        // A process-wide lock prevents duplicate intervals/unlock events/cursor races.
        private val COLLECT_LOCK = Any()
    }
}
