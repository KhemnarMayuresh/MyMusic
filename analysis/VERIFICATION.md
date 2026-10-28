# Current APK — For you recommendations

2026-10-28: added enabled-by-default For you before Songs; Songs remains the fresh-launch default. Added Top picks, Recently Played, Top Artist mix, Daily discover, Never Played, Before 1980, Before 2000, High Quality and current-month chart. Mixes open existing playable song lists and individual cards start the corresponding queue. Hidden/filtered/unavailable songs are excluded by using the visible library.

Settings > For you hides the tab and stops feature-specific aggregate writes while retaining all saved data. Re-enabling restores the retained counters. Clear recommendation data is a separate confirmation; cancellation preserves records and confirmation deletes recommendation counters/monthly charts only. Existing playlist history stays separate. No automatic pruning. Additive SQLite v1->v2 migration preserves existing collections. Nullable year/bitrate are read from MediaStore; bitrate query is guarded for API30+. Detailed design and plan are in docs/superpowers.

Verification: 36 JVM tests passed, zero failures/errors/skips. All four new emulator cases passed in the full run: v1 migration, retention/disable/reload/cleanup, monthly buckets/concurrent disabled writes, and actual UI default tab/mix playback/switch/confirmed cleanup. Full API37 suite passed 29/30; the sole failure was a pre-existing static Film screenshot assertion whose old crop included the moving progress bar. After excluding the timeline using its actual bounds, the targeted visual rerun passed (52.319s). All 30 device cases therefore passed across these runs; there was not one fully green 30-case run. UI helper fixes use stable captured list bounds and actual tab-row coordinates rather than stale accessibility handles or fixed screen positions.

Final main unit/build/test-APK/lint command passed in 4m36s; final test-build/lint command after test-only corrections passed in 44s. Lint: zero errors/fatal issues, 69 warnings. No production changes followed the full device run. Read-only independent review found no blocking defects; its artist/empty-mix copy refinements were addressed. No tests or installations targeted the physical device.

APK package my.music, version 1.0.0, version code 2. Final APK copied, installed in emulator-5554 and launched to Songs. Current MusicPlayer.apk SHA-256: `823E5CC21D61D2C7785C0CCE04C2CA30BD8529D83AC3DB830AD90C8C0F82916D`. Artifact P:\MyMusic\MusicPlayer.apk. Runtime screenshots under analysis/evidence/for-you. Recommendation aggregates are device-local and not included in the existing backup format. Historical sections below retain their original identities and limitations.

# Previous APK — library theme preview cleanup

2026-10-28: removed the four sample text lines from Themes > Library and reduced the retained gradient preview from 210dp to 100dp. Theme creation/editing, colors and saved selections are unchanged. Package remains my.music, version 1.0.0.

Verification: assembleDebug succeeded in 1m46s and git diff --check passed. No tests or lint rerun for this visual-only text removal; no runtime visual check performed for this revision. Previous broader checks below are historical.

Current MusicPlayer.apk SHA-256: `C21B94E1459DB1C0F84528A3D2BFA644AFB7B8EA7F4460DC8D73C9D5F9A1EB12`. Copied from the successful debug build to P:\MyMusic\MusicPlayer.apk.

# Previous APK — package my.music

2026-10-28: migrated applicationId, namespace, main/test/androidTest Kotlin packages, imports, fully qualified component references and source directories to `my.music`. Active README/plan references updated. Version remains 1.0.0, version code 2, label My Music. Project is now located at P:\MyMusic. User IDE/Gradle configuration changes were preserved.

Verification: all 33 JVM tests passed, zero failures/errors/skips. Unit/build/lint command succeeded in 13m27s; lint has zero errors/fatal issues and 69 warnings. Source scan found no old identifier in app source or active Gradle configuration. aapt2 verified my.music, my.music.MainActivity, my.music.playback.PlaybackService, all three my.music.widgets receivers, and provider authority my.music.files. All APK DEX files were checked for the old dotted and slash package identifiers; none remained.

Runtime limitation: the test emulator did not complete a reliable startup for installation/launch checks. Initial installation attempts failed because Android package/activity services were not yet available; runtime migration checks remain unverified. No device instrumentation suite was run for this revision. Physical connected device was not modified. The new application ID installs separately from the old package and does not automatically inherit its data.

Current `MusicPlayer.apk` SHA-256: `00BAB1E7F7030669685EA858B85F8C4A4AAAE53258EC8768C1C37090B7897AF7`. Artifact: P:\MyMusic\MusicPlayer.apk. Sections below are historical and retain their original package IDs and APK identities.

