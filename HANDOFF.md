# Handoff — 2026-11-13

## Scope and provenance

User requested feature completion, best-practice cleanup, a release build, and public-facing GitHub presentation. This pass hardened the existing time tracker. The larger historical goals/routines/offline/reminders roadmap is not complete and must not be represented as shipped.

Origin: https://github.com/MphoCodes/AceTimeTracker.git
Starting HEAD: `31b8cec104ce89075a34f51c1bbee1e11fe8f654`.
The owner subsequently requested a push to origin/main. This handoff accompanies the implementation commit; store publication is not included. Existing auth/photo/UI/Gradle changes are included; personal IDE modifications remain local.

## Changes

- Per-account paused timer draft recovery with checkpoints and monotonic active timing.
- Stable per-mode entry IDs for save retries; remote writes use supplied IDs, and demo saves upsert those IDs.
- Local checkpoints are flushed off the main thread before submission. Writer tokens prevent an older ViewModel callback from overwriting a newer draft.
- Timer mutation controls are guarded during save; users can leave after a pending-save explanation.
- CSV quoting covers every field and neutralizes spreadsheet formulas in user text.
- Category targets reject nonfinite and out-of-range values; removed obsolete global category writes.
- History deletion now reports errors and guards repeated concurrent requests.
- Timer service stopping uses stopService rather than starting a service just to stop it.
- Backup/transfer allowlists appearance settings instead of restoring account sessions, drafts, or media caches.
- Optional release signing through four ACE_RELEASE_* environment variables; no credentials added.
- README with screenshots and accurate feature/setup boundaries, development/architecture/release guides, contributing/security docs, release CI artifacts, and root portable context.
- Old tracked root app-debug.apk moved to ignored app/build/legacy-artifacts/app-debug.apk. New artifacts belong in CI/Releases, not source control.

## Verification

Final host command:
```sh
JAVA_HOME='<Android Studio JBR>' ./gradlew --console=plain \
  :app:testDebugUnitTest :app:lintDebug :app:lintRelease \
  :app:assembleDebug :app:assembleRelease :app:bundleRelease
```

Passed: **43 JVM tests, 0 failures, 0 errors**. Added regression coverage for draft recovery/reset/account separation, delayed save callbacks, retry identity, category validation, and CSV export. Initial red runs reproduced timer data loss, invalid targets, broken CSV quoting, and duplicate save behavior.

Debug/release lint: no errors. Release report retains **282 warnings**, largely legacy resources/layouts and dependency suggestions. Do not describe it as warning-free.

Instrumentation APK compiled. **1 instrumentation package smoke test passed** on API 36 (Arc_Testing); this is not complete journey coverage.

Manual API 36 debug/demo checks on final APK:
- Quick test sign-in and dashboard.
- Denied notification permission; timer remained usable.
- Selected Development, ran stopwatch, confirmed pause-and-leave.
- Reopened the timer with the same 51-second paused draft.
- Force-stopped/reopened and recovered the paused draft.
- Saved it, saw timer reset and saved-session feedback, then saw Development in dashboard recent sessions.

Emulator was booted read-only without snapshot save, preserving its persistent disk. Live Firebase authentication/storage flows, release-device sign-in, broad accessibility, all form factors, and reboot tracking were not validated.

Independent review identified a pending-save duplicate/stale-draft race; fixed with stable IDs and writer ownership and covered by tests. A follow-up review noted offline navigation blocking; corrected to allow leaving with pending-save feedback.

## Artifacts

- `app/build/outputs/apk/release/app-release-unsigned.apk`
- `app/build/outputs/bundle/release/app-release.aab`
- `app/build/outputs/apk/debug/app-debug.apk`

AAB inspected with jarsigner: **unsigned**.
SHA-256:
- AAB: `a585e0dcd8db8ff63421bc85f66d16932632746a98bf8616430e6dde07e71ea3`
- Release APK: `91ce5df3187ff7381f651a504c9980818509408fe2b9b4c932476db0bf27a1ea`

## Remaining work before publication

- Confirm final package ID and owned Firebase release project; verify deployed access rules and real account/media flows.
- Implement complete account/data deletion and publish privacy/deletion disclosures; finish store declarations.
- Supply the intended signing key, verify its certificate/backup, and validate the signed release on devices.
- Broader roadmap features (history editing, category renaming, durable offline synchronization, reminders) remain unimplemented.
- CI configuration was updated locally but has not run on GitHub.
- No project knowledge-ingestion tool was found. No graph was rebuilt or replaced; historical indexed context may be stale. Portable files reflect this pass.


## Bottom navigation refinement — 2026-11-13

- Extracted HomeBottomBar into a reusable themed component with a rounded floating surface, subtle outline/shadow, branded active indicator, and system navigation insets. Existing dashboard routes remain unchanged.
- Added light, dark, and 320dp/1.5x font previews. Large-text labels use Log/Stats with full accessibility descriptions. Visually checked all previews and the installed API 36 emulator dashboard.
- Passed :app:testDebugUnitTest, :app:lintDebug, :app:lintRelease, :app:assembleDebug, :app:assembleRelease, :app:bundleRelease and :app:connectedDebugAndroidTest. Release artifacts were rebuilt; checksums listed above describe the previous build. Release signing/publication blockers above still apply.
- Personal .idea changes excluded from the commit. No source knowledge-ingestion tool is available; portable handoff is current.
