package com.bigcorps.guardian.core

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class GuardianWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val state = SchedulerStateStore(applicationContext)

        state.recordWorkerStart(
            workId = id.toString(),
            attempt = runAttemptCount
        )

        runCatching {
            GuardianDatabase(applicationContext).logTechnical(
                "WORK_START",
                "attempt=$runAttemptCount"
            )
        }

        return try {
            val result =
                UsageCollector(applicationContext).collect()

            state.recordWorkerFinish(
                outcome = "success",
                events = result.eventsRead,
                note = result.note
            )

            runCatching {
                GuardianDatabase(applicationContext).logTechnical(
                    "WORK_FINISH",
                    "events=${result.eventsRead};${result.note.take(50)}"
                )
            }

            Result.success()
        } catch (t: Throwable) {
            val error =
                t::class.java.simpleName.take(60)

            state.recordWorkerFinish(
                outcome = "retry",
                events = 0,
                note = error
            )

            runCatching {
                GuardianDatabase(applicationContext).logTechnical(
                    "WORK_RETRY",
                    error
                )
            }

            Result.retry()
        }
    }
}
