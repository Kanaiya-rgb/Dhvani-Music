package com.music.dhvani.playback

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import com.music.dhvani.data.settings.OutputPcmMode
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Live facts about the Android output route, kept separate from source-format
 * statistics. The framework owns the final mixer/DAC decision, so these fields
 * deliberately describe the selected device's advertised capabilities rather
 * than pretending that an app can guarantee bit-perfect delivery.
 */
object AudioOutputStatus {
    data class OutputRoute(
        val id: String, // "speaker", "bluetooth", "wired"
        val name: String,
        val isSelected: Boolean,
        val isConnected: Boolean = true,
        val device: AudioDeviceInfo? = null,
    )

    data class Snapshot(
        val sink: String = "AudioTrack",
        val requestedPcmMode: OutputPcmMode = OutputPcmMode.PCM_16,
        val deviceName: String = getPhoneName(),
        val sampleRatesHz: IntArray = IntArray(0),
        val encodings: IntArray = IntArray(0),
        val isUsb: Boolean = false,
        val actualEncoding: Int? = null,
        val actualSampleRateHz: Int? = null,
        val floatFallback: Boolean = false,
        val decoderName: String? = null,
        val bufferSize: Int? = null,
    )

    val current = MutableStateFlow(Snapshot())
    val availableRoutes = MutableStateFlow<List<OutputRoute>>(emptyList())
    val selectedRouteId = MutableStateFlow<String>("auto")

    var onRouteSelectedListener: ((String) -> Unit)? = null

    fun selectRoute(routeId: String) {
        selectedRouteId.value = routeId
        onRouteSelectedListener?.invoke(routeId)
    }

    fun updateDevice(context: Context, manager: AudioManager) {
        val device = resolveActiveDevice(manager)
        val resolvedName = resolveDeviceDisplayName(context, device)
        val isUsb = device?.type in setOf(
            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_USB_ACCESSORY,
        )
        current.value = current.value.copy(
            deviceName = resolvedName,
            sampleRatesHz = device?.sampleRates ?: IntArray(0),
            encodings = device?.encodings ?: IntArray(0),
            isUsb = isUsb,
        )

        // Build list of available output routes
        val routes = mutableListOf<OutputRoute>()
        val phoneName = getPhoneName(context)

        val devices = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try { manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList() } catch (_: Throwable) { emptyList() }
        } else emptyList()

        // 1. Phone Speaker
        val speakerDevice = devices.firstOrNull {
            it.isSink && (it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER || it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE)
        }
        val isSpeakerActive = device == null || device.type in setOf(
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE,
            AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
        )
        routes.add(
            OutputRoute(
                id = "speaker",
                name = phoneName,
                isSelected = if (selectedRouteId.value == "auto") isSpeakerActive else selectedRouteId.value == "speaker",
                isConnected = true,
                device = speakerDevice,
            )
        )

