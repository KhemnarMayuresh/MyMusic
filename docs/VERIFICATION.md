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

## Artifact identity

| Field | Value |
| --- | --- |
| Application ID | `my.music` |
| Launcher name | My Music |
| Version name / code | 1.0.0 / 2 |
| Minimum / target / compile API | 24 / 36 / 37 |
| Current installable artifact | `MusicPlayer.apk` (local, debug-signed, excluded from Git) |
| Current APK SHA-256 | `80DF430F8343E1EE18EBB6C4CFBB7F4D5C3ED2DD47D9ACC7260D0FB90C3622E9` |
| Tagged v1.0.0 APK SHA-256 | `2F053141D1B3CAAF5642CF06B0F8C7305943AE1FEEE3C82793FF28340CD0DD25` |

The widget update follows the existing `v1.0.0` tag. The tag and its artifact identity are unchanged. Signing for distribution remains owner-controlled.

## Remaining validation

Runtime coverage is currently an API 37 x86_64 emulator. Older Android versions, physical headsets and Bluetooth controls, additional launchers, captured audio-output quality, paired-device transfer, and an exhaustive codec matrix need additional validation.

MP3 tag-writing tests use an owned copy when a suitable MP3 is available. Other-format metadata and artwork changes remain app-local. Crash or power-loss recovery during physical file replacement has not been established.

See [testing](TESTING.md) for reproducible commands and fixture setup.
