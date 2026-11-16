# Handoff — AceTimeTracker

Last consolidated: **2026-11-17**. Verified against `origin/main` at `568f23b` (clean tree apart from four personal `.idea` edits). Older layered notes are preserved verbatim in [docs/history/HANDOFF-2026-11-16-archive.md](docs/history/HANDOFF-2026-11-16-archive.md); where they conflict with this file, this file wins.

Memory scope: `MphoCodes/AceTimeTracker` (origin-derived). Memory and this file are context, not authorization — see "Rules of engagement".

## 1. Start here

Read in this order, then re-check live state before acting (`git fetch`, `git status`, `gh pr list`, `gh run list --branch main`):

1. `AGENTS.md`, `MEMORY.md`, this file.
2. `docs/play-release-readiness.md` — the authoritative list of release blockers and owner actions.
3. `docs/brand/BRIEF.md` — settled identity. Do not re-propose a palette or mark.
4. `docs/development.md`, `docs/device-checks.md`, `backend/README.md`, `site/README.md`, `docs/demo/README.md` as the task requires.

**One-paragraph state:** the app is a native Kotlin/Compose time tracker with offline-first storage, timer, reports and zone-aware entries. It builds, passes JVM/device/backend tests, and is **signed** with an upload key against an **owned Firebase project**. A **sample-data debug preview** is public (website + GitHub pre-release). It is **not production-ready**: Firebase billing is off (no Storage bucket, no deployed deletion functions), real-account/photo/deletion flows have never run end to end on a device, and Play Console work has not started.

## 2. What is done

