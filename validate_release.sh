#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
grep -q 'compileSdk = 36' "$ROOT/app/build.gradle.kts"
grep -q 'targetSdk = 36' "$ROOT/app/build.gradle.kts"
grep -q 'versionCode = 16' "$ROOT/app/build.gradle.kts"
grep -q 'versionName = "1.6.0"' "$ROOT/app/build.gradle.kts"
test -f "$ROOT/app/proguard-rules.pro"
test -f "$ROOT/PLAY_RELEASE_CHECKLIST.md"
test -f "$ROOT/PRIVACY_POLICY_TR.md"
test -f "$ROOT/PLAY_STORE_LISTING_TR.md"
echo 'STATIC VALIDATION: PASS'
