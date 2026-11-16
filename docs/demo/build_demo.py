#!/usr/bin/env python3
"""Build the AceTimeTracker demo video from the README screenshots.

Renders every frame with Pillow, narrates with the macOS speech synthesiser and
encodes with the ffmpeg binary bundled in imageio-ffmpeg, so no system install
is needed beyond `pip3 install --user imageio-ffmpeg`.

    python3 docs/demo/build_demo.py            # writes docs/demo/acetimetracker-demo.mp4
    python3 docs/demo/build_demo.py --music bed.mp3   # mix a music bed under the voice

Run it from the repository root. macOS only (uses `say`).
"""
from __future__ import annotations

import argparse
import math
import os
import subprocess
import sys
import tempfile
from dataclasses import dataclass, field
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

try:
    import imageio_ffmpeg
except ImportError:  # pragma: no cover - guidance only
    sys.exit("pip3 install --user imageio-ffmpeg")

ROOT = Path(__file__).resolve().parents[2]
SHOTS = ROOT / "docs" / "screenshots"
OUT_DIR = ROOT / "docs" / "demo"
FFMPEG = imageio_ffmpeg.get_ffmpeg_exe()

W, H, FPS = 1920, 1080, 30

# Brand tokens mirror ui/theme/Color.kt.
RED = (220, 38, 38)
RED_DARK = (153, 27, 27)
RED_LIGHT = (254, 226, 226)
BLUE = (37, 99, 235)
SLATE_900 = (15, 23, 42)
SLATE_600 = (71, 85, 105)
SLATE_400 = (148, 163, 184)
SLATE_200 = (226, 232, 240)
SLATE_50 = (248, 250, 252)
WHITE = (255, 255, 255)

FONT_FILE = "/System/Library/Fonts/HelveticaNeue.ttc"


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont:
    for index in ([1, 0] if bold else [0]):
        try:
            return ImageFont.truetype(FONT_FILE, size, index=index)
        except OSError:
            continue
    return ImageFont.load_default()


@dataclass
class Scene:
    title: str
    bullets: list[str]
    narration: str
    screenshot: str | None = None
    min_seconds: float = 6.0
    duration: float = field(default=0.0)
    voice_file: Path | None = None


SCENES = [
    Scene(
        title="AceTimeTracker",
        bullets=["Track your work.", "Make time for what matters."],
        narration="Every day has twenty-four hours. Ace Time Tracker helps you see where yours actually go.",
        min_seconds=6.5,
    ),
    Scene(
        title="Your day at a glance",
        bullets=["Today's total and streak", "Weekly category goals", "One tap to start a session"],
        narration="Open the app and today's time is already in front of you, with weekly goals for every category.",
        screenshot="today.png",
    ),
    Scene(
        title="Log time your way",
        bullets=["Stopwatch or Pomodoro focus timer", "Manual entries with notes and photos", "Timer keeps running while you move around"],
        narration="Start a stopwatch, run a focus session, or add time by hand with a note and a photo.",
        screenshot="add-time.png",
    ),
    Scene(
        title="Review and correct",
        bullets=["Search and filter history", "Edit dates, notes and photos", "Track a category again in one tap"],
        narration="Everything you log is searchable and editable, and you can repeat a session with a single tap.",
        screenshot="history.png",
    ),
    Scene(
        title="See where the week went",
        bullets=["Custom ranges and prior-period comparison", "Goals versus actuals", "Share CSV or PDF reports"],
        narration="Compare any period with the one before it, check goals against actuals, and share a clean report.",
        screenshot="reports.png",
    ),
    Scene(
        title="Works offline. Yours to keep.",
        bullets=["Account-scoped local storage", "Sync when you're ready", "Open source on GitHub"],
        narration="It works offline, keeps your data on your device, and syncs when you're ready. Ace Time Tracker. Track your work. Make time for what matters.",
        min_seconds=7.0,
    ),
]


# --- easing -----------------------------------------------------------------

def ease_out(t: float) -> float:
    return 1 - (1 - t) ** 3


def ease_in_out(t: float) -> float:
    return t * t * (3 - 2 * t)


def clamp(x: float, lo: float = 0.0, hi: float = 1.0) -> float:
    return max(lo, min(hi, x))


# --- drawing helpers --------------------------------------------------------

