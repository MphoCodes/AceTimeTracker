# Contributing

Please open an issue for a substantial feature or data-model change before implementing it. Small, focused fixes can go directly to a pull request.

## Local setup

Follow [Android development](docs/development.md). Use debug test sign-in for local UI exploration. Never use production accounts or upload private photos as test fixtures.

## Pull requests

Describe the problem, the resulting behavior, and how you verified it. Include screenshots for visible UI changes and a regression test for meaningful behavior changes. Preserve the existing Compose theme and repository/ViewModel boundaries.

Run:

```sh
./gradlew --console=plain :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
./gradlew --console=plain :app:lintRelease :app:assembleRelease :app:bundleRelease
```

For device behavior, run relevant instrumented tests and describe the device/API and manual checks. A compiled test APK is not a device test result.

Do not commit keystores, passwords, service-account JSON, local SDK paths, build artifacts, or personal IDE state. Share APKs through CI artifacts or GitHub Releases. Do not change package identity, deploy backend rules, or migrate live data as part of an unrelated fix.

## Bug reports

Include the app variant, Android version, steps to reproduce, expected/actual behavior, and a redacted log or screenshot if useful. Indicate whether the issue occurs with demo data or a configured Firebase backend.

## License

Contributions are provided under the repository's [MIT License](LICENSE).
