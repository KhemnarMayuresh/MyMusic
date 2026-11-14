# Verification

This report records executed checks and their scope. Documentation cleanup on 2026-10-29 does not change app runtime behavior or the existing APK.

## Current widget update — 2026-10-28

- Debug build, unit-test task, test-APK build, and debug lint completed successfully in 5m34s.
- Unit reports: **37 tests**, zero failures, errors, or skips.
- Lint: **zero errors**, 80 warnings.
- Expanded widget geometry test passed on the final APK in 0.661s: Classic, Lite, and Vinyl at 250, 320, and 400 dp widths, using their minimum heights. Controls had positive size and remained within bounds; artwork was visible.
- The in-app widget selection screen was visually checked after adjusting button height and icons.
- The emulator launcher did not support direct pin requests. Add widget correctly displayed instructions for adding widgets from the home screen.

The complete device suite was not rerun for this widget-only update. Actual launcher hosting, live progress updates on a bound widget, and older Android versions were not verified in that run. Decorative widget bars and waves are static artwork.

## Version 1.0.0 release validation — 2026-10-28

- Debug build, unsigned release build, test-APK build, unit-test task, and lint completed successfully in 3m13s. Gradle reused unchanged tasks where up to date.
- Unit reports: **37 tests**, zero failures, errors, or skips.
- Lint: **zero errors**, 69 warnings at that revision.
- Full API 37 emulator instrumentation suite: **30 tests passed in one run**, 630.211s.
- Two test-only accessibility timing issues were corrected before that green run: theme crop geometry is captured once per layout, and carousel scrolling uses stable bounds after the entire card becomes visible.

The release variant was built unsigned and was not installed for runtime testing. Device workflows used the debug variant.

## Dynamic Island transport controls & layout bounds fix — 2026-11-14

- Resolved issue where pause-play, previous, and next transport buttons in the expanded Dynamic Island did not respond to touch.
- **Root causes addressed:**
  - Expanded card height in `DynamicIslandGeometry.computeExpanded` was previously `topSafePaddingPx + 146dp`, which was shorter than the actual laid-out content height (`topSafePaddingPx + 152dp` to `158dp` with 1.15x font scaling). As a result, `controlsRow` extended outside the window frame and was clipped or dropped as outside touches.
  - Increased content clearance to `topSafePaddingPx + 168dp` with `16dp` bottom padding, ensuring transport buttons sit safely above the bottom curved outline.
  - Added `FLAG_NOT_TOUCH_MODAL` to `DynamicIslandOverlayController` window flags, preventing ambiguous modal touch capture.
  - Added `AnimatorListenerAdapter` with `onAnimationEnd` to guarantee `collapsedGroup` is set to `GONE`, `expandedGroup` to `VISIBLE`, and final bounds `(endX, endY, endW, endH)` committed even with floating-point interpolator rounding.
  - Added `requestDisallowInterceptTouchEvent(true)` on `ACTION_DOWN` for transport buttons, standard borderless ripple backgrounds, explicit `isFocusable = false` / `isClickable = true`, and expanded touch targets (`48dp` minimum).
  - Improved `previous()` and `next()` media item navigation logic in `PlaybackService`.
- **Unit tests (`:app:testDebugUnitTest`, run after final edit):** Passed — **49** tests across **14** test classes (`DynamicIslandTest`: 12/12, all tests passing).
- **APK build (`:app:assembleDebug`):** Passed. Local `MusicPlayer.apk` updated.

## Dynamic Island refinement & swipeable library tabs — 2026-11-11

- Fixed floating-overlay window inset translation jitter using display-level `WindowMetrics` (API 30+) and non-negative cutout normalization, memoized overlay updates, lowered calibration minimums to `-48dp` vertical offset and `-10dp` pill height delta, enforced background-only Dynamic Island suppression (`MusicApplication` activity-token lifecycle tracking; no in-app overlay pill), and added `HorizontalPager` left/right swiping across Library tabs.
- **Unit tests (`:app:testDebugUnitTest`, run after final edit):** Passed — **48** tests across **14** test classes (`AudioSignalTest` 2, `CoreTest` 6, `CoverColorsTest` 2, `CustomLibraryThemesTest` 2, `CustomThemesTest` 3, `DynamicIslandTest` 11, `EqProcessorTest` 2, `EqualizerTest` 3, `Id3EditorTest` 2, `LibraryDetailsTest` 1, `RecommendationsTest` 4, `ReviewRegressionTest` 4, `RevisionTest` 3, `ThemeProgressTest` 3), 0 failures, 0 errors, 0 skipped.
- **APK build (`:app:assembleDebug`, `:app:assembleDebugAndroidTest`, run after final edit):** Passed. Local `MusicPlayer.apk` updated from `app/build/outputs/apk/debug/app-debug.apk`.
- **Targeted emulator instrumentation (`emulator-5554`, `DynamicIslandWorkflowTest` and `LibraryThemeRevisionTest`, run after final edit):** Passed — **5** tests (`DynamicIslandWorkflowTest`: 2, `LibraryThemeRevisionTest`: 3), 0 failures (`105.597s`). Verified background-only Dynamic Island visibility, swipe switching (`For you <-> Songs <-> Playlists`), and clean `island_cutout_cache.xml` display cutout coordinates (`left=954, top=0, right=1080, bottom=126` under corner cutout emulation).
- **Scoped / earlier checks:** `:app:lintDebug` passed (`0` errors, `97` warnings) immediately prior to the final activity-token/pager-yield refinement (not re-run after that final edit). The remaining 9 instrumentation classes and physical `Micromax E7533` visual verification were not run on this revision.

