package my.music.island

import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import androidx.core.content.ContextCompat
import my.music.MainActivity
import my.music.MusicApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

internal class DynamicIslandOverlayController(
    private val context: Context,
    private val mode: IslandRenderMode,
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var view: DynamicIslandOverlayView? = null
    private var params: WindowManager.LayoutParams? = null
    private var expanded = false
    private var activatedSession = false
    private var pausedTimedOut = false
    private var dismissedMediaId: String? = null
    private var lastMediaId: String? = null
    private var lastPlaying = false
    private var collectJob: Job? = null
    private var receiverRegistered = false

    private val pauseTimeout = Runnable {
        if (!expanded && !DynamicIslandBus.media.value.playing) {
            pausedTimedOut = true
            refresh()
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            refresh()
        }
    }

    fun start() {
        if (collectJob != null) return
        DynamicIslandBus.restoreCachedCutout(context)
        DynamicIslandBus.refreshFromWindowManager(context)

        if (!receiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
            ContextCompat.registerReceiver(context, screenReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
            receiverRegistered = true
        }

        val collections = (context.applicationContext as MusicApplication).collections
        collectJob = scope.launch {
            combine(
                DynamicIslandBus.media,
                collections.state,
                DynamicIslandBus.cutout,
                DynamicIslandBus.appInForeground,
                DynamicIslandBus.introVisible,
                DynamicIslandBus.accessibilityRunning,
                DynamicIslandBus.refreshSignal,
            ) { values ->
                val media = values[0] as DynamicIslandMediaState
                val colState = values[1] as my.music.collections.CollectionState
                media to IslandConfig.from(colState)
            }.collect { (media, config) ->
                handleMediaProgress(media, config)
                refresh(media, config)
            }
        }
    }

    fun onConfigurationChanged(@Suppress("UNUSED_PARAMETER") newConfig: Configuration) {
        DynamicIslandBus.refreshFromWindowManager(context)
        refresh()
    }

    fun stop() {
        mainHandler.removeCallbacks(pauseTimeout)
        collectJob?.cancel()
        collectJob = null
        if (receiverRegistered) {
            runCatching { context.unregisterReceiver(screenReceiver) }
            receiverRegistered = false
        }
        detachView()
        scope.cancel()
    }

    private fun handleMediaProgress(media: DynamicIslandMediaState, config: IslandConfig) {
        if (media.mediaId != lastMediaId) {
            lastMediaId = media.mediaId
            dismissedMediaId = null
            pausedTimedOut = false
        }
        if (!media.active) {
            activatedSession = false
            pausedTimedOut = false
            expanded = false
            mainHandler.removeCallbacks(pauseTimeout)
            return
        }
        if (media.playing) {
            activatedSession = true
            pausedTimedOut = false
            if (!lastPlaying) dismissedMediaId = null
            mainHandler.removeCallbacks(pauseTimeout)
        } else if (lastPlaying && activatedSession && !config.showPaused && !expanded) {
            mainHandler.removeCallbacks(pauseTimeout)
            mainHandler.postDelayed(pauseTimeout, 8_000L)
        }
        lastPlaying = media.playing
    }

    private fun refresh(
        media: DynamicIslandMediaState = DynamicIslandBus.media.value,
        config: IslandConfig = IslandConfig.from((context.applicationContext as MusicApplication).collections.state.value),
    ) {
        val suppressed = !config.enabled ||
            !media.active ||
            !activatedSession ||
            DynamicIslandBus.introVisible.value ||
            DynamicIslandBus.appInForeground.value ||
            (keyguardManager?.isKeyguardLocked == true) ||
            (dismissedMediaId != null && dismissedMediaId == media.mediaId) ||
            (!media.playing && !config.showPaused && !expanded && pausedTimedOut) ||
            (mode == IslandRenderMode.CUTOUT_ACCESSIBILITY && !DynamicIslandAccessibilityService.running) ||
            (mode == IslandRenderMode.OVERLAY_BELOW_STATUS_BAR && (DynamicIslandAccessibilityService.running || !DynamicIslandCapability.canDrawOverlays(context)))

        if (suppressed) {
            detachView()
            return
        }

        val (collapsedLayout, expandedLayout) = computeLayouts(config)
        val existing = view
        if (existing == null) {
            attachView(collapsedLayout, expandedLayout, media)
        } else {
            existing.applyGeometry(collapsedLayout, expandedLayout)
            existing.updateMedia(media)
            if (!existing.isAnimating) {
                if (!expanded) {
                    updateWindowBounds(collapsedLayout.x, collapsedLayout.y, collapsedLayout.width, collapsedLayout.height, expandFlags = false)
                } else {
                    updateWindowBounds(expandedLayout.x, expandedLayout.y, expandedLayout.width, expandedLayout.height, expandFlags = true)
                }
            }
        }
    }

    private fun computeLayouts(config: IslandConfig): Pair<CollapsedLayout, ExpandedLayout> {
        val dm = context.resources.displayMetrics
        val isLandscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val cutout = DynamicIslandBus.cutout.value
        val screenW = if (!isLandscape && cutout.screenWidthPx > 0) cutout.screenWidthPx else dm.widthPixels
        val statusBarH = if (cutout.statusBarHeightPx > 0) cutout.statusBarHeightPx else readFallbackStatusBarHeight()
        val collapsed = DynamicIslandGeometry.computeCollapsed(
            screenWidthPx = screenW,
            statusBarHeightPx = statusBarH,
            cutoutRect = cutout.portraitCutout,
            isLandscape = isLandscape,
            mode = mode,
            config = config,
            density = dm.density,
        )
        val expandedLayout = DynamicIslandGeometry.computeExpanded(
            screenWidthPx = screenW,
            statusBarHeightPx = statusBarH,
            cutoutRect = cutout.portraitCutout,
            isLandscape = isLandscape,
            mode = mode,
            config = config,
            density = dm.density,
        )
        return collapsed to expandedLayout
    }

    private fun readFallbackStatusBarHeight(): Int {
        val id = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) context.resources.getDimensionPixelSize(id) else (24f * context.resources.displayMetrics.density).toInt()
    }

    private fun attachView(
        collapsed: CollapsedLayout,
        expandedLayout: ExpandedLayout,
        media: DynamicIslandMediaState,
    ) {
        val windowType = when (mode) {
            IslandRenderMode.CUTOUT_ACCESSIBILITY -> WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            IslandRenderMode.OVERLAY_BELOW_STATUS_BAR -> {
                if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else return
            }
            else -> return
        }

        val initial = if (expanded) {
            Triple(expandedLayout.x, expandedLayout.y, expandedLayout.width to expandedLayout.height)
        } else {
            Triple(collapsed.x, collapsed.y, collapsed.width to collapsed.height)
        }

        val lp = WindowManager.LayoutParams(
            initial.third.first,
            initial.third.second,
            initial.first,
            initial.second,
            windowType,
            flagsFor(expanded),
            PixelFormat.TRANSLUCENT,
        ).apply {
            @SuppressLint("RtlHardcoded")
            val topLeft = Gravity.TOP or Gravity.LEFT
            gravity = topLeft
            if (Build.VERSION.SDK_INT >= 30) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            } else if (Build.VERSION.SDK_INT >= 28) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        val overlayView = DynamicIslandOverlayView(
            context = context,
            onToggleExpand = { expand -> toggleExpand(expand) },
            onDismiss = {
                expanded = false
                dismissedMediaId = DynamicIslandBus.media.value.mediaId ?: "__current__"
                detachView()
            },
            onOpenApp = {
                toggleExpand(false)
                val launch = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra("open_player", true)
                }
                runCatching { context.startActivity(launch) }
            },
            onPrevious = { DynamicIslandBus.actions?.previous() },
            onPlayPause = { DynamicIslandBus.actions?.togglePlayPause() },
            onNext = { DynamicIslandBus.actions?.next() },
            onSeekTo = { pos -> DynamicIslandBus.actions?.seekTo(pos) },
            onBoundsProgress = { x, y, w, h ->
                updateWindowBounds(x, y, w, h, expandFlags = expanded)
            },
        )
        overlayView.applyGeometry(collapsed, expandedLayout)
        overlayView.updateMedia(media)
        overlayView.setExpanded(expanded, animate = false)

        val attached = runCatching {
            windowManager.addView(overlayView, lp)
            true
        }.getOrDefault(false)
        if (attached) {
            view = overlayView
            params = lp
        }
    }

    private fun toggleExpand(expand: Boolean) {
        if (expanded == expand) return
        expanded = expand
        val config = IslandConfig.from((context.applicationContext as MusicApplication).collections.state.value)
        if (expand) {
            pausedTimedOut = false
            mainHandler.removeCallbacks(pauseTimeout)
        } else if (!DynamicIslandBus.media.value.playing && !config.showPaused) {
            mainHandler.removeCallbacks(pauseTimeout)
            mainHandler.postDelayed(pauseTimeout, 8_000L)
        }
        val (collapsed, expandedLayout) = computeLayouts(config)
        view?.applyGeometry(collapsed, expandedLayout)
        view?.setExpanded(expand, animate = true)
    }

    private fun flagsFor(isExpanded: Boolean): Int {
        var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        if (isExpanded) flags = flags or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        return flags
    }

    private fun updateWindowBounds(x: Int, y: Int, width: Int, height: Int, expandFlags: Boolean) {
        val currentView = view ?: return
        val lp = params ?: return
        if (!currentView.isAttachedToWindow) return
        val nextFlags = flagsFor(expandFlags)
        if (lp.x == x && lp.y == y && lp.width == width && lp.height == height && lp.flags == nextFlags) return
        lp.x = x
        lp.y = y
        lp.width = width.coerceAtLeast(1)
        lp.height = height.coerceAtLeast(1)
        lp.flags = nextFlags
        runCatching { windowManager.updateViewLayout(currentView, lp) }
    }

    private fun detachView() {
        expanded = false
        val currentView = view ?: return
        view = null
        params = null
        if (currentView.isAttachedToWindow) {
            runCatching { windowManager.removeViewImmediate(currentView) }
        }
    }
}
