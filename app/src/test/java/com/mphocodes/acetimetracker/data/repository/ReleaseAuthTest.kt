package com.mphocodes.acetimetracker.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import org.robolectric.RuntimeEnvironment
import com.mphocodes.acetimetracker.AceTimeTrackerApp
import com.mphocodes.acetimetracker.data.model.AuthUser
import com.mphocodes.acetimetracker.util.Resource
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = AceTimeTrackerApp::class, sdk = [36])
class ReleaseAuthTest {
    private lateinit var context: Context
    private var originalFlags: Int = 0

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        originalFlags = context.applicationInfo.flags
        context.applicationInfo.flags = originalFlags and ApplicationInfo.FLAG_DEBUGGABLE.inv()
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setApplicationId("1:123456789:android:1234567890abcdef")
                    .setApiKey("local-test-api-key")
                    .setProjectId("demo-acetimetracker")
                    .build()
            )
        }
        FirebaseAuth.getInstance().signOut()
        context.getSharedPreferences("auth_session_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @After
    fun tearDown() {
        context.getSharedPreferences("auth_session_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
        FirebaseAuth.getInstance().signOut()
        context.applicationInfo.flags = originalFlags
    }

    @Test
    fun releaseRejectsQuickTestLogin() {
        val repository = AuthRepository()
        var result: Resource<AuthUser>? = null

        repository.loginTestAccount { result = it }

        assertTrue(result is Resource.Error)
        assertFalse(repository.isAuthenticated)
    }

    @Test
    fun releaseDoesNotResumeAStoredMockSession() {
        context.getSharedPreferences("auth_session_prefs", Context.MODE_PRIVATE).edit()
            .putString("mock_user_id", AuthRepository.TEST_USER_ID)
            .putString("mock_user_email", AuthRepository.TEST_USER_EMAIL)
            .commit()

        val repository = AuthRepository()

        assertFalse(repository.isMockSession)
        assertFalse(repository.isAuthenticated)
        assertTrue(repository.currentUserId.isEmpty())
    }

    @Test
    fun debugBuildCanStillUseQuickTestLogin() {
        context.applicationInfo.flags = context.applicationInfo.flags or ApplicationInfo.FLAG_DEBUGGABLE
        val repository = AuthRepository()
        var result: Resource<AuthUser>? = null

        repository.loginTestAccount { result = it }

        assertTrue(result is Resource.Success)
        assertTrue(repository.isMockSession)
        assertTrue(repository.isAuthenticated)
    }
}
