# Testing

Run checks from the repository root with an existing Android SDK and JDK 25 configured.

## Local checks

```powershell
.\scripts\verify.ps1
```

This runs unit tests, debug lint, and the debug build. Equivalent Gradle tasks:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Build the instrumentation APK separately with `:app:assembleDebugAndroidTest`.

## Emulator setup

Use a dedicated emulator containing disposable test data. Generate original WAV/LRC fixtures:

```powershell
.\scripts\New-AudioFixtures.ps1
```

The default output is the ignored `analysis/fixtures/` directory. The generated audio contains synthetic tones named Dawn, Night, and Dawn reprise, plus a timed lyrics file. Copy these to a dedicated music folder on the emulator and ensure MediaStore has indexed them before running library and playback tests.

With only the test emulator connected:

```powershell
.\scripts\verify.ps1 -Device
```

The device option uses Gradle instrumentation and a disposable app installation. It can remove that installation after the run; reinstall the APK separately for manual use. Do not point this workflow at a phone containing app data you want to retain.

The MP3 tag-writing test requires an available MP3 and Android API 29 or later. It copies the source to an owned temporary media entry, writes and validates tags on that copy, then removes the temporary entry. Without an MP3, that case is skipped. Source audio is not modified by the test.

## Targeted instrumentation

Install both the debug app and test APK on a chosen emulator. Run a single test class, replacing the serial:

```powershell
adb -s <emulator-serial> install -r app/build/outputs/apk/debug/app-debug.apk
adb -s <emulator-serial> install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s <emulator-serial> shell am instrument -w -r -e class my.music.RevisionWorkflowTest my.music.test/androidx.test.runner.AndroidJUnitRunner
```

Read the final test summary; the instrumentation completion code alone does not distinguish success from failed test cases.

## Coverage

- Unit tests cover media identities, queue behavior, audio processing, metadata parsing, lyrics, themes, recommendation ranking, and timers.
- Emulator workflows cover navigation, collections, playback, queue dragging, seeking, themes, storage migration, recommendation opt-out/cleanup, transfer, and supported file actions.
- Widget geometry checks inflate the actual RemoteViews at 250, 320, and 400 dp widths, using each widget’s minimum height, and check that controls remain inside the layout.

Use [recorded verification](VERIFICATION.md) to distinguish executed checks from areas that still need hardware or additional Android-version coverage.