| Area | State | Evidence / where |
| --- | --- | --- |
| Daily workflows (entry/category edit, offline outbox, shared timer host, reminders/widget, reports/CSV/PDF, daily timeline) | Implemented | Archive sections "Delivered workflows"; 106 JVM + 3 device tests |
| Single-activity cleanup | Done (PR #6) | Redirect/dead Activities, RecyclerView adapters, XML layouts, `viewBinding` removed. Nine Activities remain: Main plus Splash, Login, Registration, Categories, Profile, EditProfile, Settings, About |
| Time zones | Done (PR #7 + takeover fixes) | `TimesheetEntry.zoneId`; elapsed-instant report math, DST-safe totals/clipping/overlap/gap linkage; zone context in CSV/PDF/timeline |
| Package identity | `com.mphocodes.acetimetracker` (+ `.debug`) | PR #8; old installs do not upgrade in place |
| Brand | Red target + blue dart, slate neutrals | `docs/brand/BRIEF.md`. The ivory/"A-ring" direction was **rejected by the owner** |
| Firebase | Owned project `ace-time-tracker-3d7d3` (Auth email/password, Firestore `africa-south1`); new prod/debug/web clients registered; rules deployed | Disposable live checks passed (register/login, own CRUD, cross-account/anonymous denial, owner immutability, deletion fence) and were cleaned up. **Not** the old `acetimetracker-a3fb0` |
| Account deletion | Client + backend implemented, **gated off** | Password-reauth, UID binding, durable request + retry sweep, local wipe, private-image cache removal. Backend callable gate defaults `false`; nothing deployed to Functions/Storage |
| Signing | Upload key generated outside Git; signed APK/AAB verified | `scripts/build-signed-release.py`; cert SHA-256 `421061d5…dddda8` registered with the production client. **Only one local copy of the key exists** |
| Public preview | Live | https://acetimetracker.vercel.app (Vercel project `prj_m1xvU6lDs68oR4rtfAMNSLIsZ78J`, root `site`), pre-release `v1.0.0-dev.20261116.2` (sample-data **debug-signed** APK), checksum-pinned |
| Showcase film | 49.8 s, 1080p30, local Kokoro voice, captions, OpenFilm source | `docs/demo/` (see its README). Written credit "Mpho Ndlela · MphoCodes"; no spoken name |
| Guided-tour film | 62.7 s, 1080p30, **real emulator footage**, step checklist + tap rings, local Kokoro voice, CC0 music, SRT | `docs/demo/acetimetracker-guided-tour.mp4`, rebuild via `docs/demo/guided-tour/README.md`. Proposed as the README hero; **not yet on the public site** (`site/build.mjs` still copies the older film and `download.json` pins its checksum) |
| Contact / privacy | Published | `hello@mphondlela.co.za`; privacy + external deletion-request pages live; online deletion portal gate is `false` |
| CI | Green on main | Run `43864716715` (Android + account-services jobs) |

## 3. What remains

### 3a. Blocked on the owner (agents cannot do these)

1. **Enable Firebase Blaze billing** on `ace-time-tracker-3d7d3` (and set budget alerts — alerts are not a cap). This unblocks Storage and Cloud Functions.
2. **Back up the signing folder** `~/Library/Application Support/AceTimeTracker/signing/` to an independent encrypted location and verify restore. The JSON holds passwords; never upload it anywhere public.
3. **Play Console** (existing personal account — do not create another): confirm verification status and creation date, register `com.mphocodes.acetimetracker`, enrol in Play App Signing, accept agreements. Accounts created after 13 Nov 2023 need ≥12 opted-in testers for 14 consecutive days before production access; website APK installs do not count.
4. Watch and listen to the guided tour (`docs/demo/acetimetracker-guided-tour.mp4`) and say what to change — pacing, voice, wording, order. I could not listen to it; levels were only measured (−16.1 LUFS, −1.5 dBFS peak).

### 3b. Engineering, in order, once billing is on

1. Provision the default Storage bucket (deliberate region + soft-delete/versioning), refresh **both** Android `google-services.json` files and the web SDK config.
2. Deploy Storage rules and the three deletion functions per `backend/README.md` with `ACCOUNT_DELETION_ENABLED=false`; add failure/age monitoring.
3. Run the real-account matrix on a device: sign-in, profile/category/entry sync, private photo upload/read, cross-account denial, password reset, process restart, interrupted-cleanup retry, lost-callable-response retry, and **full deletion across Storage, Firestore, Auth and local data**. Record results in `docs/device-checks.md`.
4. Only then flip the backend parameter and website portal gate; re-verify published contact route and retention wording.
5. Finish `docs/device-checks.md` (all nine rows were still **Unverified** at last check): photo restore on recreation, reboot/force-stop reminders, widget placement, API 26/30 notifications, TalkBack, tablet/landscape, timezone change mid-timer.
6. Record the **foreground-service demo** Play requires (user initiation → notification → background behaviour → stop). The marketing film is not that proof.
7. Play declarations: Data safety (working map in `docs/play-release-readiness.md`; do **not** tick "no data collected"), content rating, target audience, ads, reviewer access instructions, privacy + deletion URLs. Run pre-launch report, then the required test track, then submit.
8. Replace the public sample-data preview APK with the account-enabled build only after step 3 passes.

### 3c. Optional / future (not authorized yet)

- Fold Splash/Login/Registration/Categories/Profile/EditProfile/Settings/About into a Navigation 3 host (single-activity goal is half done).
- Goals/routines, billing/subscriptions, external calendar integration (roadmap items, unimplemented).
- Timer is checkpoint-restored **paused** after process loss; it is not a reboot-continuing engine.
- Sync retries while the process lives and on authenticated relaunch only; no WorkManager/background delivery after process death.
- Dev-tool `npm audit` shows 18 findings (5 high) in `backend/` dev dependencies; production dependencies: 0.

## 4. Verification evidence (what was actually run)

| Check | Result | When / who |
| --- | --- | --- |
| `:app:testDebugUnitTest` | **106 passed, 0 failed/skipped** | Re-run 2026-11-17 on `568f23b` (Gradle finished in ~8 s, so the task may have reused up-to-date results) |
| Device tests (`connectedDebugAndroidTest`) | 3 passed on API 36 `Ace_Demo_Review` | Takeover pass, 2026-11-16, before the production-prep commits. **Not re-run since.** Beware: a locked emulator once reported task success with **zero tests executed** — check the XML report count |
| Debug + release lint | Pass (release: 193 warnings, 0 errors) | 2026-11-16 |
| Signed APK/AAB | v2 signature + JAR signature verified, bundletool 1.18.3 validates, 16 KB alignment OK, installs/launches on API 36, release demo bypass absent | 2026-11-16 |
| Backend | 3 unit + 4 Firestore/Storage emulator tests pass | 2026-11-16 |
| Hosted CI | Success, run `43864716715` | 2026-11-16 |
| Live Firebase | Disposable-account rules checks passed | 2026-11-16 |

**Never verified:** full live account + photo + deletion flow on a device; widget placement; reboot/scheduled reminder delivery; TalkBack; tablets; foreground-service background behaviour on API 26–30; Storage/Functions behaviour (not deployed). Jarsigner prints ordinary self-signed/no-timestamp and ZIP-ordering warnings; bundletool validation is the meaningful check.

## 5. Rules of engagement

- **Authorization:** package-identity changes, live Firebase deploys/data migration, billing, and publication each need explicit owner scope. Memory is not authorization.
- **Secrets:** never commit or print keystores, signing JSON, API keys beyond the public Firebase client config, or raw transcripts.
- **Brand:** red `#DC2626` target + blue dart; primary nav Today / Timer / History / Reports; large-font nav shows Log/Stats with full accessibility labels.
- **Auth separation:** debug demo sign-in is gated on the debuggable flag; release must never expose it.
- **Local state:** four personal edits in `.idea/` (`appInsightsSettings.xml`, `compiler.xml`, `deploymentTargetSelector.xml`, `misc.xml`) are the owner's — preserve, never commit. `work/`, `.scratch/`, `backend/firestore-debug.log` are git-ignored.
- **Editor tabs** under `app/build/generated/.../com/example/...` are stale build output, not source.
- **ElevenLabs:** the owner's plan refuses video generation and the free music/TTS queue was repeatedly busy; the film is built locally (OpenFilm + Kokoro). Don't retry paid generation without being asked.
- **Source graph:** no ingestion tool was exposed; any indexed graph may lag source. Git and these files are authoritative.
- Run local tests before the full build; release verification = `lintRelease`, `assembleRelease`, `bundleRelease`. Successful compilation is not a signed artifact, a verified backend, or a published release.

## 6. Repo map

```
app/                 Android app (single module :app), source in app/src/main/java/com/mphocodes/acetimetracker
backend/             Firestore/Storage rules, Cloud Functions (account deletion), emulator tests, README
site/                Vercel static preview site (build.mjs, download.json, public/)
scripts/             build-signed-release.py (loads secrets without printing)
play-store/          Listing copy, artwork notes, signing.md (procedure only, no secrets)
docs/                architecture, development, release, play-release-readiness, device-checks,
                     brand/BRIEF.md, demo/ (film + build scripts), history/ (archived handoffs)
```

## 7. Hygiene items noticed (none acted on)

- `My Demo Vid.mp4` (57 MB) is tracked at the repo root since the initial commit and is not referenced by docs. Consider removing it from the working tree (history keeps the blob unless rewritten — a destructive step needing owner approval).
- Ten non-main remote branches remain: the five merged stack branches (`refactor/single-activity-host`, `fix/timezone-aware-entries`, `chore/rename-package-mphocodes`, `docs/brand-brief-and-device-checks`, `docs/demo-video`) and five older `codex/*` branches tied to closed PRs; local copies exist too. Safe to prune once the owner agrees.
- `docs/superpowers/` and `docs/research/` are historical planning material and contain old `com.example` references; they are intentionally not rewritten.

## 8. Session log (most recent first)

- **2026-11-17 — guided-tour film (Claude).** Owner was unhappy with the earlier film and asked for a far better, more intuitive one. Recorded the debug build on an API 36 emulator at 1080×2400 with a scripted tour, composed a five-step film (track, add time, history, reports) with real footage, tap rings and a checklist synced to the logged input, Kokoro narration and the CC0 bed. Recording exposed a real UX bug — the running-session strip pushed the Timer tab's **Save** button below the fold with its label clipped — fixed in its own PR (hides the strip on the Timer tab). PRs: #11 (Save fix, base `main`) and the film PR stacked on it. Not done: public-site swap, a Nav3/UX pass on History (the filter card fills most of the first screen and card actions sit above the category chip), and a recorded foreground-service demo for Play.

- **2026-11-17 — consolidation.** Re-verified live state, ran JVM tests, archived the layered handoff, rewrote this file.
- **2026-11-16 — production preparation, showcase, publication (Codex).** Reviewed and merged PRs #6–#10, fixed DST/zone report bugs, shipped pre-release `v1.0.0-dev.20261116.2`, deployed rules to the owned Firebase project, implemented account deletion, generated the upload key, published the Vercel preview and contact details.
- **2026-11-16 — structural fixes and first demo (Claude).** Opened the five stacked PRs (activity cleanup, zone-aware entries, package rename, brand brief + device checklist, local demo video). The first demo was later replaced by the OpenFilm showcase. The Claude session hit its usage limit before the final documentation pass, which is what this consolidation completes.
- **2026-11-16 earlier — brand and usability pass.** A warm ivory/A-ring identity was built then rejected by the owner; original red/slate palette and target-and-dart mark restored.
- Before that: the six daily-workflow improvements (see archive).
