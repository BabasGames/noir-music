# Noir Music

An always-on, pure black & white music controller for Android, built for AMOLED screens.

Noir Music doesn't play music itself: it shows and controls whatever is playing in your other apps (Deezer, Spotify, YouTube Music, KDE Connect…) on a minimal full-screen display, meant to stay on next to your desk or in the car.

## Features

- **Controls** — play/pause, previous/next, seek, volume, and the app's own ♥ button when it exposes one. Swipe left/right to change track.
- **Synced lyrics** — swipe up to show lyrics that follow the music, from [LRCLIB](https://lrclib.net) with NetEase as a fallback. Tap the source under the lyrics to try the other one. Downloaded lyrics are kept for offline use.
- **Car mode** — huge clock, big buttons at the bottom, no lyrics to keep your eyes on the road.
- **Made to stay on** — keeps the screen awake while music plays, reduced refresh rate on the player, periodic pixel shifting against burn-in, night mode (always, on a schedule or in the dark), standby screen when nothing plays, sleep timer.
- **Clock** — any font, including the Nothing dot-matrix fonts when present on the phone, 12/24 h, localized date, and it fits next to the camera hole (detected automatically or set by hand).
- **Source picker** — switch between the apps that have a media session, or pin a preferred one.
- **14 languages** — English, French, Spanish, German, Italian, Portuguese, Dutch, Polish, Turkish, Russian, Ukrainian, Japanese, Chinese, Korean.

## Permissions

| Permission | Why |
|---|---|
| Notification access | The only way Android lets an app see and control other apps' media sessions. Noir Music never reads your notifications. |
| Internet, network state | Fetching lyrics, and knowing whether you are on mobile data (lyrics downloads can be limited to Wi-Fi). |
| Query all packages | Showing the name of the playing app and listing music apps for the "preferred app" setting. |

No account, no analytics, no ads. The only network requests are lyrics lookups (title, artist and duration of the current track) sent to LRCLIB and NetEase.

## Building

Requirements: JDK 17 or newer and the Android SDK (platform 35).

```sh
./gradlew assembleRelease
```

The APK ends up in `app/build/outputs/apk/release/`. It is signed with the debug key, which is fine for sideloading.

`install.sh [IP:PORT]` builds, installs over ADB (optionally connecting to a wireless-debugging address first), grants notification access and starts the app.

## Lyrics disclaimer

Lyrics come from community-maintained databases. They may be out of sync or not match the song, and Noir Music has no control over their content. NetEase's API is unofficial and may stop working at any time.

## License

Copyright © 2026 BabasGames

Noir Music is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. Any distributed copy or derivative must keep this notice, credit the original author, and be released under the same license with its source code.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See [LICENSE](LICENSE) for the full text.
