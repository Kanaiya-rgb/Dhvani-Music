package com.music.dhvani.playback

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.provider.Settings
import android.util.Log
import android.widget.Toast

object SystemEqualizerUtils {
    private const val TAG = "SystemEqualizerUtils"

    /**
     * Broadcasts open signal and launches the system equalizer or audio effects control panel.
     * Supports:
     * 1. Standard Android AudioEffect control panel (MusicFX, AOSP, LineageOS)
     * 2. Xiaomi / Redmi / POCO (MIUI / HyperOS AudioEffect & Sound effects)
     * 3. Samsung One UI (SoundAlive & Sound Quality and Effects)
     * 4. OnePlus / Oppo / Realme (ColorOS / OxygenOS Sound Effects)
     * 5. Motorola (AudioFX & MotoAudio)
     * 6. Vivo / iQOO (AudioFx & Sound Effects)
     * 7. Asus (AudioWizard)
     * 8. Sony Xperia (SoundEnhancement)
     * 9. Installed 3rd-party equalizer apps (Wavelet, Poweramp EQ, etc.)
     * 10. Guaranteed fallback to System Sound & Vibration Settings
     */
    fun openSystemEqualizer(context: Context, sessionId: Int = 0): Boolean {
        // 1. Notify audio effect engine to open session
        try {
            val openIntent = Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                if (sessionId > 0) {
                    putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
                }
                putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            }
            context.sendBroadcast(openIntent)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to broadcast ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION", e)
        }

        val pm = context.packageManager

