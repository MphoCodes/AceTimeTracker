package com.example.acetimetracker

import android.app.Application
import android.content.Context
import com.example.acetimetracker.data.repository.SettingsRepository

class AceTimeTrackerApp : Application() {

    companion object {
        lateinit var instance: AceTimeTrackerApp
            private set

        val context: Context
            get() = instance.applicationContext
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        SettingsRepository(this).applyTheme()
    }
}
