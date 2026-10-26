package com.example.acetimetracker.data.repository

import android.net.Uri
import com.example.acetimetracker.data.model.TimesheetEntry
import com.example.acetimetracker.util.Resource
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class TimesheetRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {

    fun uploadImage(imageUri: Uri, onResult: (Resource<String>) -> Unit) {
        onResult(Resource.Loading)
        val imageRef = storage.reference.child("images/${UUID.randomUUID()}")
        imageRef.putFile(imageUri)
            .addOnSuccessListener {
                imageRef.downloadUrl
                    .addOnSuccessListener { uri ->
                        onResult(Resource.Success(uri.toString()))
                    }
                    .addOnFailureListener { exception ->
                        onResult(Resource.Error(exception.localizedMessage ?: "Failed to get download URL", exception))
                    }
            }
            .addOnFailureListener { exception ->
                onResult(Resource.Error(exception.localizedMessage ?: "Image upload failed", exception))
            }
    }

    fun saveTimesheetEntry(
        userId: String,
        entry: TimesheetEntry,
        onResult: (Resource<Unit>) -> Unit
    ) {
        if (userId.isEmpty()) {
            onResult(Resource.Error("User not authenticated"))
            return
        }

        onResult(Resource.Loading)
        val timesheetDocRef = firestore.collection("timesheetEntries").document()
        val entryWithId = entry.copy(id = timesheetDocRef.id, userId = userId)

        val totalHoursWorked = entry.timeSpent / (1000.0 * 60.0 * 60.0)

        val dailySummary = hashMapOf(
            "date" to entry.startDate,
            "totalHours" to totalHoursWorked,
            "category" to entry.category,
            "userId" to userId
        )

        // Save detailed entry in timesheetEntries collection
        timesheetDocRef.set(entryWithId)
            .addOnSuccessListener {
                // Also update user's dailyTimesheetEntries array for graph/summary views
                firestore.collection("users").document(userId)
                    .update("dailyTimesheetEntries", FieldValue.arrayUnion(dailySummary))

                onResult(Resource.Success(Unit))
            }
            .addOnFailureListener { exception ->
                onResult(Resource.Error(exception.localizedMessage ?: "Failed to save timesheet", exception))
            }
    }

    fun fetchTimesheets(userId: String, onResult: (Resource<List<TimesheetEntry>>) -> Unit) {
        if (userId.isEmpty()) {
            onResult(Resource.Error("User not authenticated"))
            return
        }

        onResult(Resource.Loading)
        firestore.collection("timesheetEntries")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val entries = snapshot.documents.mapNotNull { it.toObject(TimesheetEntry::class.java) }
                onResult(Resource.Success(entries))
            }
            .addOnFailureListener { exception ->
                onResult(Resource.Error(exception.localizedMessage ?: "Failed to fetch timesheets", exception))
            }
    }

    fun fetchTimesheetsByCategory(
        userId: String,
        category: String,
        onResult: (Resource<List<TimesheetEntry>>) -> Unit
    ) {
        if (userId.isEmpty()) {
            onResult(Resource.Error("User not authenticated"))
            return
        }

        onResult(Resource.Loading)
        val query = if (category == "All" || category.isEmpty()) {
            firestore.collection("timesheetEntries").whereEqualTo("userId", userId)
        } else {
            firestore.collection("timesheetEntries")
                .whereEqualTo("userId", userId)
                .whereEqualTo("category", category)
        }

        query.get()
            .addOnSuccessListener { snapshot ->
                val entries = snapshot.documents.mapNotNull { it.toObject(TimesheetEntry::class.java) }
                onResult(Resource.Success(entries))
            }
            .addOnFailureListener { exception ->
                onResult(Resource.Error(exception.localizedMessage ?: "Failed to fetch timesheets by category", exception))
            }
    }

    fun fetchChartData(userId: String, onResult: (Resource<Map<String, Float>>) -> Unit) {
        if (userId.isEmpty()) {
            onResult(Resource.Error("User not authenticated"))
            return
        }

        onResult(Resource.Loading)
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                @Suppress("UNCHECKED_CAST")
                val dailyTimesheetEntries = document.get("dailyTimesheetEntries") as? List<Map<String, Any>>
                val categoryTimeMap = mutableMapOf<String, Float>()

                dailyTimesheetEntries?.forEach { entry ->
                    val cat = entry["category"] as? String ?: return@forEach
                    val hours = (entry["totalHours"] as? Number)?.toFloat() ?: return@forEach
                    categoryTimeMap[cat] = (categoryTimeMap[cat] ?: 0f) + hours
                }
                onResult(Resource.Success(categoryTimeMap))
            }
            .addOnFailureListener { exception ->
                onResult(Resource.Error(exception.localizedMessage ?: "Failed to fetch chart data", exception))
            }
    }
}
