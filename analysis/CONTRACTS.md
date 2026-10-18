# Reference contracts

Reference: musicplayer.musicapps.music.mp3player 4.4.1.189, emulator-5554. Initial findings and canonical screenshot Evidence are in FINDINGS.md. Each contract below remains limited to its recorded evidence; implementation choices are not reference observations.

| ID | Behavior | Reference evidence | Verification status |
|---|---|---|---|
| REF-01 | Songs/Playlists/Folders/Albums/Artists, search and library controls | recovered screen and drawer2.xml | Screen observed; multi-track behavior pending |
| REF-02 | Full and compact player share current track | now-playing screen/XML and recovered screen | Observed |
| REF-03 | Playback advances in a media session | playing-media-session.txt | PLAYING at position 1818ms, speed 1.0 |
| REF-04 | Seek ±10sec, shuffle, repeat, previous/next and queue | now-playing and queue-screen screenshots | Controls observed; transitions pending |
| REF-05 | Favorites and add-to-playlist | now-playing screenshot | Controls observed; persistence pending |
| REF-06 | Sleep custom duration, 15/30/45/60min, finish-current-song | timer.xml | Controls observed; expiry pending |
| REF-07 | Equalizer 5/10 bands, presets, bass, virtualizer, reverb | eq-fresh.xml | Controls observed; audio effects pending |
| REF-08 | Speed 0.5–2.0x | speed.xml | Controls observed; timed comparison pending |
| REF-09 | Scan, hide, backup, transfer and playback preferences | settings-fresh.xml/settings-bottom.xml | Controls observed; contracts pending |
| REF-10 | Tags, cover, ringtone, album, hide and delete actions | overflow.xml | Controls observed; fixture-only execution pending |

Original app mutations are restricted to generated fixtures. User music is preserved. New implementation uses URI identity and unique queue-entry identity; duplicate/repeat policies remain implementation decisions until runtime comparison.
