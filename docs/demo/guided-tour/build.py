#!/usr/bin/env python3
"""Compose the AceTimeTracker guided-tour film from a real emulator recording.

Inputs (made by record.py and narrate.py):
  --take   directory holding take30.mp4 (constant-30fps screen recording) and events.json
  --voice  directory holding s0..s6.wav and narration.json
  --music  optional music bed (the CC0 track under docs/demo/film/assets works well)

    python3 build.py --take /tmp/take --voice /tmp/voice --music ../film/assets/minimal-product-launch-pads-c52fb2.mp3

Every pixel inside the phone is real app footage. The tap rings and step checklist are drawn from the
logged input events, so they stay in sync with what the viewer sees. Needs Pillow, numpy, imageio-ffmpeg.
"""
from __future__ import annotations

import argparse, json, math, subprocess, sys, tempfile
from pathlib import Path

import numpy as np
from PIL import Image, ImageChops, ImageDraw, ImageFilter, ImageFont

try:
    import imageio_ffmpeg
except ImportError:  # pragma: no cover
    sys.exit("pip install imageio-ffmpeg pillow numpy")

FF = imageio_ffmpeg.get_ffmpeg_exe()
W, H = 1920, 1080
FPS = 30
HERE = Path(__file__).resolve().parent
REPO = HERE.parents[2]

# Brand tokens (docs/brand/BRIEF.md).
RED, RED_DARK, RED_LIGHT = (220, 38, 38), (153, 27, 27), (254, 226, 226)
BLUE = (37, 99, 235)
S900, S700, S600, S400, S200, S100, S50 = (15, 23, 42), (51, 65, 85), (71, 85, 105), (148, 163, 184), (226, 232, 240), (241, 245, 249), (248, 250, 252)
WHITE = (255, 255, 255)
GREEN = (16, 185, 129)

FONT = "/System/Library/Fonts/HelveticaNeue.ttc"
def font(size, bold=False):
    try: return ImageFont.truetype(FONT, size, index=1 if bold else 0)
    except OSError: return ImageFont.load_default()

# Phone geometry (screen is 1080x2400 scaled by 0.398).
SCR_W, SCR_H = 430, 956
BEZEL = 16
PHONE_CX, PHONE_CY = 1460, 540
SRC_W, SRC_H = 720, 1600          # decoded footage size
VOICE_LEAD = 0.5                  # narration starts this long after a scene begins
TAIL = 0.9                        # breathing room after narration

def ease_out(t): t = min(1, max(0, t)); return 1 - (1 - t) ** 3
def ease_io(t): t = min(1, max(0, t)); return t * t * (3 - 2 * t)
def clamp(x, a=0.0, b=1.0): return max(a, min(b, x))


def draw_mark(size):
    """The target-and-dart mark, redrawn from docs/brand/ace-mark.svg (96-unit viewBox) with supersampling."""
    ss = 4; px = size * ss; k = px / 96.0
    img = Image.new("RGBA", (px, px), (0, 0, 0, 0)); d = ImageDraw.Draw(img)
    def disc(cx, cy, r, fill): d.ellipse(((cx - r) * k, (cy - r) * k, (cx + r) * k, (cy + r) * k), fill=fill)
    def ring(cx, cy, r, w, col):
        d.ellipse(((cx - r - w / 2) * k, (cy - r - w / 2) * k, (cx + r + w / 2) * k, (cy + r + w / 2) * k), fill=col)
        d.ellipse(((cx - r + w / 2) * k, (cy - r + w / 2) * k, (cx + r - w / 2) * k, (cy + r - w / 2) * k), fill=(0, 0, 0, 0))
    def line(x1, y1, x2, y2, w, col):
        d.line([(x1 * k, y1 * k), (x2 * k, y2 * k)], fill=col, width=int(w * k)); disc(x1, y1, w / 2, col); disc(x2, y2, w / 2, col)
    ring(43, 53, 30, 6, RED); ring(43, 53, 18, 6, RED); disc(43, 53, 6, RED)
    line(43, 53, 80, 16, 6, (51, 65, 85))
    d.polygon([(x * k, y * k) for x, y in [(64, 13), (78, 13), (78, 27), (64, 41), (64, 27), (50, 27)]], fill=BLUE)
    line(66, 27, 83, 10, 5, (51, 65, 85))
    return img.resize((size, size), Image.LANCZOS)

