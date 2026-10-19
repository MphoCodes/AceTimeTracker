# Android development setup

P1 provides a Compose-ready build alongside the existing View screens.
The app ID/namespace remains `com.example.acetimetracker`; minimum SDK is 27,
compile SDK is 37, and target SDK is 36. The thirteen XML activities share a
temporary edge-to-edge and dispatcher Back compatibility boundary. No Firebase credentials, signing
secrets, production writes or service deployment are needed for these checks.

## Java and Gradle

Run Gradle on JDK 21. Java and Kotlin application bytecode targets are both 17.
Select your installed JDK in the shell, for example on macOS with Android Studio:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
java -version
```

Use a JDK 21 installation appropriate to your host on Linux/Windows. In Android
Studio, select JDK/JBR 21 as the Gradle JDK as well. Do not commit local Java or
SDK paths. `local.properties` is optional and ignored; `ANDROID_HOME` works for
command-line builds without it.

The committed executable wrapper is Gradle **9.5.0**. It was generated in an
empty temporary project using the official distribution after verifying SHA-256:

```text
553c78f50dafcd54d65b9a444649057857469edf836431389695608536d6b746
```

`gradle-wrapper.properties` enforces this ZIP checksum. The generated wrapper JAR
SHA-256 is `497c8c2a7e5031f6aa847f88104aa80a93532ec32ee17bdb8d1d2f67a194a9c7`.
Official values: [distribution checksum](https://services.gradle.org/distributions/gradle-9.5.0-bin.zip.sha256)
and [wrapper JAR checksum](https://services.gradle.org/distributions/gradle-9.5.0-wrapper.jar.sha256).
To regenerate, download and verify that ZIP first, unzip outside the repository,
then run its `bin/gradle -p /path/to/empty-project wrapper --gradle-version 9.5.0
--distribution-type bin --gradle-distribution-sha256-sum <checksum>` and copy all
four generated wrapper files. Do not bootstrap through the old Gradle 8 wrapper
on JDK 21.

## Isolated Android SDK

Use an SDK directory outside the checkout. Install command-line tools **23.0**
(build **16111833**) into `$ANDROID_HOME/cmdline-tools/23.0`.
The official [SDK repository manifest](https://dl.google.com/android/repository/repository2-4.xml)
provides these archive SHA-1 integrity values (not SHA-256):

| Host | Archive | SHA-1 |
| --- | --- | --- |
| Linux x86-64 | `commandlinetools-linux-16111833_latest.zip` | `e025545c62a8e64c7559119566a569fb1dec5f60` |
| macOS x86-64 | `commandlinetools-mac_x86_64-16111833_latest.zip` | `112cf9618794a997ff273537d55bee02c22abffe` |
| macOS arm64 | `commandlinetools-mac_arm64-16111833_latest.zip` | `ad03dc49bfacfd52c110b14104ea548b8a07e830` |

Example bootstrap in Bash (replace the archive/checksum for your host):

```bash
export ANDROID_HOME="$HOME/.cache/acetimetracker-sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export GRADLE_USER_HOME="$HOME/.cache/acetimetracker-gradle"
cli_archive=commandlinetools-mac_arm64-16111833_latest.zip
cli_sha1=ad03dc49bfacfd52c110b14104ea548b8a07e830
bootstrap_dir=$(mktemp -d)
curl --fail --location --retry 3 \
  "https://dl.google.com/android/repository/$cli_archive" \
  -o "$bootstrap_dir/commandlinetools.zip"
echo "$cli_sha1  $bootstrap_dir/commandlinetools.zip" | shasum -a 1 --check
mkdir -p "$ANDROID_HOME/cmdline-tools"
unzip -q "$bootstrap_dir/commandlinetools.zip" -d "$bootstrap_dir/unpacked"
mv "$bootstrap_dir/unpacked/cmdline-tools" "$ANDROID_HOME/cmdline-tools/23.0"
export PATH="$ANDROID_HOME/cmdline-tools/23.0/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"
sdkmanager --sdk_root="$ANDROID_HOME" --licenses
sdkmanager --sdk_root="$ANDROID_HOME" \
  'platforms;android-37.0' 'build-tools;36.0.0' platform-tools
