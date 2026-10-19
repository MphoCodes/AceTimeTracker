# AceTimeTracker Android modernization: approved product and technical specification

- Date: 19 October 2026
- Status: Approved by the owner on 19 October 2026. P1 implementation is underway: the toolchain PR is verified; target-36 compatibility is awaiting its remaining runtime/CI checks.
- Baseline: `main` at `340958c1ccbc9f6b57cf9e1055ae671b8da7d27a`.
- Audience confirmed by the owner: **anyone building personal goals and habits**.

## 1. Outcome and boundaries

Make AceTimeTracker a beautiful, useful daily companion for turning personal goals into small, repeatable actions. A person should understand what to do next, start tracking quickly, trust their history, and see how that effort contributes to their chosen goals.

The owner requested native Android modernization with Compose, competitor research including Duolingo's engagement/reminder practices, code cleanup, separate work chats, and one pull request per implementation task. Android comes first. iOS work, its implementation chat, and a detailed iOS specification are deferred until the Android goal is met.

The owner approved this scope and these decisions on 19 October 2026. Approval is not evidence that any new behavior has been implemented. The delivery sequence is a roadmap; each stage receives its own executable implementation plan before code is written.

### Proposed success criteria

- A new person can create a simple goal and begin a session within 60 seconds in a usability walkthrough, excluding time they spend choosing credentials. This is a design target to test, not an observed result.
- Every new saved session appears consistently in history, goal progress, and summaries; repeated Save/Stop actions never add it twice.
- Tracking and saving an already configured goal work without connectivity, including after activity recreation and process death.
- Upgrading preserves all recoverable user-owned records and clearly distinguishes incomplete legacy summaries from precise sessions.
- Daily use is possible with notifications and optional analytics disabled.
- All primary screens use Compose; the build, behavior tests, accessibility checks, and representative device walkthroughs pass.

### Deliberate scope choices

The first version supports **time-based personal goals**: reading, exercise, learning, creative work, or other pursuits. A habit is a recurring plan attached to a goal. Daily and weekly duration targets are included. Arbitrary count-based habits, financial goals, public leaderboards, social networks, subscriptions, AI coaching, wearable apps, and iOS are later product decisions.

Existing Firebase accounts remain usable. Retain the Android application ID `com.example.acetimetracker` so the project can support an upgrade rather than silently creating a different installed app. Publishing, production Firebase changes, and merging PRs require their own explicit release/integration decision; opening PRs is already authorized.

## 2. Migration approaches considered

| Approach | Benefit | Cost/risk | Decision |
| --- | --- | --- | --- |
| Incremental replacement in the existing Android app | Preserves identity and historical access; yields small PRs and a runnable app throughout | Requires temporary adapters between old activities and new screens | Recommended |
| Replace the entire Android app in one rewrite | Clean starting point | Delays usable results and concentrates data, lifecycle, and UI risk into one change | Reject for this modernization |
| Introduce shared Android/iOS architecture now | Could share some future work | Adds a second platform before product behavior and data contracts are stable | Defer with iOS |

