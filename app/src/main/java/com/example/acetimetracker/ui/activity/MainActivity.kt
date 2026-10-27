package com.example.acetimetracker.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import com.example.acetimetracker.data.model.UserProfile
import com.example.acetimetracker.data.repository.AuthRepository
import com.example.acetimetracker.ui.screen.Destination
import com.example.acetimetracker.ui.screen.MainScreen
import com.example.acetimetracker.ui.theme.AceTimeTrackerTheme
import com.example.acetimetracker.ui.viewmodel.ProfileViewModel
import com.example.acetimetracker.util.Resource

class MainActivity : ComponentActivity() {

    private val profileViewModel: ProfileViewModel by viewModels()
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        profileViewModel.loadUserProfile()

        setContent {
            AceTimeTrackerTheme {
                val profileState by profileViewModel.userProfileState.observeAsState()
                val profile = (profileState as? Resource.Success<UserProfile>)?.data

                MainScreen(
                    userProfile = profile,
                    onNavigate = { destination ->
                        handleNavigation(destination)
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        profileViewModel.loadUserProfile()
    }

    private fun handleNavigation(destination: Destination) {
        when (destination) {
            Destination.Profile -> startActivity(Intent(this, ProfileActivity::class.java))
            Destination.Categories -> startActivity(Intent(this, CategoriesActivity::class.java))
            Destination.Timesheet -> startActivity(Intent(this, TimesheetActivity::class.java))
            Destination.Timer -> startActivity(Intent(this, TimerActivity::class.java))
            Destination.MyProgress -> startActivity(Intent(this, MyProgressActivity::class.java))
            Destination.Graph -> startActivity(Intent(this, GraphActivity::class.java))
            Destination.Settings -> startActivity(Intent(this, SettingsActivity::class.java))
            Destination.About -> startActivity(Intent(this, AboutActivity::class.java))
            Destination.Logout -> {
                authRepository.logout()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }
    }
}
