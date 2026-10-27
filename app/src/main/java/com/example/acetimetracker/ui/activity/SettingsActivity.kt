package com.example.acetimetracker.ui.activity

import android.os.Bundle
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
                    onThemeChange = { theme -> settingsRepository.saveTheme(theme) },
                    onNotificationsChange = { enabled -> settingsRepository.saveNotificationsEnabled(enabled) }
                )
            }
        }
    }
}
