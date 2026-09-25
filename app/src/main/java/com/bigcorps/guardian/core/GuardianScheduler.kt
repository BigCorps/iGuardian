package com.bigcorps.guardian.core

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper

object GuardianScheduler {
    const val LOGIC_VERSION = 3

    private const val JOB_ID = 41001
    private const val PERIOD_MS = 30L * 60L * 1000L

    // Important for a process started by JobScheduler itself:
    // Application.onCreate() runs before GuardianJobService.onStartJob().
    // A short grace window prevents us from mistaking that normal job launch
    // for a missing schedule and scheduling a second periodic job.
    private const val PROCESS_START_GRACE_MS = 5_000L
    private const val RECENT_JOB_GRACE_MS = 15_000L

    fun recoverAfterProcessStart(context: Context) {
        val appContext = context.applicationContext
        val state = SchedulerStateStore(appContext)
        state.recordProcessStart()

        Handler(Looper.getMainLooper()).postDelayed({
            val now = System.currentTimeMillis()
            val lastJobStart = state.lastJobStartMs()
            val recentJobLaunch =
                lastJobStart > 0L &&
                    now >= lastJobStart &&
                    now - lastJobStart <= RECENT_JOB_GRACE_MS

            if (recentJobLaunch) {
                state.recordProcessStartDecision(
                    decision = "skip_recent_job_launch",
                    skippedRecovery = true,
                    nowMs = now
                )
                runCatching {
                    GuardianDatabase(appContext).logTechnical(
                        "SCHEDULER_RECOVERY_SKIP",
                        "reason=recent_job_launch"
                    )
                }
                return@postDelayed
            }

            if (isScheduled(appContext)) {
                state.recordCheck("process_start_grace", true, now)
                state.recordProcessStartDecision(
                    decision = "pending_ok",
                    skippedRecovery = true,
                    nowMs = now
                )
                return@postDelayed
            }

            state.recordProcessStartDecision(
                decision = "recover_missing",
                skippedRecovery = false,
                nowMs = now
            )
            ensureScheduled(
                appContext,
                "process_start_grace"
            )
        }, PROCESS_START_GRACE_MS)
    }

    fun ensureScheduled(
        context: Context,
        reason: String
    ): Boolean {
        val scheduler =
            context.getSystemService(JobScheduler::class.java) ?: return false

        val state = SchedulerStateStore(context)
        val existing = scheduler.getPendingJob(JOB_ID)

        if (existing != null) {
            state.recordCheck(reason, true)
            return true
        }

        state.recordCheck(reason, false)

        val info = JobInfo.Builder(
            JOB_ID,
            ComponentName(context, GuardianJobService::class.java)
        )
            .setPersisted(true)
            .setPeriodic(PERIOD_MS)
            .build()

        val result = scheduler.schedule(info)
        val pending = scheduler.getPendingJob(JOB_ID) != null

        state.recordScheduleAttempt(reason, result, pending)

        runCatching {
            GuardianDatabase(context).logTechnical(
                "JOB_SCHEDULE",
                "reason=${reason.take(24)};result=$result;pending=$pending"
            )
        }

        return result == JobScheduler.RESULT_SUCCESS && pending
    }

    fun isScheduled(context: Context): Boolean =
        context.getSystemService(JobScheduler::class.java)
            ?.getPendingJob(JOB_ID) != null

    fun periodMinutes(): Int =
        (PERIOD_MS / 60_000L).toInt()

    fun processStartGraceMs(): Long = PROCESS_START_GRACE_MS
    fun recentJobGraceMs(): Long = RECENT_JOB_GRACE_MS
}
