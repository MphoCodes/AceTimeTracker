package com.example.acetimetracker.util

import java.text.SimpleDateFormat
import java.util.Locale

object TimeUtils {
    private const val TIME_FORMAT = "HH:mm"

    fun calculateTimeDifference(startTime: String, endTime: String): String {
        val timeFormat = SimpleDateFormat(TIME_FORMAT, Locale.getDefault())
        return try {
            val startDate = timeFormat.parse(startTime) ?: return "00:00"
            val endDate = timeFormat.parse(endTime) ?: return "00:00"
            val difference = endDate.time - startDate.time
            val hours = difference / (1000 * 60 * 60)
            val minutes = (difference % (1000 * 60 * 60)) / (1000 * 60)
            String.format(Locale.getDefault(), "%02d:%02d", hours, minutes)
        } catch (e: Exception) {
            "00:00"
        }
    }

    fun calculateTimeSpentMillis(startTime: String, endTime: String): Long {
        val timeFormat = SimpleDateFormat(TIME_FORMAT, Locale.getDefault())
        return try {
            val startDate = timeFormat.parse(startTime) ?: return 0L
            val endDate = timeFormat.parse(endTime) ?: return 0L
            endDate.time - startDate.time
        } catch (e: Exception) {
            0L
        }
    }

    fun formatDuration(timeInMillis: Long): String {
        val hours = timeInMillis / (1000 * 60 * 60)
        val minutes = (timeInMillis % (1000 * 60 * 60)) / (1000 * 60)
        val seconds = (timeInMillis % (1000 * 60)) / 1000
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun isValidDate(date: String): Boolean {
        return Regex("^\\d{4}-\\d{2}-\\d{2}\$").matches(date)
    }

    fun isValidTime(time: String): Boolean {
        return Regex("^\\d{2}:\\d{2}\$").matches(time)
    }
}
