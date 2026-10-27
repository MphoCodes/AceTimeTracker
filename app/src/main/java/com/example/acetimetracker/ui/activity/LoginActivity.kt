package com.example.acetimetracker.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.acetimetracker.R
import com.example.acetimetracker.ui.viewmodel.AuthViewModel
import com.example.acetimetracker.util.Resource

class LoginActivity : AppCompatActivity() {

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnTestLogin = findViewById<Button>(R.id.btnTestLogin)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()
            viewModel.login(email, password)
        }

        btnTestLogin.setOnClickListener {
            viewModel.loginTestAccount()
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.loginState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    // Show progress if needed
                }
                is Resource.Success -> {
                    Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                is Resource.Error -> {
                    Toast.makeText(this, resource.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.passwordResetState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {}
                is Resource.Success -> {
                    Toast.makeText(this, "Password reset email sent.", Toast.LENGTH_SHORT).show()
                }
                is Resource.Error -> {
                    Toast.makeText(this, resource.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun onForgotPasswordClick(view: View) {
        showForgotPasswordDialog()
    }

    private fun showForgotPasswordDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_forgot_password, null)
        val emailEditText = dialogView.findViewById<EditText>(R.id.forgotPasswordEmailEditText)
        val confirmButton = dialogView.findViewById<Button>(R.id.forgotPasswordConfirmButton)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Forgot Password")
            .create()

        confirmButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            viewModel.sendPasswordReset(email)
            dialog.dismiss()
        }

        dialog.show()
    }
}
