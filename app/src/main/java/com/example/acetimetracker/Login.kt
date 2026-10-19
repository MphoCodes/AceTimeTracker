package com.example.acetimetracker

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.example.acetimetracker.account.*

class Login : AuthEntryActivity() {
    private var resetDialog: AlertDialog? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        findViewById<Button>(R.id.btnLogin).setOnClickListener {
            if (authModel.state.value?.phase == AuthPhase.PROFILE_RETRY) authModel.resume()
            else authModel.signIn(findViewById<EditText>(R.id.etEmail).text.toString(), findViewById<EditText>(R.id.etPassword).text.toString())
        }
        findViewById<View>(R.id.tvRegisterLink).setOnClickListener { SessionRouting.root(this, Registration::class.java) }
        findViewById<View>(R.id.buttonDifferentAccount).setOnClickListener { authModel.useDifferentAccount() }
    }
    override fun render(state: AuthState) {
        val busy = state.phase == AuthPhase.WORKING
        findViewById<Button>(R.id.btnLogin).apply {
            isEnabled = !busy
            setText(if (state.phase == AuthPhase.PROFILE_RETRY) R.string.account_retry else R.string.btn_login)
        }
        findViewById<View>(R.id.tvRegisterLink).isEnabled = !busy
        findViewById<View>(R.id.forgotPasswordTextView).isEnabled = !busy
        findViewById<View>(R.id.buttonDifferentAccount).visibility = if (state.phase == AuthPhase.PROFILE_RETRY) View.VISIBLE else View.GONE
        findViewById<TextView>(R.id.authStatusTextView).text = if (busy) getString(R.string.account_working) else state.problem?.let { getString(it.messageResource()) }.orEmpty()
        resetDialog?.findViewById<Button>(R.id.forgotPasswordConfirmButton)?.isEnabled = !busy
    }
    @Suppress("UNUSED_PARAMETER")
    fun onForgotPasswordClick(view: View) {
        if (resetDialog?.isShowing == true) return
        val content = layoutInflater.inflate(R.layout.dialog_forgot_password, null)
        val dialog = AlertDialog.Builder(this).setTitle(R.string.forgot_password).setView(content).create()
        resetDialog = dialog
        content.findViewById<Button>(R.id.forgotPasswordConfirmButton).setOnClickListener {
            authModel.resetPassword(content.findViewById<EditText>(R.id.forgotPasswordEmailEditText).text.toString())
        }
        dialog.setOnDismissListener { resetDialog = null }
        dialog.show()
    }
    override fun onResetSent() { resetDialog?.dismiss() }
    override fun onDestroy() { resetDialog?.dismiss(); super.onDestroy() }
}