## Dynamic Island feature update — 2026-11-11

- Added opt-in `'Dynamic Island'` cutout playback hub (`island/`), settings screen (`Settings → Dynamic Island`), one-time first-install prompt, decline explanation dialog, `ic_island_note.xml` transparent fallback artwork vector, and `DynamicIslandTest` / `DynamicIslandWorkflowTest`.
- **Unit tests (`:app:testDebugUnitTest`, run after final code edit):** Passed — **44** tests across **14** test classes (`AudioSignalTest` 2, `CoreTest` 6, `CoverColorsTest` 2, `CustomLibraryThemesTest` 2, `CustomThemesTest` 3, `DynamicIslandTest` 7, `EqProcessorTest` 2, `EqualizerTest` 3, `Id3EditorTest` 2, `LibraryDetailsTest` 1, `RecommendationsTest` 4, `ReviewRegressionTest` 4, `RevisionTest` 3, `ThemeProgressTest` 3), 0 skipped, 0 failed, 0 errors.
- **Lint & build (`:app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest`, run after final code edit):** Passed (`0` lint errors; `98` warnings). Local `MusicPlayer.apk` updated from `app/build/outputs/apk/debug/app-debug.apk`.
- **Dedicated emulator verification (`emulator-5554`, `Medium_Phone_API_37.0`):**
  - Visually verified after final `ic_island_note.xml` fix: `My Music Dynamic Island` Accessibility service draws `TYPE_ACCESSIBILITY_OVERLAY` cutout pill above the status bar on Launcher with crisp music-note fallback artwork and active visualizer bars; tapping expands the full card with scrubber, elapsed/`-remaining`, and playback controls (`Pause` flips state to paused cleanly).
  - Earlier in the session prior to the `ic_island_note.xml` drawable fix: `ForYouWorkflowTest#songsDefaultMixPlaybackDisableRetainReenableAndConfirmedCleanup` passed on `emulator-5554`, and `InAppDynamicIslandOverlay` was visually verified on the `Songs` tab clearing top-bar controls without overlap.
  - Not re-run after the final `ic_island_note.xml` edit: `DynamicIslandWorkflowTest` and the full connected instrumentation suite (stopped at user request to begin physical-device testing promptly).

## Artifact identity

| Field | Value |
| --- | --- |
| Application ID | `my.music` |
| Launcher name | My Music |
| Version name / code | 1.0.0 / 2 |
| Minimum / target / compile API | 24 / 36 / 37 |
| Current installable artifact | `MusicPlayer.apk` (local, debug-signed, excluded from Git) |
| Current APK SHA-256 | `639BF3BA6E1E587F8E5025056DB66F806F2E2588CEE6A6B44908C07B4C9ABCCF` |
| Tagged v1.0.0 APK SHA-256 | `2F053141D1B3CAAF5642CF06B0F8C7305943AE1FEEE3C82793FF28340CD0DD25` |

The widget refresh and Dynamic Island feature follow the existing `v1.0.0` tag. The tag itself is unchanged. Signing for distribution remains owner-controlled.

## Remaining validation

Runtime coverage is currently an API 37 x86_64 emulator. Older Android versions, physical headsets and Bluetooth controls, additional launchers, captured audio-output quality, paired-device transfer, and an exhaustive codec matrix need additional validation.

MP3 tag-writing tests use an owned copy when a suitable MP3 is available. Other-format metadata and artwork changes remain app-local. Crash or power-loss recovery during physical file replacement has not been established.

See [testing](TESTING.md) for reproducible commands and fixture setup.
