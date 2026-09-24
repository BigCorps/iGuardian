package com.bigcorps.guardian.core

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class GuardianDatabase(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE intervals (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                start_ms INTEGER NOT NULL,
                end_ms INTEGER NOT NULL,
                type TEXT NOT NULL,
                package_name TEXT NULL,
                app_label TEXT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_intervals_time ON intervals(start_ms, end_ms)")
        db.execSQL(
            """
            CREATE TABLE technical_events (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                ts_ms INTEGER NOT NULL,
                code TEXT NOT NULL,
                value TEXT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_technical_time ON technical_events(ts_ms)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // First schema. Future migrations must preserve local history where possible.
    }

    @Synchronized
    fun insertInterval(interval: TimelineInterval): Long {
        if (interval.endMs <= interval.startMs) return -1
        val identity = StorageSanitizer.identityFor(interval.type, interval.packageName, interval.appLabel)
        val values = ContentValues().apply {
            put("start_ms", interval.startMs)
            put("end_ms", interval.endMs)
            put("type", interval.type.name)
            if (identity.packageName == null) putNull("package_name") else put("package_name", identity.packageName)
            if (identity.appLabel == null) putNull("app_label") else put("app_label", identity.appLabel)
        }
        return writableDatabase.insert("intervals", null, values)
    }

    @Synchronized
    fun logTechnical(code: String, value: String? = null, tsMs: Long = System.currentTimeMillis()) {
        val safeCode = code.take(80).replace(Regex("[^A-Z0-9_-]"), "_")
        val safeValue = value?.take(120)?.replace(Regex("https?://\\S+"), "[redacted]")
        val values = ContentValues().apply {
            put("ts_ms", tsMs)
            put("code", safeCode)
            if (safeValue == null) putNull("value") else put("value", safeValue)
        }
        writableDatabase.insert("technical_events", null, values)
    }

    fun intervalsBetween(startMs: Long, endMs: Long): List<TimelineInterval> {
        val result = mutableListOf<TimelineInterval>()
        readableDatabase.query(
            "intervals",
            arrayOf("id", "start_ms", "end_ms", "type", "package_name", "app_label"),
            "end_ms > ? AND start_ms < ?",
            arrayOf(startMs.toString(), endMs.toString()),
            null,
            null,
            "start_ms ASC"
        ).use { c ->
            while (c.moveToNext()) {
                val type = runCatching { IntervalType.valueOf(c.getString(3)) }.getOrDefault(IntervalType.PRIVATE)
                result += TimelineInterval(
                    id = c.getLong(0),
                    startMs = c.getLong(1),
                    endMs = c.getLong(2),
                    type = type,
                    packageName = if (c.isNull(4)) null else c.getString(4),
                    appLabel = if (c.isNull(5)) null else c.getString(5)
                )
            }
        }
        return result
    }

    fun technicalCount(code: String, startMs: Long, endMs: Long): Int {
        readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM technical_events WHERE code = ? AND ts_ms >= ? AND ts_ms < ?",
            arrayOf(code, startMs.toString(), endMs.toString())
        ).use { c ->
            return if (c.moveToFirst()) c.getInt(0) else 0
        }
    }

    data class TechnicalEvent(val code: String, val value: String?, val tsMs: Long)

    fun recentTechnical(limit: Int = 40): List<TechnicalEvent> {
        val result = mutableListOf<TechnicalEvent>()
        readableDatabase.query(
            "technical_events",
            arrayOf("code", "value", "ts_ms"),
            null,
            null,
            null,
            null,
            "ts_ms DESC",
            limit.coerceIn(1, 100).toString()
        ).use { c ->
            while (c.moveToNext()) {
                result += TechnicalEvent(
                    code = c.getString(0),
                    value = if (c.isNull(1)) null else c.getString(1),
                    tsMs = c.getLong(2)
                )
            }
        }
        return result
    }

    fun intervalCounts(): Map<IntervalType, Int> {
        val result = IntervalType.entries.associateWith { 0 }.toMutableMap()
        readableDatabase.rawQuery("SELECT type, COUNT(*) FROM intervals GROUP BY type", null).use { c ->
            while (c.moveToNext()) {
                runCatching { IntervalType.valueOf(c.getString(0)) }.getOrNull()?.let { result[it] = c.getInt(1) }
            }
        }
        return result
    }

    fun databaseSizeBytes(): Long {
        val path = readableDatabase.path ?: return 0
        return runCatching { java.io.File(path).length() }.getOrDefault(0L)
    }

    companion object {
        private const val DB_NAME = "guardian.db"
        private const val DB_VERSION = 1
    }
}
