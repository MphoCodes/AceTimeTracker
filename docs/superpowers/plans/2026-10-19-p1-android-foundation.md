# P1 Android Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans in the dedicated implementation chat. Steps use checkbox (`- [ ]`) syntax for tracking. The owner selected separate chats and one PR per implementation task; preserve that method. Use a fresh reviewer before each implementation PR is handed back.

**Goal:** Establish a reproducible Compose-ready Android build and CI, then raise the target SDK to 36 with the existing screen navigation and system-bar behavior working.

**Architecture:** Keep the existing single app module and Firebase data contracts. First replace the build/dependency configuration while retaining target 34; then add a small temporary compatibility boundary for legacy View activities and raise the target to 36. P5 later replaces those screens with the Compose application shell, and P11 removes the temporary legacy boundary.

**Tech Stack:** AGP 9.3.3, Gradle 9.5.0, JDK 21, built-in Kotlin/Compose compiler 2.4.21, compile SDK 37, minimum SDK 27, Compose BOM 2026.09.00, Firebase BOM 35.0.2.

**Spec:** [Approved Android design](../specs/2026-10-19-android-modernization-design.md), especially sections 4, 7 and 8.

**Status:** Approved by the owner in this chat on 19 October 2026 with “Approve P1 and begin implementation.” The specification is also approved. Execute Task 1 first, then Task 2; no further design/plan approval is pending for P1.

## Global Constraints

- Retain application ID and namespace `com.example.acetimetracker`, `minSdk 27`, and the existing Firebase account/project configuration.
- Use `compileSdk 37`; P1 finishes at `targetSdk 36`. Task 1 retains target 34 solely to isolate build changes from runtime behavior changes in Task 2.
- Run Gradle on JDK/JBR 21 and compile Java/Kotlin to bytecode 17. Do not commit a machine-specific Java/SDK path.
- Pin exact stable artifact/plugin versions in `gradle/libs.versions.toml`; use the same Kotlin version for the built-in KGP runtime and Compose compiler plugin. The root buildscript may repeat that single version where required by plugin bootstrap ordering; verify they match.
- No permanent built-in-Kotlin/AGP DSL opt-out flags, dynamic dependency versions, preview ConstraintLayout dependencies, disabled lint errors, or broad new lint baselines.
- Preserve current product/data behavior. No auth redesign, data migration, timer correction, reminder implementation, production Firebase writes, store publication, PR merging or iOS work belongs to P1.
- One implementation task produces one PR. Task 2 waits for Task 1. P2 waits for both. Use a dedicated implementation chat per task and avoid simultaneous writers to a shared checkout.

## Review Focus

1. **Fresh checkout without Java/SDK defaults:** document and prove local SDK/JDK selection; a warm IDE cache is not evidence of reproducibility. Task 1 steps 1, 2 and 7 own this.
2. **Published versions with incompatible metadata:** fail on SDK/AGP/compiler mismatch rather than hiding it; artifact publication alone is not a passing build. Task 1 steps 3–7 own this.
3. **Oldest supported device and existing resource/library loading:** compile the test APK and run device smoke checks at API 27; keep the app ID, launcher and existing layouts. Task 1 step 8 and Task 2 step 6 own this.
4. **System bars, cutouts and keyboard transitions:** content must not gain cumulative padding, receive double insets, or stay raised after the keyboard hides. Task 2 steps 1–4 and 6 own this.
5. **Drawer Back versus leaving the app:** Back closes an open drawer first; both system Back and visible Back buttons still navigate after targeting 36. Task 2 steps 1, 4 and 6 own this.

## Version and environment decisions

