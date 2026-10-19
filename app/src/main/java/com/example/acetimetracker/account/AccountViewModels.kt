package com.example.acetimetracker.account

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.tasks.Task

enum class AccountProblem { INPUT, SIGN_IN, REGISTRATION, PROFILE_SETUP, PASSWORD_RESET, PROFILE_SAVE, UPLOAD, SESSION_CHANGED }
enum class AuthPhase { IDLE, SIGNED_OUT, WORKING, READY, PROFILE_RETRY, ERROR, RESET_SENT }
data class AuthState(val phase: AuthPhase = AuthPhase.IDLE, val uid: String? = null, val problem: AccountProblem? = null)

class AuthViewModel(private val services: AccountServices) : ViewModel() {
    private val mutable = MutableLiveData(AuthState())
    val state: LiveData<AuthState> = mutable
    private var generation = 0
    private var cleared = false
    private var preferredName: String? = null
    private var preferredUid: String? = null
    private val busy get() = mutable.value?.phase == AuthPhase.WORKING
    private val stopObserving = services.auth.observe { account ->
        val currentState = mutable.value!!
        // A restored identity can arrive after onStart. In-flight registration owns
        // its chosen-name bootstrap; same-UID setup failures still require retry.
        if (!cleared && !busy && (account?.uid != currentState.uid || currentState.phase == AuthPhase.IDLE)) {
            resume()
        }
    }

    fun resume() {
        if (busy) return
        val account = services.auth.current
        if (account == null) { mutable.value = AuthState(AuthPhase.SIGNED_OUT); return }
        if (mutable.value?.phase == AuthPhase.READY && mutable.value?.uid == account.uid) return
        val request = ++generation
        mutable.value = AuthState(AuthPhase.WORKING)
        bootstrap(account, if (preferredUid == account.uid) preferredName else null, request)
    }
    fun signIn(email: String, password: String) {
        if (busy) return
        if (email.isBlank() || password.isEmpty()) { fail(AccountProblem.INPUT); return }
        authenticate(null, AccountProblem.SIGN_IN) { services.auth.signIn(email.trim(), password) }
    }
    fun register(name: String, email: String, password: String, confirmation: String) {
        if (busy) return
        if (name.isBlank() || email.isBlank() || password.isEmpty() || password != confirmation) { fail(AccountProblem.INPUT); return }
        authenticate(name.trim(), AccountProblem.REGISTRATION) { services.auth.register(email.trim(), password) }
    }
    private fun authenticate(name: String?, problem: AccountProblem, action: () -> Task<Account>) {
        val request = ++generation
        mutable.value = AuthState(AuthPhase.WORKING)
        action().addOnCompleteListener { result ->
            if (cleared || request != generation) return@addOnCompleteListener
            if (!result.isSuccessful) {
                fail(problem)
                if (services.auth.current != null) resume()
                return@addOnCompleteListener
            }
            val account = result.result
            if (!current(account.uid, request)) return@addOnCompleteListener
            preferredName = name; preferredUid = account.uid
            bootstrap(account, name, request)
        }
    }
    private fun bootstrap(account: Account, name: String?, request: Int) {
        services.profiles.ensure(account, name).addOnCompleteListener { result ->
            if (!current(account.uid, request)) return@addOnCompleteListener
            mutable.value = if (result.isSuccessful) AuthState(AuthPhase.READY, account.uid)
                else AuthState(AuthPhase.PROFILE_RETRY, account.uid, AccountProblem.PROFILE_SETUP)
        }
    }
    private fun current(uid: String, request: Int): Boolean {
        if (cleared || request != generation) return false
        if (services.auth.current?.uid == uid) return true
        mutable.value = AuthState(AuthPhase.IDLE)
        resume()
        return false
    }
    fun resetPassword(email: String) {
        if (busy) return
        if (email.isBlank()) { fail(AccountProblem.INPUT); return }
        val request = ++generation
        mutable.value = AuthState(AuthPhase.WORKING)
        services.auth.resetPassword(email.trim()).addOnCompleteListener { result ->
            if (cleared || request != generation) return@addOnCompleteListener
            if (result.isSuccessful) mutable.value = AuthState(AuthPhase.RESET_SENT) else fail(AccountProblem.PASSWORD_RESET)
            // Auth notifications are deferred while reset is working as well.
            if (services.auth.current != null) resume()
        }
    }
    fun useDifferentAccount() {
        ++generation; preferredUid = null; preferredName = null
        services.auth.signOut(); mutable.value = AuthState(AuthPhase.SIGNED_OUT)
    }
    fun acknowledgeReset() { if (mutable.value?.phase == AuthPhase.RESET_SENT) mutable.value = AuthState() }
    private fun fail(problem: AccountProblem) { mutable.value = AuthState(AuthPhase.ERROR, problem = problem) }
    override fun onCleared() { cleared = true; ++generation; stopObserving() }
}