Compose and Views can coexist during migration. New screens use Compose, reusable UI components accumulate, and legacy screen layouts are removed only after their replacements are verified. This follows the [official Compose migration strategy](https://developer.android.com/develop/ui/compose/migrate/strategy). The Android manifest, string resources, and other appropriate XML resources remain valid project files.

## 3. Product structure and interaction

### Core loop

Choose a meaningful goal → commit to a manageable action → start a session → save/refine the result → see progress → choose the next action.

### Main destinations

| Destination | Primary information | Primary action |
| --- | --- | --- |
| Today | One suggested next action, due goals, current session, today's effort | Start/resume a session |
| Goals | Active/archived goals, weekly or daily targets, progress and schedules | Create or adjust a goal |
| Progress | Session history, goal comparisons, weekly trends, optional consistency view | Understand progress or correct an entry |
| You | Profile, sync/account state, theme, reminder controls, data controls, About | Adjust preferences |

Use bottom navigation on compact windows and an adaptive rail/list-detail arrangement when space permits. A running-session control remains reachable across destinations. Avoid navigation-click Toasts, forced waiting screens, and separate screens for redundant chart variants.

### Onboarding and accounts

1. Briefly communicate the benefit with one clear action.
2. Offer example personal goals and a custom goal; avoid student-only language.
3. Ask for a manageable daily or weekly time target, with editable suggestions.
4. Make the first session action immediately visible.
5. Offer reminders after the person has chosen a goal/time, with a skip option.

Preserve email/password sign-in, registration, and password reset. Existing signed-in users resume their app state rather than always visiting registration. Initialize the Firestore profile idempotently after successful authentication. Authentication is the initial account model; guest mode is deferred so it does not introduce a second account-ownership migration during the first release.

Profile display email and authentication email must have clear semantics. A display-name/photo edit cannot accidentally change sign-in credentials. A sign-in email change uses the supported authenticated verification flow and communicates its outcome. Saving text-only profile changes must work without uploading a new photo.

### Goals and routines

- A goal has a name, optional description, icon/color, daily or weekly target minutes, selected routine days, and a time zone.
- A target is positive and editable. Prefer one clear target over the old goal/minimum/maximum input form; retain old minimum/maximum values in migration provenance.
- Archive a goal without deleting its sessions. Confirm destructive history deletion and offer undo where practical.
- Goal details show progress, recent sessions, a prominent Start action, and the next scheduled opportunity.
- Consistency rewards completing the chosen schedule. Rest days are legitimate; missing a day does not erase historical effort.
- Daily periods begin at midnight in the goal's stored time zone; weekly periods begin Monday at midnight there. Retain effective-dated revisions of the target, cadence, routine and period time zone. Changes take effect at the next boundary of the existing period, recorded as an instant, and cannot rewrite completed historical periods. New goals begin immediately with the full chosen target for the first partial period, clearly labeled.
- Imported categories start in a visible **Finish setup** state. Preserve their original hour targets and provenance, but require the person to choose cadence, routine days and a time zone before calculating scheduled completion or enabling reminders. Historical duration remains visible; do not invent a previous routine or retroactively mark it complete.

### Tracking and manual sessions

- Support start, pause, resume, finish, and discard, with one active timer per local app installation.
- Finish produces one stable session ID; Stop/Save/retry cannot create multiple records for the same timer.
- Persist timer state and checkpoints outside the activity. Use monotonic elapsed time while a boot session remains valid; store wall-clock anchors for recovery.
- Persist each active interval between Start/Resume and Pause/Finish, including its UTC endpoints and measured duration. A saved session retains these intervals and sums their durations; paused time earns no progress. A new uninterrupted manual entry has one interval. Complete new sessions require a positive total duration.
- Rotation, navigation, backgrounding, screen lock, and process death do not discard a session. A reboot or implausible time change leads to an editable recovered draft rather than an invented duration.
- Use an appropriate user-visible ongoing notification for an active session and verify platform/background rules during the tracking stage. Do not use WorkManager as a per-second clock.
- Manual entry accepts a goal, start/end date-times, a note, and an optional image. Validate real calendar times and positive duration, including midnight, time zones, and daylight-saving ambiguity.
- Keep sub-hour precision. Do not truncate each entry to integer hours before summing.
- Stage selected images in app-private storage, show pending/retry state, and associate uploaded files only with the intended saved session. Saving must not race an incomplete upload or retain a temporary picker URI as a remote image URL.
- Preserve existing legacy image references when available; allow private user-owned storage paths for new uploads.

### Progress and visual design

Use one trustworthy progress model for Today, Goals, and Progress. Calculate totals from canonical sessions; show minutes/hours with readable labels. Provide period filters and a text equivalent for every chart. For goal progress, split active intervals at the boundaries defined by that goal's effective period/time-zone revision. Recording time zone is history-display metadata, not an alternative completion rule. Pauses earn no duration on either day. An all-goal report uses one explicitly displayed reporting zone, initially the device zone, rather than mixing incompatible day boundaries. Retain summary-only legacy records on their recorded date, labeled as summaries without fabricated interval allocations or historical target completion.

The proposed visual direction is calm, confident, and encouraging: warm neutral surfaces, a refined coral/red brand accent, restrained success colors, generous spacing, clear typography, and a prominent time/progress display. A brief completion animation should reinforce a saved action. Animation is decorative and respects system settings; it must not obscure state or delay interaction. Specific tokens and representative screen previews are delivered with the Compose design-system stage before broad screen migration.

Support light, dark, and system themes from startup. Use meaningful loading, empty, offline, failed, and pending-sync states. Make controls at least 48dp, support TalkBack and large text, and avoid conveying progress by color alone. See [Android accessibility guidance](https://support.google.com/accessibility/android/answer/7101858).

## 4. Architecture and source layout

Keep one app module initially, organized by feature with small shared packages. Do not introduce a large module graph to modernize 19 source files.

```text
app/src/main/java/com/example/acetimetracker/
  app/                  Application container, activity, navigation/session routing
  core/model/           Goal, session, profile, legacy provenance, duration/time policy
  core/data/            Repositories, persistence, Firebase adapters, migration and sync
  core/designsystem/    Compose theme, shared controls, previews
  core/reminders/       Reminder policy and platform scheduling adapters
  feature/auth/         Sign-in, registration and recovery
  feature/today/        Today and onboarding
  feature/goals/        Goals and routine editing
  feature/tracking/     Timer state machine and active-session UI
  feature/history/      Entry editing, attachments and history
  feature/progress/     Period aggregation and accessible summaries
  feature/profile/      Profile, preferences and data controls
```

Screen composables render immutable UI state and emit actions. ViewModels expose lifecycle-aware `StateFlow`; repositories own data access. Duration math, aggregation, migration decisions, reminder eligibility, and timer transitions are testable Kotlin logic with injectable clocks. Firebase SDK calls do not live in composables.

A small application-scoped dependency container is sufficient initially. Introduce additional dependency-injection machinery only when its lifecycle or testing benefit is concrete.

### Local persistence and remote sync

Use Room for the local goal/session/timer state that screens observe, DataStore for device preferences, and Firebase Authentication/Firestore/Storage for account and remote synchronization. Room is the UI's local source of truth; Firestore is the remote account store, not a second independent UI model. This follows Android's [offline-first guidance](https://developer.android.com/topic/architecture/data-layer/offline-first).

Record local writes and an outbox operation in one local transaction. Stable entity/operation IDs make retries idempotent. A connectivity-aware worker drains pending operations with bounded backoff and exposes pending/error state. Server-confirmed revisions and tombstones prevent a delayed offline edit from silently resurrecting deleted data; conflicting edits become a resolution state. New session identities do not depend on a mutable category name.

Firestore already has offline caching and queued writes, with last-write-wins behavior for changes to a document. Room's outbox is the sole durable queue for new canonical goal/session mutations. Its remote adapter uses online transactions that validate the expected entity revision and atomically record an immutable operation receipt. A retry first checks that receipt; a committed operation with a lost acknowledgement is acknowledged locally without applying it twice. An offline failure remains pending in Room; do not issue a separate ordinary/batched Firestore write as a fallback. Legacy cached writes remain subject to the migration reconciliation policy. See [Firestore offline behavior](https://firebase.google.com/docs/firestore/manage-data/enable-offline) and [transaction semantics](https://firebase.google.com/docs/firestore/manage-data/transactions).

Keep local records partitioned by account ID. On sign-out/account replacement, first pause and checkpoint the originating account's active timer, cancel its notification/actions, then stop its listeners/workers and reminders and remove its data from active UI access. The paused timer does not block a different account from starting its own session; its owner must explicitly resume it after returning. Notification actions and in-flight async callbacks validate both the originating UID and timer/operation identity before acting. Warn about unsynced local work before a destructive local clear. Account A's drafts, attachments, and pending writes cannot appear or upload under account B.

### Approved compatible toolchain snapshot

Research date: 19 October 2026. P1 Task 1 built this combination locally, from a clean tracked-source export, and in submitted-head CI in [PR #4](https://github.com/MphoCodes/AceTimeTracker/pull/4). The catalog and wrapper checksum pin the resolved versions. Later-stage libraries remain unimplemented decisions until their owning stages validate them; publication alone is not a passing build.

| Component | Proposed pin/decision |
| --- | --- |
| Android Gradle Plugin | `9.3.3` |
| Gradle wrapper | `9.5.0` |
| Gradle runtime | JBR/JDK 21; keep machine-specific paths untracked |
| Kotlin and Compose compiler plugin | `2.4.21`, same compiler/plugin version |
| Java/Kotlin bytecode | 17, independently of the Gradle runtime |
| Android SDK | `compileSdk 37`, `targetSdk 36` by P1 completion, `minSdk 27` retained; the toolchain-only task temporarily retains target 34 until its paired compatibility task passes |
| Compose BOM | `2026.09.00` |
| [Activity Compose](https://developer.android.com/jetpack/androidx/releases/activity) / [Lifecycle](https://developer.android.com/jetpack/androidx/releases/lifecycle) | `1.13.0` / `2.11.0` |
| [Navigation 3](https://developer.android.com/jetpack/androidx/releases/navigation3) / [Material 3 Adaptive](https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive) | `1.2.0` / `1.3.0` |
| [Room](https://developer.android.com/jetpack/androidx/releases/room) / [DataStore](https://developer.android.com/jetpack/androidx/releases/datastore) / [WorkManager](https://developer.android.com/jetpack/androidx/releases/work) | `2.8.5` / `1.2.1` / `2.12.0`, introduced with the stage that uses them |
| [Firebase Android BOM](https://firebase.google.com/support/release-notes/android#2026-10-15) | `35.0.2`; SDK migration is validated explicitly |

AGP 9.3 documents API 37 support and Gradle 9.5.0. Its 9.3.3 patch includes relevant D8/R8 fixes. Kotlin's published fully tested range for 2.4.21 ends at AGP 9.3.1; choosing 9.3.3 is an engineering inference within the patch line, and P1 must prove it through dependency metadata checks, assembly, lint, and tests. Do not silently present the combination as certified or fall back to a known-regression patch without review. See [AGP release notes](https://developer.android.com/build/releases/agp-9-3-0-release-notes), [Kotlin compatibility](https://kotlinlang.org/docs/gradle-configure-project.html), and [Gradle Java compatibility](https://docs.gradle.org/current/userguide/compatibility.html).

Use AGP's built-in Kotlin support: remove `org.jetbrains.kotlin.android`, explicitly align the built-in Kotlin Gradle Plugin runtime to the chosen Kotlin version, apply the matching Compose compiler plugin, and migrate `kotlinOptions` to `kotlin.compilerOptions`. Do not hide incompatibilities behind permanent built-in-Kotlin/DSL opt-out flags. See [built-in Kotlin migration](https://developer.android.com/build/migrate-to-built-in-kotlin) and [documented KGP override](https://developer.android.com/build/releases/agp-9-0-0-release-notes#upgrade-to-a-higher-kgp-version).

Current Compose/navigation artifacts impose higher SDK/AGP metadata requirements than the repository's old toolchain. Check `:app:checkDebugAarMetadata` instead of upgrading isolated version numbers. See the [official Compose BOM](https://dl.google.com/dl/android/maven2/androidx/compose/compose-bom/2026.09.00/compose-bom-2026.09.00.pom) and [Navigation 3 releases](https://developer.android.com/jetpack/androidx/releases/navigation3).

Migrate obsolete Firebase KTX imports to the main modules during dependency modernization; the current `Login.kt` still imports old KTX packages. The BOM update is not just formatting. See [Firebase Kotlin migration](https://firebase.google.com/docs/android/kotlin-migration) and [Android SDK release notes](https://firebase.google.com/support/release-notes/android#2026-10-15).

Compile against API 37 for current dependencies; start the foundation at target API 36, then complete the API 37 behavior audit and target update in P11. Reverify Play requirements before a release; the snapshot requirement for new apps/updates is API 36. See [target SDK requirements](https://developer.android.com/google/play/requirements/target-sdk). This spec does not authorize store publication.

## 5. Canonical data contract and legacy migration

### Remote collections

| Path | Purpose |
| --- | --- |
| `users/{uid}` | Profile and account schema metadata |
| `users/{uid}/goals/{goalId}` | Goal identity, target/cadence, routine, archive state |
| `users/{uid}/sessions/{sessionId}` | One canonical saved duration/session with optional note and attachment |
| `users/{uid}/migrationRecords/{sourceKey}` | Provenance, deterministic mappings, imported/skipped/review status |
| `users/{uid}/syncReceipts/{operationId}` | Immutable acknowledgement written atomically with a canonical mutation to make retries safe |

Session duration is a `Long` count of milliseconds. Complete new sessions have a positive total, UTC outer start/end instants, recording time zone and ordered non-overlapping active intervals; their total duration is the sum of active interval durations. The outer interval alone must never be used to allocate paused-session progress. A legacy summary may have only its original local date and nonnegative duration. Missing start/end times or intervals remain missing: no synthetic midnight timestamps or fabricated notes/images.

Each entity has a stable ID, schema version, revision/update metadata, and explicit deletion/archive semantics. A session preserves its goal-label snapshot for history even if the goal is renamed or later unavailable. Legacy goal mapping uses source document identity where available; ambiguous duplicate names require review rather than an arbitrary first match.

### Current sources and decisions

| Existing source | Existing usage | Migration policy |
| --- | --- | --- |
| `users/{uid}/categories` | Category management and manual selection | Import owned categories into goals requiring routine setup; retain original fields/provenance without inventing cadence/zone |
| `users/{uid}.dailyTimesheetEntries` | Written by manual entry, read by pie chart | Import as summary-quality sessions using only fields actually stored |
| `timesheetEntries` filtered by `userId` | Read by My Progress | Import only records with verified ownership; preserve available detail |
| Global `Category` and `totalTime` | Timer selection/aggregate writes | Preserve untouched; do not assign aggregate values to a person or fabricate individual sessions |

Migration must support dry-run reporting, incremental batches, restart after interruption, and deterministic source keys. Use an allowlisted pilot, explicit write limits, create-only canonical writes, a created-document manifest, and resumable checkpoints. Protect migration provenance from ordinary client edits. Deduplicate repeated imports by source identity while preserving any duplicate multiplicity actually present in source arrays. Similar records from different sources are not automatically the same session; place ambiguous cross-source matches in a review report. Previously collapsed identical array entries and unstored notes/images cannot be recovered from this repository alone. Quarantine malformed, negative, contradictory, or uncertain records; do not silently count them as verified progress.

Do not delete legacy source records during the modernization. Use a migration version and per-source reconciliation so that delayed writes from older/offline clients can be discovered after initial import. A live production inventory and explicit cutover plan are prerequisites to a production data migration; a timestamp in the new client alone cannot prove that old clients have stopped writing.

Rollback must retain sessions created after cutover. Revert a screen through the compatibility adapter or disable the new surface while keeping canonical writes accessible; restoring an old reader/writer alone is not a complete rollback. Verify reconciliation and rollback against mixed legacy/canonical fixtures before changing a live client cutoff.

Ship versioned Firestore/Storage rules and required indexes alongside code. Emulator tests must prove account isolation and permitted writes before any live deployment. Production backups, actual rule state, release signing, store access, and live data volume are not known from this repository and must not be assumed.

## 6. Reminder and engagement policy

Duolingo's published engineering describes selecting reminder copy using experiment outcomes and avoiding over-repeated messages. That is evidence about one published system, not a complete description of today's internal telemetry or notification scheduler. Our proposed adaptations must be labeled as our own. See [Duolingo's engineering article](https://blog.duolingo.com/hi-its-duo-the-ai-behind-the-meme/).

### First reminder release

- Reminders are opt-in, attached to the person's selected routine and local time, with an editable schedule.
- Default quiet hours are 21:00–08:00 in the device's current time zone. Settings clearly show the effective time. A reminder that falls inside quiet hours is skipped, not accumulated for a burst later.
- Limit automatic goal-engagement reminders to one per account per device per local day across all goals. A person can start, snooze, or disable reminders; ongoing active-timer status is a separate notification channel.
- Allow one explicit 20-minute snooze of that day's original reminder. It replaces the notification within the same stable reminder chain; it is the sole exception to the automatic posting cap. Repeated Snooze taps cannot schedule additional follow-ups. If the follow-up would land in quiet hours or on another local day, skip it.
- Recheck eligibility when work actually executes: permissions and channel enabled, correct account, configured/active goal, due day, no current session, no goal completion in the relevant period, and no quiet hours. An automatic nudge requires unused daily quota; a snoozed follow-up requires the original chain and its unused single snooze instead.
- Suppress while the app is being used. Reschedule after routine, permission, time-zone, or device-time changes.
- Pause engagement nudges after three scheduled-day automatic posting attempts without a subsequent completed session; snoozed follow-ups do not increment that counter. Show the paused state in settings; resume only when the person explicitly enables reminders again.
- Use inexact scheduling for engagement nudges and communicate that Android may delay delivery. Do not request exact-alarm privileges for ordinary habit nudges. WorkManager coordinates durable deferrable work; AlarmManager is considered only for the appropriate scheduling need. See [Android alarm guidance](https://developer.android.com/develop/background-work/services/alarms) and [notification permission behavior](https://developer.android.com/develop/ui/compose/notifications/notification-permission).
- No cloud push service or paid messaging infrastructure is required for the first local reminder implementation.

### Behavior signals and later personalization

Record only the app behavior needed to evaluate the goal loop: goal creation, session start/pause/finish, goal-period completion, reminder scheduling/posting attempts, taps, snoozes, and opt-out. Do not label an API posting success as confirmed delivery or an absent tap as a known dismissal. Optional product analytics excludes goal names, notes, images, email addresses, and raw text.

The person can disable optional analytics without disabling tracking or reminders. A local learning mode, if enabled, can propose a reminder window after at least five completed sessions on three distinct days in a rolling 14-day window. The proposed window is shown for approval; it never silently overrides a chosen schedule or quiet hours. Use simple deterministic scoring first. Machine learning and bandit selection are deferred until event quality, sample size, and an evaluation plan justify them.

Evaluate goal-period completion and completed-session return behavior, with reminder opt-out/frequency as guardrails. Notification click-through alone is not success. Any comparison of timing/copy needs randomized assignment or another credible control; an observational rise after a reminder is not proof of incremental benefit.

## 7. Delivery sequence: one coherent task per PR

Stage IDs below are roadmap identifiers, not GitHub PR numbers. Each implementation stage has a bounded plan, branch, validation evidence, review, and PR. Dependent stages execute in order; independent read-only research runs in parallel. Later code work uses isolated checkouts when parallel work would otherwise touch shared files. A PR should be reviewable without accepting its neighbors.

| Stage | Deliverable | Depends on | Minimum acceptance evidence |
| --- | --- | --- | --- |
| P0 | Research, proposed specification and roadmap | Current audit | Source links, legacy-source mapping, explicit assumptions and scope review |
| P1 | Compatible build/toolchain and CI foundation, then target-36 runtime compatibility in a separate PR | P0 approved | Reproducible clean checkout; debug assembly, lint and unit tests; dependency metadata/compatibility check; system-bar/back compatibility; no credentials in tracked config |
| P2 | Auth/profile correctness and app-session routing | P1 | Existing login/registration/reset work; profile bootstrap is idempotent; text-only edit works; persisted session routes correctly; account-switch tests |
| P3 | Canonical goals/sessions repositories and migration | P2 | Precision, ownership and idempotency tests; migration dry-run/fixtures; imported routine-setup state; emulator rules/index tests; old data retained |
| P4 | Durable local tracking data and synchronization | P3 | Offline writes/readback; process-death persistence; lost-ack receipt retries; active-timer sign-out/account separation; conflict and tombstone tests |
| P5 | Compose design system, navigation and preferences | P2–P4 | Representative Today/Goal/Timer previews; auth/back navigation; light/dark/system themes; large text, TalkBack and adaptive-window checks |
| P6 | Goal creation, editing, archive and routine UI | P5 | Create/edit/archive flows; daily/weekly boundaries; target-change history; historical sessions retained |
| P7 | Reliable active timer | P6 | Monotonic-time state-machine tests; paused overnight interval allocation; repeated finish; rotation, lock, background, process death and reboot recovery walkthroughs |
| P8 | Manual sessions, history editing and attachments | P7 | Overnight/DST validation; note/image preservation; pending/retry upload; edit/delete recalculates summaries; no duplicate saves |
| P9 | Today and Progress summaries | P8 | All screens agree on totals; minute precision; period/zone boundaries; legacy quality labels; accessible chart/text equivalence |
| P10 | Onboarding, reminders and optional measurement | P9 | Goal-first onboarding; denied permission; daily cap/single snooze/quiet-hours tests; completed-goal suppression; rescheduling, preference and event payload tests |
| P11 | Legacy removal and release-quality verification | P10 | Remove replaced activities/layouts/dependencies; current target behavior audit; min/latest representative device tests; critical flow, accessibility and performance evidence |

Research chats completed for competitor/engagement research and data/migration audit. Their initial student-oriented assumption is superseded by the owner's confirmed general personal-goals audience. The integrated findings are recorded in [the research brief](../../research/2026-10-19-modernization-research.md). This coordinating chat owns the integrated specification, dependency order, review outcomes, and the active modernization goal. Implementation chats receive the approved stage contract and its predecessor state; do not launch every stage simultaneously.

## 8. Verification and completion contract

### Baseline facts

This is the pre-implementation snapshot. Current build/runtime evidence is recorded in the P1 plan and implementation PRs.

- Nineteen Kotlin source files; activity-based XML screens; direct Firebase calls; no implemented repository/ViewModel separation.
- Existing tests are generated arithmetic and package-name examples, not behavior coverage.
- All 55 resource XML files parsed successfully during repository familiarization; this is not an Android compilation check.
- The shell's current `bash gradlew :app:testDebugUnitTest --console=plain` attempt fails before Gradle because no Java runtime is configured for the shell. Android Studio bundles JBR 21, while tracked IDE metadata references JBR 17. A local Android SDK path has not been established.
- The repository includes Firebase client configuration but no versioned Firebase rules/indexes or CI workflow. The live project state has not been inspected.

### Required validation

Unit tests own time math, timer transitions, aggregation, migration mapping, reminder eligibility and event payloads. Repository/emulator tests own account isolation, offline/sync semantics, rules and migration restart. Compose tests own primary navigation and editing flows. Device checks own permissions, background behavior, notification actions, image selection, process recovery, adaptive layout and accessibility. Add meaningful tests with the stage that changes behavior; avoid tests that merely restate implementation details.

Record exact commands/results in each implementation PR. CI should run `:app:assembleDebug`, `:app:lintDebug`, and `:app:testDebugUnitTest`; instrumented/emulator tests run where the environment supports them. A blocked check is reported as blocked, never as passed.

The Android goal is met when the agreed P1–P11 behavior is implemented, its validation evidence passes, every implementation task is available in its own reviewable PR, the resulting branches can be integrated coherently, and the final Android review has no unresolved required work. Open PRs alone do not prove the complete product works. Store publication and iOS are separate goals.

## 9. Review decisions and follow-through

Confirmed: general personal goals/habits audience; native Android/Compose modernization; stepwise PR delivery; separate chats; iOS deferred.

Approved: time-based goals first, incremental migration, the four main destinations, offline local persistence with Firebase synchronization, the visual direction, opt-in reminders and simple personalization before ML, and the staged delivery sequence.

The owner approved the [P1 implementation plan](../plans/2026-10-19-p1-android-foundation.md); its toolchain task is verified in PR #4 and its compatibility task is in draft [PR #5](https://github.com/MphoCodes/AceTimeTracker/pull/5). P1 ends at target 36 and P2 waits for both tasks' required evidence. A [proposed P2 plan](../plans/2026-10-19-p2-accounts-and-profiles.md) covers account/session routing followed by reliable profile editing and sign-in email changes. P2 implementation waits for its plan review. Later plans refine their own interfaces against this specification and the verified predecessor.
