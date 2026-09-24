package com.bigcorps.guardian.core

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context

object GuardianScheduler {
    private const val JOB_ID = 41001
    private const val PERIOD_MS = 6L * 60L * 60L * 1000L

    fun ensureScheduled(context: Context): Boolean {
        val scheduler =
            context.getSystemService(JobScheduler::class.java)
                ?: return false

        if (scheduler.getPendingJob(JOB_ID) != null) {
            return true
        }

        val info = JobInfo.Builder(
            JOB_ID,
            ComponentName(context, GuardianJobService::class.java)
        )
            .setPersisted(true)
            .setPeriodic(PERIOD_MS)
            .build()

        val result = scheduler.schedule(info)
        val pending = scheduler.getPendingJob(JOB_ID) != null

        runCatching {
            GuardianDatabase(context).logTechnical(
                "JOB_SCHEDULE",
                "result=$result;pending=$pending"
            )
        }

        return result == JobScheduler.RESULT_SUCCESS && pending
    }

    fun isScheduled(context: Context): Boolean {
        return context
            .getSystemService(JobScheduler::class.java)
            ?.getPendingJob(JOB_ID) != null
    }
}