# ---------------------------------------------------------------------------------------------
# Storyboard. Source times (seconds into take30.mp4) come from record.py's markers/events.
# segs = (src_start, src_end, speed). steps = (label, trigger) where trigger is ("tap", i),
# ("swipe", i) – indexes within the scene's source window – or ("t", seconds after scene start).
# ---------------------------------------------------------------------------------------------
SCENES = [
    dict(id="s0", kind="title", voice="s0"),
    dict(id="s1", kind="app", voice="s1", title="Your day, at a glance",
         sub="Time tracked, your streak and weekly goals are there the moment you open Ace.",
         segs=[(3.35, 10.85, 1.0)],
         steps=[("Open the app — try the demo, no account", ("tap", 0)),
                ("Scroll to this week's goals", ("swipe", 0)),
                ("Recent sessions, one glance", ("swipe", 1))]),
    dict(id="s2", kind="app", voice="s2", title="Track a session",
         sub="Pick what you're working on and press play. Ace keeps counting while you move around the app.",
         segs=[(10.85, 14.0, 1.0), (14.0, 20.4, 2.2), (20.4, 23.8, 1.0)],
         zoom=[(13.9, 1.0, 540, 1200), (14.6, 1.05, 540, 1143), (20.2, 1.05, 540, 1143), (21.0, 1.0, 540, 1200)],
         steps=[("Tap Start session", ("tap", 0)),
                ("Choose a category", ("tap", 1)),
                ("Press play", ("tap", 2)),
                ("Pause when you stop", ("tap", 3)),
                ("Save — it lands in your history", ("tap", 4))]),
    dict(id="s3", kind="app", voice="s3", voice_lead=1.3, title="Missed one? Add it later",
         sub="Choose a category, set when you started and finished, and Ace adds up the time.",
         segs=[(23.8, 28.4, 1.0), (28.4, 37.5, 2.3), (37.5, 41.6, 1.6), (41.6, 44.77, 1.0)],
         steps=[("Open Add time", ("tap", 1)),
                ("Pick a category", ("tap", 2)),
                ("Set start and end", ("tap", 3)),
                ("Write a quick note", ("tap", 11)),
                ("Save — Ace totals it for you", ("tap", 12))]),
    dict(id="s4", kind="app", voice="s4", title="Find and fix anything",
         sub="Search by category or note, filter by period, then edit a session or track it again.",
         segs=[(44.77, 52.6, 1.0)],
         steps=[("Search by category or notes", ("tap", 0)),
                ("Scroll through your sessions", ("swipe", 0)),
                ("Edit, or track it again", ("swipe", 1))]),
    dict(id="s5", kind="app", voice="s5", title="See where the week went",
         sub="Compare with the last period, check goals against actuals, and share a PDF or CSV.",
         segs=[(52.6, 62.96, 1.0)],
         steps=[("Compare with the previous period", ("t", 1.6)),
                ("Goals versus actual time", ("swipe", 0)),
                ("A daily timeline with time zones", ("t", 4.2)),
                ("Switch week or month in a tap", ("tap", 1))]),
    dict(id="s6", kind="closing", voice="s6"),
]

