package my.music.island

import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.view.DisplayCutout
import android.view.WindowInsets
import android.view.WindowManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DynamicIslandMediaState(
    val active: Boolean = false,
    val playing: Boolean = false,
    val mediaId: String? = null,
    val title: String = "",
    val artist: String = "",
    val artworkUri: String? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
)

data class CutoutSnapshot(
    val portraitCutout: IslandRect? = null,
    val statusBarHeightPx: Int = 0,
    val screenWidthPx: Int = 0,
)

interface DynamicIslandActions {
    fun togglePlayPause()
    fun previous()
    fun next()
    fun seekTo(positionMs: Long)
}

object DynamicIslandBus {
    private const val PREF_NAME = "island_cutout_cache"

    private val mutableMedia = MutableStateFlow(DynamicIslandMediaState())
    val media = mutableMedia.asStateFlow()

    private val mutableCutout = MutableStateFlow(CutoutSnapshot())
    val cutout = mutableCutout.asStateFlow()

    private val mutableAppInForeground = MutableStateFlow(false)
    val appInForeground = mutableAppInForeground.asStateFlow()

    private val foregroundActivityTokens = LinkedHashSet<Int>()

    private val mutableIntroVisible = MutableStateFlow(false)
    val introVisible = mutableIntroVisible.asStateFlow()

    private val mutableAccessibilityRunning = MutableStateFlow(false)
    val accessibilityRunning = mutableAccessibilityRunning.asStateFlow()

    private val mutableRefreshSignal = MutableStateFlow(0L)
    val refreshSignal = mutableRefreshSignal.asStateFlow()

    @Volatile
    var actions: DynamicIslandActions? = null

    fun notifyRefresh() {
        mutableRefreshSignal.value = mutableRefreshSignal.value + 1L
    }

    fun publishMedia(state: DynamicIslandMediaState) {
        mutableMedia.value = state
    }

    fun clearMedia() {
        mutableMedia.value = DynamicIslandMediaState()
    }

    fun setAppInForeground(foreground: Boolean) {
        synchronized(foregroundActivityTokens) {
            if (!foreground) foregroundActivityTokens.clear()
            mutableAppInForeground.value = foreground
        }
    }

    fun onActivityStarted(token: Int) {
        synchronized(foregroundActivityTokens) {
            foregroundActivityTokens.add(token)
            mutableAppInForeground.value = true
        }
    }

    fun onActivityStopped(token: Int, changingConfigurations: Boolean) {
        synchronized(foregroundActivityTokens) {
            foregroundActivityTokens.remove(token)
            if (!changingConfigurations) {
                mutableAppInForeground.value = foregroundActivityTokens.isNotEmpty()
            }
        }
    }

    fun onActivityDestroyed(token: Int, changingConfigurations: Boolean) {
        synchronized(foregroundActivityTokens) {
            foregroundActivityTokens.remove(token)
            if (!changingConfigurations && foregroundActivityTokens.isEmpty()) {
                mutableAppInForeground.value = false
            }
        }
    }

    fun setIntroVisible(visible: Boolean) {
        mutableIntroVisible.value = visible
    }

    fun setAccessibilityRunning(running: Boolean) {
        mutableAccessibilityRunning.value = running
    }

