#!/usr/bin/env bash
# Compile Noir et l'installe sur le téléphone via ADB Wi-Fi.
# Usage : ./install.sh [IP:PORT]   (le port change à chaque activation du débogage sans fil)
set -e
cd "$(dirname "$0")"
./gradlew assembleRelease -q
if [ -n "$1" ]; then adb connect "$1"; fi
adb install -r app/build/outputs/apk/release/app-release.apk
adb shell cmd notification allow_listener fr.babas.noir/fr.babas.noir.MediaListenerService
adb shell am start -n fr.babas.noir/.MainActivity
