package com.music.dhvani.playback

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.util.Log
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
        val id: String, // "speaker", "bluetooth", "bluetooth_123", "wired", "usb_123"
        val name: String,
        val isSelected: Boolean,
        val isConnected: Boolean = true,
        val isBluetooth: Boolean = false,
        val isWired: Boolean = false,
        val isUsb: Boolean = false,
        val isSpeaker: Boolean = false,
        val device: AudioDeviceInfo? = null,
    )

    data class Snapshot(
        val sink: String = "AudioTrack",
        val requestedPcmMode: OutputPcmMode = OutputPcmMode.PCM_16,
        val deviceName: String = getPhoneName(),
        val sampleRatesHz: IntArray = IntArray(0),
        val encodings: IntArray = IntArray(0),
        val isUsb: Boolean = false,
        val isBluetooth: Boolean = false,
        val isWired: Boolean = false,
        val isSpeaker: Boolean = true,
        val actualEncoding: Int? = null,
        val actualSampleRateHz: Int? = null,
        val floatFallback: Boolean = false,
        val decoderName: String? = null,
        val bufferSize: Int? = null,
    )

    val current = MutableStateFlow(Snapshot())
    val availableRoutes = MutableStateFlow<List<OutputRoute>>(emptyList())
    val selectedRouteId = MutableStateFlow<String>("speaker")
    val confirmedActiveRouteId = MutableStateFlow<String>("speaker")
    val confirmedActiveDevice = MutableStateFlow<AudioDeviceInfo?>(null)
    val errorMessage = MutableStateFlow<String?>(null)

    var onRouteSelectedListener: ((String) -> Boolean)? = null

    fun isMediaBluetoothDevice(dev: AudioDeviceInfo): Boolean {
        if (!dev.isSink) return false
        return dev.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && (
                dev.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                dev.type == AudioDeviceInfo.TYPE_BLE_SPEAKER ||
                dev.type == AudioDeviceInfo.TYPE_BLE_BROADCAST
            ))
    }

    fun isWiredDevice(dev: AudioDeviceInfo): Boolean {
        if (!dev.isSink) return false
        return dev.type in setOf(
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_LINE_ANALOG,
            AudioDeviceInfo.TYPE_LINE_DIGITAL,
        )
    }

    fun isUsbDevice(dev: AudioDeviceInfo): Boolean {
        if (!dev.isSink) return false
        return dev.type in setOf(
            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_USB_ACCESSORY,
        )
    }

    fun isSpeakerDevice(dev: AudioDeviceInfo): Boolean {
        if (!dev.isSink) return false
        return dev.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER ||
            dev.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE
    }

    fun findBuiltInSpeaker(devices: Iterable<AudioDeviceInfo>): AudioDeviceInfo? {
        return devices.firstOrNull { it.isSink && it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
            ?: devices.firstOrNull { it.isSink && it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE }
            ?: devices.firstOrNull { it.isSink && !isMediaBluetoothDevice(it) && !isWiredDevice(it) && !isUsbDevice(it) && it.type != AudioDeviceInfo.TYPE_BUILTIN_EARPIECE }
    }

    fun selectRoute(routeId: String): Boolean {
        Log.d("MediaOutput", "User requested route: $routeId")
        errorMessage.value = null
        val listener = onRouteSelectedListener
        if (listener == null) {
            Log.w("MediaOutput", "Route request failed: PlaybackService onRouteSelectedListener is null")
            errorMessage.value = "Playback service unavailable"
            return false
        }
        val success = listener.invoke(routeId)
        Log.d("MediaOutput", "Route request result for $routeId: success=$success")
        if (!success) {
            if (errorMessage.value == null) {
                errorMessage.value = "Could not route playback to selected device"
            }
        }
        return success
    }

    /**
     * Called when the ExoPlayer AudioTrack confirms hardware playout routing or
     * when the system audio routing changes.
     */
    fun onActiveDeviceConfirmed(context: Context, device: AudioDeviceInfo?) {
        val isBt = device != null && isMediaBluetoothDevice(device)
        val isWired = device != null && isWiredDevice(device)
        val isUsb = device != null && isUsbDevice(device)
        val isSpeaker = device == null || isSpeakerDevice(device)

        val resolvedName = resolveDeviceDisplayName(context, device)
        Log.d("MediaOutput", "Confirmed active route: '$resolvedName' (type=${device?.type}, id=${device?.id}, isBt=$isBt, isSpeaker=$isSpeaker)")

        confirmedActiveDevice.value = device
        errorMessage.value = null

        val activeRouteId = when {
            isBt -> {
                val match = availableRoutes.value.firstOrNull { it.device?.id == device.id }
                match?.id ?: if (availableRoutes.value.count { it.isBluetooth } <= 1) "bluetooth" else "bluetooth_${device.id}"
            }
            isWired -> "wired"
            isUsb -> {
                val match = availableRoutes.value.firstOrNull { it.device?.id == device.id }
                match?.id ?: if (availableRoutes.value.count { it.isUsb } <= 1) "usb" else "usb_${device.id}"
            }
            else -> "speaker"
        }

        confirmedActiveRouteId.value = activeRouteId
        selectedRouteId.value = activeRouteId

        current.value = current.value.copy(
            deviceName = resolvedName,
            isBluetooth = isBt,
            isWired = isWired,
            isUsb = isUsb,
            isSpeaker = isSpeaker,
            sampleRatesHz = device?.sampleRates ?: current.value.sampleRatesHz,
            encodings = device?.encodings ?: current.value.encodings,
        )

        // Keep availableRoutes synchronized with real active state
        val updatedRoutes = availableRoutes.value.map { route ->
            val isSelected = when {
                activeRouteId == "speaker" -> route.id == "speaker"
                activeRouteId.startsWith("bluetooth") -> route.id == activeRouteId || (route.isBluetooth && route.device?.id == device?.id)
                activeRouteId.startsWith("usb") -> route.id == activeRouteId || (route.isUsb && route.device?.id == device?.id)
                activeRouteId == "wired" -> route.id == "wired"
                else -> false
            }
            route.copy(isSelected = isSelected)
        }
        if (updatedRoutes.isNotEmpty()) {
            availableRoutes.value = updatedRoutes
        }
    }

    fun updateDevice(context: Context, manager: AudioManager) {
        val devices = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try { manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList() } catch (_: Throwable) { emptyList() }
        } else emptyList()

        val activeDev = confirmedActiveDevice.value?.takeIf { confirmed ->
            devices.any { it.id == confirmed.id }
        } ?: resolveActiveDevice(manager)

        val resolvedName = resolveDeviceDisplayName(context, activeDev)
        val isUsb = activeDev != null && isUsbDevice(activeDev)
        val isBt = activeDev != null && isMediaBluetoothDevice(activeDev)
        val isWired = activeDev != null && isWiredDevice(activeDev)
        val isSpeaker = activeDev == null || isSpeakerDevice(activeDev)

        current.value = current.value.copy(
            deviceName = resolvedName,
            sampleRatesHz = activeDev?.sampleRates ?: IntArray(0),
            encodings = activeDev?.encodings ?: IntArray(0),
            isUsb = isUsb,
            isBluetooth = isBt,
            isWired = isWired,
            isSpeaker = isSpeaker,
        )

        // Build list of available output routes
        val routes = mutableListOf<OutputRoute>()
        val phoneName = getPhoneName(context)

        // 1. Phone Speaker
        val speakerDevice = findBuiltInSpeaker(devices)
        val isSpeakerSelected = (confirmedActiveDevice.value == null && isSpeaker) ||
            (confirmedActiveDevice.value != null && isSpeakerDevice(confirmedActiveDevice.value!!)) ||
            (selectedRouteId.value == "speaker" && confirmedActiveDevice.value == null)
        routes.add(
            OutputRoute(
                id = "speaker",
                name = phoneName,
                isSelected = isSpeakerSelected,
                isConnected = true,
                isSpeaker = true,
                device = speakerDevice,
            )
        )

        // 2. Bluetooth Sinks (Deduplicated, media-only: strictly exclude SCO)
        val btDevices = devices.filter { dev -> isMediaBluetoothDevice(dev) }.distinctBy { dev ->
            val addr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                dev.address.orEmpty().ifBlank { dev.productName?.toString().orEmpty().trim() }
            } else {
                dev.productName?.toString().orEmpty().trim()
            }
            addr.ifBlank { dev.id.toString() }
        }

        btDevices.forEach { btDev ->
            val routeId = if (btDevices.size == 1) "bluetooth" else "bluetooth_${btDev.id}"
            val isBtConfirmed = confirmedActiveDevice.value != null && confirmedActiveDevice.value!!.id == btDev.id
            val rawName = btDev.productName?.toString()?.trim().orEmpty()
            val btName = if (isValidExternalName(rawName)) rawName else "Bluetooth Audio"
            routes.add(
                OutputRoute(
                    id = routeId,
                    name = btName,
                    isSelected = isBtConfirmed,
                    isConnected = true,
                    isBluetooth = true,
                    device = btDev,
                )
            )
        }

        // 3. USB Audio Devices (DACs, USB Headsets)
        val usbDevices = devices.filter { dev -> isUsbDevice(dev) }
        usbDevices.forEach { usbDev ->
            val routeId = if (usbDevices.size == 1) "usb" else "usb_${usbDev.id}"
            val isUsbConfirmed = confirmedActiveDevice.value != null && confirmedActiveDevice.value!!.id == usbDev.id
            val rawName = usbDev.productName?.toString()?.trim().orEmpty()
            val usbName = if (isValidExternalName(rawName)) rawName else "USB Audio Device"
            routes.add(
                OutputRoute(
                    id = routeId,
                    name = usbName,
                    isSelected = isUsbConfirmed,
                    isConnected = true,
                    isUsb = true,
                    device = usbDev,
                )
            )
        }

        // 4. Wired Headphones / Line out
        val wiredDevice = devices.firstOrNull { isWiredDevice(it) }
        if (wiredDevice != null) {
            val isWiredConfirmed = confirmedActiveDevice.value != null && isWiredDevice(confirmedActiveDevice.value!!)
            routes.add(
                OutputRoute(
                    id = "wired",
                    name = "Wired Headphones",
                    isSelected = isWiredConfirmed,
                    isConnected = true,
                    isWired = true,
                    device = wiredDevice,
                )
            )
        }

        availableRoutes.value = routes
        Log.d("MediaOutput", "Available routes updated: ${routes.map { "${it.name} (id=${it.id}, selected=${it.isSelected})" }}")
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

        val devices = try {
            manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList()
        } catch (_: Throwable) {
            return null
        }

        when {
            selectedRouteId.value == "speaker" -> {
                val speaker = findBuiltInSpeaker(devices)
                if (speaker != null) return speaker
            }
            selectedRouteId.value.startsWith("bluetooth") -> {
                val specificId = selectedRouteId.value.removePrefix("bluetooth_").removePrefix("bluetooth").takeIf { it.isNotBlank() }?.toIntOrNull()
                if (specificId != null) {
                    val specific = devices.firstOrNull { it.id == specificId && isMediaBluetoothDevice(it) }
                    if (specific != null) return specific
                }
                val bt = devices.firstOrNull { isMediaBluetoothDevice(it) }
                if (bt != null) return bt
            }
            selectedRouteId.value.startsWith("usb") -> {
                val specificId = selectedRouteId.value.removePrefix("usb_").removePrefix("usb").takeIf { it.isNotBlank() }?.toIntOrNull()
                if (specificId != null) {
                    val specific = devices.firstOrNull { it.id == specificId && isUsbDevice(it) }
                    if (specific != null) return specific
                }
                val usb = devices.firstOrNull { isUsbDevice(it) }
                if (usb != null) return usb
            }
            selectedRouteId.value == "wired" -> {
                val wired = devices.firstOrNull { isWiredDevice(it) }
                if (wired != null) return wired
            }
        }

        // Priority 1: Bluetooth (Media only, no SCO)
        val bluetooth = devices.firstOrNull { isMediaBluetoothDevice(it) }
        if (bluetooth != null) return bluetooth

        // Priority 2: Wired / USB
        val wiredOrUsb = devices.firstOrNull { isWiredDevice(it) || isUsbDevice(it) }
        if (wiredOrUsb != null) return wiredOrUsb

        // Priority 3: Built-in speaker
        val speaker = findBuiltInSpeaker(devices)
        return speaker ?: devices.firstOrNull { it.isSink && it.type != AudioDeviceInfo.TYPE_BUILTIN_EARPIECE }
    }

    fun resolveDeviceDisplayName(context: Context?, device: AudioDeviceInfo?): String {
        if (device == null) return getPhoneName(context)

        val rawName = device.productName?.toString()?.trim().orEmpty()

        return when {
            isMediaBluetoothDevice(device) -> {
                if (isValidExternalName(rawName)) rawName else "Bluetooth Audio"
            }

            isWiredDevice(device) -> {
                if (isValidExternalName(rawName) && !rawName.equals("headset", ignoreCase = true)) rawName else "Wired Headphones"
            }

            isUsbDevice(device) -> {
                if (isValidExternalName(rawName)) rawName else "USB Audio"
            }

            isSpeakerDevice(device) -> {
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

    fun isMiuiOrHyperOs(): Boolean {
        val brand = Build.BRAND.lowercase()
        val manufacturer = Build.MANUFACTURER.lowercase()
        if (brand.contains("xiaomi") || brand.contains("redmi") || brand.contains("poco") ||
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco")
        ) {
            return true
        }
        val miuiProp = getSystemProperty("ro.miui.ui.version.name")
        if (!miuiProp.isNullOrBlank()) return true
        val miuiCode = getSystemProperty("ro.miui.ui.version.code")
        if (!miuiCode.isNullOrBlank()) return true
        val hyperOs = getSystemProperty("ro.mi.os.version.name")
        if (!hyperOs.isNullOrBlank()) return true
        return false
    }

    /**
     * Attempts to launch Android's native system output switcher for the active MediaSession.
     * Returns true if successfully launched; returns false if unavailable or unsupported on the device/OEM.
     */
    fun launchSystemMediaOutputSwitcher(context: Context): Boolean {
        // 1. Media output switcher panels require Android 11+ (API 30+)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            Log.d("MediaOutput", "Android ${Build.VERSION.SDK_INT} < 30: System media output panel unsupported; using in-app picker")
            return false
        }

        // 2. Xiaomi / MIUI / HyperOS check:
        // Xiaomi custom ROMs strip or stub out AOSP Settings panels and slices, causing
        // intent resolution to match but the activity to immediately finish/swallow without
        // displaying any UI. Do not rely on private AOSP Settings intents on MIUI.
        if (isMiuiOrHyperOs()) {
            Log.d("MediaOutput", "Xiaomi/MIUI device detected (${Build.MANUFACTURER} ${Build.MODEL}). System output panel unsupported; opening in-app Media Output sheet")
            return false
        }

        val sessionToken = PlaybackService.activeMediaSession?.platformToken
        if (sessionToken == null) {
            Log.d("MediaOutput", "No active MediaSession platformToken found; using in-app Media Output sheet")
            return false
        }

        val packageName = context.packageName

        // 3. For supported non-MIUI devices (such as Google Pixel), attempt system panel if supported
        try {
            val panelIntent = Intent("com.android.settings.panel.action.MEDIA_OUTPUT").apply {
                putExtra("com.android.settings.panel.extra.PACKAGE_NAME", packageName)
                putExtra("package_name", packageName)
                putExtra("key_media_session_token", sessionToken)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.queryIntentActivities(
                    panelIntent,
                    PackageManager.ResolveInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.queryIntentActivities(panelIntent, 0)
            }

            // Only attempt launch if supported by Google Pixel or authentic AOSP settings handler
            val isGooglePixelOrAosp = Build.MANUFACTURER.equals("Google", ignoreCase = true) ||
                resolveInfo.any { it.activityInfo?.packageName == "com.android.settings" || it.activityInfo?.packageName == "com.google.android.settings" }

            if (resolveInfo.isNotEmpty() && isGooglePixelOrAosp) {
                Log.d("MediaOutput", "Launching supported system media output panel on ${Build.MANUFACTURER} ${Build.MODEL}")
                context.startActivity(panelIntent)
                return true
            } else {
                Log.d("MediaOutput", "System media output panel not resolved or unsupported by OEM (${Build.MANUFACTURER}); using in-app picker")
            }
        } catch (e: Throwable) {
            Log.w("MediaOutput", "System media output panel launch failed: ${e.message}; using in-app picker")
        }

        return false
    }
}
