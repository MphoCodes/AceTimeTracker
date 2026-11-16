package com.mphocodes.acetimetracker.data.model

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
    val description: String = "",
    /** IANA zone the clock fields were recorded in; blank for records predating zone capture. */
    val zoneId: String = ""
)
