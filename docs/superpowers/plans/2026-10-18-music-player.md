# MusicPlayer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver an installable, clean Android music player with verified functional parity to the installed reference, explicitly recording every remaining gap.

**Architecture:** Kotlin/Compose UI observes one Media3 MediaSession service. MediaStore owns discovered media facts; local repositories own collections, preferences, and custom metadata. File operations require explicit Android consent, and original-app observations establish comparison contracts.

**Tech Stack:** Kotlin, Jetpack Compose, AndroidX Media3, MediaStore, Room, DataStore, Gradle; existing Android SDK and Android Studio JDK. Exact compatible versions are pinned after official-documentation and local-toolchain checks in Task 2.

**Spec:** `P:\MusicPlayer\DESIGN.md`; reference findings: `P:\MusicPlayer\analysis\FINDINGS.md`.

## Global Constraints

- Build a clean Android offline music player at `P:\MusicPlayer` matching the installed reference app version `4.4.1.189` on a functional level.
- Remove ads, in-app purchases, premium locks, and unrelated app promotions.
- Keep ordinary music features available without payment.
- Create original icons/default artwork and use a distinct application ID: `my.music`.
- Never write source music merely to update display state.
- Persist restart state without silently auto-playing after process restoration.
- Use the existing Android SDK and Android Studio JDK; do not install unrelated tools.
- No source changes to REA or QuietLauncher.
- Any unimplemented item stays documented as a gap; no inert feature buttons or settings that merely store a boolean.
- A paired-device transfer test is required before claiming transfer parity.
- Functional equality is a goal, not a claim made before tests.
- Use minSdk 24 to cover the installed reference's declared floor; validate current-platform restrictions independently. Keep build outputs, pulled APKs, fixtures and device captures out of Git.

## Review Focus

1. Permission loss or ejected media during playback: show unavailable state, retain collections, and avoid unintended file deletion (Tasks 2–4).
2. Duplicate playlist entries and duplicate filenames: preserve media and queue-entry identities independently (Tasks 1, 3, 4).
3. Corrupt imports or restore interruption: reject invalid input without replacing valid saved data (Task 9).
4. Crossfade combined with repeat, speed changes or focus loss: preserve one coherent session state and release both players/effects (Task 7).
5. Android API/device differences in codec, audio effects and background execution: disclose unsupported behavior and prove supported behavior with platform tests (Tasks 3, 7, 8, 10).

## File Structure and Shared Interfaces

All paths below are relative to `P:\MusicPlayer`.