| Item | Exact value |
| --- | --- |
| Android Gradle Plugin | `9.3.3` |
| Gradle | `9.5.0-bin` |
| Gradle ZIP SHA-256 | `553c78f50dafcd54d65b9a444649057857469edf836431389695608536d6b746` |
| Kotlin/KGP and Compose compiler | `2.4.21` |
| Google Services plugin | `4.5.0` |
| Compose BOM | `2026.09.00`, imported for production and Android-test configurations |
| Activity Compose | `1.13.0` |
| AndroidX Core | `androidx.core:core:1.19.1`; its KTX APIs are merged into the main artifact |
| AppCompat / XML ConstraintLayout | `1.8.0` / `2.2.2` |
| Firebase BOM | `35.0.2`; use unversioned main Auth/Firestore/Database/Storage modules under the BOM |
| Android test runner / JUnit extension / Espresso | `1.7.0` / `1.3.0` / `3.7.0` |
| Local unit JUnit | Retain `4.13.2` |
| SDK Manager platform package | `platforms;android-37.0`, revision 2 in the checked official manifest; do not assume `platforms;android-37` exists |
| Build Tools | `36.0.0`, the documented AGP 9.3 default |
| Command-line tools | Revision `23.0.0`, official archive build `16111833` |

Keep existing Material Components `1.12.0`, GIF drawable `1.2.28`, Glide `4.16.0`, MPAndroidChart `v3.1.0`, Lottie `5.2.0`, and existing independently versioned legacy navigation/lifecycle dependencies pinned during Task 1 unless a real compatibility failure requires a separately documented change. Remove only the duplicate/alpha ConstraintLayout declarations and unused alpha Compose ConstraintLayout dependency in this task. Do not install Room, DataStore, WorkManager or Navigation 3 ahead of their implementation stages.

