package com.mphocodes.acetimetracker.data.repository

import android.content.Context
import com.mphocodes.acetimetracker.data.model.Category

/** Device-local favorites use stable category IDs and are isolated by the signed-in account. */
class FavoriteCategories(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences("favorite_categories", Context.MODE_PRIVATE)

    fun getIds(userId: String): Set<String> =
        if (userId.isBlank()) emptySet()
        else preferences.getStringSet("ids_$userId", emptySet()).orEmpty().toSet()

    fun toggle(userId: String, categoryId: String): Set<String> {
        val current = getIds(userId)
        if (userId.isBlank() || categoryId.isBlank()) return current
        val next = if (categoryId in current) current - categoryId else current + categoryId
        preferences.edit().putStringSet("ids_$userId", next).apply()
        return next
    }

    fun sort(userId: String, categories: List<Category>): List<Category> {
        val ids = getIds(userId)
        return categories.sortedByDescending { it.documentId in ids }
    }
}