- `analysis/CONTRACTS.md`, `analysis/PARITY.md`: reference contracts and feature-by-feature evidence/test status.
- `analysis/scripts/`: fixture generation and bounded ADB observation scripts; outputs under ignored `analysis/fixtures/` and `analysis/evidence/`.
- Root Gradle wrapper/configuration, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`: independent Android build and platform declarations.
- `app/src/main/java/my/music/model/Media.kt`: `TrackId(uri: String)`, `Track(id: TrackId, title: String, artist: String, album: String, albumId: Long?, folder: String, durationMs: Long, mimeType: String?, dateAddedSeconds: Long, artworkUri: String?)`, `QueueEntry(entryId: String, trackId: TrackId)`.
- `library/`: permission-aware MediaStore repository and search/sort/group queries.
- `playback/`: service/controller, durable queue, timer and effects integration.
- `collections/`: Room entities/DAO/repository for ordered playlists, favorites, history and overrides.
- `settings/`: typed DataStore preferences; settings UI resides with its owning feature.
- `ui/`: navigation, library, compact/full player, queue and reusable accessible controls.
- `lyrics/`, `files/`, `backup/`, `transfer/`, `widgets/`: feature-specific storage, platform integrations and UI.
- `app/src/test/java/my/music/`: deterministic JVM tests for pure semantics.
- `app/src/androidTest/java/my/music/`: real Android workflow tests and permission/media-service tests.
- `scripts/verify.ps1`, `README.md`: reproducible verification/build commands and delivery instructions.

Domain results use `OperationResult<T>` with `Success(value)` and `Failure(reason)`; named failures distinguish permission required, unavailable media, unsupported format, malformed input, cancelled consent, and I/O failure. Never collapse unknown/unavailable into an empty successful library. Kotlin suspending APIs below are called from structured application scopes, never blocking the UI thread.

## Stage A: Reference Contracts and Working Player

### Task 1: Establish repeatable reference behavior contracts

**Files:** Create `analysis/scripts/New-AudioFixtures.ps1`, `analysis/scripts/Capture-Reference.ps1`, `analysis/CONTRACTS.md`, `analysis/PARITY.md`; update `analysis/FINDINGS.md`.

**Interfaces:** Produces numbered cases `REF-*` with setup, exact actions, observed result, evidence path/digest, environment, and coverage limitation. These identifiers are the acceptance-test contract for all later tasks.

- [ ] Generate original PCM audio fixtures with distinct tones, durations and deterministic identities; add tagged compressed fixtures through an existing encoder or a documented local build dependency. Include multiple folders/artists/albums, duplicate titles, absent tags/artwork, malformed audio and LRC files. Preserve the user's existing song unchanged.
- [ ] Add a fixture integrity check: a second generation yields identical hashes, distinct tracks retain distinct IDs, and malformed input is intentionally identified. Verify fixtures before pushing them into a dedicated device folder.
- [ ] Observe library scan/filter/search/sort/grouping, playlist create/rename/add/remove/order/duplicates, queue play-next/append/remove/reorder/clear, repeat cycle, shuffle and previous-button behavior. Capture each stable state; failed dumps cannot supply current observations.
- [ ] Observe lyrics edit/import/sync, scan exclusions, tag/cover edits and delete cancellation on fixture copies, ringtone, timer expiry/end-of-track, speed, focus/headphone behavior, crossfade/gapless interaction, and process restoration. Record visible ranges/defaults/preset lists.
- [ ] Inspect backup/export and transfer flows without sharing user media externally. Establish their observable user contract; if a paired device or audio capture is unavailable, mark that specific coverage unresolved.
- [ ] Fill `PARITY.md` with every design feature, its `REF-*` cases, implementation owner, clone test and status. Distinguish runtime evidence from UI presence and inferred behavior. Review against the original design scope; no silent exclusions.

### Task 2: Build the native library and permission flow

**Files:** Create root/app Gradle configuration, wrapper, manifest, `MainActivity.kt`, `model/Media.kt`, `model/OperationResult.kt`, `library/MediaLibrary.kt`, `library/LibraryScreen.kt`, `settings/Preferences.kt`, `ui/Theme.kt`; tests `library/MediaLibraryTest.kt` and `library/LibraryPermissionTest.kt`.

**Interfaces:** Produces `MediaLibrary.observeTracks(): Flow<LibraryState>` and `suspend MediaLibrary.rescan(): OperationResult<List<Track>>`; `LibraryState` explicitly represents permission-required, loading, ready and failed states. `TrackId` is a content URI, never a filename or display title.

- [ ] Verify official Android/Media3/Compose guidance and existing SDK/JDK/Gradle compatibility; pin compatible stable dependencies and compile/target SDK. Record choices in README. Create the independent wrapper and minimal build; declare only permissions used by implemented features.
- [ ] Write tests asserting permission denial produces `PermissionRequired`, duplicate titles produce distinct `TrackId`s, missing tags use defined display fallbacks without changing identity, and volume removal is unavailable rather than a successful empty scan. Run the focused tests and record the expected failures before implementation.
- [ ] Implement MediaStore queries on an I/O dispatcher, cursor cleanup, content observation, runtime permission requests by Android version and safe rescan cancellation. Create the library tabs, count, empty/loading/error states and reference-matched search/sort/grouping.
- [ ] Run JVM tests, device permission/revocation and scan tests, then `gradlew.bat :app:assembleDebug :app:lintDebug`. Install alongside the reference and compare fixture track counts/groups. Record cold/warm scan elapsed time; label unmeasured CPU/memory costs.

### Task 3: Deliver real background playback and shared controls

**Files:** Create `playback/PlaybackService.kt`, `playback/PlaybackConnection.kt`, `playback/QueueState.kt`, `playback/QueueStore.kt`, `ui/CompactPlayer.kt`, `ui/PlayerScreen.kt`; tests `playback/QueueStateTest.kt`, `playback/PlaybackServiceTest.kt`.

**Interfaces:** Consumes `Track` and `QueueEntry`; produces a Media3 `MediaController` through `PlaybackConnection` and `suspend PlaybackConnection.play(entries: List<QueueEntry>, startEntryId: String): OperationResult<Unit>`. Service owns the player/session and durable queue. Controller commands implement play/pause/seek/next/previous/shuffle/repeat/speed.

- [ ] Write queue tests: duplicate track references have distinct entry IDs, reordering retains the active entry, unavailable entries produce the reference-defined transition, and restored state remains paused. Write Android tests that a selected fixture advances position, the same state appears in both UI and media session, and background controls work. Confirm expected failures.
- [ ] Implement Media3 service/session, player lifecycle, notification channel, audio attributes/focus/noisy events, URI media items, queue persistence and asynchronous controller connection. Close owned resources on final destruction without stopping playback when a screen unbinds.
- [ ] Implement compact and full-player controls matching Task 1 contracts, ±10-second clamping, title/artist/artwork, Song/Lyrics navigation and speed. Error states identify unsupported/missing media and offer a working recovery action.
- [ ] Run queue and Android service tests; compare reference/clone play, seek, previous/next, repeat/shuffle and speed on the same fixtures. Verify notification/lock-screen controls, screen-off playback, activity recreation and process restoration. Capture the media-session state and limits of audible-output verification.

## Stage B: Collections and Music Workflows

### Task 4: Persist playlists/favorites/history and expose queue management

**Files:** Create `collections/CollectionDatabase.kt`, `collections/CollectionRepository.kt`, `collections/PlaylistScreen.kt`, `playback/QueueScreen.kt`; tests `collections/CollectionRepositoryTest.kt`, `collections/PlaylistWorkflowTest.kt`, `playback/QueueWorkflowTest.kt`.

**Interfaces:** `CollectionRepository.observePlaylists(): Flow<List<Playlist>>`, `observeFavorites(): Flow<Set<TrackId>>`, `suspend setFavorite(id: TrackId, favorite: Boolean): OperationResult<Unit>`, `suspend createPlaylist(name: String): OperationResult<PlaylistId>`, `suspend updatePlaylist(id: PlaylistId, name: String, entries: List<QueueEntry>): OperationResult<Unit>`. Missing media does not delete membership.

- [ ] Write failing transactional repository tests for rename/add/remove/reorder, duplicate membership according to `REF-*`, restart persistence and unavailable tracks. Assert playlist ordering uses entry identity, not title or track ID alone.
- [ ] Implement Room schema/transactions and shared favorites/history updates from the service. Add working playlist and queue screens, selection actions, play-next/append/remove/reorder/clear and current-entry navigation using the single playback controller.
- [ ] Run repository and device workflow tests, including cancel paths and process restart. Compare the original and clone for all collection/queue `REF-*` cases and update parity evidence.

### Task 5: Implement scan filters, hidden media and track actions

**Files:** Create `library/ScanSettings.kt`, `library/HiddenMusicScreen.kt`, `files/TrackActions.kt`, `files/MetadataEditor.kt`, `files/RingtoneWriter.kt`; tests `files/TrackActionsTest.kt`, `files/TrackConsentTest.kt`, `library/ScanFilterTest.kt`.

**Interfaces:** `suspend TrackActions.hide(ids: Set<TrackId>): OperationResult<Unit>`, `unhide(ids)`, `requestDelete(ids)`, `share(ids)`; `suspend MetadataEditor.save(id: TrackId, change: MetadataChange): OperationResult<Track>` and `suspend RingtoneWriter.set(id: TrackId): OperationResult<Unit>`. Consent launchers return typed completion/cancellation to the operation owner.

- [ ] Write failing tests pinning reference scan thresholds/exclusions, reversible hiding, delete cancellation preserving bytes, revoked permission, metadata edits on fixture copies and source identity preservation. Ensure a display override cannot be mistaken for a successful file-tag write.
- [ ] Implement filters at the library-owning boundary and persist hidden identities. Implement share/album navigation, Android delete/write consent and ringtone assignment. Select a maintained local tag writer after verifying format coverage; writes stage/validate before replacement where supported, and unsupported formats return a clear error.
- [ ] Implement working edit/cover/hidden/scan screens; distinguish app-local artwork/metadata from source-file writes according to observed reference semantics. Never modify the user's real reference song during testing.
- [ ] Run file/permission tests on fixture copies, verify resulting bytes/tags with an independent metadata reader, and confirm cancellation/denial leave source bytes unchanged. Rescan both apps and compare the supported actions.

### Task 6: Deliver lyrics and themes without network dependencies

**Files:** Create `lyrics/LrcParser.kt`, `lyrics/LyricsRepository.kt`, `lyrics/LyricsScreen.kt`, `ui/ThemePicker.kt`; tests `lyrics/LrcParserTest.kt`, `lyrics/LyricsWorkflowTest.kt`.

**Interfaces:** `LrcParser.parse(text: String): OperationResult<LyricsDocument>`, where documents preserve untimed text and timestamped lines; `LyricsRepository.observe(id: TrackId): Flow<LyricsDocument?>`, `suspend save(id: TrackId, document: LyricsDocument): OperationResult<Unit>`. Theme selection flows through typed preferences.

- [ ] Write failing tests for multiple timestamps, offsets, malformed lines, Unicode, unsynchronized text, seek-following and persistence. Pin import/edit behavior to reference observations rather than assuming embedded lyrics are always present.
- [ ] Implement bounded LRC parsing, local import/edit/association and seek-synchronized display. Implement original artwork fallbacks and functional library/player theme selection; all themes remain freely accessible.
- [ ] Run parser and device import/edit/seek tests; compare reference lyrics behavior and representative layout states. Check font scaling, contrast, touch targets, content descriptions and TalkBack navigation.

## Stage C: Audio, Platform, Backup and Delivery

### Task 7: Implement real audio effects, transitions and sleep timer

**Files:** Create `playback/AudioEffects.kt`, `playback/TransitionCoordinator.kt`, `playback/SleepTimer.kt`, `playback/AudioSettingsScreen.kt`, `playback/EqualizerScreen.kt`; tests `playback/SleepTimerTest.kt`, `playback/TransitionCoordinatorTest.kt`, `playback/AudioEffectsDeviceTest.kt`.

**Interfaces:** `suspend SleepTimer.start(durationMs: Long, finishCurrentTrack: Boolean): OperationResult<Unit>`, `cancel()`, `observe(): StateFlow<TimerState>`; `AudioEffects.attach(audioSessionId: Int): OperationResult<EffectCapabilities>` and `release()`. `TransitionCoordinator` is service-owned and is the only owner of a second player during crossfade.

- [ ] Write failing monotonic-clock tests for expiry, cancel/restart, end-of-current-track and service recreation. Write transition tests for repeat-one/all, skip during fade, focus loss, speed changes and cleanup of both players. Add device tests that effect parameters apply to the active audio session, not merely preferences.
- [ ] Implement timer state transitions and controls matching reference presets/custom values. Implement 5/10-band equalizer, presets, reverb, bass/virtualizer using reported device capabilities; show unsupported states when actual bands/effects cannot match the reference.
- [ ] Implement the reference-defined crossfade duration/curve and gapless interaction, simultaneous-playback focus policy and pause-on-detach. Keep the MediaSession timeline/position/metadata coherent while two decoders overlap; restore requested gain and release abandoned effects/players.
- [ ] Run deterministic tests, device audio-effect tests and captured-output comparisons where available. Exercise two distinct tracks at supported speeds; measure transition timing, verify timer expiry and record device-specific unsupported codecs/effects explicitly.

### Task 8: Complete settings, widgets and external media integration

**Files:** Create `settings/SettingsScreen.kt`, `widgets/PlayerWidgetProvider.kt`, `widgets/WidgetActions.kt`, widget layouts/provider metadata and localized strings; tests `widgets/WidgetWorkflowTest.kt`, `playback/ExternalControlsTest.kt`.

**Interfaces:** Widgets issue explicit immutable PendingIntents to the same playback/session command path. External `ACTION_VIEW` audio URIs resolve to `Track` or typed failure without losing the caller's URI permission semantics.

- [ ] Write failing device tests for widget play/pause/next state synchronization, controller reconnection, denied notification permission, revoked external URI grants and screen-on preference limited to the player screen.
- [ ] Implement widget sizes/layouts and controls from reference observation, external audio opening, lock-screen preference, keep-screen-on, language selection and actionable background-play guidance. Implement notifications settings only for meaningful supported notifications.
- [ ] Run device workflows across available API levels. Verify content URI grants, foreground-service startup restrictions, audio focus changes, headphone events and user stop actions. Mark physical-headset or unavailable-version testing unverified rather than simulated as real hardware.

### Task 9: Implement atomic backup/restore and device transfer

**Files:** Create `backup/BackupCodec.kt`, `backup/BackupRepository.kt`, `backup/BackupScreen.kt`, `transfer/TransferSession.kt`, `transfer/TransferScreen.kt`; tests `backup/BackupCodecTest.kt`, `backup/RestoreTransactionTest.kt`, `transfer/TransferWorkflowTest.kt`.

**Interfaces:** `suspend BackupRepository.export(destination: Uri): OperationResult<Unit>`, `preview(source: Uri): OperationResult<RestorePreview>`, `restore(preview: RestorePreview): OperationResult<Unit>`; `TransferSession` exposes `StateFlow<TransferState>` and `cancel()`. Preview identifies version, counts and missing-media mappings without mutation.

- [ ] Write failing tests for round-trip collections/preferences, unsupported versions, oversized/malformed/truncated input, path traversal, identity collisions, cancellation and transaction rollback. Assert invalid input never replaces valid state and reference music remains unchanged.
- [ ] Implement a versioned validated export and atomic restore through Android document pickers, following reference conflict behavior. If the original backup format is unsupported, report clone-format compatibility explicitly; never imply original backups work without a test.
- [ ] Implement the observed transfer workflow over an explicit paired local session with bounded streaming, consent, progress/cancel, verified file digests, safe destination names and retry cleanup. Internet permission is permitted only if the local transfer transport needs it; no advertising/analytics endpoints.
- [ ] Run a paired-device transfer including disconnect/retry and duplicate filenames, verify digests and playlist associations, and verify backup recovery after forced interruption. If a second device is unavailable, retain this task as unverified and report the delivery limitation.

### Task 10: Verify parity and package the deliverable

**Files:** Create `scripts/verify.ps1`, `README.md`; update `analysis/PARITY.md` and `analysis/FINDINGS.md`.

**Interfaces:** Verification script runs the pinned wrapper with the existing JDK/SDK and reports executed tests, outputs and failures; parity report maps each `REF-*` to exact clone evidence and remaining gaps.

- [ ] Run JVM tests, Android integration/UI tests, assemble and lint with the project wrapper. Record actual task output and device/API identity. Test fixtures include corrupt audio, missing tags, duplicate entries, absent/ejected storage and revoked permissions.
- [ ] Compare original/clone workflows end to end with identical fixture inputs. Validate codec claims through real decoding. Capture screenshots plus service state; use actual audio capture to substantiate audible-output/effect claims or leave those claims limited.
- [ ] Inspect merged manifest, dependency graph and packaged APK for billing/ads/analytics components. Confirm original media files were not changed and project/repo boundaries were respected.
- [ ] Measure comparable cold/warm library and playback-start tasks: elapsed time, process CPU and peak memory where tools permit; label estimates and unmeasured costs. Do not substitute output byte size for resource measurements.
- [ ] Review the whole diff against the approved design, fix actionable findings, and rerun affected checks. Use a fresh reviewer if the chosen execution workflow requires it.
- [ ] Deliver the source project and debug/installable APK, SHA-256, build/install instructions, executed validation and explicit parity gaps. Do not label the app an exact functional clone while unresolved required behavior remains.

## Plan Self-Review

The ten tasks cover every approved component and feature; Task 1 owns unresolved reference semantics, and later tasks may not invent equivalence. Collection/queue identities, permission failures, file consent, audio resource ownership and restore atomicity have explicit owners and tests. Hardware, paired-device, decoder and audio-capture gaps remain limits on verification rather than reasons to hide scope. This plan does not authorize purchase flows, deleting the user's real music, modifying REA, or changing client configuration.

## Execution Review

This plan is ready for user review. Recommended execution: native execution in this chat, because the service/controller, library identities and collections share interfaces that benefit from one implementer maintaining continuity. Subagent-driven execution is also available if explicitly chosen by the user. Product coding begins after the user confirms the plan and execution method.
