package com.bigcorps.guardian.core

import android.content.Context

class PrivatePreferences(context: Context) {
    private val prefs = context.getSharedPreferences("guardian_private_apps", Context.MODE_PRIVATE)

    fun packages(): Set<String> = prefs.getStringSet(KEY_PACKAGES, emptySet())?.toSet() ?: emptySet()

    fun isPrivate(packageName: String): Boolean = packages().contains(packageName)

    fun setPrivate(packageName: String, privateValue: Boolean) {
        val current = packages().toMutableSet()
        if (privateValue) current += packageName else current -= packageName
        prefs.edit().putStringSet(KEY_PACKAGES, current).apply()
    }

    fun setupComplete(): Boolean = prefs.getBoolean("setup_complete", false)
    fun markSetupComplete() = prefs.edit().putBoolean("setup_complete", true).apply()

    companion object {
        private const val KEY_PACKAGES = "packages"
    }
}

class CollectorStateStore(context: Context) {
    private val prefs = context.getSharedPreferences("guardian_collector_state", Context.MODE_PRIVATE)

    fun cursorMs(): Long = prefs.getLong("cursor_ms", 0L)
    fun setCursorMs(value: Long) = prefs.edit().putLong("cursor_ms", value).apply()

    fun trackingStartMs(): Long = prefs.getLong("tracking_start_ms", 0L)
    fun markTrackingStartIfMissing(nowMs: Long = System.currentTimeMillis()) {
        if (trackingStartMs() <= 0L) prefs.edit().putLong("tracking_start_ms", nowMs).apply()
    }

    fun usagePermissionWasActive(): Boolean = prefs.getBoolean("usage_permission_active", false)
    fun setUsagePermissionActive(active: Boolean) = prefs.edit().putBoolean("usage_permission_active", active).apply()
    fun lastPermissionBaselineMs(): Long = prefs.getLong("permission_baseline_ms", 0L)
    fun setLastPermissionBaselineMs(value: Long) = prefs.edit().putLong("permission_baseline_ms", value).apply()

    fun openState(): OpenState? {
        val typeName = prefs.getString("open_type", null) ?: return null
        val start = prefs.getLong("open_start", 0L)
        if (start <= 0) return null
        val type = runCatching { IntervalType.valueOf(typeName) }.getOrNull() ?: return null
        return OpenState(
            type = type,
            startMs = start,
            packageName = if (type == IntervalType.APP) prefs.getString("open_package", null) else null,
            appLabel = if (type == IntervalType.APP) prefs.getString("open_label", null) else null
        )
    }

    fun setOpenState(state: OpenState?) {
        val edit = prefs.edit().clearOpenStateKeys()
        if (state != null) {
            edit.putString("open_type", state.type.name)
            edit.putLong("open_start", state.startMs)
            if (state.type == IntervalType.APP) {
                state.packageName?.let { edit.putString("open_package", it) }
                state.appLabel?.let { edit.putString("open_label", it) }
            }
        }
        edit.apply()
    }

    fun privateOverrideStartMs(): Long = prefs.getLong("private_override_start", 0L)
    fun setPrivateOverrideStartMs(value: Long) = prefs.edit().putLong("private_override_start", value).apply()
    fun clearPrivateOverride() = prefs.edit().remove("private_override_start").apply()

    private fun android.content.SharedPreferences.Editor.clearOpenStateKeys(): android.content.SharedPreferences.Editor {
        return remove("open_type").remove("open_start").remove("open_package").remove("open_label")
    }
}
