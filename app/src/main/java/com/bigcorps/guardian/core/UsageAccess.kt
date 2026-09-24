package com.bigcorps.guardian.core

import android.app.AppOpsManager
import android.content.Context
import android.os.Process

object UsageAccess {
    fun hasPermission(context: Context): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
