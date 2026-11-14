package my.music

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import kotlinx.coroutines.runBlocking
import my.music.island.IslandConfig
import my.music.library.MediaLibrary
import my.music.model.OperationResult
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class DynamicIslandWorkflowTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val device get() = UiDevice.getInstance(instrumentation)

    private fun grantBasePermissions() {
        instrumentation.uiAutomation.grantRuntimePermission(
            context.packageName,
            if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE,
        )
        if (Build.VERSION.SDK_INT >= 33) {
            instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun launchApp() {
        device.wakeUp()
        device.executeShellCommand("wm dismiss-keyguard")
        context.startActivity(
            context.packageManager.getLaunchIntentForPackage(context.packageName)!!
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        assertTrue(device.wait(Until.hasObject(By.desc("Open menu")), 60_000))
    }

    @Test
    fun firstInstallDeclineShowsWhatItDoesAndWhereToEnableAndAsksOnlyOnce(): Unit = runBlocking {
        grantBasePermissions()
        val repo = (context.applicationContext as MusicApplication).collections
        val oldPrompted = repo.state.value.pref(IslandConfig.KEY_PROMPTED, "false")
        val oldEnabled = repo.state.value.pref(IslandConfig.KEY_ENABLED, "false")
        try {
            repo.setPreferences(mapOf(IslandConfig.KEY_PROMPTED to "false", IslandConfig.KEY_ENABLED to "false"))
            launchApp()
            assertTrue("First-install card should appear", device.wait(Until.hasObject(By.text("Try Dynamic Island?")), 15_000))
            device.findObject(By.text("Not now")).click()

            assertTrue("Decline explanation should appear", device.wait(Until.hasObject(By.text("Dynamic Island stays off")), 10_000))
            assertTrue("Should explain what it does", device.hasObject(By.textContains("What it does:")))
            assertTrue("Should explain where to enable later", device.hasObject(By.textContains("Settings → Dynamic Island")))

            device.findObject(By.text("Got it")).click()
            assertTrue(repo.state.value.flag(IslandConfig.KEY_PROMPTED))
            assertFalse(repo.state.value.flag(IslandConfig.KEY_ENABLED))

            launchApp()
            device.waitForIdle()
            assertFalse("First-install prompt must not appear again", device.hasObject(By.text("Try Dynamic Island?")))
        } finally {
            repo.setPreferences(mapOf(IslandConfig.KEY_PROMPTED to oldPrompted, IslandConfig.KEY_ENABLED to oldEnabled))
        }
    }

    @Test
    fun islandStaysHiddenInAppAndAppearsOnlyInBackground(): Unit = runBlocking {
        grantBasePermissions()
        val repo = (context.applicationContext as MusicApplication).collections
        val oldPrompted = repo.state.value.pref(IslandConfig.KEY_PROMPTED, "false")
        val oldEnabled = repo.state.value.pref(IslandConfig.KEY_ENABLED, "false")
        val tracks = (MediaLibrary(context).rescan() as OperationResult.Success).value
        assertTrue("Audio fixtures must be indexed", tracks.isNotEmpty())
        val controller = MediaController.Builder(
            context,
            SessionToken(context, ComponentName(context, "my.music.playback.PlaybackService")),
        ).buildAsync().get(20, TimeUnit.SECONDS)

        fun overlayWindowDump(): String =
            device.executeShellCommand("dumpsys window windows")

        fun hasVisibleOverlayWindow(): Boolean {
            val blocks = overlayWindowDump().split("Window #")
            return blocks.any { b ->
                b.contains("package=my.music") &&
                    (b.contains("ty=APPLICATION_OVERLAY") || b.contains("ty=ACCESSIBILITY_OVERLAY")) &&
                    b.contains("isVisible=true")
            }
        }

        try {
            device.executeShellCommand("appops set ${context.packageName} SYSTEM_ALERT_WINDOW allow")
            repo.setPreferences(mapOf(IslandConfig.KEY_PROMPTED to "true", IslandConfig.KEY_ENABLED to "false"))
            launchApp()
            device.findObject(By.desc("Settings")).click()
            assertTrue(device.wait(Until.hasObject(By.text("General")), 10_000))
            val cx = device.displayWidth / 2
            val startY = (device.displayHeight * 0.75f).toInt()
            val endY = (device.displayHeight * 0.30f).toInt()
            var attempts = 0
            while (!device.hasObject(By.text("Dynamic Island")) && attempts < 6) {
                device.swipe(cx, startY, cx, endY, 20)
                device.waitForIdle()
                attempts++
            }
            assertTrue("Dynamic Island setting row should be visible after scrolling", device.wait(Until.hasObject(By.text("Dynamic Island")), 5_000))
            device.findObject(By.text("Dynamic Island")).click()
            assertTrue(device.wait(Until.hasObject(By.text("Enable Dynamic Island")), 10_000))

            device.findObject(By.text("Enable Dynamic Island")).click()
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
            while (!repo.state.value.flag(IslandConfig.KEY_ENABLED) && System.nanoTime() < deadline) Thread.sleep(50)
            assertTrue(repo.state.value.flag(IslandConfig.KEY_ENABLED))

            device.pressBack()
            assertTrue("Should return from Dynamic Island screen to Settings", device.wait(Until.hasObject(By.text("Settings")), 10_000))
            device.pressBack()
            assertTrue("Should return from Settings to Library", device.wait(Until.hasObject(By.desc("Open menu")), 10_000))

            val targetTrack = tracks.maxBy { it.durationMs }
            instrumentation.runOnMainSync {
                controller.setMediaItem(
                    MediaItem.Builder()
                        .setMediaId("island-fixture")
                        .setUri(targetTrack.id.uri)
                        .setMediaMetadata(MediaMetadata.Builder().setTitle("Island Track").setArtist("Test Artist").build())
                        .build()
                )
                controller.prepare()
                controller.play()
            }

            // While My Music is open in foreground, no Dynamic Island overlay may appear.
            Thread.sleep(1_000)
            assertFalse("In-app Dynamic Island pill must not show inside app", device.hasObject(By.desc("In-app Dynamic Island")))
            assertFalse("System Dynamic Island overlay must stay hidden while My Music is open", hasVisibleOverlayWindow())

            // Putting My Music in the background must reveal the Dynamic Island overlay.
            device.pressHome()
            val bgDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
            var shownInBg = false
            while (!shownInBg && System.nanoTime() < bgDeadline) {
                shownInBg = hasVisibleOverlayWindow()
                if (!shownInBg) Thread.sleep(150)
            }
            assertTrue("Dynamic Island overlay must appear when My Music is in the background", shownInBg)

            // Returning to My Music must hide the overlay again.
            launchApp()
            val fgDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
            var hiddenInFg = false
            while (!hiddenInFg && System.nanoTime() < fgDeadline) {
                hiddenInFg = !hasVisibleOverlayWindow()
                if (!hiddenInFg) Thread.sleep(100)
            }
            assertTrue("Dynamic Island overlay must hide when returning to My Music", hiddenInFg)
        } finally {
            instrumentation.runOnMainSync { controller.pause(); controller.release() }
            repo.setPreferences(mapOf(IslandConfig.KEY_PROMPTED to oldPrompted, IslandConfig.KEY_ENABLED to oldEnabled))
            device.executeShellCommand("appops set ${context.packageName} SYSTEM_ALERT_WINDOW default")
        }
    }
}

