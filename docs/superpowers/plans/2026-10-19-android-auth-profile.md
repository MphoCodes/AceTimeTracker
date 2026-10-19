# P2: authentication, profiles, and session routing

Build on merged P1. Keep the current Views and Firebase paths. iOS, historical
migration, rules deployment, and production Firebase operations remain out of scope.

- [x] Add account/profile gateways and a small application dependency container.
  Initialize only missing profile fields in a transaction; preserve all existing
  fields, arrays, category documents, and image references. Cached existing
  profiles may open offline; incomplete setup exposes retry without recreating Auth.
- [x] Retain asynchronous auth/profile work in ViewModels, gate duplicate actions,
  and reject stale results after account changes. Do not persist passwords.
- [x] Route startup and successful authentication to the correct destination;
  clear the task on sign-out and guard account screens, including restored state.
- [x] Fix text-only profile edits and retain existing images. Save only intentional
  profile changes; sign-in email is read-only and sourced from Auth. Keep failed
  saves editable and expose upload/download failures without blocking Back.
- [x] Test preservation, failures/retries, stale callbacks, routing/back-stack
  flags, text-only saving and rotation with fake gateways and real UI components.
- [ ] Run full metadata/build/test/lint checks, obtain review, open a separate P2
  PR, and verify hosted CI. Do not merge P2 without a later request.

Validation uses demo configuration and in-memory gateway fixtures, never live
accounts/data. Firebase adapter integration and deployed upload rules remain
explicit staging/device checks before production rollout.

Local validation: metadata check, debug APK, instrumentation APK compilation,
39 JVM/Robolectric tests, and lint passed. Lint has zero errors and 192 warnings.
Independent review found a destroyed-Activity callback and a delayed-listener
resume edge case; both now have failing-before/passing-after regression tests,
and follow-up review found no remaining actionable issues. Hosted CI is pending.
