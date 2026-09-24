package com.bigcorps.guardian.core

import android.content.Context

object PrivacyRepair {
    private const val PREFS = "guardian_privacy_repair"
    private const val KEY_VERSION = "classifier_repair_version"

    fun runIfNeeded(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val applied = prefs.getInt(KEY_VERSION, 0)

        if (applied >= PrivacyClassifier.CLASSIFIER_VERSION) {
            return 0
        }

        val db = GuardianDatabase(context)
        val repaired = db.repairSensitiveAppRows()

        // Generated reports are derived from SQLite. Remove them after a privacy
        // repair so an older report can never keep an app name/package that is
        // now classified as PRIVATE.
        LocalReportStore(context).clearGeneratedReports()
        LocalReportStore(context).writeToday()

        db.logTechnical(
            "PRIVACY_REPAIR",
            "v=${PrivacyClassifier.CLASSIFIER_VERSION};rows=$repaired"
        )

        prefs.edit()
            .putInt(KEY_VERSION, PrivacyClassifier.CLASSIFIER_VERSION)
            .apply()

        return repaired
    }

    fun appliedVersion(context: Context): Int {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_VERSION, 0)
    }
}