data class ProfileState(val loading: Boolean = true, val loaded: Boolean = false, val name: String = "",
    val photo: String = "", val saving: Boolean = false, val uploading: Boolean = false,
    val saved: Boolean = false, val problem: AccountProblem? = null)

class ProfileViewModel(private val services: AccountServices) : ViewModel() {
    private val mutable = MutableLiveData(ProfileState())
    val state: LiveData<ProfileState> = mutable
    private var owner: Account? = null
    private var newPhoto: String? = null
    private var cleared = false
    private fun current(): Boolean = !cleared && owner != null && services.auth.current?.uid == owner?.uid
    fun load(account: Account) {
        if (owner != null) return
        owner = account
        reload()
    }
    fun reload() {
        val account = owner ?: return
        if (!current()) return
        mutable.value = ProfileState()
        services.profiles.ensure(account).addOnCompleteListener { result ->
            if (!current()) return@addOnCompleteListener
            mutable.value = if (result.isSuccessful) ProfileState(loading = false, loaded = true,
                name = result.result.displayName, photo = result.result.profilePictureUrl)
                else ProfileState(loading = false, problem = AccountProblem.PROFILE_SETUP)
        }
    }
    fun editName(value: String) {
        val s = mutable.value ?: return
        if (s.loaded && !s.saving && s.name != value) mutable.value = s.copy(name = value, problem = null)
    }
    fun save() {
        val s = mutable.value ?: return
        if (!current() || !s.loaded || s.saving || s.uploading) return
        if (s.name.isBlank()) { mutable.value = s.copy(problem = AccountProblem.INPUT); return }
        mutable.value = s.copy(saving = true, problem = null)
        services.profiles.update(owner!!.uid, ProfileFields.edit(s.name, newPhoto)).addOnCompleteListener { result ->
            if (!current()) return@addOnCompleteListener
            mutable.value = mutable.value!!.copy(saving = false, saved = result.isSuccessful,
                problem = if (result.isSuccessful) null else AccountProblem.PROFILE_SAVE)
        }
    }
    fun upload(uri: Uri) {
        val s = mutable.value ?: return
        if (!current() || !s.loaded || s.saving || s.uploading) return
        mutable.value = s.copy(uploading = true, problem = null)
        services.photos.upload(owner!!.uid, uri).addOnCompleteListener { result ->
            if (!current()) return@addOnCompleteListener
            if (result.isSuccessful) newPhoto = result.result
            mutable.value = mutable.value!!.copy(uploading = false,
                photo = if (result.isSuccessful) result.result else mutable.value!!.photo,
                problem = if (result.isSuccessful) null else AccountProblem.UPLOAD)
        }
    }
    override fun onCleared() { cleared = true }
}

class AccountViewModelFactory(private val services: AccountServices) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        AuthViewModel::class.java -> AuthViewModel(services) as T
        ProfileViewModel::class.java -> ProfileViewModel(services) as T
        else -> throw IllegalArgumentException("Unknown account ViewModel")
    }
}
