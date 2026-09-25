package com.bigcorps.guardian.core

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper

object GuardianScheduler {
    const val LOGIC_VERSION = 4

    private const val JOB_A = 41001
    private const val JOB_B = 41002

    private const val DELAY_MS =
        30L * 60L * 1000L

    private const val DEADLINE_MS =
        45L * 60L * 1000L

    private const val PROCESS_START_GRACE_MS =
        8_000L

    fun resetForLogicUpgrade(
        context: Context
    ) {
        val scheduler =
            context.getSystemService(
                JobScheduler::class.java
            ) ?: return

        scheduler.cancel(JOB_A)
        scheduler.cancel(JOB_B)

        scheduleOne(
            context = context,
            jobId = JOB_A,
            reason = "logic_upgrade"
        )
    }

    fun recoverAfterProcessStart(
        context: Context
    ) {
        val appContext =
            context.applicationContext

        val state =
            SchedulerStateStore(appContext)

        val processStartMs =
            System.currentTimeMillis()

        state.recordProcessStart(
            processStartMs
        )

        Handler(
            Looper.getMainLooper()
        ).postDelayed({
            val now =
                System.currentTimeMillis()

            val jobs =
                managedJobIds(
                    appContext
                )

            if (jobs.isNotEmpty()) {
                state.recordCheck(
                    "process_start_grace",
                    true,
                    now
                )

                state.recordProcessStartDecision(
                    decision = "managed_job_present",
                    skippedRecovery = true,
                    nowMs = now
                )

                return@postDelayed
            }

            val lastJobStart =
                state.lastJobStartMs()

            if (
                lastJobStart >=
                processStartMs
            ) {
                state.recordProcessStartDecision(
                    decision = "job_started_during_grace",
                    skippedRecovery = true,
                    nowMs = now
                )

                return@postDelayed
            }

            state.recordProcessStartDecision(
                decision = "recover_missing_chain",
                skippedRecovery = false,
                nowMs = now
            )

            ensureScheduled(
                appContext,
                "process_start_recover"
            )
        }, PROCESS_START_GRACE_MS)
    }

    fun ensureScheduled(
        context: Context,
        reason: String
    ): Boolean {
        val state =
            SchedulerStateStore(context)

        val jobs =
            managedJobIds(context)

        if (jobs.isNotEmpty()) {
            state.recordCheck(
                reason,
                true
            )
            return true
        }

        state.recordCheck(
            reason,
            false
        )

        return scheduleOne(
            context = context,
            jobId = JOB_A,
            reason = reason
        )
    }

    fun scheduleAfterJob(
        context: Context,
        completedJobId: Int
    ): Boolean {
        val nextId =
            if (completedJobId == JOB_A) {
                JOB_B
            } else {
                JOB_A
            }

        val scheduler =
            context.getSystemService(
                JobScheduler::class.java
            ) ?: return false

        val existing =
            scheduler
                .getAllPendingJobs()
                .any {
                    it.id == nextId
                }

        if (existing) {
            SchedulerStateStore(context)
                .recordCheck(
                    "job_finish_existing",
                    true
                )
            return true
        }

        return scheduleOne(
            context = context,
            jobId = nextId,
            reason = "job_finish"
        )
    }

    private fun scheduleOne(
        context: Context,
        jobId: Int,
        reason: String
    ): Boolean {
        val scheduler =
            context.getSystemService(
                JobScheduler::class.java
            ) ?: return false

        val now =
            System.currentTimeMillis()

        val targetMs =
            now + DELAY_MS

        val deadlineMs =
            now + DEADLINE_MS

        val info =
            JobInfo.Builder(
                jobId,
                ComponentName(
                    context,
                    GuardianJobService::class.java
                )
            )
                .setPersisted(true)
                .setMinimumLatency(
                    DELAY_MS
                )
                .setOverrideDeadline(
                    DEADLINE_MS
                )
                .build()

        val result =
            scheduler.schedule(
                info
            )

        val present =
            scheduler
                .getAllPendingJobs()
                .any {
                    it.id == jobId
                }

        SchedulerStateStore(context)
            .recordScheduleAttempt(
                reason = reason,
                result = result,
                pending = present,
                jobId = jobId,
                targetMs = targetMs,
                deadlineMs = deadlineMs
            )

        runCatching {
            GuardianDatabase(context)
                .logTechnical(
                    "JOB_SCHEDULE",
                    "mode=chain;id=$jobId;reason=${reason.take(20)};result=$result;present=$present"
                )
        }

        return (
            result ==
                JobScheduler.RESULT_SUCCESS &&
                present
            )
    }

    fun isScheduled(
        context: Context
    ): Boolean =
        managedJobIds(context)
            .isNotEmpty()

    fun managedJobIds(
        context: Context
    ): List<Int> {
        val scheduler =
            context.getSystemService(
                JobScheduler::class.java
            ) ?: return emptyList()

        return scheduler
            .getAllPendingJobs()
            .map { it.id }
            .filter {
                it == JOB_A ||
                    it == JOB_B
            }
            .distinct()
            .sorted()
    }

    fun pendingReasons(
        context: Context
    ): Map<Int, List<Int>> {
        if (
            Build.VERSION.SDK_INT < 36
        ) {
            return emptyMap()
        }

        val scheduler =
            context.getSystemService(
                JobScheduler::class.java
            ) ?: return emptyMap()

        return listOf(
            JOB_A,
            JOB_B
        ).associateWith { id ->
            runCatching {
                scheduler
                    .getPendingJobReasons(id)
                    .toList()
            }.getOrDefault(
                emptyList()
            )
        }
    }

    fun mode():
        String =
        "chained_one_shot"

    fun delayMinutes():
        Int =
        (
            DELAY_MS /
                60_000L
            )
            .toInt()

    fun deadlineMinutes():
        Int =
        (
            DEADLINE_MS /
                60_000L
            )
            .toInt()

    fun processStartGraceMs():
        Long =
        PROCESS_START_GRACE_MS
}
