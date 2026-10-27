package com.example.acetimetracker.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.acetimetracker.AceTimeTrackerApp
import com.example.acetimetracker.data.model.AuthUser
import com.example.acetimetracker.util.Resource
import com.google.firebase.auth.FirebaseAuth

class AuthRepository(private val auth: FirebaseAuth = FirebaseAuth.getInstance()) {

    companion object {
        const val TEST_USER_ID = "mock_test_user_001"
        const val TEST_USER_EMAIL = "test@acetimetracker.com"
        const val TEST_USER_PASSWORD = "password123"

        const val TEST_STUDENT_EMAIL = "st10203065@vcconnect.edu.za"
        const val TEST_STUDENT_PASSWORD = "password123"

        private const val PREFS_NAME = "auth_session_prefs"
        private const val KEY_MOCK_USER_ID = "mock_user_id"
        private const val KEY_MOCK_USER_EMAIL = "mock_user_email"

        val TEST_ACCOUNTS = mapOf(
            TEST_USER_EMAIL.lowercase() to TEST_USER_PASSWORD,
            TEST_STUDENT_EMAIL.lowercase() to TEST_STUDENT_PASSWORD
        )
    }

    private val prefs: SharedPreferences? by lazy {
        try {
            AceTimeTrackerApp.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        } catch (e: Exception) {
            null
        }
    }

    val currentUserId: String
        get() {
            val mockId = prefs?.getString(KEY_MOCK_USER_ID, null)
            if (!mockId.isNullOrEmpty()) return mockId
            return auth.currentUser?.uid.orEmpty()
        }

    val currentUserEmail: String
        get() {
            val mockEmail = prefs?.getString(KEY_MOCK_USER_EMAIL, null)
            if (!mockEmail.isNullOrEmpty()) return mockEmail
            return auth.currentUser?.email.orEmpty()
        }

    val isAuthenticated: Boolean
        get() {
            val mockId = prefs?.getString(KEY_MOCK_USER_ID, null)
            return !mockId.isNullOrEmpty() || auth.currentUser != null
        }

    val isMockSession: Boolean
        get() = !prefs?.getString(KEY_MOCK_USER_ID, null).isNullOrEmpty()

    fun loginTestAccount(
        email: String = TEST_USER_EMAIL,
        onResult: (Resource<AuthUser>) -> Unit
    ) {
        onResult(Resource.Loading)
        prefs?.edit()
            ?.putString(KEY_MOCK_USER_ID, TEST_USER_ID)
            ?.putString(KEY_MOCK_USER_EMAIL, email)
            ?.apply()

        onResult(Resource.Success(AuthUser(TEST_USER_ID, email, isMock = true)))
    }

    fun login(email: String, password: String, onResult: (Resource<AuthUser>) -> Unit) {
        val normalizedEmail = email.trim().lowercase()

        // Check for local test account to bypass remote server
        if (TEST_ACCOUNTS.containsKey(normalizedEmail) && TEST_ACCOUNTS[normalizedEmail] == password) {
            loginTestAccount(normalizedEmail, onResult)
            return
        }

        onResult(Resource.Loading)
        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { result ->
                result.user?.let { firebaseUser ->
                    // Clear any previous mock session
                    clearMockSession()
                    onResult(Resource.Success(AuthUser(firebaseUser.uid, firebaseUser.email.orEmpty())))
                } ?: onResult(Resource.Error("User session not found"))
            }
            .addOnFailureListener { exception ->
                onResult(Resource.Error(exception.localizedMessage ?: "Login failed", exception))
            }
    }

    fun register(email: String, password: String, onResult: (Resource<AuthUser>) -> Unit) {
        val normalizedEmail = email.trim().lowercase()

        // Local test account registration bypass
        if (TEST_ACCOUNTS.containsKey(normalizedEmail)) {
            loginTestAccount(normalizedEmail, onResult)
            return
        }

        onResult(Resource.Loading)
        auth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { result ->
                result.user?.let { firebaseUser ->
                    clearMockSession()
                    onResult(Resource.Success(AuthUser(firebaseUser.uid, firebaseUser.email.orEmpty())))
                } ?: onResult(Resource.Error("Registration succeeded but user is null"))
            }
            .addOnFailureListener { exception ->
                onResult(Resource.Error(exception.localizedMessage ?: "Registration failed", exception))
            }
    }

    fun sendPasswordReset(email: String, onResult: (Resource<Unit>) -> Unit) {
        val normalizedEmail = email.trim().lowercase()
        if (TEST_ACCOUNTS.containsKey(normalizedEmail)) {
            onResult(Resource.Success(Unit))
            return
        }

        onResult(Resource.Loading)
        auth.sendPasswordResetEmail(email.trim())
            .addOnSuccessListener {
                onResult(Resource.Success(Unit))
            }
            .addOnFailureListener { exception ->
                onResult(Resource.Error(exception.localizedMessage ?: "Password reset failed", exception))
            }
    }

    fun logout() {
        clearMockSession()
        auth.signOut()
    }

    private fun clearMockSession() {
        prefs?.edit()
            ?.remove(KEY_MOCK_USER_ID)
            ?.remove(KEY_MOCK_USER_EMAIL)
            ?.apply()
    }
}
