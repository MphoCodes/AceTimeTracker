# Daily workflows implementation plan

Goal: Implement the six approved product improvements as integrated Android workflows.
Architecture: Retain Compose, ViewModels and callback repository APIs while introducing account-scoped durable local storage and a retryable outbox. MainActivity owns the four tab screens and one timer ViewModel; reports use pure date-aware aggregation. Device entry points route through the authenticated main host.

## Constraints
- Preserve existing user edits, package identity, Firebase paths and debug/release separation.
- No backend deployment or store publication. Existing owner verification/signing requirements remain.
- Time entries retain stable IDs, edits preserve photo and account identity, categories retain historical labels when renamed.
- Keep UI responsive; disk, PDF work and retries off the main thread; scopes and listeners must terminate correctly.
- Local success means persisted locally. Never label pending or failed sync as synced.
- All six requested features are in scope. Client billing/invoicing and external calendar integrations were conditional future suggestions and remain out of scope.

## Tasks / contracts
- [x] Data: durable TimesheetRepository and CategoryRepository with existing callback interfaces; add updateCategory(userId, Category, callback Resource<Unit>), sync state observable, explicit retry. Account-scoped durable demo data. Test restart, update, delete, failed/stale acknowledgment, account separation.
- [x] Editing: existing TimesheetScreen/VM support editing by stable ID, preserve image, validation, repeat-to-timer callback; categories editable with local favorites.
- [x] Navigation: MainActivity single retained timer, state-preserving Today/Timer/Timesheet/Progress tabs, active timer strip, legacy activities route to main. MainScreen optional own bar disabled in host; HomeBottomBar takes selected destination.
- [x] Quick actions: ReminderScheduler + settings controls; notification pause/resume routes to MainActivity via EXTRA_TIMER_ACTION; home widget opens timer through main; no unguarded exported mutation endpoint.
- [x] Reports: new ReportsScreen(entries, categories, onBackClick), pure ReportSummary calculation, custom ranges, prior equal-period comparison, goal/actual, PDF and CSV file sharing; daily timeline with overlap/gap indicators and overnight clipping. Parent wires Progress tab to ReportsScreen.
- [x] Verify: focused local tests, all local suite, debug/release lint/build, emulator workflow checks, author review, docs/handoff update. Independent review stopped at its usage limit; its reported finding was fixed.

## Review focus
1. Queued edits/delete must not resurrect stale remote data or cross accounts.
2. Switching tabs/configuration must preserve running timer and draft state.
3. Overnight sessions and invalid dates must not inflate reports or crash timelines.
4. Widget/notification actions must authenticate and not duplicate timer state.
5. Notification denial and offline failure must leave primary tracking usable.

## Execution ledger
- Existing branch starts at ad5adb5. Only four personal .idea files modified; exclude from commits.
- User authorized implementing the preceding six-point proposal. Routine design choices resolved here without another approval gate.
- Parallel ownership: data repositories/local store; new reports files; reminder/widget/service/settings. Root owns navigation and entry/category editing. Root runs integration Gradle to avoid concurrent builds.

## Completion evidence — 2026-11-16
- 90 JVM tests and 3 API 36 device tests passed. Both lint variants, debug/release APKs and release bundle passed. Lint retains 284 warnings and no errors.
- Demo device journeys: running timer across tabs, pause/save, same-ID history edit preserving active duration, restart persistence, category rename/favorite, reports/timeline and PDF chooser. Reminder permission, time picker and scheduled AlarmManager record verified; test reminder disabled afterward.
- Regression fixes included rejected cross-account writes, stale snapshot/ack handling, repeated timer action idempotence, image editing restoration, and sub-minute display.
- Independent review was interrupted by reviewer usage limits after identifying an edit-photo recreation issue. Fixed retained-ViewModel overwrite and saved-state restoration; author reviewed integration. No claim of a completed independent review.
- Real Firebase/auth/media journeys, widget placement, reboot notification delivery and exhaustive configuration-change journeys remain unverified. Release artifacts are unsigned.
- README, architecture, project memory and HANDOFF updated. No source-ingestion tool available; existing graph retained and may be stale.
