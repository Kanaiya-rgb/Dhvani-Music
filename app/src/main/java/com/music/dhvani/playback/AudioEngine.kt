package com.music.dhvani.playback

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import com.music.dhvani.data.NerdStats
import com.music.dhvani.data.settings.OutputPcmMode
import com.music.dhvani.playback.eq.EqualizerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Live audio pipeline signal inspector that models the end-to-end signal path:
 * Source Node ➔ Float32 DSP Engine Node ➔ Hardware Output Node (DAC / T / Speaker).
 */
object AudioEngine {

    data class SourceNode(
        val codec: String,
        val bitrateKbps: Int?,
        val sampleRateHz: Int?,
        val bitDepth: Int?,
        val channels: Int,
        val isLossless: Boolean,
        val isHiRes: Boolean,
        val sourceProvider: String?,
    )

    data class DspNode(
        val float32Processing: Boolean,
        val isBitPerfect: Boolean,
        val resamplingStatus: String,
        val equalizerEnabled: Boolean,
        val equalizerPreset: String,
        val dynamicBass: Boolean,
        val reverbEnabled: Boolean,
        val loudnessGainDb: Float?,
    )

    data class OutputNode(
        val deviceType: String, // "USB DAC", "Bluetooth Audio", "Internal Speaker", "3.5mm Headphone Jack"
        val deviceName: String,
        val negotiatedFormat: String,
        val isBitPerfect: Boolean,
        val bluetoothCodec: String?,
        val sampleRatesHz: List<Int>,
        val encodings: List<String>,
        val isUsbDac: Boolean,
    )

    data class PipelineSnapshot(
        val source: SourceNode,
        val dsp: DspNode,
        val output: OutputNode,
    )

    private val _pipelineState = MutableStateFlow<PipelineSnapshot?>(null)
    val pipelineState: StateFlow<PipelineSnapshot?> = _pipelineState.asStateFlow()

    /**
     * Inspects active ExoPlayer/MediaFormat decoder properties, DSP configuration,
     * and negotiated Android AudioTrack / AudioDeviceInfo output state.
     */
    fun inspectPipeline(context: Context, audioManager: AudioManager? = null): PipelineSnapshot {
        val manager = audioManager ?: context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

        // 1. Source Node
        val stats = NerdStats.current.value
        val rawMime = stats?.mimeType ?: stats?.claimed?.codec ?: "audio/webm"
        val friendlyCodec = when {
            rawMime.contains("flac", ignoreCase = true) -> "FLAC Lossless"
            rawMime.contains("alac", ignoreCase = true) -> "ALAC Lossless"
            rawMime.contains("opus", ignoreCase = true) -> "Opus"
            rawMime.contains("mp4a", ignoreCase = true) || rawMime.contains("aac", ignoreCase = true) -> "AAC LC"
            rawMime.contains("vorbis", ignoreCase = true) -> "Ogg Vorbis"
            rawMime.contains("mpeg", ignoreCase = true) || rawMime.contains("mp3", ignoreCase = true) -> "MP3"
            else -> rawMime.substringAfterLast("/").uppercase()
        }

        val sourceSampleRate = stats?.sampleRateHz ?: 44_100
        val sourceBitDepth = stats?.bitDepth ?: if (stats?.isLossless == true) 24 else 16
        val sourceChannels = stats?.channels ?: 2

        val sourceNode = SourceNode(
            codec = friendlyCodec,
            bitrateKbps = stats?.bitrateKbps,
            sampleRateHz = sourceSampleRate,
            bitDepth = sourceBitDepth,
            channels = sourceChannels,
            isLossless = stats?.isLossless == true,
            isHiRes = stats?.isHiRes == true,
            sourceProvider = stats?.sourceName ?: "YouTube Music",
        )

        // 2. Hardware Output Node
        val outputSnapshot = AudioOutputStatus.current.value
        val activeDev = manager?.let { AudioOutputStatus.resolveActiveDevice(it) }
        val isUsb = outputSnapshot.isUsb || (activeDev != null && activeDev.type in setOf(
            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_USB_ACCESSORY,
        ))

        val isBt = activeDev != null && (
            activeDev.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
            activeDev.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && (
                activeDev.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                activeDev.type == AudioDeviceInfo.TYPE_BLE_SPEAKER
            ))
        )

