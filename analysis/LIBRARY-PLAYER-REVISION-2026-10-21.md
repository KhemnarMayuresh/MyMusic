# Library and player-theme revision — 2026-10-21

## Changes

- Launcher label, intro heading and drawer heading: My Music. Supplied icon preserved.
- Themes > Library: custom gradient/accent palettes with preview, visual color picker, atomic save/apply, edit, delete and persisted identity. Eight built-in palettes remain.
- Cassette uses cover artwork behind its black reel window. Minimal centers artwork. Neon rotates a sweep-color glow around the stationary cover; Film scrolls its filmstrip perforations. Animations freeze when playback pauses and stop off screen.
- Waveform draws two smooth cubic paths from the existing PCM energy history, with spatial smoothing and frame-rate-independent temporal interpolation. It measures energy, not frequency bands; decoder buffering can lead the audible output as documented in the earlier visual analysis.
- Aurora, Prism and Frost removed from the built-in picker and their unused rendering branches removed. Retired built-in selections resolve to Artwork. Retained layouts keep numeric IDs so custom presets do not silently change layout.
- Active library tab is owned by the app navigation scope, alongside the existing opened group. Player collapse/back restores that tab and group.

## Verification

- Regression confirmed before catalog removal: retired-theme test failed against the previous 18-theme catalog.
- Focused checks passed: 33 JVM tests; library create/reload/edit/delete and direct player-return workflows; playback/paused animation checks for Neon, Film and Waveform.
- A new test initially pressed Back immediately after observing the repository save, before the editor's UI transition completed. Waiting for the theme-list screen and using its Back button corrected the test synchronization; no product workaround was added. The Film screenshot assertion also needed to wait for pause delivery and visual settling, matching the existing Vinyl/Cassette test approach.
- Read-only independent review found no blocking issues; its navigation coverage gap was addressed with direct player-return tests for Playlists, Folders, Albums, Artists and an opened folder.
- Screenshots retained locally under analysis/evidence/library-revision. Synthetic cover colors and existing fixture music used for animation checks. Original audio files were not edited.

Final full-suite/lint results and APK identity are recorded in VERIFICATION.md.
