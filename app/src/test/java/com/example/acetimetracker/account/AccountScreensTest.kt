package com.example.acetimetracker.account

import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import com.example.acetimetracker.*
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog

class AccountTestApplication : AceApplication() {
    val fixture = FakeAccounts()
    override val accounts get() = fixture.services
}
class GuardedTestActivity : AccountActivity()

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = AccountTestApplication::class)
class AccountScreensTest {
    private lateinit var f: FakeAccounts
    private val a = Account("a", "login@example.com")
    @Before fun fixture() { f = (RuntimeEnvironment.getApplication() as AccountTestApplication).fixture }
    private fun drain() = shadowOf(Looper.getMainLooper()).idle()
    private fun splash(): org.robolectric.android.controller.ActivityController<Splash> {
        val controller = Robolectric.buildActivity(Splash::class.java)
        // Replace only the decorative native GIF; inflate the real screen and exercise real routing.
        controller.get().layoutInflater.factory2 = object : android.view.LayoutInflater.Factory2 {
            override fun onCreateView(parent: android.view.View?, name: String, context: android.content.Context, attrs: android.util.AttributeSet): android.view.View? =
                if (name == "pl.droidsonroids.gif.GifImageView") android.widget.ImageView(context) else null
            override fun onCreateView(name: String, context: android.content.Context, attrs: android.util.AttributeSet) = onCreateView(null, name, context, attrs)
        }
        return controller
    }
    private fun assertRoot(intent: Intent, target: Class<*>) {
        assertEquals(target.name, intent.component!!.className)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_CLEAR_TASK != 0)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }
    @Test fun signedOutLaunchOpensLoginWithoutForcedDelay() {
        val c = splash().setup(); drain()
        assertRoot(shadowOf(c.get()).nextStartedActivity, Login::class.java); c.pause().stop().destroy()
    }
    @Test fun persistedSessionOpensHomeAfterProfileInitialization() {
        f.auth.account = a
        val c = splash().setup(); drain()
        assertRoot(shadowOf(c.get()).nextStartedActivity, MainActivity::class.java)
        assertEquals("a", f.profiles.ensures.single().first); c.pause().stop().destroy()
    }
    @Test fun protectedScreenRejectsSignedOutSession() {
        val c = Robolectric.buildActivity(GuardedTestActivity::class.java).setup(); drain()
        assertRoot(shadowOf(c.get()).nextStartedActivity, Login::class.java); c.pause().stop().destroy()
    }
    @Test fun everyAccountScreenRejectsSignedOutSessionBeforeAccessingFirebase() {
        val screens = listOf(MainActivity::class.java, profile::class.java, EditProfileActivity::class.java,
            Categories::class.java, Timer::class.java, Timesheet::class.java, Myprogress::class.java, GraphActivity::class.java)
        screens.forEach { screen ->
            val c = Robolectric.buildActivity(screen).setup(); drain()
            assertRoot(shadowOf(c.get()).nextStartedActivity, Login::class.java)
            c.pause().stop().destroy()
        }
    }
    @Test fun homeShowsAuthEmailAndLogoutClearsAccountTask() {
        f.auth.account = a
        f.profiles.ensureResult = Tasks.forResult(UserProfile("Existing", "stale@example.com", ""))
        val c = Robolectric.buildActivity(MainActivity::class.java).setup(); drain()
        val navigation = c.get().findViewById<com.google.android.material.navigation.NavigationView>(R.id.nav_view)
        assertEquals("login@example.com", navigation.getHeaderView(0).findViewById<android.widget.TextView>(R.id.nav_email_textview).text.toString())
        navigation.menu.performIdentifierAction(R.id.nav_logout, 0); drain()
        assertNull(f.auth.current)
        assertRoot(shadowOf(c.get()).nextStartedActivity, Login::class.java)
        c.pause().stop().destroy()
    }
    @Test fun rotatingDuringProfileSaveRetainsDraftAndDoesNotSubmitTwice() {
        f.auth.account = a
        val pending = TaskCompletionSource<Void>(); f.profiles.updateResult = pending.task
        val c = Robolectric.buildActivity(EditProfileActivity::class.java).setup(); drain()
        c.get().findViewById<EditText>(R.id.editDisplayNameEditText).setText("Draft")
        c.get().findViewById<Button>(R.id.saveProfileChangesButton).performClick(); drain()
        c.recreate(); drain()
        assertEquals("Draft", c.get().findViewById<EditText>(R.id.editDisplayNameEditText).text.toString())
        assertFalse(c.get().findViewById<Button>(R.id.saveProfileChangesButton).isEnabled)
        assertEquals(1, f.profiles.updates.size)
        pending.setResult(null); drain(); assertTrue(c.get().isFinishing)
        c.pause().stop().destroy()
    }
    @Test fun accountSwitchClearsOldScreenAndRemovesListener() {
        f.auth.account = a
        val c = Robolectric.buildActivity(GuardedTestActivity::class.java).setup()
        f.auth.change(Account("b")); drain()
        assertRoot(shadowOf(c.get()).nextStartedActivity, Splash::class.java)
        c.pause().stop().destroy(); assertTrue(f.auth.listeners.isEmpty())
    }
    @Test fun resumedScreenChecksOwnerBeforeDelayedAuthListenerNotification() {
        f.auth.account = a; f.auth.notifyInitially = false
        val c = Robolectric.buildActivity(GuardedTestActivity::class.java).setup()
        c.pause().stop()
        f.auth.account = Account("b")
        c.start().resume()
        assertRoot(shadowOf(c.get()).nextStartedActivity, Splash::class.java)
        c.pause().stop().destroy()
    }
    @Test fun homeIgnoresProfilePhotoResultAfterActivityIsDestroyed() {
        f.auth.account = a
        val pending = TaskCompletionSource<UserProfile>(); f.profiles.ensureResult = pending.task
        val c = Robolectric.buildActivity(MainActivity::class.java).setup(); drain()
        c.pause().stop().destroy()
        pending.setResult(UserProfile("Name", "old@example.com", "https://example.test/photo")); drain()
        assertTrue(c.get().isDestroyed)
    }
    @Test fun restoredScreenCannotReusePreviousAccountsViewState() {
        f.auth.account = a
        val original = Robolectric.buildActivity(GuardedTestActivity::class.java).setup()
        val saved = Bundle(); original.saveInstanceState(saved).pause().stop().destroy()
        f.auth.change(Account("b"))
        val c = Robolectric.buildActivity(GuardedTestActivity::class.java).create(saved).start().resume(); drain()
        assertRoot(shadowOf(c.get()).nextStartedActivity, Splash::class.java); c.pause().stop().destroy()
    }
    @Test fun textOnlyProfileEditPreservesPhotoAndEmailAndWaitsForSave() {
        f.auth.account = a
        val pending = TaskCompletionSource<Void>(); f.profiles.updateResult = pending.task
        val c = Robolectric.buildActivity(EditProfileActivity::class.java).setup(); drain()
        val activity = c.get()
        assertEquals("login@example.com", activity.findViewById<EditText>(R.id.editEmailEditText).text.toString())
        assertFalse(activity.findViewById<EditText>(R.id.editEmailEditText).isEnabled)
        activity.findViewById<EditText>(R.id.editDisplayNameEditText).setText("Edited")
        activity.findViewById<Button>(R.id.saveProfileChangesButton).performClick(); drain()
        assertEquals("a" to mapOf("displayName" to "Edited"), f.profiles.updates.single())
        assertFalse(activity.isFinishing)
        pending.setResult(null); drain(); assertTrue(activity.isFinishing)
        c.pause().stop().destroy()
    }
    @Test fun failedProfileSaveKeepsEditorOpenAndRetryEnabled() {
        f.auth.account = a; f.profiles.updateResult = Tasks.forException(IllegalStateException("offline"))
        val c = Robolectric.buildActivity(EditProfileActivity::class.java).setup(); drain()
        c.get().findViewById<EditText>(R.id.editDisplayNameEditText).setText("Draft")
        c.get().findViewById<Button>(R.id.saveProfileChangesButton).performClick(); drain()
        assertFalse(c.get().isFinishing)
        assertTrue(c.get().findViewById<Button>(R.id.saveProfileChangesButton).isEnabled)
        c.pause().stop().destroy()
    }
    @Test fun rotationDuringSignInDoesNotDuplicateAuthAndRoutesAfterProfileSetup() {
        val pending = TaskCompletionSource<Account>(); f.auth.signInResult = pending.task
        val c = Robolectric.buildActivity(Login::class.java).setup(); drain()
        c.get().findViewById<EditText>(R.id.etEmail).setText("a@example.com")
        c.get().findViewById<EditText>(R.id.etPassword).setText("password")
        c.get().findViewById<Button>(R.id.btnLogin).performClick(); drain(); c.recreate(); drain()
        assertEquals(1, f.auth.signInCalls)
        assertFalse(c.get().findViewById<Button>(R.id.btnLogin).isEnabled)
        pending.setResult(a); drain()
        assertRoot(shadowOf(c.get()).nextStartedActivity, MainActivity::class.java)
        c.pause().stop().destroy()
    }
    @Test fun registrationProfileFailureRetriesWithoutCreatingSecondAuthAccount() {
        f.profiles.ensureResult = Tasks.forException(IllegalStateException("offline"))
        val c = Robolectric.buildActivity(Registration::class.java).setup(); drain()
        c.get().findViewById<EditText>(R.id.etUsername).setText("Chosen name")
        c.get().findViewById<EditText>(R.id.etEmail).setText("a@example.com")
        c.get().findViewById<EditText>(R.id.etPassword).setText("password")
        c.get().findViewById<EditText>(R.id.etConfirmPassword).setText("password")
        c.get().findViewById<Button>(R.id.btnRegister).performClick(); drain()
        assertFalse(c.get().isFinishing)
        assertEquals(AuthPhase.PROFILE_RETRY, androidx.lifecycle.ViewModelProvider(c.get(), AccountViewModelFactory(f.services))[AuthViewModel::class.java].state.value!!.phase)
        f.profiles.ensureResult = Tasks.forResult(UserProfile())
        c.get().findViewById<Button>(R.id.btnRegister).performClick(); drain()
        assertEquals(1, f.auth.signInCalls)
        assertEquals("Chosen name", f.profiles.ensures.last().second)
        assertRoot(shadowOf(c.get()).nextStartedActivity, MainActivity::class.java)
        c.pause().stop().destroy()
    }
    @Test fun passwordResetFailureKeepsActualDialogForRetryThenDismissesOnSuccess() {
        val c = Robolectric.buildActivity(Login::class.java).setup(); drain()
        c.get().findViewById<android.view.View>(R.id.forgotPasswordTextView).performClick(); drain()
        val dialog = ShadowDialog.getLatestDialog()
        dialog.findViewById<EditText>(R.id.forgotPasswordEmailEditText).setText("a@example.com")
        f.auth.resetResult = Tasks.forException(IllegalStateException("offline"))
        dialog.findViewById<Button>(R.id.forgotPasswordConfirmButton).performClick(); drain()
        assertTrue(dialog.isShowing)
        assertTrue(dialog.findViewById<Button>(R.id.forgotPasswordConfirmButton).isEnabled)
        f.auth.resetResult = Tasks.forResult(null)
        dialog.findViewById<Button>(R.id.forgotPasswordConfirmButton).performClick(); drain()
        assertFalse(dialog.isShowing); c.pause().stop().destroy()
    }
}
