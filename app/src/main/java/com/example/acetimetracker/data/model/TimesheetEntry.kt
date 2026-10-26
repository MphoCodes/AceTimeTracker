package com.example.acetimetracker.data.model

data class TimesheetEntry(
    val id: String = "",
    val userId: String = "",
    val category: String = "",
    val startDate: String = "",
    val startTime: String = "",
    val endDate: String = "",
    val endTime: String = "",
    val timeSpent: Long = 0,
    val imageUrl: String = "",
    val description: String = ""
)
