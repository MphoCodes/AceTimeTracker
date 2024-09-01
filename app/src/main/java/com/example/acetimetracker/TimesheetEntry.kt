package com.example.acetimetracker

data class TimesheetEntry(
    val id: String = "", // Unique identifier for the entry
    val userId: String = "", // User ID of the entry owner
    val category: String = "",
    val startDate: String = "",
    val startTime: String = "",
    val endDate: String = "",
    val endTime: String = "",
    val timeSpent: Long = 0, // Represents the time spent on the timesheet in milliseconds
    val imageUrl: String = "",
    val description: String = "" // Description of the timesheet entry
) {
    // Default no-argument constructor
    constructor() : this("", "", "", "", "", "", "", 0, "", "")
}
