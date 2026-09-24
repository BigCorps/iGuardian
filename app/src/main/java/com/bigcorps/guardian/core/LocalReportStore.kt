package com.bigcorps.guardian.core

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class LocalReportStore(private val context: Context) {
    private val root = File(context.filesDir, "reports")

    fun writeDaysIntersecting(startMs: Long, endMs: Long) {
        if (endMs <= startMs) return
        val calendar = Calendar.getInstance().apply {
            timeInMillis = startMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        var days = 0
        while (calendar.timeInMillis < endMs && days < MAX_DAYS_PER_PASS) {
            val dayStart = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            val dayEnd = minOf(calendar.timeInMillis, endMs)
            writeDay(dayStart, dayEnd)
            days++
        }
    }

    fun writeToday(nowMs: Long = System.currentTimeMillis()) {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = nowMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        writeDay(calendar.timeInMillis, nowMs)
    }

    fun reportCount(): Int = root.walkTopDown().count { it.isFile && it.extension.equals("json", true) }

    fun totalBytes(): Long = root.walkTopDown().filter { it.isFile }.sumOf { it.length() }

    private fun writeDay(dayStartMs: Long, dayEndMs: Long) {
        val date = Date(dayStartMs)
        val year = SimpleDateFormat("yyyy", Locale.US).format(date)
        val month = SimpleDateFormat("MM", Locale.US).format(date)
        val day = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)
        val directory = File(File(root, year), month)
        if (!directory.exists()) directory.mkdirs()
        val target = File(directory, "$day.json")
        val temp = File(directory, "$day.json.tmp")
        val payload = ReportGenerator(context).periodJson(dayStartMs, dayEndMs).toString(2)
        temp.writeText(payload, Charsets.UTF_8)
        if (target.exists()) target.delete()
        if (!temp.renameTo(target)) {
            target.writeText(payload, Charsets.UTF_8)
            temp.delete()
        }
    }

    companion object {
        private const val MAX_DAYS_PER_PASS = 8
    }
}
