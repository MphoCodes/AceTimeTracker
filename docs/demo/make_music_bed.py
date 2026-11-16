#!/usr/bin/env python3
"""Synthesise a royalty-free instrumental bed for the demo video.

Used when no licensed track is available. Writes a 48 kHz stereo WAV:
a soft pad over a I–V–vi–IV progression, a felt-piano style pluck motif and a
light kick/snap pulse that builds across the track.

    python3 docs/demo/make_music_bed.py --seconds 52 --out /tmp/bed.wav
"""
from __future__ import annotations

import argparse
import math
import wave

import numpy as np

SR = 48000
BPM = 108
BEAT = 60 / BPM


def note(freq: float, length: float, attack: float = 0.01, decay: float = 0.6, sustain: float = 0.5, release: float = 0.3,
         harmonics=((1, 1.0), (2, 0.35), (3, 0.12), (4, 0.05))) -> np.ndarray:
    n = int(length * SR)
    t = np.arange(n) / SR
    wave_ = sum(a * np.sin(2 * math.pi * freq * k * t) for k, a in harmonics)
    env = np.ones(n)
    a, d, r = int(attack * SR), int(decay * SR), int(release * SR)
    env[:a] = np.linspace(0, 1, a)
    env[a:a + d] = np.linspace(1, sustain, min(d, n - a))
    env[a + d:] = sustain
    env[-r:] *= np.linspace(1, 0, r)
    return wave_ * env


def pad_chord(freqs, length: float) -> np.ndarray:
    n = int(length * SR)
    t = np.arange(n) / SR
    out = np.zeros(n)
    for f in freqs:
        for detune in (-0.3, 0.0, 0.3):
            out += np.sin(2 * math.pi * (f + detune) * t) * 0.18
            out += np.sin(2 * math.pi * (f * 2 + detune) * t) * 0.04
    # Slow swell in, long tail out.
    env = np.minimum(1, t / 0.8) * np.minimum(1, (length - t) / 1.2)
    lfo = 1 + 0.05 * np.sin(2 * math.pi * 0.2 * t)
    return out * env * lfo


def kick(length: float = 0.35) -> np.ndarray:
    n = int(length * SR)
    t = np.arange(n) / SR
    f = 120 * np.exp(-t * 18) + 45
    return np.sin(2 * math.pi * np.cumsum(f) / SR) * np.exp(-t * 9)


def snap(length: float = 0.12) -> np.ndarray:
    n = int(length * SR)
    rng = np.random.default_rng(7)
    noise = rng.standard_normal(n)
    env = np.exp(-np.arange(n) / SR * 60)
    hp = noise - np.concatenate([[0], noise[:-1]])
    return hp * env * 0.6


def midi(n: int) -> float:
    return 440 * 2 ** ((n - 69) / 12)


# C major, I–V–vi–IV in a warm register.
CHORDS = [
    [48, 55, 60, 64],  # C
    [43, 50, 55, 59],  # G
    [45, 52, 57, 60],  # Am
    [41, 48, 53, 57],  # F
]
MOTIF = [[72, 76, 79, 76], [74, 79, 83, 79], [76, 81, 84, 81], [77, 81, 84, 81]]


def build(seconds: float) -> np.ndarray:
    total = int(seconds * SR)
    mix = np.zeros(total)
    bar = BEAT * 4
    bars = int(math.ceil(seconds / bar)) + 1
    for b in range(bars):
        start = int(b * bar * SR)
        if start >= total:
            break
        chord = CHORDS[b % 4]
        build_amt = min(1.0, b / max(1, bars - 3))
        seg = pad_chord([midi(n) for n in chord], bar + 0.5) * (0.55 + 0.25 * build_amt)
        end = min(total, start + len(seg))
        mix[start:end] += seg[:end - start]
        # Pluck motif: quarter notes, slightly humanised timing.
        for i, n in enumerate(MOTIF[b % 4]):
            off = start + int((i * BEAT + (0.01 if i % 2 else 0)) * SR)
            if off >= total:
                break
            vel = 0.35 if i in (0, 2) else 0.22
            pl = note(midi(n), BEAT * 1.6, decay=0.35, sustain=0.25, release=0.5) * vel * (0.6 + 0.4 * build_amt)
            end = min(total, off + len(pl))
            mix[off:end] += pl[:end - off]
        # Pulse: kick on 1 and 3 after the first two bars, snaps on 2 and 4 after four bars.
        for beat in range(4):
            off = start + int(beat * BEAT * SR)
            if off >= total:
                break
            if b >= 2 and beat in (0, 2):
                k = kick() * (0.5 + 0.4 * build_amt)
                end = min(total, off + len(k)); mix[off:end] += k[:end - off]
            if b >= 4 and beat in (1, 3):
                s = snap() * (0.35 + 0.3 * build_amt)
                end = min(total, off + len(s)); mix[off:end] += s[:end - off]
    # Final resolve: hold the tonic and fade.
    t = np.arange(total) / SR
    fade = np.minimum(1, (seconds - t) / 3.0).clip(0, 1)
    mix *= fade
    mix /= max(1e-6, np.abs(mix).max()) * 1.15
    # Gentle stereo width via a short delay on the right channel.
    delay = int(0.012 * SR)
    right = np.concatenate([np.zeros(delay), mix[:-delay]])
    stereo = np.stack([mix, 0.9 * right + 0.1 * mix], axis=1)
    return (stereo * 32767).astype(np.int16)


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--seconds", type=float, default=52)
    ap.add_argument("--out", default="bed.wav")
    args = ap.parse_args()
    data = build(args.seconds)
    with wave.open(args.out, "wb") as w:
        w.setnchannels(2); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes(data.tobytes())
    print(f"wrote {args.out} ({args.seconds:.0f}s)")


if __name__ == "__main__":
    main()
