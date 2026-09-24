package com.bigcorps.guardian.core

import android.app.job.JobParameters
import android.app.job.JobService

class GuardianJobService : JobService() {
    override fun onStartJob(params: JobParameters?): Boolean {
        val schedulerState = SchedulerStateStore(applicationContext)
        schedulerState.recordJobStart()
        runCatching { GuardianDatabase(applicationContext).logTechnical("JOB_START") }

        Thread {
            var result: UsageCollector.Result? = null
            try {
                result = UsageCollector(applicationContext).collect()
            } finally {
                schedulerState.recordJobFinish()
                runCatching {
                    GuardianDatabase(applicationContext).logTechnical(
                        "JOB_FINISH",
                        result?.let { "events=${it.eventsRead};${it.note.take(40)}" } ?: "no_result"
                    )
                }
                jobFinished(params, false)
                GuardianScheduler.ensureScheduled(applicationContext, "job_finish")
            }
        }.start()
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        SchedulerStateStore(applicationContext).recordJobStop()
        runCatching { GuardianDatabase(applicationContext).logTechnical("JOB_STOP") }
        return true
    }
}
