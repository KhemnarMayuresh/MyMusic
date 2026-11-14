package my.music.island

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager

enum class IslandRenderMode {
    DISABLED,
    CUTOUT_ACCESSIBILITY,
    OVERLAY_BELOW_STATUS_BAR,
    IN_APP_FALLBACK,
}

data class IslandEnvironment(
    val enabled: Boolean,
    val sdkInt: Int,
    val isAndroidGo: Boolean,
    val hasAccessibility: Boolean,
    val hasOverlayPermission: Boolean,
    val hasCutout: Boolean,
)

object DynamicIslandCapability {
    const val MIN_CUTOUT_API = 28
    const val MIN_OVERLAY_API = 26

    fun resolveRenderMode(env: IslandEnvironment): IslandRenderMode = when {
        !env.enabled -> IslandRenderMode.DISABLED
        env.sdkInt >= MIN_CUTOUT_API && env.hasAccessibility -> IslandRenderMode.CUTOUT_ACCESSIBILITY
        env.sdkInt >= MIN_OVERLAY_API && !env.isAndroidGo && env.hasOverlayPermission -> IslandRenderMode.OVERLAY_BELOW_STATUS_BAR
        else -> IslandRenderMode.IN_APP_FALLBACK
    }

    fun supportIssue(sdkInt: Int, isAndroidGo: Boolean): String? = when {
        sdkInt < MIN_OVERLAY_API ->
            "Camera cutout overlays require Android 9+ (or Android 8+ for a floating overlay). My Music uses the media notification on this version."
        isAndroidGo && sdkInt >= 29 ->
            "Android Go restricts system floating overlays. The media notification remains available."
        sdkInt < MIN_CUTOUT_API ->
            "Android 9+ is required to detect front camera cutouts. On Android 8, the floating pill anchors below the status bar."
        else -> null
    }

    fun isAndroidGo(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 29) return false
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        return am?.isLowRamDevice == true
    }

    fun canDrawOverlays(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < MIN_OVERLAY_API) return false
        if (isAndroidGo(context)) return false
        return Settings.canDrawOverlays(context)
    }

    fun isAccessibilityEnabled(context: Context): Boolean {
        if (DynamicIslandAccessibilityService.running) return true
        val expected = ComponentName(context, DynamicIslandAccessibilityService::class.java)
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty()
        val listed = enabledServices.split(':').any { entry ->
            val parsed = ComponentName.unflattenFromString(entry.trim())
            parsed == expected
        }
        if (listed) return true
        val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        return manager?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            ?.any { info ->
                val si = info.resolveInfo?.serviceInfo
                si != null && si.packageName == context.packageName && si.name == DynamicIslandAccessibilityService::class.java.name
            } == true
    }

    fun environment(context: Context, enabled: Boolean): IslandEnvironment = IslandEnvironment(
        enabled = enabled,
        sdkInt = Build.VERSION.SDK_INT,
        isAndroidGo = isAndroidGo(context),
        hasAccessibility = isAccessibilityEnabled(context),
        hasOverlayPermission = canDrawOverlays(context),
        hasCutout = DynamicIslandBus.cutout.value.portraitCutout != null,
    )

    fun openAccessibilitySettings(context: Context): Boolean = launchSettingsIntent(
        context,
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
    )

    fun openOverlaySettings(context: Context): Boolean {
        val packageUri = Uri.parse("package:${context.packageName}")
        val primary = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, packageUri)
        if (launchSettingsIntent(context, primary)) return true
        return launchSettingsIntent(context, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri))
    }

    fun openAppInfoSettings(context: Context): Boolean = launchSettingsIntent(
        context,
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")),
    )

    private fun launchSettingsIntent(context: Context, intent: Intent): Boolean {
        if (context !is android.app.Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(intent); true }.getOrDefault(false)
    }
}

