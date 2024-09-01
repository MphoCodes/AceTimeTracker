package com.example.acetimetracker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class profile : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener {
            onBackPressed()
        }

        val editProfileButton = findViewById<Button>(R.id.editProfileButton)
        editProfileButton.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivity(intent)
        }

        fetchUserData()
    }

    private fun fetchUserData() {
        val userId = auth.currentUser?.uid ?: ""
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                val displayName = document.getString("displayName")
                val email = document.getString("email")
                val profilePictureUrl = document.getString("profilePictureUrl")

                val displayNameTextView = findViewById<TextView>(R.id.displayNameTextView)
                val emailTextView = findViewById<TextView>(R.id.emailTextView)
                val profileImageView = findViewById<ImageView>(R.id.profileImageView)

                displayNameTextView.text = displayName ?: "Display Name"
                emailTextView.text = email ?: "Email Address"

                // Load the profile picture using Glide (if available)
                if (profilePictureUrl != null) {
                    Glide.with(this)
                        .load(profilePictureUrl)
                        .transform(CircleCrop()) // Apply the circular transformation
                        .into(profileImageView)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("profileActivity", "Error fetching user data: ${exception.message}", exception)
            }

            .addOnFailureListener { exception ->
                Log.e("profileActivity", "Error fetching user data: ${exception.message}", exception)
            }
    }
}