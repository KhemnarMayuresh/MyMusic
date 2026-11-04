# AI handoff

Read this first when starting work on My Music with an AI coding assistant. This is a maintained project summary, not a transcript. It collects the user requirements and implementation decisions needed to continue work without relying on chat history.

## Current repository snapshot

- Project root: `P:\MyMusic`.
- App: native Android, Kotlin, Jetpack Compose, AndroidX Media3.
- Application ID and namespace: `my.music`; launcher name: **My Music**.
- Version: `1.0.0`, version code `2`; minimum API 24, target API 36, compile API 37.
- At the last handoff update (2026-11-04): `main` was at `e8f7410`, matching `origin/main`. Release tag `v1.0.0` points to the 1.0.0 release commit. Check Git before relying on these hashes or the branch state.
- The working tree was clean when this handoff was written. APKs, test media, local settings, and emulator captures are intentionally ignored by Git.
- No ads, purchases, accounts, analytics, or internet permission.

## What the user asked the product to be

My Music is a clean, useful local music player. Keep existing features functional; do not add inert controls or paid gates. Brand the app and package consistently as My Music / `my.music`.

### Library and playback

- Browse songs, playlists, folders, albums, and artists. Preserve the active tab and opened group when returning from the player.
- Folder, album, and artist screens need sorting, multi-selection, and actions such as play, play next, add to queue, add to playlist, and hide where applicable.
- The queue supports direct drag-and-drop reordering. Keep playback position attached to the active occurrence when queue entries move.
- Shuffle controls must reshuffle the active list or queue. Global Play starts in order; selecting an individual song or mix preserves the current shuffle setting.
- Maintain seeking, previous/next, repeat, speed, background playback, notification and lock-screen controls, and the lock-screen setting.
- Protect user files: use Android confirmation for operations that require it. Keep source-file edits distinct from app-local metadata and artwork.

### Player and library personalization

- Provide more than ten player themes as visual previews, with an Apply action; allow custom player themes and custom library palettes.
- Cassette shows cover art behind its reel window. Waveform is smooth. Neon animates the cover border. Minimal centers the cover art. Film is static. Retired Aurora, Prism, and Frost designs stay out of the picker.
- Derive the player background from cover colors; use the app default when cover art is absent.
- Retain the branded animated intro and keep the app name short enough for launchers.

### Widgets

- Provide Classic 4×1, Lite 4×1, and Vinyl 4×2 widgets.
- Keep the shared widget previews and launcher layouts visually consistent: purple/neon gradients, cover art or record, compact padding, clear playback actions, and a Vinyl progress display.
- Widget controls operate the app's playback service. Do not claim launcher compatibility that has not been tested.

## For you: required behavior

- Enabled by default and placed before Songs. Songs remains selected on a fresh launch. Settings can hide the tab and disable its feature-specific tracking.
- Disabling For you hides the tab and stops recommendation writes **while retaining existing recommendation data**. Re-enabling uses retained data. Only the separately confirmed cleanup action deletes those aggregates.
- Show Top picks (Your Hits, Top Album, Top Artist), Recently Played, Explore your library, and Month’s Top Chart.
- Explore order is **High Quality, Top Artist mix, Daily discover, Never Played, Before 1980, Before 2000**.
- Use only tracks in the visible library. Do not guess missing year or bitrate tags. High Quality means known lossless MIME type or bitrate at least 320,000 bits/second.
- Keep feature recommendation counts separate from playlist history. Store compact per-track and per-month aggregates, not every listening event or duplicate media. No automatic pruning. Counters are not currently included in JSON backup.
- See [recommendation behavior](RECOMMENDATIONS.md) for storage and ranking details.

## Architecture and source ownership

See [DESIGN.md](../DESIGN.md) for the fuller architecture. Key owners:

- `library/`: MediaStore discovery, grouping/filtering, and recommendation ranking.
- `collections/`: SQLite user collections, preferences, history, overrides, and recommendation aggregates.
- `playback/`: the foreground Media3 service, active player, queue, transitions, and audio effects.
- `ui/`: navigation, screens, settings, themes, and Compose widget previews.
- `widgets/`: RemoteViews layouts, service actions, artwork requests, and Vinyl timeline refresh.
- `lyrics/`, `files/`, `backup/`, `transfer/`: their named local features.

Preserve the existing ownership boundaries: MediaStore owns discovered media facts; repositories own saved app data; the playback service owns playback. Keep platform permissions and file-consent flows intact.

## Build and work safely

- Toolchain: JDK 25 is pinned in `gradle/gradle-daemon-jvm.properties`; Gradle 9.3.1 is pinned by the wrapper. Use the existing Android SDK; do not install unrelated tools.
- Configure the local SDK in ignored `local.properties`.
- Build: `.\gradlew.bat :app:assembleDebug`.
- Unit tests, lint, and build: `.\scripts\verify.ps1` or the commands in [testing](TESTING.md).
- Instrumentation requires a dedicated emulator with generated fixtures indexed. `verify.ps1 -Device` may uninstall the test app after its run; reinstall the debug APK separately.
- Runtime verification recorded so far uses an API 37 emulator. The emulator used for UI tests has no direct widget-pin support; its app screen shows the long-press-home fallback. Physical devices, older Android versions, audio capture, and other launchers need separate validation.
- See [verification](VERIFICATION.md) for the actual results, dates, APK identities, and coverage limits. Do not describe historical checks as freshly rerun.
- Inspect `git status` and existing diffs before editing. Preserve unrelated work. The user pushes manually; do not push or repoint `v1.0.0` without a later explicit instruction.

## Conversation history captured

The detailed asks that shaped the product are summarized in [project context](PROJECT_CONTEXT.md), including library menus, queue drag behavior, theme-specific requirements, brand/package identity, For you tracking semantics, widget design, and documentation preferences. That file is the durable record to update when the user makes a new product decision.
