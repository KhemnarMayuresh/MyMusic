package my.music

import my.music.collections.CollectionState
import my.music.island.*
import org.junit.Assert.*
import org.junit.Test

class DynamicIslandTest {
    @Test
    fun renderModeSelectsAccessibilityOverlayThenInAppFallback() {
        assertEquals(
            IslandRenderMode.DISABLED,
            DynamicIslandCapability.resolveRenderMode(
                IslandEnvironment(enabled = false, sdkInt = 35, isAndroidGo = false, hasAccessibility = true, hasOverlayPermission = true, hasCutout = true)
            )
        )
        assertEquals(
            IslandRenderMode.CUTOUT_ACCESSIBILITY,
            DynamicIslandCapability.resolveRenderMode(
                IslandEnvironment(enabled = true, sdkInt = 28, isAndroidGo = false, hasAccessibility = true, hasOverlayPermission = true, hasCutout = true)
            )
        )
        assertEquals(
            IslandRenderMode.OVERLAY_BELOW_STATUS_BAR,
            DynamicIslandCapability.resolveRenderMode(
                IslandEnvironment(enabled = true, sdkInt = 34, isAndroidGo = false, hasAccessibility = false, hasOverlayPermission = true, hasCutout = true)
            )
        )
        assertEquals(
            IslandRenderMode.IN_APP_FALLBACK,
            DynamicIslandCapability.resolveRenderMode(
                IslandEnvironment(enabled = true, sdkInt = 30, isAndroidGo = true, hasAccessibility = false, hasOverlayPermission = true, hasCutout = false)
            )
        )
        assertEquals(
            IslandRenderMode.IN_APP_FALLBACK,
            DynamicIslandCapability.resolveRenderMode(
                IslandEnvironment(enabled = true, sdkInt = 25, isAndroidGo = false, hasAccessibility = true, hasOverlayPermission = true, hasCutout = false)
            )
        )
    }

    @Test
    fun supportIssuesIdentifyMinimumApisAndAndroidGo() {
        assertNotNull(DynamicIslandCapability.supportIssue(24, isAndroidGo = false))
        assertNotNull(DynamicIslandCapability.supportIssue(27, isAndroidGo = false))
        assertNotNull(DynamicIslandCapability.supportIssue(30, isAndroidGo = true))
        assertNull(DynamicIslandCapability.supportIssue(28, isAndroidGo = false))
        assertNull(DynamicIslandCapability.supportIssue(36, isAndroidGo = false))
    }

    @Test
    fun collapsedLayoutWrapsCenterLeftAndRightCutoutsWithoutOverlap() {
        val density = 3f
        val screenW = 1080
        val statusH = 96
        val cfg = IslandConfig(enabled = true)

        val center = DynamicIslandGeometry.computeCollapsed(
            screenW, statusH, IslandRect(500, 12, 580, 84), isLandscape = false,
            mode = IslandRenderMode.CUTOUT_ACCESSIBILITY, config = cfg, density = density
        )
        assertEquals(CutoutPlacement.CENTER, center.placement)
        assertTrue(center.artLeft + center.artSize <= center.cameraGapLeft)
        assertTrue(center.cameraGapLeft + center.cameraGapWidth <= center.vizLeft)

        val left = DynamicIslandGeometry.computeCollapsed(
            screenW, statusH, IslandRect(36, 12, 110, 84), isLandscape = false,
            mode = IslandRenderMode.CUTOUT_ACCESSIBILITY, config = cfg, density = density
        )
        assertEquals(CutoutPlacement.LEFT, left.placement)
        assertTrue(left.cameraGapLeft + left.cameraGapWidth <= left.artLeft)
        assertTrue(left.artLeft + left.artSize < left.vizLeft)

        val right = DynamicIslandGeometry.computeCollapsed(
            screenW, statusH, IslandRect(960, 12, 1044, 84), isLandscape = false,
            mode = IslandRenderMode.CUTOUT_ACCESSIBILITY, config = cfg, density = density
        )
        assertEquals(CutoutPlacement.RIGHT, right.placement)
        assertTrue(right.artLeft + right.artSize < right.vizLeft)
        assertTrue(right.vizLeft + right.vizWidth <= right.cameraGapLeft)
    }

