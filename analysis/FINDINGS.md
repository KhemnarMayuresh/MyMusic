# Reference app investigation

Date: 2026-10-18. Target: `musicplayer.musicapps.music.mp3player`, installed on `emulator-5554`. ADB package output reports version `4.4.1.189` (versionCode 189), minSdk 24, targetSdk 35. This version, rather than a changing store listing, is the proposed behavior reference.

## Evidence and confidence

REA was used through the existing repository CLI with a process-scoped `REA_ADB_PATH` pointing to the installed Android SDK. The connected REA MCP device operation failed because its environment could not resolve adb. No client configuration was changed. UI interactions and hierarchy dumps used the existing adb directly; these are observations, not REA Evidence records. JSON files contain canonical REA records where available.

| Observation | Evidence | Scope |
|---|---|---|
| App launch | `evidence/launch.json`, ev_d53f0bd7837aced9b2c717e2a9c22d5f04f94b850d2e7ddba8c1100779e9fef1 | Launch only; not authenticated APK identity |
| Library with local track and mini-player | `evidence/recovered.json`, ev_83a6e378eec35f73b132e231f9abe83b3f8e1f7a8e000afda37fc14a07d238a6 | One screen frame |
| Full player | `evidence/now-playing.json`, ev_d780b073ba1820576b3e21899981839926c213b6b909aa903108bafac025fbf7; `evidence/now-playing.xml` | One frame/hierarchy; control presence |
| Actual playback | `evidence/playing-media-session.txt` | Android reports target package PLAYING, position 1818ms, speed 1.0; this does not prove audible output |
| Settings | `evidence/settings-screen.json`, `settings-fresh.xml`, `settings-bottom.xml` | Visible options, not correctness of audio effects |
| Equalizer | `evidence/eq-fresh.xml` | Visible controls only |
| Sleep timer | `evidence/timer.xml` | Visible choices only; timer expiry untested |
| Track actions and speed | `evidence/overflow.xml`, `evidence/speed.xml` | Visible choices only; no destructive action executed |
| Playing queue | `evidence/queue-screen.json`, ev_2b6acfc3512bf40d88d76d070f900d0c8d8f67b68d7b231b6c5c49f6054f63cd | One frame; queue hierarchy capture failed to reach idle |

Important artifact naming limitations: `equalizer.json` actually captures a background-play permission explanation that appeared over the player, not the equalizer. `settings.xml` is a stale drawer hierarchy after a failed UI dump. `menu.xml` and `drawer.xml` show an Android system ANR dialog. Use the fresh evidence named above, not filenames alone. `player.json` captures the library; `now-playing.json` captures the actual expanded player.

## Observed reference behavior and controls

- Library tabs: Songs, Playlists, Folders, Albums, Artists. Songs has count, sort and selection icons, Shuffle and Play buttons, song title, artist/folder subtitle, date and overflow. Current song is highlighted amber. Empty library offers Scan music and Music transfer.
- Persistent mini-player: artwork, scrolling title, artist, play/pause, queue access.
- Playing queue screen: current/total indicator (1/1), shuffle/repeat, selection and clear icons, per-track overflow and drag handle, mini-player. Reordering and clear semantics were not exercised.
- Full player: Song/Lyrics tabs, artwork, title, artist, favorite, add to playlist, equalizer, sleep timer, queue; seek bar with current/total time; backward/forward 10 seconds; shuffle, previous, play/pause, next, repeat. Observed track duration 3:48.
- Full-player menu: set as ringtone, change cover, edit tags, go to album, hide song, delete from device, speed, playback settings, keep screen on, settings.
- Speed: slider labeled 0.5X through 2.0X, presets 0.5X/1.0X/1.5X/2.0X.
- Equalizer: Custom, Normal, Rock, Dance, Bass Boost, Electronic, Flat, Classical visible among horizontally arranged presets; 5/10 Bands selector; observed low-frequency labels 31/62/125/250/500Hz; reverb selector, Bass Boost, Virtualizer. Complete preset list/ranges remain unknown.
- Sleep timer: custom hours/minutes/seconds controls, 15/30/45/60 minute presets, stop when current song ends, Cancel/Start.
- Settings: Scan music, Hidden music, Backup & restore, Music transfer; Simultaneous playback, Crossfade, Gapless playback, Pause on detach, Ensure continuous background play; Keep screen on, Lock screen playing, Notifications, Language; FAQ/Feedback. Reference screenshot shows pause-on-detach enabled and simultaneous/crossfade/gapless disabled.
- Drawer: library, settings, equalizer, sleep timer, music-stops-playing help, skin theme, widgets. Music Recap says "Coming soon, stay tuned!" in this installed version.
- Excluded by user: ads, remove-ads/premium/purchase flows. Drawer also promotes other apps; proposed clone omits those promotions.

## Still unresolved

Playlist creation/edit/order, search matching and sorting contracts, queue insertion/reorder semantics, repeat cycle and previous-button threshold, lyrics matching/edit/sync, scan filters and exclusions, backup format/restore conflicts, transfer protocol, widgets, tag/cover persistence, ringtone behavior, theme catalog, audio-focus policy, crossfade duration/curve and gapless interaction, exact codec coverage, service restoration, accessibility, and errors have not been fully tested. Presence of a control does not establish its implementation.

Only one real music track was present during this pass. Multi-track behavior needs controlled fixtures. No APK code was decompiled, no purchase flow was activated, and no original music file was edited or deleted. The emulator repeatedly showed system ANRs; one adb reboot recovered interaction, but intermittent hierarchy-capture failures persisted.

No claim of complete functional equivalence is justified by this pass. Next work must maintain an acceptance matrix linking each implemented feature to an original-app observation and a clone test.
