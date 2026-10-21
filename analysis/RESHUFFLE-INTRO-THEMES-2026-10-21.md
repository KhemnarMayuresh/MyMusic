# Reshuffle, branded intro and custom themes — 2026-10-21

## Approved behavior

The user approved a fresh visible shuffle order from queue/player buttons, preserving the current song and position; a brief purple/gold intro matching the supplied logo; six additional player themes and a live custom-theme editor with saved presets.

## Queue action

The old buttons toggled Media3 shuffle mode, leaving the displayed canonical queue unchanged and switching shuffle off on the next tap. Both buttons and the preview now call the same ViewModel action and service-owned `queue.reshuffle` command. Only this app's controller is granted that command. The current MediaItem source moves to the top without being replaced; the other occurrences are shuffled and replaced together. Playback follows the visible order. If randomization returns the existing order, the remaining entries rotate once so a queue with at least two other songs visibly changes on every tap.

Existing media IDs, duplicate occurrences, position, pause/play state and queue persistence are retained. Pending crossfade is cancelled before mutation. A pending intentional between-song resume is preserved; explicit pause and audio suppression remain authoritative. With fewer than three songs, the UI explains that more songs are needed rather than implying a different upcoming order is possible.

The baseline regression received permission-denied (-4) for the unadvertised reshuffle action. Its first cold-emulator attempt crashed before the assertion and was not counted as reproduction. The corrected foreground setup produced the assertion failure. The new regression passes repeated service requests, taps the actual queue and full-player icons, and checks current occurrence/position while paused and during actual playback.

## Intro

The native window background now uses a purple gradient and the supplied logo; Android 12+ uses a matching branded system splash. Compose follows it with a 1.4-second animated gradient intro and brief fade, with a Start listening shortcut. Underlying library accessibility nodes and touches are blocked until the intro exits. Activity recreation retains completion state. No audio startup or library scan is artificially blocked. The original icon asset is unchanged.

## Themes and persistence

There are 18 built-in player themes (Prism, Frost, Duotone, Pulse, Film and Halo added) and eight library palettes. Shared library palette definitions now own both preview and applied colors. New player designs have distinct artwork treatments; Pulse reads existing PCM energy and Halo animates while playing.

Create theme reuses the full player preview. Users choose a layout, top/bottom gradient colors, accent, and whether cover colors take precedence. Color controls offer swatches, HSV sliders and exact hex entry. Presets can be saved/applied, edited, renamed and deleted. Stable custom IDs preserve selection across renames. Custom definitions live in existing preferences, so existing JSON backup/restore carries them automatically. The parser rejects malformed/unsupported entries and bounds persisted definitions to 50 presets. Custom colors can be chosen independently of album artwork.

A device check exposed the intermediate state between two separate preference writes. `CollectionRepository.setPreferences` now commits theme definition and applied selection in one SQLite transaction and emits one complete snapshot. Delete/reset uses the same atomic boundary. Single preference writes delegate to it. The editor preserves its draft on a failed save.

## Verification

Focused checks: 30 JVM tests passed; the real queue/player-icon workflow passed; the intro/editor workflow passed, including selecting a blue swatch, saving, editing without changing identity, activity recreation and deletion. Intro and saved-theme screenshots were captured in ignored `analysis/evidence/revision-20261021/`. The initial editor automation incorrectly pressed Back without an active keyboard and later omitted a wait for Edit fields; those harness failures were corrected and are not counted as product failures.

Read-only review covered command authority, queue/decoder continuity, intro lifecycle/resources, custom parsing and visual picker state. Its pending gap-resume finding was fixed. The two-song case is explicitly explained in the UI because one upcoming song has no alternate order. Final full suite/build passed in 5m43s: 30 JVM tests and 24 API37 emulator tests, zero failures/errors/skips. Instrumentation report time: 320.25 seconds. Fresh-process build/lint passed in 5m16s: zero errors, 69 warnings. Final trailing-blank-line cleanup changes no executable logic. Delivered APK SHA-256: `4666F5B3EF480C6970F329F1001D08F80EDAB51A300CD60748954BBCF9B9C6EF`, installed separately after instrumentation cleanup.

API24 execution, physical-device/OEM launch behavior and exact reference equivalence remain unverified. Source music is only read; temporary test metadata/collections are restored. No ads, purchases, new network access or microphone capture were added.
