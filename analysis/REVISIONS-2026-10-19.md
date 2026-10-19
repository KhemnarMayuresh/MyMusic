# UI and functional corrections — 2026-10-19

Requested corrections: arbitrary queue drag/drop; grouped-library sorting, selection and menus; lock-screen preference; visual player-theme previews and Apply; three reference-like widgets.

## Reference evidence

Retained REA JSON/screens under `analysis/evidence/revisions/` preserve the current virtual-device observations. The initial reference queue capture shows a left drag handle and right overflow. The fresh `folder-menu.xml` records Play, Play next, Add to queue, Add to playlist, Hide folder and folder/song header. The reference widget screenshot shows Classic/Lite (4×1), Vinyl (4×2), Simple and Circular cards with thumbnail previews and Add. Ads/rating prompts encountered during inspection were dismissed, not reproduced in the clone. Failed/null hierarchy captures were not reused. The complete original player-theme catalog remains unverified; twelve original local designs implement the requested visual choice and preview flow.

## Changes

- Queue: left drag handles, overflow, repeat/shuffle, occurrence identity preserved at drop. Gesture ownership lives on the stable list, with floating row and edge autoscroll, so row recycling cannot end a long drag. Concurrent queue reconciliation uses current session identities.
- Folders/albums/artists: per-tab persistent sorting, long-press/checklist selection, play/queue/playlist/delete actions; individual bottom sheets with Play, Play next, Add to queue, Add to playlist and Hide. Folder hiding excludes its path and children and can be reversed in Hidden music. Bulk deletion keeps Android confirmation and legacy permissions.
- Lock screen playing: persistent preference controls whether the open, playing activity may appear over the system lock screen. It does not dismiss secure keyguard or replace Android's own notification policy.
- Themes: twelve distinct artwork treatments and color pairs, a central phone preview, horizontal choices and Apply. Preview and live player share the same artwork renderer and accent/background palette. Library themes also have colored previews.
- Widgets: Classic and Lite compact layouts, plus Vinyl with record artwork; rounded tinted backgrounds, vector controls, shared queue, favorite feedback and queue shortcut. Gallery previews inflate the real RemoteViews layouts. Covers decode off the UI thread with bounded dimensions and request-generation checks against stale completions.

## Validation

See revision test/build logs and final report. Initial drag across several rows and return passed, as did lock-screen preference persistence. UI test failures exposed immediate lookups after navigation/sort/tab transitions; explicit observable-control waits replaced those lookups. The timer integration check now measures additional playback from its timer-start position instead of assuming the initial playback-state observation arrived at position zero. A review found off-screen drag ownership and stale artwork completion bugs; both were fixed at their owners and regression coverage was extended. A Windows R.jar lock from overlapping lint builds was resolved by stopping only this project's identified build processes and using sequential verification.

Exact original theme artwork, all original widget variants, OEM lock-screen policies and every launcher size remain outside verified equivalence.

Final executed verification: 19 JVM tests and all 18 emulator tests passed with no skips/failures/errors (`revision-acceptance.log`, BUILD SUCCESSFUL 4m23s; device report time220.032s). This includes 25-song off-screen dragging and all widget controls fitting their minimum height. Final package/lint (`revision-package.log`) passed in 1m 8s, zero lint errors and 66 warnings; warnings are mostly hardcoded English XML labels, KTX/style/dependency suggestions and API31 widget attributes ignored on older OS. The final metadata specifies preferred 4-cell widget widths on supporting launchers. No advertising/billing/internet permission was added.

Installed `P:\MusicPlayer\MusicPlayer.apk`, SHA-256 `770995471099EEEDB541A9A63464A1CA0CFC7C763BCCA08802D8EE3F92C2D27E`. Classic was pinned on the emulator launcher and rendered all controls; tapping its queue shortcut opened Playing queue. Gallery and minimum-height previews were visually inspected. Existing and newly generated source music were preserved. `final-widgets.json` is the final REA gallery capture; `accepted-widgets.png`, `test-themes-initial.png` and `test-queue.png` supplement it. Older screenshots showing clipped widget controls describe the earlier build, not the final APK.
