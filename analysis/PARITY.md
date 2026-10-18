# Parity and implementation status

Reference: installed InShot Music Player `musicplayer.musicapps.music.mp3player` 4.4.1.189. Clone: `app.local.musicplayer` 0.1.0. The implementation covers the observed local-player feature set, without ads, purchases, accounts or promotions. Exact functional equivalence is **not established** where the reference behavior was not exercised. See CONTRACTS.md and FINDINGS.md for provenance.

| Feature | Reference | Implementation and clone verification |
|---|---|---|
| Library, permission, scan, search, grouping | REF-01/09 | MediaStore library, observer, duration/folder exclusions, accent-insensitive search, sort and tabs. Real fixture discovery and launcher navigation tested; search unit tested. |
| Playback, service, notification, queue | REF-02/03/04 | Media3 foreground session, full/compact player, seek, speed, shuffle/repeat, unique queue entries, reorder/remove and paused restoration. Real playback and crossfade tested. Full notification/lock-screen interaction and process-death restoration remain unverified. |
| Playlists, favorites, history | REF-05 | SQLite transactions, duplicate playlist occurrences, entry-based selection, favorites, recent/most lists. Collections and UI song-to-playlist action tested; transition counting and ranked browsing unit tested. Reference duplicate/history rules unknown. |
| Hide and file operations | REF-09/10 | Hide/unhide, share, ringtone, OS consent deletion; legacy write-permission policy unit tested. Physical MP3 tags verified on an owned copy with original bytes retained; user source unchanged. Ringtone/delete confirmation and older Android execution need separate verification. |
| Lyrics and themes | Player/settings | Plain/LRC import/edit, offset/timestamp seeking, local artwork, color/player themes. LRC parsing unit tested. Reference theme catalog, lyric matching and rendering equivalence unverified. |
| Equalizer and effects | REF-07 | PCM ten-band EQ, five-band mode, presets, optional Android effects with capability feedback. Flat output, measured signal gain, stereo isolation and Media3 empty-buffer regression tested. Audible quality, reference curves and hardware effects unverified. |
| Transitions and timer | REF-06/08/09 | Crossfade with service-owned second decoder, gapless preference, 0.5–2x speed, monotonic timer and same-boot deadline retention. Real overlap, edits during overlap, manual pause during delayed transition, timer expiry and deadline recreation tested. Reference timing/curves unknown. |
| Widgets/external controls | Drawer/settings | RemoteViews controls share the service queue; MediaSession handles trusted controllers. Launcher pinning, physical Bluetooth/headsets and all focus routes unverified. |
| Backup, restore, transfer | REF-09 | Bounded JSON transactional restore; clone ZIP music packages with SHA-256, staging, owned-output rollback and cancellation. Real import/export byte comparison and collections tested. Original proprietary backups and wireless protocol are not compatible. |

## Verification scope

Device coverage is one API37 Android17 x86_64 emulator. No real paired device, physical audio-output capture, exhaustive codec matrix or API24–36 runtime coverage was performed. Completion evidence records executed tests and lint separately from these limitations.

The UI is English. Physical tag editing supports ordinary ID3v2.3/v2.4 MP3 files up to 128 MB; unsupported extended/unsynchronised tags are rejected before mutation. Artwork and other-format metadata changes are local to the player. Backups support recovery after failed writes, but crash/power-loss recovery during physical file replacement has not been established.

An emulator launcher frame, stale UI hierarchy, successful build or simulated provider does not establish reference parity.
