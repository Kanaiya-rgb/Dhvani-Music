package com.music.dhvani.data.icon

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.DrawableRes
import com.music.dhvani.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppIcon(
    val key: String,
    val title: String,
    val componentAlias: String,
    @DrawableRes val previewRes: Int
) {
    DEFAULT(
        key = "default",
        title = "Default",
        componentAlias = "com.music.dhvani.MainActivityDefault",
        previewRes = R.drawable.ic_app_icon_preview_default
    ),
    MONOCHROME(
        key = "monochrome",
        title = "Monochrome",
        componentAlias = "com.music.dhvani.MainActivityMonochrome",
        previewRes = R.drawable.ic_app_icon_preview_monochrome
    ),
    LIQUID_GLASS(
        key = "liquid_glass",
        title = "Liquid Glass",
        componentAlias = "com.music.dhvani.MainActivityLiquidGlass",
        previewRes = R.drawable.ic_app_icon_preview_liquid_glass
    ),
    COSMIC_CRYSTAL(
        key = "cosmic_crystal",
        title = "Cosmic Crystal",
        componentAlias = "com.music.dhvani.MainActivityCosmicCrystal",
        previewRes = R.drawable.ic_app_icon_preview_cosmic_crystal
    ),
    SMOKY_AMBER(
        key = "smoky_amber",
        title = "Smoky Amber",
        componentAlias = "com.music.dhvani.MainActivitySmokyAmber",
        previewRes = R.drawable.ic_app_icon_preview_smoky_amber
    ),
    CYBERPUNK_NEON(
        key = "cyberpunk_neon",
        title = "Cyberpunk",
        componentAlias = "com.music.dhvani.MainActivityCyberpunkNeon",
        previewRes = R.drawable.ic_app_icon_preview_cyberpunk_neon
    ),
    ROYAL_GOLD(
        key = "royal_gold",
        title = "Royal Gold",
        componentAlias = "com.music.dhvani.MainActivityRoyalGold",
        previewRes = R.drawable.ic_app_icon_preview_royal_gold
    ),
    STEALTH_BLACK(
        key = "stealth_black",
        title = "Stealth Black",
        componentAlias = "com.music.dhvani.MainActivityStealthBlack",
        previewRes = R.drawable.ic_app_icon_preview_stealth_black
    );

    companion object {
        fun fromKey(key: String?): AppIcon {
            return entries.find { it.key == key } ?: DEFAULT
        }
    }
}

object AppIconManager {
    private const val PREFS_NAME = "dhvani_app_icon_prefs"
    private const val KEY_CURRENT_ICON = "current_icon_key"

    private val _currentIconFlow = MutableStateFlow(AppIcon.DEFAULT)
    val currentIconFlow: StateFlow<AppIcon> = _currentIconFlow.asStateFlow()

    fun getCurrentIcon(context: Context): AppIcon {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedKey = prefs.getString(KEY_CURRENT_ICON, null)
        val icon = if (savedKey != null) {
            AppIcon.fromKey(savedKey)
        } else {
            val pm = context.packageManager
            var detected = AppIcon.DEFAULT
            for (item in AppIcon.entries) {
                val component = ComponentName(context.packageName, item.componentAlias)
                val state = pm.getComponentEnabledSetting(component)
                if (state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                    detected = item
                    break
                }
            }
            detected
        }
        _currentIconFlow.value = icon
        return icon
    }

    fun setIcon(context: Context, targetIcon: AppIcon): Boolean {
        val current = getCurrentIcon(context)
        if (current == targetIcon) return true
        val pm = context.packageManager
        val packageName = context.packageName

        try {
            // 1. Enable the newly selected alias first so the app always has at least one valid launcher
            val targetComponent = ComponentName(packageName, targetIcon.componentAlias)
            pm.setComponentEnabledSetting(
                targetComponent,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )

            // 2. Disable all other aliases
            for (icon in AppIcon.entries) {
                if (icon != targetIcon) {
                    val otherComponent = ComponentName(packageName, icon.componentAlias)
                    pm.setComponentEnabledSetting(
                        otherComponent,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                }
            }

            // 3. Save preference
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_CURRENT_ICON, targetIcon.key)
                .apply()

            _currentIconFlow.value = targetIcon
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}
