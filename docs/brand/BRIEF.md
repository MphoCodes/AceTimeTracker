# Ace brand brief

This is the settled visual identity for AceTimeTracker. It exists so that future
work does not re-litigate it: on 2026-11-16 a warm ivory/charcoal palette and an
"A-ring" mark were proposed, implemented and then rejected by the owner as a loss
of Ace identity. The tokens below are the ones that were restored and approved.

## Name and voice

- Product name: **AceTimeTracker** (one word in code and URLs, "Ace Time Tracker"
  when spoken). Short form: **Ace**.
- Tagline: *Track your work. Make time for what matters.*
- Tone: direct, calm, confident. Short sentences. No exclamation marks in UI copy.

## Mark

- A bright red archery target (concentric red/white rings) with a royal-blue dart
  landing in the bullseye. Source of truth: [`ace-mark.svg`](ace-mark.svg).
- Derived assets: adaptive launcher (`res/drawable/ic_launcher_foreground.xml`),
  monochrome notification silhouette, Android 12+ system splash, README header.
- Do not replace the target/dart with lettermarks, clocks or abstract rings.

## Colour

Tokens live in `ui/theme/Color.kt` and must stay in sync with this table.

| Role | Token | Hex |
| --- | --- | --- |
| Primary / brand red | `AceRed` | `#DC2626` |
| Pressed / dark red | `AceRedDark` | `#991B1B` |
| Red tint surfaces | `AceRedLight`, `AceRedSubtle` | `#FEE2E2`, `#FEF2F2` |
| Dart / links / info | `AccentBlue` | `#2563EB` |
| Text and dark surfaces | `Slate900` … `Slate600` | `#0F172A` … `#475569` |
| Borders, dividers, chips | `Slate300` … `Slate100` | `#CBD5E1` … `#F1F5F9` |
| Page background (light) | `Slate50` | `#F8FAFC` |
| Success / warning | `AccentEmerald`, `AccentAmber` | `#10B981`, `#F59E0B` |

Rules: red is for primary actions, the dashboard header, active tab and progress.
Blue is reserved for the dart, links and informational states. Never use red for
error text on a red surface; use white text or the amber/emerald accents.

## Type and shape

- System font (Roboto on device). Headline weight 700, body 400, labels 500.
- Corner radius: 28 dp for cards and the bottom dock, 16 dp for buttons/chips,
  full-round for the timer ring and avatar.
- Elevation is light: 1–2 dp cards, 6 dp for the floating dock only.

## Layout and navigation

- Primary navigation is the floating bottom dock: **Today / Timer / History / Reports**.
- The drawer holds only secondary routes (Profile, Categories, Settings, About,
  Log out) and must not duplicate the dock.
- Empty states always offer an action (e.g. *Start session* / *Choose another period*).

## Motion

- Splash reveal ≤ 420 ms inside the existing 700 ms launch delay.
- Tab switches: fade/slide ≤ 200 ms. The running-session strip never animates
  on recomposition, only on appear/disappear.

## Demo and marketing assets

- Demo video: `docs/demo/acetimetracker-demo.mp4`, rebuilt with
  `python3 docs/demo/build_demo.py` (see that file for narration, scene order and
  how to add a music bed). Keep it under 60 s and keep the screenshots free of
  personal data.
- Screenshots: `docs/screenshots/*.png` from the debug demo account only.

## Changing this brief

Edit this file and `Color.kt` in the same PR, update the SVG if the mark changes,
and re-export the launcher/notification/splash assets. Note the decision and the
reason in `HANDOFF.md`.
