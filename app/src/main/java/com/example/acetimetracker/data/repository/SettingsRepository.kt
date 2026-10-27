package com.example.acetimetracker.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class SettingsRepository(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "app_settings"
        private const val KEY_THEME = "theme"
        private const val KEY_NOTIFICATIONS = "notifications"

        const val THEME_SYSTEM = "system"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
    }

    fun getTheme(): String {
        return sharedPreferences.getString(KEY_THEME, THEME_SYSTEM) ?: THEME_SYSTEM
    }

    fun saveTheme(theme: String) {
        sharedPreferences.edit().putString(KEY_THEME, theme).apply()
        ThemeState.theme = theme
    }

    fun isNotificationsEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_NOTIFICATIONS, true)
    }

    fun saveNotificationsEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply()
    }

    /** Loads the persisted theme into [ThemeState] so every activity's Compose theme picks it up. */
    fun applyTheme(theme: String = getTheme()) {
        ThemeState.theme = theme
    }
}

/** App-wide, observable theme preference read by [com.example.acetimetracker.ui.theme.AceTimeTrackerTheme]. */
object ThemeState {
    var theme by mutableStateOf(SettingsRepository.THEME_SYSTEM)
}
