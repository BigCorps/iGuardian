package com.bigcorps.guardian.web

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.view.accessibility.AccessibilityManager

object BrowserWebAccess {
    fun isEnabled(
        context: Context
    ): Boolean {
        val manager =
            context.getSystemService(
                Context.ACCESSIBILITY_SERVICE
            ) as?
                AccessibilityManager
                ?: return false

        val expected =
            ComponentName(
                context,
                BrowserAccessibilityService::class.java
            )
                .flattenToString()

        return manager
            .getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK
            )
            .any {
                item ->
                item.resolveInfo
                    ?.serviceInfo
                    ?.let {
                        info ->
                        ComponentName(
                            info.packageName,
                            info.name
                        )
                            .flattenToString() ==
                            expected
                    }
                    ?: false
            }
    }
}
