# Architecture

AceTimeTracker is a single-module native Android app. Compose screens receive state and callbacks from activity/ViewModel pairs. Navigation remains activity-based; there is no Navigation 3 migration in this release.

## State and data

- `AuthRepository` owns Firebase authentication and the debuggable-build-only sample session.
- `CategoryRepository` reads/writes `users/{uid}/categories/{categoryId}`. New category creation no longer updates the obsolete global `Category` collection.
- `TimesheetRepository` stores entries in `timesheetEntries/{entryId}`, queried by `userId`. Server security rules must enforce ownership; client filtering is not authorization.
- `UserRepository` owns profile and profile-photo operations.
- `TimesheetStats` derives display totals from saved entries instead of maintaining separate totals.
- `SettingsRepository` persists theme and timer notification preferences.

Repositories currently expose callback-based `Resource` results. ViewModels translate them into observable screen state. This repository does not yet have Room, a durable outbox, or a complete offline conflict-resolution protocol.

## Timer lifecycle

`TimerViewModel` uses elapsed realtime anchors, so delayed UI ticks do not accumulate clock drift. Configuration changes retain the ViewModel. Stopwatch time and Pomodoro focus time are distinct; breaks are excluded from saved focus duration.

A per-account local draft checkpoints the timer approximately once per displayed second and at state transitions. Leaving the screen pauses and stores the draft. Reopening or recovering after process death restores it paused at the last checkpoint. This deliberately does not count unknown time after process termination. Preferences are asynchronous, so an abrupt kill may lose the most recent checkpoint.

The foreground service displays the timer notification while the ViewModel remains active. It is not a separate durable timer engine. Force-stop, reboot continuation, exact alarms, and background completion alerts are not supported guarantees.

Controls cannot mutate timer state during an outstanding save. Users can leave after a pending-save explanation, so an offline request cannot trap navigation. Each timer mode persists a stable entry ID; retrying an interrupted save updates that document instead of adding a duplicate. Before submitting a save, pending local checkpoint writes are flushed on an IO dispatcher. A writer token prevents callbacks from an older ViewModel from overwriting a newer draft. Successful saves reset the relevant timer and allocate a new entry ID; failures retain accumulated time. The owner identity is captured when the timer is created and checked before saving. Android backup/transfer includes appearance settings only, excluding timer drafts, account sessions, and cached media.

## Demo boundary

Debug test sign-in uses a fixed sample identity. Sample repositories keep data in process memory. Release authentication ignores stored demo sessions and hides quick test sign-in. Debug and release have different application IDs and Firebase client configuration.

## Known boundaries

- Sessions retain a category name rather than a canonical category ID. Category renaming and history editing are not implemented.
- Local date/time strings do not preserve a recording time zone. Historical timezone changes and ambiguous daylight-saving times need a future schema migration.
- Firebase rules, indexes, and deployed backend state have not been verified by local JVM tests.
- Account deletion must cover Auth, profile, categories, timesheets, and media together; deleting Auth alone is not a complete implementation.
- The historical modernization design is a roadmap. Its goals/session migration, offline synchronization, and reminders are not silently included in the current release.
