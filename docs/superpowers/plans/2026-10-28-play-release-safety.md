# Play Release Safety Implementation Plan

> Execution: make the smallest changes needed for a safe Play release and verify each behavior before continuing.

**Goal:** Prevent production users from entering the temporary local test session while keeping that session available to developers.

**Architecture:** Use Android's debuggable application flag as the single gate in authentication and the two authentication screens. Release builds ignore stored mock sessions and never route login, registration, or password reset through test-account shortcuts.

**Tech stack:** Kotlin, Android, Jetpack Compose, Firebase Auth, JUnit and Robolectric.

**Spec:** `.scratch/play_policy_insights_4718d8d9-0691-4812-ad14-5f054e85417d/compliance_report.md` and the user's requested Play release.

## Global constraints

- Preserve the existing debug-only Firebase configuration and offline test workflow.
- Do not change the release Firebase client until the confirmed project is accessible.
- Preserve the user's pre-existing Gradle and IDE modifications.
- Do not create or commit signing credentials in source control.

## Review focus

- A release user taps or invokes the test-login route: no mock session is created.
- Stale mock preferences exist in release: they do not make a user appear signed in.
- A debug user invokes the test-login route: the mock session still works.
- Release login, registration, and password reset for test-looking email addresses reach Firebase rather than local shortcuts.
- Both authentication screens hide the test-login affordance in release.

## Task: Gate local test sessions to debuggable applications

**Files:** `app/src/main/java/com/example/acetimetracker/data/repository/AuthRepository.kt`, `app/src/main/java/com/example/acetimetracker/ui/screen/LoginScreen.kt`, `app/src/main/java/com/example/acetimetracker/ui/screen/RegistrationScreen.kt`, and a shared Android application-mode utility. Test: `app/src/test/java/com/example/acetimetracker/data/repository/ReleaseAuthTest.kt`.

- [x] Write Robolectric tests using a local Firebase app and the real repository. Assert that a non-debuggable app cannot start or resume a mock session and that a debuggable app can still start one.
- [x] Run `:app:testDebugUnitTest --tests '*ReleaseAuthTest'` and verify the release expectations fail for the existing behavior.
- [x] Gate all mock-session reads and shortcuts, and conditionally render the test-login card only in debuggable builds.
- [x] Re-run the focused tests and full `:app:testDebugUnitTest`, then build and lint the release variant.
- [x] Inspect the resulting release/debug behavior on an isolated Android emulator if one is available.

## Task: Report photo upload failures accurately

**Files:** `app/src/main/java/com/example/acetimetracker/data/repository/TimesheetRepository.kt`, `app/src/main/java/com/example/acetimetracker/data/repository/UserRepository.kt`, and `app/src/main/java/com/example/acetimetracker/ui/viewmodel/TimesheetViewModel.kt`. Test: `app/src/test/java/com/example/acetimetracker/data/repository/ImageUploadFailureTest.kt`.

- [x] Test both real Firebase Storage SDK upload paths with an unavailable local image and a local emulator endpoint; each must return `Resource.Error`, not a local URI as success.
- [x] Run `:app:testDebugUnitTest --tests '*ImageUploadFailureTest'` and confirm both tests reproduce the false-success behavior.
- [x] Propagate upload and download-URL failures, preserve local photos in debug mock sessions, and clear a previously selected timesheet photo when a new upload starts.
- [x] Test out-of-order timesheet and profile upload completions and removing a timesheet photo during upload; ignore stale callbacks and clear the loading state on removal.
- [x] Prevent profile saving and duplicate selection while a photo upload is in progress.
- [x] Re-run the focused tests and full debug unit suite, then build and lint the release variant.
