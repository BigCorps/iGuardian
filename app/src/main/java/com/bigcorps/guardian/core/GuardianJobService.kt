package com.bigcorps.guardian.core

import android.app.job.JobParameters
import android.app.job.JobService

class GuardianJobService : JobService() {
    override fun onStartJob(params: JobParameters?): Boolean {
        Thread {
            try {
                UsageCollector(applicationContext).collect()
            } finally {
                jobFinished(params, false)
            }
        }.start()
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean = true
}
