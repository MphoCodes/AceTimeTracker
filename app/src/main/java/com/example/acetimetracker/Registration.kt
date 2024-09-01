package com.example.acetimetracker

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import android.util.Log
import android.content.Intent
import android.graphics.Typeface
import android.text.SpannableString
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth

class Registration : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth  // Declare Firebase Authentication instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)


        // Get references to UI elements
        val etUsername = findViewById<EditText>(R.id.etUsername)  // Replace with your username field id
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val tvLoginLink = findViewById<TextView>(R.id.tvLoginLink)
        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()


        // Underline and make "Login here" text bold
        val loginLinkText = SpannableString(tvLoginLink.text)
        loginLinkText.setSpan(UnderlineSpan(), 0, loginLinkText.length, 0)
        loginLinkText.setSpan(StyleSpan(Typeface.BOLD), 0, loginLinkText.length, 0)
        tvLoginLink.text = loginLinkText

        tvLoginLink.setOnClickListener {
            // Navigate to the Login activity
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
        }
        // Handle register button click
        btnRegister.setOnClickListener {
            val username = etUsername.text.toString()  // Get username (optional)
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()

            // Validate user input (optional, implement your validation logic here)
            if (email.isEmpty() || password.isEmpty() || confirmPassword != password) {
                Toast.makeText(this, "Please fill in all fields and confirm password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Attempt user registration with Firebase Auth
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Registration successful
                        Log.d("Registration", "createUserWithEmailAndPassword:success")
                        val user = auth.currentUser

                        // Handle successful registration (e.g., navigate to another activity)
                        Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                        // Navigate to the Login activity
                        val intent = Intent(this, Login::class.java)
                        startActivity(intent)
                        finish() // Optional: Close the current activity
                    } else {
                        // Registration failed
                        Log.w("Registration", "createUserWithEmailAndPassword:failure", task.exception)
                        val errorMessage = task.exception?.message.toString()
                        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}
