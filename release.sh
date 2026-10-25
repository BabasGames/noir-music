#!/usr/bin/env bash
# Builds the release APK from a clean clone of a tag, so it matches F-Droid's build byte for byte
# (apart from the signature). A build from the working copy can differ because of leftover
# incremental build state. Usage: ./release.sh [vX.Y.Z]  (defaults to the latest tag)
set -euo pipefail
root="$(cd "$(dirname "$0")" && pwd)"
tag="${1:-$(git -C "$root" describe --tags --abbrev=0)}"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

git clone -q "$root" "$work/src"
git -C "$work/src" -c advice.detachedHead=false checkout -q "$tag"
[ -f "$root/local.properties" ] && cp "$root/local.properties" "$work/src/"
(cd "$work/src" && ./gradlew -q assembleRelease)

mkdir -p "$root/dist"
out="$root/dist/noir-music-${tag#v}.apk"
cp "$work/src/app/build/outputs/apk/release/app-release.apk" "$out"
echo "$out"
sha256sum "$out" | cut -d' ' -f1
