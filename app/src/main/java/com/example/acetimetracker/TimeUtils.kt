package com.example.acetimetracker

import java.text.SimpleDateFormat
import java.util.Locale

object TimeUtils {
    // Function to calculate time difference and return formatted string
    fun calculateTimeDifference(startTime: String, endTime: String): String {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        return try {
            val startDate = timeFormat.parse(startTime)
            val endDate = timeFormat.parse(endTime)

            // Calculate the difference in milliseconds
            val difference = endDate.time - startDate.time

            // Convert milliseconds to hours and minutes
            val hours = difference / (1000 * 60 * 60)
            val minutes = (difference % (1000 * 60 * 60)) / (1000 * 60)

            // Return formatted string
            String.format("%02d:%02d", hours, minutes)
        } catch (e: Exception) {
            e.printStackTrace()
            "Error calculating time"
        }
    }
}

