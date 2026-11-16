package com.mphocodes.acetimetracker.data.repository

import android.content.pm.ApplicationInfo
import android.os.Looper
import org.robolectric.Shadows.shadowOf
import com.mphocodes.acetimetracker.AceTimeTrackerApp
import com.mphocodes.acetimetracker.data.model.TimesheetEntry
import com.mphocodes.acetimetracker.util.Resource
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = AceTimeTrackerApp::class, sdk = [36])
class TimerSaveRetryTest {
    @Before fun setup() {
        val app = RuntimeEnvironment.getApplication()
        if (FirebaseApp.getApps(app).isEmpty()) {
            FirebaseApp.initializeApp(app, FirebaseOptions.Builder()
                .setApplicationId("1:123456789:android:1234567890abcdef")
                .setApiKey("local-test-api-key").setProjectId("demo-acetimetracker")
                .setStorageBucket("demo-acetimetracker.appspot.com").build())
        }
    }

    @Test fun retryingOneSessionDoesNotDuplicateItsDuration() {
        val app = RuntimeEnvironment.getApplication()
        app.applicationInfo.flags = app.applicationInfo.flags or ApplicationInfo.FLAG_DEBUGGABLE
        AuthRepository().loginTestAccount { }
        val repository = TimesheetRepository()
        val entry = TimesheetEntry(id = "retry-session", category = "Retry test", timeSpent = 60_000L)
        repository.saveTimesheetEntry(AuthRepository.TEST_USER_ID, entry) {}
        repository.saveTimesheetEntry(AuthRepository.TEST_USER_ID, entry) {}
        var entries = emptyList<TimesheetEntry>()
        repository.fetchTimesheets(AuthRepository.TEST_USER_ID) {
            if (it is Resource.Success) entries = it.data
        }
        val deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5)
        while (entries.none { it.category == "Retry test" } && System.nanoTime() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        assertEquals(1, entries.count { it.category == "Retry test" })
        assertEquals("retry-session", entries.single { it.category == "Retry test" }.id)
    }
}
