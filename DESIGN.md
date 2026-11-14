# Architecture

My Music is a native, offline Android application. Kotlin and Jetpack Compose own the interface; Media3 owns playback and the media session. Application ID: `my.music`.

## Components

| Component | Responsibility |
| --- | --- |
| `library/` | MediaStore discovery, filtering, grouping, sorting, and recommendation ranking. |
| `collections/` | SQLite storage for playlists, favorites, history, preferences, metadata overrides, and recommendation aggregates. |
| `playback/` | Foreground media service, queue and transition ownership, audio processing, audio focus, session controls, and sleep timer. |
| `ui/` | Library navigation (tab bar + horizontal swipe pager), player controls, themes, settings, Dynamic Island onboarding/settings preview, and widget previews. |
| `island/` | Cutout capability checks, layout geometry, `DynamicIslandBus`, `TYPE_ACCESSIBILITY_OVERLAY` cutout service, and `TYPE_APPLICATION_OVERLAY` fallback. |
| `widgets/` | RemoteViews layouts, playback shortcuts, bounded artwork loading, and timeline updates. |
| `lyrics/` | Plain-text and LRC parsing and lyrics persistence. |
| `files/` | File operations and supported MP3 metadata writes with original-file recovery. |
| `backup/`, `transfer/` | Validated collection backup/restore and integrity-checked music packages. |
| `model/` | Shared media identities and operation results. |

## State and identity

MediaStore owns discovered file metadata. App-local repositories own preferences, user collections, lyrics, and display overrides. Queue occurrences and playlist entries have distinct identities so duplicates can be selected, reordered, or removed independently.

The playback service owns the current player and transition resources. UI and home-screen widgets control that service rather than creating separate playback queues. Queue restoration prepares the saved position without automatically resuming sound.

Navigation retains the active library tab and opened group when entering and leaving the player. Songs is the initial tab; users can switch sections via the top tab strip or left/right horizontal swipes (`HorizontalPager`), and For you can be enabled or disabled in Settings.

## Data and permissions

The app requires no accounts or internet permission. Media access uses Android's runtime permissions and content URIs. File modification and deletion use platform consent where required. Local metadata overrides are distinct from physical source-file edits.

Backup restoration validates input before replacing collections. Music-package transfers use size and digest checks, staging, and owned-output cleanup. Signing material and generated media are excluded from source control.

## Recommendations and widgets

Recommendation ranking uses only visible tracks and feature-specific aggregates. Disabling tracking and writing counters share the repository transaction boundary. Data remains until confirmed cleanup. See [recommendation behavior](docs/RECOMMENDATIONS.md).

Classic, Lite, and Vinyl use the same RemoteViews resources for previews and launcher widgets. Playback events update metadata and control state; active Vinyl timelines receive lightweight partial updates every five seconds. Artwork decoding uses a bounded shared worker.

## Dynamic Island & validation

`DynamicIslandCapability` selects the highest supported background overlay mode when enabled and while `MainActivity` is not in the foreground: `CUTOUT_ACCESSIBILITY` (API 28+ with Accessibility enabled, drawing around the front camera cutout via `TYPE_ACCESSIBILITY_OVERLAY`) or `OVERLAY_BELOW_STATUS_BAR` (API 26+ excluding Android Go on API 29+, using `SYSTEM_ALERT_WINDOW`). when overlay permissions are missing or unsupported, playback controls remain in standard Media3 notifications and widgets (`IN_APP_FALLBACK`). First-install opt-in prompts once via `dynamic_island_prompted`; declining explains what the feature does and points to `Settings → Dynamic Island`.

Unit tests exercise ranking, identities, parsers, themes, timers, audio processing, Dynamic Island capability/geometry/config, and file contracts. Emulator workflows cover public UI and playback operations. See [testing](docs/TESTING.md) and [recorded verification](docs/VERIFICATION.md) for setup and known coverage limits.
