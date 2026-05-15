#!/usr/bin/env bash
# Збирає debug-APK і заливає у Firebase App Distribution.
# Auth — через Firebase CLI (`firebase login` має бути зроблено заздалегідь).
#
# Чому debug, а не release: для DieselCalc немає release keystore'а,
# а debug-APK Firebase App Distribution приймає без проблем для тестування.
#
# Передумови:
#   - firebase.properties у корені репо (appId + groups, з firebase.properties.sample)
#   - npm install -g firebase-tools && firebase login
#
# Usage:
#   ./scripts/distribute-android.sh                       # release notes = git log -1
#   ./scripts/distribute-android.sh "fix виглубокий баг X"  # custom notes

set -euo pipefail

# Корінь репо — на рівень вище від scripts/.
cd "$(dirname "$0")/.."

if [ ! -f firebase.properties ]; then
    echo "ERROR: firebase.properties не знайдено."
    echo "  Скопіюй: cp firebase.properties.sample firebase.properties"
    echo "  Підстав appId з Firebase Console → Project Settings → Your apps."
    exit 1
fi

if ! command -v firebase >/dev/null 2>&1; then
    echo "ERROR: firebase CLI не встановлено."
    echo "  npm install -g firebase-tools"
    exit 1
fi

if ! firebase projects:list >/dev/null 2>&1; then
    echo "ERROR: firebase CLI не залогінено."
    echo "  firebase login"
    exit 1
fi

# Release notes — або з аргументу, або останній git commit.
mkdir -p build
if [ "${1:-}" != "" ]; then
    printf '%s\n' "$1" > build/last-commit.txt
else
    git log -1 --pretty='%h %s%n%n%b' > build/last-commit.txt
fi

echo "==> Release notes:"
sed 's/^/    /' build/last-commit.txt
echo

echo "==> Building debug APK + uploading to Firebase App Distribution..."
./gradlew clean assembleDebug appDistributionUploadDebug

echo
echo "✓ APK залито. Тестери з invite link отримають доступ до нової збірки."
