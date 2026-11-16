package com.mphocodes.acetimetracker.ui.viewmodel

import com.mphocodes.acetimetracker.data.model.Category

object CategoryValidation {
    fun error(category: Category, existing: List<Category>): String? {
        val name = category.name.trim()
        if (name.isEmpty()) return "Please enter a category name"
        val values = listOfNotNull(category.goal, category.minGoal, category.maxGoal)
        if (values.any { !it.isFinite() || it < 0f }) return "Goals must be finite, non-negative hours"
        val min = category.minGoal
        val max = category.maxGoal
        val goal = category.goal
        if (min != null && max != null && min > max) return "Minimum goal can't be more than the maximum"
        if (goal != null && ((min != null && goal < min) || (max != null && goal > max))) {
            return "Goal must be within the minimum and maximum"
        }
        if (existing.any {
            it.documentId != category.documentId && it.name.trim().equals(name, ignoreCase = true)
        }) return "A category named '$name' already exists"
        return null
    }

    /** Non-empty invalid text must not silently turn into an absent optional goal. */
    fun goalInputError(vararg values: String): String? =
        if (values.any { value ->
            value.isNotBlank() && (value.toFloatOrNull()?.let { it.isFinite() && it >= 0f } != true)
        }) "Enter valid, non-negative hours or leave the field empty." else null
}
