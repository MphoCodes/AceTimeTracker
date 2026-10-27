package com.example.acetimetracker.util

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object TimeUtils {
    private const val TIME_FORMAT = "HH:mm"
    private const val DATE_FORMAT = "yyyy-MM-dd"
    private const val DATE_TIME_FORMAT = "$DATE_FORMAT $TIME_FORMAT"

    private fun strictFormat(pattern: String) =
        SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }

    fun calculateTimeDifference(startTime: String, endTime: String): String {
        val difference = calculateTimeSpentMillis(startTime, endTime)
        if (difference <= 0L) return "00:00"
        val hours = difference / (1000 * 60 * 60)
        val minutes = (difference % (1000 * 60 * 60)) / (1000 * 60)
        return String.format(Locale.US, "%02d:%02d", hours, minutes)
    }

    /** Duration between two same-day clock times. Prefer the date-aware overload. */
    fun calculateTimeSpentMillis(startTime: String, endTime: String): Long {
        val timeFormat = strictFormat(TIME_FORMAT)
        return try {
            val startDate = timeFormat.parse(startTime) ?: return 0L
            val endDate = timeFormat.parse(endTime) ?: return 0L
            endDate.time - startDate.time
        } catch (e: ParseException) {
            0L
        }
    }

    /**
     * Duration between two local date-times. Returns a negative value when the end is before the
     * start and 0 when either side cannot be parsed, so callers can reject invalid ranges.
     */
    fun calculateTimeSpentMillis(startDate: String, startTime: String, endDate: String, endTime: String): Long {
        val start = parseDateTime(startDate, startTime) ?: return 0L
        val end = parseDateTime(endDate, endTime) ?: return 0L
        return end.time - start.time
    }

    fun parseDateTime(date: String, time: String): Date? {
        if (!isValidDate(date) || !isValidTime(time)) return null
        return try {
            strictFormat(DATE_TIME_FORMAT).parse("$date $time")
        } catch (e: ParseException) {
            null
        }
    }

    fun formatDate(date: Date): String = strictFormat(DATE_FORMAT).format(date)

    fun formatTime(date: Date): String = strictFormat(TIME_FORMAT).format(date)

    fun today(): String = formatDate(Date())

    /** First day (Monday) of the current week as yyyy-MM-dd. */
    fun startOfWeek(now: Date = Date()): String {
        val cal = Calendar.getInstance().apply {
            time = now
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val offset = (cal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
        cal.add(Calendar.DAY_OF_MONTH, -offset)
        return formatDate(cal.time)
    }

    fun formatDuration(timeInMillis: Long): String {
        val safe = timeInMillis.coerceAtLeast(0L)
        val hours = safe / (1000 * 60 * 60)
        val minutes = (safe % (1000 * 60 * 60)) / (1000 * 60)
        val seconds = (safe % (1000 * 60)) / 1000
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    /** Human-friendly duration such as "2h 15m", "45m" or "0m". */
    fun formatDurationShort(timeInMillis: Long): String {
        val totalMinutes = timeInMillis.coerceAtLeast(0L) / (1000 * 60)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            else -> "${minutes}m"
        }
    }

    fun millisToHours(timeInMillis: Long): Float = timeInMillis / (1000f * 60f * 60f)

    fun isValidDate(date: String): Boolean {
        if (!Regex("^\\d{4}-\\d{2}-\\d{2}\$").matches(date)) return false
        return try {
            strictFormat(DATE_FORMAT).parse(date) != null
        } catch (e: ParseException) {
            false
        }
    }

    fun isValidTime(time: String): Boolean {
        if (!Regex("^\\d{2}:\\d{2}\$").matches(time)) return false
        return try {
            strictFormat(TIME_FORMAT).parse(time) != null
        } catch (e: ParseException) {
            false
        }
    }
}
