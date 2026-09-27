package com.bigcorps.guardian.web

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager

data class BrowserWebAccessStatus(
    val managerReported: Boolean,
    val secureSettingReported: Boolean,
    val recentHeartbeat: Boolean,
    val enabled: Boolean,
    val alive: Boolean
)

object BrowserWebAccess {
    private const val HEARTBEAT_ALIVE_MS = 20_000L

    fun status(
        context: Context,
        nowMs: Long = System.currentTimeMillis()
    ): BrowserWebAccessStatus {
        val managerReported = managerReported(context)
        val secureSettingReported = secureSettingReported(context)
        val heartbeat = BrowserWebPreferences(context).lastHeartbeatMs()
        val recentHeartbeat =
            heartbeat > 0L &&
                nowMs >= heartbeat &&
                nowMs - heartbeat <= HEARTBEAT_ALIVE_MS

        return BrowserWebAccessStatus(
            managerReported,
            secureSettingReported,
            recentHeartbeat,
            managerReported || secureSettingReported || recentHeartbeat,
            recentHeartbeat
        )
    }

    fun isEnabled(context: Context): Boolean = status(context).enabled

    private fun managerReported(context: Context): Boolean {
        val manager =
            context.getSystemService(Context.ACCESSIBILITY_SERVICE) as?
                AccessibilityManager ?: return false
        val expected =
            ComponentName(context, BrowserAccessibilityService::class.java)

        return runCatching {
            manager.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK
            ).any { item ->
                item.resolveInfo?.serviceInfo?.let { info ->
                    sameComponent(
                        expected,
                        ComponentName(info.packageName, info.name)
                    )
                } ?: false
            }
        }.getOrDefault(false)
    }

    private fun secureSettingReported(context: Context): Boolean {
        val accessibilityEnabled =
            runCatching {
                Settings.Secure.getInt(
                    context.contentResolver,
                    Settings.Secure.ACCESSIBILITY_ENABLED,
                    0
                ) == 1
            }.getOrDefault(false)

        if (!accessibilityEnabled) return false

        val raw =
            runCatching {
                Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                )
            }.getOrNull().orEmpty()

        val expected =
            ComponentName(context, BrowserAccessibilityService::class.java)

        return raw.split(':')
            .mapNotNull { ComponentName.unflattenFromString(it.trim()) }
            .any { sameComponent(expected, it) }
    }

    private fun sameComponent(
        expected: ComponentName,
        actual: ComponentName
    ): Boolean {
        if (expected.packageName != actual.packageName) return false

        fun normalized(component: ComponentName): String {
            val name = component.className
            return if (name.startsWith(".")) component.packageName + name else name
        }

        return normalized(expected) == normalized(actual)
    }
}
