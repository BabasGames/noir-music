# Noir Music: full feature list

Everything the app does, grouped by area. For a quick overview, see the [README](README.md).

## Playback and sources

- Works with any app that publishes a media session: Deezer, Spotify, YouTube Music, VLC, podcast apps, Bluetooth and casting bridges, and so on.
- Shows the title, artist, album art, the name of the playing app, elapsed and total time.
- Play / pause with the big button or by tapping the title.
- Previous / next with the buttons or by swiping left / right (the screen follows your finger while you swipe).
- Seek by dragging the progress bar.
- Volume slider with − / + buttons, kept in sync with the phone's hardware volume keys.
- ♥ / like button when the playing app exposes one, using the app's own icon so it shows the real state (liked or not). Also on double tap.
- The position keeps moving smoothly between the updates sent by the playing app.
- **Source picker**: tap the app name to see every app with a media session (playing or paused) and switch between them, or open the playing app.
- **Preferred app**: pin one app so it stays on screen even when paused, instead of jumping to whatever plays next.
- Long press anywhere empty to open the playing app.
- Double tap and long press can be turned off in the settings.

## Lyrics

- Synced lyrics that scroll with the music, with the current line highlighted, upcoming lines slightly brighter than past ones, and faded top and bottom edges.
- Two sources: [LRCLIB](https://lrclib.net) first, NetEase as a fallback.
- Picks the version whose duration matches the track (within 4 seconds), so the timing fits the version you are listening to.
- Retries without "(feat. …)", "- Remastered" and similar suffixes, and with only the first artist, when the exact title finds nothing.
- Tap the source name under the lyrics to try the other source when lyrics are out of sync or wrong; the choice is remembered for that track.
- Plain (unsynced) lyrics scroll on their own along with the song, and can also be scrolled by hand (automatic scrolling resumes 8 seconds later).
- Instrumental tracks are recognized as such.
- Lyrics are saved on the phone once downloaded, so they work offline afterwards.
- Optional download on mobile data (off by default).
- Clear messages when there is no network, when the sources don't answer (automatic retry every 30 seconds) or when nothing is found.
- Separate lyrics text size.
- Open with a swipe up or the lyrics button, close with a swipe down; stays open from one track to the next and across restarts.
- Album art and clock shrink and slide up with an animation to make room for the lyrics.

## Car mode

- One tap on the car button (top, next to the settings gear), or from the settings.
- Huge clock that fills the whole width.
- Big previous / play / next buttons at the bottom, within thumb reach.
- No lyrics, to keep your eyes on the road.
- Landscape version with album art, clock, title, progress and big buttons.
- Smooth animated transition in and out.

## Clock and information

- Clock size: hidden, medium or large.
- Time format: automatic (follows the phone), 24 h or 12 h.
- Clock font: system, serif, monospace, manufacturer fonts found on the phone, or any .ttf / .otf file you import, with a live preview in the settings.
- Date under the clock (short, long or numeric), translated and ordered for the selected language.
- Battery level, with a charging indicator.
- Tap the clock or the date for the sleep timer; its remaining time shows under the clock.

## Camera hole

- The clock and buttons sit next to the front camera hole instead of leaving an empty band at the top.
- The clock shrinks just enough to fit beside the hole, or moves below it if it would get too small.
- Camera position detected automatically, or set by hand (left, center, right) when the phone reports it wrong.
- Clock side: automatic (the side with more room), left or right.

## Sleep timer

- 15, 30, 45 or 60 minutes, or at the end of the current track.
- "End of track" also stops right away if the playing app moves to the next track on its own.
- Can be stopped at any time.

## Made for AMOLED and always-on use

- Pure black background and white (or gray) elements only.
- Full screen, system bars hidden.
- Keep the screen on: never, while music plays, or always.
- Reduced refresh rate while the player is shown (the tutorial and settings stay smooth).
- Periodic pixel shift against burn-in: every 30 seconds, 1 minute or 5 minutes, by ±5, ±10 or ±20 dp, with a smooth glide.
- Night mode (gray text and darkened album art): off, always, on a schedule, or automatically in the dark using the light sensor (with a short delay so it doesn't flicker).
- Night brightness, previewed live on the screen while you move the slider.
- Custom brightness, also previewed live.
- Standby when nothing plays, after 1, 5 or 15 minutes: a clock alone, exactly centered and shifting slightly over time, or a fully black screen. A touch or the music starting again wakes it up.
- Black & white album art option.

## Layout

- Portrait and landscape layouts, each designed separately.
- Show or hide the album art; when hidden, the clock and controls form one centered block.
- Swap album art and controls.
- Lock the orientation to portrait or landscape, or follow the phone.
- Hide the app name, the progress bar or the volume slider.
- Text size: small, normal, large or very large.

## Languages

- English, French, Spanish, German, Italian, Portuguese, Dutch, Polish, Turkish, Russian, Ukrainian, Japanese, Chinese and Korean.
- Follows the phone's language automatically, or can be chosen in the settings; switches instantly, without restarting.

## Getting started

- A short tutorial on first launch: language first, then notification access, gestures, lyrics, car mode, screen settings and how to report bugs.
- Can be replayed from the settings.
- The settings header stays visible while scrolling.

## Privacy

- No account, no analytics, no ads.
- The only network requests are lyrics lookups (title, artist and duration of the current track) sent to LRCLIB and NetEase.
- Notification access is only used to reach the media sessions; notifications themselves are never read.
