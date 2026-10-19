package com.example.acetimetracker.account

import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import java.util.UUID

fun firebaseAccountServices(): AccountServices = AccountServices(FirebaseAccountAuth(), FirebaseProfiles(), FirebaseProfilePhotos())

private fun FirebaseUser.account() = Account(uid, email, displayName, photoUrl?.toString())

private class FirebaseAccountAuth : AccountAuth {
    private val auth get() = FirebaseAuth.getInstance()
    override val current get() = auth.currentUser?.account()
    override fun signIn(email: String, password: String): Task<Account> =
        auth.signInWithEmailAndPassword(email, password).onSuccessTask { Tasks.forResult(requireNotNull(it.user).account()) }
    override fun register(email: String, password: String): Task<Account> =
        auth.createUserWithEmailAndPassword(email, password).onSuccessTask { Tasks.forResult(requireNotNull(it.user).account()) }
    override fun resetPassword(email: String): Task<Void> = auth.sendPasswordResetEmail(email)
    override fun signOut() = auth.signOut()
    override fun observe(listener: (Account?) -> Unit): () -> Unit {
        val firebaseListener = FirebaseAuth.AuthStateListener { listener(it.currentUser?.account()) }
        auth.addAuthStateListener(firebaseListener)
        return { auth.removeAuthStateListener(firebaseListener) }
    }
}

private class FirebaseProfiles : Profiles {
    private val db get() = FirebaseFirestore.getInstance()
    override fun ensure(account: Account, preferredName: String?): Task<UserProfile> {
        val ref = db.collection("users").document(account.uid)
        return db.runTransaction { transaction ->
            val existing = transaction.get(ref).data.orEmpty()
            val missing = ProfileFields.missing(existing, account, preferredName)
            if (missing.isNotEmpty()) transaction.set(ref, missing, SetOptions.merge())
            ProfileFields.read(existing + missing)
        }.continueWithTask { result ->
            if (result.isSuccessful) Tasks.forResult(result.result)
            else ref.get(Source.CACHE).continueWith { cached ->
                if (!cached.isSuccessful || !cached.result.exists()) throw (result.exception ?: IllegalStateException("Profile setup unavailable"))
                // Existing cached accounts can open offline; no synthetic profile is written.
                ProfileFields.read(cached.result.data.orEmpty())
            }
        }
    }
    override fun update(uid: String, fields: Map<String, String>): Task<Void> {
        require(uid.isNotBlank())
        require(fields.keys.all { it == "displayName" || it == "profilePictureUrl" })
        return db.collection("users").document(uid).set(fields, SetOptions.merge())
    }
}

private class FirebaseProfilePhotos : ProfilePhotos {
    override fun upload(uid: String, uri: Uri): Task<String> {
        require(uid.isNotBlank())
        val ref = FirebaseStorage.getInstance().reference.child("profile_pictures/$uid/${UUID.randomUUID()}")
        val metadata = StorageMetadata.Builder().setCustomMetadata("ownerUid", uid).build()
        return ref.putFile(uri, metadata).onSuccessTask { ref.downloadUrl }.onSuccessTask { Tasks.forResult(it.toString()) }
    }
}
