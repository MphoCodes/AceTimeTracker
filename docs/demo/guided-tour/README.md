# Guided-tour film

A ~60-second, 1080p30 walkthrough of the app. **Everything inside the phone is real footage** of the debug build running on an Android emulator; the tap rings, step checklist and captions are drawn from the logged input events so they match what the viewer sees.

Output: `../acetimetracker-guided-tour.mp4` (+ `.srt` captions and `.jpg` poster). The older OpenFilm showcase (`../acetimetracker-demo.mp4`) is untouched; the public website still pins that file's checksum.

## Story

Title → 1 *Your day, at a glance* → 2 *Track a session* (Start session → category → play → pause → save) → 3 *Missed one? Add it later* (category, start/end pickers, note, save) → 4 *Find and fix anything* (search, scroll, edit/track again) → 5 *See where the week went* (period comparison, goals vs actual, daily timeline with its time zone, week/month) → closing card.

The timer wait and the time-picker taps are sped up (2.2×) and the timer screen punches in; the rest is real time.

## Rebuild

1. **Record** — start an emulator at 1080×2400, install the debug APK (`./gradlew :app:assembleDebug`), then
   `python3 record.py /tmp/take` and convert: `ffmpeg -i /tmp/take/take.mp4 -vf fps=30 -an -c:v libx264 -crf 14 /tmp/take/take30.mp4`.
   Tap coordinates are for the layout of that size/density (420 dpi, light theme, font scale 1.0); change them if the UI moves.
2. **Narrate** — `narrate.py` (see its docstring) uses local Kokoro-82M, voice `af_heart`, speed 1.0. Output goes to a `voice/` directory.
3. **Compose** — `python3 build.py --take /tmp/take --voice /tmp/voice --music ../film/assets/minimal-product-launch-pads-c52fb2.mp3`.
   `--preview --limit 30` renders a quick half-size check. Needs `pip install pillow numpy imageio-ffmpeg`.
   Edit `SCENES` in `build.py` to change cut points, speeds, zooms, titles and checklist items.

`build.py` ducks the music under the voice, applies light EQ/compression to the narration and normalises the final mix to −16 LUFS.

## Provenance and limits

- **Footage:** this repository's debug build, "Explore the demo" sample data only. No account, backend or real user data. The status bar is SystemUI demo mode (8:41, full battery). Entry times in History come from the recording moment.
- **Narration:** [Kokoro-82M v1.0](https://huggingface.co/hexgrad/Kokoro-82M) (Apache-2.0) via [kokoro-onnx](https://github.com/thewh1teagle/kokoro-onnx) (MIT). Stock synthetic voice; nobody's voice was cloned.
- **Music:** OpenFilm library track `minimal-product-launch-pads-c52fb2.mp3`, CC0 per OpenFilm's library; already in `../film/assets/`.
- **Mark:** redrawn from `docs/brand/ace-mark.svg`.
- **Font:** Helvetica Neue from macOS is used only to render captions into the video; it is not redistributed.
- This is a development preview. The film does not claim Google Play availability, verified cloud sync or account deletion.
- The raw 63-second screen take is not committed; re-record to change footage.
