package com.example.acetimetracker.data.model

data class UserProfile(
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val profilePictureUrl: String = "",
    val minGoal: Float? = null,
    val maxGoal: Float? = null
)
