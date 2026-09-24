package com.bigcorps.guardian.core

import android.content.Context

object GuardianPrivacyOverride {
    fun start(
        context: Context,
        nowMs: Long = System.currentTimeMillis()
    ) {
        val state = CollectorStateStore(context)

        // Ignore duplicate background signals.
        if (state.privateOverrideStartMs() > 0L) return

        val db = GuardianDatabase(context)
        val collector = UsageCollector(context)

        // First capture only the owner's history up to the switch boundary.
        collector.collect(nowMs)
        collector.closeOpen(nowMs)

        state.setPrivateOverrideStartMs(nowMs)
        state.setOpenState(
            OpenState(
                type = IntervalType.PRIVATE,
                startMs = nowMs
            )
        )

        db.logTechnical("PRIVATE_STARTED", tsMs = nowMs)
    }

    fun end(
        context: Context,
        nowMs: Long = System.currentTimeMillis()
    ) {
        val state = CollectorStateStore(context)
        val overrideStart = state.privateOverrideStartMs()

        if (overrideStart <= 0L) return

        val db = GuardianDatabase(context)
        val current = state.openState()

        if (current?.type == IntervalType.PRIVATE) {
            UsageCollector(context).closeOpen(nowMs)
        } else if (nowMs > overrideStart) {
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

        // Critical privacy boundary: do not replay UsageStats events generated
        // while another Android user/profile was active.
        if (nowMs > state.cursorMs()) {
            state.setCursorMs(nowMs)
        }

        LocalReportStore(context).writeToday(nowMs)
        db.logTechnical("PRIVATE_ENDED", tsMs = nowMs)
    }

    fun closePendingIfAny(context: Context) {
        if (CollectorStateStore(context).privateOverrideStartMs() > 0L) {
            end(context)
        }
    }
}
