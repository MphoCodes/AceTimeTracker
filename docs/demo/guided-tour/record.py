#!/usr/bin/env python3
"""Drive AceTimeTracker on an emulator while recording the screen; writes take.mp4 and events.json.

Prerequisites: an emulator at 1080x2400 (the tap coordinates below are for that layout), the *debug* build
installed (it has the "Explore the demo" sample data), adb on PATH.

    python3 record.py /tmp/take            # then:  ffmpeg -i take.mp4 -vf fps=30 -an -crf 14 take30.mp4

The script resets app data, puts SystemUI into demo mode (clean 8:41 clock, full battery, no notifications),
performs a fixed tour while `screenrecord` runs, and logs every tap/swipe time so build.py can draw rings
that match the footage. Marker names (s1..s5) are informational; build.py takes its cut points from SCENES.
"""
import json, subprocess, sys, time, os
import os as _os
D = _os.environ.get("ANDROID_SERIAL", "emulator-5556"); P = "com.mphocodes.acetimetracker.debug"
OUT = sys.argv[1] if len(sys.argv) > 1 else "/tmp/ace-rec/take"
os.makedirs(OUT, exist_ok=True)

def adb(*a, **k): return subprocess.run(["adb", "-s", D, *a], capture_output=True, **k)
ev, marks = [], {}
t0 = None
def now(): return time.time() - t0

def tap(x, y, wait=0.8):
    adb("shell", "input", "tap", str(x), str(y)); ev.append(dict(k="tap", t=round(now() - 0.06, 3), x=x, y=y)); time.sleep(wait)
def swipe(x1, y1, x2, y2, ms=600, wait=0.8):
    s = now(); adb("shell", "input", "swipe", *map(str, (x1, y1, x2, y2, ms)))
    ev.append(dict(k="swipe", t=round(now() - ms / 1000 - 0.05, 3), d=ms / 1000, x=x1, y=y1, x2=x2, y2=y2)); time.sleep(wait)
def text(s, wait=0.8): adb("shell", "input", "text", s.replace(" ", "%s")); time.sleep(wait)
def key(k, wait=0.7): adb("shell", "input", "keyevent", str(k)); time.sleep(wait)
def mark(n): marks[n] = round(now(), 3); print(n, marks[n], flush=True)

# Clean status bar (SystemUI demo mode) and fresh app state.
for c in ["settings put global sysui_demo_allowed 1",
          "am broadcast -a com.android.systemui.demo -e command enter",
          "am broadcast -a com.android.systemui.demo -e command clock -e hhmm 2041",
          "am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false",
          "am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4 -e fully true",
          "am broadcast -a com.android.systemui.demo -e command network -e mobile show -e datatype none -e level 4 -e fully true",
          "am broadcast -a com.android.systemui.demo -e command notifications -e visible false",
          f"pm clear {P}", f"pm grant {P} android.permission.POST_NOTIFICATIONS", "input keyevent 3"]:
    adb("shell", *c.split(" "));
time.sleep(1.0)

adb("shell", "rm", "-f", "/sdcard/take.mp4")
rec = subprocess.Popen(["adb", "-s", D, "shell", "screenrecord", "--bit-rate", "20000000", "--size", "1080x2400", "--time-limit", "170", "/sdcard/take.mp4"])
t0 = time.time(); time.sleep(0.9)
adb("shell", "am", "start", "-n", f"{P}/com.mphocodes.acetimetracker.ui.activity.SplashActivity"); ev.append(dict(k="launch", t=round(now(), 3)))
time.sleep(3.4)

# --- Scene 1: Today
mark("s1")
tap(347, 1837, 2.6)                       # Explore the demo
swipe(540, 1750, 540, 1050, 900, 1.3)     # scroll to goals
swipe(540, 1100, 540, 1800, 700, 0.8)     # back up
mark("s1_end")

# --- Scene 2: Timer
mark("s2")
tap(345, 864, 1.7)                        # Start session
tap(154, 640, 0.9)                        # Design
tap(540, 1617, 6.4)                       # play, let it run
tap(540, 1617, 1.0)                       # pause
tap(540, 1854, 2.4)                       # save
mark("s2_end")

# --- Scene 3: Add time
mark("s3")
tap(173, 2189, 1.4)                       # Today tab
tap(764, 864, 1.9)                        # Add time
tap(154, 951, 0.8)                        # Design
tap(795, 1217, 1.2)                       # start time
tap(275, 1301, 1.1)                       # 9
tap(540, 1036, 0.9)                       # :00
tap(796, 1837, 0.9)                       # set
tap(795, 1429, 1.2)                       # end time
tap(407, 1071, 1.1)                       # 11
tap(540, 1566, 0.9)                       # :30
tap(796, 1837, 1.0)                       # set
tap(540, 1784, 0.7)                       # notes
text("Landing page mockups", 0.9)
key(4, 0.8)
swipe(540, 1700, 540, 900, 600, 0.9)
tap(540, 1780, 2.2)                       # save
mark("s3_end")

# --- Scene 4: History
mark("s4")
time.sleep(1.2)
tap(540, 568, 0.8)                        # search
text("Design", 1.1)
key(4, 0.9)
swipe(540, 1600, 540, 800, 800, 1.2)
swipe(540, 900, 540, 1700, 700, 0.8)
mark("s4_end")

# --- Scene 5: Reports
mark("s5")
tap(905, 2189, 1.9)
swipe(540, 1750, 540, 1000, 900, 1.3)
swipe(540, 1100, 540, 1800, 700, 0.9)
tap(449, 405, 2.0)                        # This month
tap(167, 405, 1.6)                        # This week
mark("s5_end")
time.sleep(0.8)

adb("shell", "pkill", "-2", "screenrecord"); rec.wait(timeout=30); time.sleep(1.0)
subprocess.run(["adb", "-s", D, "pull", "/sdcard/take.mp4", f"{OUT}/take.mp4"], capture_output=True)
adb("shell", "am", "broadcast", "-a", "com.android.systemui.demo", "-e", "command", "exit")
json.dump(dict(events=ev, marks=marks), open(f"{OUT}/events.json", "w"), indent=1)
print("done", marks)
