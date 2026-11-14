package my.music.island

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.res.Configuration
import android.view.accessibility.AccessibilityEvent

class DynamicIslandAccessibilityService : AccessibilityService() {
    private var controller: DynamicIslandOverlayController? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        running = true
        DynamicIslandBus.setAccessibilityRunning(true)
        controller?.stop()
        controller = DynamicIslandOverlayController(this, IslandRenderMode.CUTOUT_ACCESSIBILITY).also {
            it.start()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Privacy-minimal: this service does not inspect or read other apps' window contents.
    }

    override fun onInterrupt() {
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        controller?.onConfigurationChanged(newConfig)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        teardown()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        teardown()
        super.onDestroy()
    }

    private fun teardown() {
        running = false
        DynamicIslandBus.setAccessibilityRunning(false)
        controller?.stop()
        controller = null
    }

    companion object {
        @Volatile
        var running: Boolean = false
            private set
    }
}

