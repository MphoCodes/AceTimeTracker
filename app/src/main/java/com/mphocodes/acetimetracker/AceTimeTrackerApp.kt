package com.mphocodes.acetimetracker

import android.app.Application
import android.content.Context
import com.mphocodes.acetimetracker.data.repository.SettingsRepository

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
        // Private photos never enter a shared disk/memory cache, including in-flight requests.
        coil.Coil.setImageLoader(coil.ImageLoader.Builder(this)
            .diskCachePolicy(coil.request.CachePolicy.DISABLED)
            .memoryCachePolicy(coil.request.CachePolicy.DISABLED).build())
        // DurableStore owns offline data. Avoid a second, undeletable per-account disk cache.
        if (com.google.firebase.FirebaseApp.getApps(this).isNotEmpty()) {
            // Called before the first Firestore operation; removes caches from older versions.
            com.google.firebase.firestore.FirebaseFirestore.getInstance().clearPersistence()
            com.google.firebase.firestore.FirebaseFirestore.getInstance().firestoreSettings =
            com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(com.google.firebase.firestore.MemoryCacheSettings.newBuilder().build())
                .build()
        }
        if (com.google.firebase.FirebaseApp.getApps(this).isNotEmpty())
            com.mphocodes.acetimetracker.data.repository.AccountLocalCleanup.recover(this)
        SettingsRepository(this).applyTheme()
        com.mphocodes.acetimetracker.reminder.ReminderScheduler.reschedule(this)
    }
}
