# MusicPlayer: proposed implementation design

Status: approved by the user on 2026-10-18; implementation plan pending review. No product implementation yet.

## Outcome

Build a clean Android offline music player at `P:\MusicPlayer` matching the installed reference app version `4.4.1.189` on a functional level. Remove ads, in-app purchases, premium locks, and unrelated app promotions. Keep ordinary music features available without payment. The observed navy/purple library and artwork-driven full-player layout guide navigation and visual hierarchy; create original icons/default artwork and use a distinct application ID.

The investigation in `analysis/FINDINGS.md` is partial. Unverified behaviors remain explicit investigation tasks, not assumed exact matches. The Play Store listing is supporting context and cannot establish installed-version behavior.

## Approaches considered

1. **Recommended: Kotlin, Jetpack Compose, AndroidX Media3.** Native MediaStore access, foreground MediaSession service, notifications, audio focus, external media controls and Android permission handling fit the reference. Media3 supplies a playback foundation; crossfade and device audio effects require explicit integration and tests.
2. **Kotlin with platform MediaPlayer and Views.** Smaller dependency surface, but considerably more custom playback queue, buffering, session and lifecycle work; harder to verify equivalent gapless behavior.
3. **Flutter or React Native.** Useful for multiple platforms, but this request targets Android, and media service, widgets, tags and audio effects would still need native bridges. Added integration cost without a requested benefit.

## Components and ownership

- `library`: MediaStore queries and permission-aware scanning; stable content-URI identities, metadata, albums, artists, folders, search/sort/filter and hidden entries. Permission denial and unavailable/removable media are explicit states. Never treat a vanished URI as a request to delete user files.
- `playback`: one foreground MediaSession service owns players, queue, position, shuffle/repeat, speed, audio focus, noisy/headphone events, sleep timer and audio effects. Screens, notification actions and widgets use the same controller. Persist restart state without silently auto-playing after process restoration.
- `collections`: persistent playlists, favorites, listening history and custom metadata/artwork. Media identity and presentation labels remain separate. Reordering and duplicate entries require observation before finalizing semantics.
- `presentation`: Compose library tabs, drawer, search, selection/action menus, compact player, full Song/Lyrics player, queue sheet, settings, equalizer and timer. A shared state model prevents screens from displaying contradictory playback state.
- `files`: explicit user-approved tag/cover writes, ringtone assignment, sharing, export/import, delete requests and transfer. Use Android consent APIs and document pickers as appropriate. Hide is reversible and separate from deletion. Never write source music merely to update display state.
- `platform`: notification/media buttons/lock screen, widgets, URI opening, permission and battery-related guidance, localization and accessibility.

Use local persistence for user collections and preferences. Choose and pin supported library versions when implementation begins after checking official documentation and the existing toolchain. Use the existing Android SDK and Android Studio JDK; do not install unrelated tools. The project gets its own Gradle wrapper and package identity; no source changes to REA or QuietLauncher.

## Feature scope

Deliver the observed library and playback controls, playlists/favorites, queue management, search/sort, lyrics, themes, speed, equalizer/effects, timer, scan/hidden music, supported track actions, background/lock-screen controls, widgets, backup/restore and transfer. Reference-only "Coming soon" recap is not treated as a working feature. Any unimplemented item stays documented as a gap; no inert feature buttons or settings that merely store a boolean.

Codec support must be reported from actual decode tests. The store's "all formats" wording is not a guarantee: formats such as APE/MIDI need explicit investigation and potentially additional decoder work. Crossfade and gapless must operate on real audio output and have a defined interaction based on reference tests. A paired-device transfer test is required before claiming transfer parity.

## Sequencing and verification

1. Finish behavior contracts with deterministic, original audio fixtures: multiple artists/albums/folders, duplicates, missing tags/artwork, short/long tracks, malformed files, LRC lyrics. Observe the original first, preserving results and source media.
2. Scaffold independent Android project and build a working playback/service/library vertical slice. Install alongside the reference app.
3. Add collection, queue, search, scan and file workflows; test persistence and cancellation/consent paths.
4. Add lyrics, audio settings/effects, timer, theme and platform integration. Complete backup/transfer/widget workflows.
5. Compare original and clone with the same fixture library: track selection, next/previous, seek, repeat/shuffle, speed, queue changes, screen-off/background, media buttons, focus loss, headset detachment, timer expiry, process recreation and permission revocation. Verify crossfade/gapless and effects with captured audio where available.
6. Run unit tests for queue/timer/persistence semantics and Android integration/UI tests for the public workflows. Build/lint the release candidate, inspect requested permissions and dependencies for advertising/billing components, and deliver source plus installable APK with a measured parity/gap report.

Functional equality is a goal, not a claim made before tests. Each behavior is recorded as observed, inferred, implemented, verified, or unresolved. Compare equivalent completed tasks for media scan responsiveness and resource use; disclose unmeasured costs. Screenshots alone are insufficient playback validation.

## Review decision

The user approved the native Android approach and this feature scope on 2026-10-18. Next: review the implementation plan and choose its execution method before product coding.
