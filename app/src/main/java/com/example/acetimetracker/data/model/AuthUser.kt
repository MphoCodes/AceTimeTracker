package com.example.acetimetracker.data.model

data class AuthUser(
    val uid: String,
    val email: String,
    val isMock: Boolean = false
)
