package com.bigcorps.guardian.core

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

class SystemSurfaceClassifier(private val context: Context) {
    private val exactSystemPackages = setOf(
        "com.android.systemui",
        "com.android.intentresolver",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.miui.global.packageinstaller",
        "com.google.android.gms",
        "com.miui.securitycenter",
        "com.miui.securitycore",
        "com.android.documentsui",
        "com.google.android.documentsui"
    )

    private val homePackages: Set<String> by lazy {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }

        val pm = context.packageManager
        val resolved =
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                pm.queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(
                        PackageManager.MATCH_DEFAULT_ONLY.toLong()
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(
                    intent,
                    PackageManager.MATCH_DEFAULT_ONLY
                )
            }

        resolved.mapNotNull { it.activityInfo?.packageName }.toSet()
    }

    fun isSystemSurface(packageName: String): Boolean {
        if (packageName in exactSystemPackages) return true
        if (packageName in homePackages) return true

        // OEM package-installer/document-picker package names vary.
        // Only apply the generic rule when Android marks the package as
        // a system or updated-system application.
        if (!isSystemApplication(packageName)) return false

        val normalized = packageName.lowercase()
        return normalized.contains("packageinstaller") ||
            normalized.endsWith(".documentsui")
    }

    private fun isSystemApplication(packageName: String): Boolean = try {
        val pm = context.packageManager
        val info =
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                pm.getApplicationInfo(
                    packageName,
                    PackageManager.ApplicationInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }

        (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
            (info.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
    } catch (_: Exception) {
        false
    }
}
