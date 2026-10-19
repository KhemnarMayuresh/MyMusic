# Shuffle, cover backgrounds and playback animation — 2026-10-19

## Observed failures and producing boundaries

The new device regression reproduced shuffle being cleared when selecting another song: PlaybackConnection.play defaulted its shuffle argument to false. An explicit shuffled start also used canonical index zero, which may be partway through Media3's shuffle traversal. TransitionCoordinator created a fresh random order for its replacement player during crossfade.

play now preserves the current shuffle flag unless explicitly provided. An explicit Shuffle action enables shuffle before replacing the playlist with resetPosition=true, allowing ExoPlayer to start at its first shuffled window. Crossfade copies the current timeline's complete shuffle traversal into the replacement player before preparation. Queue identity and displayed canonical ordering stay stable.

The first traversal probe incorrectly skipped synchronously against MediaController's temporary masking timeline; it was corrected to yield for service updates and use repeat OFF. The final test checks 20 unique occurrences and preservation after song selection.

## Artwork ownership and rendering

Artwork decoding is shared by thumbnails, full player and preview. One bounded 8 MiB cache owns sampled covers (maximum 600 pixels per side), a 40x40 blurred bitmap and its alpha-weighted colors. Decoding, palette generation and three box-blur passes run off the main thread. URI changes reset the composable's result; cancellation propagates. Invalid, missing or inaccessible covers retain the selected theme's default background and built-in artwork.

PlayerSurface applies a dark cover-derived overlay to the blurred bitmap and a lighter matching control accent. Preview and applied player use the same surface. PlayerViewModel.playingArtwork owns current cover selection: the latest app-local cover override, then the current MediaItem artwork. LibraryDetails owns override parsing for both library rows and playing artwork. This preserves cover edits while playing and prevents stale library album art from replacing an explicit queue cover. Full player, preview and system-bar background consume this resolved artwork.

## Playback animation and signal ownership

Vinyl rotates its complete disc and center artwork. Cassette has two independently drawn spoke reels rotating at the same speed. Both retain their angle while paused. A lifecycle-owned frame coroutine runs only while playing and the relevant theme is visible; default static themes do not start that coroutine.

Waveform bars use a rolling history of 43 RMS energy windows (30 windows per audio second), from the existing PCM16 equalizer processor after EQ, including its flat bypass. No microphone, capture service, new permission or network dependency is used. PCM bytes and buffer positions are preserved. The render-thread writer publishes bounded immutable snapshots. Service promotion selects the active player's signal; service destruction clears it. Flush/reset clears history, and stale signal expires after 250 ms. UI pause immediately shows a quiet waveform.

The visual is an audio-energy waveform rather than a frequency spectrum. Decoder-output telemetry precedes audible output by sink buffering; it is not sample-synchronized to speaker output. Buffering or resume can briefly show a quiet waveform before fresh PCM arrives. Physical-device latency, hardware decoders/offload and API24 execution remain unverified.

## Verification

Final command: `:app:testDebugUnitTest :app:connectedDebugAndroidTest :app:assembleDebug --no-parallel --max-workers=1`. BUILD SUCCESSFUL, 8m02s. All 28 JVM tests and 22 API37 emulator tests passed, with zero failures/errors/skips. Instrumentation report time: 455.732 seconds; the visual workflow took 75.231 seconds in this run. Final fresh-process lint/build passed in 2m46s: zero errors, 69 warnings.

The visual test checks red/blue cover backgrounds, vinyl/cassette movement and pause stability, real PCM levels, and live cover overrides. Initial image comparisons were invalidated because the Android notification prompt could cover the player; the corrected setup grants POST_NOTIFICATIONS before launching. Only the unobstructed run is counted. The permission-overlay and launcher captures are not animation evidence.

The original shuffle-reset assertion failed before the fix. Traversal against the real service visits all 20 unique occurrences with repeat OFF and then verifies that selecting a song preserves shuffle. Existing device regressions cover queue drag/autoscroll, folders and bulk actions, widgets, theme seeking, lyrics/timer feedback, crossfade ownership, playback restoration and file operations on owned fixtures.

JVM additions cover solid-color palette changes, alpha handling/default palette, valid/invalid override parsing, signal/silence, stereo antiphase, expiry/reset, and byte-identical PCM through the flat EQ path. The read-only review prompted correction of the traversal test and documentation of decoder/speaker timing limits; its literal-null parsing finding was corrected while preserving deliberately entered text named `null`.

APK: `P:/MusicPlayer/MusicPlayer.apk`, SHA-256 `DB52DDCD3D153F5C4D44FB6F61D416050AA7EF518364FE9E5BC59F5EB96FF48B`. Installed separately after instrumentation cleanup, with audio/notification permission granted in the dedicated emulator. Original source music was preserved. Captures are under ignored `analysis/evidence/shuffle-visuals/`; `cover-vinyl-blue.png` and `cover-cassette-blue.png` show the unobstructed player.
