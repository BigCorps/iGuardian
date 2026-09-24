package com.bigcorps.guardian.core

import android.content.Context

object SystemSignalRecorder {
    fun screenOff(context: Context, nowMs: Long = System.currentTimeMillis()) {
        if (CollectorStateStore(context).privateOverrideStartMs() > 0L) return
        val collector = UsageCollector(context)
        collector.closeOpen(nowMs)
        CollectorStateStore(context).setOpenState(OpenState(IntervalType.SCREEN_OFF, nowMs))
        GuardianDatabase(context).logTechnical("SCREEN_OFF_RUNTIME", tsMs = nowMs)
    }

    fun screenOn(context: Context, nowMs: Long = System.currentTimeMillis()) {
        val state = CollectorStateStore(context)
        if (state.privateOverrideStartMs() > 0L) return
        if (state.openState()?.type == IntervalType.SCREEN_OFF) {
            UsageCollector(context).closeOpen(nowMs)
        }
        GuardianDatabase(context).logTechnical("SCREEN_ON_RUNTIME", tsMs = nowMs)
    }

    fun userPresent(context: Context, nowMs: Long = System.currentTimeMillis()) {
        GuardianDatabase(context).logTechnical("UNLOCK", tsMs = nowMs)
    }
}
