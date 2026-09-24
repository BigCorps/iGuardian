package com.bigcorps.guardian.core

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

class SystemSurfaceClassifier(private val context: Context) {
    private val exactSystemPackages = setOf(
        "com.android.systemui",
        "com.android.intentresolver",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller"
    )

    private val homePackages: Set<String> by lazy {
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
        val pm = context.packageManager
        val resolved = if (android.os.Build.VERSION.SDK_INT >= 33) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }
        resolved.mapNotNull { it.activityInfo?.packageName }.toSet()
    }

    fun isSystemSurface(packageName: String): Boolean =
        packageName in exactSystemPackages || packageName in homePackages
}
