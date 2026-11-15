# Ace brand and usability implementation plan

**Goal:** Deliver the user-approved ivory/charcoal/crimson direction, A/clock identity, clearer navigation and actionable empty states.
**Architecture:** Keep repositories and timer ownership unchanged. Use shared color tokens and vector assets; route explicit add-entry actions separately from History.
**Tech Stack:** Kotlin, Compose Material 3, Android vector/adaptive icons.
**Spec:** User-approved proposal in this conversation: Today / Timer / History / Reports, prominent Start session, simple A/clock logo and brief splash.

- [x] Replace brand assets and splash treatment, including adaptive launcher and notification silhouette. Keep launch delay at most 700ms.
- [x] Apply warm neutral/crimson tokens; simplify drawer to secondary destinations.
- [x] Wire Add time to the entry form, History to saved entries, and return to history after save. Preserve draft/filter state.
- [x] Put an actionable report empty state before goals; hide empty goal/timeline noise.
- [x] Build/test, inspect device light/dark layouts, update handoff and push.

Review focus: small/large-font labels; edit vs add navigation; save completion; dark contrast; launcher safe zone; notification monochrome visibility.

Validation: 90 JVM tests, 3 device tests, zero lint errors (279 warnings), release APK/AAB built. Light/dark splash and narrow large-text dock visually rendered; device routes and edit/save return verified. README screenshots refreshed.