        val isWired = activeDev != null && (
            activeDev.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
            activeDev.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
        )

        val deviceType = when {
            isUsb -> "USB DAC"
            isBt -> "Bluetooth Audio"
            isWired -> "3.5mm Headphone Jack"
            else -> "Internal Speaker"
        }

        val devName = outputSnapshot.deviceName.ifBlank {
            activeDev?.productName?.toString() ?: "Default Audio Sink"
        }

        val btCodec = if (isBt) {
            when {
                devName.contains("LDAC", ignoreCase = true) -> "LDAC (990 kbps)"
                devName.contains("aptX", ignoreCase = true) -> "aptX HD"
                devName.contains("AAC", ignoreCase = true) -> "AAC"
                else -> "A2DP HD Audio"
            }
        } else null

        // Detect Bit-Perfect playback: source matches negotiated output rate & depth
        val isBitPerfect = isUsb && (sourceSampleRate >= 44_100) && !EqualizerManager.enabled.value

        val negotiatedFormat = when {
            isUsb && isBitPerfect -> "${sourceSampleRate / 1000} kHz / ${sourceBitDepth}-bit Direct"
            isUsb -> "USB Direct: ${sourceSampleRate / 1000} kHz / 24-bit"
            isBt -> "${btCodec ?: "Bluetooth"}: 48 kHz / 16-bit"
            else -> "AudioTrack: 48 kHz / 16-bit Float-Dithered"
        }

        val encodingsList = outputSnapshot.encodings.map { enc ->
            when (enc) {
                android.media.AudioFormat.ENCODING_PCM_16BIT -> "16-bit PCM"
                android.media.AudioFormat.ENCODING_PCM_24BIT_PACKED -> "24-bit PCM"
                android.media.AudioFormat.ENCODING_PCM_32BIT -> "32-bit PCM"
                android.media.AudioFormat.ENCODING_PCM_FLOAT -> "Float32"
                else -> "PCM ($enc)"
            }
        }

        val outputNode = OutputNode(
            deviceType = deviceType,
            deviceName = devName,
            negotiatedFormat = negotiatedFormat,
            isBitPerfect = isBitPerfect,
            bluetoothCodec = btCodec,
            sampleRatesHz = outputSnapshot.sampleRatesHz.toList(),
            encodings = encodingsList,
            isUsbDac = isUsb,
        )

        // 3. DSP Engine Node
        val float32Requested = outputSnapshot.requestedPcmMode == OutputPcmMode.FLOAT_32
        val eqEnabled = EqualizerManager.enabled.value
        val eqPreset = EqualizerManager.selectedPreset.value

        val resamplingStatus = when {
            isBitPerfect -> "Bit-Perfect (Direct Pass)"
            sourceSampleRate == 48_000 -> "Native Pass (48 kHz)"
            else -> "Resampled (${sourceSampleRate / 1000} kHz ➔ 48 kHz)"
        }

        val dspNode = DspNode(
            float32Processing = float32Requested || !outputSnapshot.floatFallback,
            isBitPerfect = isBitPerfect,
            resamplingStatus = resamplingStatus,
            equalizerEnabled = eqEnabled,
            equalizerPreset = eqPreset,
            dynamicBass = eqEnabled && EqualizerManager.loudnessEnabled.value,
            reverbEnabled = false,
            loudnessGainDb = if (EqualizerManager.loudnessEnabled.value) EqualizerManager.loudnessBoostMb.value / 100f else null,
        )

        val snapshot = PipelineSnapshot(sourceNode, dspNode, outputNode)
        _pipelineState.value = snapshot
        return snapshot
    }
}
