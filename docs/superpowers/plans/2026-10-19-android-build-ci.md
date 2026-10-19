# Android build and CI foundation

**Goal:** Make P1 reproducible: Android dependency metadata checks, debug assembly,
unit tests, and lint pass locally and in GitHub Actions. iOS and Firebase data
changes are out of scope.

**Design:** Use the existing single app module and Views screens. Pin AGP 9.3.3,
Gradle 9.5.0, Kotlin 2.4.21, JDK 21, compile SDK 37, target SDK 36, and minimum
SDK 27. Use built-in Kotlin and Java/Kotlin bytecode 17. Introduce only libraries
used by existing code; Compose UI and persistence arrive in their owning stages.
Debug has a distinct application ID and a non-production demo Firebase config.

**Reference:** [Modernization proposal, P1](https://github.com/MphoCodes/AceTimeTracker/blob/codex/android-modernization-spec/docs/superpowers/specs/2026-10-19-android-modernization-design.md).

## Execution

- [x] Reproduce the original build failure and establish local JDK/SDK prerequisites.
- [x] Pin dependencies in a version catalog, refresh and checksum the wrapper,
  remove unused/preview dependencies, and isolate debug Firebase configuration.
- [x] Run metadata checks and assembly; repair only errors needed for this stage.
- [x] Replace the arithmetic example with focused tests of existing time formatting;
  keep later timer/date behavior changes out of this PR.
- [x] Run lint without a blanket baseline or disabling failure on errors; repair
  blocking findings and document remaining legacy warnings.
- [x] Add a least-privilege, pinned GitHub Actions workflow using the same commands,
  with test/lint reports and a debug APK artifact, plus local build documentation.
- [x] Verify a clean build and workflow syntax, review the final diff, and open a PR.

## Acceptance

`./gradlew :app:checkDebugAarMetadata :app:assembleDebug :app:lintDebug :app:testDebugUnitTest`
must succeed on JDK 21 with SDK platform 37 and build tools 36.0.0. Debug checks
need no repository secrets and execute no production Firebase operations. Existing
release application identity and Firebase data paths remain unchanged. CI remains
red on compilation, unit-test, or lint errors. Device behavior and the deferred
product defects are not claimed fixed by this foundation.

## Implementation decisions

- The SDK repository's package is `platforms;android-37.0`; installation commands
  and CI use that name while AGP compiles API 37.
- The Firebase BOM requires main-module Kotlin imports; only the two imports in
  Login change, without changing the reset-password call or any data contract.
- Target API 36 makes the existing drawer `onBackPressed` override a lint error.
  Replace it with an AndroidX dispatcher callback and test the real drawer on a
  simulated API 36 runtime. Other navigation/UI modernization remains deferred.
- Remove the manifest entry for a class absent from both source and dependencies.
- Update android-gif-drawable from 1.2.28 to 1.2.32 because lint identifies its
  old arm64 native library as incompatible with 16 KB memory pages.

## Local verification, 19 October 2026

- Clean full command passed: `./gradlew clean :app:checkDebugAarMetadata
  :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest
  :app:lintDebug --no-daemon --console=plain` (92 executed tasks).
- Eight JVM tests passed: five duration fixtures and three drawer Back cases.
  The restored-open-drawer case failed before the post-layout synchronization
  fix, then passed in the full suite. Robolectric's API 36 runtime needs the
  test-only JDK `jdk.internal.access` opening configured in the build.
- Lint: zero errors, 194 visible warnings, zero `Aligned16KB` findings.
- Generated APK metadata and Google Services resources confirm the debug app ID
  and demo project; the release Firebase config is unchanged.
- Actionlint 1.7.12 and `git diff --check` passed. Wrapper JAR and distribution
  hashes match Gradle's published checksums.
- Instrumentation APK compiled; no emulator/device test run or release-quality
  claim is made. Hosted CI results are recorded in the pull request.
