# Release signing

Use the project's existing upload key if it is already registered with Google Play. Store the keystore outside this repository and keep its password in a secret manager.

The build accepts optional signing through four environment variables. See [release preparation](../docs/release.md#optional-signing) for the exact names and verification steps.

Before publishing, confirm the upload certificate, Firebase configuration, permanent application ID, and a restorable encrypted backup of the keystore and password. A previously signed disposable test bundle does not prove that the current release AAB is signed or ready to upload.

Never commit a keystore, password, keychain export, or service-account key.
