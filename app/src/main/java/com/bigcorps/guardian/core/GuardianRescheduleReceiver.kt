package com.bigcorps.guardian.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class GuardianRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action?.substringAfterLast('.')?.take(30) ?: "UNKNOWN"
        runCatching {
            GuardianDatabase(context.applicationContext).logTechnical("RESCHEDULE_SIGNAL", action)
        }
        GuardianScheduler.ensureScheduled(context.applicationContext, "broadcast_$action")
    }
}
