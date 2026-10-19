# AceTimeTracker modernization research

- Checked: 19 October 2026.
- Purpose: inform the proposed [Android specification](../superpowers/specs/2026-10-19-android-modernization-design.md).

## Scope and evidence

The owner confirmed that the new audience is anyone building personal goals and habits. The app's older student positioning is historical context, not a constraint on the redesign.

This is research from public first-party product documentation, engineering writing, a first-hand interview, official platform references, dependency metadata, and a read-only code audit. It does not include running competitor binaries, private traffic inspection, private telemetry schemas, or a live Firebase inventory. No installed skill matching the mentioned reverse-engineering name was found. These sources are sufficient for the current product proposal; the exact skill/link would be needed to reproduce that particular workflow.

Historical articles establish what was published at their date. They do not prove identical behavior across today's accounts, experiments, countries, and app versions. Recommendations below are proposed AceTimeTracker adaptations, not claims about competitor internals.

## Product comparison

| Product | Publicly documented pattern | Proposed lesson for AceTimeTracker |
| --- | --- | --- |
| Duolingo | Guided next actions, visible streaks, reminder settings and flexibility. [Guide](https://blog.duolingo.com/duolingo-101-how-to-learn-a-language-on-duolingo/) | Make the next manageable action obvious and keep actual goal completion distinct from consistency |
| Forest | Focus sessions contribute to an accumulating visual forest; strict modes have consequences for abandoning focus. [Product page](https://www.forestapp.cc/) | Make effort visible over time; preserve earned effort when a person stops early |
| Focus To-Do | Combines tasks, Pomodoro timing, reminders and completion/time reporting. [Features](https://www.focustodo.cn/) | Give time a purpose and place relevant actions together |
| TickTick | Stopwatch/Pomodoro options, linked focus sessions and flexible habit frequency. [Focus](https://help.ticktick.com/articles/7055782010496745472), [habits](https://help.ticktick.com/articles/7055781878401335296) | Offer flexible routines and a direct action from the reminder into the relevant goal |
| Toggl Track | Editable entries, reminder windows/weekdays, snooze and explicit idle-time choices in its desktop product. [macOS documentation](https://support.toggl.com/en-us/article/toggl-track-desktop-app-for-macos-1669b8x/) | Prioritize trustworthy, correctable history; phone inactivity does not prove that a reading/exercise session stopped |

The resulting direction is a goal-first personal companion, not a billing system or a full task-management suite. Start with duration-based daily/weekly goals and optional routine reminders. A focus countdown, richer outcome/check-in measures, or count-based habits can be evaluated after the core flow is reliable.

## What Duolingo's public evidence supports

- **Message selection:** A 2020 engineering account describes experiments with prewritten notifications and reducing recent repetition, evaluated against lesson completion. It is a useful example of testing helpful behavior rather than optimizing clicks alone. [Engineering account](https://blog.duolingo.com/hi-its-duo-the-ai-behind-the-meme/)
- **Timing:** Retention lead Jackson Shuttleworth described a reminder based on 23.5 hours after the previous day's first practice and reducing reminders after inactivity. This is a reported strategy to test, not a universal best time or a verified description of every current user. [First-hand interview](https://subclub.com/episode/how-to-time-reactivation-campaigns-for-maximum-impact-jackson-shuttleworth-duolingo)
- **Control:** The public product guide documents reminder settings and smart scheduling. A person's selected routine should remain the starting point. [Guide](https://blog.duolingo.com/duolingo-101-how-to-learn-a-language-on-duolingo/)
- **Streak tradeoffs:** A historical experiment made maintaining a streak easier and improved retention while fewer learners reached daily goals. Consistency and achievement must therefore be reported separately. [Experiment](https://blog.duolingo.com/improving-the-streak/)
- **Quality:** Its published Time Spent Learning Well metric reflects attention to the quality of activity. For AceTimeTracker, completed planned actions and the person's reported usefulness are stronger product signals than accumulated screen time alone. [Metric discussion](https://blog.duolingo.com/time-spent-learning-well/)

## Proposed engagement sequence

1. Make the goal/session/progress loop correct and easy to use before adding personalization.
2. Introduce opt-in scheduled reminders with local quiet hours, one daily nudge cap, a direct goal action, snooze/disable, completion suppression and an inactivity pause.
3. Keep scheduled, posting-attempt, opened and completed-session events distinct; Android does not provide universal delivery certainty.
4. If the person opts in, use a simple local history-based suggestion before considering an ML model. A suggested time requires acceptance and respects quiet hours and manual choices.
5. Evaluate complete goal periods and useful returns, with opt-out/frequency pressure as guardrails. Use credible controls before claiming reminder-driven improvement.

The integrated spec fixes its own defaults: 21:00–08:00 quiet hours, at most one automatic nudge per account/device/local day, and one explicitly requested 20-minute snooze in the same day's reminder chain. Pause after three scheduled-day automatic posting attempts without a subsequent completed session; snoozes do not add to this counter. These are proposed AceTimeTracker policies; they are not copied competitor settings. Validate denied permission, time-zone changes, daylight saving, reboot, canceled goals and delayed background execution. [Android notifications](https://developer.android.com/develop/ui/compose/notifications/notification-permission), [alarm guidance](https://developer.android.com/develop/background-work/services/alarms).

## Data audit: verified source contracts

Baseline commit: `340958c1ccbc9f6b57cf9e1055ae671b8da7d27a`.

| Source | Evidence | Consequence |
| --- | --- | --- |
| `users/{uid}/categories/{id}` | [Categories.kt](../../app/src/main/java/com/example/acetimetracker/Categories.kt#L57) writes names, descriptions, goals and zero initial `timeSpent` | Keep source document identities; category names are not reliable unique keys |
| `users/{uid}.dailyTimesheetEntries` | [Timesheet.kt](../../app/src/main/java/com/example/acetimetracker/Timesheet.kt#L213) stores date, total hours, category and user ID | Description, image and interval details are not saved by this path |
| `timesheetEntries` | [Myprogress.kt](../../app/src/main/java/com/example/acetimetracker/Myprogress.kt#L224) reads user-filtered entries; no app writer exists | Progress and the manual-entry/pie-chart path cannot be assumed consistent |
| Global `Category` | [Timer.kt](../../app/src/main/java/com/example/acetimetracker/Timer.kt#L175) adds elapsed time to a name-addressed aggregate | No per-session evidence or recorded user ownership for this write |
| User profile | [Registration.kt](../../app/src/main/java/com/example/acetimetracker/Registration.kt#L49) creates Auth only; [profile editing](../../app/src/main/java/com/example/acetimetracker/EditProfileActivity.kt#L69) can access an uninitialized photo URL | Bootstrap a profile idempotently and test text-only editing |

Other confirmed defects: repeated timer Stop/Save/Reset can recount the full elapsed time; per-entry integer-hour conversion drops sub-hour progress; manual duration ignores calendar dates; upload/save ordering can lose image association; theme is not restored at app startup; notification preference has no implementation. The generated tests cover arithmetic and package identity only.

## Non-destructive migration requirements

- Inventory an allowlisted pilot in staging before live migration. No live data, deployed rules, production bucket ownership or signing setup was verified by this audit.
- Import into one per-user goals/session contract; preserve the original source and a deterministic provenance mapping.
- Imported categories require routine setup before scheduled completion or reminders, because the old source supplies no cadence, routine days or historical time zone.
- Keep summary-only records explicitly incomplete. Do not manufacture endpoints, historical time zones, descriptions, image ownership or individual sessions from aggregates.
- Preserve unowned global `Category.totalTime` untouched. Similar names do not establish user ownership.
- Repeated imports must not duplicate records. Preserve multiplicity actually present in a source; do not merge records across sources just because date/category/duration match.
- Keep malformed or ambiguous history visible in a review report and separate from verified totals.
- Reconcile late queued writes from older offline clients. A successful snapshot/import does not prove that every older device has synchronized.
- Do not delete original data or deploy live rules as part of documentation. Stage code, rules, indexes, dry-run evidence and a rollback/recovery plan before cutover.

Firestore's documented [offline cache](https://firebase.google.com/docs/firestore/manage-data/enable-offline), [array operations](https://firebase.google.com/docs/firestore/manage-data/add-data#update_elements_in_an_array), [transaction behavior](https://firebase.google.com/docs/firestore/manage-data/transactions), [ownership rules](https://firebase.google.com/docs/firestore/security/rules-conditions) and [indexes](https://firebase.google.com/docs/firestore/query-data/indexing) constrain the migration. Server-side import code must use its own explicit user/write limits because administrative access bypasses normal client rules.

## Android foundation evidence

Use a current compatible release set, with exact pins and build evidence in P1. The integrated spec proposes AGP 9.3.3, Gradle 9.5.0, Kotlin/Compose compiler 2.4.21, JBR 21, compile SDK 37, initial target SDK 36 and minimum SDK 27. Its compatibility qualification is explicit: Kotlin's published tested AGP upper bound is 9.3.1; the newer patch choice must be demonstrated by a real build. [AGP 9.3](https://developer.android.com/build/releases/agp-9-3-0-release-notes), [Kotlin](https://kotlinlang.org/docs/gradle-configure-project.html).

Current Android dependencies cannot be upgraded independently of SDK/AGP metadata. The [Compose BOM 2026.09.00](https://dl.google.com/dl/android/maven2/androidx/compose/compose-bom/2026.09.00/compose-bom-2026.09.00.pom) and [stable Navigation 3](https://developer.android.com/jetpack/androidx/releases/navigation3) are proposed for the new UI. Use [built-in Kotlin migration](https://developer.android.com/build/migrate-to-built-in-kotlin) and [Firebase main-module Kotlin APIs](https://firebase.google.com/docs/android/kotlin-migration), rather than carrying obsolete configuration forward.

Compose migration is incremental and screen-based. Repositories and local persistence isolate UI from Firebase and make timer/progress correctness testable. [Compose migration](https://developer.android.com/develop/ui/compose/migrate/strategy), [architecture recommendations](https://developer.android.com/topic/architecture/recommendations), [offline-first data layer](https://developer.android.com/topic/architecture/data-layer/offline-first).

## Research chats

- Competitor/engagement research: `01a15363-d945-78c1-82ca-eff30cfed112`.
- Data/migration audit: `01a15363-dcba-7311-bbb0-f29566dbb9c3`.

Both chats completed read-only research. Their findings were reconciled into the broader personal-goals scope in the integrated specification. Android implementation and iOS work have not started.
