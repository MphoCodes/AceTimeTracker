package com.example.acetimetracker.ui.activity

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.acetimetracker.data.repository.SettingsRepository
import com.example.acetimetracker.ui.screen.SettingsScreen
import com.example.acetimetracker.ui.theme.AceTimeTrackerTheme

class SettingsActivity : ComponentActivity() {

    private lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        settingsRepository = SettingsRepository(this)

        val currentTheme = settingsRepository.getTheme()
        val notificationsEnabled = settingsRepository.isNotificationsEnabled()

        setContent {
            AceTimeTrackerTheme {
                SettingsScreen(
                    currentTheme = currentTheme,
                    initialNotificationsEnabled = notificationsEnabled,
                    onBackClick = { finish() },
                    onSaveSettings = { theme, notifications ->
                        settingsRepository.saveTheme(theme)
                        settingsRepository.saveNotificationsEnabled(notifications)
                        settingsRepository.applyTheme(theme)
                        Toast.makeText(this, "Settings saved!", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}
