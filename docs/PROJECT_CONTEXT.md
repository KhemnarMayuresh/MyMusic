# Project context and decisions

This document preserves the decisions and requirements that were shared across the My Music project conversations. It is intentionally concise and avoids storing private chat text or unrelated investigation details. The most recent explicit user decision takes precedence over older wording.

## Product direction

- Build a clean native Android music player for local audio.
- Keep ordinary music features available without ads, purchases, premium locks, accounts, analytics, or network access.
- Use **My Music** as the product name and `my.music` for the application ID, namespace, and Kotlin package root.
- Keep launcher naming short. The app version requested for the initial release is **1.0.0**; version code is `2`.
- Keep the user’s original media intact unless an explicit in-app operation and Android consent authorize a file change.

## Requirements captured from the conversations

### Library, queue, and playback

- Library sections: Songs, Playlists, Folders, Albums, Artists, and the optional For you section.
- Folder, album, and artist views need sorting, multi-selection, and relevant group actions. Group menus include play, play next, add to queue, add to playlist, and hide when meaningful.
- The playing queue must support direct drag-and-drop. One-step up/down arrow movement is insufficient. Preserve active-track identity and seek position while reordering.
- Shuffle in queue and player options must create a new queue order. Global Play is ordered playback; selecting a specific song or mix retains the current shuffle preference.
- Remember the current library tab and group when opening and returning from the player.
- Keep screen-off/background playback, lock-screen and notification controls, and a setting for lock-screen playback.

### Themes and launch experience

- Player themes should be easy to compare visually, show a useful preview, and require Apply to change the active theme. The catalog has more than ten designs and supports user-created themes.
- Preserve explicit theme decisions: cover art behind the cassette reel; smooth, music-responsive waveform; animated neon around the cover; centered cover for Minimal; static Film; remove Aurora, Prism, and Frost from the catalog.
- Use cover-derived color and a blurred matching player background, with default colors when no artwork is available.
- Library themes need their own custom creation flow, preview, and color controls. Remove placeholder marketing copy from that preview.
- Use a branded animated opening screen and align app branding in the drawer and launcher.

### Widgets

- At least three useful widgets: Classic 4×1, Lite 4×1, and Vinyl 4×2.
- The user supplied a widget-selection design. Match its card proportions, padding, purple/neon gradient style, cover artwork, gradient play action, and compact controls. Vinyl includes cover/record art, metadata, progress, and playback controls.
- The widget preview and launcher implementation should share the actual RemoteViews layout and controls. Progress and playback state should come from the active service rather than demo values.

### For you and retained data

- Add For you before Songs, enabled by default; Songs remains the default selected tab.
- Settings can disable For you. Disabling must hide the section and stop feature-specific tracking, while retaining saved recommendation aggregates until the user explicitly requests cleanup.
- Cleanup is a separate, confirmed action. Re-enabling resumes from retained records. Do not prune data automatically.
- Sections: Top picks (Your Hits, Top Album, Top Artist), Recently Played, Explore your library, and Month’s Top Chart.
- Explore order: High Quality first, then Top Artist mix, Daily discover, Never Played, Before 1980, Before 2000.
- Recommendations use the visible library only. Do not infer missing metadata. High Quality uses known lossless format tags or bitrate of at least 320 kbps.
- Keep recommendation counters separate from existing playlist history. Use compact per-track/per-month aggregates, not an event log or media copies. Recommendation aggregates are not currently in JSON backups.
- The dashboard should use compact Continue/Shuffle/Play actions, horizontal mix cards, compact recent artwork, and the monthly chart treatment. Do not add a redundant in-page For you heading.

## Implementation notes that affect future changes

- Kotlin, Jetpack Compose, AndroidX Media3; application package `my.music`.
- Minimum API 24, target API 36, compile API 37. JDK 25 and Gradle 9.3.1.
- SQLite version 2 includes recommendation aggregate tables and additively migrates version 1 data.
- MediaStore owns discovered file facts. `collections/` owns user state. `playback/` owns the active player and queue. Avoid duplicating interpretation or state in UI code.
- Queue entries have occurrence identity so duplicates can be reordered and removed independently.
- Album/artist mixes derive from existing recommendation counts and currently visible media; they add no new tracking table.
- Widgets use the app’s playback service. A bound home-screen widget and live timeline update were not verified on the emulator used for the last widget check.

## Current status as of 2026-11-04

- `main` is at `e8f7410` and matched `origin/main` when checked; latest release tag is `v1.0.0`.
- The widget visual refresh is on `main` after the release tag. It keeps app version `1.0.0`, code `2`.
- Last recorded verification: 37 unit-test report results, all 30 emulator workflows passed in the final full run at release preparation, and the widget-specific RemoteViews geometry check passed afterward. See [verification](VERIFICATION.md); these are recorded results, not claims that a full suite was rerun today.
- Widget selection previews were checked on an API 37 emulator. Direct widget pinning was unsupported by its launcher and the app displayed the long-press-home instructions.
- Runtime coverage on API 24–36, physical audio devices, external headset behavior, actual widget pinning across launchers, audio output capture, and paired-device transfer remain unverified.
- Distribution signing is owner-controlled. Debug APKs and unsigned release builds are generated artifacts and are not committed.

## When a new decision arrives

Update this file and `docs/AI_HANDOFF.md` when a user decision changes product behavior, data retention, naming, package/version, or the contribution workflow. Update the owning feature doc for implementation detail. Keep test results in [verification](VERIFICATION.md) and distinguish source changes, build results, and device observations.
