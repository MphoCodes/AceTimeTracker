# Play release readiness — 16 November 2026

## Delivered

- Public preview: https://acetimetracker.vercel.app — APK, film, screenshots, installation instructions, privacy and deletion information. The public APK remains the checksum-pinned **sample-data debug preview**, not the new account-enabled release candidate.
- Owned Firebase project confirmed by IAM: `ace-time-tracker-3d7d3`. The unrelated old `acetimetracker-a3fb0` configuration is replaced. New production, debug and web clients are registered. Email/password authentication was already enabled.
- Firestore ownership/deletion-fence rules deployed. Live disposable-account checks passed: registration/sign-in, own profile/entry access, cross-account/anonymous denial, immutable entry owner and pending-deletion access denial. All test records and logins were removed.
- Account deletion implementation: password reauthentication, exact account binding, durable request, server cleanup with retry sweep, local cleanup/recovery and private image-cache removal. Three backend unit tests and four Firestore/Storage emulator tests pass. In-app Settings includes privacy/deletion links.
- Upload key generated outside Git; SHA-256 registered on production Firebase client. Signed release APK/AAB generated and signature-verified. The key is not a Play App Signing key unless the owner chooses to use it that way in Play Console.
- Film: no spoken founder name; written credit retained; explicit Pomodoro pronunciation; 49.8s, 1080p30, captions and editable OpenFilm source.

## Owner actions

1. **Firebase billing:** open https://console.firebase.google.com/project/ace-time-tracker-3d7d3/usage/details and link a billing account/upgrade to Blaze. Storage and deployed Cloud Functions require billing. Set budget alerts; alerts are not a spending cap. No billing account was linked by this work.
2. **Public contact:** provide the email to publish for support, privacy and deletion requests. The site explicitly labels the contact route as pending; it is not yet a complete Play privacy-policy submission. Do not put private requests in GitHub issues.
3. **Play Console:** create/access the developer account, complete identity/device/organisation verification applicable to that account and accept the developer agreements yourself. Confirm the listing developer identity and contact information. Register package `com.mphocodes.acetimetracker` and enrol in Play App Signing. Upload-key certificate is included in the private local readiness output, but the key/password are not.
4. **Signing backup:** securely back up the complete local signing folder and verify it can be restored. It is at `~/Library/Application Support/AceTimeTracker/signing/`; the JSON contains passwords and must never be uploaded to GitHub, Vercel or a public release. Keep an encrypted independent copy, preferably in your password manager and a second secure location. Only one local copy currently exists.
5. **Testing:** install and review the candidate on a physical Android device after backend setup. For personal developer accounts created after 13 November 2023, Google currently requires at least 12 opted-in testers for 14 consecutive days before applying for production access. Website APK downloads do not count as that Play closed test.

## Remaining engineering gates after owner input

- Provision a default Storage bucket in the intended region with deliberate soft-delete/versioning retention, then refresh both Android `google-services.json` files and the web SDK configuration (the current project had no bucket).
- Deploy Storage rules and all three account-deletion functions using `backend/README.md`, initially with `ACCOUNT_DELETION_ENABLED=false`. Set up deletion failure/age monitoring, billing alerts and operator access.
- Verify real account sign-in, profile/category/entry sync, private photo upload/read, cross-account denial, password reset, process restart, and full deletion end to end including Storage, Firestore, Auth and local data. Test retry after interrupted cleanup and lost callable response.
- Enable the backend parameter and the website portal only after those checks; publish the support email and final retention wording. A request acceptance response is not completion of deletion.
- Complete device/accessibility/widget/reminder/background timer checks in `docs/device-checks.md`. The timer foreground-service declaration requires an accurate demo showing user initiation, notification, background behavior and stop action; the marketing film alone is not that proof.
- Finalise Data safety, content rating, target audience, ads declaration, app-access/reviewer instructions, privacy URL and external deletion URL. Resolve any Play pre-launch report findings, then run the required test track and submit for review.

## Data safety working map (not a submitted declaration)

| Data | Purpose | Source / deletion |
|---|---|---|
| Email, account ID, optional name | Authentication, profile, account management | Firebase Auth + user profile; deleted through account cleanup |
| Optional profile/entry photos | User-selected app functionality | Firebase Storage under user UID; deleted by account cleanup |
| Categories, notes, time entries and time zones | Tracking, history, reports and sync | Firestore + local store; account cleanup removes server data and this device's local copy |
| App preferences and timer draft | Local app functionality | On device; clear storage/account cleanup where account-scoped |
| Exported PDF/CSV | User-requested sharing | User-selected destination; external copies remain user's responsibility |
| Minimal deletion ID/status/timestamps/attempt count | Prevent stale writes and recover failed cleanup | Retained operational record; disclose final retention practice |

No ad/analytics SDK is included in the current dependency set. Firebase/Vercel operational processing and the exact Google Play collection/sharing exemptions must be reviewed against the final production configuration; do not tick “no data collected” for the account-enabled app. Account creation and stored content are optional only if the final release actually offers a usable account-free path (currently the sample demo is debug-only).

## Official references checked

- Firebase Storage billing: https://firebase.google.com/docs/storage/faqs-storage-changes-announced-sept-2024
- Play account deletion: https://support.google.com/googleplay/android-developer/answer/13327111
- User Data/privacy: https://support.google.com/googleplay/android-developer/answer/10144311
- New personal-account testing: https://support.google.com/googleplay/android-developer/answer/14151465
- Foreground service declaration: https://developer.android.com/develop/background-work/services/fgs/service-types

Recheck policy details at submission. A signed file and passing local tests do not establish Play approval or a working deployed deletion backend.
