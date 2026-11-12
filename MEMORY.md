# Project memory

- Canonical project: `MphoCodes/AceTimeTracker`; origin: `https://github.com/MphoCodes/AceTimeTracker.git`.
- Current product is a native Compose time tracker with categories, stopwatch/Pomodoro, manual entries, history, progress, and profile/settings screens.
- Debug demo authentication is gated by the application's debuggable flag. Sample repository data is process-local; do not describe it as durable offline synchronization.
- Timer drafts are local, per-account, restored paused. Stable per-mode entry IDs make interrupted saves retryable without adding another entry. Writer ownership prevents old callbacks replacing newer drafts.
- Public documentation must distinguish current features from the larger historical goals/routines/offline/reminders roadmap.
- Preserve existing source and IDE changes; do not change the permanent package ID or deploy to the configured Firebase project without verified ownership and explicit scope.
- See HANDOFF.md for current validation, artifacts, and remaining release requirements.
