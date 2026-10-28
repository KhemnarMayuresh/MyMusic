# My Music

A native Android player for local audio, implemented in Kotlin, Compose and Media3. Application ID: `my.music`. The original reference app is installed separately and is never replaced.

## Release

Final local release: **1.0.0**, Git tag `v1.0.0`, application ID `my.music`, Android version code `2`. See [release notes](RELEASE_NOTES.md) for validation and signing details. Build outputs and signing keys are excluded from Git.

## Build and install

Use an existing Android SDK with platform 37 and JDK 25 (pinned by `gradle/gradle-daemon-jvm.properties`; a matching Android Studio JBR works). Gradle 9.3.1 is pinned and its distribution checksum is verified by the wrapper. Dependencies are pinned in `app/build.gradle.kts`.

On Windows PowerShell:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
# Create local.properties once, using the actual existing SDK path:
'sdk.dir=C:/Users/mayur/AppData/Local/Android/Sdk' | Set-Content local.properties
.\gradlew.bat :app:assembleDebug
& 'C:\Users\mayur\AppData\Local\Android\Sdk\platform-tools\adb.exe' install -r app\build\outputs\apk\debug\app-debug.apk
```

The debug APK uses the local Android debug key. Release signing is intentionally left to the project owner; no signing credentials are stored in the project. No unrelated SDK, JDK or other applications are installed by these scripts.

## Features

- Songs, playlists, albums, artists and folders; per-group menus, persistent sorting, bulk selection/actions, scan filters and reversible hiding.
- Foreground MediaSession playback, compact/full player, drag-and-drop queue management with edge scrolling, shuffle/repeat, seek, ±10 seconds, speed and Android notification/lock-screen controls.
- Optional For you tab before Songs (Songs remains the launch default): Top picks, Recently Played, artist mix, Daily discover, Never Played, year-based mixes, High Quality and the current month's chart. Mixes use the visible local library and playable queues. The Moto-inspired dashboard uses horizontal Top picks and Explore cards, compact recent covers and a monthly recap; High Quality comes first in Explore.
- Settings can hide For you and stop its recommendation tracking without deleting saved counts. Clear recommendation data is a separate confirmed action. Counts and monthly aggregates stay on-device; existing playlist history is separate. Year/quality mixes use known metadata, with no guessing for missing tags. Recommendation counters are not included in the existing backup format.
- Ordered persistent playlists with duplicate entries, favorites, recently/most-played collections.
- Plain/LRC lyrics import and editing, timestamp seeking, local artwork and fifteen built-in player themes with shared visual previews, theme-specific linear/circular seeking layouts and Apply.
- Real PCM ten-band equalizer, five-band control mode, audio-effect controls, crossfade, gapless mode and sleep timer.
- Share, ringtone assignment, Android-confirmed deletion, app-local metadata editing and supported MP3 file-tag writing.
- JSON backup/restore of collections/preferences and a local ZIP music-package transfer flow with size/digest validation.
- Cover-derived colors and blurred player backgrounds, spinning vinyl/cassette reels, moving neon borders and static filmstrip artwork, and a smooth audio-energy waveform. Animation follows playback and pauses off screen. Shuffle survives song selection and crossfade transitions.
- Branded animated intro, eight library palettes, custom library palettes and saved custom player themes with layout, gradient/accent colors, visual color picker and live preview. Queue/player shuffle buttons generate a fresh visible order while preserving the playing occurrence and position.
- Classic, Lite and Vinyl home-screen playback widgets with real layout previews. No ads, purchases, premium locks, accounts, analytics or internet permission.

Grant audio-library access when prompted, or open an audio file with this app. Add audio through your device's normal file manager and use Scan music. File edits/deletion require Android consent where applicable. MP3 tag writes preserve an original-file backup under Backup & restore → Original file backups.

## Verification

```powershell
.\scripts\verify.ps1
# Requires a connected emulator/device with the generated fixtures indexed:
.\scripts\verify.ps1 -Device
```

Create original WAV/LRC fixtures with `analysis/scripts/New-AudioFixtures.ps1`. Device tests expect the generated `Dawn`, `Night`, and `Dawn reprise` WAVs under a dedicated music folder and indexed by MediaStore. The physical MP3-tag test copies an available MP3 to an app-owned temporary media entry, validates it, and deletes only the temporary copy. It is skipped when no MP3 fixture is available. The user's existing MP3 is read without modification. Tests remove their own temporary collections and transfer files. Gradle instrumentation uses a disposable app installation and removes it after the run; use a dedicated emulator and install the APK separately afterward. Source music files are preserved.

Latest validation and APK identity are recorded in the verification report. See [verification](analysis/VERIFICATION.md), [parity coverage](analysis/PARITY.md) and [reference findings](analysis/FINDINGS.md) for evidence and limitations. A build or screenshot alone does not establish functional parity.

## Compatibility and remaining reference gaps

The reference is installed version `4.4.1.189`; the store listing describes a newer feature set. The installed reference's recap is marked coming soon. Investigation was limited by intermittent emulator/system UI failures.

- Format playback depends on available decoders; no untested "all formats" guarantee is made.
- Source-file text-tag editing supports ordinary ID3v2.3/v2.4 MP3 files up to 128 MB, rejecting unsupported extended/unsynchronised tags before mutation. Other metadata/artwork changes are app-local. Original unknown frames and audio bytes are preserved; older ID3v1 trailers are retained unchanged.
- Transfer uses this clone's local package format. The original app's wireless protocol and proprietary backups are not implemented or claimed compatible.
- Equalizer preset curves, fade curves/durations, search normalization and some queue policies are implementation choices until compared against complete reference runtime cases. Unsupported Android effects are reported.
- Minimum Android version is 7.0/API24; current device verification uses an API37 x86_64 emulator. Older devices, physical headsets, widgets on different launchers, full audio-output capture, paired-device transfer and complete codec coverage need separate verification.
- UI copy is currently English. Cover changes and lyrics are stored inside the player. No external lyrics service or music downloader is included.

The approved design and execution plan remain in `DESIGN.md` and `docs/superpowers/plans/`.

UI corrections and reference comparison: [2026-10-19 revision notes](analysis/REVISIONS-2026-10-19.md).

Icon and seeking corrections: [2026-10-19 theme progress notes](analysis/THEME-PROGRESS-2026-10-19.md).

Shuffle and visual corrections: [2026-10-19 playback visual notes](analysis/SHUFFLE-VISUALS-2026-10-19.md).

Reshuffle, intro and custom themes: [2026-10-21 revision notes](analysis/RESHUFFLE-INTRO-THEMES-2026-10-21.md).
