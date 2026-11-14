package my.music.island

import my.music.collections.CollectionState
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class IslandRect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = (right - left).coerceAtLeast(0)
    val height: Int get() = (bottom - top).coerceAtLeast(0)
    val centerX: Int get() = left + width / 2
    val centerY: Int get() = top + height / 2
}

enum class CutoutPlacement { CENTER, LEFT, RIGHT, NONE }

data class IslandConfig(
    val enabled: Boolean = false,
    val showPaused: Boolean = false,
    val hideInPlayer: Boolean = true,
    val offsetXDp: Int = 0,
    val offsetYDp: Int = 0,
    val widthDeltaDp: Int = 0,
    val heightDeltaDp: Int = 0,
) {
    companion object {
        const val KEY_ENABLED = "dynamic_island_enabled"
        const val KEY_PROMPTED = "dynamic_island_prompted"
        const val KEY_SHOW_PAUSED = "dynamic_island_show_paused"
        const val KEY_HIDE_IN_PLAYER = "dynamic_island_hide_in_player"
        const val KEY_OFFSET_X = "dynamic_island_offset_x"
        const val KEY_OFFSET_Y = "dynamic_island_offset_y"
        const val KEY_WIDTH_DELTA = "dynamic_island_width_delta"
        const val KEY_HEIGHT_DELTA = "dynamic_island_height_delta"

        fun from(state: CollectionState): IslandConfig = IslandConfig(
            enabled = state.flag(KEY_ENABLED, false),
            showPaused = state.flag(KEY_SHOW_PAUSED, false),
            hideInPlayer = state.flag(KEY_HIDE_IN_PLAYER, true),
            offsetXDp = (state.pref(KEY_OFFSET_X, "0").toIntOrNull() ?: 0).coerceIn(-48, 48),
            offsetYDp = (state.pref(KEY_OFFSET_Y, "0").toIntOrNull() ?: 0).coerceIn(-48, 48),
            widthDeltaDp = (state.pref(KEY_WIDTH_DELTA, "0").toIntOrNull() ?: 0).coerceIn(-32, 64),
            heightDeltaDp = (state.pref(KEY_HEIGHT_DELTA, "0").toIntOrNull() ?: 0).coerceIn(-10, 20),
        )
    }
}

data class CollapsedLayout(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val placement: CutoutPlacement,
    val cameraGapLeft: Int,
    val cameraGapWidth: Int,
    val artLeft: Int,
    val artSize: Int,
    val vizLeft: Int,
    val vizWidth: Int,
)

data class ExpandedLayout(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val topSafePaddingPx: Int,
)

object DynamicIslandGeometry {
    private fun dp(density: Float, value: Int): Int = (value * density).roundToInt()

