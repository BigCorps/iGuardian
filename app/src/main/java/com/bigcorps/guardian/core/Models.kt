package com.bigcorps.guardian.core

enum class IntervalType { APP, PRIVATE, SCREEN_OFF, SYSTEM, ANONYMOUS_BROWSER }

data class TimelineInterval(
    val id: Long = 0,
    val startMs: Long,
    val endMs: Long,
    val type: IntervalType,
    val packageName: String? = null,
    val appLabel: String? = null
)

data class OpenState(
    val type: IntervalType,
    val startMs: Long,
    val packageName: String? = null,
    val appLabel: String? = null
)

data class SanitizedIdentity(val packageName: String?, val appLabel: String?)

object StorageSanitizer {
    fun identityFor(type: IntervalType, packageName: String?, appLabel: String?): SanitizedIdentity =
        if (type == IntervalType.APP) {
            SanitizedIdentity(packageName?.takeIf { it.isNotBlank() }, appLabel?.takeIf { it.isNotBlank() })
        } else {
            SanitizedIdentity(null, null)
        }
}