```

Accept the SDK licenses when prompted. CLI 23 prints a deprecation message for
`sdkmanager` and delegates to the Android CLI; this is expected. The baseline
API 37 platform is named **`platforms;android-37.0`**, revision 2, in the manifest;
`platforms;android-37` is not an interchangeable package ID. Do not rename SDK
folders or lower compile SDK to work around a resolution error.

## Build acceptance

From the repository root, with the Java/SDK environment above:

```bash
./gradlew --version
./gradlew --no-daemon --stacktrace \
  :app:checkDebugAarMetadata :app:assembleDebug :app:assembleDebugAndroidTest \
  :app:lintDebug :app:testDebugUnitTest
```

The version command must report Gradle 9.5.0 and JVM 21. Gradle's own embedded
Kotlin version is separate from the Android compiler. The Android built-in KGP
runtime and Compose compiler are both pinned to 2.4.21. AGP is 9.3.3, Compose BOM
2026.09.00, Firebase BOM 35.0.2 and Google Services 4.5.0. Exact library/plugin pins
live in `gradle/libs.versions.toml`; root `build.gradle.kts` repeats the KGP pin
because its bootstrap classpath precedes generated catalog accessors. Keep both
Kotlin pins aligned. Build output and AAR metadata checks establish compatibility;
publication of a version alone does not.

The Compose `FoundationPreview` lives only in `src/debug`, uses Material 3, and
has no launcher or production route. Firebase uses BOM-managed main modules;
KTX imports have moved into those modules. Retained legacy dependencies stay on
the plan's pins. No Room/DataStore/WorkManager/Navigation 3 is introduced here.

Inspect these outputs after successful checks:

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.
- Test APK: `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`.
- Lint: `app/build/reports/lint-results-debug.html` and `.sarif`.
- Unit tests: `app/build/reports/tests/testDebugUnitTest/index.html` and
  `app/build/test-results/testDebugUnitTest/`.
- Device tests: `app/build/reports/androidTests/connected/debug/` and
  `app/build/outputs/androidTest-results/connected/debug/`.

The generated arithmetic and package-name examples provide build/package smoke
evidence. Legacy instrumentation additionally covers inset padding/reset, real IME
show/hide and usable content bounds, drawer/system Back, form navigation and a
visible Back button, plus focused Login password/Registration confirmation visibility
above the IME. These checks do not establish auth, timer or migration correctness.
Existing lint warnings remain visible; lint errors must fail the build.

For clean-source reproduction, commit your changes and export tracked files with
`git archive HEAD | tar -x -C /path/to/new-empty-directory`. Run the same commands
there with the explicit Java/SDK environment and the same external task caches.
No IDE state, `local.properties`, project `.gradle` directory or build output
should be copied. A fresh source tree plus a warm dependency cache tests missing
source inputs; it does not claim offline or independent mirror availability.

## Legacy runtime verification and CI

Use clean AVDs (`-wipe-data -no-snapshot`) with these published image packages:

| Runtime | Linux CI | Apple Silicon local |
| --- | --- | --- |
| API 27 | `system-images;android-27;default;x86_64` | `system-images;android-27;google_apis;arm64-v8a` |
| API 36 | `system-images;android-36;google_apis;x86_64` | `system-images;android-36;google_apis;arm64-v8a` |
| API 37 walkthrough | Not in CI | `system-images;android-37.0;google_apis;arm64-v8a` |

Install the selected image and emulator through SDK Manager. Select one connected
device via `ANDROID_SERIAL`, then run the complete instrumentation suite:

```bash
./gradlew --no-daemon --stacktrace :app:connectedDebugAndroidTest
```

CI selects `LegacyWindowInsetsTest`, `MainActivityBackTest`,
`LegacyWindowRuntimeTest` and the existing `ExampleInstrumentedTest`. The helper
uses real FrameLayouts and listener dispatch. Typed synthetic cutout/IME fixtures
run on API 30+ because older platform dispatch discards those compat overrides;
API 27 still checks bars, hidden/reset/reinstall, actual keyboard bounds and Back.

On API 36 and 37, also walk through system Back with gesture and three-button
navigation, launcher to registration, the Login link, IME show/hide, drawer
open/close, the About visible Back button and an expanded window. Do not submit
credentials or save records. Record image build, dimensions/density, navigation
mode and evidence. Unavailable execution must be reported as unavailable.

`LegacyActivity` enables edge-to-edge and `adjustResize` for pre-30 IME delivery.
The content root records initial padding once, owns bars/cutout and the maximum
of IME/navigation bottom, then forwards unhandled inset types to children. Both
old `fitsSystemWindows` consumers were in `activity_main.xml`; the navigation
header's ordinary design padding is preserved. The future Compose shell must own
its own insets, without also installing this temporary XML boundary. MainActivity
closes its drawer first and temporarily disables its dispatcher callback while
delegating, restoring it in `finally`. Existing save/auth `finish()` paths remain. Login adds a `fillViewport` scroll
container around its unchanged form: the explicit resize/inset policy otherwise
obscured the focused password on API 27, whereas the predecessor panned it clear.
Registration already keeps its focused confirmation visible without a layout change.
See Android's [edge-to-edge guidance](https://developer.android.com/develop/ui/views/layout/edge-to-edge),
[keyboard guidance](https://developer.android.com/develop/ui/views/layout/sw-keyboard)
and [target-36 Back behavior](https://developer.android.com/about/versions/16/behavior-changes-16#predictive-back).

`.github/workflows/android.yml` runs the five host tasks and clean API 27/36
instrumentation jobs on Ubuntu 24.04/JDK 21. Each bootstraps CLI 23 and the SDK in
runner temporary storage. Actions are pinned by publisher commit SHA; Gradle
setup owns caching/wrapper validation. Device XML, HTML and logcat results upload
as `legacy-api27` / `legacy-api36` artifacts even on failure. Triggers are pull
requests, main pushes and manual dispatch, with `contents: read`, superseded-run
cancellation and a 30-minute timeout per job. No signing/production secrets.

## Task 1 evidence and plan rulings

The local arm64/JBR 21.0.10 run passed all five host checks. The compiled Compose
preview has JVM class major version 61 (Java 17) and generated Compose runtime
parameters. SDK resolution succeeded using the installed API 37.0 revision 2.
The initial lint report contains 204 warnings and no errors; the single generated
arithmetic unit test passed. Warnings include legacy resources/text/deprecations,
old target SDK, dependency update suggestions and three 16 KB native-alignment
warnings. These are retained, with no lint suppressions or baseline. The native
alignment warnings need review in the later release stage; target 34 is temporary.
The compiler's nullable date-parsing diagnostics are resolved with the plan's
`requireNotNull` guards inside the existing error-handling blocks.

Two bootstrap details differ from the recorded plan after publisher inspection:

- `gradle/actions` v5's recorded annotated tag object resolves to commit
  `0723195856401067f7a2779048b490ace7a47d7c`; emulator runner v2's resolves to
  `a421e43855164a8197daf9d8d40fe71c6996bb0d`. The workflow pins these commits,
  preserving both selected version families. See the publishers' tag objects
  ([Gradle](https://api.github.com/repos/gradle/actions/git/tags/4c125117fe7c5aed11272ec4213f602f012f89f2),
  [emulator runner](https://api.github.com/repos/ReactiveCircus/android-emulator-runner/git/tags/4c44018e59b437e86cdfc41da381398f93ed8808)).
- The stable SDK inventory has no API 27 Google APIs x86-64 image. Linux CI uses
  the published `default` x86-64 image instead. An API 27 ARM Google APIs image
  is available for a separate local attempt. No toolchain version was lowered.

Final clean-source, device and submitted-head CI outcomes are recorded in the PR.
