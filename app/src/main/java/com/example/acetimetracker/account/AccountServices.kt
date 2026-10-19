package com.example.acetimetracker.account

import android.net.Uri
import com.google.android.gms.tasks.Task

data class Account(val uid: String, val email: String? = null, val displayName: String? = null, val photoUrl: String? = null) {
    init { require(uid.isNotBlank()) }
}
data class UserProfile(val displayName: String = "", val email: String = "", val profilePictureUrl: String = "")

interface AccountAuth {
    val current: Account?
    fun signIn(email: String, password: String): Task<Account>
    fun register(email: String, password: String): Task<Account>
    fun resetPassword(email: String): Task<Void>
    fun signOut()
    fun observe(listener: (Account?) -> Unit): () -> Unit
}
interface Profiles {
    fun ensure(account: Account, preferredName: String? = null): Task<UserProfile>
    fun update(uid: String, fields: Map<String, String>): Task<Void>
}
interface ProfilePhotos { fun upload(uid: String, uri: Uri): Task<String> }
data class AccountServices(val auth: AccountAuth, val profiles: Profiles, val photos: ProfilePhotos)

object ProfileFields {
    fun missing(existing: Map<String, Any?>, account: Account, preferredName: String? = null): Map<String, String> =
        mapOf("displayName" to (preferredName?.trim()?.takeIf { it.isNotEmpty() } ?: account.displayName.orEmpty()),
            "email" to account.email.orEmpty(), "profilePictureUrl" to account.photoUrl.orEmpty())
            .filterKeys { !existing.containsKey(it) }

    fun edit(displayName: String, newPhotoUrl: String?): Map<String, String> = buildMap {
        put("displayName", displayName.trim())
        if (newPhotoUrl != null) put("profilePictureUrl", newPhotoUrl)
    }

    fun read(fields: Map<String, Any?>): UserProfile = UserProfile(
        fields["displayName"] as? String ?: "", fields["email"] as? String ?: "",
        fields["profilePictureUrl"] as? String ?: "")
}
