# Release preparation

## Build artifacts

```sh
./gradlew --console=plain :app:testDebugUnitTest :app:lintRelease :app:assembleRelease :app:bundleRelease
```

Without signing configuration:

- APK: `app/build/outputs/apk/release/app-release-unsigned.apk`
- AAB: `app/build/outputs/bundle/release/app-release.aab`
- Lint: `app/build/reports/lint-results-release.html`

An unsigned AAB is useful for build verification, but cannot be uploaded as a signed Play release. A successful Gradle task named `signReleaseBundle` does not prove that a signing key was configured.

## Optional signing

Provide all four environment variables through your local secret manager or CI secret store:

- `ACE_RELEASE_STORE_FILE`: absolute path to the existing upload keystore
- `ACE_RELEASE_STORE_PASSWORD`
- `ACE_RELEASE_KEY_ALIAS`
- `ACE_RELEASE_KEY_PASSWORD`

Partial configuration fails early. No default or debug key is used for release. Do not print these values, put them in shell history, or commit them. The signed APK is emitted as `app-release.apk`; the AAB path stays the same.

Verify signatures with Android SDK `apksigner verify --verbose` for the APK and JDK `jarsigner -verify` for the AAB. Confirm the certificate fingerprint matches the intended upload key and keep an independently verified backup.

## Publication requirements

Before an external release, the owner must resolve:

- Permanent application ID. Source still uses `com.example.acetimetracker`; a prior proposed replacement must be confirmed before migration.
- Ownership and configuration of the release Firebase project, including rules, storage access, and real account flows.
- Complete in-app and external account/data deletion, privacy policy, and accurate Data Safety declarations.
- The special-use foreground-service declaration and its actual timer behavior.
- A device walkthrough covering permissions, background/lock behavior, timer recovery, photos, history, themes, and accessibility.
- Signing identity, key backup, version code, store listing, and applicable Play testing requirements.

No local build or demo screenshot proves these requirements are complete. Store policy requirements must be rechecked against current official guidance at submission time.

## Current limitations

The app supports paused local timer draft recovery, not uninterrupted tracking after force-stop or reboot. Demo categories and entries persist locally. Category editing, past-session editing, an account-scoped entry/category outbox, and opt-in weekday reminders are implemented. Outbox retry runs while the process lives and on authenticated relaunch; media uploads still need connectivity. Complete account/data deletion remains a publication requirement. See [architecture](architecture.md).

The material under `play-store/` is prepared artwork and draft copy; it does not establish that a listing is published. Historical signing notes may refer to one developer's machine and are not portable setup instructions.
