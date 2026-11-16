package com.mphocodes.acetimetracker.data.repository

import android.net.Uri
import androidx.lifecycle.LiveData
import com.mphocodes.acetimetracker.data.local.RecordCodec
import com.mphocodes.acetimetracker.data.model.TimesheetEntry
import com.mphocodes.acetimetracker.data.sync.OfflineData
import com.mphocodes.acetimetracker.data.sync.SyncStatus
import com.mphocodes.acetimetracker.util.Resource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class TimesheetRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {
    private val local by lazy { OfflineData.get(firestore) }
    fun observeSyncStatus(userId: String): LiveData<SyncStatus> = local.status(userId)
    fun observeChanges(userId: String): LiveData<Long> = local.changes(userId)
    fun retrySync(userId: String) = local.retry(userId)

    fun uploadImage(imageUri: Uri, onResult: (Resource<String>) -> Unit) {
        onResult(Resource.Loading)
        try {
            val imageRef = storage.reference.child("images/${UUID.randomUUID()}")
            imageRef.putFile(imageUri)
                .addOnSuccessListener {
                    imageRef.downloadUrl
                        .addOnSuccessListener { uri ->
                            onResult(Resource.Success(uri.toString()))
                        }
                        .addOnFailureListener { exception ->
                            onResult(Resource.Error("Could not retrieve the uploaded photo. Please try again.", exception))
                        }
                }
                .addOnFailureListener { exception ->
                    onResult(Resource.Error("Could not upload the photo. Please try again.", exception))
                }
        } catch (exception: Exception) {
            onResult(Resource.Error("Could not upload the photo. Please try again.", exception))
        }
    }

    /** Success means the entry and its pending sync have been durably committed locally. */
    fun saveTimesheetEntry(userId: String, entry: TimesheetEntry, onResult: (Resource<Unit>) -> Unit) {
        if (userId.isBlank()) { onResult(Resource.Error("User not authenticated")); return }
        val saved = entry.copy(id = entry.id.ifBlank { UUID.randomUUID().toString() }, userId = userId)
        local.write(userId, RecordCodec.ENTRIES, saved.id, RecordCodec.encode(saved), onResult)
    }

    fun fetchTimesheets(userId: String, onResult: (Resource<List<TimesheetEntry>>) -> Unit) {
        local.read(userId, RecordCodec.ENTRIES) { result ->
            onResult(when (result) {
                is Resource.Success -> Resource.Success(result.data.map(RecordCodec::entry)
                    .sortedWith(compareByDescending<TimesheetEntry> { it.startDate }.thenByDescending { it.startTime }))
                is Resource.Error -> result
                Resource.Loading -> Resource.Loading
            })
        }
    }

    fun fetchTimesheetsByCategory(userId: String, category: String, onResult: (Resource<List<TimesheetEntry>>) -> Unit) {
        fetchTimesheets(userId) { result ->
            onResult(when (result) {
                is Resource.Success -> Resource.Success(if (category.isBlank() || category == "All") result.data else result.data.filter { it.category == category })
                is Resource.Error -> result
                Resource.Loading -> Resource.Loading
            })
        }
    }

    fun fetchChartData(userId: String, onResult: (Resource<Map<String, Float>>) -> Unit) {
        fetchTimesheets(userId) { result ->
            onResult(when (result) {
                is Resource.Success -> Resource.Success(TimesheetStats.hoursByCategory(result.data))
                is Resource.Error -> result
                Resource.Loading -> Resource.Loading
            })
        }
    }

    fun deleteTimesheetEntry(userId: String, entryId: String, onResult: (Resource<Unit>) -> Unit) {
        local.write(userId, RecordCodec.ENTRIES, entryId, null, onResult)
    }
}
