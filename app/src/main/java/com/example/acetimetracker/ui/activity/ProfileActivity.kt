package com.example.acetimetracker.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.acetimetracker.R
import com.example.acetimetracker.ui.viewmodel.ProfileViewModel
import com.example.acetimetracker.util.Resource

class ProfileActivity : AppCompatActivity() {

    private val profileViewModel: ProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val editProfileButton = findViewById<Button>(R.id.editProfileButton)

        btnBack.setOnClickListener {
            onBackPressed()
        }

        editProfileButton.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        observeProfile()
    }

    override fun onResume() {
        super.onResume()
        profileViewModel.loadUserProfile()
    }

    private fun observeProfile() {
        val displayNameTextView = findViewById<TextView>(R.id.displayNameTextView)
        val emailTextView = findViewById<TextView>(R.id.emailTextView)
        val profileImageView = findViewById<ImageView>(R.id.profileImageView)

        profileViewModel.userProfileState.observe(this) { resource ->
            if (resource is Resource.Success) {
                val profile = resource.data
                displayNameTextView.text = if (profile.displayName.isNotEmpty()) profile.displayName else "Display Name"
                emailTextView.text = if (profile.email.isNotEmpty()) profile.email else "Email Address"

                if (profile.profilePictureUrl.isNotEmpty()) {
                    Glide.with(this)
                        .load(profile.profilePictureUrl)
                        .transform(CircleCrop())
                        .into(profileImageView)
                } else {
                    profileImageView.setImageResource(R.drawable.ic_account_circle)
                }
            }
        }
    }
}
