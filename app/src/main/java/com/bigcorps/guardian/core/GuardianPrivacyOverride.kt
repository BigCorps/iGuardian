package com.bigcorps.guardian.core

import android.content.Context

object GuardianPrivacyOverride {
    fun start(context: Context, nowMs: Long = System.currentTimeMillis()) {
        val state = CollectorStateStore(context)
        val db = GuardianDatabase(context)
        val collector = UsageCollector(context)
        collector.closeOpen(nowMs)
        state.setPrivateOverrideStartMs(nowMs)
        state.setOpenState(OpenState(IntervalType.PRIVATE, nowMs))
        db.logTechnical("PRIVATE_STARTED", tsMs = nowMs)
    }

    fun end(context: Context, nowMs: Long = System.currentTimeMillis()) {
        val state = CollectorStateStore(context)
        val db = GuardianDatabase(context)
        val overrideStart = state.privateOverrideStartMs()
        val current = state.openState()

        if (current?.type == IntervalType.PRIVATE) {
            UsageCollector(context).closeOpen(nowMs)
        } else if (overrideStart > 0L && nowMs > overrideStart) {
            db.insertInterval(
                TimelineInterval(
                    startMs = overrideStart,
                    endMs = nowMs,
                    type = IntervalType.PRIVATE
                )
            )
        }

        state.clearPrivateOverride()
        state.setOpenState(null)
        db.logTechnical("PRIVATE_ENDED", tsMs = nowMs)
    }

    fun closePendingIfAny(context: Context) {
        if (CollectorStateStore(context).privateOverrideStartMs() > 0L) {
            end(context)
        }
    }
}
