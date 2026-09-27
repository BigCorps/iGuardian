package com.bigcorps.guardian.core

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.bigcorps.guardian.web.BrowserCatalog
import com.bigcorps.guardian.web.BrowserDomainSanitizer

class GuardianDatabase(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

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

        createBrowserSessionTable(
            db
        )
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        if (oldVersion < 3) {
            // 0.1.4 could run the manual collector and JobService concurrently.
            db.execSQL(
                """
                DELETE FROM intervals
                WHERE id NOT IN (
                    SELECT MIN(id)
                    FROM intervals
                    GROUP BY
                        start_ms,
                        end_ms,
                        type,
                        IFNULL(package_name, ''),
                        IFNULL(app_label, '')
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                DELETE FROM technical_events
                WHERE code = 'UNLOCK'
                  AND id NOT IN (
                    SELECT MIN(id)
                    FROM technical_events
                    WHERE code = 'UNLOCK'
                    GROUP BY ts_ms
                  )
                """.trimIndent()
            )
        }

        if (oldVersion < 4) {
            // Clean historical technical/system surfaces that older builds
            // had already stored as APP. Identity is removed as part of the
            // migration, matching current StorageSanitizer behavior.
            val knownSystemPackages = listOf(
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

            val placeholders =
                knownSystemPackages.joinToString(",") { "?" }

            val values = ContentValues().apply {
                put("type", IntervalType.SYSTEM.name)
                putNull("package_name")
                putNull("app_label")
            }

            db.update(
                "intervals",
                values,
                "type = ? AND package_name IN ($placeholders)",
                arrayOf(
                    IntervalType.APP.name,
                    *knownSystemPackages.toTypedArray()
                )
            )
        }

        if (oldVersion < 5) {
            val newlyKnownSystemPackages = listOf(
                "com.google.android.photopicker",
                "com.mi.appfinder"
            )

            val placeholders =
                newlyKnownSystemPackages.joinToString(",") { "?" }

            val values = ContentValues().apply {
                put("type", IntervalType.SYSTEM.name)
                putNull("package_name")
                putNull("app_label")
            }

            db.update(
                "intervals",
                values,
                "type = ? AND package_name IN ($placeholders)",
                arrayOf(
                    IntervalType.APP.name,
                    *newlyKnownSystemPackages.toTypedArray()
                )
            )
        }

        if (oldVersion < 6) {
            val newlyKnownSystemPackages = listOf(
                "com.android.providers.downloads.ui"
            )

            val placeholders =
                newlyKnownSystemPackages.joinToString(",") { "?" }

            val values = ContentValues().apply {
                put("type", IntervalType.SYSTEM.name)
                putNull("package_name")
                putNull("app_label")
            }

            db.update(
                "intervals",
                values,
                "type = ? AND package_name IN ($placeholders)",
                arrayOf(
                    IntervalType.APP.name,
                    *newlyKnownSystemPackages.toTypedArray()
                )
            )
        }

        if (oldVersion < 7) {
            val newlyKnownSystemPackages = listOf(
                "com.miui.android.fashiongallery"
            )

            val placeholders =
                newlyKnownSystemPackages.joinToString(",") { "?" }

            val values = ContentValues().apply {
                put("type", IntervalType.SYSTEM.name)
                putNull("package_name")
                putNull("app_label")
            }

            db.update(
                "intervals",
                values,
                "type = ? AND package_name IN ($placeholders)",
                arrayOf(
                    IntervalType.APP.name,
                    *newlyKnownSystemPackages.toTypedArray()
                )
            )
        }
        if (oldVersion < 8) {
            createBrowserSessionTable(
                db
            )
        }
    }

    @Synchronized
    fun insertInterval(interval: TimelineInterval): Long {
        if (interval.endMs <= interval.startMs) return -1

        val identity = StorageSanitizer.identityFor(
            interval.type,
            interval.packageName,
            interval.appLabel
        )

        val values = ContentValues().apply {
            put("start_ms", interval.startMs)
            put("end_ms", interval.endMs)
            put("type", interval.type.name)
            if (identity.packageName == null) putNull("package_name")
            else put("package_name", identity.packageName)

            if (identity.appLabel == null) putNull("app_label")
            else put("app_label", identity.appLabel)
        }

        return writableDatabase.insert("intervals", null, values)
    }

    @Synchronized
    fun repairSensitiveAppRows(): Int {
        val ids = mutableListOf<Long>()

        readableDatabase.query(
            "intervals",
            arrayOf("id", "package_name", "app_label"),
            "type = ?",
            arrayOf(IntervalType.APP.name),
            null,
            null,
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val pkg = if (cursor.isNull(1)) null else cursor.getString(1)
                val label = if (cursor.isNull(2)) null else cursor.getString(2)

                if (
                    !pkg.isNullOrBlank() &&
                    PrivacyClassifier.isAutomaticallyPrivate(pkg, label)
                ) {
                    ids += id
                }
            }
        }

        if (ids.isEmpty()) return 0

        val values = ContentValues().apply {
            put("type", IntervalType.PRIVATE.name)
            putNull("package_name")
            putNull("app_label")
        }

        var repaired = 0
        writableDatabase.beginTransaction()
        try {
            ids.forEach { id ->
                repaired += writableDatabase.update(
                    "intervals",
                    values,
                    "id = ?",
                    arrayOf(id.toString())
                )
            }
            writableDatabase.setTransactionSuccessful()
        } finally {
            writableDatabase.endTransaction()
        }

        return repaired
    }

    @Synchronized
    fun logTechnical(
        code: String,
        value: String? = null,
        tsMs: Long = System.currentTimeMillis()
    ) {
        val safeCode = code
            .take(80)
            .replace(Regex("[^A-Z0-9_-]"), "_")

        val safeValue = value
            ?.take(120)
            ?.replace(Regex("https?://\\S+"), "[redacted]")

        val values = ContentValues().apply {
            put("ts_ms", tsMs)
            put("code", safeCode)
            if (safeValue == null) putNull("value")
            else put("value", safeValue)
        }

        writableDatabase.insert("technical_events", null, values)
    }

    fun intervalsBetween(
        startMs: Long,
        endMs: Long
    ): List<TimelineInterval> {
        val result = mutableListOf<TimelineInterval>()

        readableDatabase.query(
            "intervals",
            arrayOf(
                "id",
                "start_ms",
                "end_ms",
                "type",
                "package_name",
                "app_label"
            ),
            "end_ms > ? AND start_ms < ?",
            arrayOf(startMs.toString(), endMs.toString()),
            null,
            null,
            "start_ms ASC"
        ).use { c ->
            while (c.moveToNext()) {
                val type = runCatching {
                    IntervalType.valueOf(c.getString(3))
                }.getOrDefault(IntervalType.PRIVATE)

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

    data class BrowserSession(
        val id: Long,
        val startMs: Long,
        val endMs: Long,
        val host: String,
        val browserPackage: String,
        val privateMode: Boolean
    )

    data class BrowserStorageAudit(
        val rows: Int,
        val normalRows: Int,
        val privateRows: Int,
        val distinctHosts: Int,
        val invalidHosts: Int,
        val unsupportedBrowsers: Int,
        val badDurations: Int
    )

    @Synchronized
    fun recordBrowserChunk(
        startMs: Long,
        endMs: Long,
        host: String,
        browserPackage: String,
        privateMode: Boolean
    ): Long {
        if (
            endMs <=
            startMs
        ) {
            return -1L
        }

        val safeHost =
            BrowserDomainSanitizer.hostFromRaw(
                host
            )
                ?: return -1L

        if (
            !BrowserCatalog.isSupported(
                browserPackage
            )
        ) {
            return -1L
        }

        val db =
            writableDatabase

        var lastId =
            -1L

        var lastEnd =
            0L

        var lastHost: String? =
            null

        var lastBrowser: String? =
            null

        var lastPrivate =
            false

        db.rawQuery(
            """
            SELECT
                id,
                end_ms,
                host,
                browser_package,
                private_mode
            FROM browser_sessions
            ORDER BY end_ms DESC, id DESC
            LIMIT 1
            """.trimIndent(),
            null
        ).use {
            cursor ->
            if (
                cursor.moveToFirst()
            ) {
                lastId =
                    cursor.getLong(
                        0
                    )

                lastEnd =
                    cursor.getLong(
                        1
                    )

                lastHost =
                    cursor.getString(
                        2
                    )

                lastBrowser =
                    cursor.getString(
                        3
                    )

                lastPrivate =
                    cursor.getInt(
                        4
                    ) !=
                        0
            }
        }

        val canMerge =
            lastId >
                0L &&
                lastHost ==
                    safeHost &&
                lastBrowser ==
                    browserPackage &&
                lastPrivate ==
                    privateMode &&
                startMs <=
                    lastEnd +
                        BROWSER_MERGE_GAP_MS &&
                endMs >=
                    lastEnd -
                        1_000L

        if (
            canMerge
        ) {
            val values =
                ContentValues().apply {
                    put(
                        "end_ms",
                        maxOf(
                            lastEnd,
                            endMs
                        )
                    )
                }

            db.update(
                "browser_sessions",
                values,
                "id = ?",
                arrayOf(
                    lastId.toString()
                )
            )

            return lastId
        }

        val values =
            ContentValues().apply {
                put(
                    "start_ms",
                    startMs
                )

                put(
                    "end_ms",
                    endMs
                )

                put(
                    "host",
                    safeHost
                )

                put(
                    "browser_package",
                    browserPackage
                )

                put(
                    "private_mode",
                    if (
                        privateMode
                    ) {
                        1
                    } else {
                        0
                    }
                )
            }

        return db.insert(
            "browser_sessions",
            null,
            values
        )
    }

    fun browserSessionsBetween(
        startMs: Long,
        endMs: Long
    ): List<BrowserSession> {
        val result =
            mutableListOf<
                BrowserSession
                >()

        readableDatabase.query(
            "browser_sessions",
            arrayOf(
                "id",
                "start_ms",
                "end_ms",
                "host",
                "browser_package",
                "private_mode"
            ),
            "end_ms > ? AND start_ms < ?",
            arrayOf(
                startMs.toString(),
                endMs.toString()
            ),
            null,
            null,
            "start_ms ASC"
        ).use {
            cursor ->
            while (
                cursor.moveToNext()
            ) {
                result +=
                    BrowserSession(
                        id =
                            cursor.getLong(
                                0
                            ),
                        startMs =
                            cursor.getLong(
                                1
                            ),
                        endMs =
                            cursor.getLong(
                                2
                            ),
                        host =
                            cursor.getString(
                                3
                            ),
                        browserPackage =
                            cursor.getString(
                                4
                            ),
                        privateMode =
                            cursor.getInt(
                                5
                            ) !=
                                0
                    )
            }
        }

        return result
    }

    fun browserStorageAudit(): BrowserStorageAudit {
        var rows =
            0

        var normalRows =
            0

        var privateRows =
            0

        var invalidHosts =
            0

        var unsupportedBrowsers =
            0

        var badDurations =
            0

        val hosts =
            mutableSetOf<
                String
                >()

        readableDatabase.query(
            "browser_sessions",
            arrayOf(
                "start_ms",
                "end_ms",
                "host",
                "browser_package",
                "private_mode"
            ),
            null,
            null,
            null,
            null,
            null
        ).use {
            cursor ->
            while (
                cursor.moveToNext()
            ) {
                rows +=
                    1

                val start =
                    cursor.getLong(
                        0
                    )

                val end =
                    cursor.getLong(
                        1
                    )

                val host =
                    cursor.getString(
                        2
                    )

                val browser =
                    cursor.getString(
                        3
                    )

                val privateMode =
                    cursor.getInt(
                        4
                    ) !=
                        0

                if (
                    privateMode
                ) {
                    privateRows +=
                        1
                } else {
                    normalRows +=
                        1
                }

                hosts +=
                    host

                if (
                    !BrowserDomainSanitizer.isSanitizedHost(
                        host
                    )
                ) {
                    invalidHosts +=
                        1
                }

                if (
                    !BrowserCatalog.isSupported(
                        browser
                    )
                ) {
                    unsupportedBrowsers +=
                        1
                }

                if (
                    end <=
                    start
                ) {
                    badDurations +=
                        1
                }
            }
        }

        return BrowserStorageAudit(
            rows =
                rows,
            normalRows =
                normalRows,
            privateRows =
                privateRows,
            distinctHosts =
                hosts.size,
            invalidHosts =
                invalidHosts,
            unsupportedBrowsers =
                unsupportedBrowsers,
            badDurations =
                badDurations
        )
    }

    @Synchronized
    fun clearBrowserSessions() {
        writableDatabase.delete(
            "browser_sessions",
            null,
            null
        )
    }

    fun technicalCount(
        code: String,
        startMs: Long,
        endMs: Long
    ): Int {
        val countExpression =
            if (code == "UNLOCK") "COUNT(DISTINCT ts_ms)"
            else "COUNT(*)"

        readableDatabase.rawQuery(
            """
            SELECT $countExpression
            FROM technical_events
            WHERE code = ? AND ts_ms >= ? AND ts_ms < ?
            """.trimIndent(),
            arrayOf(code, startMs.toString(), endMs.toString())
        ).use { c ->
            return if (c.moveToFirst()) c.getInt(0) else 0
        }
    }

    data class TechnicalEvent(
        val code: String,
        val value: String?,
        val tsMs: Long
    )

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

        readableDatabase.rawQuery(
            "SELECT type, COUNT(*) FROM intervals GROUP BY type",
            null
        ).use { c ->
            while (c.moveToNext()) {
                runCatching {
                    IntervalType.valueOf(c.getString(0))
                }.getOrNull()?.let { type ->
                    result[type] = c.getInt(1)
                }
            }
        }
        return result
    }

    fun databaseSizeBytes(): Long {
        val path = readableDatabase.path ?: return 0
        return runCatching { java.io.File(path).length() }.getOrDefault(0L)
    }

    private fun createBrowserSessionTable(
        db: SQLiteDatabase
    ) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS browser_sessions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                start_ms INTEGER NOT NULL,
                end_ms INTEGER NOT NULL,
                host TEXT NOT NULL,
                browser_package TEXT NOT NULL,
                private_mode INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )

        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_browser_sessions_time ON browser_sessions(start_ms, end_ms)"
        )

        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_browser_sessions_host ON browser_sessions(host, start_ms)"
        )
    }

    companion object {
        private const val DB_NAME = "guardian.db"
        private const val DB_VERSION = 8
        private const val BROWSER_MERGE_GAP_MS = 6_000L
    }
}
