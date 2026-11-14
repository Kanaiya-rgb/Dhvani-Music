package com.music.dhvani.playback

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.audiofx.AudioEffect
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import java.util.UUID

object DolbyUtils {

    // Genuine OEM Dolby Atmos / DAX Effect UUIDs (excludes generic Android AOSP effects)
    private val DOLBY_EFFECT_TYPE_UUIDS = setOf(
        UUID.fromString("46d279d9-9be7-453d-9d7c-ef937f675587"), // DAX3 / DAX2 Processing
        UUID.fromString("9d492024-d946-4923-a5ba-61a15379cb0f"), // Dolby Atmos Effect
        UUID.fromString("e0e04fc0-4da1-11e0-8340-0002a5d5c51b"), // Dolby DAP / Mobile
    )

    /**
     * Detects if the current runtime environment is an Android Emulator / AVD.
     * Emulators never contain OEM hardware Dolby Atmos audio hardware or DSP licenses.
     */
    fun isEmulator(): Boolean {
        val brand = Build.BRAND.lowercase()
        val manufacturer = Build.MANUFACTURER.lowercase()
        // Physical OEM hardware (Samsung, Xiaomi, OnePlus, Vivo, etc.) is never an emulator
        if (brand == "samsung" || manufacturer == "samsung" ||
            brand == "xiaomi" || manufacturer == "xiaomi" ||
            brand == "oneplus" || manufacturer == "oneplus" ||
            brand == "oppo" || manufacturer == "oppo" ||
            brand == "vivo" || manufacturer == "vivo" ||
            brand == "realme" || manufacturer == "realme" ||
            brand == "motorola" || manufacturer == "motorola"
        ) {
            return false
        }

        val finger = Build.FINGERPRINT.lowercase()
        val model = Build.MODEL.lowercase()
        val device = Build.DEVICE.lowercase()
        val product = Build.PRODUCT.lowercase()
        val hardware = Build.HARDWARE.lowercase()

        return finger.startsWith("generic") ||
            finger.startsWith("unknown") ||
            finger.contains("test-keys") && model.contains("sdk") ||
            model.contains("google_sdk") ||
            model.contains("emulator") ||
            model.contains("android sdk built for x86") ||
            brand.startsWith("generic") && device.startsWith("generic") ||
            hardware.contains("goldfish") ||
            hardware.contains("ranchu") ||
            manufacturer.contains("genymotion") ||
            product.contains("sdk_google") ||
            product.contains("google_sdk") ||
            product.contains("sdk_x86") ||
            product.contains("vbox86p") ||
            product.contains("emulator") ||
            product.contains("simulator")
    }

    /**
     * Checks if the device has a genuine OEM Dolby Atmos hardware audio pipeline,
     * DSP effect, system setting, or control panel across ANY Android manufacturer in the world.
     */
    fun isDolbyAtmosAvailable(context: Context): Boolean {
        if (isEmulator()) return false
        return isSamsungDeviceWithDolby(context) ||
            hasDolbyAudioEffect() ||
            isDolbyPanelAvailable(context) ||
            hasDolbySystemSettings(context) ||
            hasAnyDolbyPackage(context)
    }

    /**
     * Checks whether an OEM Dolby Atmos settings panel activity or effect can be resolved.
     */
    fun isDolbyPanelAvailable(context: Context): Boolean {
        if (isEmulator()) return false
        val pm = context.packageManager ?: return false

        if (isSamsungDeviceWithDolby(context)) {
            return true
        }

        val hasResolvableIntent = getDolbyIntents().any { intent ->
            runCatching {
                val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.resolveActivity(intent, PackageManager.ResolveInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.resolveActivity(intent, 0)
                }
                if (resolved != null && resolved.activityInfo != null) {
                    val pkg = resolved.activityInfo.packageName.lowercase()
                    val cls = resolved.activityInfo.name.lowercase()
                    // Filter out Android system fallback ResolverActivity
                    pkg != "android" && !pkg.contains("resolveractivity") &&
                        (pkg.contains("dolby") || cls.contains("dolby") || pkg.contains("soundalive"))
                } else {
                    false
                }
            }.getOrDefault(false)
        }

        return hasResolvableIntent || hasDolbyAudioEffect() || hasAnyDolbyPackage(context)
    }