        // 2. Try standard Android AudioEffect ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL
        try {
            val displayIntent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                if (sessionId > 0) {
                    putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
                }
                putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            }
            if (pm != null && displayIntent.resolveActivity(pm) != null) {
                context.startActivity(displayIntent)
                return true
            }
        } catch (e: Exception) {
            Log.d(TAG, "ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL failed", e)
        }

        // 3. Try OEM-specific equalizer and sound effect activities
        val oemIntents = getOemEqualizerIntents(context, sessionId)
        for (intent in oemIntents) {
            try {
                if (pm != null && intent.resolveActivity(pm) != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    return true
                }
            } catch (e: Exception) {
                Log.d(TAG, "OEM intent failed: $intent", e)
            }
        }

        // 4. Try installed third-party equalizers (Wavelet, Poweramp, etc.)
        val thirdPartyIntents = getThirdPartyEqualizerIntents(context)
        for (intent in thirdPartyIntents) {
            try {
                if (pm != null && intent.resolveActivity(pm) != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    return true
                }
            } catch (_: Exception) {}
        }

        // 5. Guaranteed fallback: System Sound Settings
        val soundSettingsIntents = listOf(
            Intent(Settings.ACTION_SOUND_SETTINGS),
            Intent("android.settings.SOUND_SETTINGS"),
            Intent("miui.intent.action.SOUND_SETTINGS"),
            Intent(Settings.ACTION_SETTINGS),
        )

        for (intent in soundSettingsIntents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                Toast.makeText(context, "Opening device sound & equalizer settings", Toast.LENGTH_SHORT).show()
                return true
            } catch (_: Exception) {}
        }

        Toast.makeText(context, "Could not open system equalizer on this device", Toast.LENGTH_SHORT).show()
        return false
    }

    private fun getOemEqualizerIntents(context: Context, sessionId: Int): List<Intent> {
        val list = mutableListOf<Intent>()

        fun makeIntent(pkg: String, cls: String): Intent {
            return Intent().apply {
                component = ComponentName(pkg, cls)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                if (sessionId > 0) putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
                putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            }
        }

        // Xiaomi / Redmi / POCO (MIUI / HyperOS)
        list.add(makeIntent("com.miui.audioeffect", "com.miui.audioeffect.AudioEffectActivity"))
        list.add(makeIntent("com.miui.audioeffect", "com.miui.audioeffect.MiuiAudioEffectActivity"))
        list.add(Intent("com.miui.audioeffect.action.AUDIO_EFFECT"))
        list.add(Intent("miui.intent.action.SOUND_EFFECT"))

        // Samsung One UI (SoundAlive & Sound Quality)
        list.add(makeIntent("com.sec.android.app.soundalive", "com.sec.android.app.soundalive.activity.SoundAliveMainActivity"))
        list.add(makeIntent("com.sec.android.app.soundalive", "com.sec.android.app.soundalive.activity.MusicEqualizerActivity"))
        list.add(makeIntent("com.sec.android.app.soundalive", "com.sec.android.app.soundalive.ui.EqualizerActivity"))
        list.add(makeIntent("com.sec.android.app.soundalive", "com.sec.android.app.soundalive.ui.SoundAliveMainActivity"))
        list.add(makeIntent("com.sec.android.app.soundalive", "com.sec.android.app.soundalive.MainActivity"))
        list.add(makeIntent("com.android.settings", "com.android.settings.Settings\$SoundQualityAndEffectsActivity"))
        list.add(Intent("com.sec.android.app.soundalive.ACTION_SOUND_ALIVE"))
        list.add(Intent("com.sec.android.app.soundalive.action.EQUALIZER"))

        // OnePlus / OPPO / Realme (ColorOS / OxygenOS)
        list.add(makeIntent("com.oplus.sound", "com.oplus.sound.AudioEffectMainActivity"))
        list.add(makeIntent("com.oplus.sound", "com.oplus.sound.activity.SoundEffectMainActivity"))
        list.add(makeIntent("com.coloros.sound", "com.coloros.sound.AudioEffectActivity"))
        list.add(makeIntent("com.oppo.music", "com.oppo.music.EqualizerActivity"))
        list.add(Intent("com.realme.sound.AUDIO_EFFECT"))
        list.add(Intent("com.oplus.sound.AUDIO_EFFECT"))

        // Motorola MotoAudio / AudioFX
        list.add(makeIntent("com.motorola.audiofx", "com.motorola.audiofx.ActivityMusic"))
        list.add(makeIntent("com.motorola.motoaudio", "com.motorola.motoaudio.AudioMainActivity"))

        // Vivo / iQOO
        list.add(makeIntent("com.vivo.audiofx", "com.vivo.audiofx.AudioFxActivity"))
        list.add(makeIntent("com.vivo.sound", "com.vivo.sound.SoundEffectActivity"))
        list.add(makeIntent("com.vivo.audioeffect", "com.vivo.audioeffect.AudioEffectActivity"))

        // Asus AudioWizard
        list.add(makeIntent("com.asus.audiowizard", "com.asus.audiowizard.AudioWizardActivity"))
        list.add(makeIntent("com.asus.audiowizard", "com.asus.audiowizard.MainActivity"))

        // Sony Xperia SoundEnhancement
        list.add(makeIntent("com.sonyericsson.soundenhancement", "com.sonyericsson.soundenhancement.SoundEnhancementActivity"))

        // LineageOS / Cyanogen AudioFX
        list.add(makeIntent("org.lineageos.audiofx", "org.lineageos.audiofx.activity.ActivityMusic"))
        list.add(makeIntent("com.cyanogenmod.audiofx", "com.cyanogenmod.audiofx.activity.ActivityMusic"))

        // AOSP standard MusicFX
        list.add(makeIntent("com.android.musicfx", "com.android.musicfx.ActivityMusic"))

        return list
    }

    private fun getThirdPartyEqualizerIntents(context: Context): List<Intent> {
        val list = mutableListOf<Intent>()
        fun makeLaunchIntent(packageName: String): Intent? {
            return context.packageManager.getLaunchIntentForPackage(packageName)
        }

        // Wavelet
        makeLaunchIntent("com.pittvandewitt.wavelet")?.let { list.add(it) }
        // Poweramp Equalizer
        makeLaunchIntent("com.maxmpz.audioplayer.equalizer")?.let { list.add(it) }
        // Flat Equalizer
        makeLaunchIntent("com.jazibkhan.equalizer")?.let { list.add(it) }
        // SpotiQ
        makeLaunchIntent("com.fr.spotiq")?.let { list.add(it) }

        return list
    }
}
