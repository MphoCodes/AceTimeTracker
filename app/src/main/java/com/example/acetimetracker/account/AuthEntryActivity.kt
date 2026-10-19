package com.example.acetimetracker.account

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.acetimetracker.AceApplication
import com.example.acetimetracker.Login
import com.example.acetimetracker.R

abstract class AuthEntryActivity : AppCompatActivity() {
    protected val accounts get() = (application as AceApplication).accounts
    protected val authModel by lazy { ViewModelProvider(this, AccountViewModelFactory(accounts))[AuthViewModel::class.java] }
    protected open val startup = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authModel.state.observe(this) { state ->
            render(state)
            when (state.phase) {
                AuthPhase.READY -> if (accounts.auth.current?.uid == state.uid) SessionRouting.home(this, state.uid!!) else authModel.resume()
                AuthPhase.SIGNED_OUT, AuthPhase.PROFILE_RETRY -> if (startup) SessionRouting.root(this, Login::class.java)
                AuthPhase.RESET_SENT -> {
                    onResetSent(); Toast.makeText(this, R.string.account_reset_sent, Toast.LENGTH_LONG).show(); authModel.acknowledgeReset()
                }
                else -> Unit
            }
        }
    }
    override fun onStart() { super.onStart(); authModel.resume() }
    protected open fun render(state: AuthState) = Unit
    protected open fun onResetSent() = Unit
}

fun AccountProblem.messageResource(): Int = when (this) {
    AccountProblem.INPUT -> R.string.account_invalid_input
    AccountProblem.SIGN_IN -> R.string.account_sign_in_failed
    AccountProblem.REGISTRATION -> R.string.account_registration_failed
    AccountProblem.PROFILE_SETUP -> R.string.account_profile_retry
    AccountProblem.PASSWORD_RESET -> R.string.account_reset_failed
    AccountProblem.PROFILE_SAVE -> R.string.account_save_failed
    AccountProblem.UPLOAD -> R.string.account_upload_failed
    AccountProblem.SESSION_CHANGED -> R.string.account_session_changed
}
