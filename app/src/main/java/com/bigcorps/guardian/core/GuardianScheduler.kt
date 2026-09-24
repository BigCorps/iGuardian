package com.bigcorps.guardian.core

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context

object GuardianScheduler {
    private const val JOB_ID = 41001
    private const val PERIOD_MS = 30L * 60L * 1000L

    fun ensureScheduled(context: Context, reason: String): Boolean {
        val scheduler = context.getSystemService(JobScheduler::class.java) ?: return false
        val state = SchedulerStateStore(context)
        val existing = scheduler.getPendingJob(JOB_ID)
        if (existing != null) {
            state.recordCheck(reason, true)
            return true
        }

        state.recordCheck(reason, false)
        val info = JobInfo.Builder(JOB_ID, ComponentName(context, GuardianJobService::class.java))
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
        context.getSystemService(JobScheduler::class.java)?.getPendingJob(JOB_ID) != null

    fun periodMinutes(): Int = (PERIOD_MS / 60_000L).toInt()
}
