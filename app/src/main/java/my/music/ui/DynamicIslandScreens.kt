package my.music.ui

import android.content.Context
import android.os.Build
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import my.music.PlayerViewModel
import my.music.collections.CollectionState
import my.music.island.*
import my.music.model.Track
import my.music.playback.PlaybackSnapshot
import kotlin.math.roundToInt

@Composable
fun FirstInstallIslandCard(
    onEnable: () -> Unit,
    onDecline: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(18.dp),
        color = Panel.copy(alpha = 0.85f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.GraphicEq, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text("Try Dynamic Island?", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Text(
                    "Wrap your front camera cutout with live visualizer & playback controls.",
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                TextButton(onClick = onDecline, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                    Text("Not now", color = Muted)
                }
                Button(
                    onClick = onEnable,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp),
                ) {
                    Text("Enable")
                }
            }
        }
    }
}

@Composable
fun DynamicIslandPermissionGuideDialog(
    onOpenAccessibility: () -> Unit,
    onOpenOverlay: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onDecline: () -> Unit,
) {
    val context = LocalContext.current
    val isGo = remember { DynamicIslandCapability.isAndroidGo(context) }
    val issue = remember { DynamicIslandCapability.supportIssue(Build.VERSION.SDK_INT, isGo) }

    AlertDialog(
        onDismissRequest = onDecline,
        icon = { Icon(Icons.Rounded.GraphicEq, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Enable Dynamic Island") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "To wrap your front camera cutout inside the status bar, Android requires enabling My Music Dynamic Island in Accessibility settings.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "Privacy guarantee: My Music only displays its own playback and never reads other apps or screen content.",
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                if (Build.VERSION.SDK_INT >= 33) {
                    Text(
                        "Installed via APK? If Android grays out Accessibility, open App Info → ⋮ (top right) → Allow restricted settings first.",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (issue != null) {
                    Text(issue, color = Color(0xFFFFB74D), style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                TextButton(onClick = onOpenAccessibility) { Text("Open Accessibility Settings") }
                if (Build.VERSION.SDK_INT >= DynamicIslandCapability.MIN_OVERLAY_API && !isGo) {
                    TextButton(onClick = onOpenOverlay) { Text("Use Overlay Fallback instead") }
                }
                if (Build.VERSION.SDK_INT >= 33) {
                    TextButton(onClick = onOpenAppInfo) { Text("Open App Info (Restricted settings)") }
                }
                TextButton(onClick = onDecline) { Text("Not now", color = Muted) }
            }
        },
    )
}

@Composable
fun DynamicIslandDeclinedDialog(
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Info, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Dynamic Island stays off") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "What it does: Dynamic Island turns your front camera cutout into an interactive pill showing album art, a live music visualizer, seek bar, and playback controls.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "Where to enable it later: Open Settings → Dynamic Island, turn on Enable Dynamic Island, and allow Accessibility → My Music Dynamic Island (or Display over other apps as a floating fallback).",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "My Music continues working normally with standard media notifications and widgets.",
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Got it") }
        },
        dismissButton = {
            TextButton(onClick = onOpenSettings) { Text("Open Settings") }
        },
    )
}

@Composable
fun DynamicIslandSettingsScreen(
    vm: PlayerViewModel,
    data: CollectionState,
    playback: PlaybackSnapshot,
    current: Track?,
    artworkUri: String?,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val config = remember(data) { IslandConfig.from(data) }
    val cutout by DynamicIslandBus.cutout.collectAsStateWithLifecycle()
    val accRunning by DynamicIslandBus.accessibilityRunning.collectAsStateWithLifecycle()
    val refreshTick by DynamicIslandBus.refreshSignal.collectAsStateWithLifecycle()

    val env = remember(config.enabled, accRunning, refreshTick, cutout) {
        DynamicIslandCapability.environment(context, config.enabled)
    }
    val mode = remember(env) { DynamicIslandCapability.resolveRenderMode(env) }
    val supportNote = remember { DynamicIslandCapability.supportIssue(Build.VERSION.SDK_INT, env.isAndroidGo) }

    var showSetupDialog by remember { mutableStateOf(false) }
    var showRestrictedDialog by remember { mutableStateOf(false) }
    var previewExpanded by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Header("Dynamic Island", onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            IslandPreviewCard(
                title = current?.title ?: playback.current?.mediaMetadata?.title?.toString() ?: "Over the Horizon",
                artist = current?.artist ?: playback.current?.mediaMetadata?.artist?.toString() ?: "My Music",
                artworkUri = artworkUri,
                playing = playback.playing,
                positionMs = if (playback.duration > 0) playback.position else 92_000L,
                durationMs = if (playback.duration > 0) playback.duration else 245_000L,
                expanded = previewExpanded,
                config = config,
                onToggle = { previewExpanded = !previewExpanded },
            )

            PreferenceToggle(
                title = "Enable Dynamic Island",
                subtitle = modeSummary(mode, env),
                checked = config.enabled,
            ) { turnOn ->
                vm.work {
                    vm.repository.setPreferences(
                        mapOf(
                            IslandConfig.KEY_ENABLED to turnOn.toString(),
                            IslandConfig.KEY_PROMPTED to "true",
                        )
                    )
                }
                if (turnOn && !env.hasAccessibility && !env.hasOverlayPermission) {
                    showSetupDialog = true
                }
                DynamicIslandBus.notifyRefresh()
            }

            if (supportNote != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = Panel.copy(alpha = 0.7f),
                ) {
                    Text(supportNote, Modifier.padding(14.dp), color = Color(0xFFFFCC80), style = MaterialTheme.typography.bodySmall)
                }
            }

            SectionTitle("Permissions & fallback")
            PermissionStatusRow(
                title = "Accessibility Service (Cutout Hub)",
                subtitle = if (env.hasAccessibility) "Active — wraps front camera cutout above the status bar."
                else "Recommended — needed to draw around the camera cutout in the status bar.",
                granted = env.hasAccessibility,
                available = Build.VERSION.SDK_INT >= DynamicIslandCapability.MIN_CUTOUT_API,
                onAction = { DynamicIslandCapability.openAccessibilitySettings(context) },
            )

            if (Build.VERSION.SDK_INT >= 33 && !env.hasAccessibility) {
                ActionRow(
                    Icons.Rounded.Security,
                    "Allow restricted settings (APK installs)",
                    "If Accessibility is grayed out on Android 13+, open App Info → ⋮ → Allow restricted settings.",
                ) { showRestrictedDialog = true }
            }

            PermissionStatusRow(
                title = "Display over other apps (Fallback)",
                subtitle = when {
                    env.isAndroidGo && Build.VERSION.SDK_INT >= 29 -> "Disabled by Android Go edition."
                    Build.VERSION.SDK_INT < DynamicIslandCapability.MIN_OVERLAY_API -> "Requires Android 8.0 or newer."
                    env.hasOverlayPermission -> "Allowed — floats just below the status bar when Accessibility is off."
                    else -> "Optional fallback if you prefer not to enable Accessibility."
                },
                granted = env.hasOverlayPermission,
                available = Build.VERSION.SDK_INT >= DynamicIslandCapability.MIN_OVERLAY_API && !env.isAndroidGo,
                onAction = { DynamicIslandCapability.openOverlaySettings(context) },
            )

            if (config.enabled) {
                SectionTitle("Behavior")
                PreferenceToggle(
                    "Show while paused",
                    "Keep the compact pill visible after pausing instead of hiding after 8 seconds",
                    config.showPaused,
                ) { vm.pref(IslandConfig.KEY_SHOW_PAUSED, it.toString()) }

                SectionTitle("Cutout alignment")
                AlignmentSlider("Horizontal offset", config.offsetXDp, -48f..48f) {
                    vm.pref(IslandConfig.KEY_OFFSET_X, it.toString())
                }
                AlignmentSlider("Vertical offset", config.offsetYDp, -48f..48f) {
                    vm.pref(IslandConfig.KEY_OFFSET_Y, it.toString())
                }
                AlignmentSlider("Pill width", config.widthDeltaDp, -32f..64f) {
                    vm.pref(IslandConfig.KEY_WIDTH_DELTA, it.toString())
                }
                AlignmentSlider("Pill height", config.heightDeltaDp, -10f..20f) {
                    vm.pref(IslandConfig.KEY_HEIGHT_DELTA, it.toString())
                }
                TextButton(
                    onClick = {
                        vm.work {
                            vm.repository.setPreferences(
                                mapOf(
                                    IslandConfig.KEY_OFFSET_X to "0",
                                    IslandConfig.KEY_OFFSET_Y to "0",
                                    IslandConfig.KEY_WIDTH_DELTA to "0",
                                    IslandConfig.KEY_HEIGHT_DELTA to "0",
                                )
                            )
                        }
                    },
                    modifier = Modifier.padding(horizontal = 20.dp),
                ) {
                    Text("Reset alignment")
                }
            }

            Text(
                "Background only: Dynamic Island automatically hides while My Music is open. Privacy: My Music never reads notifications, messages, or screen content from other apps.",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                color = Muted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }

    if (showSetupDialog) {
        DynamicIslandPermissionGuideDialog(
            onOpenAccessibility = {
                showSetupDialog = false
                DynamicIslandCapability.openAccessibilitySettings(context)
            },
            onOpenOverlay = {
                showSetupDialog = false
                DynamicIslandCapability.openOverlaySettings(context)
            },
            onOpenAppInfo = {
                showSetupDialog = false
                DynamicIslandCapability.openAppInfoSettings(context)
            },
            onDecline = { showSetupDialog = false },
        )
    }

    if (showRestrictedDialog) {
        AlertDialog(
            onDismissRequest = { showRestrictedDialog = false },
            title = { Text("Restricted settings on Android 13+") },
            text = {
                Text(
                    "1. Tap Open App Info below.\n2. Tap the ⋮ menu in the top-right corner.\n3. Choose Allow restricted settings and confirm your screen lock.\n4. Return to Accessibility and enable My Music Dynamic Island."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showRestrictedDialog = false
                    DynamicIslandCapability.openAppInfoSettings(context)
                }) { Text("Open App Info") }
            },
            dismissButton = {
                TextButton(onClick = { showRestrictedDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 6.dp),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun PermissionStatusRow(
    title: String,
    subtitle: String,
    granted: Boolean,
    available: Boolean,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = available, onClick = onAction)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (granted) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
            null,
            tint = if (granted) Color(0xFF66BB6A) else Muted,
            modifier = Modifier.padding(end = 14.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Muted, style = MaterialTheme.typography.bodySmall)
        }
        if (available) {
            Text(
                if (granted) "Manage" else "Allow",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

@Composable
private fun AlignmentSlider(label: String, value: Int, range: ClosedFloatingPointRange<Float>, onChange: (Int) -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 2.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text("${if (value > 0) "+" else ""}${value}dp", color = Muted, style = MaterialTheme.typography.bodySmall)
        }
        Slider(
            value = value.toFloat().coerceIn(range),
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = range,
        )
    }
}

@Composable
private fun IslandPreviewCard(
    title: String,
    artist: String,
    artworkUri: String?,
    playing: Boolean,
    positionMs: Long,
    durationMs: Long,
    expanded: Boolean,
    config: IslandConfig,
    onToggle: () -> Unit,
) {
    val motion = rememberPlayerMotion(playing, title)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFFE8ECF2),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 14.dp)
                .animateContentSize(tween(220)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!expanded) {
                Box(
                    modifier = Modifier
                        .offset(x = (config.offsetXDp * 0.4f).dp, y = (config.offsetYDp * 0.25f).dp.coerceIn((-6).dp, 12.dp))
                        .width((156 + config.widthDeltaDp).coerceIn(120, 260).dp)
                        .height((36 + config.heightDeltaDp).coerceIn(24, 56).dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black)
                        .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(50))
                        .padding(horizontal = 8.dp),
                ) {
                    Artwork(artworkUri, Modifier.size(24.dp).clip(CircleShape).align(Alignment.CenterStart))
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF14171E))
                            .align(Alignment.Center),
                    )
                    IslandWaveBars(
                        levels = motion.levels,
                        playing = playing,
                        modifier = Modifier.size(24.dp, 16.dp).align(Alignment.CenterEnd),
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Black)
                        .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(24.dp))
                        .padding(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Artwork(artworkUri, Modifier.size(46.dp))
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(artist, color = Color(0xFFB3B3C0), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IslandWaveBars(motion.levels, playing, Modifier.size(26.dp, 18.dp))
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(timeLabel(positionMs), color = Color(0xFFD0D0DC), fontSize = 11.sp)
                        LinearProgressIndicator(
                            progress = { if (durationMs > 0) positionMs.toFloat() / durationMs else 0.35f },
                            modifier = Modifier.weight(1f).padding(horizontal = 10.dp).height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = Color(0xFF70A9CE),
                            trackColor = Color.White.copy(alpha = 0.2f),
                        )
                        Text("-${timeLabel((durationMs - positionMs).coerceAtLeast(0))}", color = Color(0xFFD0D0DC), fontSize = 11.sp)
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.FastRewind, null, tint = Color.White, modifier = Modifier.size(26.dp))
                        Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(30.dp))
                        Icon(Icons.Rounded.FastForward, null, tint = Color.White, modifier = Modifier.size(26.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                if (expanded) "Preview (Expanded) — Tap to collapse" else "Preview (Cutout pill) — Tap to expand",
                color = Color(0xFF3E4656),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun IslandWaveBars(levels: FloatArray, playing: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val bars = 6
        val slotW = size.width / bars
        val barW = (slotW * 0.48f).coerceAtLeast(2f)
        val cy = size.height / 2f
        val alpha = if (playing) 1f else 0.55f
        for (i in 0 until bars) {
            val raw = if (levels.isNotEmpty()) {
                levels[((i * (levels.size - 1)) / (bars - 1)).coerceIn(0, levels.lastIndex)]
            } else 0f
            val level = if (playing) (0.22f + raw * 0.78f).coerceIn(0.22f, 1f) else 0.22f
            val h = (size.height * level).coerceIn(barW, size.height)
            drawRoundRect(
                color = Color.White.copy(alpha = alpha),
                topLeft = Offset(slotW * i + (slotW - barW) / 2f, cy - h / 2f),
                size = Size(barW, h),
                cornerRadius = CornerRadius(barW / 2f, barW / 2f),
            )
        }
    }
}

private fun modeSummary(mode: IslandRenderMode, env: IslandEnvironment): String = when (mode) {
    IslandRenderMode.DISABLED -> "Off — using standard media notification and widgets"
    IslandRenderMode.CUTOUT_ACCESSIBILITY -> "Active in background — Camera cutout hub (Accessibility)"
    IslandRenderMode.OVERLAY_BELOW_STATUS_BAR -> "Active in background — Floating pill below status bar (Fallback)"
    IslandRenderMode.IN_APP_FALLBACK -> if (env.enabled) {
        "Needs permission — grant Accessibility or Overlay to show when My Music is in background"
    } else "Off"
}

