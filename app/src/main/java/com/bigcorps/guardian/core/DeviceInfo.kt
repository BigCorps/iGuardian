package com.bigcorps.guardian.core

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.StatFs
import org.json.JSONObject

object DeviceInfo {
    fun asJson(context: Context): JSONObject {
        val activityManager = context.getSystemService(ActivityManager::class.java)
        val memoryInfo = ActivityManager.MemoryInfo().also { activityManager?.getMemoryInfo(it) }
        val statFs = StatFs(context.filesDir.absolutePath)
        val prefs = context.getSharedPreferences("guardian_user", Context.MODE_PRIVATE)
        val deviceName = prefs.getString("device_name", null)?.takeIf { it.isNotBlank() }
            ?: "${Build.MANUFACTURER} ${Build.MODEL}".trim()

        return JSONObject().apply {
            put("name", deviceName)
            put("manufacturer", Build.MANUFACTURER)
            put("brand", Build.BRAND)
            put("model", Build.MODEL)
            put("device", Build.DEVICE)
            put("product", Build.PRODUCT)
            put("hardware", Build.HARDWARE)
            put("android_version", Build.VERSION.RELEASE)
            put("api_level", Build.VERSION.SDK_INT)
            put("ram_total_mb", memoryInfo.totalMem / (1024L * 1024L))
            put("storage_total_mb", statFs.totalBytes / (1024L * 1024L))
            put("storage_free_mb", statFs.availableBytes / (1024L * 1024L))
        }
    }
}
