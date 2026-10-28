# My Music 1.0.0

Final local release — 2026-10-28. Git tag: `v1.0.0`.

- Android package: `my.music`; launcher name: **My Music**.
- Version name: **1.0.0**; version code: **2**.
- Minimum Android 7.0 (API 24); target API 36; compile API 37.
- Local music library, playlists, folders/albums/artists, queue reordering, shuffle/repeat and notification/lock-screen playback.
- Cover-based player backgrounds, animated player themes, custom player/library themes and three widgets.
- Optional For you recommendations with a Moto-inspired dashboard, High Quality first in Explore, and explicit recommendation-data cleanup. Disabling hides the tab and stops tracking while retaining stored data.
- No ads, in-app purchases, accounts or internet permission.

## Build and signing

Gradle 9.3.1 and JDK 25 are pinned in the repository. Use the existing Android SDK and the README build instructions. Source code, resources, build configuration and release documentation are committed; local IDE state, build outputs, media fixtures, evidence captures and signing keys are ignored.

`P:\MyMusic\MusicPlayer.apk` is the installable debug-signed validation artifact, SHA-256 `2F053141D1B3CAAF5642CF06B0F8C7305943AE1FEEE3C82793FF28340CD0DD25`. It is intentionally excluded from Git. `:app:assembleRelease` generates an unsigned release APK under `app/build/outputs/apk/release/`; owner-controlled signing is required for distribution. No signing key is generated or committed as part of this release.

## Verification and scope

Final verification: all 30 emulator instrumentation tests passed in one run; 37 unit-test report results are green; debug/release builds and lint succeeded (zero lint errors, 69 warnings). Two test-only accessibility timing fixes were included; app behavior and the installable APK are unchanged. Detailed validation is recorded in [analysis/VERIFICATION.md](analysis/VERIFICATION.md). See [README compatibility notes](README.md#compatibility-and-remaining-reference-gaps) for remaining coverage limits, supported metadata edits and transfer-format differences. The release designation records this accepted project version; it does not claim untested feature parity or physical-device coverage.

## Local Git release

The release commit is on `main` and marked by the annotated `v1.0.0` tag. Nothing is pushed or published by this task. No remote was configured at finalization; add your chosen remote and push `main` and `v1.0.0` manually when ready.
