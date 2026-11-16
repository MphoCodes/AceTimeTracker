package com.mphocodes.acetimetracker.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.mphocodes.acetimetracker.data.repository.AuthRepository

class AccountDeletionViewModel(application: Application) : AndroidViewModel(application) {
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var accepted by mutableStateOf(false)
        private set
    private var acceptedUid: String? = null
    val available: Boolean get() = !AuthRepository().isMockSession && FirebaseAuth.getInstance().currentUser != null

    fun delete(password: String) {
        if (busy) return
        acceptedUid?.let { cleanup(it); return }
        val user = FirebaseAuth.getInstance().currentUser
        val email = user?.email
        if (user == null || email.isNullOrBlank() || password.isEmpty()) {
            error = "Enter your current password to confirm deletion."
            return
        }
        busy = true
        error = null
        user.reauthenticate(EmailAuthProvider.getCredential(email, password))
            .continueWithTask { task ->
                task.result
                user.getIdToken(true)
            }.continueWithTask { task ->
                task.result
                check(FirebaseAuth.getInstance().currentUser?.uid == user.uid) { "Account changed" }
                FirebaseFunctions.getInstance("us-central1").getHttpsCallable("deleteAccount")
                    .call(mapOf("confirm" to true, "expectedUid" to user.uid))
            }.addOnSuccessListener { result ->
                if ((result.data as? Map<*, *>)?.get("status") == "accepted") {
                    acceptedUid = user.uid
                    cleanup(user.uid)
                } else {
                    busy = false
                    error = "Deletion was not confirmed. Please try again."
                }
            }.addOnFailureListener {
                busy = false
                error = "Could not confirm deletion. Check your password and connection, then try again. If the request already reached the server, deletion will continue."
            }
    }

    private fun cleanup(uid: String) {
        busy = true
        com.mphocodes.acetimetracker.data.repository.AccountLocalCleanup.erase(getApplication(), uid) { success ->
            busy = false
            if (success) accepted = true
            else error = "Deletion was requested. Device cleanup failed; reopen the app to retry automatically."
        }
    }
}
