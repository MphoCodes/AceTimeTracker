package com.example.acetimetracker.ui.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.acetimetracker.R
import com.example.acetimetracker.ui.viewmodel.ProfileViewModel
import com.example.acetimetracker.util.Resource

class EditProfileActivity : AppCompatActivity() {

    private val profileViewModel: ProfileViewModel by viewModels()
    private var profilePictureUrl: String = ""
    private lateinit var profileImageView: ImageView
    private lateinit var loadingAnimation: LottieAnimationView

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val selectedUri = result.data?.data
                if (selectedUri != null) {
                    profileViewModel.uploadProfilePicture(selectedUri)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        val editDisplayNameEditText = findViewById<EditText>(R.id.editDisplayNameEditText)
        val editEmailEditText = findViewById<EditText>(R.id.editEmailEditText)
        val uploadProfilePictureButton = findViewById<Button>(R.id.uploadProfilePictureButton)
        val saveProfileChangesButton = findViewById<Button>(R.id.saveProfileChangesButton)

        profileImageView = findViewById(R.id.profileImageView)
        loadingAnimation = findViewById(R.id.profilePictureLoadingAnimation)

        uploadProfilePictureButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "image/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            imagePickerLauncher.launch(intent)
        }

        saveProfileChangesButton.setOnClickListener {
            val displayName = editDisplayNameEditText.text.toString().trim()
            val email = editEmailEditText.text.toString().trim()
            profileViewModel.updateUserProfile(displayName, email, profilePictureUrl)
        }

        observeViewModel(editDisplayNameEditText, editEmailEditText)
        profileViewModel.loadUserProfile()
    }

    private fun observeViewModel(nameEdit: EditText, emailEdit: EditText) {
        profileViewModel.userProfileState.observe(this) { resource ->
            if (resource is Resource.Success) {
                val profile = resource.data
                nameEdit.setText(profile.displayName)
                emailEdit.setText(profile.email)
                profilePictureUrl = profile.profilePictureUrl

                if (profile.profilePictureUrl.isNotEmpty()) {
                    Glide.with(this)
                        .load(profile.profilePictureUrl)
                        .transform(CircleCrop())
                        .into(profileImageView)
                }
            }
        }

        profileViewModel.uploadImageState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    loadingAnimation.visibility = View.VISIBLE
                    loadingAnimation.repeatCount = LottieDrawable.INFINITE
                    loadingAnimation.playAnimation()
                }
                is Resource.Success -> {
                    loadingAnimation.cancelAnimation()
                    loadingAnimation.visibility = View.INVISIBLE
                    profilePictureUrl = resource.data
                    Glide.with(this)
                        .load(profilePictureUrl)
                        .transform(CircleCrop())
                        .into(profileImageView)
                    Toast.makeText(this, "Image uploaded successfully!", Toast.LENGTH_SHORT).show()
                }
                is Resource.Error -> {
                    loadingAnimation.cancelAnimation()
                    loadingAnimation.visibility = View.INVISIBLE
                    Toast.makeText(this, resource.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        profileViewModel.updateProfileState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {}
                is Resource.Success -> {
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                is Resource.Error -> {
                    Toast.makeText(this, resource.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
