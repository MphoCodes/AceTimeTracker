package com.mphocodes.acetimetracker.ui.viewmodel

import android.content.pm.ApplicationInfo
import com.mphocodes.acetimetracker.AceTimeTrackerApp
import com.mphocodes.acetimetracker.data.repository.AuthRepository
import com.mphocodes.acetimetracker.util.Resource
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = AceTimeTrackerApp::class, sdk = [36])
class CategoryValidationTest {
    @Before fun setup() {
        val app = RuntimeEnvironment.getApplication()
        app.applicationInfo.flags = app.applicationInfo.flags or ApplicationInfo.FLAG_DEBUGGABLE
        if (com.google.firebase.FirebaseApp.getApps(app).isEmpty()) {
            com.google.firebase.FirebaseApp.initializeApp(app, com.google.firebase.FirebaseOptions.Builder()
                .setApplicationId("1:123456789:android:1234567890abcdef")
                .setApiKey("local-test-api-key")
                .setProjectId("demo-acetimetracker")
                .setStorageBucket("demo-acetimetracker.appspot.com")
                .build())
        }
        AuthRepository().loginTestAccount {}
    }

    @Test fun rejectsNonFiniteTargets() {
        for (value in listOf(Float.NaN, Float.POSITIVE_INFINITY)) {
            val vm = CategoryViewModel()
            vm.addCategory("Invalid $value", "", value, null, null)
            assertTrue("Nonfinite target must be rejected", vm.addCategoryState.value is Resource.Error)
        }
    }

    @Test fun rejectsTargetOutsideItsRange() {
        val vm = CategoryViewModel()
        vm.addCategory("Outside range", "", 8f, 1f, 4f)
        assertTrue(vm.addCategoryState.value is Resource.Error)
    }
}
