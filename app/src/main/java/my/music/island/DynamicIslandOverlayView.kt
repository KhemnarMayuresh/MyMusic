package my.music.island

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewOutlineProvider
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import my.music.R
import my.music.playback.PlayerAudioSignal
import my.music.widgets.WidgetArtwork
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal class DynamicIslandOverlayView(
    context: Context,
    private val onToggleExpand: (Boolean) -> Unit,
    private val onDismiss: () -> Unit,
    private val onOpenApp: () -> Unit,
    private val onPrevious: () -> Unit,
    private val onPlayPause: () -> Unit,
    private val onNext: () -> Unit,
    private val onSeekTo: (Long) -> Unit,
    private val onBoundsProgress: (x: Int, y: Int, width: Int, height: Int) -> Unit,
) : FrameLayout(context) {

    private val density = context.resources.displayMetrics.density
    private fun dp(value: Float): Int = (value * density).roundToInt()
    private fun dpf(value: Float): Float = value * density

    private val backgroundDrawable = GradientDrawable().apply {
        setColor(Color.BLACK)
        setStroke(dp(1f).coerceAtLeast(1), 0x2EFFFFFF)
        cornerRadius = dpf(20f)
    }
    private var currentCornerRadius = dpf(20f)

    private val collapsedGroup = FrameLayout(context)
    private val collapsedArt = IslandArtworkView(context, circle = true)
    private val collapsedViz = IslandVisualizerView(context, barCount = 6)

    private val expandedGroup = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        visibility = GONE
        alpha = 0f
    }
    private val expandedArt = IslandArtworkView(context, circle = false)
    private val titleView = TextView(context).apply {
        setTextColor(Color.WHITE)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        typeface = Typeface.DEFAULT_BOLD
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
    }
    private val artistView = TextView(context).apply {
        setTextColor(0xFFB3B3C0.toInt())
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
    }
    private val expandedViz = IslandVisualizerView(context, barCount = 6)
    private val elapsedView = TextView(context).apply {
        setTextColor(0xFFD0D0DC.toInt())
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        fontFeatureSettings = "tnum"
        text = "0:00"
    }
    private val remainingView = TextView(context).apply {
        setTextColor(0xFFD0D0DC.toInt())
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        fontFeatureSettings = "tnum"
        text = "-0:00"
    }
    private val seekBar = IslandScrubberView(context) { pos ->
        draggingPositionMs = null
        onSeekTo(pos)
    }
    private val prevBtn = transportButton(R.drawable.ic_widget_previous, "Previous", 26f, onPrevious)
    private val playPauseBtn = transportButton(R.drawable.ic_widget_play, "Play or pause", 32f, onPlayPause)
    private val nextBtn = transportButton(R.drawable.ic_widget_next, "Next", 26f, onNext)

    private var expanded = false
    private var animator: ValueAnimator? = null
    val isAnimating: Boolean get() = animator?.isRunning == true
    private var lastArtworkUri: String? = null
    private var lastTitle: String? = null
    private var lastArtist: String? = null
    private var lastPlayPauseRes: Int = 0
    private var geometryApplied = false
    private var currentMedia = DynamicIslandMediaState()
    private var draggingPositionMs: Long? = null
    private var currentCollapsed = CollapsedLayout(0, 0, dp(146f), dp(36f), CutoutPlacement.NONE, 0, 0, dp(6f), dp(26f), dp(116f), dp(24f))
    private var currentExpanded = ExpandedLayout(0, 0, dp(340f), dp(182f), dp(14f))

    private var downX = 0f
    private var downY = 0f
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    init {
        background = backgroundDrawable
        elevation = dpf(8f)
        clipToOutline = true
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, currentCornerRadius)
            }
        }

        collapsedGroup.addView(collapsedArt)
        collapsedGroup.addView(collapsedViz)
        addView(collapsedGroup, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val openClick = OnClickListener { onOpenApp() }
        expandedArt.setOnClickListener(openClick)
        header.addView(expandedArt, LinearLayout.LayoutParams(dp(50f), dp(50f)))

        val textCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12f), 0, dp(10f), 0)
        }
        textCol.setOnClickListener(openClick)
        textCol.addView(titleView)
        textCol.addView(artistView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(2f)
        })
        header.addView(textCol, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        expandedViz.setPadding(dp(4f), dp(4f), dp(4f), dp(4f))
        expandedViz.setOnClickListener { onToggleExpand(false) }
        header.addView(expandedViz, LinearLayout.LayoutParams(dp(34f), dp(28f)))

        expandedGroup.addView(header, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        val timelineRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        timelineRow.addView(elapsedView)
        seekBar.onScrubPreview = { previewMs ->
            draggingPositionMs = previewMs
            updateTimes(previewMs, currentMedia.durationMs)
        }
        timelineRow.addView(seekBar, LinearLayout.LayoutParams(0, dp(28f), 1f).apply {
            marginStart = dp(10f)
            marginEnd = dp(10f)
        })
        timelineRow.addView(remainingView)
        expandedGroup.addView(timelineRow, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(12f)
        })

        val controlsRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            clipChildren = false
            clipToPadding = false
        }
        controlsRow.addView(prevBtn, LinearLayout.LayoutParams(dp(56f), dp(48f)))
        controlsRow.addView(playPauseBtn, LinearLayout.LayoutParams(dp(68f), dp(48f)).apply {
            marginStart = dp(16f)
            marginEnd = dp(16f)
        })
        controlsRow.addView(nextBtn, LinearLayout.LayoutParams(dp(56f), dp(48f)))
        expandedGroup.addView(controlsRow, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(8f)
        })

        expandedGroup.clipChildren = false
        expandedGroup.clipToPadding = false
        addView(expandedGroup, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    private fun transportButton(drawableRes: Int, desc: String, iconDp: Float, click: () -> Unit): ImageButton =
        ImageButton(context).apply {
            setImageResource(drawableRes)
            setColorFilter(Color.WHITE)
            val outValue = TypedValue()
            if (context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)) {
                setBackgroundResource(outValue.resourceId)
            } else {
                setBackgroundColor(Color.TRANSPARENT)
            }
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            val pad = dp((48f - iconDp) / 2f).coerceAtLeast(dp(6f))
            setPadding(pad, pad, pad, pad)
            contentDescription = desc
            isFocusable = false
            isClickable = true
            setOnTouchListener { v, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                }
                false
            }
            setOnClickListener { click() }
        }

    fun applyGeometry(collapsed: CollapsedLayout, expandedLayout: ExpandedLayout) {
        if (geometryApplied && currentCollapsed == collapsed && currentExpanded == expandedLayout) return
        geometryApplied = true
        currentCollapsed = collapsed
        currentExpanded = expandedLayout
        positionCollapsedChildren(collapsed)
        expandedGroup.setPadding(dp(16f), expandedLayout.topSafePaddingPx, dp(16f), dp(16f))
        if (!expanded && animator?.isRunning != true) {
            currentCornerRadius = collapsed.height / 2f
            backgroundDrawable.cornerRadius = currentCornerRadius
            invalidateOutline()
        } else if (expanded && animator?.isRunning != true) {
            currentCornerRadius = dpf(28f)
            backgroundDrawable.cornerRadius = currentCornerRadius
            invalidateOutline()
        }
    }

    private fun positionCollapsedChildren(layout: CollapsedLayout) {
        val artY = (layout.height - layout.artSize) / 2
        collapsedArt.layoutParams = LayoutParams(layout.artSize, layout.artSize).apply {
            leftMargin = layout.artLeft
            topMargin = artY
        }
        val vizHeight = (layout.height * 0.56f).roundToInt().coerceIn(dp(12f), dp(24f))
        collapsedViz.layoutParams = LayoutParams(layout.vizWidth, vizHeight).apply {
            leftMargin = layout.vizLeft
            topMargin = (layout.height - vizHeight) / 2
        }
    }

    fun updateMedia(state: DynamicIslandMediaState) {
        currentMedia = state
        val nextTitle = state.title.ifBlank { "My Music" }
        if (nextTitle != lastTitle) {
            lastTitle = nextTitle
            titleView.text = nextTitle
        }
        val nextArtist = state.artist.ifBlank { "Local audio" }
        if (nextArtist != lastArtist) {
            lastArtist = nextArtist
            artistView.text = nextArtist
        }
        val nextPlayRes = if (state.playing) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
        if (nextPlayRes != lastPlayPauseRes) {
            lastPlayPauseRes = nextPlayRes
            playPauseBtn.setImageResource(nextPlayRes)
        }
        collapsedViz.setPlaying(state.playing)
        expandedViz.setPlaying(state.playing)

        val shownPos = draggingPositionMs ?: state.positionMs
        seekBar.update(shownPos, state.durationMs, dragging = draggingPositionMs != null)
        updateTimes(shownPos, state.durationMs)

        if (state.artworkUri != lastArtworkUri) {
            lastArtworkUri = state.artworkUri
            if (state.artworkUri == null) {
                collapsedArt.setBitmap(null)
                expandedArt.setBitmap(null)
            } else {
                val targetUri = state.artworkUri
                WidgetArtwork.request(context.applicationContext, targetUri) { bmp ->
                    if (lastArtworkUri == targetUri) {
                        collapsedArt.setBitmap(bmp)
                        expandedArt.setBitmap(bmp)
                    }
                }
            }
        }
    }

    private fun updateTimes(positionMs: Long, durationMs: Long) {
        val nextElapsed = formatTime(positionMs)
        if (elapsedView.text != nextElapsed) elapsedView.text = nextElapsed
        val rem = (durationMs - positionMs).coerceAtLeast(0L)
        val nextRemaining = "-${formatTime(rem)}"
        if (remainingView.text != nextRemaining) remainingView.text = nextRemaining
    }

    fun setExpanded(expand: Boolean, animate: Boolean) {
        if (expanded == expand && animator?.isRunning != true) return
        expanded = expand
        animator?.cancel()

        val startX = if (expand) currentCollapsed.x else currentExpanded.x
        val startY = if (expand) currentCollapsed.y else currentExpanded.y
        val startW = if (expand) currentCollapsed.width else currentExpanded.width
        val startH = if (expand) currentCollapsed.height else currentExpanded.height
        val startR = if (expand) currentCollapsed.height / 2f else dpf(28f)

        val endX = if (expand) currentExpanded.x else currentCollapsed.x
        val endY = if (expand) currentExpanded.y else currentCollapsed.y
        val endW = if (expand) currentExpanded.width else currentCollapsed.width
        val endH = if (expand) currentExpanded.height else currentCollapsed.height
        val endR = if (expand) dpf(28f) else currentCollapsed.height / 2f

        if (!animate) {
            collapsedGroup.visibility = if (expand) GONE else VISIBLE
            collapsedGroup.alpha = if (expand) 0f else 1f
            expandedGroup.visibility = if (expand) VISIBLE else GONE
            expandedGroup.alpha = if (expand) 1f else 0f
            currentCornerRadius = endR
            backgroundDrawable.cornerRadius = endR
            invalidateOutline()
            onBoundsProgress(endX, endY, endW, endH)
            return
        }

        collapsedGroup.visibility = VISIBLE
        expandedGroup.visibility = VISIBLE

        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 250L
            interpolator = PathInterpolator(0.22f, 1f, 0.36f, 1f)
            addUpdateListener { anim ->
                val t = anim.animatedValue as Float
                val x = (startX + (endX - startX) * t).roundToInt()
                val y = (startY + (endY - startY) * t).roundToInt()
                val w = (startW + (endW - startW) * t).roundToInt().coerceAtLeast(1)
                val h = (startH + (endH - startH) * t).roundToInt().coerceAtLeast(1)
                currentCornerRadius = startR + (endR - startR) * t
                backgroundDrawable.cornerRadius = currentCornerRadius
                invalidateOutline()

                if (expand) {
                    collapsedGroup.alpha = (1f - t * 2.4f).coerceIn(0f, 1f)
                    expandedGroup.alpha = ((t - 0.35f) / 0.65f).coerceIn(0f, 1f)
                } else {
                    expandedGroup.alpha = (1f - t * 2.4f).coerceIn(0f, 1f)
                    collapsedGroup.alpha = ((t - 0.35f) / 0.65f).coerceIn(0f, 1f)
                }
                onBoundsProgress(x, y, w, h)
                if (t >= 1f) {
                    collapsedGroup.visibility = if (expand) GONE else VISIBLE
                    expandedGroup.visibility = if (expand) VISIBLE else GONE
                }
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    collapsedGroup.visibility = if (expand) GONE else VISIBLE
                    expandedGroup.visibility = if (expand) VISIBLE else GONE
                    collapsedGroup.alpha = if (expand) 0f else 1f
                    expandedGroup.alpha = if (expand) 1f else 0f
                    currentCornerRadius = endR
                    backgroundDrawable.cornerRadius = endR
                    invalidateOutline()
                    onBoundsProgress(endX, endY, endW, endH)
                }
            })
            start()
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_OUTSIDE) {
            if (expanded) onToggleExpand(false)
            return true
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_OUTSIDE) {
            if (expanded) onToggleExpand(false)
            return false
        }
        if (!expanded) return true
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.rawX
                downY = ev.rawY
            }
            MotionEvent.ACTION_MOVE -> {
                val dy = ev.rawY - downY
                val dx = ev.rawX - downX
                if (dy < -dp(30f) && abs(dy) > abs(dx) && !seekBar.isDragging) {
                    return true
                }
            }
        }
        return false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_OUTSIDE) {
            if (expanded) onToggleExpand(false)
            return true
        }
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                downY = event.rawY
                return true
            }
            MotionEvent.ACTION_UP -> {
                val dx = event.rawX - downX
                val dy = event.rawY - downY
                if (!expanded) {
                    if (abs(dx) > dp(42f) && abs(dx) > abs(dy)) {
                        onDismiss()
                    } else if (abs(dx) <= touchSlop && abs(dy) <= touchSlop) {
                        onToggleExpand(true)
                    }
                } else if (dy < -dp(30f) && abs(dy) > abs(dx)) {
                    onToggleExpand(false)
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        super.onDetachedFromWindow()
    }

    companion object {
        fun formatTime(ms: Long): String {
            val seconds = (ms / 1000L).coerceAtLeast(0L)
            return if (seconds >= 3600L) {
                "%d:%02d:%02d".format(seconds / 3600L, (seconds % 3600L) / 60L, seconds % 60L)
            } else {
                "%d:%02d".format(seconds / 60L, seconds % 60L)
            }
        }
    }
}

