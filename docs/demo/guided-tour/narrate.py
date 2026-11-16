#!/usr/bin/env python3
"""Synthesise the narration with local Kokoro-82M (Apache-2.0); no account, no network after the model download.

    python3.12 -m venv venv && venv/bin/pip install kokoro-onnx==0.6.1 soundfile numpy
    # download kokoro-v1.0.onnx and voices-v1.0.bin from the kokoro-onnx "model-files-v1.0" release into ./models
    venv/bin/python narrate.py ./models ./voice

Writes voice/s0..s6.wav and voice/narration.json (text + seconds) for build.py. "P D F" / "C S V" are spelled
for the voice only; the captions written by build.py use normal spelling.
"""
import json, sys
from pathlib import Path
import numpy as np, soundfile as sf
from kokoro_onnx import Kokoro

MODELS = Path(sys.argv[1]); OUT = Path(sys.argv[2]); OUT.mkdir(parents=True, exist_ok=True)
VOICE, SPEED = "af_heart", 1.0

LINES = {
    "s0": "Meet Ace Time Tracker. A calm, simple way to see where your time really goes.",
    "s1": "Open the app, and your day is already waiting. Time tracked so far, your streak, and progress toward this week's goals.",
    "s2": "Ready to work? Tap Start session, pick what you're working on, and press play. When you're done, one tap saves it.",
    "s3": "Forgot to track something? Add it afterwards. Choose a category, set when you started and finished, and Ace adds up the time. Jot a quick note, and save.",
    "s4": "Everything you log lives in your history. Search it, filter it, fix a mistake, or track the same thing again.",
    "s5": "And for the bigger picture, Reports compare any period with the one before, check your goals, and share a P D F or C S V.",
    "s6": "Ace Time Tracker. Track your work. Make time for what matters.",
}

k = Kokoro(str(MODELS / "kokoro-v1.0.onnx"), str(MODELS / "voices-v1.0.bin"))
meta = {}
for key, text in LINES.items():
    samples, sr = k.create(text, voice=VOICE, speed=SPEED, lang="en-us")
    # Trim leading/trailing silence, then add a short natural tail.
    amp = np.abs(samples); idx = np.where(amp > 0.01)[0]
    samples = samples[max(0, idx[0] - int(0.05 * sr)): idx[-1] + int(0.12 * sr)]
    sf.write(OUT / f"{key}.wav", samples, sr)
    meta[key] = {"text": text, "seconds": round(len(samples) / sr, 2)}
    print(key, meta[key]["seconds"], flush=True)
(OUT / "narration.json").write_text(json.dumps(meta, indent=2))
