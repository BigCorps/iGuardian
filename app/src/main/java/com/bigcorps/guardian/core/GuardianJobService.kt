package com.bigcorps.guardian.core

import android.app.job.JobParameters
import android.app.job.JobService
import android.os.Build

class GuardianJobService :
    JobService() {

    override fun onStartJob(
        params: JobParameters?
    ): Boolean {
        val currentJobId =
            params?.jobId ?: -1

        val schedulerState =
            SchedulerStateStore(
                applicationContext
            )

        schedulerState.recordJobStart(
            currentJobId
        )

        runCatching {
            GuardianDatabase(
                applicationContext
            ).logTechnical(
                "JOB_START",
                "id=$currentJobId"
            )
        }

        Thread {
            var result:
                UsageCollector.Result? =
                null

            var nextScheduled =
                false

            try {
                result =
                    UsageCollector(
                        applicationContext
                    ).collect()

                nextScheduled =
                    GuardianScheduler
                        .scheduleAfterJob(
                            applicationContext,
                            currentJobId
                        )
            } finally {
                schedulerState
                    .recordJobFinish()

                runCatching {
                    GuardianDatabase(
                        applicationContext
                    ).logTechnical(
                        "JOB_FINISH",
                        result?.let {
                            "id=$currentJobId;events=${it.eventsRead};next=$nextScheduled;${it.note.take(28)}"
                        } ?: "id=$currentJobId;no_result;next=$nextScheduled"
                    )
                }

                jobFinished(
                    params,
                    !nextScheduled
                )
            }
        }.start()

        return true
    }

    override fun onStopJob(
        params: JobParameters?
    ): Boolean {
        val reason =
            if (
                Build.VERSION.SDK_INT >= 31
            ) {
                params?.stopReason ?: -1
            } else {
                -1
            }

        SchedulerStateStore(
            applicationContext
        ).recordJobStop(
            reason
        )

        runCatching {
            GuardianDatabase(
                applicationContext
            ).logTechnical(
                "JOB_STOP",
                "id=${params?.jobId ?: -1};reason=$reason"
            )
        }

        // Let Android retry the interrupted one-shot job.
        return true
    }
}