internal class IslandArtworkView(context: Context, private val circle: Boolean) : View(context) {
    private var bitmap: Bitmap? = null
    private val clipPath = Path()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val rect = RectF()
    private var gradientShader: Shader? = null
    private var lastW = 0
    private var lastH = 0
    private val noteDrawable = context.getDrawable(R.drawable.ic_island_note)?.mutate()

    fun setBitmap(bmp: Bitmap?) {
        bitmap = bmp
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w != lastW || h != lastH) {
            lastW = w
            lastH = h
            gradientShader = LinearGradient(
                0f, 0f, w.toFloat(), h.toFloat(),
                intArrayOf(0xFFEE628A.toInt(), 0xFF8E6EB8.toInt(), 0xFF376BBB.toInt()),
                null,
                Shader.TileMode.CLAMP,
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (width <= 0 || height <= 0) return
        rect.set(0f, 0f, width.toFloat(), height.toFloat())
        val radius = if (circle) min(width, height) / 2f else 12f * resources.displayMetrics.density
        clipPath.reset()
        clipPath.addRoundRect(rect, radius, radius, Path.Direction.CW)
        val save = canvas.save()
        canvas.clipPath(clipPath)
        val bmp = bitmap
        if (bmp != null && !bmp.isRecycled) {
            val scale = max(width.toFloat() / bmp.width, height.toFloat() / bmp.height)
            val dw = bmp.width * scale
            val dh = bmp.height * scale
            val dx = (width - dw) / 2f
            val dy = (height - dh) / 2f
            val dst = RectF(dx, dy, dx + dw, dy + dh)
            paint.shader = null
            canvas.drawBitmap(bmp, null, dst, paint)
        } else {
            paint.shader = gradientShader
            canvas.drawRect(rect, paint)
            paint.shader = null
            noteDrawable?.let { icon ->
                val inset = (min(width, height) * 0.25f).roundToInt()
                icon.setTint(0xD9FFFFFF.toInt())
                icon.setBounds(inset, inset, width - inset, height - inset)
                icon.draw(canvas)
            }
        }
        canvas.restoreToCount(save)
    }
}

internal class IslandVisualizerView(context: Context, private val barCount: Int = 6) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val levels = FloatArray(barCount) { 0.22f }
    private val barRect = RectF()
    private var playing = false
    private var running = false

