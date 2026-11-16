# Ace account services

Prepared for the owner-controlled `ace-time-tracker-3d7d3` Firebase project. The default Firestore database is in `africa-south1`; event and scheduled functions use that region. Android/web callable clients use `us-central1`. This folder is deployment preparation, not evidence of live account deletion.

## Owner setup and rollout

1. Enable billing on the owned project. Provision the default Cloud Storage bucket and explicitly choose its region and retention. Decide whether soft-delete and object versioning should be disabled for prompt permanent erasure or retained for a disclosed recovery period. Logical object deletion alone does not erase retained versions. Do not enable account deletion until bucket behavior matches the privacy policy.
2. Use Node 22 and install locked dependencies: `cd backend/functions && npm ci`. Production dependencies are separated from emulator development tools. `npm audit --omit=dev` reported zero findings during preparation; development tooling had 18 findings (13 moderate, 5 high) and must not be included in the deployed runtime.
3. From `backend`, explicitly select the owned project on every command. Deploy reviewed rules with `firebase deploy --project ace-time-tracker-3d7d3 --only firestore:rules,storage`. Storage rules use Firestore to check deletion tombstones; allow the CLI to configure the required cross-service permission. The owner must verify this permission in the actual bucket.
4. Deploy all three functions together: `firebase deploy --project ace-time-tracker-3d7d3 --only functions`. Keep the Boolean parameter `ACCOUNT_DELETION_ENABLED=false` (its default) during initial rollout. The callable must remain closed until the processor, hourly sweep, bucket and rules are verified. Local environment files are ignored; never commit credentials.
5. With a disposable test account, validate registration/login, password reset, profile/category/entry read-write, photo upload, ownership denial, and deletion. Verify there are no production user records in the test account. Enable the parameter only for the controlled verification, redeploy functions, and disable it again if verification fails. The public deletion portal has a separate deployment gate; enable it only after this server verification.
6. Exercise interrupted/repeated requests, stale credentials, switched accounts, server-side cleanup failure/recovery, and offline devices reconnecting. Confirm profile subcollections, root entries and all UID media disappear, then Auth identity disappears and request status becomes `complete`. Confirm signed Android local cleanup/restart behavior on a device before release.

No Firebase credentials belong in this repository. `firebase.json` deliberately contains no project alias that could accidentally select the old unverified project.

## Deletion contract

`deleteAccount({confirm: true, expectedUid: currentUser.uid})` requires authenticated identity, an exact expected-UID match and authentication within five minutes. Reauthenticate with the current password, force-refresh the ID token and check the current identity before calling. Server cleanup always targets the authenticated UID; the client cannot supply a different target.

Response `{status: "accepted"}` means a durable request exists, **not** that erasure has completed. Rules immediately fence profile, category, entry and Storage access for that UID. Only the owner can get their request document; clients cannot list or modify requests.

The processor deletes `users/{uid}/` Storage objects, root `timesheetEntries` filtered by `userId`, recursively deletes `users/{uid}` including categories, and deletes Firebase Auth last. Operations tolerate repeats. Request documents retain only UID-keyed status, timestamps and attempt count; retain this minimal tombstone indefinitely to prevent delayed clients recreating data, and disclose this operational record.

New media paths are `users/{uid}/images/{id}` and `users/{uid}/profile_pictures/{id}`. Unscoped legacy `images/` and `profile_pictures/` paths are denied. Never deploy this cleanup on a different legacy project and assume it safely identifies old media ownership. Parent inspection found no Storage bucket in this owned project; no legacy media migration was performed.

## Monitoring and recovery

Monitor `deletionRequests` where `status == "pending"`, especially old `requestedAt` and increasing `attempts`/`lastFailureAt`. Configure an owner alert for function errors and requests older than the promised deletion period before production. Do not delete request records to retry them: that removes the write fence.

The Firestore creation trigger has retries enabled. An hourly scheduled sweep provides recovery beyond the event retry window. It processes at most 100 requests per run with a persistent document-ID cursor at `operations/accountDeletionSweep`; the cursor advances before processing each account and wraps after the final page so repeatedly failing requests do not starve later accounts. It may take multiple hourly runs for a large backlog. Trigger the deployed `retryAccountDeletions` Cloud Scheduler job manually after fixing an outage, then verify completion in Firestore and Auth. Never mark a request complete manually before verifying actual cleanup.

Default bucket absence, permissions or retention configuration are deployment blockers. Verify cleanup service account permissions, Cloud Scheduler invocation and Eventarc delivery in the live project. The local emulator tests do not verify these cloud permissions or billing.

## Android local handling

Settings contains irreversible confirmation and current-password entry. Passwords are held only in memory. On accepted requests, a committed recovery marker drives cleanup of timer drafts/writer ownership, favorites, SQLite records/outbox, report cache and prior Coil image cache, disables reminders, stops the timer service and signs out. All required preference/file/database results must succeed before clearing that marker; interrupted cleanup is retried on launch. A server tombstone check recovers a lost callable response on a subsequent connected session.

DurableStore remains the offline data source. Firestore disk persistence is cleared before initial operations and the SDK uses a memory cache. Coil private photo disk and memory caching are disabled globally to prevent in-flight requests repopulating erased caches. Other already-offline installations cannot be remotely wiped until they reconnect. Reports copied/shared outside the app remain under the recipient's control. Existing Firebase download URLs are shareable bearer links until their objects are deleted.

## Local checks

From `backend/functions`:

```sh
npm test
npm audit --omit=dev
```

From `backend`, with Java available:

```sh
functions/node_modules/.bin/firebase emulators:exec \
  --project demo-ace-deletion --only firestore,storage \
  'cd functions && node --test rules.test.js'
```

The demo project identifier prevents emulator tests from accessing a live backend. Three backend unit tests cover authentication/UID binding, cleanup failure ordering and fair retry traversal. Four emulator tests cover account isolation, filtered entry queries/immutable ownership, deletion fences and owner-only image paths/type checks. Android tests cover account-scoped row erasure and recovery marker retention on local cleanup failure. Live end-to-end function/Auth/Storage deletion remains a release gate.