def background() -> Image.Image:
    """Soft ivory canvas with a faint red glow in one corner."""
    img = Image.new("RGB", (W, H), SLATE_50)
    glow = Image.new("RGB", (W, H), SLATE_50)
    gd = ImageDraw.Draw(glow)
    gd.ellipse((W - 900, -500, W + 400, 700), fill=RED_LIGHT)
    glow = glow.filter(ImageFilter.GaussianBlur(220))
    return Image.blend(img, glow, 0.9)


BG = background()


def target_mark(size: int, dart_progress: float = 1.0) -> Image.Image:
    """Red target rings with a blue dart, matching docs/brand/ace-mark.svg."""
    s = size * 4  # supersample for smooth edges
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx = cy = s / 2
    for i, r in enumerate([0.48, 0.36, 0.24, 0.12]):
        d.ellipse((cx - r * s, cy - r * s, cx + r * s, cy + r * s), fill=RED if i % 2 == 0 else WHITE)
    d.ellipse((cx - 0.05 * s, cy - 0.05 * s, cx + 0.05 * s, cy + 0.05 * s), fill=RED_DARK)
    if dart_progress > 0:
        p = ease_out(clamp(dart_progress))
        start = (cx + 0.9 * s, cy - 0.9 * s)
        end = (cx + 0.02 * s, cy - 0.02 * s)
        tip = (start[0] + (end[0] - start[0]) * p, start[1] + (end[1] - start[1]) * p)
        tail = (tip[0] + 0.42 * s, tip[1] - 0.42 * s)
        d.line([tail, tip], fill=BLUE, width=int(0.045 * s))
        d.polygon([tail, (tail[0] + 0.06 * s, tail[1] - 0.14 * s), (tail[0] + 0.14 * s, tail[1] - 0.06 * s)], fill=BLUE)
        d.ellipse((tip[0] - 0.03 * s, tip[1] - 0.03 * s, tip[0] + 0.03 * s, tip[1] + 0.03 * s), fill=BLUE)
    return img.resize((size, size), Image.LANCZOS)


_PHONE_CACHE: dict[int, Image.Image] = {}
PHONE_MAX_HEIGHT = 1240


def phone_frame(shot: Image.Image, height: int) -> Image.Image:
    """Bezel + shadow rendered once per screenshot at full size, then scaled per frame."""
    key = id(shot)
    if key not in _PHONE_CACHE:
        _PHONE_CACHE[key] = _render_phone(shot, PHONE_MAX_HEIGHT)
    full = _PHONE_CACHE[key]
    if height >= PHONE_MAX_HEIGHT:
        return full
    ratio = height / PHONE_MAX_HEIGHT
    return full.resize((int(full.width * ratio), int(full.height * ratio)), Image.BILINEAR)


def _render_phone(shot: Image.Image, height: int) -> Image.Image:
    """Screenshot inside a rounded dark bezel with a soft drop shadow."""
    scale = height / shot.height
    inner = shot.resize((int(shot.width * scale), height), Image.LANCZOS)
    bezel = 22
    radius = 70
    pw, ph = inner.width + 2 * bezel, inner.height + 2 * bezel
    pad = 80
    canvas = Image.new("RGBA", (pw + 2 * pad, ph + 2 * pad), (0, 0, 0, 0))
    shadow = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle((pad, pad + 30, pad + pw, pad + ph + 30), radius=radius + 10, fill=(15, 23, 42, 110))
    shadow = shadow.filter(ImageFilter.GaussianBlur(40))
    canvas.alpha_composite(shadow)
    body = Image.new("RGBA", (pw, ph), (0, 0, 0, 0))
    ImageDraw.Draw(body).rounded_rectangle((0, 0, pw - 1, ph - 1), radius=radius, fill=(17, 24, 39, 255))
    mask = Image.new("L", inner.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, inner.width - 1, inner.height - 1), radius=radius - bezel, fill=255)
    body.paste(inner, (bezel, bezel), mask)
    canvas.alpha_composite(body, (pad, pad))
    return canvas


def text_block(draw: ImageDraw.ImageDraw, x: int, y: int, scene: Scene, t: float, duration: float) -> None:
    title_a = clamp((t - 0.15) / 0.45)
    rise = int((1 - ease_out(title_a)) * 30)
    draw.text((x, y + rise), scene.title, font=font(78, bold=True), fill=(*SLATE_900, int(255 * title_a)))
    bar_w = int(ease_out(clamp((t - 0.35) / 0.5)) * 140)
    draw.rounded_rectangle((x, y + 108, x + max(bar_w, 1), y + 116), radius=4, fill=RED)
    for i, line in enumerate(scene.bullets):
        a = clamp((t - 0.7 - i * 0.35) / 0.45)
        rise = int((1 - ease_out(a)) * 24)
        yy = y + 160 + i * 74 + rise
        draw.ellipse((x, yy + 22, x + 16, yy + 38), fill=(*RED, int(255 * a)))
        draw.text((x + 40, yy), line, font=font(40), fill=(*SLATE_600, int(255 * a)))


