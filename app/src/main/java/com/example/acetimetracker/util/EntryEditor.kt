package com.example.acetimetracker.util

import com.example.acetimetracker.data.model.TimesheetEntry
import java.util.UUID

/** Validates edits without turning paused/focus time into wall-clock duration. */
object EntryEditor {
    fun build(original: TimesheetEntry?, userId: String, category: String,
              startDate: String, startTime: String, endDate: String, endTime: String,
              description: String, imageUrl: String): TimesheetEntry {
        require(userId.isNotBlank() && (original == null || original.userId == userId)) { "Sign in to the account that owns this entry" }
        require(category.isNotBlank() && category != "Select Category") { "Please select a category" }
        require(TimeUtils.isValidDate(startDate) && TimeUtils.isValidDate(endDate)) { "Enter valid dates (YYYY-MM-DD)" }
        require(TimeUtils.isValidTime(startTime) && TimeUtils.isValidTime(endTime)) { "Enter valid times (HH:MM)" }
        val unchangedRange = original != null && original.startDate == startDate && original.startTime == startTime && original.endDate == endDate && original.endTime == endTime
        val duration = if (unchangedRange) original.timeSpent else TimeUtils.calculateTimeSpentMillis(startDate, startTime, endDate, endTime)
        require(duration > 0) { "End time must be after the start time" }
        return TimesheetEntry(original?.id ?: UUID.randomUUID().toString(), userId, category.trim(), startDate, startTime, endDate, endTime, duration, imageUrl, description.trim())
    }
}
