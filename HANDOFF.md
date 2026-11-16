# Handoff — 2026-11-16

## Scope and provenance

Implemented the six approved daily-workflow improvements: entry/category editing, durable offline data and sync status, persistent navigation/shared timer, quick actions/reminders/favorites, reporting/export, and daily timeline. This supersedes the November 13 implementation and artifact notes.

Origin: https://github.com/MphoCodes/AceTimeTracker.git
Starting commit: `ad5adb54e7b9ee09a256be319a5c0334367c4363` on `main`.
Prior user push authorization applies to this implementation. Personal changes in four `.idea` files are excluded. No Firebase deployment, package-identity change or store publication.

## Delivered workflows

- Account-scoped SQLite records and transactional outbox for entries/categories, stable IDs, revision-checked acknowledgments, persistent tombstones, server-snapshot watermarks and explicit sync status/retry. Demo data is durable and never uploaded.
- Entry editing with validation, notes/photo support, same-ID updates and preserved active duration when the clock range is unchanged. Repeat tracking refuses to overwrite an existing timer draft.
- Category editing, validated hour targets/colors and per-account favorites. Rename preserves historical entry labels.
- Shared Today/Timer/Timesheet/Progress host, saved tab UI state, one retained timer ViewModel and running/paused session strip. Legacy activities redirect to the host.
- Immutable notification pause/resume intents, authenticated timer shortcut widget, opt-in weekday reminders with inexact alarms and permission feedback.
- Custom-range reports, equal prior-period comparison, category goals/actuals, daily gap/overlap timeline, overnight clipping, formula-safe CSV and multi-page PDF sharing. Heavy calculations/export/storage run off the UI thread.
- Existing paused-draft recovery, stable timer retry IDs, writer ownership, debug/release authentication separation and restricted backup behavior retained.

## Verification

Final commands, using Android Studio JBR:
```sh
./gradlew --console=plain :app:testDebugUnitTest
./gradlew --console=plain :app:lintDebug :app:lintRelease \
  :app:assembleDebug :app:assembleRelease :app:bundleRelease \
  :app:connectedDebugAndroidTest
```

**90 JVM tests and 3 instrumentation tests passed, zero failures/errors/skips.** Device tests include real PDF generation/rendering and the package smoke test. Debug/release lint passed with **284 warnings and zero errors**. There is only one application module, `:app`. `git diff --check` passed.

Manual API 36 Business_Phone debug/demo checks:
- Start timer, change primary tabs while it runs, pause from the shared strip and save.
- Edit the resulting entry without duplicating it or inflating its 48 seconds of active time.
- Force-stop/relaunch and verify the entry and edited note survived.
- Rename/favorite a category and verify the updated list and dashboard.
- Open report/timeline and Android PDF share chooser; no recipient selected or report sent.
- Enable reminder, grant notification permission, inspect time picker and verify the scheduled inexact AlarmManager record. Disable test reminder afterward.

An independent reviewer found a photo-restoration issue on recreation. Fixed reinitialization of the retained edit ViewModel and preserved the selected URL/pending upload in saved state. The reviewer then reached its usage limit; no completed independent review is claimed. Author integration review and full verification completed. The specific real-photo recreation journey remains unverified.

Compose preview rendering aborted during compilation; the report screen was instead inspected on the emulator. The user's emulator remains running with the debug app installed. Only sample/demo records were changed.

## Release artifacts

- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/release/app-release-unsigned.apk`
- `app/build/outputs/bundle/release/app-release.aab`

AAB checked with jarsigner: **unsigned**. SHA-256:
- AAB: `19328ef68739a1dd089eb044ef64b596222b4e454e71a58380eb27d26226bb49`
- Release APK: `15543508a86f4586f1490e440db43220c639ec60a7b95c21a514e62ddfed6e49`

## Boundaries and remaining publication work

- Sync retries while the process lives and on authenticated relaunch; background delivery after process death is not guaranteed. Real-account photo uploads require connectivity.
- Live Firebase authentication, ownership rules and media flows were not exercised. Backend deployment and production data migration were not performed.
- Timer process loss restores the last checkpoint paused; it is not a reboot-continuing timer engine. Local date/time fields do not encode a timezone.
- Widget placement, actual scheduled/reboot notification delivery, broad accessibility and all form factors need further device validation.
- Release owner must confirm package/Firebase ownership, implement complete account/data deletion and disclosures, supply intended signing credentials, test signed release and complete store declarations.
- Broader goals/routines, billing and external calendar integrations remain future work. The six daily-workflow features above are implemented.
- No project source-ingestion tool was available. No existing graph was replaced; indexed historical context may be stale. README, architecture, plan and portable memory reflect this pass.

## Brand and usability refinement — 2026-11-16

User approved the proposed visual/usability direction and requested implementation.
- New editable vector A/clock identity, adaptive launcher, monochrome notification icon, branded Android 12+ system splash and a 420ms Compose reveal within the existing 700ms launch delay. Light/dark splash previews rendered successfully.
- Warm ivory/charcoal/crimson theme, clearer dashboard action hierarchy and a lighter bottom dock. Primary navigation is Today / Timer / History / Reports; secondary drawer no longer duplicates these routes.
- Add time opens the form, History opens saved entries, form title reflects add/edit state, and saving returns to history. Route requests and tab state survive recreation.
- Empty reports offer Start session / Choose another period before any empty goals or timeline. Routine successful/demo sync text is hidden; pending/error states remain visible.
- New README screenshots captured from the debug demo. Source mark: docs/brand/ace-mark.svg.

Verification: 90 JVM tests and 3 device tests passed. Final debug/release builds and both lint variants passed (279 warnings, zero errors). Device tests preceded only the final large-font label adjustment; final JVM/lint/build checks include it. Rendered splash in both themes and bottom dock at 320dp/1.5x font; fixed word wrapping found in that preview. Demo device checks verified Today → Add time, History, report → Timer, and editing/saving a sample entry returning to the same three-entry history. No live account or backend mutation performed.

Release outputs rebuilt and remain unsigned; older checksums above are superseded. Personal .idea changes remain excluded. No source graph refresh tool is available; portable context is current.

## Owner brand correction — 2026-11-16

The owner rejected the A-ring and warm palette as a loss of Ace identity. Restored the original bright-red/slate/white tokens and red dashboard. Replaced the A-ring with simplified red target rings and a blue dart, using matching vector, adaptive launcher, notification silhouette and SVG assets. Retained all navigation/form/empty-state improvements and the brief splash reveal. Refreshed README screenshots.

Verification: local suite and debug/release APK, release bundle and lintRelease passed. Splash preview rendered (preview continued to display a monochrome cached rendering; source uses Image to preserve vector colors). Installed rebuilt debug app and checked navigation while refreshing screenshots. No data or backend changes. Existing release-signing/publication limitations remain.
