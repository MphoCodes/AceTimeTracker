# Project memory

- Canonical project: `MphoCodes/AceTimeTracker`; origin: `https://github.com/MphoCodes/AceTimeTracker.git`.
- Current product is a native Compose time tracker with categories, stopwatch/Pomodoro, manual entries, history, progress, and profile/settings screens.
- Debug demo authentication is gated by the application's debuggable flag. Sample categories/entries persist locally; profile sample remains process-local. Entry/category outbox sync retries while process lives and on authenticated relaunch; media uploads remain online.
- Timer drafts are local, per-account, restored paused. Stable per-mode entry IDs make interrupted saves retryable without adding another entry. Writer ownership prevents old callbacks replacing newer drafts.
- Public documentation must distinguish implemented entry editing, offline outbox, persistent tabs, reports, timeline, widgets/reminders from the still-future goals/routines roadmap.
- Preserve existing source and IDE changes; do not change the permanent package ID or deploy to the configured Firebase project without verified ownership and explicit scope.
- See HANDOFF.md for current validation, artifacts, and remaining release requirements.

- Correction from owner: preserve Ace’s bright red/crisp neutral theme and target-and-arrow identity. The ivory/muted crimson and generic A-ring redesign was rejected. Do not interpret usability improvements as permission to replace the brand identity. Primary labels are Today, Timer, History and Reports; Add time is an explicit form route. Large-font navigation uses Log/Stats with full accessibility descriptions.
