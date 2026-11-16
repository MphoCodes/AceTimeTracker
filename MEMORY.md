# Project memory

- Canonical project: `MphoCodes/AceTimeTracker`; origin: `https://github.com/MphoCodes/AceTimeTracker.git`.
- Current product is a native Compose time tracker with categories, stopwatch/Pomodoro, manual entries, history, progress, and profile/settings screens.
- Debug demo authentication is gated by the application's debuggable flag. Sample categories/entries persist locally; profile sample remains process-local. Entry/category outbox sync retries while process lives and on authenticated relaunch; media uploads remain online.
- Timer drafts are local, per-account, restored paused. Stable per-mode entry IDs make interrupted saves retryable without adding another entry. Writer ownership prevents old callbacks replacing newer drafts.
- Public documentation must distinguish implemented entry editing, offline outbox, persistent tabs, reports, timeline, widgets/reminders from the still-future goals/routines roadmap.
- Preserve existing source and IDE changes; do not change the permanent package ID or deploy to the configured Firebase project without verified ownership and explicit scope.
- See HANDOFF.md for current validation, artifacts, and remaining release requirements.

- Correction from owner: preserve Ace’s bright red/crisp neutral theme and target-and-arrow identity. The ivory/muted crimson and generic A-ring redesign was rejected. Do not interpret usability improvements as permission to replace the brand identity. Primary labels are Today, Timer, History and Reports; Add time is an explicit form route. Large-font navigation uses Log/Stats with full accessibility descriptions.

- November 16 Claude-to-Codex handoff: current stack is PRs #6–#10, branch `docs/demo-video`; each PR targets the previous branch, starting at main. See HANDOFF.md Start here for verified provenance, remaining device/Firebase/signing work and continuation instructions. New namespace/application ID is `com.mphocodes.acetimetracker`; Firebase client registration is unverified. Nine Activities remain. Earlier verification and artifact hashes are historical.

- November 16 takeover review: timezone reports now use elapsed instants; timeline gap linkage includes entry identity; CSV/PDF include zone context. 103 JVM + 3 real device tests pass. Owner has no production signing key or owned Firebase setup; production remains blocked. New OpenFilm showcase uses local neural narration, actual captures and Mpho Ndlela · MphoCodes credit. Read latest HANDOFF section and live GitHub state, not older branch snapshots.

- Final publication: PRs #6–#10 merged; local checkout returned to main at integration a328132. Public development pre-release v1.0.0-dev.20261116.2 targets verified source d3f73e2, with video, full showcase package, debug-signed APK and checksums. Hosted run 43860599437 passed. Production remains blocked as above.