    @Test
    fun overlayFallbackAndLandscapeUseCenteredPillBelowStatusBarOrTopEdge() {
        val density = 3f
        val statusH = 96
        val overlay = DynamicIslandGeometry.computeCollapsed(
            1080, statusH, IslandRect(500, 12, 580, 84), isLandscape = false,
            mode = IslandRenderMode.OVERLAY_BELOW_STATUS_BAR, config = IslandConfig(enabled = true), density = density
        )
        assertEquals(CutoutPlacement.NONE, overlay.placement)
        assertTrue(overlay.y > statusH)
        assertEquals(0, overlay.cameraGapWidth)

        val landscape = DynamicIslandGeometry.computeCollapsed(
            2400, statusH, IslandRect(500, 12, 580, 84), isLandscape = true,
            mode = IslandRenderMode.CUTOUT_ACCESSIBILITY, config = IslandConfig(enabled = true), density = density
        )
        assertEquals(CutoutPlacement.NONE, landscape.placement)
    }

    @Test
    fun expandedCardClearsPhysicalCameraCutout() {
        val density = 3f
        val cutout = IslandRect(500, 16, 580, 88)
        val expanded = DynamicIslandGeometry.computeExpanded(
            1080, 96, cutout, isLandscape = false,
            mode = IslandRenderMode.CUTOUT_ACCESSIBILITY, config = IslandConfig(enabled = true), density = density
        )
        assertTrue(expanded.y + expanded.topSafePaddingPx > cutout.bottom)
        assertTrue(expanded.height > expanded.topSafePaddingPx)
    }

    @Test
    fun expandedCardAccommodatesTransportControlsAndSafePadding() {
        val density = 2.8125f // 450 dpi
        val cutout = IslandRect(565, 0, 655, 125)
        val expanded = DynamicIslandGeometry.computeExpanded(
            1220, 125, cutout, isLandscape = false,
            mode = IslandRenderMode.CUTOUT_ACCESSIBILITY, config = IslandConfig(enabled = true), density = density
        )
        val contentHeightPx = expanded.height - expanded.topSafePaddingPx
        val expectedMinPx = (168 * density).toInt()
        assertTrue("Content height $contentHeightPx must be at least $expectedMinPx", contentHeightPx >= expectedMinPx)
    }

    @Test
    fun configParsesAndClampsCalibrationBoundsSafely() {
        val defaults = IslandConfig.from(CollectionState())
        assertFalse(defaults.enabled)
        assertFalse(defaults.showPaused)
        assertTrue(defaults.hideInPlayer)

        val clamped = IslandConfig.from(
            CollectionState(
                preferences = mapOf(
                    IslandConfig.KEY_ENABLED to "true",
                    IslandConfig.KEY_OFFSET_X to "500",
                    IslandConfig.KEY_OFFSET_Y to "-99",
                    IslandConfig.KEY_WIDTH_DELTA to "-200",
                    IslandConfig.KEY_HEIGHT_DELTA to "100",
                )
            )
        )
        assertTrue(clamped.enabled)
        assertEquals(48, clamped.offsetXDp)
        assertEquals(-48, clamped.offsetYDp)
        assertEquals(-32, clamped.widthDeltaDp)
        assertEquals(20, clamped.heightDeltaDp)

        val minHeight = IslandConfig.from(
            CollectionState(preferences = mapOf(IslandConfig.KEY_HEIGHT_DELTA to "-99"))
        )
        assertEquals(-10, minHeight.heightDeltaDp)
    }

    @Test
    fun timeFormattingMatchesStandardAndNegativeRemaining() {
        assertEquals("0:00", DynamicIslandOverlayView.formatTime(0))
        assertEquals("3:04", DynamicIslandOverlayView.formatTime(184_000))
        assertEquals("1:02:05", DynamicIslandOverlayView.formatTime(3_725_000))
    }

    @Test
    fun topCutoutBoundsRejectWindowTranslatedNegativeOffsets() {
        assertNull(DynamicIslandBus.normalizeTopCutoutBounds(266, -4, 427, 42))
        assertNull(DynamicIslandBus.normalizeTopCutoutBounds(-12, 0, 140, 46))
        assertNull(DynamicIslandBus.normalizeTopCutoutBounds(280, 0, 280, 46))
        assertEquals(
            IslandRect(280, 0, 441, 46),
            DynamicIslandBus.normalizeTopCutoutBounds(280, 0, 441, 46),
        )
    }

