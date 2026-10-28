# Neon widget design update

Reference: user-provided Neon Music Player Widget Selection.png. Rebuilt Classic, Lite and Vinyl using native RemoteViews layouts, also inflated by the in-app selection screen. Card horizontal margin 14dp, inner padding 12dp, inter-card gap 8dp, preview-to-button gap 8dp, Add widget visual height 34dp. Preview aspect ratios follow the reference: 892:170, 892:152 and 892:340.

Classic places the cover, title/artist and all five controls on one row with subtle decorative bars. Lite has circular artwork, a wave motif and controls. Vinyl has a grooved record with central cover artwork, metadata, timeline, timestamps and controls. Uses the supplied app logo as fallback, native gradients, outlined favorite and music-list queue icons. Control actions are preserved. Vinyl's timeline reflects player position/duration on player events and refreshes every five seconds while playing; periodic updates query only installed Vinyl widgets and avoid artwork decoding or preference writes.

Updated Vinyl's advertised minimum height to 128dp. Expanded the RemoteViews bounds test to 250/320/400dp widths and each advertised minimum height; all five controls have positive size and remain inside the layout, and cover art is visible. Emulator renders are retained under ignored analysis/evidence/widget-redesign. Final in-app screenshot selection-final.png was visually inspected after correcting the Add widget component's intrinsic height. Emulator launcher does not support direct pin requests; pressing Add widget correctly shows the long-press-home/Widgets fallback. Actual launcher-host pinning and older Android devices were not verified in this revision. Decorative bars/waves are static widget artwork, not audio-level claims.

The prior v1.0.0 Git tag remains unchanged; this design update is a subsequent commit on main. Package/version remain my.music / 1.0.0 (code 2). No push or publication.

Validation: final build/unit/test-APK/lint command passed in 5m34s; 37 unit reports green, zero lint errors (80 warnings). Final geometry device test passed in 0.661s. Current APK SHA-256: 80DF430F8343E1EE18EBB6C4CFBB7F4D5C3ED2DD47D9ACC7260D0FB90C3622E9.
