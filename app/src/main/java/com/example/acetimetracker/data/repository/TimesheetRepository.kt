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

    companion object {
        private val mockTimesheets = mutableListOf(
            TimesheetEntry(
                id = "mock_ts_1",
                userId = AuthRepository.TEST_USER_ID,
                category = "Development",
                startDate = "2026-10-25",
                startTime = "09:00",
                endDate = "2026-10-25",
                endTime = "14:30",
                timeSpent = 19800000L, // 5.5 hours
                imageUrl = "",
                description = "Refactored Clean Architecture and MVVM layers"
            ),
            TimesheetEntry(
                id = "mock_ts_2",
                userId = AuthRepository.TEST_USER_ID,
                category = "Design",
                startDate = "2026-10-26",
                startTime = "10:00",
                endDate = "2026-10-26",
                endTime = "13:00",
                timeSpent = 10800000L, // 3 hours
                imageUrl = "",
                description = "Designed progress chart layouts and navigation"
            ),
            TimesheetEntry(
                id = "mock_ts_3",
                userId = AuthRepository.TEST_USER_ID,
                category = "Testing",
                startDate = "2026-10-27",
                startTime = "08:00",
                endDate = "2026-10-27",
                endTime = "10:00",
                timeSpent = 7200000L, // 2 hours
                imageUrl = "",
                description = "Verified offline mock login flow"
            )
        )
    }

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
                        .addOnFailureListener {
                            // Fallback to local URI so testing without server works
                            onResult(Resource.Success(imageUri.toString()))
                        }
                }
                .addOnFailureListener {
                    // Fallback to local URI so testing without server works
                    onResult(Resource.Success(imageUri.toString()))
                }
        } catch (e: Exception) {
            onResult(Resource.Success(imageUri.toString()))
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

        // Offline test mode bypass
        if (userId == AuthRepository.TEST_USER_ID) {
            val generatedId = "mock_ts_${UUID.randomUUID().toString().take(6)}"
            val entryWithId = entry.copy(id = generatedId, userId = userId)
            mockTimesheets.add(0, entryWithId)
            onResult(Resource.Success(Unit))
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

        timesheetDocRef.set(entryWithId)
            .addOnSuccessListener {
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

        // Offline test mode bypass
        if (userId == AuthRepository.TEST_USER_ID) {
            onResult(Resource.Success(mockTimesheets.toList()))
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

        // Offline test mode bypass
        if (userId == AuthRepository.TEST_USER_ID) {
            val filtered = if (category == "All" || category.isEmpty()) {
                mockTimesheets.toList()
            } else {
                mockTimesheets.filter { it.category == category }
            }
            onResult(Resource.Success(filtered))
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

        // Offline test mode bypass
        if (userId == AuthRepository.TEST_USER_ID) {
            val categoryTimeMap = mutableMapOf<String, Float>()
            mockTimesheets.forEach { entry ->
                val hours = (entry.timeSpent / (1000f * 60f * 60f))
                categoryTimeMap[entry.category] = (categoryTimeMap[entry.category] ?: 0f) + hours
            }
            onResult(Resource.Success(categoryTimeMap))
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

    fun deleteTimesheetEntry(userId: String, entryId: String, onResult: (Resource<Unit>) -> Unit) {
        if (userId.isEmpty()) {
            onResult(Resource.Error("User not authenticated"))
            return
        }

        if (userId == AuthRepository.TEST_USER_ID) {
            mockTimesheets.removeAll { it.id == entryId }
            onResult(Resource.Success(Unit))
            return
        }

        onResult(Resource.Loading)
        firestore.collection("timesheetEntries").document(entryId)
            .delete()
            .addOnSuccessListener {
                onResult(Resource.Success(Unit))
            }
            .addOnFailureListener { exception ->
                onResult(Resource.Error(exception.localizedMessage ?: "Failed to delete timesheet entry", exception))
            }
    }
}
