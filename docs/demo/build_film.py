"""Build editable OpenFilm pages from approved narration and real app captures.
Run from repository root: python3 docs/demo/build_film.py.
Narration WAVs and narration.json are delivered with the film; no API key needed.
"""
from pathlib import Path
import json, shutil, html
ROOT=Path(__file__).resolve().parents[2]
FILM=ROOT/'docs/demo/film'; ASSETS=FILM/'assets'
ASSETS.mkdir(parents=True,exist_ok=True)
for p in (ROOT/'docs/screenshots').glob('*.png'): shutil.copy2(p,ASSETS/p.name)
shutil.copy2(ROOT/'docs/brand/ace-mark.svg',ASSETS/'ace-mark.svg')
meta=json.loads((ASSETS/'narration.json').read_text())
scenes=[
('MAKE TIME COUNT','A clearer view.\nA more focused day.','Track your work. Make time for what matters.','today.png'),
('01 / SEE YOUR DAY','Know where\nyour time goes.','Your daily total, categories and progress.','today.png'),
('02 / FIND YOUR FOCUS','One session.\nYour full attention.','Stopwatch or Pomodoro. Your choice.','timer.png'),
('03 / KEEP THE CONTEXT','Every session\ntells a story.','Add time, notes and photos.','add-time.png'),
('04 / REVIEW YOUR WORK','A history you\ncan work with.','Find, edit and revisit your sessions.','history.png'),
('05 / SEE THE BIGGER PICTURE','Turn time\ninto perspective.','Compare periods. Share PDF and CSV reports.','reports.png'),
('BUILT WITH INTENT','Make time for\nwhat matters.','Mpho Ndlela · MphoCodes','today.png')]
css='''*{box-sizing:border-box}html,body{margin:0;width:100%;height:100%;overflow:hidden}body{font-family:Arial,Helvetica,sans-serif;background:#f8fafc;color:#0f172a}.bar{position:absolute;left:110px;top:70px;display:flex;align-items:center;gap:18px;font-size:27px;font-weight:700}.bar img{width:58px;height:58px}.eyebrow{position:absolute;top:280px;left:120px;color:#dc2626;font-size:19px;font-weight:700;letter-spacing:4px}h1{position:absolute;left:116px;top:320px;font-size:88px;line-height:1.06;letter-spacing:-4px;margin:0;font-weight:700;white-space:pre-line}.detail{position:absolute;left:120px;top:570px;font-size:27px;line-height:1.6;max-width:780px;color:#475569}.rule{position:absolute;left:120px;top:675px;background:#dc2626;width:94px;height:6px}.foot{position:absolute;left:120px;bottom:76px;font-size:19px;letter-spacing:1px;color:#64748b}.phone{position:absolute;left:1230px;top:80px;width:410px;height:911px;background:#0f172a;border:10px solid #0f172a;border-radius:48px;overflow:hidden;box-shadow:0 30px 70px #0f172a25}.phone img{width:100%;height:100%;object-fit:cover}.phone:after{content:'';position:absolute;top:10px;left:185px;width:12px;height:12px;background:#111827;border-radius:50%}.index{position:absolute;right:105px;bottom:70px;font-size:19px;color:#64748b}.progress{position:absolute;bottom:0;height:5px;background:#dc2626;left:0}.dark{background:#0f172a;color:#fff}.dark .detail{color:#cbd5e1}.dark .eyebrow{color:#f87171}.dark .phone{border-color:#334155}.dark .foot,.dark .index{color:#94a3b8}.dark .bar img{background:white;border-radius:12px}.credit{position:absolute;left:120px;top:735px;color:#94a3b8;font-size:22px}'''
(FILM/'style.css').write_text(css)
clips=[];audio=[];offset=0;srt=[]
def stamp(t):
 ms=round(t*1000);return f'{ms//3600000:02}:{ms//60000%60:02}:{ms//1000%60:02},{ms%1000:03}'
for i,(kicker,title,detail,shot) in enumerate(scenes):
 duration=meta[i]['duration'];dark=i in (0,6)
 # Phone images are actual captures; no invented UI.
 page=f'''<!doctype html><html><head><meta charset="utf-8"><link rel="stylesheet" href="style.css"></head><body class="{'dark' if dark else ''}"><div class="bar"><img src="assets/ace-mark.svg">AceTimeTracker</div><div class="eyebrow">{kicker}</div><h1>{html.escape(title)}</h1><div class="detail">{html.escape(detail)}</div><div class="rule"></div><div class="phone"><img src="assets/{shot}"></div><div class="foot">{'ANDROID · PRODUCT WALKTHROUGH' if i!=6 else 'github.com/MphoCodes/AceTimeTracker'}</div><div class="index">{i+1:02} / 07</div>{'<div class="credit">Software engineer · Android &amp; product development</div>' if i==6 else ''}<div class="progress"></div><script>window.film={{duration:{duration},ready:Promise.all(Array.from(document.images).map(x=>x.decode())),frame(t){{const e=1-Math.pow(1-Math.min(1,t/.65),3);for(const x of document.querySelectorAll('h1,.detail,.eyebrow,.rule')){{x.style.opacity=e;x.style.transform=`translateY(${{(1-e)*22}}px)`}}document.querySelector('.phone').style.transform=`translateY(${{(1-e)*28}}px)`;document.querySelector('.progress').style.width=`${{100*({offset}+t)/{sum(m['duration'] for m in meta)}}}%`}}}};</script></body></html>'''
 (FILM/f'scene-{i}.html').write_text(page)
 clips.append(f'<iframe src="scene-{i}.html#t=0,{duration}" at="{offset:.3f}"></iframe>')
 audio.append(f'<audio src="assets/voice-{i}.wav" at="{offset+.35:.3f}" volume="1"></audio>')
 srt.append(f'{i+1}\n{stamp(offset+.35)} --> {stamp(offset+duration-.3)}\n{meta[i]["text"]}\n')
 offset+=duration
timer_start=sum(m['duration'] for m in meta[:2])
timer_end=1+min(7.848,meta[2]['duration'])
video=f'<section><video src="assets/timer-recording.mp4#t=1,{timer_end:.3f}" at="{timer_start:.3f}" muted style="left:1240px;top:90px;width:390px;height:891px;object-fit:cover;border-radius:37px"></video></section>'
(FILM/'film.html').write_text('<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=1920, height=1080"></head><body>'+video+'<section>'+''.join(clips)+'</section><section>'+''.join(audio)+'</section><section><audio src="assets/minimal-product-launch-pads-c52fb2.mp3#t=0,'+str(offset)+'" volume="0.10"></audio></section></body></html>')
(ROOT/'docs/demo/acetimetracker-demo.srt').write_text('\n'.join(srt))
print(f'Built seven editable shots, {offset:.2f}s')
