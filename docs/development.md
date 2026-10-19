# Android development

## Prerequisites

- JDK 21 to run Gradle (Android Studio's bundled JBR 21 also works).
- Android SDK command-line tools, platform **37.0**, build tools **36.0.0**, and
  platform tools. Minimum supported device API is 27; target API is 36.
- Network access to Google Maven, Maven Central, the Gradle distribution service,
  and JitPack for MPAndroidChart.

Set `JAVA_HOME` to your JDK installation and `ANDROID_HOME` to your SDK directory.
Alternatively, select JDK 21 in Android Studio's Gradle settings and set `sdk.dir`
in an untracked `local.properties`. Never commit machine-specific paths.

Install SDK packages with Android Studio's SDK Manager or the command-line tools:

```sh
sdkmanager 'platforms;android-37.0' 'build-tools;36.0.0' 'platform-tools'
```

The SDK repository names API 37's initial platform `android-37.0`, not
`android-37`. Resolve and accept the SDK license locally when prompted.

## Build and verify

From the repository root:

```sh
./gradlew --version
./gradlew --no-daemon --console=plain \
  :app:checkDebugAarMetadata \
  :app:assembleDebug \
  :app:assembleDebugAndroidTest \
  :app:testDebugUnitTest \
  :app:lintDebug
```

`./gradlew clean` clears generated project outputs for a clean rebuild. The wrapper
pins Gradle 9.5.0 with a SHA-256 checksum. `gradle/libs.versions.toml` pins direct
dependencies and plugins. AGP uses built-in Kotlin, with KGP 2.4.21 explicitly
aligned; Java/Kotlin bytecode targets 17 independently of the JDK running Gradle.

The foundation uses AGP 9.3.3 to include the fixes in that patch. Kotlin 2.4.21's
published fully tested AGP range ends at 9.3.1, so the metadata, compilation, test,
and lint checks verify this newer patch combination rather than assuming support.
See [AGP requirements](https://developer.android.com/build/releases/agp-9-3-0-release-notes),
[built-in Kotlin](https://developer.android.com/build/migrate-to-built-in-kotlin),
and [Kotlin compatibility](https://kotlinlang.org/docs/gradle-configure-project.html).

Outputs:

- APK: `app/build/outputs/apk/debug/app-debug.apk`
- Unit tests: `app/build/reports/tests/testDebugUnitTest/index.html`
- Lint: `app/build/reports/lint-results-debug.html`

GitHub Actions runs these checks for pull requests and pushes to `main`, and
uploads reports and the debug APK. It requires no repository secrets. Lint errors
fail the job; legacy warnings remain visible for later modernization stages.

## Firebase boundary

Debug builds install as `com.example.acetimetracker.debug` and use the checked-in
**demo** Firebase configuration in `app/src/debug/google-services.json`. Its
placeholder key cannot authenticate to the production project. This variant is
for build/UI validation; live sign-in and cloud writes need a separately configured
development project or an explicit emulator integration in a later stage.

The existing `app/google-services.json` remains the release client configuration.
It contains Firebase client identifiers, not administrative credentials. Do not
add service-account keys, signing keys, access tokens, or production secrets.
This foundation does not deploy rules, migrate records, upload images, or execute
Firebase operations during its JVM tests or CI checks.

## Test scope

The JVM suite exercises existing same-day time formatting with literal fixtures,
including minute precision and hour boundaries. Robolectric tests run a real
AndroidX drawer/Back dispatcher on API 36, covering drawer dismissal and default
Back navigation without initializing Firebase clients. The first run downloads
Robolectric's Android runtime from Maven Central.

The suite does not claim timer, overnight date handling, migration, or account
isolation correctness; those require behavior changes and tests in their owning
stages.

The obsolete manifest entry for the absent `timetracker` activity was removed,
the drawer uses `OnBackPressedDispatcher` for API 36 gestures, and the GIF library
was updated to address lint's native 16 KB alignment warning.

CI also compiles the instrumentation APK. Running it requires a connected emulator
or device (`./gradlew :app:connectedDebugAndroidTest`); compilation alone does not
count as an on-device test run. Target-SDK behavior, edge-to-edge layouts, storage
permissions, accessibility, and the deferred functional defects need device
verification before release. iOS remains deferred.
