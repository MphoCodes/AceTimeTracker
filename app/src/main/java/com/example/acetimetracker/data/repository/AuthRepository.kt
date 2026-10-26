package com.example.acetimetracker.data.repository

import com.example.acetimetracker.util.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class AuthRepository(private val auth: FirebaseAuth = FirebaseAuth.getInstance()) {

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val currentUserId: String
        get() = auth.currentUser?.uid.orEmpty()

    val isAuthenticated: Boolean
        get() = auth.currentUser != null

    fun login(email: String, password: String, onResult: (Resource<FirebaseUser>) -> Unit) {
        onResult(Resource.Loading)
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                result.user?.let {
                    onResult(Resource.Success(it))
                } ?: onResult(Resource.Error("User session not found"))
            }
            .addOnFailureListener { exception ->
                onResult(Resource.Error(exception.localizedMessage ?: "Login failed", exception))
            }
    }

    fun register(email: String, password: String, onResult: (Resource<FirebaseUser>) -> Unit) {
        onResult(Resource.Loading)
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                result.user?.let {
                    onResult(Resource.Success(it))
                } ?: onResult(Resource.Error("Registration succeeded but user is null"))
            }
            .addOnFailureListener { exception ->
                onResult(Resource.Error(exception.localizedMessage ?: "Registration failed", exception))
            }
    }

    fun sendPasswordReset(email: String, onResult: (Resource<Unit>) -> Unit) {
        onResult(Resource.Loading)
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                onResult(Resource.Success(Unit))
            }
            .addOnFailureListener { exception ->
                onResult(Resource.Error(exception.localizedMessage ?: "Password reset failed", exception))
            }
    }

    fun logout() {
        auth.signOut()
    }
}
