package com.example.acetimetracker

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.example.acetimetracker.account.*

class Registration : AuthEntryActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)
        findViewById<Button>(R.id.btnRegister).setOnClickListener {
            if (authModel.state.value?.phase == AuthPhase.PROFILE_RETRY) authModel.resume()
            else authModel.register(findViewById<EditText>(R.id.etUsername).text.toString(),
                findViewById<EditText>(R.id.etEmail).text.toString(), findViewById<EditText>(R.id.etPassword).text.toString(),
                findViewById<EditText>(R.id.etConfirmPassword).text.toString())
        }
        findViewById<View>(R.id.tvLoginLink).setOnClickListener { SessionRouting.root(this, Login::class.java) }
        findViewById<View>(R.id.buttonDifferentAccount).setOnClickListener { authModel.useDifferentAccount() }
    }
    override fun render(state: AuthState) {
        val busy = state.phase == AuthPhase.WORKING
        findViewById<Button>(R.id.btnRegister).apply {
            isEnabled = !busy
            setText(if (state.phase == AuthPhase.PROFILE_RETRY) R.string.account_retry else R.string.btn_register)
        }
        findViewById<View>(R.id.tvLoginLink).isEnabled = !busy
        findViewById<View>(R.id.buttonDifferentAccount).visibility = if (state.phase == AuthPhase.PROFILE_RETRY) View.VISIBLE else View.GONE
        findViewById<TextView>(R.id.authStatusTextView).text = if (busy) getString(R.string.account_working) else state.problem?.let { getString(it.messageResource()) }.orEmpty()
    }
}
