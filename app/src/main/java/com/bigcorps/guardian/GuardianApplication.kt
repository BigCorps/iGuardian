package com.bigcorps.guardian

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import com.bigcorps.guardian.core.GuardianPrivacyOverride
import com.bigcorps.guardian.core.GuardianScheduler
import com.bigcorps.guardian.core.PrivacyRepair
import com.bigcorps.guardian.core.SchedulerStateStore
import com.bigcorps.guardian.core.SystemSignalRecorder

class GuardianApplication : Application() {
    private val userReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_USER_BACKGROUND ->
                    GuardianPrivacyOverride.start(context.applicationContext)

                Intent.ACTION_USER_FOREGROUND ->
                    GuardianPrivacyOverride.end(context.applicationContext)

                Intent.ACTION_SCREEN_OFF ->
                    if (Build.VERSION.SDK_INT < 28) {
                        SystemSignalRecorder.screenOff(context.applicationContext)
                    }

                Intent.ACTION_SCREEN_ON ->
                    if (Build.VERSION.SDK_INT < 28) {
                        SystemSignalRecorder.screenOn(context.applicationContext)
                    }

                Intent.ACTION_USER_PRESENT ->
                    if (Build.VERSION.SDK_INT < 28) {
                        SystemSignalRecorder.userPresent(context.applicationContext)
                    }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        PrivacyRepair.runIfNeeded(this)

        val schedulerState =
            SchedulerStateStore(this)

        val previousSchedulerLogic =
            schedulerState.logicVersion()

        schedulerState.ensureLogicVersion(
            GuardianScheduler.LOGIC_VERSION
        )

        if (
            previousSchedulerLogic !=
            GuardianScheduler.LOGIC_VERSION
        ) {
            GuardianScheduler
                .resetForLogicUpgrade(
                    this
                )
        } else {
            GuardianScheduler
                .recoverAfterProcessStart(
                    this
                )
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_USER_BACKGROUND)
            addAction(Intent.ACTION_USER_FOREGROUND)

            if (Build.VERSION.SDK_INT < 28) {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
            }
        }

        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(
                userReceiver,
                filter,
                "android.permission.MANAGE_USERS",
                null,
                Context.RECEIVER_EXPORTED
            )
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(
                userReceiver,
                filter,
                "android.permission.MANAGE_USERS",
                null
            )
        }
    }
}
