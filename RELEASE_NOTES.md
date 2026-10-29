# Release notes

## Unreleased

- Redesigned Classic, Lite, and Vinyl widgets with gradient backgrounds, outlined controls, cover artwork, and consistent preview spacing.
- Added playback position and duration to Vinyl with lightweight timeline updates.
- Expanded widget geometry checks across three widths.
- Consolidated product and developer documentation, with screenshots in the README.

## 1.0.0 — 2026-10-28

Git tag: `v1.0.0`. Application ID: `my.music`. Launcher name: **My Music**. Android version code: **2**.

- Local music browsing with songs, playlists, folders, albums, and artists.
- Foreground media-session playback, queue reordering, shuffle, repeat, and notification and lock-screen controls.
- Cover-based backgrounds, animated player layouts, custom player and library themes, and home-screen widgets.
- Local recommendations with daily mixes, high-quality audio collections, recent listening, and monthly charts.
- Recommendation opt-out that stops tracking while retaining data until confirmed cleanup.
- Lyrics, equalizer, sleep timer, supported metadata editing, collection backup, and local music packages.
- No ads, in-app purchases, accounts, or internet permission.

Validation at the release tag: 37 unit-test results green, all 30 emulator tests passed in one run, debug and unsigned release builds succeeded, and lint reported zero errors with 69 warnings. See [verification](docs/VERIFICATION.md) for subsequent updates, artifact identities, and coverage limits.

## Distribution

Debug APKs use the local Android debug key. `:app:assembleRelease` generates an unsigned release APK; configure owner-controlled signing for distribution. Build outputs and signing credentials are excluded from Git.
