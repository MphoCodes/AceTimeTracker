package com.example.acetimetracker.data.repository

import android.net.Uri
import com.example.acetimetracker.data.model.UserProfile
import com.example.acetimetracker.util.Resource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class UserRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {

    companion object {
        private var mockProfile = UserProfile(
            userId = AuthRepository.TEST_USER_ID,
            displayName = "Ace Tester",
            email = AuthRepository.TEST_USER_EMAIL,
            profilePictureUrl = "",
            minGoal = 2f,
            maxGoal = 8f
        )
    }

    fun fetchUserProfile(userId: String, onResult: (Resource<UserProfile>) -> Unit) {
        if (userId.isEmpty()) {
            onResult(Resource.Error("User not authenticated"))
            return
        }

        // Offline test mode bypass
        if (userId == AuthRepository.TEST_USER_ID) {
            onResult(Resource.Success(mockProfile))
            return
        }

        onResult(Resource.Loading)
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { doc ->
                val profile = UserProfile(
                    userId = userId,
                    displayName = doc.getString("displayName").orEmpty(),
                    email = doc.getString("email").orEmpty(),
                    profilePictureUrl = doc.getString("profilePictureUrl").orEmpty(),
                    minGoal = doc.getDouble("minGoal")?.toFloat(),
                    maxGoal = doc.getDouble("maxGoal")?.toFloat()
                )
                onResult(Resource.Success(profile))
            }
            .addOnFailureListener { exception ->
                onResult(Resource.Error(exception.localizedMessage ?: "Failed to fetch user profile", exception))
            }
    }

    fun updateUserProfile(
        userId: String,
        displayName: String,
        email: String,
        profilePictureUrl: String,
        onResult: (Resource<Unit>) -> Unit
    ) {
        if (userId.isEmpty()) {
            onResult(Resource.Error("User not authenticated"))
            return
        }

        // Offline test mode bypass
        if (userId == AuthRepository.TEST_USER_ID) {
            mockProfile = mockProfile.copy(
                displayName = displayName,
                email = email,
                profilePictureUrl = profilePictureUrl
            )
            onResult(Resource.Success(Unit))
            return
        }

        onResult(Resource.Loading)
        val data = hashMapOf(
            "displayName" to displayName,
            "email" to email,
            "profilePictureUrl" to profilePictureUrl
        )

        firestore.collection("users").document(userId)
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                onResult(Resource.Success(Unit))
            }
            .addOnFailureListener { exception ->
                onResult(Resource.Error(exception.localizedMessage ?: "Failed to update profile", exception))
            }
    }

    fun updateUserGoals(
        userId: String,
        minGoal: Float,
        maxGoal: Float,
        onResult: (Resource<Unit>) -> Unit
    ) {
        if (userId.isEmpty()) {
            onResult(Resource.Error("User not authenticated"))
            return
        }

        // Offline test mode bypass
        if (userId == AuthRepository.TEST_USER_ID) {
            mockProfile = mockProfile.copy(minGoal = minGoal, maxGoal = maxGoal)
            onResult(Resource.Success(Unit))
            return
        }

        onResult(Resource.Loading)
        firestore.collection("users").document(userId)
            .update(
                "minGoal", minGoal,
                "maxGoal", maxGoal
            )
            .addOnSuccessListener {
                onResult(Resource.Success(Unit))
            }
            .addOnFailureListener { exception ->
                onResult(Resource.Error(exception.localizedMessage ?: "Failed to update goals", exception))
            }
    }

    fun uploadProfilePicture(imageUri: Uri, onResult: (Resource<String>) -> Unit) {
        onResult(Resource.Loading)
        try {
            val imageRef = storage.reference.child("profile_pictures/${UUID.randomUUID()}")
            imageRef.putFile(imageUri)
                .addOnSuccessListener {
                    imageRef.downloadUrl
                        .addOnSuccessListener { uri ->
                            onResult(Resource.Success(uri.toString()))
                        }
                        .addOnFailureListener {
                            onResult(Resource.Success(imageUri.toString()))
                        }
                }
                .addOnFailureListener {
                    onResult(Resource.Success(imageUri.toString()))
                }
        } catch (e: Exception) {
            onResult(Resource.Success(imageUri.toString()))
        }
    }
}
