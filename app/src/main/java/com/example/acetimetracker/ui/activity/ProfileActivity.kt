package com.example.acetimetracker.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import com.example.acetimetracker.ui.screen.ProfileScreen
import com.example.acetimetracker.ui.theme.AceTimeTrackerTheme
import com.example.acetimetracker.ui.viewmodel.ProfileViewModel

class ProfileActivity : ComponentActivity() {

    private val profileViewModel: ProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AceTimeTrackerTheme {
                val profileState by profileViewModel.userProfileState.observeAsState()

                ProfileScreen(
                    profileState = profileState,
                    onBackClick = { finish() },
                    onEditProfileClick = {
                        startActivity(Intent(this, EditProfileActivity::class.java))
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        profileViewModel.loadUserProfile()
    }
}