    @Test
    fun centeredTeardropAndCornerCutoutStayCenteredOrPinnedAcrossRepeats() {
        // Micromax E7533 telemetry: 720x1600, density 1.75 (280dpi), cutout Rect(280, 0 - 441, 46).
        val micromaxCutout = IslandRect(280, 0, 441, 46)
        val firstMicromax = DynamicIslandGeometry.computeCollapsed(
            screenWidthPx = 720,
            statusBarHeightPx = 46,
            cutoutRect = micromaxCutout,
            isLandscape = false,
            mode = IslandRenderMode.CUTOUT_ACCESSIBILITY,
            config = IslandConfig(enabled = true),
            density = 1.75f,
        )
        val secondMicromax = DynamicIslandGeometry.computeCollapsed(
            screenWidthPx = 720,
            statusBarHeightPx = 46,
            cutoutRect = micromaxCutout,
            isLandscape = false,
            mode = IslandRenderMode.CUTOUT_ACCESSIBILITY,
            config = IslandConfig(enabled = true),
            density = 1.75f,
        )
        assertEquals(CutoutPlacement.CENTER, firstMicromax.placement)
        assertEquals(firstMicromax, secondMicromax)
        assertTrue(firstMicromax.x <= micromaxCutout.left)
        assertTrue(firstMicromax.x + firstMicromax.width >= micromaxCutout.right)

        // Corner cutout (top-right) stays pinned RIGHT with valid visualizer bounds before the camera gap.
        val cornerCutout = IslandRect(954, 0, 1080, 126)
        val corner = DynamicIslandGeometry.computeCollapsed(
            screenWidthPx = 1080,
            statusBarHeightPx = 126,
            cutoutRect = cornerCutout,
            isLandscape = false,
            mode = IslandRenderMode.CUTOUT_ACCESSIBILITY,
            config = IslandConfig(enabled = true),
            density = 2.625f,
        )
        assertEquals(CutoutPlacement.RIGHT, corner.placement)
        assertTrue(corner.vizLeft + corner.vizWidth <= corner.cameraGapLeft)
        assertTrue(corner.x + corner.width >= cornerCutout.left)
    }

    @Test
    fun compactNegativeHeightAndNegativeVerticalOffsetReduceSpacingToFlushTop() {
        val density = 1.75f
        val cutout = IslandRect(280, 0, 441, 46)
        val compactConfig = IslandConfig(enabled = true, offsetYDp = -48, heightDeltaDp = -10)
        val compact = DynamicIslandGeometry.computeCollapsed(
            screenWidthPx = 720,
            statusBarHeightPx = 46,
            cutoutRect = cutout,
            isLandscape = false,
            mode = IslandRenderMode.CUTOUT_ACCESSIBILITY,
            config = compactConfig,
            density = density,
        )
        assertEquals(0, compact.y)
        assertEquals(43, compact.height)

        val noCutout = DynamicIslandGeometry.computeCollapsed(
            screenWidthPx = 1080,
            statusBarHeightPx = 96,
            cutoutRect = null,
            isLandscape = false,
            mode = IslandRenderMode.OVERLAY_BELOW_STATUS_BAR,
            config = IslandConfig(enabled = true, offsetYDp = -48, heightDeltaDp = -10),
            density = 3f,
        )
        assertEquals(0, noCutout.y)
        assertEquals(78, noCutout.height)
    }

    @Test
    fun appForegroundStateTogglesCleanlyOnBus() {
        val initial = DynamicIslandBus.appInForeground.value
        try {
            DynamicIslandBus.setAppInForeground(false)
            DynamicIslandBus.onActivityStarted(101)
            assertTrue(DynamicIslandBus.appInForeground.value)
            // Overlapping Activity replacement (CLEAR_TASK / relaunch): old activity stops after new starts
            DynamicIslandBus.onActivityStarted(202)
            DynamicIslandBus.onActivityStopped(101, changingConfigurations = false)
            DynamicIslandBus.onActivityDestroyed(101, changingConfigurations = false)
            assertTrue(DynamicIslandBus.appInForeground.value)
            // Configuration change preserves foreground across stop until destroyed/recreated
            DynamicIslandBus.onActivityStopped(202, changingConfigurations = true)
            assertTrue(DynamicIslandBus.appInForeground.value)
            DynamicIslandBus.onActivityStarted(303)
            DynamicIslandBus.onActivityDestroyed(202, changingConfigurations = true)
            assertTrue(DynamicIslandBus.appInForeground.value)
            // Normal background transition hides appInForeground
            DynamicIslandBus.onActivityStopped(303, changingConfigurations = false)
            assertFalse(DynamicIslandBus.appInForeground.value)
        } finally {
            DynamicIslandBus.setAppInForeground(initial)
        }
    }
}

