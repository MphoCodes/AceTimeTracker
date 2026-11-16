"""Regenerate included narration. Requires Python 3.12, kokoro-onnx and soundfile. Stock voice, no cloning."""
from pathlib import Path
from kokoro_onnx import Kokoro
import soundfile as sf,json
import argparse
a=argparse.ArgumentParser();a.add_argument('--models',type=Path,required=True);args=a.parse_args()
out=Path(__file__).resolve().parent/'film/assets'
lines=[
'Busy days can blur together. Ace Time Tracker helps you see where your time goes, and make room for what matters.',
'Start with a clear view of your day. See your progress, choose a category, and decide what deserves your attention.',
'Ready to focus? Start the timer, or use a Pomodoro session. Your timer stays with you as you move through the app.',
'Missed a session? Add it by hand. Keep a note, attach a photo, and give your work the context it needs.',
'Your history stays easy to review. Find a session, correct a detail, or return to the work you want to continue.',
'See the bigger picture with reports. Compare periods, review your categories, and share a clear P D F or spreadsheet.',
'Built by Mpho Ndlela, at Mpho Codes. Ace Time Tracker. Track your work. Make time for what matters.'
]
k=Kokoro(str(args.models/'kokoro-v1.0.onnx'),str(args.models/'voices-v1.0.bin'))
metadata=[]
for i,line in enumerate(lines):
 samples,sr=k.create(line,voice='af_heart',speed=1.04,lang='en-us')
 sf.write(str(out/f'voice-{i}.wav'),samples,sr)
 metadata.append({'text':line,'duration':round(len(samples)/sr+1.0,3)})
 print(i,metadata[-1]['duration'],flush=True)
(out/'narration.json').write_text(json.dumps(metadata,indent=2))