    private val frameTick = object : Runnable {
        override fun run() {
            if (!running || !isAttachedToWindow) return
            val signal = PlayerAudioSignal.active?.snapshot()
            val hasAudio = signal?.any { it > 0.01f } == true
            val t = System.nanoTime() / 1_000_000_000.0
            for (i in 0 until barCount) {
                val target = if (!playing) {
                    0.18f
                } else if (signal != null && hasAudio) {
                    val idx = ((i * (signal.size - 1)) / (barCount - 1).coerceAtLeast(1)).coerceIn(0, signal.lastIndex)
                    (0.22f + signal[idx] * 0.85f).coerceIn(0.2f, 1f)
                } else {
                    (0.32f + 0.35f * kotlin.math.abs(kotlin.math.sin(t * 4.5 + i * 0.85)).toFloat()).coerceIn(0.2f, 0.95f)
                }
                levels[i] += (target - levels[i]) * 0.35f
            }
            invalidate()
            if (playing) {
                postOnAnimation(this)
            } else {
                val settled = levels.all { abs(it - 0.18f) < 0.02f }
                if (!settled) postOnAnimation(this) else running = false
            }
        }
    }

    fun setPlaying(isPlaying: Boolean) {
        if (playing == isPlaying && (running || !isPlaying)) return
        playing = isPlaying
        paint.alpha = if (isPlaying) 255 else 150
        startLoopIfNeeded()
    }