        // 2. Bluetooth
        val btDevice = devices.firstOrNull {
            it.isSink && (
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && (
                    it.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_BLE_SPEAKER
                ))
            )
        }
        if (btDevice != null) {
            val isBtActive = device != null && (
                device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && (
                    device.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                    device.type == AudioDeviceInfo.TYPE_BLE_SPEAKER
                ))
            )
            val btName = btDevice.productName?.toString()?.trim().orEmpty()
            routes.add(
                OutputRoute(
                    id = "bluetooth",
                    name = if (isValidExternalName(btName)) btName else "Bluetooth Audio",
                    isSelected = if (selectedRouteId.value == "auto") isBtActive else selectedRouteId.value == "bluetooth",
                    isConnected = true,
                    device = btDevice,
                )
            )
        }

        // 3. Wired Headphones
        val wiredDevice = devices.firstOrNull {
            it.isSink && (
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
            )
        }
        if (wiredDevice != null) {
            val isWiredActive = device != null && (
                device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
            )
            routes.add(
                OutputRoute(
                    id = "wired",
                    name = "Wired Headphones",
                    isSelected = if (selectedRouteId.value == "auto") isWiredActive else selectedRouteId.value == "wired",
                    isConnected = true,
                    device = wiredDevice,
                )
            )
        }

        availableRoutes.value = routes
    }

    fun publish(
        manager: AudioManager,
        requestedPcmMode: OutputPcmMode = OutputPcmMode.PCM_16,
        device: AudioDeviceInfo? = null,
        floatEnabled: Boolean = false,
        context: Context? = null,
    ) {
        val activeDev = device ?: resolveActiveDevice(manager)
        val resolvedName = resolveDeviceDisplayName(context, activeDev)

        current.value = Snapshot(
            sink = "AudioTrack",
            requestedPcmMode = requestedPcmMode,
            deviceName = resolvedName,
            sampleRatesHz = activeDev?.sampleRates ?: IntArray(0),
            encodings = activeDev?.encodings ?: IntArray(0),
            isUsb = activeDev?.type in setOf(
                AudioDeviceInfo.TYPE_USB_DEVICE,
                AudioDeviceInfo.TYPE_USB_HEADSET,
                AudioDeviceInfo.TYPE_USB_ACCESSORY,
            ),
            floatFallback = requestedPcmMode == OutputPcmMode.FLOAT_32 && !floatEnabled,
            decoderName = current.value.decoderName,
        )
    }

    fun resolveActiveDevice(manager: AudioManager): AudioDeviceInfo? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return null

        // If user explicitly forced a route
        val devices = try {
            manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        } catch (_: Throwable) {
            return null
        }

        when (selectedRouteId.value) {
            "speaker" -> {
                val speaker = devices.firstOrNull {
                    dev -> dev.isSink && (
                        dev.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER ||
                        dev.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE ||
                        dev.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
                    )
                }
                if (speaker != null) return speaker
            }
            "bluetooth" -> {
                val bt = devices.firstOrNull { dev ->
                    dev.isSink && (
                        dev.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                        dev.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && (
                            dev.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                            dev.type == AudioDeviceInfo.TYPE_BLE_SPEAKER
                        ))
                    )
                }
                if (bt != null) return bt
            }
        }

        // Priority 1: Bluetooth
        val bluetooth = devices.firstOrNull { dev ->
            dev.isSink && (
                dev.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                dev.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && (
                    dev.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                    dev.type == AudioDeviceInfo.TYPE_BLE_SPEAKER ||
                    dev.type == AudioDeviceInfo.TYPE_BLE_BROADCAST
                ))
            )
        }
        if (bluetooth != null) return bluetooth

        // Priority 2: Wired / USB
        val wiredOrUsb = devices.firstOrNull { dev ->
            dev.isSink && (
                dev.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                dev.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                dev.type == AudioDeviceInfo.TYPE_USB_DEVICE ||
                dev.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                dev.type == AudioDeviceInfo.TYPE_USB_ACCESSORY ||
                dev.type == AudioDeviceInfo.TYPE_LINE_ANALOG ||
                dev.type == AudioDeviceInfo.TYPE_LINE_DIGITAL ||
                dev.type == AudioDeviceInfo.TYPE_HEARING_AID
            )
        }
        if (wiredOrUsb != null) return wiredOrUsb

        // Priority 3: Built-in speaker
        val speaker = devices.firstOrNull { dev ->
            dev.isSink && (
                dev.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER ||
                dev.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE ||
                dev.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
            )
        }
        return speaker ?: devices.firstOrNull { it.isSink }
    }

    fun resolveDeviceDisplayName(context: Context?, device: AudioDeviceInfo?): String {
        if (device == null) return getPhoneName(context)

        val type = device.type
        val rawName = device.productName?.toString()?.trim().orEmpty()

        return when {
            type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
            type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && (
                type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                type == AudioDeviceInfo.TYPE_BLE_SPEAKER ||
                type == AudioDeviceInfo.TYPE_BLE_BROADCAST
            )) -> {
                if (isValidExternalName(rawName)) rawName else "Bluetooth Audio"
            }

            type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
            type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
            type == AudioDeviceInfo.TYPE_LINE_ANALOG ||
            type == AudioDeviceInfo.TYPE_LINE_DIGITAL -> {
                if (isValidExternalName(rawName) && !rawName.equals("headset", ignoreCase = true)) rawName else "Wired Headphones"
            }

            type == AudioDeviceInfo.TYPE_USB_DEVICE ||
            type == AudioDeviceInfo.TYPE_USB_HEADSET ||
            type == AudioDeviceInfo.TYPE_USB_ACCESSORY -> {
                if (isValidExternalName(rawName)) rawName else "USB Audio"
            }

            type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER ||
            type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE ||
            type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> {
                getPhoneName(context)
            }

            else -> {
                if (isValidExternalName(rawName)) rawName else getPhoneName(context)
            }
        }
    }

    fun getPhoneName(context: Context? = null): String {
        // 1. Settings.Global "device_name" (e.g. "Redmi 12C" or user's custom phone name in Settings)
        if (context != null) {
            try {
                val name = Settings.Global.getString(context.contentResolver, "device_name")
                if (!name.isNullOrBlank() && !isInternalModelCode(name)) {
                    return name.trim()
                }
            } catch (_: Throwable) {}

            try {
                val btName = Settings.Secure.getString(context.contentResolver, "bluetooth_name")
                if (!btName.isNullOrBlank() && !isInternalModelCode(btName)) {
                    return btName.trim()
                }
            } catch (_: Throwable) {}

            try {
                val sysName = Settings.System.getString(context.contentResolver, "device_name")
                if (!sysName.isNullOrBlank() && !isInternalModelCode(sysName)) {
                    return sysName.trim()
                }
            } catch (_: Throwable) {}
        }

        // 2. System properties (Xiaomi/Redmi ro.product.marketname, Oppo, Vivo, Samsung, etc.)
        val propKeys = listOf(
            "ro.product.marketname",
            "ro.product.vendor.marketname",
            "ro.product.odm.marketname",
            "ro.config.marketing_name",
        )
        for (key in propKeys) {
            val prop = getSystemProperty(key)
            if (!prop.isNullOrBlank() && !isInternalModelCode(prop)) {
                return prop.trim()
            }
        }

        // 3. Bluetooth adapter name
        try {
            val btAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
            val btName = btAdapter?.name
            if (!btName.isNullOrBlank() && !isInternalModelCode(btName)) {
                return btName.trim()
            }
        } catch (_: Throwable) {}

        // 4. Model name if user friendly
        val model = Build.MODEL.trim()
        if (model.isNotBlank() && !isInternalModelCode(model)) {
            val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
            return if (model.startsWith(manufacturer, ignoreCase = true)) model else "$manufacturer $model"
        }

        // 5. Fallback
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        return if (manufacturer.isNotBlank()) "$manufacturer Phone" else "This Phone"
    }

    private fun getSystemProperty(key: String): String? {
        return try {
            val clazz = Class.forName("android.os.SystemProperties")
            val getMethod = clazz.getMethod("get", String::class.java)
            val value = getMethod.invoke(null, key) as? String
            value?.takeIf { it.isNotBlank() }
        } catch (_: Throwable) {
            null
        }
    }

    private fun isInternalModelCode(name: String): Boolean {
        val s = name.trim()
        if (s.isEmpty()) return true
        if (s.equals("System default", ignoreCase = true)) return true
        // If it starts with digits followed by letters/digits (e.g. 22120RN86I)
        if (s.matches(Regex("^[0-9]+[A-Za-z0-9_-]+$"))) return true
        // If it's a single code without spaces, containing uppercase letters and digits, length >= 7 (e.g. SM-G991B)
        if (s.length >= 7 && !s.contains(" ") && s.matches(Regex("^[A-Z0-9_-]+$")) && s.any { it.isDigit() }) return true
        return false
    }

    private fun isValidExternalName(name: String): Boolean {
        val s = name.trim()
        if (s.isEmpty()) return false
        if (s.equals("System default", ignoreCase = true)) return false
        if (s.equals(Build.MODEL, ignoreCase = true)) return false
        if (isInternalModelCode(s)) return false
        return true
    }

    fun publishDecoder(decoderName: String?) {
        current.value = current.value.copy(decoderName = decoderName)
    }

    fun publishAudioTrack(encoding: Int, sampleRateHz: Int, bufferSize: Int? = null) {
        current.value = current.value.copy(
            actualEncoding = encoding,
            actualSampleRateHz = sampleRateHz,
            bufferSize = bufferSize ?: current.value.bufferSize,
            floatFallback = current.value.requestedPcmMode == OutputPcmMode.FLOAT_32 &&
                encoding != AudioFormat.ENCODING_PCM_FLOAT,
        )
    }

    fun reset() {
        current.value = Snapshot()
    }

    fun encodingLabel(snapshot: Snapshot): String = when (snapshot.actualEncoding) {
        AudioFormat.ENCODING_PCM_FLOAT -> "32-bit float"
        AudioFormat.ENCODING_PCM_16BIT -> if (snapshot.floatFallback) "16-bit fallback" else "16-bit PCM"
        null -> if (snapshot.floatFallback) "16-bit fallback" else snapshot.requestedPcmMode.label
        else -> "PCM (${snapshot.actualEncoding})"
    }
}