    /**
     * Queries Android AudioEffect framework for Dolby Atmos / DAX audio effect descriptors.
     * Universal across all Android devices in the world with hardware/system Dolby DSP.
     */
    fun hasDolbyAudioEffect(): Boolean {
        return try {
            val effects = AudioEffect.queryEffects() ?: return false
            effects.any { desc ->
                val nameMatch = desc.name?.let {
                    it.contains("Dolby", ignoreCase = true) ||
                        it.contains("Atmos", ignoreCase = true) ||
                        it.contains("DAX", ignoreCase = true) ||
                        it.contains("DAP", ignoreCase = true) ||
                        it.contains("SoundAlive", ignoreCase = true)
                } ?: false

                val implementorMatch = desc.implementor?.let {
                    it.contains("Dolby", ignoreCase = true) ||
                        it.contains("Samsung Electronics", ignoreCase = true)
                } ?: false

                val uuidMatch = desc.type in DOLBY_EFFECT_TYPE_UUIDS

                nameMatch || implementorMatch || uuidMatch
            }
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Checks OEM system / global settings for Dolby Atmos flags across manufacturers.
     */
    fun hasDolbySystemSettings(context: Context): Boolean {
        return try {
            val cr = context.contentResolver
            val globalMode = Settings.Global.getInt(cr, "dolby_audio_mode", -1) != -1 ||
                Settings.Global.getInt(cr, "dolby_audio_state", -1) != -1 ||
                Settings.Global.getString(cr, "dolby_audio_mode") != null
            val systemMode = Settings.System.getInt(cr, "dolby_audio_mode", -1) != -1 ||
                Settings.System.getInt(cr, "dolby_atmos_switch", -1) != -1 ||
                Settings.System.getInt(cr, "oplus_dolby_switch", -1) != -1 ||
                Settings.System.getInt(cr, "soundalive_dolby_status", -1) != -1
            val propCheck = getSystemProp("ro.vendor.audio.dolby.dax.support") == "true" ||
                getSystemProp("vendor.audio.dolby.ds2.enabled") == "true" ||
                getSystemProp("ro.audio.soundfx.dolby").isNotBlank()

            globalMode || systemMode || propCheck
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Detects if the current device is a Samsung Galaxy phone with SoundAlive & Dolby Atmos.
     */
    fun isSamsungDeviceWithDolby(context: Context): Boolean {
        val isSamsung = Build.MANUFACTURER.equals("samsung", ignoreCase = true) ||
            Build.BRAND.equals("samsung", ignoreCase = true)
        if (!isSamsung) return false

        val pm = context.packageManager ?: return false
        val hasSoundAlive = try {
            pm.getPackageInfo("com.sec.android.app.soundalive", 0) != null
        } catch (_: Throwable) {
            false
        }
        if (hasSoundAlive) return true

        val hasDolbySetting = try {
            Settings.Global.getInt(context.contentResolver, "dolby_audio_mode", -1) != -1 ||
                Settings.System.getInt(context.contentResolver, "dolby_audio_mode", -1) != -1 ||
                Settings.Global.getInt(context.contentResolver, "dolby_audio_state", -1) != -1 ||
                Settings.Global.getString(context.contentResolver, "dolby_audio_mode") != null
        } catch (_: Throwable) {
            false
        }

        return hasDolbySetting || hasDolbyAudioEffect()
    }

    /**
     * Checks if any installed package on the system contains genuine OEM Dolby Atmos components.
     */
    fun hasAnyDolbyPackage(context: Context): Boolean {
        val pm = context.packageManager ?: return false
        val dolbyPkgs = listOf(
            "com.dolby.daxappui",
            "com.dolby.spatialaudio",
            "com.dolby.daxservice",
            "com.dolby.dap",
            "com.motorola.dolby.dolbyui",
            "com.lenovo.dolby",
            "com.transsion.dolby",
        )
        return dolbyPkgs.any { pkg ->
            try {
                pm.getPackageInfo(pkg, 0) != null
            } catch (_: Throwable) {
                false
            }
        }
    }

    /**
     * Launches the system Dolby Atmos or device equalizer panel on ANY Android device in the world.
     */
    fun openDolbyAtmos(context: Context) {
        val pm = context.packageManager

        // 1. Try OEM dedicated intents first
        for (intent in getDolbyIntents()) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (pm != null && intent.resolveActivity(pm) != null) {
                    context.startActivity(intent)
                    return
                }
            } catch (_: Exception) {}
        }

        // 2. Dynamic Search: Find any installed package containing "dolby" or "soundalive" with a launch intent
        if (pm != null) {
            val potentialPkgs = listOf(
                "com.sec.android.app.soundalive",
                "com.dolby.daxappui",
                "com.dolby.spatialaudio",
                "com.motorola.dolby.dolbyui",
                "com.motorola.motoaudio",
                "com.oneplus.sound",
                "com.oplus.sound",
                "com.realme.sound",
                "com.miui.audioeffect",
                "com.lenovo.dolby",
                "com.asus.audiowizard",
                "com.transsion.dolby",
                "com.zte.audioeffect",
            )
            for (pkg in potentialPkgs) {
                try {
                    val launchIntent = pm.getLaunchIntentForPackage(pkg)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launchIntent)
                        return
                    }
                } catch (_: Exception) {}
            }
        }

        // 3. Samsung Sound Quality & Effects Settings
        try {
            val samsungSoundSettings = Intent().apply {
                setComponent(ComponentName("com.android.settings", "com.android.settings.Settings\$SoundQualityAndEffectsActivity"))
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (pm != null && samsungSoundSettings.resolveActivity(pm) != null) {
                context.startActivity(samsungSoundSettings)
                return
            }
        } catch (_: Exception) {}

        // 4. Samsung SoundAlive Action Intent
        try {
            val soundAliveSetting = Intent("com.sec.android.app.soundalive.ACTION_SOUND_ALIVE").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (pm != null && soundAliveSetting.resolveActivity(pm) != null) {
                context.startActivity(soundAliveSetting)
                return
            }
        } catch (_: Exception) {}

        // 5. Standard Android Audio Effect Control Panel
        try {
            val genericEffect = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            }
            if (pm != null && genericEffect.resolveActivity(pm) != null) {
                context.startActivity(genericEffect)
                return
            }
        } catch (_: Exception) {}

        // 6. System Sound Settings fallback
        try {
            val soundSettings = Intent(Settings.ACTION_SOUND_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(soundSettings)
            return
        } catch (_: Exception) {}

        Toast.makeText(
            context,
            "No OEM Dolby Atmos panel found on this device. Dhvani's built-in 3D Spatial Audio is active!",
            Toast.LENGTH_LONG,
        ).show()
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

    private fun getDolbyIntents(): List<Intent> = listOf(
        // Samsung One UI Dolby Atmos / SoundAlive
        Intent().setComponent(ComponentName("com.sec.android.app.soundalive", "com.sec.android.app.soundalive.ui.DolbyAtmosActivity")),
        Intent().setComponent(ComponentName("com.sec.android.app.soundalive", "com.sec.android.app.soundalive.activity.DolbyAtmosActivity")),
        Intent().setComponent(ComponentName("com.sec.android.app.soundalive", "com.sec.android.app.soundalive.ui.DolbyMainActivity")),
        Intent().setComponent(ComponentName("com.sec.android.app.soundalive", "com.sec.android.app.soundalive.activity.SoundAliveMainActivity")),
        Intent().setComponent(ComponentName("com.sec.android.app.soundalive", "com.sec.android.app.soundalive.ui.SoundAliveMainActivity")),
        Intent().setComponent(ComponentName("com.sec.android.app.soundalive", "com.sec.android.app.soundalive.MainActivity")),
        Intent("com.sec.android.app.soundalive.ACTION_SOUND_ALIVE"),
        Intent().setComponent(ComponentName("com.android.settings", "com.android.settings.Settings\$SoundQualityAndEffectsActivity")),

        // Xiaomi / MIUI / HyperOS Dolby Atmos & Audio Effects
        Intent().setComponent(ComponentName("com.miui.audioeffect", "com.miui.audioeffect.AudioEffectActivity")),
        Intent().setComponent(ComponentName("com.miui.audioeffect", "com.miui.audioeffect.DolbyActivity")),
        Intent().setComponent(ComponentName("com.xiaomi.audioeffect", "com.xiaomi.audioeffect.AudioEffectActivity")),

        // Generic Dolby Atmos DAX applications (Motorola, Lenovo, Xiaomi Dolby, ZTE, Nokia, Sharp, Custom ROMs)
        Intent().setComponent(ComponentName("com.dolby.daxappui", "com.dolby.daxappui.MainActivity")),
        Intent().setComponent(ComponentName("com.dolby.daxappui", "com.dolby.daxappui.Main")),
        Intent().setComponent(ComponentName("com.dolby.daxappui", "com.dolby.daxappui.ui.MainActivity")),
        Intent().setComponent(ComponentName("com.dolby.spatialaudio", "com.dolby.spatialaudio.MainActivity")),
        Intent().setComponent(ComponentName("com.dolby.daxservice", "com.dolby.daxservice.MainActivity")),

        // Motorola Dolby Atmos dedicated UI
        Intent().setComponent(ComponentName("com.motorola.dolby.dolbyui", "com.motorola.dolby.dolbyui.ui.MainActivity")),
        Intent().setComponent(ComponentName("com.motorola.dolby.dolbyui", "com.motorola.dolby.dolbyui.MainActivity")),
        Intent().setComponent(ComponentName("com.motorola.motoaudio", "com.motorola.motoaudio.MainActivity")),

        // OnePlus / Oppo / Realme Dolby Atmos dedicated activities
        Intent().setComponent(ComponentName("com.oneplus.sound", "com.oneplus.sound.dolby.DolbySettingsActivity")),
        Intent().setComponent(ComponentName("com.oplus.sound", "com.oplus.sound.dolby.DolbySettingsActivity")),
        Intent().setComponent(ComponentName("com.realme.sound", "com.realme.sound.dolby.DolbySettingsActivity")),
        Intent().setComponent(ComponentName("com.oneplus.sound.dolby", "com.oneplus.sound.dolby.MainActivity")),
        Intent().setComponent(ComponentName("com.oplus.audio", "com.oplus.audio.DolbySettingsActivity")),
        Intent().setComponent(ComponentName("com.oplus.multimedia.sound", "com.oplus.multimedia.sound.dolby.DolbySettingsActivity")),

        // Lenovo dedicated Dolby
        Intent().setComponent(ComponentName("com.lenovo.dolby", "com.lenovo.dolby.MainActivity")),
        Intent().setComponent(ComponentName("com.lenovo.audioeffect", "com.lenovo.audioeffect.MainActivity")),

        // Sony Xperia Dolby Atmos
        Intent().setComponent(ComponentName("com.sonyericsson.soundenhancement", "com.sonyericsson.soundenhancement.DolbyAtmosActivity")),
        Intent().setComponent(ComponentName("com.sonymobile.soundenhancement", "com.sonymobile.soundenhancement.DolbyAtmosActivity")),

        // Asus ROG / Zenfone AudioWizard
        Intent().setComponent(ComponentName("com.asus.audiowizard", "com.asus.audiowizard.MainActivity")),

        // Transsion (Tecno / Infinix) Dolby Atmos
        Intent().setComponent(ComponentName("com.transsion.dolby", "com.transsion.dolby.MainActivity")),
        Intent().setComponent(ComponentName("com.transsion.soundeffect", "com.transsion.soundeffect.activity.DolbyActivity")),

        // ZTE / Nubia / RedMagic Dolby & Audio Effects
        Intent().setComponent(ComponentName("com.zte.zmusic", "com.zte.zmusic.dolby.DolbyActivity")),
        Intent().setComponent(ComponentName("com.zte.audioeffect", "com.zte.audioeffect.MainActivity")),

        // Vivo / iQOO Audio Effects
        Intent().setComponent(ComponentName("com.vivo.audiofx", "com.vivo.audiofx.MainActivity")),
    )
}
