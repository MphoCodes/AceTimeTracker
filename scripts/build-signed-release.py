#!/usr/bin/env python3
"""Build using private local signing configuration, without printing secrets."""
import json
import os
from pathlib import Path
import subprocess
import sys

root = Path(__file__).resolve().parents[1]
config = Path(os.environ.get('ACE_SIGNING_CONFIG', str(Path.home() / 'Library/Application Support/AceTimeTracker/signing/release-signing.json')))
if not config.is_file():
    sys.exit('Signing configuration missing. Set ACE_SIGNING_CONFIG to your private JSON configuration; see docs/release.md.')
values = json.loads(config.read_text())
required = ('ACE_RELEASE_STORE_FILE', 'ACE_RELEASE_STORE_PASSWORD', 'ACE_RELEASE_KEY_ALIAS', 'ACE_RELEASE_KEY_PASSWORD')
if any(not values.get(key) for key in required):
    sys.exit('Signing configuration is incomplete.')
if config.stat().st_mode & 0o077:
    sys.exit('Signing configuration must be owner-only (chmod 600).')
env = dict(os.environ, **{key: str(values[key]) for key in required})
subprocess.run([str(root / 'gradlew'), '--console=plain', ':app:assembleRelease', ':app:bundleRelease'], cwd=root, env=env, check=True)
