# Architecture

AceTimeTracker is a single-module native Android app. Compose screens receive state and callbacks from activity/ViewModel pairs. MainActivity owns Today, Timer, Timesheet and Progress tabs, retaining one TimerViewModel. A SaveableStateHolder preserves tab UI state; secondary settings/profile/category activities remain separate. Legacy timer/history/progress activity entry points redirect to the main host.

## State and data

- `AuthRepository` owns Firebase authentication and the debuggable-build-only sample session.
- `CategoryRepository` reads/writes `users/{uid}/categories/{categoryId}`. New category creation no longer updates the obsolete global `Category` collection.
- `TimesheetRepository` stores entries in `timesheetEntries/{entryId}`, queried by `userId`. Server security rules must enforce ownership; client filtering is not authorization.
- `UserRepository` owns profile and profile-photo operations.
- `TimesheetStats` derives display totals from saved entries instead of maintaining separate totals.
- `SettingsRepository` persists theme and timer notification preferences.

Repositories currently expose callback-based `Resource` results. ViewModels translate them into observable screen state. DurableStore uses SQLite transactions for local records and an outbox on a single IO worker. Successful mutations mean committed locally, not server acknowledgment. OfflineData serializes writes with stable IDs, revision-checked acknowledgments, persistent deletion tombstones and server snapshot watermarks. Reads are account-scoped and delayed callbacks recheck the session. SyncStatus exposes pending/error/local-only states; collection-specific read errors cannot be hidden by an unrelated successful request. Retries run in-process and on authenticated relaunch. This is not a cross-device conflict UI or a guarantee of background work after process death.

## Timer lifecycle

`TimerViewModel` uses elapsed realtime anchors, so delayed UI ticks do not accumulate clock drift. Configuration changes retain the ViewModel. Stopwatch time and Pomodoro focus time are distinct; breaks are excluded from saved focus duration.

A per-account local draft checkpoints the timer approximately once per displayed second and at state transitions. Changing primary tabs keeps the same timer running. Leaving the host permanently pauses and stores the draft. Reopening or recovering after process death restores it paused at the last checkpoint. This deliberately does not count unknown time after process termination. Preferences are asynchronous, so an abrupt kill may lose the most recent checkpoint.

The foreground service displays the timer notification while the ViewModel remains active. It is not a separate durable timer engine. Force-stop, reboot continuation, exact alarms, and background completion alerts are not supported guarantees.

Controls cannot mutate timer state during an outstanding save. Users can leave after a pending-save explanation, so an offline request cannot trap navigation. Each timer mode persists a stable entry ID; retrying an interrupted save updates that document instead of adding a duplicate. Before submitting a save, pending local checkpoint writes are flushed on an IO dispatcher. A writer token prevents callbacks from an older ViewModel from overwriting a newer draft. Successful saves reset the relevant timer and allocate a new entry ID; failures retain accumulated time. The owner identity is captured when the timer is created and checked before saving. Android backup/transfer includes appearance settings only, excluding timer drafts, account sessions, and cached media.

## Demo boundary

Debug test sign-in uses a fixed sample identity. Sample categories and entries are seeded once into the same durable local store; they never enter the remote outbox. The sample profile remains process-local. Release authentication ignores stored demo sessions and hides quick test sign-in. Debug and release have different application IDs and Firebase client configuration.

## Known boundaries

- Sessions retain category names rather than canonical category IDs. Renaming a category preserves historical labels. Editing a session retains its ID and photo, and preserves active duration when the time range is unchanged.
- Local date/time strings do not preserve a recording time zone. Historical timezone changes and ambiguous daylight-saving times need a future schema migration.
- Firebase rules, indexes, and deployed backend state have not been verified by local JVM tests.
- Account deletion must cover Auth, profile, categories, timesheets, and media together; deleting Auth alone is not a complete implementation.
- The historical goals/routines migration remains a roadmap. Current reminders are opt-in inexact AlarmManager alarms, rescheduled on reboot/time changes. The widget and notification actions use immutable intents into the authenticated host.
- ReportCalculator performs date-range clipping and previous-period comparisons off the UI thread; PDF/CSV files are generated on IO and shared with scoped FileProvider grants.
- Photo attachment still requires a successful online upload for real accounts; media is not part of the durable outbox.