def render_scene_frame(scene: Scene, shot: Image.Image | None, t: float, idx: int, total: int) -> Image.Image:
    frame = BG.copy().convert("RGBA")
    draw = ImageDraw.Draw(frame, "RGBA")
    if shot is None:
        # Title / closing card: centred mark with the dart flying in.
        mark_a = ease_out(clamp(t / 0.6))
        size = int(300 + 40 * mark_a)
        mark = target_mark(size, dart_progress=clamp((t - 0.4) / 0.8))
        mark.putalpha(mark.getchannel("A").point(lambda v: int(v * mark_a)))
        frame.alpha_composite(mark, (W // 2 - size // 2, 190 - (size - 300) // 2))
        title_a = clamp((t - 0.6) / 0.6)
        title_font = font(112, bold=True)
        tw = draw.textlength(scene.title, font=title_font)
        draw.text(((W - tw) / 2, 560 + int((1 - ease_out(title_a)) * 30)), scene.title, font=title_font, fill=(*SLATE_900, int(255 * title_a)))
        for i, line in enumerate(scene.bullets):
            a = clamp((t - 1.2 - i * 0.4) / 0.5)
            f = font(44)
            lw = draw.textlength(line, font=f)
            draw.text(((W - lw) / 2, 720 + i * 64), line, font=f, fill=(*SLATE_600, int(255 * a)))
        if idx == total - 1:
            a = clamp((t - 2.4) / 0.6)
            f = font(34)
            s = "github.com/MphoCodes/AceTimeTracker"
            lw = draw.textlength(s, font=f)
            draw.text(((W - lw) / 2, 920), s, font=f, fill=(*RED, int(255 * a)))
    else:
        # Feature scene: text on the left, phone drifting upward on the right.
        text_block(draw, 150, 300, scene, t, scene.duration)
        progress = ease_in_out(clamp(t / scene.duration))
        height = int(1180 + 60 * progress)
        phone = phone_frame(shot, height)
        slide = int((1 - ease_out(clamp(t / 0.7))) * 80)
        y = int(120 - 160 * progress) + slide
        phone.putalpha(phone.getchannel("A").point(lambda v, a=ease_out(clamp(t / 0.5)): int(v * a)))
        frame.alpha_composite(phone, (1120, y))
        # Keep the phone clipped below the canvas edge for a clean bottom.
    # Scene index dots
    for i in range(total):
        cx = W // 2 - (total - 1) * 14 + i * 28
        draw.ellipse((cx - 5, H - 40, cx + 5, H - 30), fill=RED if i == idx else SLATE_200)
    return frame.convert("RGB")


# --- audio ------------------------------------------------------------------

def run(cmd: list[str]) -> None:
    subprocess.run(cmd, check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)


def probe_duration(path: Path) -> float:
    out = subprocess.run([FFMPEG, "-i", str(path)], capture_output=True, text=True).stderr
    for line in out.splitlines():
        if "Duration:" in line:
            hms = line.split("Duration:")[1].split(",")[0].strip()
            h, m, s = hms.split(":")
            return int(h) * 3600 + int(m) * 60 + float(s)
    raise RuntimeError(f"no duration for {path}")


def synthesize(scene: Scene, work: Path, voice: str, rate: int) -> None:
    aiff = work / f"{id(scene)}.aiff"
    # Light punctuation tuning keeps the system voice from rushing clause boundaries.
    text = scene.narration.replace(". ", ".  ").replace(", ", ",  ")
    run(["say", "-v", voice, "-r", str(rate), "-o", str(aiff), text])
    wav = aiff.with_suffix(".wav")
    run([FFMPEG, "-y", "-i", str(aiff), "-ar", "48000", "-ac", "2",
         "-af", "afftdn=nf=-30,highpass=f=80,lowpass=f=12000,acompressor=threshold=-18dB:ratio=2.5:attack=10:release=120,loudnorm=I=-18:TP=-2",
         str(wav)])
    scene.voice_file = wav
    scene.duration = max(scene.min_seconds, probe_duration(wav) + 1.4)


def build_audio(work: Path, music: Path | None, total: float) -> Path:
    """Narration placed at each scene start, with an optional ducked music bed."""
    inputs: list[str] = []
    filters: list[str] = []
    offset = 0.0
    for i, scene in enumerate(SCENES):
        inputs += ["-i", str(scene.voice_file)]
        filters.append(f"[{i}:a]adelay={int((offset + 0.6) * 1000)}|{int((offset + 0.6) * 1000)}[v{i}]")
        offset += scene.duration
    n = len(SCENES)
    filters.append("".join(f"[v{i}]" for i in range(n)) + f"amix=inputs={n}:normalize=0,apad=whole_dur={total:.2f}[voice]")
    if music:
        inputs += ["-i", str(music)]
        # Duck the bed with explicit automation from the known narration windows rather than a
        # sidechain, so the level is predictable and the bed recovers in the gaps between scenes.
        ducks = []
        offset = 0.0
        for scene in SCENES:
            spoken = probe_duration(scene.voice_file)
            a, b = offset + 0.4, offset + 0.6 + spoken + 0.3
            ducks.append(f"volume=enable='between(t,{a:.2f},{b:.2f})':volume=0.35")
            offset += scene.duration
        chain = ",".join(ducks)
        filters.append(f"[{n}:a]aloop=loop=-1:size=2e9,atrim=0:{total:.2f},afade=t=in:d=1.5,"
                       f"afade=t=out:st={total - 3:.2f}:d=3,volume=0.28,{chain}[bed]")
        filters.append("[voice][bed]amix=inputs=2:normalize=0[mix]")
        out_label = "[mix]"
    else:
        out_label = "[voice]"
    out = work / "audio.m4a"
    subprocess.run([FFMPEG, "-y", *inputs, "-filter_complex", ";".join(filters), "-map", out_label,
                    "-c:a", "aac", "-b:a", "192k", str(out)], check=True, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)
    return out


# --- main -------------------------------------------------------------------

def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--music", type=Path, help="optional music bed (mp3/wav)")
    ap.add_argument("--voice", default="Samantha", help="macOS voice name")
    ap.add_argument("--rate", type=int, default=168, help="words per minute")
    ap.add_argument("--out", type=Path, default=OUT_DIR / "acetimetracker-demo.mp4")
    ap.add_argument("--preview", action="store_true", help="render at 15fps/960px for a quick check")
    args = ap.parse_args()

    fps = 15 if args.preview else FPS
    work = Path(tempfile.mkdtemp(prefix="ace-demo-"))
    for scene in SCENES:
        synthesize(scene, work, args.voice, args.rate)
    total = sum(s.duration for s in SCENES)
    audio = build_audio(work, args.music, total)

    shots = {s.screenshot: Image.open(SHOTS / s.screenshot).convert("RGB") for s in SCENES if s.screenshot}
    out_w, out_h = (960, 540) if args.preview else (W, H)
    args.out.parent.mkdir(parents=True, exist_ok=True)
    enc = subprocess.Popen(
        [FFMPEG, "-y", "-f", "rawvideo", "-pix_fmt", "rgb24", "-s", f"{out_w}x{out_h}", "-r", str(fps), "-i", "-",
         "-i", str(audio), "-c:v", "libx264", "-preset", "medium", "-crf", "19", "-pix_fmt", "yuv420p",
         "-movflags", "+faststart", "-c:a", "copy", "-shortest", str(args.out)],
        stdin=subprocess.PIPE, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    assert enc.stdin
    total_frames = int(total * fps)
    done = 0
    for idx, scene in enumerate(SCENES):
        frames = int(scene.duration * fps)
        for f in range(frames):
            t = f / fps
            img = render_scene_frame(scene, shots.get(scene.screenshot), t, idx, len(SCENES))
            # Cross-fade edges: fade from/to white at scene boundaries.
            edge = min(t, scene.duration - t)
            if edge < 0.4:
                img = Image.blend(Image.new("RGB", (W, H), SLATE_50), img, ease_in_out(edge / 0.4))
            if (out_w, out_h) != (W, H):
                img = img.resize((out_w, out_h), Image.BILINEAR)
            enc.stdin.write(img.tobytes())
            done += 1
            if done % (fps * 5) == 0:
                print(f"\r{done}/{total_frames} frames", end="", flush=True)
    enc.stdin.close()
    enc.wait()
    print(f"\nwrote {args.out} ({total:.1f}s)")
    poster = render_scene_frame(SCENES[1], shots["today.png"], 2.5, 1, len(SCENES))
    poster.save(args.out.with_suffix(".jpg"), quality=88)


if __name__ == "__main__":
    os.chdir(ROOT)
    main()
