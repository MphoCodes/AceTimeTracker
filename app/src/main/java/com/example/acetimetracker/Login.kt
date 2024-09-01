package com.example.acetimetracker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class Login : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var etEmail: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Get references to UI elements
        etEmail = findViewById(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        // Initialize Firebase Auth instance
        auth = FirebaseAuth.getInstance()

        // Handle login button click
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()

            // Validate user input (optional)
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in both fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Attempt user login with Firebase Auth
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Login successful
                        Log.d("Login", "signInWithEmailAndPassword:success")
                        val user = auth.currentUser

                        // Handle successful login (e.g., navigate to another activity)
                        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()  //
                    } else {
                        // Login failed
                        Log.w("Login", "signInWithEmailAndPassword:failure", task.exception)
                        val errorMessage = task.exception?.message.toString()
                        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    // Handle Forgot Password Click
    fun onForgotPasswordClick(view: View) {
        showForgotPasswordDialog()
    }

    // Show Forgot Password Dialog
    private fun showForgotPasswordDialog() {
        val builder = AlertDialog.Builder(this)
        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.dialog_forgot_password, null) // Inflate the layout
        builder.setView(dialogView)
            .setTitle("Forgot Password")

        // Get references to the dialog's UI elements
        val forgotPasswordEmailEditText =
            dialogView.findViewById<EditText>(R.id.forgotPasswordEmailEditText)
        val forgotPasswordConfirmButton =
            dialogView.findViewById<Button>(R.id.forgotPasswordConfirmButton)

        // Handle the Confirm button click
        forgotPasswordConfirmButton.setOnClickListener {
            val email = forgotPasswordEmailEditText.text.toString()
            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Firebase.auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Log.d("Login", "Email sent.")
                        Toast.makeText(this, "Password reset email sent.", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        Log.w("Login", "sendEmailVerification", task.exception)
                        Toast.makeText(this, "Failed to send reset email.", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            // Dismiss the dialog after confirming
            builder.create().dismiss()
        }

        // Create and show the dialog
        builder.create().show()
    }
}