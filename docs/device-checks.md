# Device verification checklist

The surfaces below are implemented but have not been verified on hardware, or were
only partly verified. Each item records what to do, what "pass" looks like, and
the date/device of the last run. Update the table when you run one; do not mark a
release-ready claim in `HANDOFF.md` while any row is **Unverified**.

| # | Surface | Status | Last run |
| --- | --- | --- | --- |
| 1 | Photo restoration after process recreation | **Unverified** | — |
| 2 | Reminder delivery after device reboot | **Unverified** | — |
| 3 | Reminder delivery while app is force-stopped | **Unverified** | — |
| 4 | Home-screen widget placement and tap | **Unverified** | — |
| 5 | Live Firebase auth + Firestore ownership rules | **Unverified** | — |
| 6 | Timer notification pause/resume on API 26–30 | **Unverified** | — |
| 7 | TalkBack pass over the four primary tabs | **Unverified** | — |
| 8 | Tablet / landscape layout | **Unverified** | — |
| 9 | Timezone change while a timer is running | **Unverified** | — |

## Procedures

### 1. Photo restoration after process recreation
1. Debug build, demo sign-in. History → edit an entry → attach a photo from the gallery.
2. Before saving, background the app and run
   `adb shell am kill com.mphocodes.acetimetracker.debug` (use the old ID on branches before PR #8).
3. Reopen from recents. **Pass:** the edit form is restored with the same photo preview and the
   pending upload still queued; saving produces one entry with that image.

### 2. Reminder delivery after device reboot
1. Settings → Reminders → enable, pick a time 3 minutes ahead, grant notifications.
2. `adb reboot`, unlock, do **not** open the app.
3. **Pass:** notification appears within the inexact-alarm window (≤ 10 min on API 31+).
   Check `adb shell dumpsys alarm | grep acetimetracker` shows the rescheduled alarm.

### 3. Reminder delivery while force-stopped
Same setup as #2, then `adb shell am force-stop <id>`. Android cancels alarms for
force-stopped apps; **expected:** no notification until next launch. Document the
behaviour in Settings copy if it surprises testers.

### 4. Widget placement
Long-press home → Widgets → AceTimeTracker. **Pass:** preview renders, placement
succeeds on a 2×1 cell, tapping opens the Timer tab while authenticated and the
Login screen when signed out.

### 5. Live Firebase
Requires the owner's real `google-services.json` (see PR #8). Register, sign in,
create an entry, sign in as a second user. **Pass:** second user cannot read the
first user's entries (check Firestore rules simulator), uploads land under
`users/<uid>/`.

### 6. Timer notification on older APIs
Emulator API 26 and API 30. Start timer, pause/resume from the notification.
**Pass:** actions work, no `ForegroundServiceStartNotAllowedException`, the strip
in the app reflects the state within 1 s.

### 7. TalkBack
Enable TalkBack, traverse Today/Timer/History/Reports. **Pass:** every control has
a spoken label, the timer announces state changes, no unlabeled image buttons.

### 8. Tablet / landscape
Pixel Tablet AVD and phone landscape. **Pass:** no clipped dock, forms scroll,
report charts stay legible.

### 9. Timezone change during a running timer
Start timer, `adb shell service call alarm 3 s16 Asia/Tokyo` (or change zone in
Settings), wait a minute, save. **Pass:** `timeSpent` equals real elapsed time and the
entry's `zoneId` is the zone at *start*.
