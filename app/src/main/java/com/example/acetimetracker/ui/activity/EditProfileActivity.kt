package com.example.acetimetracker.ui.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import com.example.acetimetracker.data.model.UserProfile
import com.example.acetimetracker.ui.screen.EditProfileScreen
import com.example.acetimetracker.ui.theme.AceTimeTrackerTheme
import com.example.acetimetracker.ui.viewmodel.ProfileViewModel
import com.example.acetimetracker.util.Resource

class EditProfileActivity : ComponentActivity() {

    private val profileViewModel: ProfileViewModel by viewModels()

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

        observeViewModel()

        setContent {
            AceTimeTrackerTheme {
                val profileState by profileViewModel.userProfileState.observeAsState()
                val uploadState by profileViewModel.uploadImageState.observeAsState()
                val updateState by profileViewModel.updateProfileState.observeAsState()

                EditProfileScreen(
                    profileState = profileState,
                    uploadState = uploadState,
                    updateState = updateState,
                    onBackClick = { finish() },
                    onUploadPhotoClick = {
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                            type = "image/*"
                            addCategory(Intent.CATEGORY_OPENABLE)
                        }
                        imagePickerLauncher.launch(intent)
                    },
                    onSaveClick = { displayName, email ->
                        val currentPic = (uploadState as? Resource.Success<String>)?.data
                            ?: (profileState as? Resource.Success<UserProfile>)?.data?.profilePictureUrl.orEmpty()
                        profileViewModel.updateUserProfile(displayName, email, currentPic)
                    }
                )
            }
        }

        profileViewModel.loadUserProfile()
    }

    private fun observeViewModel() {
        profileViewModel.uploadImageState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {}
                is Resource.Success -> {
                    Toast.makeText(this, "Image uploaded successfully!", Toast.LENGTH_SHORT).show()
                }
                is Resource.Error -> {
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
