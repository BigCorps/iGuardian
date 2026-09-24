package com.bigcorps.guardian.core

import android.app.job.JobParameters
import android.app.job.JobService

class GuardianJobService : JobService() {
    override fun onStartJob(params: JobParameters?): Boolean {
        val schedulerState = SchedulerStateStore(applicationContext)
        schedulerState.recordJobStart()

        runCatching {
            GuardianDatabase(applicationContext)
                .logTechnical("JOB_START")
        }

        Thread {
            var result: UsageCollector.Result? = null
            try {
                result = UsageCollector(applicationContext).collect()
            } finally {
                schedulerState.recordJobFinish()

                runCatching {
                    GuardianDatabase(applicationContext).logTechnical(
                        "JOB_FINISH",
                        result?.let {
                            "events=${it.eventsRead};${it.note.take(40)}"
                        } ?: "no_result"
                    )
                }

                // Periodic JobScheduler jobs reschedule themselves.
                // Re-scheduling here caused immediate repeated runs in 0.1.4.
                jobFinished(params, false)
            }
        }.start()

        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        SchedulerStateStore(applicationContext).recordJobStop()

        runCatching {
            GuardianDatabase(applicationContext).logTechnical("JOB_STOP")
        }

        return true
    }
}
