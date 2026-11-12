package com.example.acetimetracker.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Looper
import com.example.acetimetracker.AceTimeTrackerApp
import com.example.acetimetracker.util.Resource
import com.example.acetimetracker.ui.viewmodel.ProfileViewModel
import com.example.acetimetracker.ui.viewmodel.TimesheetViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(application = AceTimeTrackerApp::class, sdk = [36])
@LooperMode(LooperMode.Mode.PAUSED)
class ImageUploadFailureTest {
    private lateinit var context: Context
    private lateinit var firebaseApp: FirebaseApp
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private var originalFlags: Int = 0

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        originalFlags = context.applicationInfo.flags
        context.getSharedPreferences("auth_session_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        firebaseApp = FirebaseApp.initializeApp(
            context,
            FirebaseOptions.Builder()
                .setApplicationId("1:123456789:android:1234567890abcdef")
                .setApiKey("local-test-api-key")
                .setProjectId("demo-acetimetracker")
                .setStorageBucket("demo-acetimetracker.appspot.com")
                .build(),
            "upload-test-${UUID.randomUUID()}"
        )
        firestore = FirebaseFirestore.getInstance(firebaseApp)
        storage = FirebaseStorage.getInstance(firebaseApp)
        storage.useEmulator("127.0.0.1", 9199)
        storage.setMaxUploadRetryTimeMillis(500)
    }

    @After
    fun tearDown() {
        context.getSharedPreferences("auth_session_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        context.applicationInfo.flags = originalFlags
        firebaseApp.delete()
    }

    @Test
    fun missingTimesheetPhotoIsReportedAsFailure() {
        val repository = TimesheetRepository(firestore, storage)
        val result = awaitUpload { callback -> repository.uploadImage(missingImage(), callback) }

        assertTrue("A missing photo must not be reported as uploaded", result is Resource.Error)
    }

    @Test
    fun missingProfilePhotoIsReportedAsFailure() {
        val repository = UserRepository(firestore, storage)
        val result = awaitUpload { callback -> repository.uploadProfilePicture(missingImage(), callback) }

        assertTrue("A missing photo must not be reported as uploaded", result is Resource.Error)
    }

    @Test
    fun selectingANewTimesheetPhotoClearsThePreviousPhoto() {
        context.applicationInfo.flags = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE.inv()
        val auth = AuthRepository(FirebaseAuth.getInstance(firebaseApp))
        val viewModel = TimesheetViewModel(
            TimesheetRepository(firestore, storage),
            CategoryRepository(firestore),
            auth
        )
        viewModel.selectedImageUrl = "https://example.com/previous-photo.jpg"

        viewModel.uploadImage(missingImage())

        assertEquals("", viewModel.selectedImageUrl)
    }

    @Test
    fun latestTimesheetPhotoWinsWhenUploadsFinishOutOfOrder() {
        val callbacks = mutableListOf<(Resource<String>) -> Unit>()
        val viewModel = timesheetViewModel { _, callback ->
            callbacks.add(callback)
            callback(Resource.Loading)
        }

        viewModel.uploadImage(Uri.parse("content://test/first"))
        viewModel.uploadImage(Uri.parse("content://test/second"))
        callbacks[1](Resource.Success("https://example.com/second.jpg"))
        callbacks[0](Resource.Success("https://example.com/first.jpg"))

        assertEquals("https://example.com/second.jpg", viewModel.selectedImageUrl)
        assertEquals(Resource.Success("https://example.com/second.jpg"), viewModel.uploadImageState.value)
    }

    @Test
    fun removedTimesheetPhotoStaysRemovedWhenUploadFinishes() {
        val callbacks = mutableListOf<(Resource<String>) -> Unit>()
        val viewModel = timesheetViewModel { _, callback ->
            callbacks.add(callback)
            callback(Resource.Loading)
        }

        viewModel.uploadImage(Uri.parse("content://test/first"))
        viewModel.clearSelectedImage()
        callbacks.single()(Resource.Success("https://example.com/first.jpg"))

        assertEquals("", viewModel.selectedImageUrl)
        assertNull(viewModel.uploadImageState.value)
    }

    @Test
    fun profileCannotSaveWhilePhotoIsUploading() {
        context.applicationInfo.flags = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE.inv()
        val viewModel = ProfileViewModel(
            UserRepository(firestore, storage),
            AuthRepository(FirebaseAuth.getInstance(firebaseApp)),
            imageUploader = { _, callback -> callback(Resource.Loading) }
        )
        viewModel.uploadProfilePicture(Uri.parse("content://test/photo"))

        viewModel.updateUserProfile("Name", "user@example.com", "https://example.com/old.jpg")

        val error = viewModel.updateProfileState.value as? Resource.Error
        assertEquals("Please wait for the photo to finish uploading", error?.message)
    }

    @Test
    fun latestProfilePhotoWinsWhenUploadsFinishOutOfOrder() {
        val callbacks = mutableListOf<(Resource<String>) -> Unit>()
        context.applicationInfo.flags = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE.inv()
        val viewModel = ProfileViewModel(
            UserRepository(firestore, storage),
            AuthRepository(FirebaseAuth.getInstance(firebaseApp)),
            imageUploader = { _, callback ->
                callbacks.add(callback)
                callback(Resource.Loading)
            }
        )

        viewModel.uploadProfilePicture(Uri.parse("content://test/first"))
        viewModel.uploadProfilePicture(Uri.parse("content://test/second"))
        callbacks[1](Resource.Success("https://example.com/second.jpg"))
        callbacks[0](Resource.Error("old upload failed"))

        assertEquals(Resource.Success("https://example.com/second.jpg"), viewModel.uploadImageState.value)
    }

    @Test
    fun debugMockSessionKeepsTimesheetPhotoLocally() {
        val auth = debugMockAuth()
        val viewModel = TimesheetViewModel(
            TimesheetRepository(firestore, storage),
            CategoryRepository(firestore),
            auth
        )
        val image = localImage()

        viewModel.uploadImage(image)

        assertEquals(image.toString(), viewModel.selectedImageUrl)
    }

    @Test
    fun debugMockSessionKeepsProfilePhotoLocally() {
        val auth = debugMockAuth()
        val viewModel = ProfileViewModel(UserRepository(firestore, storage), auth)
        val image = localImage()

        viewModel.uploadProfilePicture(image)

        val state = viewModel.uploadImageState.value as? Resource.Success
        assertEquals(image.toString(), state?.data)
    }

    private fun timesheetViewModel(
        uploader: (Uri, (Resource<String>) -> Unit) -> Unit
    ): TimesheetViewModel {
        context.applicationInfo.flags = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE.inv()
        return TimesheetViewModel(
            TimesheetRepository(firestore, storage),
            CategoryRepository(firestore),
            AuthRepository(FirebaseAuth.getInstance(firebaseApp)),
            imageUploader = uploader
        )
    }

    private fun debugMockAuth(): AuthRepository {
        context.applicationInfo.flags = context.applicationInfo.flags or ApplicationInfo.FLAG_DEBUGGABLE
        return AuthRepository(FirebaseAuth.getInstance(firebaseApp)).also {
            it.loginTestAccount { }
        }
    }

    private fun localImage(): Uri {
        val file = File(context.cacheDir, "mock-photo-${UUID.randomUUID()}.jpg")
        file.writeBytes(byteArrayOf(1))
        return Uri.fromFile(file)
    }

    private fun missingImage(): Uri =
        Uri.fromFile(File(context.cacheDir, "missing-photo-${UUID.randomUUID()}.jpg"))

    private fun awaitUpload(start: (((Resource<String>) -> Unit) -> Unit)): Resource<String> {
        val result = AtomicReference<Resource<String>?>()
        start { state ->
            if (state !is Resource.Loading) result.set(state)
        }
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (result.get() == null && System.nanoTime() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(20)
        }
        shadowOf(Looper.getMainLooper()).idle()
        assertNotNull("The upload did not report a terminal result", result.get())
        return requireNotNull(result.get())
    }
}