    fun updateCutoutFromInsets(context: Context, insets: WindowInsets?) {
        if (insets == null) return
        val app = context.applicationContext
        val wm = (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)
            ?: (app.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)
        if (Build.VERSION.SDK_INT >= 30 && wm != null) {
            // WindowInsets on floating/split windows can be translated or null; prefer display-level WindowMetrics.
            val metrics = wm.maximumWindowMetrics
            val widthPx = metrics.bounds.width()
            val isPortrait = metrics.bounds.height() >= widthPx
            val metricBar = metrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.statusBars()).top
            val statusBarHeight = if (metricBar > 0) metricBar else readStatusBarHeight(app, insets)
            val cutout = extractTopCutout(metrics.windowInsets.displayCutout, statusBarHeight)
                ?: extractTopCutout(insets.displayCutout, statusBarHeight)
            updateCutoutSnapshot(app, isPortrait, cutout, statusBarHeight, widthPx)
            return
        }
        val dm = app.resources.displayMetrics
        val isPortrait = dm.heightPixels >= dm.widthPixels
        val statusBarHeight = readStatusBarHeight(app, insets)
        val cutout = if (Build.VERSION.SDK_INT >= 28) extractTopCutout(insets.displayCutout, statusBarHeight) else null
        updateCutoutSnapshot(app, isPortrait, cutout, statusBarHeight, dm.widthPixels)
    }

    fun refreshFromWindowManager(context: Context) {
        val app = context.applicationContext
        val wm = (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)
            ?: (app.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)
        val dm = app.resources.displayMetrics
        var isPortrait = dm.heightPixels >= dm.widthPixels
        var statusBarHeight = readStatusBarHeight(app, null)
        var topCutout: IslandRect? = null
        var widthPx = dm.widthPixels

        if (Build.VERSION.SDK_INT >= 30 && wm != null) {
            val metrics = wm.maximumWindowMetrics
            widthPx = metrics.bounds.width()
            isPortrait = metrics.bounds.height() >= widthPx
            val insets = metrics.windowInsets
            val bar = insets.getInsetsIgnoringVisibility(WindowInsets.Type.statusBars()).top
            if (bar > 0) statusBarHeight = bar
            topCutout = extractTopCutout(insets.displayCutout, statusBarHeight)
        } else if (Build.VERSION.SDK_INT >= 29 && wm != null) {
            @Suppress("DEPRECATION")
            topCutout = extractTopCutout(wm.defaultDisplay?.cutout, statusBarHeight)
        }
        updateCutoutSnapshot(app, isPortrait, topCutout, statusBarHeight, widthPx)
    }

    fun restoreCachedCutout(context: Context) {
        if (mutableCutout.value.portraitCutout != null && mutableCutout.value.statusBarHeightPx > 0) return
        val prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val left = prefs.getInt("left", -1)
        val top = prefs.getInt("top", -1)
        val right = prefs.getInt("right", -1)
        val bottom = prefs.getInt("bottom", -1)
        val statusBar = prefs.getInt("status_bar", 0)
        val screenWidth = prefs.getInt("screen_width", 0)
        val rect = normalizeTopCutoutBounds(left, top, right, bottom)
        mutableCutout.value = CutoutSnapshot(rect, statusBar, screenWidth)
    }

    private fun updateCutoutSnapshot(
        app: Context,
        isPortrait: Boolean,
        cutout: IslandRect?,
        statusBarHeight: Int,
        screenWidthPx: Int,
    ) {
        val existing = mutableCutout.value
        val portraitCutout = if (isPortrait) cutout ?: existing.portraitCutout else existing.portraitCutout
        val bar = if (statusBarHeight > 0) statusBarHeight else existing.statusBarHeightPx
        val width = if (isPortrait && screenWidthPx > 0) screenWidthPx else existing.screenWidthPx
        val next = CutoutSnapshot(portraitCutout, bar, width)
        if (next != existing) {
            mutableCutout.value = next
            if (isPortrait && portraitCutout != null) {
                app.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
                    .putInt("left", portraitCutout.left)
                    .putInt("top", portraitCutout.top)
                    .putInt("right", portraitCutout.right)
                    .putInt("bottom", portraitCutout.bottom)
                    .putInt("status_bar", bar)
                    .putInt("screen_width", width)
                    .apply()
            }
        }
    }

    private fun extractTopCutout(cutout: DisplayCutout?, statusBarHeight: Int): IslandRect? {
        if (Build.VERSION.SDK_INT < 28 || cutout == null) return null
        val rect: Rect? = if (Build.VERSION.SDK_INT >= 29) {
            cutout.boundingRectTop.takeIf { !it.isEmpty }
        } else {
            cutout.boundingRects.firstOrNull { !it.isEmpty && it.top <= statusBarHeight + 24 }
        }
        return normalizeTopCutout(rect)
    }

    internal fun normalizeTopCutout(rect: Rect?): IslandRect? =
        if (rect == null) null else normalizeTopCutoutBounds(rect.left, rect.top, rect.right, rect.bottom)

    internal fun normalizeTopCutoutBounds(left: Int, top: Int, right: Int, bottom: Int): IslandRect? =
        if (left >= 0 && top >= 0 && right > left && bottom > top) IslandRect(left, top, right, bottom) else null

    private fun readStatusBarHeight(context: Context, insets: WindowInsets?): Int {
        if (Build.VERSION.SDK_INT >= 30 && insets != null) {
            val top = insets.getInsetsIgnoringVisibility(WindowInsets.Type.statusBars()).top
            if (top > 0) return top
        } else if (insets != null) {
            @Suppress("DEPRECATION")
            val top = insets.systemWindowInsetTop
            if (top > 0) return top
        }
        val id = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) context.resources.getDimensionPixelSize(id) else 0
    }
}