# ---------------------------------------------------------------------------------------------
class Film:
    def __init__(self, a):
        self.a = a
        self.take = Path(a.take) / "take30.mp4"
        self.events = json.load(open(Path(a.take) / "events.json"))["events"]
        self.voice = Path(a.voice)
        self.meta = json.load(open(self.voice / "narration.json"))
        self.scale = 0.5 if a.preview else 1.0
        self.fps = 15 if a.preview else FPS
        self._build_static()
        self._plan()

    # ---- static art ---------------------------------------------------------------------
    def _build_static(self):
        bg = Image.new("RGB", (W, H), S50)
        glow = Image.new("RGB", (W, H), S50)
        gd = ImageDraw.Draw(glow)
        gd.ellipse((W - 1000, -420, W + 360, 760), fill=RED_LIGHT)
        gd.ellipse((-420, H - 520, 620, H + 380), fill=(224, 236, 255))
        glow = glow.filter(ImageFilter.GaussianBlur(190))
        self.bg = Image.blend(bg, glow, 0.85)
        # Phone body with shadow.
        pw, ph = SCR_W + 2 * BEZEL, SCR_H + 2 * BEZEL
        pad = 90
        layer = Image.new("RGBA", (pw + 2 * pad, ph + 2 * pad), (0, 0, 0, 0))
        sh = Image.new("RGBA", layer.size, (0, 0, 0, 0))
        ImageDraw.Draw(sh).rounded_rectangle((pad, pad + 34, pad + pw, pad + ph + 34), 84, fill=(15, 23, 42, 120))
        layer.alpha_composite(sh.filter(ImageFilter.GaussianBlur(38)))
        body = Image.new("RGBA", (pw * 2, ph * 2), (0, 0, 0, 0))
        bd = ImageDraw.Draw(body)
        bd.rounded_rectangle((0, 0, pw * 2 - 1, ph * 2 - 1), 76 * 2, fill=(11, 18, 32, 255))
        bd.rounded_rectangle((3, 3, pw * 2 - 4, ph * 2 - 4), 74 * 2, outline=(71, 85, 105, 255), width=3)
        layer.alpha_composite(body.resize((pw, ph), Image.LANCZOS), (pad, pad))
        self.phone_base, self.phone_pad = layer, pad
        m = Image.new("L", (SCR_W * 3, SCR_H * 3), 0)
        ImageDraw.Draw(m).rounded_rectangle((0, 0, SCR_W * 3 - 1, SCR_H * 3 - 1), 58 * 3, fill=255)
        self.scr_mask = m.resize((SCR_W, SCR_H), Image.LANCZOS)
        self.px0 = PHONE_CX - (pw // 2) - pad
        self.py0 = PHONE_CY - (ph // 2) - pad
        self.sx0 = self.px0 + pad + BEZEL                      # screen origin on the canvas
        self.sy0 = self.py0 + pad + BEZEL

    # ---- planning -----------------------------------------------------------------------
    def _plan(self):
        t = 0.0
        self.plan = []
        for sc in SCENES:
            v = self.meta[sc["voice"]]["seconds"]
            foot = sum((b - a) / sp for a, b, sp in sc.get("segs", []))
            lead = sc.get("voice_lead", VOICE_LEAD)
            dur = max(foot, v + lead + TAIL, 5.6)
            if sc["kind"] in ("title", "closing"): dur = max(dur, v + lead + 1.6)
            self.plan.append(dict(sc, start=t, dur=dur, foot=foot, vlen=v, vlead=lead))
            t += dur
        self.total = t
        # source -> output time maps, per scene
        for p in self.plan:
            p["win"] = (p["segs"][0][0], p["segs"][-1][1]) if p.get("segs") else None
            p["taps"] = [e for e in self.events if e["k"] == "tap" and p["win"] and p["win"][0] <= e["t"] < p["win"][1]]
            p["swipes"] = [e for e in self.events if e["k"] == "swipe" and p["win"] and p["win"][0] <= e["t"] < p["win"][1]]

    def src_to_out(self, p, ts):
        o = 0.0
        for a, b, sp in p["segs"]:
            if ts <= a: return o
            if ts < b: return o + (ts - a) / sp
            o += (b - a) / sp
        return o

    # ---- footage ------------------------------------------------------------------------
    def footage(self, p):
        """Yield decoded frames for the scene, holding the last frame if the scene outlasts the footage."""
        n_total = int(round(p["dur"] * self.fps)); n = 0; last = None
        for a, b, sp in p["segs"]:
            cmd = [FF, "-loglevel", "error", "-ss", f"{a:.3f}", "-t", f"{b - a:.3f}", "-i", str(self.take),
                   "-vf", f"setpts=PTS/{sp},fps={self.fps},scale={SRC_W}:{SRC_H}:flags=lanczos",
                   "-f", "rawvideo", "-pix_fmt", "rgb24", "-"]
            pr = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.DEVNULL)
            want = int(round((b - a) / sp * self.fps)); size = SRC_W * SRC_H * 3
            try:
                for _ in range(want):
                    buf = pr.stdout.read(size)
                    if len(buf) < size: break
                    last = Image.frombuffer("RGB", (SRC_W, SRC_H), buf, "raw", "RGB", 0, 1)
                    if n < n_total: yield last; n += 1
            finally:
                pr.kill(); pr.stdout.close(); pr.wait()   # the scene may end before the clip does
        while n < n_total: yield last; n += 1

    def zoom_at(self, p, ts):
        kf = p.get("zoom")
        if not kf: return 1.0, SRC_W / 2, SRC_H / 2
        if ts <= kf[0][0]: k = kf[0]; return k[1], k[2] * SRC_W / 1080, k[3] * SRC_H / 2400
        for (t0, z0, x0, y0), (t1, z1, x1, y1) in zip(kf, kf[1:]):
            if ts <= t1:
                e = ease_io((ts - t0) / max(1e-6, t1 - t0))
                return z0 + (z1 - z0) * e, (x0 + (x1 - x0) * e) * SRC_W / 1080, (y0 + (y1 - y0) * e) * SRC_H / 2400
        k = kf[-1]; return k[1], k[2] * SRC_W / 1080, k[3] * SRC_H / 2400

    def crop_box(self, z, cx, cy):
        w, h = SRC_W / z, SRC_H / z
        x0 = clamp(cx - w / 2, 0, SRC_W - w); y0 = clamp(cy - h / 2, 0, SRC_H - h)
        return (x0, y0, x0 + w, y0 + h)

    def to_screen(self, x, y, box):
        """Map a point in 1080x2400 device pixels to canvas pixels given the current crop."""
        sx, sy = x * SRC_W / 1080, y * SRC_H / 2400
        return (self.sx0 + (sx - box[0]) / (box[2] - box[0]) * SCR_W, self.sy0 + (sy - box[1]) / (box[3] - box[1]) * SCR_H)

    # ---- drawing ------------------------------------------------------------------------
    def draw_phone(self, canvas, p, frame, ts_src, t_out, alpha, dx):
        z, cx, cy = self.zoom_at(p, ts_src)
        box = self.crop_box(z, cx, cy)
        scr = frame.resize((SCR_W, SCR_H), Image.BILINEAR, box=box)
        layer = self.phone_base.copy()
        layer.paste(scr, (self.phone_pad + BEZEL, self.phone_pad + BEZEL), self.scr_mask)
        # taps and swipes
        ov = Image.new("RGBA", layer.size, (0, 0, 0, 0)); od = ImageDraw.Draw(ov)
        ox, oy = self.phone_pad + BEZEL, self.phone_pad + BEZEL
        def pt(x, y):
            px, py = self.to_screen(x, y, box); return px - self.sx0 + ox, py - self.sy0 + oy
        for e in p["taps"]:
            age = t_out - (self.src_to_out(p, e["t"] - 0.26))
            if 0 <= age < 0.6:
                u = age / 0.6; r = 16 + 64 * ease_out(u); a = 1 - u
                x, y = pt(e["x"], e["y"])
                od.ellipse((x - r, y - r, x + r, y + r), fill=(30, 41, 59, int(55 * a)), outline=(255, 255, 255, int(230 * a)), width=4)
                r2 = r + 9; od.ellipse((x - r2, y - r2, x + r2, y + r2), outline=(*RED, int(190 * a)), width=3)
                if age < 0.16: od.ellipse((x - 21, y - 21, x + 21, y + 21), fill=(30, 41, 59, 150), outline=(255, 255, 255, 220), width=3)
        for e in p["swipes"]:
            t0 = self.src_to_out(p, e["t"] - 0.26); span = max(0.25, e["d"] / max(1.0, self._speed_at(p, e["t"])))
            age = t_out - t0
            if -0.1 <= age <= span + 0.25:
                u = clamp(age / span); k = ease_io(u)
                x, y = pt(e["x"] + (e["x2"] - e["x"]) * k, e["y"] + (e["y2"] - e["y"]) * k)
                fade = clamp(1 - max(0, age - span) / 0.25) * clamp((age + 0.1) / 0.1)
                od.ellipse((x - 26, y - 26, x + 26, y + 26), fill=(30, 41, 59, int(95 * fade)), outline=(255, 255, 255, int(230 * fade)), width=4)
        layer.alpha_composite(ov)
        if alpha < 1: layer.putalpha(layer.getchannel("A").point(lambda v, a=alpha: int(v * a)))
        canvas.alpha_composite(layer, (int(self.px0 + dx), self.py0))

    def _speed_at(self, p, ts):
        for a, b, sp in p["segs"]:
            if a <= ts < b: return sp
        return 1.0

    def wrap(self, text, f, width):
        words, lines, cur = text.split(), [], ""
        for w in words:
            t = (cur + " " + w).strip()
            if f.getlength(t) <= width: cur = t
            else: lines.append(cur); cur = w
        if cur: lines.append(cur)
        return lines

    def draw_panel(self, canvas, p, t_out, idx):
        dur = p["dur"]
        a_in = ease_out(t_out / 0.55); a_out = 1 - ease_io((t_out - (dur - 0.32)) / 0.32)
        al = clamp(a_in * a_out)
        if al <= 0.004: return
        rise = int((1 - a_in) * 30)
        lay = Image.new("RGBA", (1060, 900), (0, 0, 0, 0)); d = ImageDraw.Draw(lay)
        # step pill
        pill = f"STEP {idx} OF 5"
        pf = font(24, True); pw = int(pf.getlength(pill)) + 44
        d.rounded_rectangle((0, 20, pw, 66), 23, fill=RED_LIGHT)
        d.text((22, 28), pill, font=pf, fill=RED_DARK)
        d.text((0, 100), "", font=font(10))
        hf = font(84, True); y = 106
        for line in self.wrap(p["title"], hf, 960):
            d.text((0, y), line, font=hf, fill=S900); y += 98
        d.rounded_rectangle((0, y + 8, 150 * ease_out((t_out - 0.25) / 0.5) + 2, y + 16), 4, fill=RED)
        y += 44
        sf = font(36)
        for line in self.wrap(p["sub"], sf, 900):
            d.text((0, y), line, font=sf, fill=S600); y += 50
        y += 34
        # checklist
        trig = []
        for label, (kind, i) in [(s[0], s[1]) for s in p["steps"]]:
            if kind == "tap": tt = self.src_to_out(p, p["taps"][min(i, len(p["taps"]) - 1)]["t"] - 0.26) if p["taps"] else 0
            elif kind == "swipe": tt = self.src_to_out(p, p["swipes"][min(i, len(p["swipes"]) - 1)]["t"] - 0.26) if p["swipes"] else 0
            else: tt = float(i)
            trig.append((label, max(0.5, tt)))
        active = max([k for k, (_, tt) in enumerate(trig) if t_out >= tt] or [-1])
        lf, lb = font(36), font(36, True)
        for k, (label, tt) in enumerate(trig):
            u = ease_out((t_out - tt + 0.25) / 0.4)
            if u <= 0: continue
            yy = y + k * 66 + int((1 - u) * 16)
            done = k < active
            col = (*RED, int(255 * u)) if not done else (*GREEN, int(255 * u))
            d.ellipse((0, yy + 4, 40, yy + 44), fill=col)
            if done:
                d.line([(11, yy + 25), (18, yy + 32), (30, yy + 16)], fill=(255, 255, 255, 255), width=5, joint="curve")
            else:
                d.ellipse((13, yy + 17, 27, yy + 31), fill=(255, 255, 255, 255))
            d.text((60, yy + 2), label, font=lb if k == active else lf, fill=(*(S900 if k == active else S600), int(255 * u)))
        if al < 1: lay.putalpha(lay.getchannel("A").point(lambda v, a=al: int(v * a)))
        canvas.alpha_composite(lay, (140, 120 + rise))

    def draw_dots(self, canvas, idx):
        d = ImageDraw.Draw(canvas)
        n = len(SCENES); x = W // 2 - (n * 26) // 2 + 13
        for i in range(n):
            on = i == idx
            w = 30 if on else 10
            cx = x + i * 26 + (10 if i > idx else 0)
            d.rounded_rectangle((cx - w // 2 + (10 if on else 0), H - 44, cx + w // 2 + (10 if on else 0), H - 34), 5, fill=RED if on else S200)

    def draw_title(self, canvas, p, t, closing=False):
        d = ImageDraw.Draw(canvas)
        a = ease_out(t / 0.8)
        size = int(300 + 30 * a)
        ic = draw_mark(size)
        ic.putalpha(ic.getchannel("A").point(lambda v, a=a: int(v * a)))
        y0 = 150 - int((1 - a) * 40)
        canvas.alpha_composite(ic, (W // 2 - size // 2, y0))
        f = font(118, True); title = "AceTimeTracker"
        ta = ease_out((t - 0.55) / 0.7)
        tw = f.getlength(title)
        lay = Image.new("RGBA", (W, 160), (0, 0, 0, 0)); ld = ImageDraw.Draw(lay)
        ld.text(((W - tw) / 2, 10), title, font=f, fill=S900)
        lay.putalpha(lay.getchannel("A").point(lambda v, a=ta: int(v * a)))
        canvas.alpha_composite(lay, (0, 530 + int((1 - ta) * 26)))
        tag = "Track your work. Make time for what matters."
        tf = font(46); tga = ease_out((t - 1.1) / 0.7); tl = tf.getlength(tag)
        lay = Image.new("RGBA", (W, 80), (0, 0, 0, 0)); ImageDraw.Draw(lay).text(((W - tl) / 2, 6), tag, font=tf, fill=S600)
        lay.putalpha(lay.getchannel("A").point(lambda v, a=tga: int(v * a)))
        canvas.alpha_composite(lay, (0, 700))
        if closing:
            lines = [("Open source · MIT · github.com/MphoCodes/AceTimeTracker", RED, 36, True),
                     ("Development preview with sample data — not yet on Google Play", S400, 28, False)]
            for i, (s, col, sz, b) in enumerate(lines):
                ga = ease_out((t - 2.0 - i * 0.4) / 0.6); ff = font(sz, b); w_ = ff.getlength(s)
                lay = Image.new("RGBA", (W, 70), (0, 0, 0, 0)); ImageDraw.Draw(lay).text(((W - w_) / 2, 6), s, font=ff, fill=col)
                lay.putalpha(lay.getchannel("A").point(lambda v, a=ga: int(v * a)))
                canvas.alpha_composite(lay, (0, 830 + i * 56))
        else:
            ga = ease_out((t - 1.9) / 0.6); ff = font(30); s = "A guided tour of the app, recorded on a real device"
            lay = Image.new("RGBA", (W, 60), (0, 0, 0, 0)); ImageDraw.Draw(lay).text(((W - ff.getlength(s)) / 2, 6), s, font=ff, fill=S400)
            lay.putalpha(lay.getchannel("A").point(lambda v, a=ga: int(v * a)))
            canvas.alpha_composite(lay, (0, 860))

    # ---- render -------------------------------------------------------------------------
    def render_video(self, out_path, limit=None):
        sc = self.scale; ow, oh = int(W * sc), int(H * sc)
        enc = subprocess.Popen([FF, "-y", "-loglevel", "error", "-f", "rawvideo", "-pix_fmt", "rgb24", "-s", f"{ow}x{oh}", "-r", str(self.fps),
                                "-i", "-", "-c:v", "libx264", "-preset", "medium", "-crf", "17", "-pix_fmt", "yuv420p", "-an", str(out_path)],
                               stdin=subprocess.PIPE)
        total_frames = int(self.total * self.fps); done = 0
        for si, p in enumerate(self.plan):
            nfr = int(round(p["dur"] * self.fps))
            gen = self.footage(p) if p["kind"] == "app" else None
            for f in range(nfr):
                if limit and done >= limit * self.fps: break
                t = f / self.fps
                canvas = self.bg.convert("RGBA")
                if p["kind"] == "app":
                    frame = next(gen)
                    ts = self._src_time(p, t)
                    # phone slides in on the first app scene, and out after the last one
                    alpha, dx = 1.0, 0.0
                    if si == 1: u = ease_out(t / 0.8); alpha, dx = u, (1 - u) * 90
                    if si == 5: u = ease_io((t - (p["dur"] - 0.6)) / 0.6); alpha, dx = 1 - u, u * 90
                    self.draw_panel(canvas, p, t, si)
                    self.draw_phone(canvas, p, frame, ts, t, alpha, dx)
                    self.draw_dots(canvas, si)
                else:
                    self.draw_title(canvas, p, t, closing=p["kind"] == "closing")
                    self.draw_dots(canvas, si)
                # scene-to-scene dip through the background
                edge = min(t, p["dur"] - t)
                img = canvas.convert("RGB")
                if p["kind"] != "app" or si in (1, 5):
                    if edge < 0.35: img = Image.blend(self.bg, img, ease_io(edge / 0.35))
                if (ow, oh) != (W, H): img = img.resize((ow, oh), Image.BILINEAR)
                enc.stdin.write(img.tobytes()); done += 1
                if done % (self.fps * 4) == 0: print(f"\r{done}/{total_frames} frames", end="", flush=True)
        enc.stdin.close(); enc.wait(); print()

    def _src_time(self, p, t_out):
        o = 0.0
        for a, b, sp in p["segs"]:
            seg = (b - a) / sp
            if t_out < o + seg: return a + (t_out - o) * sp
            o += seg
        return p["segs"][-1][1]

    # ---- audio --------------------------------------------------------------------------
    def decode(self, path, filt=None):
        cmd = [FF, "-loglevel", "error", "-i", str(path), "-ar", "48000", "-ac", "2"] + (["-af", filt] if filt else []) + ["-f", "f32le", "-"]
        raw = subprocess.run(cmd, capture_output=True, check=True).stdout
        return np.frombuffer(raw, dtype=np.float32).reshape(-1, 2).copy()

    def render_audio(self, out_wav):
        sr = 48000; n = int(self.total * sr) + sr
        voice = np.zeros((n, 2), np.float32); windows = []
        vfilt = "highpass=f=70,equalizer=f=3200:t=q:w=1.2:g=1.8,acompressor=threshold=-21dB:ratio=2.2:attack=6:release=90:makeup=3"
        for p in self.plan:
            v = self.decode(self.voice / f"{p['voice']}.wav", vfilt)
            s = int((p["start"] + p["vlead"]) * sr)
            voice[s:s + len(v)] += v[: max(0, n - s)]
            windows.append((s / sr - 0.25, (s + len(v)) / sr + 0.35))
        bed = np.zeros((n, 2), np.float32)
        if self.a.music and Path(self.a.music).exists():
            m = self.decode(self.a.music)
            reps = int(math.ceil(n / len(m))); m = np.tile(m, (reps, 1))[:n]
            env = np.full(n, 0.30, np.float32)
            for a, b in windows: env[int(max(0, a) * sr): int(b * sr)] = 0.13
            k = int(0.45 * sr); env = np.convolve(env, np.ones(k) / k, mode="same")        # smooth the ducking
            fade_in = np.minimum(1, np.arange(n) / (1.5 * sr)); fade_out = np.clip((self.total * sr - np.arange(n)) / (3.0 * sr), 0, 1)
            bed = m * (env * fade_in * fade_out)[:, None]
        mix = voice + bed
        peak = np.abs(mix).max()
        if peak > 0.98: mix *= 0.98 / peak
        import wave
        with wave.open(str(out_wav), "wb") as w:
            w.setnchannels(2); w.setsampwidth(2); w.setframerate(sr)
            w.writeframes((mix[: int(self.total * sr)] * 32767).astype(np.int16).tobytes())

    # ---- captions -----------------------------------------------------------------------
    def write_srt(self, path):
        def ts(x): h, r = divmod(x, 3600); m, s = divmod(r, 60); return f"{int(h):02}:{int(m):02}:{int(s):02},{int(round((s % 1) * 1000)):03}"
        out, n = [], 1
        for p in self.plan:
            text = self.meta[p["voice"]]["text"]
            # Spoken forms ("P D F") are for the voice; captions use normal spelling.
            text = text.replace("P D F", "PDF").replace("C S V", "CSV")
            sents = [s.strip() for s in text.replace("? ", "?|").replace(". ", ".|").split("|") if s.strip()]
            total_chars = sum(len(s) for s in sents); t = p["start"] + p["vlead"]
            for s in sents:
                d = p["vlen"] * len(s) / total_chars
                out.append(f"{n}\n{ts(t)} --> {ts(t + d)}\n{s}\n"); n += 1; t += d
        Path(path).write_text("\n".join(out))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--take", required=True); ap.add_argument("--voice", required=True)
    ap.add_argument("--music")
    ap.add_argument("--out", default=str(REPO / "docs" / "demo" / "acetimetracker-guided-tour.mp4"))
    ap.add_argument("--preview", action="store_true", help="half-size, 15 fps")
    ap.add_argument("--limit", type=float, help="render only the first N seconds (video), for quick checks")
    a = ap.parse_args()
    film = Film(a)
    out = Path(a.out); out.parent.mkdir(parents=True, exist_ok=True)
    print(f"total {film.total:.1f}s; scenes: " + ", ".join(f"{p['id']}={p['dur']:.1f}" for p in film.plan))
    with tempfile.TemporaryDirectory(prefix="ace-tour-") as td:
        vid, aud = Path(td) / "v.mp4", Path(td) / "a.wav"
        film.render_video(vid, a.limit); film.render_audio(aud)
        subprocess.run([FF, "-y", "-loglevel", "error", "-i", str(vid), "-i", str(aud), "-af", "loudnorm=I=-16:TP=-1.5:LRA=9",
                        "-c:v", "copy", "-c:a", "aac", "-b:a", "192k", "-ar", "48000", "-movflags", "+faststart", "-shortest", str(out)], check=True)
    film.write_srt(out.with_suffix(".srt"))
    print("wrote", out)


if __name__ == "__main__":
    main()
