package com.bigcorps.guardian.core

import android.app.job.JobScheduler
import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object GuardianScheduler {
    const val LOGIC_VERSION = 5
    const val WORKMANAGER_VERSION = "2.12.0"
    const val UNIQUE_WORK_NAME = "guardian_periodic_collect_v1"

    private const val TAG = "guardian_periodic_collect"
    private const val REPEAT_MINUTES = 30L
    private const val FLEX_MINUTES = 10L

    private val legacyJobIds = listOf(41001, 41002)

    data class WorkSnapshot(
        val available: Boolean,
        val infos: List<WorkSnapshotItem>,
        val error: String? = null
    )

    data class WorkSnapshotItem(
        val id: String,
        val state: String,
        val generation: Int,
        val runAttemptCount: Int,
        val nextScheduleTimeMs: Long
    )

    fun resetForLogicUpgrade(context: Context) {
        cancelLegacyJobs(context)

        val manager =
            WorkManager.getInstance(context.applicationContext)

        manager.cancelUniqueWork(UNIQUE_WORK_NAME)

        enqueuePeriodic(
            context = context,
            reason = "logic_upgrade",
            policy = ExistingPeriodicWorkPolicy.REPLACE
        )
    }

    fun ensureScheduled(
        context: Context,
        reason: String
    ): Boolean {
        val snapshot = snapshot(context)

        val active = snapshot.infos.any {
            it.state == WorkInfo.State.ENQUEUED.name ||
                it.state == WorkInfo.State.RUNNING.name ||
                it.state == WorkInfo.State.BLOCKED.name
        }

        if (active) {
            SchedulerStateStore(context).recordEnsure(
                reason = reason,
                createdNewWork = false,
                workId = snapshot.infos.firstOrNull()?.id
            )
            return true
        }

        return enqueuePeriodic(
            context = context,
            reason = reason,
            policy = ExistingPeriodicWorkPolicy.REPLACE
        )
    }

    private fun enqueuePeriodic(
        context: Context,
        reason: String,
        policy: ExistingPeriodicWorkPolicy
    ): Boolean {
        val request =
            PeriodicWorkRequest.Builder(
                GuardianWorker::class.java,
                REPEAT_MINUTES,
                TimeUnit.MINUTES,
                FLEX_MINUTES,
                TimeUnit.MINUTES
            )
                .addTag(TAG)
                .build()

        return try {
            WorkManager.getInstance(
                context.applicationContext
            ).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                policy,
                request
            )

            SchedulerStateStore(context).recordEnsure(
                reason = reason,
                createdNewWork = true,
                workId = request.id.toString()
            )

            runCatching {
                GuardianDatabase(context).logTechnical(
                    "WORK_ENQUEUE",
                    "reason=${reason.take(30)}"
                )
            }

            true
        } catch (t: Throwable) {
            runCatching {
                GuardianDatabase(context).logTechnical(
                    "WORK_ENQUEUE_ERROR",
                    t::class.java.simpleName.take(60)
                )
            }
            false
        }
    }

    fun snapshot(context: Context): WorkSnapshot {
        return try {
            val infos =
                WorkManager.getInstance(
                    context.applicationContext
                )
                    .getWorkInfosForUniqueWork(
                        UNIQUE_WORK_NAME
                    )
                    .get(4, TimeUnit.SECONDS)

            WorkSnapshot(
                available = true,
                infos = infos.map {
                    WorkSnapshotItem(
                        id = it.id.toString(),
                        state = it.state.name,
                        generation = it.generation,
                        runAttemptCount = it.runAttemptCount,
                        nextScheduleTimeMs = it.nextScheduleTimeMillis
                    )
                }
            )
        } catch (t: Throwable) {
            WorkSnapshot(
                available = false,
                infos = emptyList(),
                error = t::class.java.simpleName.take(60)
            )
        }
    }

    fun isScheduled(context: Context): Boolean =
        snapshot(context).infos.any {
            it.state == WorkInfo.State.ENQUEUED.name ||
                it.state == WorkInfo.State.RUNNING.name ||
                it.state == WorkInfo.State.BLOCKED.name
        }

    fun cancelLegacyJobs(context: Context) {
        val scheduler =
            context.getSystemService(JobScheduler::class.java)

        legacyJobIds.forEach { id ->
            runCatching {
                scheduler?.cancel(id)
            }
        }

        SchedulerStateStore(context)
            .recordLegacyJobsCancelled()
    }

    fun repeatMinutes(): Long = REPEAT_MINUTES
    fun flexMinutes(): Long = FLEX_MINUTES
    fun engine(): String = "androidx_workmanager"
}
