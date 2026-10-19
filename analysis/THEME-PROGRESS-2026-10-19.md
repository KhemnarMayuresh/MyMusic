# App icon and themed seek controls — 2026-10-19

User references: `cleaneed.png` and five supplied player-theme screenshots. The screenshots show the timeline moving between inline, circular, immersive and light-panel arrangements. Their premium badges are reference UI, not instructions to add purchases.

## Root cause and fix

The old theme picker drew a fixed 38% LinearProgressIndicator. FullPlayer also hardcoded one control layout for every theme, so changing the artwork could never move its timeline. The picker and applied player now render the same ThemedPlayerBody, with layout selected by PlayerThemes metadata. The six arrangements include standard, inline title/actions, ring around the artwork, immersive artwork, light panel, and progress above the artwork.

ThemeSeek owns touch, pending-seek and accessible progress state per media occurrence. Both tap and drag update the actual MediaSession for loaded media. Without loaded media, the preview uses an interactive local demonstration timeline. Displayed time comes from playback state; no fixed 38% indicator remains. Acknowledgement/pending state keeps the thumb from briefly jumping back while the controller reports a seek. Lyrics retains linear seeking and active sleep-timer feedback.

The exact supplied PNG was copied to `app/src/main/res/drawable-nodpi/brand_icon.png`; SHA-256 `4F66B3F1BBFB7DFFF941A7A184C038A682EDAEB81EB82C93D85DA3E42220E271`. Its bytes match the original. Legacy and adaptive launcher resources use that image. The original file was not changed.

## Verification

Unit regression tests first failed on the missing SeekMath/layout contract, then passed after implementation. Device checks tap and drag linear and circular timelines across six applied layouts, seek the same media from the theme preview, check the progress control moves above artwork in Minimal, and verify timer status remains visible in Lyrics. The demo preview is checked through its accessible progress range rather than trying to read text merged inside the seek control. Finer injected drag steps avoid interpreting the last injected move event as an exact endpoint.

Read-only review identified a lost Lyrics timer indicator during the shared-renderer refactor; the shared timer status and active tint were restored before final verification. Source music remains unmodified. Original theme photography and a pixel-exact recreation of the reference catalog are not claimed.

Final executed checks: all 22 JVM tests and 20 API37 emulator tests passed, with zero skips/failures/errors (`theme-progress-acceptance.log`, BUILD SUCCESSFUL 4m 17s; device report 217.59s). These checks include actual MediaSession seeking from both full player and theme preview across six arrangements, circular drag, changing progress geometry and Lyrics timer status. The demo preview exposes its scrubbed value in the accessibility range.

The first lint run hit an internal Kotlin UAST `AsyncExecutionService` failure; the same checks passed in a fresh single-use process (`theme-progress-final-lint.log`, BUILD SUCCESSFUL 2m 29s). Final lint: zero errors, 68 warnings. The rebuilt debug APK was installed on emulator-5554. SHA-256: `F407E8068E3BFB7EF7FA97DA8647D5D1E7BB97CA439E7F514E2AF9EC994C8086`.

The supplied icon was visually checked on the launcher (`evidence/new-icon.json`, REA screenshot); `evidence/applied-Orbit.png` and `evidence/applied-Minimal.png` show different progress placements. The source PNG and the bundled PNG share the SHA-256 recorded above. Earlier failed text-based assertions were test observation/injection issues; screenshots already showed the correct demo position, and subsequent accessible-range/session checks passed.
