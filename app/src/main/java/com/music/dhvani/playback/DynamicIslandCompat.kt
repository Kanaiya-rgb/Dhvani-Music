package com.music.dhvani.playback

import android.content.Context
import android.os.Build

/**
 * Utility to detect if the host device natively provides a system-level
 * Dynamic Island, Now Bar, or Live Alert in the status bar for ongoing media playback.
 *
 * Supported OEM native island systems:
 * - Samsung One UI 7+ (Galaxy S25 series, S24 with Android 15 / One UI 7 Now Bar)
 * - OnePlus / Oppo OxygenOS 14+ / ColorOS 14+ (Fluid Cloud / Live Alerts)
 * - Realme UI 5+ / 6+ (Mini Capsule / Live Alerts)
 * - Xiaomi HyperOS 2+ (Status Bar Focus Notification / Live Island)
 * - Honor MagicOS 8+ / 9+ (Magic Capsule)
 * - Vivo / iQOO OriginOS 4+ / 5+ (Atomic Island)
 */
object DynamicIslandCompat {

    /**
     * Checks if the device natively provides a status bar / punch hole
     * dynamic island, live alert, or Now Bar for media playback.
     */
    fun hasNativeDynamicIsland(context: Context? = null): Boolean {
        return getNativeIslandBrandName(context) != null
    }

    /**
     * Returns the human-readable name of the native system island if supported,
     * or null if the device relies purely on standard notifications.
     */
    fun getNativeIslandBrandName(context: Context? = null): String? {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()

        // 1. Samsung One UI 7+ (Galaxy S25, S24 with One UI 7 / Android 15 Now Bar)
        if ((manufacturer.contains("samsung") || brand.contains("samsung")) && isSamsungOneUI7OrHigher()) {
            return "Samsung One UI 7 Now Bar"
        }

        // 2. OnePlus (OxygenOS 14/15) - Fluid Cloud
        if ((manufacturer.contains("oneplus") || brand.contains("oneplus")) && Build.VERSION.SDK_INT >= 34) {
            return "OnePlus OxygenOS Fluid Cloud"
        }

        // 3. Oppo (ColorOS 14/15) - Aqua Dynamics / Fluid Cloud
        if ((manufacturer.contains("oppo") || brand.contains("oppo")) && Build.VERSION.SDK_INT >= 34) {
            return "Oppo ColorOS Aqua Dynamics"
        }

        // 4. Realme (Realme UI 5/6) - Mini Capsule
        if ((manufacturer.contains("realme") || brand.contains("realme")) && Build.VERSION.SDK_INT >= 34) {
            return "Realme Mini Capsule"
        }

        // 5. Xiaomi / Poco / Redmi - HyperOS 2 Live Island / Focus Notification
        if ((manufacturer.contains("xiaomi") || brand.contains("xiaomi") ||
                manufacturer.contains("poco") || brand.contains("poco") ||
                manufacturer.contains("redmi") || brand.contains("redmi")) &&
            (Build.VERSION.SDK_INT >= 35 || getSystemProp("ro.mi.os.version.name").contains("OS2", ignoreCase = true))) {
            return "Xiaomi HyperOS Live Island"
        }

        // 6. Honor - Magic Capsule (MagicOS 8/9)
        if ((manufacturer.contains("honor") || brand.contains("honor")) && Build.VERSION.SDK_INT >= 34) {
            return "Honor Magic Capsule"
        }

        // 7. Vivo / iQOO - Atomic Island (OriginOS 4/5)
        if ((manufacturer.contains("vivo") || brand.contains("vivo") ||
                manufacturer.contains("iqoo") || brand.contains("iqoo")) && Build.VERSION.SDK_INT >= 34) {
            return "Vivo / iQOO Atomic Island"
        }

        // 8. Huawei - Live Window (HarmonyOS 4/5 / EMUI 14)
        if ((manufacturer.contains("huawei") || brand.contains("huawei")) &&
            (getSystemProp("ro.build.version.emui").isNotEmpty() || getSystemProp("hw_sc.build.platform.version").isNotEmpty())) {
            return "Huawei Live Window"
        }

        // 9. Transsion (Tecno Dynamic Port, Infinix Magic Ring, itel Dynamic Bar)
        if (manufacturer.contains("tecno") || brand.contains("tecno")) {
            return "Tecno Dynamic Port"
        }
        if (manufacturer.contains("infinix") || brand.contains("infinix")) {
            return "Infinix Magic Ring"
        }
        if (manufacturer.contains("itel") || brand.contains("itel")) {
            return "itel Dynamic Bar"
        }

        // 10. Motorola (Hello UI Smart Connect / Live Alerts)
        if ((manufacturer.contains("motorola") || brand.contains("motorola") ||
                manufacturer.contains("moto") || brand.contains("moto")) && Build.VERSION.SDK_INT >= 34) {
            return "Motorola Smart Connect Alert"
        }

        // 11. Meizu (Flyme 10/11 Aicy Live Island)
        if (manufacturer.contains("meizu") || brand.contains("meizu")) {
            return "Meizu Flyme Live Island"
        }

        // 12. ZTE / Nubia / RedMagic (MyOS Live Capsule)
        if (manufacturer.contains("nubia") || brand.contains("nubia") ||
            manufacturer.contains("redmagic") || brand.contains("redmagic") ||
            manufacturer.contains("zte") || brand.contains("zte")) {
            return "Nubia / RedMagic Live Capsule"
        }

        return null
    }

    private fun isSamsungOneUI7OrHigher(): Boolean {
        val oneUiVersion = getSystemProp("ro.build.version.oneui")
        val versionNumber = oneUiVersion.toIntOrNull() ?: 0
        if (versionNumber >= 70000) return true
        val sepVersion = getSystemProp("ro.build.version.sep")
        val sepNumber = sepVersion.toIntOrNull() ?: 0
        if (sepNumber >= 160000) return true
        // One UI 7 debuted on Android 15 (API 35+) for Samsung devices
        return Build.VERSION.SDK_INT >= 35
    }

    private fun getSystemProp(key: String): String {
        return try {
            val c = Class.forName("android.os.SystemProperties")
            val get = c.getMethod("get", String::class.java)
            (get.invoke(null, key) as? String).orEmpty()
        } catch (_: Throwable) {
            ""
        }
    }
}
