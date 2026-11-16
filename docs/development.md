# Android development

## Prerequisites

- JDK 21 to run Gradle. The Java/Kotlin bytecode target is 17.
- Android SDK platform **37.0**, build tools **36.0.0**, and platform tools.
- Android Studio with support for the pinned Android Gradle Plugin.
- Access to Google Maven, Maven Central, Gradle distributions, and JitPack.

The current checkout pins **Gradle 9.6.0**, **AGP 9.4.1**, and **Kotlin 2.4.21**. The wrapper includes a distribution SHA-256 checksum. Dependencies are in `gradle/libs.versions.toml`; treat that file as the source of truth when versions change.

Set `JAVA_HOME` to your JDK and `ANDROID_HOME` to the SDK, or configure Android Studio's Gradle JDK and an untracked `local.properties` containing `sdk.dir`.

```sh
sdkmanager 'platforms;android-37.0' 'build-tools;36.0.0' 'platform-tools'
./gradlew --version
```

Minimum device API: **27** (Android 8.1). Compile API: **37**. Target API: **36**.

## Build and test

```sh
./gradlew --console=plain \
  :app:checkDebugAarMetadata \
  :app:testDebugUnitTest \
  :app:lintDebug \
  :app:assembleDebug \
  :app:assembleDebugAndroidTest
```

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Unit test report: `app/build/reports/tests/testDebugUnitTest/index.html`
- Lint report: `app/build/reports/lint-results-debug.html`

To run one test class:

```sh
./gradlew :app:testDebugUnitTest --tests '*TimerRecoveryTest'
```

With a connected emulator or device:

```sh
adb devices
./gradlew :app:installDebug
./gradlew :app:connectedDebugAndroidTest
```

The instrumentation suite currently contains a package-name smoke test; it is not full user-journey coverage. Check timer/background behavior, notification permissions, image selection, light/dark mode, larger text, and navigation manually before shipping.

## Firebase setup

### Local demo

Debug installs as `com.mphocodes.acetimetracker.debug` and uses `app/src/debug/google-services.json`. The checked-in placeholder configuration cannot authenticate to production. Tap test sign-in to use local sample data. Sample saved entries, categories, and profile changes reset on process restart; timer drafts and settings persist locally.

### Your own backend

1. Create or select a Firebase project you control.
2. Register Android clients matching the variant's application ID. Download each client configuration from that project.
3. Place development configuration in `app/src/debug/google-services.json` and release configuration in `app/google-services.json`. Review changes before committing client identifiers.
4. Enable email/password Authentication, configure Firestore and Storage, and deploy/test owner-restricted security rules.
5. Verify registration, login, password reset, profile updates, category creation, timesheet saving/deletion, and photo upload with a dedicated test account.

The existing release client identifies `acetimetracker-a3fb0`. Its ownership and deployed access rules are not established by this repository. Do not assume permission to use it. Never commit service-account keys, passwords, or upload keys.

Current data paths are described in [architecture](architecture.md). Firestore queries filtered by `userId` do not replace server-enforced ownership rules. Storage upload paths must also be protected; this repository does not include verified deployed rules.

## CI and releases

[Android CI](../.github/workflows/android.yml) runs metadata checks, JVM tests, lint, and debug/release compilation. Reports and artifacts are retained for 14 days. It does not deploy Firebase, publish to Play, or test live credentials.

See [release preparation](release.md) for signing, artifact verification, and publication blockers. Lint warnings remain visible rather than being hidden with a baseline.
