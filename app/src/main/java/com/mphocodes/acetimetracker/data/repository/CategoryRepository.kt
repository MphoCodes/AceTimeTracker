package com.mphocodes.acetimetracker.data.repository

import androidx.lifecycle.LiveData
import com.mphocodes.acetimetracker.data.local.RecordCodec
import com.mphocodes.acetimetracker.data.model.Category
import com.mphocodes.acetimetracker.data.sync.OfflineData
import com.mphocodes.acetimetracker.data.sync.SyncStatus
import com.mphocodes.acetimetracker.util.Resource
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class CategoryRepository(private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    private val local by lazy { OfflineData.get(firestore) }
    fun observeSyncStatus(userId: String): LiveData<SyncStatus> = local.status(userId)
    fun observeChanges(userId: String): LiveData<Long> = local.changes(userId)
    fun retrySync(userId: String) = local.retry(userId)

    fun fetchCategories(userId: String, onResult: (Resource<List<Category>>) -> Unit) {
        local.read(userId, RecordCodec.CATEGORIES) { result ->
            onResult(when (result) {
                is Resource.Success -> Resource.Success(result.data.map(RecordCodec::category).sortedBy { it.name.lowercase() })
                is Resource.Error -> result
                Resource.Loading -> Resource.Loading
            })
        }
    }

    fun addCategory(userId: String, category: Category, onResult: (Resource<String>) -> Unit) {
        val saved = category.copy(documentId = category.documentId.ifBlank { UUID.randomUUID().toString() })
        local.write(userId, RecordCodec.CATEGORIES, saved.documentId, RecordCodec.encode(saved)) { result ->
            onResult(when (result) {
                is Resource.Success -> Resource.Success(saved.documentId)
                is Resource.Error -> result
                Resource.Loading -> Resource.Loading
            })
        }
    }

    /** Historical entry labels deliberately remain unchanged when a category is renamed. */
    fun updateCategory(userId: String, category: Category, onResult: (Resource<Unit>) -> Unit) {
        local.write(userId, RecordCodec.CATEGORIES, category.documentId, RecordCodec.encode(category), onResult)
    }

    fun deleteCategory(userId: String, categoryId: String, onResult: (Resource<Unit>) -> Unit) {
        local.write(userId, RecordCodec.CATEGORIES, categoryId, null, onResult)
    }
}