    fun computeCollapsed(
        screenWidthPx: Int,
        statusBarHeightPx: Int,
        cutoutRect: IslandRect?,
        isLandscape: Boolean,
        mode: IslandRenderMode,
        config: IslandConfig,
        density: Float,
    ): CollapsedLayout {
        val safeWidth = screenWidthPx.coerceAtLeast(dp(density, 200))
        val maxPillWidth = (safeWidth - dp(density, 16)).coerceAtLeast(dp(density, 120))
        val offsetX = dp(density, config.offsetXDp)
        val offsetY = dp(density, config.offsetYDp)
        val widthDelta = dp(density, config.widthDeltaDp)
        val heightDelta = dp(density, config.heightDeltaDp)

        val pad = dp(density, 6)
        val gapMargin = dp(density, 4)
        val vizWidth = dp(density, 24)

        val cutout = if (mode == IslandRenderMode.CUTOUT_ACCESSIBILITY && !isLandscape && cutoutRect != null && cutoutRect.width > 0 && cutoutRect.height > 0) {
            cutoutRect
        } else null

        if (cutout == null) {
            val height = (dp(density, 36) + heightDelta).coerceIn(dp(density, 22), dp(density, 64))
            val artSize = (height - dp(density, 8)).coerceAtLeast(dp(density, 16))
            val minWidth = pad * 2 + artSize + dp(density, 24) + vizWidth
            val width = (dp(density, 146) + widthDelta).coerceIn(minWidth, maxPillWidth)
            val x = ((safeWidth - width) / 2 + offsetX).coerceIn(dp(density, 8), (safeWidth - width - dp(density, 8)).coerceAtLeast(dp(density, 8)))
            val baseY = if (mode == IslandRenderMode.OVERLAY_BELOW_STATUS_BAR) statusBarHeightPx + dp(density, 6) else dp(density, 6)
            val y = (baseY + offsetY).coerceAtLeast(0)
            return CollapsedLayout(
                x = x,
                y = y,
                width = width,
                height = height,
                placement = CutoutPlacement.NONE,
                cameraGapLeft = 0,
                cameraGapWidth = 0,
                artLeft = pad,
                artSize = artSize,
                vizLeft = width - pad - vizWidth,
                vizWidth = vizWidth,
            )
        }

        val height = (max(cutout.height + dp(density, 8), dp(density, 34)) + heightDelta)
            .coerceIn(dp(density, 22), dp(density, 64))
        val artSize = (height - dp(density, 8)).coerceAtLeast(dp(density, 16))
        val baseY = (cutout.centerY - height / 2).coerceAtLeast(dp(density, 2))
        val y = (baseY + offsetY).coerceAtLeast(0)
        val gapWidth = cutout.width + gapMargin * 2

        val centerDistance = abs(cutout.centerX - safeWidth / 2)
        val placement = when {
            centerDistance <= cutout.width + dp(density, 28) -> CutoutPlacement.CENTER
            cutout.centerX < safeWidth / 2 -> CutoutPlacement.LEFT
            else -> CutoutPlacement.RIGHT
        }

        return when (placement) {
            CutoutPlacement.CENTER -> {
                val baseWing = max(pad + artSize + gapMargin, gapMargin + vizWidth + pad)
                val wing = (baseWing + widthDelta / 2).coerceAtLeast(baseWing)
                val width = (wing * 2 + gapWidth).coerceIn(baseWing * 2 + gapWidth, maxPillWidth)
                val x = (cutout.centerX - width / 2 + offsetX)
                    .coerceIn(dp(density, 8), (safeWidth - width - dp(density, 8)).coerceAtLeast(dp(density, 8)))
                val gapLeft = (cutout.left - gapMargin - x)
                    .coerceIn(pad + artSize + dp(density, 2), (width - pad - vizWidth - gapWidth - dp(density, 2)).coerceAtLeast(pad + artSize + dp(density, 2)))
                CollapsedLayout(
                    x = x,
                    y = y,
                    width = width,
                    height = height,
                    placement = CutoutPlacement.CENTER,
                    cameraGapLeft = gapLeft,
                    cameraGapWidth = gapWidth,
                    artLeft = pad,
                    artSize = artSize,
                    vizLeft = width - pad - vizWidth,
                    vizWidth = vizWidth,
                )
            }
            CutoutPlacement.LEFT -> {
                val minRightContent = dp(density, 6) + artSize + dp(density, 8) + vizWidth + pad
                val baseWidth = gapMargin + cutout.width + gapMargin + minRightContent
                val width = (baseWidth + widthDelta).coerceIn(baseWidth, maxPillWidth)
                val x = (cutout.left - gapMargin + offsetX)
                    .coerceIn(dp(density, 4), (safeWidth - width - dp(density, 8)).coerceAtLeast(dp(density, 4)))
                val gapLeft = (cutout.left - gapMargin - x).coerceAtLeast(dp(density, 2))
                val artLeft = gapLeft + gapWidth + dp(density, 4)
                CollapsedLayout(
                    x = x,
                    y = y,
                    width = width,
                    height = height,
                    placement = CutoutPlacement.LEFT,
                    cameraGapLeft = gapLeft,
                    cameraGapWidth = gapWidth,
                    artLeft = artLeft,
                    artSize = artSize,
                    vizLeft = width - pad - vizWidth,
                    vizWidth = vizWidth,
                )
            }
            CutoutPlacement.RIGHT -> {
                val minLeftContent = pad + artSize + dp(density, 8) + vizWidth + dp(density, 6)
                val baseWidth = minLeftContent + gapMargin + cutout.width + gapMargin
                val width = (baseWidth + widthDelta).coerceIn(baseWidth, maxPillWidth)
                val x = (cutout.right + gapMargin - width + offsetX)
                    .coerceIn(dp(density, 8), (safeWidth - width - dp(density, 4)).coerceAtLeast(dp(density, 8)))
                val gapLeft = (cutout.left - gapMargin - x).coerceAtLeast(minLeftContent)
                val vizLeft = (gapLeft - dp(density, 6) - vizWidth).coerceAtLeast(pad + artSize + dp(density, 4))
                CollapsedLayout(
                    x = x,
                    y = y,
                    width = width,
                    height = height,
                    placement = CutoutPlacement.RIGHT,
                    cameraGapLeft = gapLeft,
                    cameraGapWidth = gapWidth,
                    artLeft = pad,
                    artSize = artSize,
                    vizLeft = vizLeft,
                    vizWidth = vizWidth,
                )
            }
            CutoutPlacement.NONE -> error("Unreachable")
        }
    }

    fun computeExpanded(
        screenWidthPx: Int,
        statusBarHeightPx: Int,
        cutoutRect: IslandRect?,
        isLandscape: Boolean,
        mode: IslandRenderMode,
        config: IslandConfig,
        density: Float,
    ): ExpandedLayout {
        val safeWidth = screenWidthPx.coerceAtLeast(dp(density, 260))
        val width = min(safeWidth - dp(density, 16), dp(density, 376)).coerceAtLeast(dp(density, 240))
        val x = ((safeWidth - width) / 2).coerceAtLeast(dp(density, 8))
        val offsetY = dp(density, config.offsetYDp)
        val cutout = if (mode == IslandRenderMode.CUTOUT_ACCESSIBILITY && !isLandscape && cutoutRect != null && cutoutRect.width > 0 && cutoutRect.height > 0) {
            cutoutRect
        } else null
        val baseY = when {
            mode == IslandRenderMode.OVERLAY_BELOW_STATUS_BAR -> statusBarHeightPx + dp(density, 6)
            cutout != null -> (cutout.top - dp(density, 4)).coerceAtLeast(dp(density, 4))
            else -> dp(density, 6)
        }
        val y = (baseY + offsetY).coerceAtLeast(0)
        val topSafePaddingPx = if (cutout != null) {
            ((cutout.bottom - y) + dp(density, 10)).coerceAtLeast(dp(density, 16))
        } else {
            dp(density, 14)
        }
        val height = topSafePaddingPx + dp(density, 168)
        return ExpandedLayout(x = x, y = y, width = width, height = height, topSafePaddingPx = topSafePaddingPx)
    }
}

