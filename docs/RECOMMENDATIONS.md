# Recommendations

For you is enabled by default and appears before Songs. Songs remains the initial selected tab.

## Sections

- **Top picks:** Your Hits, Top Album, and Top Artist mixes.
- **Recently Played:** songs ordered by their latest tracked play.
- **Explore your library:** High Quality, Top Artist mix, Daily discover, Never Played, Before 1980, and Before 2000, in that order.
- **Month’s Top Chart:** songs ranked by tracked plays in the current local calendar month.

Every mix opens a playable list. Global Play starts ordered library playback; Shuffle enables shuffle. Individual mix and track selection preserves the current shuffle setting.

Rankings use only visible library tracks. Missing release years and bitrates are not guessed. Known lossless MIME types or bitrates of at least 320,000 bits per second qualify for High Quality. Daily discovery uses a local date seed; no remote service is involved.

## Storage and privacy

SQLite stores one play-count/last-played aggregate per track and one counter per track and calendar month. It does not store a separate event for every play or duplicate audio/artwork files.

Recommendation records are separate from playlist listening history. Disabling For you hides the tab and stops feature-specific writes, but retains its existing records. Re-enabling uses those records again. **Clear recommendation data** requires confirmation and removes only recommendation aggregates. No automatic pruning is performed.

The enabled preference is checked under the same repository transaction lock used for recording plays, preventing queued writes from bypassing opt-out. The additive version 2 database migration preserves existing collections. Recommendation records are not included in the current JSON backup format.
