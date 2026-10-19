package com.example.acetimetracker.account

import android.net.Uri
import android.os.Looper
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
class AccountViewModelsTest {
    private val f = FakeAccounts()
    private val a = Account("a", "a@example.com")
    private fun drain() = shadowOf(Looper.getMainLooper()).idle()

    @Test fun signedOutStartupDoesNotReadProfiles() {
        val vm = AuthViewModel(f.services); vm.resume(); drain()
        assertEquals(AuthPhase.SIGNED_OUT, vm.state.value!!.phase)
        assertTrue(f.profiles.ensures.isEmpty())
    }
    @Test fun latePersistedIdentityRoutesWithoutAnotherActivityStart() {
        val vm = AuthViewModel(f.services); vm.resume(); drain()
        f.auth.change(a); drain()
        assertEquals(AuthPhase.READY, vm.state.value!!.phase)
        assertEquals("a", vm.state.value!!.uid)
    }
    @Test fun authNotificationBeforeRegistrationCompletionDoesNotLoseChosenName() {
        val pending = TaskCompletionSource<Account>(); f.auth.signInResult = pending.task
        val vm = AuthViewModel(f.services); vm.register("Chosen", "a@example.com", "password", "password")
        f.auth.change(a); drain()
        assertTrue(f.profiles.ensures.isEmpty())
        pending.setResult(a); drain()
        assertEquals(listOf("a" to "Chosen"), f.profiles.ensures)
        assertEquals(AuthPhase.READY, vm.state.value!!.phase)
    }
    @Test fun sameAccountNotificationDoesNotAutoRetryFailedProfile() {
        f.auth.account = a; f.profiles.ensureResult = Tasks.forException(IllegalStateException("offline"))
        val vm = AuthViewModel(f.services); vm.resume(); drain()
        val attempts = f.profiles.ensures.size
        f.auth.change(a); drain()
        assertEquals(attempts, f.profiles.ensures.size)
        assertEquals(AuthPhase.PROFILE_RETRY, vm.state.value!!.phase)
    }
    @Test fun identityRestoredDuringPasswordResetIsReconciledWhenResetCompletes() {
        val pending = TaskCompletionSource<Void>(); f.auth.resetResult = pending.task
        val vm = AuthViewModel(f.services); vm.resetPassword("a@example.com")
        f.auth.change(a); drain(); assertTrue(f.profiles.ensures.isEmpty())
        pending.setResult(null); drain()
        assertEquals(AuthPhase.READY, vm.state.value!!.phase)
        assertEquals("a", vm.state.value!!.uid)
    }
    @Test fun identityRestoredDuringFailedSignInIsReconciledAfterFailure() {
        val pending = TaskCompletionSource<Account>(); f.auth.signInResult = pending.task
        val vm = AuthViewModel(f.services); vm.signIn("other@example.com", "password")
        f.auth.change(a); drain(); assertTrue(f.profiles.ensures.isEmpty())
        pending.setException(IllegalStateException("failed sign-in")); drain()
        assertEquals(AuthPhase.READY, vm.state.value!!.phase)
        assertEquals("a", vm.state.value!!.uid)
    }
    @Test fun authListenerIsRemovedWhenViewModelIsCleared() {
        val vm = AuthViewModel(f.services)
        assertEquals(1, f.auth.listeners.size)
        val store = androidx.lifecycle.ViewModelStore(); store.put("auth", vm); store.clear()
        assertTrue(f.auth.listeners.isEmpty())
        f.auth.change(a); drain(); assertTrue(f.profiles.ensures.isEmpty())
    }
    @Test fun signedInStartupInitializesBeforeRouting() {
        f.auth.account = a
        val pending = TaskCompletionSource<UserProfile>(); f.profiles.ensureResult = pending.task
        val vm = AuthViewModel(f.services); vm.resume(); drain()
        assertEquals(AuthPhase.WORKING, vm.state.value!!.phase)
        pending.setResult(UserProfile()); drain()
        assertEquals(AuthPhase.READY, vm.state.value!!.phase)
        assertEquals("a", vm.state.value!!.uid)
    }
    @Test fun registrationInitializesChosenNameAndDoesNotRecreateAccountOnProfileRetry() {
        f.profiles.ensureResult = Tasks.forException(IllegalStateException("offline"))
        val vm = AuthViewModel(f.services); vm.register(" Name ", "a@example.com", "password", "password"); drain()
        assertEquals(AuthPhase.PROFILE_RETRY, vm.state.value!!.phase)
        assertEquals("a", f.auth.current!!.uid)
        f.profiles.ensureResult = Tasks.forResult(UserProfile()); vm.resume(); drain()
        assertEquals(1, f.auth.signInCalls)
        assertEquals("Name", f.profiles.ensures.last().second)
        assertEquals(AuthPhase.READY, vm.state.value!!.phase)
    }
    @Test fun duplicateSignInWhileBusyDoesNotIssueAnotherRequest() {
        f.auth.signInResult = TaskCompletionSource<Account>().task
        val vm = AuthViewModel(f.services); vm.signIn("a@example.com", "password"); vm.signIn("a@example.com", "password")
        assertEquals(1, f.auth.signInCalls)
    }
    @Test fun failedSignInDoesNotReadOrInitializeProfile() {
        f.auth.signInResult = Tasks.forException(IllegalStateException("invalid"))
        val vm = AuthViewModel(f.services); vm.signIn("a@example.com", "password"); drain()
        assertEquals(AuthPhase.ERROR, vm.state.value!!.phase)
        assertTrue(f.profiles.ensures.isEmpty())
    }
    @Test fun profileCompletionAfterSignOutCannotRouteOldAccount() {
        f.auth.account = a
        val pending = TaskCompletionSource<UserProfile>(); f.profiles.ensureResult = pending.task
        val vm = AuthViewModel(f.services); vm.resume(); vm.useDifferentAccount(); pending.setResult(UserProfile()); drain()
        assertEquals(AuthPhase.SIGNED_OUT, vm.state.value!!.phase)
        assertNull(f.auth.current)
    }
    @Test fun resetFailureCanBeRetriedAndSuccessIsReported() {
        val vm = AuthViewModel(f.services)
        f.auth.resetResult = Tasks.forException(IllegalStateException("offline")); vm.resetPassword("a@example.com"); drain()
        assertEquals(AuthPhase.ERROR, vm.state.value!!.phase)
        f.auth.resetResult = Tasks.forResult(null); vm.resetPassword("a@example.com"); drain()
        assertEquals(AuthPhase.RESET_SENT, vm.state.value!!.phase)
    }
    @Test fun textOnlyEditRetainsPhotoAndEmailAndWaitsForAcknowledgment() {
        f.auth.account = a
        val pending = TaskCompletionSource<Void>(); f.profiles.updateResult = pending.task
        val vm = ProfileViewModel(f.services); vm.load(a); drain(); vm.editName(" Changed "); vm.save(); drain()
        assertEquals("a" to mapOf("displayName" to "Changed"), f.profiles.updates.single())
        assertFalse(vm.state.value!!.saved)
        assertTrue(vm.state.value!!.saving)
        pending.setResult(null); drain(); assertTrue(vm.state.value!!.saved)
    }
    @Test fun failedSaveKeepsDraftEditable() {
        f.auth.account = a; f.profiles.updateResult = Tasks.forException(IllegalStateException("offline"))
        val vm = ProfileViewModel(f.services); vm.load(a); drain(); vm.editName("Draft"); vm.save(); drain()
        assertEquals("Draft", vm.state.value!!.name)
        assertFalse(vm.state.value!!.saving); assertFalse(vm.state.value!!.saved)
        assertEquals(AccountProblem.PROFILE_SAVE, vm.state.value!!.problem)
    }
    @Test fun failedPhotoUploadKeepsExistingPhotoAndReenablesSaving() {
        f.auth.account = a; f.photos.result = Tasks.forException(IllegalStateException("download URL failed"))
        val vm = ProfileViewModel(f.services); vm.load(a); drain(); vm.upload(Uri.parse("content://photo")); drain()
        assertFalse(vm.state.value!!.uploading)
        assertEquals("old-photo", vm.state.value!!.photo)
        vm.editName("Changed"); vm.save(); drain()
        assertFalse(f.profiles.updates.single().second.containsKey("profilePictureUrl"))
    }
    @Test fun staleUploadAfterAccountSwitchCannotModifyNewAccount() {
        f.auth.account = a
        val pending = TaskCompletionSource<String>(); f.photos.result = pending.task
        val vm = ProfileViewModel(f.services); vm.load(a); drain(); vm.upload(Uri.parse("content://photo"))
        f.auth.change(Account("b", "b@example.com")); pending.setResult("a-photo"); drain(); vm.save(); drain()
        assertFalse(vm.state.value!!.saved)
        assertTrue(f.profiles.updates.isEmpty())
    }
}
