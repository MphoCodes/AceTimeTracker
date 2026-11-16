# AceTimeTracker product film

> **Newer:** [`acetimetracker-guided-tour.mp4`](acetimetracker-guided-tour.mp4) is a one-minute walkthrough built from real emulator footage; see [guided-tour/README.md](guided-tour/README.md). The OpenFilm showcase below is kept because the public website pins its checksum.

50-second, 1920×1080, 30 fps product showcase. Built by Mpho Ndlela · MphoCodes.

The film uses actual anonymous Android demo screens, real timer footage, locally generated neural narration and a quiet music bed. The demonstrated app is a development build; the film does not claim Play availability or verified production cloud sync.

## Deliverables

- `acetimetracker-demo.mp4`: final mixed film.
- `acetimetracker-demo.jpg`: poster.
- `acetimetracker-demo.srt`: narration captions.
- `film/film.html`: editable OpenFilm timeline, with seven independent HTML shots and all used media under `film/assets/`.
- `build_film.py`: rebuilds shot pages from narration metadata and the current screenshots. The historical `build_demo.py` is the older system-voice renderer; use the OpenFilm workflow for this version.

## Rebuild and edit

Install Node 22.2+ and FFmpeg/ffprobe. Run `python3 docs/demo/build_film.py`, then `cd docs/demo/film` and `npx -y openfilm@latest open .`. Edit text, timing, voice clips, screenshots and music in Studio.

Verify with `npx -y openfilm@latest look`, then render with `npx -y openfilm@latest render --fps 30 --loudness=-16 --out ../acetimetracker-demo.mp4`. Inspect the resulting movie with `look --full` and listen to the final mix before replacing a published version. Screenshot and timer footage updates require a fresh anonymous emulator capture; no invented app screens are used.

## Media provenance

- UI and brand assets: AceTimeTracker repository, MIT licence.
- Narration: local [Kokoro-82M v1.0](https://huggingface.co/hexgrad/Kokoro-82M), Apache-2.0 model, `af_heart` stock voice, speed 1.04, with a speech-only phoneme override for Pomodoro (poh-moh-DOH-roh), via [kokoro-onnx](https://github.com/thewh1teagle/kokoro-onnx) (MIT). Synthetic stock voice; no person’s voice was cloned. WAV files are included so a rebuild needs no account or model download. To regenerate, install `requirements.txt` in a Python 3.12 environment, download the v1.0 ONNX model and voices from the kokoro-onnx model release, then run `python docs/demo/narrate.py --models /path/to/models` and rebuild the film.
- Music: OpenFilm library, `openfilm/audio/music/minimal-product-launch-pads-c52fb2.mp3`, CC0 according to OpenFilm’s media library. Mixed quietly under the voice.
- Editor/render pipeline: [OpenFilm](https://github.com/openfilm/openfilm), MIT. Checked 2026-11-16. No paid media generation service used.

Release/source limitations remain in `../release.md`. The written founder credit is owner-approved; the closing narration omits the founder’s name at the owner’s request. Captions retain the normal spelling of Pomodoro. Timer footage placement is derived from narration timing. Device data is fictional and for demonstration.
