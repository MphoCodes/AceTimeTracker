package com.example.acetimetracker.account

import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks

class FakeAuth : AccountAuth {
    var account: Account? = null
    override val current get() = account
    var signInResult: Task<Account> = Tasks.forResult(Account("a", "a@example.com"))
    var resetResult: Task<Void> = Tasks.forResult(null)
    var notifyInitially = true
    var signInCalls = 0
    val listeners = mutableListOf<(Account?) -> Unit>()
    override fun signIn(email: String, password: String): Task<Account> { signInCalls++; return signInResult.onSuccessTask { change(it); Tasks.forResult(it) } }
    override fun register(email: String, password: String) = signIn(email, password)
    override fun resetPassword(email: String) = resetResult
    override fun signOut() { change(null) }
    override fun observe(listener: (Account?) -> Unit): () -> Unit { listeners.add(listener); if (notifyInitially) listener(current); return { listeners.remove(listener) } }
    fun change(value: Account?) { account = value; listeners.toList().forEach { it(value) } }
}
class FakeProfiles : Profiles {
    var ensureResult: Task<UserProfile> = Tasks.forResult(UserProfile("Existing", "legacy@example.com", "old-photo"))
    var updateResult: Task<Void> = Tasks.forResult(null)
    val ensures = mutableListOf<Pair<String, String?>>()
    val updates = mutableListOf<Pair<String, Map<String, String>>>()
    override fun ensure(account: Account, preferredName: String?): Task<UserProfile> { ensures.add(account.uid to preferredName); return ensureResult }
    override fun update(uid: String, fields: Map<String, String>): Task<Void> { updates.add(uid to fields); return updateResult }
}
class FakePhotos : ProfilePhotos {
    var result: Task<String> = Tasks.forResult("new-photo")
    override fun upload(uid: String, uri: Uri) = result
}
class FakeAccounts {
    val auth = FakeAuth()
    val profiles = FakeProfiles()
    val photos = FakePhotos()
    val services = AccountServices(auth, profiles, photos)
}
