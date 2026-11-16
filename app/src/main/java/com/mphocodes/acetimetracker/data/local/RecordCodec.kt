package com.mphocodes.acetimetracker.data.local

import com.mphocodes.acetimetracker.data.model.Category
import com.mphocodes.acetimetracker.data.model.TimesheetEntry
import org.json.JSONObject

internal object RecordCodec {
    const val ENTRIES = "entries"
    const val CATEGORIES = "categories"
    fun encode(entry: TimesheetEntry): String = JSONObject().apply {
        put("id", entry.id); put("userId", entry.userId); put("category", entry.category)
        put("startDate", entry.startDate); put("startTime", entry.startTime)
        put("endDate", entry.endDate); put("endTime", entry.endTime)
        put("timeSpent", entry.timeSpent); put("imageUrl", entry.imageUrl); put("description", entry.description)
        put("zoneId", entry.zoneId)
    }.toString()
    fun entry(payload: String): TimesheetEntry = JSONObject(payload).let {
        TimesheetEntry(it.optString("id"), it.optString("userId"), it.optString("category"),
            it.optString("startDate"), it.optString("startTime"), it.optString("endDate"),
            it.optString("endTime"), it.optLong("timeSpent"), it.optString("imageUrl"), it.optString("description"),
            it.optString("zoneId"))
    }
    fun encode(category: Category): String = JSONObject().apply {
        put("documentId", category.documentId); put("name", category.name); put("description", category.description)
        put("minGoal", category.minGoal ?: JSONObject.NULL); put("maxGoal", category.maxGoal ?: JSONObject.NULL)
        put("goal", category.goal ?: JSONObject.NULL); put("colorHex", category.colorHex)
    }.toString()
    fun category(payload: String): Category = JSONObject(payload).let {
        Category(it.optString("documentId"), it.optString("name"), it.optString("description"),
            it.floatOrNull("minGoal"), it.floatOrNull("maxGoal"), it.floatOrNull("goal"), it.optString("colorHex", "#DC2626"))
    }
    private fun JSONObject.floatOrNull(key: String): Float? = if (isNull(key)) null else optDouble(key).toFloat().takeIf { it.isFinite() }
    fun remoteCategory(category: Category): Map<String, Any?> = mapOf(
        "name" to category.name, "description" to category.description, "minGoal" to category.minGoal,
        "maxGoal" to category.maxGoal, "goal" to category.goal, "colorHex" to category.colorHex)
}