Published artifact checks were performed on 19 October 2026. Kotlin's tested AGP range ends at 9.3.1, so the selected newer 9.3.3 patch remains a compatibility hypothesis until this repository's real checks pass. Sources: [AGP](https://developer.android.com/build/releases/agp-9-3-0-release-notes), [Kotlin](https://kotlinlang.org/docs/gradle-configure-project.html), [Gradle checksum](https://services.gradle.org/distributions/gradle-9.5.0-bin.zip.sha256), [Google Services](https://firebase.google.com/docs/android/google-services-plugin-and-file), [Firebase Kotlin migration](https://firebase.google.com/docs/android/kotlin-migration), [SDK manifest](https://dl.google.com/android/repository/repository2-4.xml).

### Local and CI bootstrap

Use an available JDK 21; Android Studio's bundled JBR was verified locally. Set it only in the task shell. The old Gradle 8.0 wrapper cannot be assumed to run on that runtime. Bootstrap Gradle 9.5.0 from its official ZIP, verify the SHA-256 above, and generate the four wrapper files in a temporary empty project before copying them into this checkout. Restore executable permission on `gradlew` and include the distribution checksum in wrapper properties.

Use a task-specific SDK directory if no local SDK exists. Install the matching official command-line-tools archive into `cmdline-tools/23.0`; expose that SDK using the standard Android environment variables. Install platform `37.0`, Build Tools `36.0.0`, and platform-tools through that SDK Manager. Verify that AGP actually resolves compile SDK 37 from the installed platform before declaring setup complete. A package-name mismatch is a diagnosed failure, not permission to lower the approved compile SDK.

Official archive names and checksums recorded from Google's repository manifest:

| Host | Archive below `https://dl.google.com/android/repository/` | Published SHA-1 integrity value |
| --- | --- | --- |
| Linux x86-64 CI | `commandlinetools-linux-16111833_latest.zip` | `e025545c62a8e64c7559119566a569fb1dec5f60` |
| macOS x86-64 | `commandlinetools-mac_x86_64-16111833_latest.zip` | `112cf9618794a997ff273537d55bee02c22abffe` |
| macOS arm64 | `commandlinetools-mac_arm64-16111833_latest.zip` | `ad03dc49bfacfd52c110b14104ea548b8a07e830` |

For CI, use Ubuntu 24.04 with a clean task-specific SDK instead of relying on the runner's preinstalled CLI/platform layout. Pins resolved from the publishers' Git refs on 19 October 2026:

- `actions/checkout` v6: `d23441a48e516b6c34aea4fa41551a30e30af803`.
- `actions/setup-java` v5: `b6effb05e454b25005698d916606bdc6ffcbf961`, distribution `temurin`, Java `21`.
- `gradle/actions/setup-gradle` v5: `4c125117fe7c5aed11272ec4213f602f012f89f2`; let it own Gradle caching/wrapper validation.
- For the device-test job, `ReactiveCircus/android-emulator-runner` v2: `4c44018e59b437e86cdfc41da381398f93ed8808`; inspect its pinned action inputs before use.

The workflow runs on pull requests, pushes to `main`, and manual dispatch, with `contents: read`, a 30-minute host-build timeout, workflow/ref concurrency and cancellation of superseded runs. It needs no signing or production-service secrets. Do not use `pull_request_target`.

## Branch and PR sequencing

The approved docs are currently on `codex/android-modernization-spec` in PR #1. Recheck its head and merge state before execution. If PR #1 is still open, start Task 1 from that exact head on `codex/p1-toolchain-ci` and target the docs branch so the implementation PR contains only Task 1. If it has merged, branch from updated `main` and target `main`. Record the chosen base in the PR description.

Task 2 uses `codex/p1-target36-compatibility`, based on the verified Task 1 head, and targets its predecessor until integration permits retargeting to `main`. Do not merge anything automatically. Attach every created PR to its implementation chat and report its URL to the coordinator through the available thread status.

## Task 1: Reproducible Compose-ready toolchain and CI

**Files:**
- Create: `gradle/libs.versions.toml`, `.github/workflows/android.yml`, `docs/development/android-setup.md`.
- Create: `app/src/debug/java/com/example/acetimetracker/FoundationPreview.kt` as a compile-only Material 3/Compose preview, with no launcher or production route.
- Modify: `build.gradle.kts`, `app/build.gradle.kts`, `gradle/wrapper/gradle-wrapper.properties`, `gradle/wrapper/gradle-wrapper.jar`, `gradlew`, `gradlew.bat`.
- Modify: `app/src/main/AndroidManifest.xml`, `app/src/main/java/com/example/acetimetracker/Login.kt`, `app/src/main/res/values/strings.xml` only for the specific build/lint compatibility fixes below.
- Conditional compiler-only compatibility edits: `TimeUtils.kt` and `Timesheet.kt` if the updated SDK/compiler reports nullable `SimpleDateFormat.parse` results; preserve their existing catch/error behavior.

**Interfaces:**
- Consumes: the existing app ID, namespace, layouts, resource IDs, Firebase configuration, launcher and activity navigation.
- Produces: executable Gradle 9.5.0 wrapper; version catalog; Compose-enabled app configuration; `@Composable internal fun FoundationPreview(): Unit`; reproducible host CI checks. The preview is debug-only and is not the P5 design system.

- [ ] **Step 1: Record the starting state.** Confirm the approved spec/plan, PR base, clean checkout, JDK version, SDK availability and current wrapper permissions. Capture the current baseline command outcome. Do not represent the existing arithmetic/package tests as feature coverage.
- [ ] **Step 2: Prepare the local bootstrap.** Provision the task SDK/JDK paths and verified temporary Gradle 9.5.0 distribution described above, then generate/copy the wrapper. Run `./gradlew --version`; it must report Gradle 9.5.0 and JVM 21. Keep cache/install directories outside tracked source.
- [ ] **Step 3: Define the version catalog.** Add exact pins from the version table and aliases for existing retained dependencies. Define plugin aliases for AGP, Compose compiler and Google Services. Do not convert stable legacy libraries to unrequested new major lines.
- [ ] **Step 4: Migrate build configuration.** Remove `org.jetbrains.kotlin.android`; use built-in Kotlin with an explicit root `kotlin-gradle-plugin:2.4.21` runtime override and matching Compose plugin. Set compile SDK 37, minimum 27, temporary target 34 and Java/Kotlin bytecode 17. Enable Compose alongside View Binding. Import the Compose and Firebase BOMs; add BOM-managed Compose UI, Material 3 and preview/tooling support. Keep Compose test artifacts BOM-managed. Flatten the nested dependency block and remove the named duplicate/alpha dependencies.
- [ ] **Step 5: Make only identified source/resource compatibility changes.** Migrate Login's Firebase KTX imports to `com.google.firebase.auth.auth` and `com.google.firebase.Firebase`. Remove the manifest's obsolete package attribute and unused `.timetracker` activity declaration; the real `Timer` activity stays. Remove the stray raw sentence outside a string element in `strings.xml`, preserving the actual About string. If compiler nullability diagnostics require it, use `requireNotNull` inside the existing time-parsing `try` blocks so their existing error return remains intact. No new temporal behavior belongs here.
- [ ] **Step 6: Compile a real Compose function.** Add a debug-only `FoundationPreview()` annotated with `@Preview` and `@Composable`, using Material 3. Build it with the same compiler/BOM as the app. Do not add a production screen, fake goal data, or tests that merely assert this preview's static text.
- [ ] **Step 7: Run the host acceptance commands.** Run `./gradlew --no-daemon --stacktrace :app:checkDebugAarMetadata :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug :app:testDebugUnitTest`. Require successful task exits; inspect both lint and test reports. Fix only introduced configuration/compilation failures within this task's contract. Report existing lint warnings accurately; do not bypass errors. If the pinned Kotlin/AGP patch combination fails, diagnose it and document a justified compatible version adjustment before proceeding.
- [ ] **Step 8: Add CI and setup instructions.** Implement the host workflow with the pinned actions/SDK steps and the exact checks in step 7. Document local JDK/SDK selection, package IDs, wrapper checksum, setup commands and report locations. Run those instructions against a clean source checkout using the same task cache, so untracked project files do not mask a missing input. Run the existing package-name instrumentation smoke on a clean API 27 test emulator; record device/runtime and result. Credentials must not be entered and live Firebase records must not be changed.
- [ ] **Step 9: Commit, review and open the Task 1 PR.** Commit with `build: establish Compose-ready Android toolchain and CI`. Have a fresh reviewer inspect the diff and actual check output, then push `codex/p1-toolchain-ci` and open/attach its PR. Wait for the submitted head's host CI result; document any unavailable device execution as unavailable rather than passing. Task 1 acceptance requires the checks above, not merely version-file edits. Leave target SDK 34 explicit in the PR; the target-36 behavior change is Task 2.

## Task 2: Target SDK 36 with legacy screen compatibility

**Files:**
- Create: `app/src/main/java/com/example/acetimetracker/LegacyActivity.kt`.
- Create: `app/src/main/java/com/example/acetimetracker/core/ui/LegacyWindowInsets.kt`.
- Create: `app/src/androidTest/java/com/example/acetimetracker/LegacyWindowInsetsTest.kt` and `MainActivityBackTest.kt`.
- Modify: `app/build.gradle.kts`, `app/src/main/res/values/ids.xml`, `app/src/main/res/layout/activity_main.xml`, `app/src/main/res/layout/nav_header_timetracker.xml`, `.github/workflows/android.yml`, `docs/development/android-setup.md`.
- Modify inheritance in all existing View activities: `Splash.kt`, `Registration.kt`, `Login.kt`, `MainActivity.kt`, `Categories.kt`, `Timer.kt`, `Timesheet.kt`, `Myprogress.kt`, `GraphActivity.kt`, `profile.kt`, `EditProfileActivity.kt`, `settings.kt`, `About.kt`.
- Modify Back handling in `MainActivity.kt` and the existing visible Back button callbacks in the activity files above.

**Interfaces:**
- Consumes: Task 1's verified catalog, wrapper, Compose-ready build and existing resource/navigation contracts.
- Produces: `abstract class LegacyActivity : AppCompatActivity`; `internal fun installLegacyWindowInsets(content: ViewGroup): Unit`; all existing View activities use this temporary boundary; final P1 target SDK 36. P5's Compose shell will own its own insets/navigation and must not receive both boundaries.

- [ ] **Step 1: Write meaningful regression tests before the compatibility code.** Define the following Android instrumentation tests, initially failing to compile/run because the new boundary does not exist. Use an actual `FrameLayout` and dispatched `WindowInsetsCompat` for helper tests; assert applied View padding rather than a duplicate implementation of the padding formula. Use `ActivityScenario<MainActivity>` for drawer behavior, without entering credentials or saving records. Each helper test starts with padding `(3,5,7,11)`; system-bar fixture top is `24`, bottom is `16`, and left cutout is `30`.

| Test | Action and required assertions |
| --- | --- |
| `appliesSystemBarsAndCutoutOnce` | Dispatch the fixture; `assertEquals(33, view.paddingLeft)`, `assertEquals(29, view.paddingTop)`, `assertEquals(7, view.paddingRight)`, `assertEquals(27, view.paddingBottom)` |
| `imeUsesMaximumBottomInsetAndResets` | Add IME bottom `250`; `assertEquals(261, view.paddingBottom)`; hide IME and `assertEquals(27, view.paddingBottom)` |
| `redispatchAndReinstallDoNotAccumulate` | Dispatch twice and install the listener twice; assert padding remains `(33,29,7,27)` using the four assertions above |
| `hiddenBarsRestoreInitialPadding` | Remove bar/cutout/IME insets; assert padding returns to `(3,5,7,11)` using four per-side assertions |
| `backClosesDrawerBeforeFinishing` | Open drawer without animation, invoke Back dispatcher; `assertFalse(drawer.isDrawerOpen(GravityCompat.START))` and `assertFalse(activity.isFinishing)`; invoke Back again and assert the scenario reaches the normal destroyed/exit state |
- [ ] **Step 2: Demonstrate the failing regression checks.** Run `./gradlew :app:assembleDebugAndroidTest` and the targeted connected tests where available; record the missing boundary or old drawer behavior. An unrelated SDK/download error is not the expected regression failure.
- [ ] **Step 3: Implement the temporary legacy boundary.** In `LegacyActivity.onCreate`, use supported edge-to-edge configuration. Override the existing layout-resource `setContentView(Int)` path to install `installLegacyWindowInsets` on `android.R.id.content` after inflation. The helper stores original padding once using a keyed ID, applies system-bar/display-cutout insets plus the maximum of bottom-bar/IME insets, and forwards only unhandled inset types to children. Request initial dispatch. Do not add an edge-to-edge opt-out. Remove the two existing `fitsSystemWindows` consumers so legacy content has one inset owner.
- [ ] **Step 4: Wire activities and Back handling.** Change the thirteen View activity base classes to `LegacyActivity`. Replace `MainActivity.onBackPressed` interception with an enabled `OnBackPressedDispatcher` callback that closes an open drawer first and delegates normal navigation with the callback temporarily disabled otherwise. Visible Back buttons invoke `onBackPressedDispatcher.onBackPressed()`. Preserve their destinations and account/data behavior; do not add an auth rewrite.
- [ ] **Step 5: Raise the target and rerun host checks.** Set target SDK 36, leaving compile 37/minimum 27. Run all Task 1 host checks again; require successful results. No manifest Back opt-out, orientation restriction, disabled lint error or system-bar opt-out is an acceptable substitute for the compatibility change.
- [ ] **Step 6: Verify on supported runtimes.** Extend CI with targeted instrumentation on clean API 27 and API 36 x86-64 emulators using the pinned emulator action and JDK/SDK setup. Run `LegacyWindowInsetsTest`, `MainActivityBackTest`, and the existing package-name smoke. Use a known supported system-image package from SDK Manager rather than guessing a minor-version package name. Also run an API 37 device/emulator walkthrough: launcher, registration/login form, open/close drawer, visible Back buttons, gesture and three-button navigation, keyboard show/hide, and an expanded window. Record runtime, navigation mode and evidence; these checks do not submit credentials or write production Firebase data. Broader legacy UX defects remain visible for P5 rather than being declared fixed by this check.
- [ ] **Step 7: Commit, review and open the Task 2 PR.** Commit with `fix: preserve legacy navigation and insets when targeting Android 16`. Obtain a fresh review of the regression tests, changed behavior and real reports; then push/open/attach `codex/p1-target36-compatibility` against its verified predecessor. Require the submitted head's host and device CI to pass. P1 is complete only when both PR deliverables and their evidence are ready for coherent integration.

## Completion and next handoff

Record each PR URL, exact head SHA, host/device results and any remaining limitations in its description. Do not call P1 complete when dependency publication is the only evidence or a required runtime check is still missing. P2's account/profile and app-session behavior is a separate plan and PR; timer/data-model and Compose product-screen changes remain in their approved later stages.

The next action is the dedicated Task 1 implementation chat using this approved plan and specification. The coordinator remains responsible for checking results and the PR sequence.
