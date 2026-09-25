package com.bigcorps.guardian.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class GuardianRescheduleReceiver :
    BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        val action =
            intent.action
                ?.substringAfterLast('.')
                ?.take(40)
                ?: "UNKNOWN"

        val appContext =
            context.applicationContext

        SchedulerStateStore(
            appContext
        ).recordRescheduleSignal(
            action
        )

        runCatching {
            GuardianDatabase(
                appContext
            ).logTechnical(
                "RESCHEDULE_SIGNAL",
                action
            )
        }

        GuardianScheduler
            .ensureScheduled(
                appContext,
                "broadcast_$action"
            )
    }
}
