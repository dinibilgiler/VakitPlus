#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
[ -f "$ROOT/app/src/main/java/com/vakitplus/app/MainActivity.kt" ]
[ -f "$ROOT/app/src/main/java/com/vakitplus/app/NotificationScheduler.kt" ]
[ -f "$ROOT/app/src/main/java/com/vakitplus/app/PrayerAlarmReceiver.kt" ]
[ -f "$ROOT/app/src/main/assets/surahs.json" ]
[ -f "$ROOT/app/src/main/assets/quran_uthmani_source.txt" ]
python3 - <<'PY'
import json, pathlib, re
root=pathlib.Path('.')
s=json.loads((root/'app/src/main/assets/surahs.json').read_text())
assert len(s)==114, len(s)
manifest=(root/'app/src/main/AndroidManifest.xml').read_text()
assert 'SCHEDULE_EXACT_ALARM' in manifest
assert 'USE_EXACT_ALARM' not in manifest
build=(root/'app/build.gradle.kts').read_text()
assert 'compileSdk = 36' in build and 'targetSdk = 36' in build
assert 'versionCode = 15' in build and 'versionName = "1.5.0"' in build
print('Vakit+ V1.5 static validation: PASS')
print('Surah count:', len(s))
print('API target:', 36)
print('Exact alarm permission: SCHEDULE_EXACT_ALARM')
PY