    private fun startLoopIfNeeded() {
        if (!running && isAttachedToWindow) {
            running = true
            postOnAnimation(frameTick)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (playing) startLoopIfNeeded()
    }

    override fun onDetachedFromWindow() {
        running = false
        removeCallbacks(frameTick)
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat() - paddingLeft - paddingRight
        val h = height.toFloat() - paddingTop - paddingBottom
        if (w <= 0f || h <= 0f) return
        val slotW = w / barCount
        val barW = (slotW * 0.48f).coerceAtLeast(1.5f * resources.displayMetrics.density)
        val radius = barW / 2f
        val cy = paddingTop + h / 2f
        for (i in 0 until barCount) {
            val barH = (h * levels[i]).coerceIn(barW, h)
            val left = paddingLeft + slotW * i + (slotW - barW) / 2f
            barRect.set(left, cy - barH / 2f, left + barW, cy + barH / 2f)
            canvas.drawRoundRect(barRect, radius, radius, paint)
        }
    }
}

internal class IslandScrubberView(
    context: Context,
    private val onCommit: (Long) -> Unit,
) : View(context) {
    private val density = context.resources.displayMetrics.density
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x3DFFFFFF }
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF70A9CE.toInt() }
    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF7BB7DD.toInt() }
    private val rect = RectF()

    var onScrubPreview: ((Long) -> Unit)? = null
    var isDragging = false; private set

    private var positionMs = 0L
    private var durationMs = 0L
    private var dragPosMs = 0L

    fun update(pos: Long, duration: Long, dragging: Boolean) {
        if (!dragging && !isDragging) {
            val nextPos = pos.coerceAtLeast(0L)
            val nextDur = duration.coerceAtLeast(0L)
            if (nextPos != positionMs || nextDur != durationMs) {
                positionMs = nextPos
                durationMs = nextDur
                invalidate()
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (durationMs <= 0L) return false
        parent?.requestDisallowInterceptTouchEvent(true)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isDragging = true
                dragPosMs = posForX(event.x)
                onScrubPreview?.invoke(dragPosMs)
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    dragPosMs = posForX(event.x)
                    onScrubPreview?.invoke(dragPosMs)
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (isDragging) {
                    val finalPos = posForX(event.x)
                    isDragging = false
                    positionMs = finalPos
                    onCommit(finalPos)
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun posForX(x: Float): Long {
        val pad = 6f * density
        val usable = (width - pad * 2).coerceAtLeast(1f)
        val fraction = ((x - pad) / usable).coerceIn(0f, 1f)
        return (durationMs * fraction).toLong()
    }

    override fun onDraw(canvas: Canvas) {
        val pad = 6f * density
        val trackH = 3.5f * density
        val thumbR = if (isDragging) 6.5f * density else 5.5f * density
        val cy = height / 2f
        val left = pad
        val right = (width - pad).coerceAtLeast(left + 1f)
        val activePos = if (isDragging) dragPosMs else positionMs
        val fraction = if (durationMs > 0L) (activePos.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
        val fillX = left + (right - left) * fraction

        rect.set(left, cy - trackH / 2f, right, cy + trackH / 2f)
        canvas.drawRoundRect(rect, trackH / 2f, trackH / 2f, trackPaint)

        rect.set(left, cy - trackH / 2f, fillX, cy + trackH / 2f)
        canvas.drawRoundRect(rect, trackH / 2f, trackH / 2f, progressPaint)

        canvas.drawCircle(fillX, cy, thumbR, thumbPaint)
    }
}
