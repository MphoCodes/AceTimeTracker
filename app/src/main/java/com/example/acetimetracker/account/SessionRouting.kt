package com.example.acetimetracker.account

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.acetimetracker.AceApplication
import com.example.acetimetracker.Login
import com.example.acetimetracker.MainActivity
import com.example.acetimetracker.Splash

object SessionRouting {
    const val OWNER = "account_session_owner"
    fun root(activity: Activity, target: Class<out Activity>, uid: String? = null) {
        if (activity.isFinishing || activity.isDestroyed) return
        activity.startActivity(Intent(activity, target).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            if (uid != null) putExtra(OWNER, uid)
        })
        activity.finish()
    }
    fun home(activity: Activity, uid: String) = root(activity, MainActivity::class.java, uid)
    fun entry(activity: Activity, account: Account?) = root(activity, if (account == null) Login::class.java else Splash::class.java)
}

open class AccountActivity : AppCompatActivity() {
    protected val accounts get() = (application as AceApplication).accounts
    protected lateinit var sessionAccount: Account
    protected var sessionReady = false
        private set
    private var stopObserving: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val current = accounts.auth.current
        val previous = savedInstanceState?.getString(SessionRouting.OWNER) ?: intent.getStringExtra(SessionRouting.OWNER)
        if (current == null || (previous != null && previous != current.uid)) {
            SessionRouting.entry(this, current); return
        }
        sessionAccount = current; sessionReady = true
    }
    override fun onStart() {
        super.onStart()
        if (!sessionReady) return
        if (currentSessionUid() == null) {
            SessionRouting.entry(this, accounts.auth.current)
            return
        }
        stopObserving = accounts.auth.observe { current ->
            if (current?.uid != sessionAccount.uid) SessionRouting.entry(this, current)
        }
    }
    override fun onStop() { stopObserving?.invoke(); stopObserving = null; super.onStop() }
    override fun onSaveInstanceState(outState: Bundle) {
        if (sessionReady) outState.putString(SessionRouting.OWNER, sessionAccount.uid)
        super.onSaveInstanceState(outState)
    }
    protected fun currentSessionUid(): String? = if (sessionReady && !isFinishing && !isDestroyed && accounts.auth.current?.uid == sessionAccount.uid) sessionAccount.uid else null
}
