package com.mphocodes.acetimetracker.data.repository

import android.content.pm.ApplicationInfo
import android.os.Looper
import com.mphocodes.acetimetracker.AceTimeTrackerApp
import com.mphocodes.acetimetracker.data.model.Category
import com.mphocodes.acetimetracker.data.model.TimesheetEntry
import com.mphocodes.acetimetracker.util.Resource
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
@Config(application = AceTimeTrackerApp::class, sdk = [36])
class DurableRepositoriesTest {
    @Before fun setup() {
        val app = RuntimeEnvironment.getApplication()
        if (FirebaseApp.getApps(app).isEmpty()) FirebaseApp.initializeApp(app, FirebaseOptions.Builder()
            .setApplicationId("1:123456789:android:1234567890abcdef").setApiKey("local-test-api-key")
            .setProjectId("demo-acetimetracker").setStorageBucket("demo-acetimetracker.appspot.com").build())
        app.applicationInfo.flags = app.applicationInfo.flags or ApplicationInfo.FLAG_DEBUGGABLE
        AuthRepository().loginTestAccount { }
    }
    @Test fun editingStableEntryPreservesPhotoAndAccountAndDeleteRemovesIt() {
        val repo = TimesheetRepository()
        val account = AuthRepository.TEST_USER_ID
        val entry = TimesheetEntry("durable-entry", "wrong-account", "Original", timeSpent = 60_000,
            imageUrl = "https://example.com/photo.jpg", description = "Before")
        assertTrue(await<Unit> { repo.saveTimesheetEntry(account, entry, it) } is Resource.Success)
        assertTrue(await<Unit> { repo.saveTimesheetEntry(account, entry.copy(description = "After"), it) } is Resource.Success)
        val rows = (await<List<TimesheetEntry>> { TimesheetRepository().fetchTimesheets(account, it) } as Resource.Success).data
        val saved = rows.single { it.id == "durable-entry" }
        assertEquals("After", saved.description)
        assertEquals(account, saved.userId)
        assertEquals("https://example.com/photo.jpg", saved.imageUrl)
        await<Unit> { repo.deleteTimesheetEntry(account, saved.id, it) }
        val remaining = (await<List<TimesheetEntry>> { repo.fetchTimesheets(account, it) } as Resource.Success).data
        assertFalse(remaining.any { it.id == saved.id })
    }
    @Test fun categoryRenameKeepsIdentityGoalsAndHistoricalEntryLabel() {
        val account = AuthRepository.TEST_USER_ID
        val categories = CategoryRepository()
        val category = Category("durable-category", "Original", "Detail", 1f, 3f, 2f, "#2563EB")
        await<String> { categories.addCategory(account, category, it) }
        await<Unit> { TimesheetRepository().saveTimesheetEntry(account, TimesheetEntry("category-entry", category = "Original"), it) }
        await<Unit> { categories.updateCategory(account, category.copy(name = "Renamed"), it) }
        val saved = (await<List<Category>> { categories.fetchCategories(account, it) } as Resource.Success).data.single { it.documentId == category.documentId }
        assertEquals("Renamed", saved.name)
        assertEquals(2f, saved.goal)
        val entry = (await<List<TimesheetEntry>> { TimesheetRepository().fetchTimesheets(account, it) } as Resource.Success).data.single { it.id == "category-entry" }
        assertEquals("Original", entry.category)
    }
    @Test fun mismatchedSessionCannotWriteOrReadAnotherAccountsData() {
        val repo = TimesheetRepository()
        assertTrue(await<Unit> { repo.saveTimesheetEntry("someone-else", TimesheetEntry("private"), it) } is Resource.Error)
        assertTrue(await<List<TimesheetEntry>> { repo.fetchTimesheets("someone-else", it) } is Resource.Error)
        assertTrue(await<Unit> { repo.deleteTimesheetEntry("someone-else", "private", it) } is Resource.Error)
    }

    private fun <T> await(start: ((Resource<T>) -> Unit) -> Unit): Resource<T> {
        val result = AtomicReference<Resource<T>?>()
        start { if (it !is Resource.Loading) result.set(it) }
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (result.get() == null && System.nanoTime() < deadline) {
            shadowOf(Looper.getMainLooper()).idle(); Thread.sleep(10)
        }
        return requireNotNull(result.get()) { "Repository callback did not complete" }
    }
}