# Previous APK — My Music 1.0.0

2026-10-21: Film is static during playback and no longer starts the animation frame loop. Drawer branding is a single vertically centered row using the supplied logo and My Music title. Version name is 1.0.0, version code 2; Settings reads BuildConfig.VERSION_NAME instead of a hardcoded version.

Executed: 33 JVM tests and the focused API37 playback/visual test passed, no failures/errors/skips. The static-Film assertion first failed against the animated renderer, then passed after the change. Final focused device/unit/build/lint command succeeded in 4m42s; lint has zero errors/fatal issues and 69 warnings. The entire device suite was not rerun for this small revision; earlier broader coverage is recorded below.

Packaged metadata verified with aapt2: My Music, versionName 1.0.0, versionCode 2. Installed and launched in emulator-5554. UI hierarchy verified the drawer logo and text share a center line; screenshot inspected at analysis/evidence/library-revision/drawer-1.0.0.png.

Current `MusicPlayer.apk` SHA-256: `005FC9DCAF2763A4FF837BF39E3BA4C2B9EB53473D4D9F359C1FA19F9992F257`. APK copied from the verified debug build. Sections below are historical snapshots.

# Previous APK — My Music, custom library themes and player revisions

2026-10-21: 33 JVM tests passed, zero errors/failures/skips. Final full emulator run passed 25 of 26 tests; the added visual screenshot check initially sampled Film before pause/visual settling. After correcting test synchronization, its focused rerun passed. Thus all 26 device cases passed across these runs; this is not a claim of one fully green 26-case run. Direct player-return checks cover Playlists, Folders, Albums, Artists and an opened folder. Custom library theme create/reload/edit/delete and the new playback/paused animations were exercised on API37. See [revision details](LIBRARY-PLAYER-REVISION-2026-10-21.md).

Final focused visual test, unit/build and fresh-process lint command succeeded in 4m21s. Lint: zero errors/fatal issues, 69 warnings. No product changes followed the full emulator run; subsequent changes only corrected test synchronization and documentation.

Current `MusicPlayer.apk` SHA-256: `3E4F3BB81C719AF37FFCAB20832545C0138EEB7DC3E68608E4646BDBF8FBFE82`. Copied from the verified debug build, installed in emulator-5554, permissions granted and launched. Packaged launcher label verified as My Music. Supplied icon SHA-256 remains `4F66B3F1BBFB7DFFF941A7A184C038A682EDAEB81EB82C93D85DA3E42220E271`. Original music preserved. Fifteen built-in player themes remain; library and player custom themes persist independently. Sections below are historical.

# Previous APK — reshuffle, branded intro and custom themes

2026-10-21: all 30 JVM tests and 24 API37 emulator tests passed, zero failures/errors/skips. Final full suite/build succeeded in 5m43s; instrumentation report time 320.25s. Actual queue and player shuffle icons were exercised, with preserved current occurrence, position and playback. The intro and custom-theme workflow exercised color picking, atomic save/apply, edit/rename, activity recreation and deletion. See [revision details](RESHUFFLE-INTRO-THEMES-2026-10-21.md).

Native/Compose startup now matches the supplied logo. There are 18 built-in player designs, eight library palettes and persistent custom player themes with visual color controls. Final fresh-process build/lint passed in 5m16s: zero errors, 69 warnings.

Current `MusicPlayer.apk` SHA-256: `4666F5B3EF480C6970F329F1001D08F80EDAB51A300CD60748954BBCF9B9C6EF`. Installed in emulator-5554 after instrumentation cleanup; audio and notification permissions granted. Original source music and logo asset preserved. Sections below are historical snapshots; their APK identities and counts do not describe this build.

# Previous APK — shuffle and animated cover themes

2026-10-19: 28 JVM tests and all 22 API37 emulator tests passed, zero failures/errors/skips. Final suite/build succeeded in 8m02s (instrumentation report: 455.732s). Shuffle survives song selection, explicit Shuffle starts at the first shuffled window, and crossfade preserves its traversal. Shared cover resolution updates the full player and preview, including live cover edits. Blurred cover backgrounds, spinning vinyl/cassette reels and PCM-driven energy waveform are implemented. See [playback visual verification](SHUFFLE-VISUALS-2026-10-19.md) for boundaries and signal timing limitations.

Final lint/build succeeded in 2m46s using a fresh single-use Gradle process: zero errors, 69 warnings. Current `MusicPlayer.apk` SHA-256: `DB52DDCD3D153F5C4D44FB6F61D416050AA7EF518364FE9E5BC59F5EB96FF48B`. Installed in emulator-5554 after test cleanup. The supplied launcher icon is preserved. Sections below are historical snapshots.

# Previous APK — supplied icon and themed seeking

2026-10-19: 22 JVM and 20 API37 emulator tests passed (no skips/failures/errors). Final build/lint passed: zero errors, 68 warnings. The supplied PNG is the launcher icon. Theme preview and live player now share six layout arrangements and functional linear/circular seek controls. Lyrics timer status was preserved. See [theme progress verification](THEME-PROGRESS-2026-10-19.md).

Previous `MusicPlayer.apk` SHA-256: `F407E8068E3BFB7EF7FA97DA8647D5D1E7BB97CA439E7F514E2AF9EC994C8086`. Installed in the emulator. Sections below are historical verification snapshots.

# Latest update — 2026-10-19

UI corrections are documented in [revision notes](REVISIONS-2026-10-19.md). Current APK: `P:\MusicPlayer\MusicPlayer.apk`; SHA-256 `770995471099EEEDB541A9A63464A1CA0CFC7C763BCCA08802D8EE3F92C2D27E`.

Executed 19 JVM tests and 18 API37 emulator tests: all passed, zero skipped. Final build/lint passed: zero errors, 66 warnings. Source music preserved. The APK is installed; one Classic widget was pinned and its queue shortcut checked. Device/OEM and reference-equivalence limits below still apply. The following 2026-10-18 section is historical.

# Completion verification — 2026-10-18

Project: `P:\MusicPlayer`, branch `codex/music-player`; package `app.local.musicplayer` 0.1.0.

- Executed own Gradle wrapper: `:app:testDebugUnitTest :app:connectedDebugAndroidTest :app:lintDebug :app:assembleDebug`. BUILD SUCCESSFUL, 2m59s. 16 JVM tests passed; 12 API37 emulator instrumentation tests passed, zero skipped/failures/errors. Android workflow report time 102.615s (suite 89.053s).
- After the status-bar contrast correction: reran `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug`. BUILD SUCCESSFUL, 1m3s; all 16 JVM tests pass; lint zero errors/37 warnings. No playback implementation changed after the successful full device suite.
- Warnings include pinned dependency/tool versions, target36 compatibility on newer OS, KTX/style suggestions, media-session export and backup/widget metadata suggestions. These are retained in `app/build/reports/lint-results-debug.html`.
- APK permissions inspected with SDK aapt2: no INTERNET or BILLING permission. Media3 contributes ACCESS_NETWORK_STATE; there is no network transport in the application. No advertising, purchasing or analytics dependency is declared.
- Independent read-only code review found six important edge cases. Targeted device tests first reproduced stale crossfade queue promotion and delayed resume overriding pause; both pass after ownership fixes. Unit regressions cover duplicate playlist selection, history counting/ranking and legacy write permission. Review findings and rulings are retained in the local execution ledger.
- Installed `P:\MusicPlayer\MusicPlayer.apk` on emulator-5554 separately after instrumentation cleanup. SHA-256: `27B8C8DF9BC3ADB17AF8F6E9F8F57061C6887E454964346D00266134CB83F0BD`.
- REA screenshot evidence includes library, full player and queue under `analysis/evidence/clone-library`, `clone-player`, `clone-queue`. The first `clone-final` image is a startup splash, not a completed library frame. Failed/null hierarchy dumps were not reused as evidence.
- The rebuilt APK's white system-bar icons were visually verified in `analysis/evidence/clone-final-bars/screen.png`; the library and paused restored queue are visible. Device startup measurements returned timeout on cold ART verification, so no startup performance claim is made.

Device: Android17/API37 x86_64 emulator; source music preserved. The fixture-copy MP3 test was executed, not skipped. Tests cover real playback, crossfade overlap and mutations, explicit pause, timer expiry/recreation, MediaStore discovery, collections/backup, transfer digest roundtrip, physical tags on owned copy, launcher scan navigation and song-to-playlist UI action.

This is functional clone verification, not proof of exact reference equivalence. See PARITY.md for unverified reference policies, wireless/proprietary formats, hardware and older-OS coverage. Emulator cold startup showed slow ART class verification and occasional system UI failures; no controlled cold/warm performance benchmark, process-family CPU/peak-memory measurement or token comparison was performed.
