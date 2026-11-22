package com.music.dhvani.playback

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.ui.graphics.vector.ImageVector
import com.music.dhvani.data.settings.AppSettings
import com.music.dhvani.data.settings.OutputPcmMode
import com.music.dhvani.ui.icons.DhvaniIcons
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Live facts about the Android output route, kept separate from source-format
 * statistics. The framework owns the final mixer/DAC decision, so these fields
 * deliberately describe the selected device's advertised capabilities rather
 * than pretending that an app can guarantee bit-perfect delivery.
 */
object AudioOutputStatus {
    enum class AudioDeviceCategory {
        PHONE,
        EARBUDS,
        HEADPHONES,
        NECKBAND,
        SINGLE_EAR_HEADSET,
        BLUETOOTH_SPEAKER,
        CAR_AUDIO,
        WIRED,
        USB,
    }

    fun iconFor(category: AudioDeviceCategory): ImageVector = when (category) {
        AudioDeviceCategory.PHONE -> Icons.Rounded.PhoneAndroid
        AudioDeviceCategory.EARBUDS -> DhvaniIcons.Earbuds
        AudioDeviceCategory.HEADPHONES -> DhvaniIcons.Headphone
        AudioDeviceCategory.BLUETOOTH_SPEAKER -> DhvaniIcons.BluetoothSpeaker
        AudioDeviceCategory.CAR_AUDIO -> DhvaniIcons.CarAudio
        AudioDeviceCategory.WIRED -> DhvaniIcons.WiredEarphones
        AudioDeviceCategory.USB -> DhvaniIcons.WiredEarphones
        AudioDeviceCategory.SINGLE_EAR_HEADSET -> DhvaniIcons.SingleEarHeadset
        AudioDeviceCategory.NECKBAND -> DhvaniIcons.Neckband
    }

    fun subtitleFor(category: AudioDeviceCategory): String = when (category) {
        AudioDeviceCategory.PHONE -> "Phone Speaker"
        AudioDeviceCategory.EARBUDS -> "Wireless Earbuds"
        AudioDeviceCategory.HEADPHONES -> "Bluetooth Headphones"
        AudioDeviceCategory.BLUETOOTH_SPEAKER -> "Bluetooth Speaker"
        AudioDeviceCategory.CAR_AUDIO -> "Car Audio System"
        AudioDeviceCategory.WIRED -> "Wired Audio Port"
        AudioDeviceCategory.USB -> "USB Audio Device"
        AudioDeviceCategory.SINGLE_EAR_HEADSET -> "Single-Ear Bluetooth Headset"
        AudioDeviceCategory.NECKBAND -> "Wireless Neckband"
    }

    data class OutputRoute(
        val id: String, // "speaker", "bluetooth", "bluetooth_123", "wired", "usb_123"
        val name: String,
        val isSelected: Boolean,
        val isConnected: Boolean = true,
        val isBluetooth: Boolean = false,
        val isWired: Boolean = false,
        val isUsb: Boolean = false,
        val isSpeaker: Boolean = false,
        val deviceCategory: AudioDeviceCategory = AudioDeviceCategory.PHONE,
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
        val deviceCategory: AudioDeviceCategory = AudioDeviceCategory.PHONE,
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
    var userExplicitlySelectedRoute: Boolean = false

    data class AudioDeviceDescriptor(
        val type: Int = 0,
        val productName: String? = null,
        val alias: String? = null,
        val bluetoothDeviceClass: Int? = null,
        val isSink: Boolean = true,
        val address: String? = null,
    ) {
        companion object {
            const val TYPE_BUILTIN_SPEAKER = 2
            const val TYPE_WIRED_HEADSET = 3
            const val TYPE_WIRED_HEADPHONES = 4
            const val TYPE_LINE_ANALOG = 5
            const val TYPE_LINE_DIGITAL = 6
            const val TYPE_BLUETOOTH_SCO = 7
            const val TYPE_BLUETOOTH_A2DP = 8
            const val TYPE_HDMI = 9
            const val TYPE_HDMI_ARC = 10
            const val TYPE_USB_DEVICE = 11
            const val TYPE_USB_ACCESSORY = 12
            const val TYPE_AUX_LINE = 19
            const val TYPE_USB_HEADSET = 22
            const val TYPE_HEARING_AID = 23
            const val TYPE_BUILTIN_SPEAKER_SAFE = 24
            const val TYPE_BLE_HEADSET = 26
            const val TYPE_BLE_SPEAKER = 27
            const val TYPE_BLE_BROADCAST = 28

            const val BT_CLASS_WEARABLE_HEADSET = 0x0404
            const val BT_CLASS_SET_TOP_BOX = 0x040C
            const val BT_CLASS_LOUDSPEAKER = 0x0414
            const val BT_CLASS_HEADPHONES = 0x0418
            const val BT_CLASS_PORTABLE_AUDIO = 0x041C
            const val BT_CLASS_CAR_AUDIO = 0x0420
            const val BT_CLASS_HIFI_AUDIO = 0x0428
        }
    }

    enum class SimulationPreset(
        val deviceName: String,
        val expectedCategory: AudioDeviceCategory,
        val subtitle: String,
        val descriptor: AudioDeviceDescriptor,
    ) {
        AIRPODS_PRO(
            deviceName = "AirPods Pro",
            expectedCategory = AudioDeviceCategory.EARBUDS,
            subtitle = "Wireless Earbuds",
            descriptor = AudioDeviceDescriptor(
                type = AudioDeviceDescriptor.TYPE_BLUETOOTH_A2DP,
                productName = "AirPods Pro",
                bluetoothDeviceClass = AudioDeviceDescriptor.BT_CLASS_WEARABLE_HEADSET,
            ),
        ),
        SONY_WH1000XM5(
            deviceName = "Sony WH-1000XM5",
            expectedCategory = AudioDeviceCategory.HEADPHONES,
            subtitle = "Bluetooth Headphones",
            descriptor = AudioDeviceDescriptor(
                type = AudioDeviceDescriptor.TYPE_BLUETOOTH_A2DP,
                productName = "WH-1000XM5",
                bluetoothDeviceClass = AudioDeviceDescriptor.BT_CLASS_HEADPHONES,
            ),
        ),
        BOAT_ROCKERZ_255(
            deviceName = "boAt Rockerz 255 Pro+",
            expectedCategory = AudioDeviceCategory.NECKBAND,
            subtitle = "Wireless Neckband",
            descriptor = AudioDeviceDescriptor(
                type = AudioDeviceDescriptor.TYPE_BLUETOOTH_A2DP,
                productName = "boAt Rockerz 255 Pro+",
                bluetoothDeviceClass = AudioDeviceDescriptor.BT_CLASS_WEARABLE_HEADSET,
            ),
        ),
        JBL_FLIP_6(
            deviceName = "JBL Flip 6",
            expectedCategory = AudioDeviceCategory.BLUETOOTH_SPEAKER,
            subtitle = "Bluetooth Speaker",
            descriptor = AudioDeviceDescriptor(
                type = AudioDeviceDescriptor.TYPE_BLUETOOTH_A2DP,
                productName = "JBL Flip 6",
                bluetoothDeviceClass = AudioDeviceDescriptor.BT_CLASS_LOUDSPEAKER,
            ),
        ),
        ANDROID_AUTO(
            deviceName = "Android Auto",
            expectedCategory = AudioDeviceCategory.CAR_AUDIO,
            subtitle = "Car Audio System",
            descriptor = AudioDeviceDescriptor(
                type = AudioDeviceDescriptor.TYPE_BLUETOOTH_A2DP,
                productName = "Android Auto",
                bluetoothDeviceClass = AudioDeviceDescriptor.BT_CLASS_CAR_AUDIO,
            ),
        ),
        JABRA_TALK_45(
            deviceName = "Jabra Talk 45",
            expectedCategory = AudioDeviceCategory.SINGLE_EAR_HEADSET,
            subtitle = "Single-Ear Bluetooth Headset",
            descriptor = AudioDeviceDescriptor(
                type = AudioDeviceDescriptor.TYPE_BLUETOOTH_A2DP,
                productName = "Jabra Talk 45",
                bluetoothDeviceClass = AudioDeviceDescriptor.BT_CLASS_WEARABLE_HEADSET,
            ),
        ),
        USB_C_EARPHONES(
            deviceName = "USB-C Earphones",
            expectedCategory = AudioDeviceCategory.USB,
            subtitle = "USB Audio Device",
            descriptor = AudioDeviceDescriptor(
                type = AudioDeviceDescriptor.TYPE_USB_HEADSET,
                productName = "Type-C Digital Earphones",
            ),
        ),
        BUILT_IN_SPEAKER(
            deviceName = "Phone Speaker",
            expectedCategory = AudioDeviceCategory.PHONE,
            subtitle = "Phone Speaker",
            descriptor = AudioDeviceDescriptor(
                type = AudioDeviceDescriptor.TYPE_BUILTIN_SPEAKER,
                productName = "Phone Speaker",
            ),
        ),
        GENERIC_BLUETOOTH_HEADPHONES(
            deviceName = "Generic Bluetooth Headphones",
            expectedCategory = AudioDeviceCategory.HEADPHONES,
            subtitle = "Unknown/Generic Bluetooth Audio",
            descriptor = AudioDeviceDescriptor(
                type = AudioDeviceDescriptor.TYPE_BLUETOOTH_A2DP,
                productName = "Bluetooth Audio",
                bluetoothDeviceClass = AudioDeviceDescriptor.BT_CLASS_HEADPHONES,
            ),
        ),
    }

    data class SimulationState(
        val isSimulated: Boolean = false,
        val preset: SimulationPreset? = null,
    )

    val simulationState = MutableStateFlow(SimulationState())
    private var realHardwareSnapshot: Snapshot? = null
    private var realHardwareRoutes: List<OutputRoute>? = null

    fun activateSimulation(preset: SimulationPreset) {
        if (!simulationState.value.isSimulated) {
            realHardwareSnapshot = current.value
            realHardwareRoutes = availableRoutes.value
        }
        val cat = classifyDescriptor(preset.descriptor)
        simulationState.value = SimulationState(isSimulated = true, preset = preset)

        current.value = current.value.copy(
            deviceName = preset.deviceName,
            deviceCategory = cat,
            isBluetooth = cat in setOf(
                AudioDeviceCategory.EARBUDS,
                AudioDeviceCategory.HEADPHONES,
                AudioDeviceCategory.NECKBAND,
                AudioDeviceCategory.SINGLE_EAR_HEADSET,
                AudioDeviceCategory.BLUETOOTH_SPEAKER,
                AudioDeviceCategory.CAR_AUDIO,
            ),
            isWired = cat == AudioDeviceCategory.WIRED,
            isUsb = cat == AudioDeviceCategory.USB,
            isSpeaker = cat == AudioDeviceCategory.PHONE,
        )

        val simRoute = OutputRoute(
            id = "simulated_${preset.name.lowercase()}",
            name = preset.deviceName,
            isSelected = true,
            isConnected = true,
            isBluetooth = cat in setOf(
                AudioDeviceCategory.EARBUDS,
                AudioDeviceCategory.HEADPHONES,
                AudioDeviceCategory.NECKBAND,
                AudioDeviceCategory.SINGLE_EAR_HEADSET,
                AudioDeviceCategory.BLUETOOTH_SPEAKER,
                AudioDeviceCategory.CAR_AUDIO,
            ),
            isWired = cat == AudioDeviceCategory.WIRED,
            isUsb = cat == AudioDeviceCategory.USB,
            isSpeaker = cat == AudioDeviceCategory.PHONE,
            deviceCategory = cat,
        )
        selectedRouteId.value = simRoute.id
        confirmedActiveRouteId.value = simRoute.id

        val routes = mutableListOf(simRoute)
        realHardwareRoutes?.forEach { realRoute ->
            routes.add(realRoute.copy(isSelected = false))
        }
        availableRoutes.value = routes
    }

    fun clearSimulation(context: Context? = null) {
        if (!simulationState.value.isSimulated) return
        simulationState.value = SimulationState(isSimulated = false, preset = null)
        realHardwareSnapshot?.let { current.value = it }
        realHardwareRoutes?.let { availableRoutes.value = it }
        realHardwareSnapshot = null
        realHardwareRoutes = null

        val effectiveCtx = context ?: appContext
        if (effectiveCtx != null) {
            val audioManager = effectiveCtx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager != null) {
                updateDevice(effectiveCtx, audioManager)
            }
        }
    }

    private var appDeviceCallback: AudioDeviceCallback? = null
    private var appContext: Context? = null

    var onRouteSelectedListener: ((String) -> Boolean)? = null

    /**
     * Initializes real-time hardware audio output monitoring on app startup.
     * Keeps `AudioOutputStatus.current` and `availableRoutes` synced immediately
     * without needing a song to start playing first.
     */
    fun initMonitoring(context: Context) {
        val appCtx = context.applicationContext
        this.appContext = appCtx
        val audioManager = appCtx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        updateDevice(appCtx, audioManager)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && appDeviceCallback == null) {
            val callback = object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                    super.onAudioDevicesAdded(addedDevices)
                    // If a new device is connected, clear manual speaker override so it switches automatically
                    userExplicitlySelectedRoute = false
                    val newlyAdded = addedDevices?.firstOrNull {
                        it.isSink && (isMediaBluetoothDevice(it) || isWiredDevice(it) || isUsbDevice(it))
                    }
                    if (newlyAdded != null) {
                        confirmedActiveDevice.value = newlyAdded
                    }
                    updateDevice(appCtx, audioManager)
                }

                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                    super.onAudioDevicesRemoved(removedDevices)
                    val active = confirmedActiveDevice.value
                    if (active != null && removedDevices?.any { it.id == active.id } == true) {
                        userExplicitlySelectedRoute = false
                        confirmedActiveDevice.value = null
                    }
                    updateDevice(appCtx, audioManager)
                }
            }
            appDeviceCallback = callback
            audioManager.registerAudioDeviceCallback(callback, android.os.Handler(android.os.Looper.getMainLooper()))
        }
    }

    fun isWiredType(type: Int): Boolean = type in setOf(
        AudioDeviceDescriptor.TYPE_WIRED_HEADSET,
        AudioDeviceDescriptor.TYPE_WIRED_HEADPHONES,
        AudioDeviceDescriptor.TYPE_LINE_ANALOG,
        AudioDeviceDescriptor.TYPE_LINE_DIGITAL,
        AudioDeviceDescriptor.TYPE_AUX_LINE,
    )

    fun isUsbType(type: Int): Boolean = type in setOf(
        AudioDeviceDescriptor.TYPE_USB_DEVICE,
        AudioDeviceDescriptor.TYPE_USB_HEADSET,
        AudioDeviceDescriptor.TYPE_USB_ACCESSORY,
    )

    fun isSpeakerType(type: Int): Boolean = type in setOf(
        AudioDeviceDescriptor.TYPE_BUILTIN_SPEAKER,
        AudioDeviceDescriptor.TYPE_BUILTIN_SPEAKER_SAFE,
    )

    fun classifyDescriptor(descriptor: AudioDeviceDescriptor): AudioDeviceCategory {
        // 1. Hardware sink type check (takes precedence when non-zero)
        if (descriptor.type != 0) {
            if (isSpeakerType(descriptor.type)) {
                return AudioDeviceCategory.PHONE
            }
            if (isWiredType(descriptor.type)) {
                return AudioDeviceCategory.WIRED
            }
            if (isUsbType(descriptor.type)) {
                return AudioDeviceCategory.USB
            }
            if (descriptor.type == AudioDeviceDescriptor.TYPE_BLE_SPEAKER) {
                return AudioDeviceCategory.BLUETOOTH_SPEAKER
            }
            if (descriptor.type == AudioDeviceDescriptor.TYPE_BLE_HEADSET ||
                descriptor.type == AudioDeviceDescriptor.TYPE_HEARING_AID
            ) {
                return AudioDeviceCategory.EARBUDS
            }
        }

        val nameToInspect = descriptor.alias?.takeIf { it.isNotBlank() && isValidExternalName(it) }
            ?: descriptor.productName?.takeIf { it.isNotBlank() && isValidExternalName(it) }
            ?: ""

        // User override takes highest priority
        if (nameToInspect.isNotBlank()) {
            AppSettings.getDeviceCategoryOverride(nameToInspect)?.let { overrideName ->
                AudioDeviceCategory.entries.firstOrNull { it.name.equals(overrideName, ignoreCase = true) }?.let {
                    return it
                }
            }
        }

        val lower = normalizeAudioName(nameToInspect)

        // 2. Hardware Bluetooth class check (if available)
        val btClass = descriptor.bluetoothDeviceClass
        if (btClass != null) {
            if (btClass == AudioDeviceDescriptor.BT_CLASS_CAR_AUDIO) {
                return AudioDeviceCategory.CAR_AUDIO
            }
            val isSpeakerClass = btClass in setOf(
                AudioDeviceDescriptor.BT_CLASS_LOUDSPEAKER,
                AudioDeviceDescriptor.BT_CLASS_PORTABLE_AUDIO,
                AudioDeviceDescriptor.BT_CLASS_SET_TOP_BOX,
                AudioDeviceDescriptor.BT_CLASS_HIFI_AUDIO,
            )
            if (isSpeakerClass && !isEarbudsName(lower) && !isHeadphonesName(lower) && !isNeckbandName(lower)) {
                return AudioDeviceCategory.BLUETOOTH_SPEAKER
            }
            if (btClass == AudioDeviceDescriptor.BT_CLASS_HEADPHONES && !isEarbudsName(lower) && !isNeckbandName(lower)) {
                return AudioDeviceCategory.HEADPHONES
            }
        }

        // 3. Name-based heuristics
        if (lower.isNotBlank()) {
            if (isWiredName(lower)) return AudioDeviceCategory.WIRED
            if (isUsbName(lower)) return AudioDeviceCategory.USB
            if (isSpeakerName(lower)) return AudioDeviceCategory.PHONE
            return classifyBluetoothName(lower)
        }

        // 4. Default fallback when no name is known
        if (descriptor.type == AudioDeviceDescriptor.TYPE_BLUETOOTH_A2DP ||
            descriptor.type == AudioDeviceDescriptor.BT_CLASS_WEARABLE_HEADSET ||
            descriptor.type == AudioDeviceDescriptor.BT_CLASS_HEADPHONES
        ) {
            return AudioDeviceCategory.HEADPHONES
        }

        return AudioDeviceCategory.PHONE
    }

    fun detectCategory(device: AudioDeviceInfo?, deviceName: String = "", context: Context? = null): AudioDeviceCategory {
        val effectiveContext = context ?: appContext
        if (device != null) {
            val rawName = device.productName?.toString()?.trim().orEmpty()
            var nameToInspect = if (rawName.isNotBlank() && isValidExternalName(rawName)) rawName else deviceName.trim()
            var btClass: Int? = null

            try {
                val btAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
                if (btAdapter != null && (effectiveContext == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                        androidx.core.content.ContextCompat.checkSelfPermission(effectiveContext, android.Manifest.permission.BLUETOOTH_CONNECT) == android.content.pm.PackageManager.PERMISSION_GRANTED)
                ) {
                    val bonded = btAdapter.bondedDevices
                    val matchedDev = bonded?.firstOrNull { bd ->
                        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && !device.address.isNullOrBlank() && bd.address.equals(device.address, ignoreCase = true)) ||
                            (!nameToInspect.isBlank() && bd.name?.trim().equals(nameToInspect, ignoreCase = true))
                    } ?: bonded?.firstOrNull { bonded.size == 1 }

                    if (matchedDev != null) {
                        btClass = matchedDev.bluetoothClass?.deviceClass
                        val alias = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) matchedDev.alias else null
                        val resolved = alias?.takeIf { it.isNotBlank() } ?: matchedDev.name
                        if (!resolved.isNullOrBlank() && isValidExternalName(resolved)) {
                            nameToInspect = resolved.trim()
                        }
                    }
                }
            } catch (_: Throwable) {}

            val desc = AudioDeviceDescriptor(
                type = device.type,
                productName = nameToInspect,
                bluetoothDeviceClass = btClass,
                isSink = device.isSink,
                address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) device.address else null,
            )
            return classifyDescriptor(desc)
        }

        // When device is null (e.g. simulated route, unit tests, or name-only resolution)
        return classifyDescriptor(AudioDeviceDescriptor(productName = deviceName))
    }

    fun normalizeAudioName(name: String): String {
        return try {
            java.text.Normalizer.normalize(name, java.text.Normalizer.Form.NFD)
                .replace(Regex("\\p{M}"), "")
                .lowercase()
                .trim()
        } catch (_: Throwable) {
            name.lowercase().trim()
        }
    }

    fun overrideDeviceCategory(deviceName: String, category: AudioDeviceCategory, context: Context) {
        AppSettings.setDeviceCategoryOverride(deviceName, category.name)
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (audioManager != null) {
            updateDevice(context, audioManager)
        }
    }

    private fun isSpeakerName(lower: String): Boolean {
        if (lower.contains("headphone") || lower.contains("earphone") || lower.contains("phone jack")) return false
        return lower.contains("phone speaker") || lower.contains("built-in") || lower.contains("internal speaker") || lower.contains("this phone") || lower == "phone" || lower == "speaker"
    }

    private fun isWiredName(lower: String): Boolean {
        return lower.contains("wired") || lower.contains("3.5mm") || lower.contains("aux") || lower.contains("headphone jack")
    }

    private fun isUsbName(lower: String): Boolean {
        return lower.contains("usb") || lower.contains("type-c") || lower.contains("dac")
    }

    fun classifyBluetoothName(lower: String): AudioDeviceCategory {
        val normalized = normalizeAudioName(lower)
        // 1. Car / Infotainment Audio
        if (isCarAudioName(normalized)) {
            return AudioDeviceCategory.CAR_AUDIO
        }

        // 2. Bluetooth Speakers & Soundbars
        if (isBluetoothSpeakerName(normalized)) {
            return AudioDeviceCategory.BLUETOOTH_SPEAKER
        }

        // 3. Single-Ear Calling Headset (mono hook / business earpiece)
        if (isSingleEarHeadsetName(normalized)) {
            return AudioDeviceCategory.SINGLE_EAR_HEADSET
        }

        // 4. Neckband (behind the neck collar wireless earphones)
        if (isNeckbandName(normalized)) {
            return AudioDeviceCategory.NECKBAND
        }

        // 5. Over-Ear / On-Ear Headphones & Headsets
        if (isHeadphonesName(normalized)) {
            return AudioDeviceCategory.HEADPHONES
        }

        // 6. Earbuds / In-Ear / TWS
        if (isEarbudsName(normalized)) {
            return AudioDeviceCategory.EARBUDS
        }

        // 7. Safe Default: If subtype cannot be reliably identified as earbuds, neckband, speaker, etc.,
        // default to HEADPHONES (over-ear / on-ear / generic Bluetooth headphones) rather than showing a misleading earbuds icon.
        return AudioDeviceCategory.HEADPHONES
    }

    private fun isCarAudioName(lower: String): Boolean {
        // Must not falsely match "audio" as "audi" or "scarlett"/"card" as "car"
        val withoutAudio = lower.replace("audio", "")

        if (lower.contains("car audio") || lower.contains("car bt") || lower.contains("car-bt") ||
            lower.contains("carkit") || lower.contains("car kit") || lower.contains("carplay") ||
            lower.contains("android auto") || lower.contains("infotainment") || lower.contains("uconnect") ||
            lower.contains("mycar") || lower.contains("head unit") || lower.contains("touchscreen") ||
            lower.contains("entune") || lower.contains("idrive") || lower.contains("r-link") ||
            lower.contains("mmi")
        ) return true

        if (Regex("""\bcar\b""").containsMatchIn(lower) && !lower.contains("card") && !lower.contains("scarlett")) {
            return true
        }

        if (Regex("""\b(android\s*auto|auto\s*bt)\b""").containsMatchIn(lower) ||
            (Regex("""\bauto\b""").containsMatchIn(lower) && !lower.contains("audio-technica") && !lower.contains("automatic"))
        ) return true

        if (Regex("""\bsync\b""").containsMatchIn(lower)) return true

        // Distinct car brand names
        if (Regex("""\b(bmw|mercedes|toyota|honda|hyundai|kia|ford|tesla|volvo|volkswagen|skoda|suzuki|maruti|mahindra|tata|nissan|renault|chevrolet|mazda|jeep|lexus|porsche)\b""").containsMatchIn(lower)) {
            return true
        }

        // Audi car brand (strictly standalone word, not part of "audio")
        if (Regex("""\baudi\b""").containsMatchIn(withoutAudio)) {
            return true
        }

        return false
    }

    private fun isBluetoothSpeakerName(lower: String): Boolean {
        if (isEarbudsName(lower) || isHeadphonesName(lower) || isNeckbandName(lower)) {
            return false
        }
        return lower.contains("speaker") || lower.contains("soundbar") || lower.contains("soundlink") ||
            lower.contains("partybox") || lower.contains("echo") || lower.contains("nest") ||
            lower.contains("homepod") || lower.contains("sonos") || lower.contains("pill") ||
            lower.contains("wonderboom") || lower.contains("megaboom") || lower.contains("hyperboom") ||
            lower.contains("boombox") || lower.contains("tribit") ||
            lower.contains("jbl flip") || lower.contains("jbl charge") || lower.contains("jbl go") ||
            lower.contains("jbl clip") || lower.contains("jbl xtreme") || lower.contains("jbl pulse") ||
            lower.contains("soundcore motion") || lower.contains("soundcore boost") ||
            lower.contains("soundcore flare") || lower.contains("soundcore mini") ||
            lower.contains("soundcore rave") || lower.contains("soundcore select") ||
            lower.contains("soundcore glow") || lower.contains("boat stone") ||
            lower.contains("boat blitz") || lower.contains("boat rugby") ||
            lower.contains("mivi roam") || lower.contains("mivi play") ||
            lower.contains("mivi octane") || lower.contains("bose revolve") ||
            lower.contains("bose soundlink") || lower.contains("marshall emberton") ||
            lower.contains("marshall stanmore") || lower.contains("marshall acton") ||
            lower.contains("marshall willen") || lower.contains("marshall middleton") ||
            lower.contains("srs-")
    }

    private fun isSingleEarHeadsetName(name: String): Boolean {
        val lower = name.lowercase()
        if (lower.contains("tune flex")) return false
        if (lower.contains("neckband") || lower.contains("collar")) return false

        return lower.contains("single") || lower.contains("mono") ||
            lower.contains("voyager") || lower.contains("plantronics") ||
            lower.contains("legend") || lower.contains("explorer") ||
            lower.contains("jabra talk") || lower.contains("jabra stealth") ||
            lower.contains("jabra classic") || lower.contains("jabra steel") ||
            lower.contains("jabra storm") || lower.contains("new bee") ||
            lower.contains("conambo") || lower.contains("glazata") ||
            lower.contains("blueparrott") || lower.contains("trucker") ||
            lower.contains("earpiece")
    }

    private fun isNeckbandName(name: String): Boolean {
        val lower = name.lowercase()
        if (lower.contains("tune flex")) return false
        if (isHeadphonesName(lower) && !lower.contains("neckband") && !lower.contains("collar")) return false

        // Explicit neckband keywords
        if (lower.contains("neckband") || lower.contains("collar") ||
            lower.contains("around-neck") || lower.contains("behind-neck") ||
            lower.contains("wireless neck") || lower.contains("bluetooth neckband")
        ) return true

        // Popular neckband models
        if (lower.contains("bullets wireless") || lower.contains("buds wireless") ||
            lower.contains("level u") || lower.contains("sonicbass") ||
            lower.contains("tangent") || lower.contains("curve") ||
            lower.contains("enco m") || lower.contains("wi-") || lower.contains("wi1000") ||
            lower.contains("wi-c") || lower.contains("beats flex")
        ) return true

        // boAt Rockerz neckband series (Rockerz 1xx, 2xx, 3xx, 8xx, 9xx, apex, trinity)
        if (lower.contains("rockerz 1") || lower.contains("rockerz 2") || lower.contains("rockerz 3") ||
            lower.contains("rockerz 8") || lower.contains("rockerz 9") || lower.contains("rockerz apex") ||
            lower.contains("rockerz trinity") || (lower.contains("rockerz") && !lower.contains("rockerz 4") && !lower.contains("rockerz 5") && !lower.contains("rockerz 6") && !lower.contains("rockerz 7"))
        ) return true

        if (Regex("""\bneck\b""").containsMatchIn(lower)) return true

        return false
    }

    private fun isHeadphonesName(name: String): Boolean {
        val lower = name.lowercase()
        if (lower.contains("airpods max")) return true
        if (lower.contains("bud") || lower.contains("buds")) return false
        if (lower.contains("single") || lower.contains("mono") || lower.contains("earpiece") ||
            lower.contains("trucker") || lower.contains("voyager") || lower.contains("jabra talk")
        ) return false

        // Explicit over-ear / on-ear keywords
        if (lower.contains("over-ear") || lower.contains("over ear") ||
            lower.contains("on-ear") || lower.contains("on ear") ||
            lower.contains("around-ear") || lower.contains("around ear") ||
            lower.contains("headphone") || lower.contains("headphones") ||
            lower.contains("headset")
        ) return true

        // Sony Over-Ear (WH series, MDR)
        if (lower.contains("wh-") || lower.contains("wh1000") || lower.contains("wh-1000") ||
            lower.contains("wh-ch") || lower.contains("mdr-") || lower.contains("mdr ")
        ) return true

        // Bose Over-Ear
        if (lower.contains("quietcomfort") || lower.contains("qc35") || lower.contains("qc45") ||
            lower.contains("qc ultra") || lower.contains("qc 35") || lower.contains("qc 45") ||
            lower.contains("bose 700") || lower.contains("nc700") || lower.contains("soundlink around") ||
            lower.contains("soundlink on-ear")
        ) return true

        // Sennheiser Over-Ear
        if ((lower.contains("hd 450") || lower.contains("hd 350") || lower.contains("hd 250") ||
             lower.contains("hd 4.50") || lower.contains("momentum 4") || lower.contains("momentum 3") ||
             lower.contains("accentum")) && !lower.contains("true wireless")
        ) return true

        // JBL Over-Ear (Tune 5xx/6xx/7xx, Live 5xx/6xx/7xx, Tour One, Quantum)
        if (lower.contains("tune 5") || lower.contains("tune 6") || lower.contains("tune 7") ||
            lower.contains("tune5") || lower.contains("tune6") || lower.contains("tune7") ||
            lower.contains("live 5") || lower.contains("live 6") || lower.contains("live 7") ||
            lower.contains("live5") || lower.contains("live6") || lower.contains("live7") ||
            lower.contains("tour one") || lower.contains("quantum") || lower.contains("everest") ||
            lower.contains("jbl 710") || lower.contains("jbl 720") || lower.contains("jbl 760") ||
            lower.contains("jbl 770") || lower.contains("jbl 510") || lower.contains("jbl 520") ||
            lower.contains("jbl 660")
        ) return true

        // Marshall Over-Ear
        if (lower.contains("major") || lower.contains("monitor") || lower.contains("mid bluetooth")) return true

        // Beats Over-Ear / On-Ear
        if (lower.contains("studio pro") || lower.contains("studio3") || lower.contains("studio 3") ||
            lower.contains("beats studio") || lower.contains("solo3") || lower.contains("solo 3") ||
            lower.contains("solo pro") || lower.contains("solo 4") || lower.contains("solo4") ||
            lower.contains("beats solo")
        ) return true

        // Skullcandy Over-Ear
        if (lower.contains("crusher") || lower.contains("hesh") || lower.contains("riff") ||
            lower.contains("cassette")
        ) return true

        // Anker Soundcore Over-Ear (Space One, Space Q45, Life Q series, Q20, Q30, Q35)
        if (lower.contains("space one") || lower.contains("space q") || lower.contains("life q") ||
            lower.contains("q10") || lower.contains("q20") || lower.contains("q30") ||
            lower.contains("q35") || lower.contains("q45")
        ) return true

        // boAt Over-Ear Headphones (Rockerz 4xx, 5xx, 6xx, 7xx, Nirvana 751)
        if (lower.contains("rockerz 4") || lower.contains("rockerz 5") || lower.contains("rockerz 6") ||
            lower.contains("rockerz 7") || lower.contains("nirvana 751") || lower.contains("boat 450") ||
            lower.contains("boat 550") || lower.contains("boat 518")
        ) return true

        // Audio-Technica Over-Ear
        if (lower.contains("ath-m") || lower.contains("ath-sr") || lower.contains("ath-anc") ||
            lower.contains("m50x") || lower.contains("m20x") || lower.contains("m40x")
        ) return true

        // Gaming Headsets
        if (lower.contains("gaming") || lower.contains("hyperx") || lower.contains("cloud ii") ||
            lower.contains("cloud 2") || lower.contains("cloud alpha") || lower.contains("arctis") ||
            lower.contains("kraken") || lower.contains("blackshark") || lower.contains("barracuda") ||
            lower.contains("corsair void") || lower.contains("corsair hs") || lower.contains("astro a")
        ) return true

        return false
    }

    private fun isEarbudsName(name: String): Boolean {
        val lower = normalizeAudioName(name)
        if (lower.contains("airpods max")) return false
        if (lower.contains("homepod")) return false
        if (lower.contains("echo") && !lower.contains("buds") && !lower.contains("earbud")) return false

        // 1. Universal earbud keywords
        if (lower.contains("bud") || lower.contains("buds") ||
            lower.contains("earbud") || lower.contains("earbuds") ||
            lower.contains("airpod") || lower.contains("airpods") ||
            lower.contains("earpod") || lower.contains("earpods") ||
            lower.contains("tws") || lower.contains("true wireless") || lower.contains("truewireless") ||
            lower.contains("in-ear") || lower.contains("in ear") ||
            lower.contains("earphone") || lower.contains("earphones") ||
            (lower.contains("dot") && !lower.contains("echo dot")) || lower.contains("dots") || lower.contains("airdot") || lower.contains("airdots") ||
            lower.contains("freebuds") || lower.contains("galaxy buds") || lower.contains("pixel buds") ||
            (lower.contains("wireless stereo") && !lower.contains("headset")) || lower.contains("wireless earbuds") || lower.contains("wireless in-ear")
        ) return true

        // Universal "pod" / "pods" (catches DuoPods, Fire Pods, Pure Pods, OpenPods, FreePods, Air Pods, etc.)
        if (lower.contains("pod") || lower.contains("pods")) return true

        // 2. Brand & popular model patterns
        // Nothing Ear series
        if (lower.contains("nothing ear") || lower.contains("ear (") || lower.contains("ear(") ||
            lower.contains("ear 1") || lower.contains("ear 2") || lower.contains("ear a")
        ) return true

        // boAt Earbuds (Airdopes, Nirvana Ion, Immortal, boAt 131, boAt 141, etc.)
        if (lower.contains("airdopes") || lower.contains("nirvana ion") || lower.contains("nirvana space") ||
            lower.contains("immortal") || lower.contains("boat 1") || lower.contains("boat 4") ||
            lower.contains("boat tws") || lower.contains("boat airdopes")
        ) return true

        // Oppo / Realme / OnePlus
        if (lower.contains("enco") && !lower.contains("enco m")) return true
        if (lower.contains("nord buds") || lower.contains("realme buds") || lower.contains("techlife")) return true

        // Sony WF- (Wireless Free = Earbuds) & LinkBuds
        if (lower.contains("wf-") || lower.contains("wf1000") || lower.contains("wf-1000") ||
            lower.contains("wf-c") || lower.contains("linkbuds") || lower.contains("inzone buds")
        ) return true

        // JBL Earbuds
        if (lower.contains("wave") || lower.contains("tune beam") || lower.contains("tune flex") ||
            lower.contains("tune buds") || lower.contains("vibe beam") || lower.contains("vibe buds") ||
            lower.contains("live pro") || lower.contains("tour pro") || lower.contains("reflect flow") ||
            lower.contains("endurance peak") || lower.contains("endurance race")
        ) return true

        // Anker Soundcore Earbuds
        if (lower.contains("liberty") || lower.contains("life dot") || lower.contains("life note") ||
            lower.contains("life p") || lower.contains("space a40") || lower.contains("vr p10") ||
            lower.contains("sport x10") || lower.contains("aerofit") || lower.contains("r50i") ||
            lower.contains("a20i") || lower.contains("p20i") || lower.contains("p30i") ||
            lower.contains("soundcore r") || lower.contains("soundcore a") || lower.contains("soundcore p")
        ) return true

        // Noise Earbuds
        if (lower.contains("shots") || lower.contains("noise buds") || lower.contains("aura") ||
            lower.contains("trance") || lower.contains("vs10") || lower.contains("vs20") ||
            lower.contains("vs30") || lower.contains("vs40") || lower.contains("noise beads")
        ) return true

        // Boult Earbuds
        if (lower.contains("airbass") || lower.contains("propods") || lower.contains("boult z") ||
            lower.contains("boult w") || lower.contains("boult y") || lower.contains("clarity") ||
            lower.contains("maverick") || lower.contains("ampere")
        ) return true

        // Mivi / Portronics / pTron / Truke / Wings / Jabra
        if (lower.contains("duopods") || lower.contains("commando") || lower.contains("harmonics") ||
            lower.contains("twins") || lower.contains("bassbuds") || lower.contains("playbuds") ||
            lower.contains("truke") || lower.contains("mega") || lower.contains("phantom") || lower.contains("fethr") ||
            lower.contains("elite 3") || lower.contains("elite 4") || lower.contains("elite 5") ||
            lower.contains("elite 7") || lower.contains("elite 8") || lower.contains("elite 65t") ||
            lower.contains("elite 75t") || lower.contains("elite 85t") || lower.contains("elite active")
        ) return true

        // Beats Earbuds
        if (lower.contains("studio buds") || lower.contains("fit pro") || lower.contains("powerbeats pro")) return true

        // Generic TWS models
        if (lower.contains("i12") || lower.contains("i10") || lower.contains("i9s") ||
            lower.contains("f9") || lower.contains("m10") || lower.contains("tws-") ||
            lower.contains("pro 4") || lower.contains("pro 5") || lower.contains("pro 6") ||
            lower.contains("air3") || lower.contains("air4") || lower.contains("air5")
        ) return true

        return false
    }

    fun getBluetoothDeviceName(device: AudioDeviceInfo, context: Context? = null): String? {
        val effectiveContext = context ?: appContext
        try {
            val btAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
            if (btAdapter != null && (effectiveContext == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                    androidx.core.content.ContextCompat.checkSelfPermission(effectiveContext, android.Manifest.permission.BLUETOOTH_CONNECT) == android.content.pm.PackageManager.PERMISSION_GRANTED)
            ) {
                val bonded = btAdapter.bondedDevices
                val matchedDev = bonded?.firstOrNull { bd ->
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && !device.address.isNullOrBlank() && bd.address.equals(device.address, ignoreCase = true)) ||
                        (!device.productName.isNullOrBlank() && bd.name?.trim().equals(device.productName.toString().trim(), ignoreCase = true))
                } ?: bonded?.firstOrNull { bonded.size == 1 }

                if (matchedDev != null) {
                    val alias = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) matchedDev.alias else null
                    val resolved = alias?.takeIf { it.isNotBlank() } ?: matchedDev.name
                    if (!resolved.isNullOrBlank() && isValidExternalName(resolved)) {
                        return resolved.trim()
                    }
                }
            }
        } catch (_: Throwable) {}
        return null
    }

    fun isMediaBluetoothDevice(dev: AudioDeviceInfo): Boolean {
        if (!dev.isSink) return false
        return dev.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && dev.type == AudioDeviceInfo.TYPE_HEARING_AID) ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && (
                dev.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                dev.type == AudioDeviceInfo.TYPE_BLE_SPEAKER ||
                dev.type == AudioDeviceInfo.TYPE_BLE_BROADCAST
            ))
    }

    fun isWiredDevice(dev: AudioDeviceInfo): Boolean {
        if (!dev.isSink) return false
        return isWiredType(dev.type)
    }

    fun isUsbDevice(dev: AudioDeviceInfo): Boolean {
        if (!dev.isSink) return false
        return isUsbType(dev.type)
    }

    fun isSpeakerDevice(dev: AudioDeviceInfo): Boolean {
        if (!dev.isSink) return false
        return isSpeakerType(dev.type)
    }

    fun findBuiltInSpeaker(devices: Iterable<AudioDeviceInfo>): AudioDeviceInfo? {
        return devices.firstOrNull { it.isSink && it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
            ?: devices.firstOrNull { it.isSink && it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE }
            ?: devices.firstOrNull { it.isSink && !isMediaBluetoothDevice(it) && !isWiredDevice(it) && !isUsbDevice(it) && it.type != AudioDeviceInfo.TYPE_BUILTIN_EARPIECE }
    }

    fun selectRoute(routeId: String): Boolean {
        Log.d("MediaOutput", "User requested route: $routeId")
        errorMessage.value = null
        userExplicitlySelectedRoute = true
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
        if (this.appContext == null) {
            this.appContext = context.applicationContext
        }
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

        val resolvedCategory = detectCategory(device, resolvedName, context)

        current.value = current.value.copy(
            deviceName = resolvedName,
            isBluetooth = isBt,
            isWired = isWired,
            isUsb = isUsb,
            isSpeaker = isSpeaker,
            deviceCategory = resolvedCategory,
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
        if (this.appContext == null) {
            this.appContext = context.applicationContext
        }
        val devices = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try { manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList() } catch (_: Throwable) { emptyList() }
        } else emptyList()

        val effectiveActiveDev = if (userExplicitlySelectedRoute) {
            confirmedActiveDevice.value?.takeIf { confirmed ->
                devices.any { it.id == confirmed.id }
            } ?: resolveActiveDevice(manager)
        } else {
            // When user hasn't explicitly locked onto a route, prefer any currently active non-speaker device
            // or newly plugged in hardware; never stay stuck on phone speaker when external sinks exist.
            confirmedActiveDevice.value?.takeIf { confirmed ->
                !isSpeakerDevice(confirmed) && devices.any { it.id == confirmed.id }
            } ?: resolveActiveDevice(manager)
        }

        if (!userExplicitlySelectedRoute) {
            confirmedActiveDevice.value = effectiveActiveDev
        }

        val resolvedName = resolveDeviceDisplayName(context, effectiveActiveDev)
        val isUsb = effectiveActiveDev != null && isUsbDevice(effectiveActiveDev)
        val isBt = effectiveActiveDev != null && isMediaBluetoothDevice(effectiveActiveDev)
        val isWired = effectiveActiveDev != null && isWiredDevice(effectiveActiveDev)
        val isSpeaker = effectiveActiveDev == null || isSpeakerDevice(effectiveActiveDev)
        val activeCategory = detectCategory(effectiveActiveDev, resolvedName, context)

        current.value = current.value.copy(
            deviceName = resolvedName,
            sampleRatesHz = effectiveActiveDev?.sampleRates ?: IntArray(0),
            encodings = effectiveActiveDev?.encodings ?: IntArray(0),
            isUsb = isUsb,
            isBluetooth = isBt,
            isWired = isWired,
            isSpeaker = isSpeaker,
            deviceCategory = activeCategory,
        )

        // Build list of available output routes
        val routes = mutableListOf<OutputRoute>()
        val phoneName = getPhoneName(context)

        // 1. Phone Speaker
        val speakerDevice = findBuiltInSpeaker(devices)
        val isSpeakerSelected = isSpeaker
        routes.add(
            OutputRoute(
                id = "speaker",
                name = phoneName,
                isSelected = isSpeakerSelected,
                isConnected = true,
                isSpeaker = true,
                deviceCategory = AudioDeviceCategory.PHONE,
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
            val isBtConfirmed = effectiveActiveDev != null && effectiveActiveDev.id == btDev.id
            val rawName = btDev.productName?.toString()?.trim().orEmpty()
            val btName = if (isValidExternalName(rawName)) rawName else (getBluetoothDeviceName(btDev, context) ?: "Bluetooth Audio")
            val category = detectCategory(btDev, btName, context)
            routes.add(
                OutputRoute(
                    id = routeId,
                    name = btName,
                    isSelected = isBtConfirmed,
                    isConnected = true,
                    isBluetooth = true,
                    deviceCategory = category,
                    device = btDev,
                )
            )
        }

        // 3. USB Audio Devices (DACs, USB Headsets)
        val usbDevices = devices.filter { dev -> isUsbDevice(dev) }
        usbDevices.forEach { usbDev ->
            val routeId = if (usbDevices.size == 1) "usb" else "usb_${usbDev.id}"
            val isUsbConfirmed = effectiveActiveDev != null && effectiveActiveDev.id == usbDev.id
            val rawName = usbDev.productName?.toString()?.trim().orEmpty()
            val usbName = if (isValidExternalName(rawName)) rawName else "USB Audio Device"
            routes.add(
                OutputRoute(
                    id = routeId,
                    name = usbName,
                    isSelected = isUsbConfirmed,
                    isConnected = true,
                    isUsb = true,
                    deviceCategory = AudioDeviceCategory.USB,
                    device = usbDev,
                )
            )
        }

        // 4. Wired Headphones / Line out
        val wiredDevice = devices.firstOrNull { isWiredDevice(it) }
        if (wiredDevice != null) {
            val isWiredConfirmed = effectiveActiveDev != null && isWiredDevice(effectiveActiveDev)
            routes.add(
                OutputRoute(
                    id = "wired",
                    name = "Wired Headphones",
                    isSelected = isWiredConfirmed,
                    isConnected = true,
                    isWired = true,
                    deviceCategory = AudioDeviceCategory.WIRED,
                    device = wiredDevice,
                )
            )
        }

        val activeRoute = routes.firstOrNull { it.isSelected }?.id ?: "speaker"
        selectedRouteId.value = activeRoute
        confirmedActiveRouteId.value = activeRoute
        availableRoutes.value = routes
        Log.d("MediaOutput", "Available routes updated: ${routes.map { "${it.name} (id=${it.id}, selected=${it.isSelected}, cat=${it.deviceCategory})" }}")
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
        val category = detectCategory(activeDev, resolvedName, context)

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
            isBluetooth = activeDev != null && isMediaBluetoothDevice(activeDev),
            isWired = activeDev != null && isWiredDevice(activeDev),
            isSpeaker = activeDev == null || isSpeakerDevice(activeDev),
            deviceCategory = category,
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

        if (userExplicitlySelectedRoute) {
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
                if (isValidExternalName(rawName)) rawName else (getBluetoothDeviceName(device, context) ?: "Bluetooth Audio")
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
        val model = runCatching { Build.MODEL?.trim().orEmpty() }.getOrDefault("")
        val manufacturer = runCatching { Build.MANUFACTURER?.replaceFirstChar { it.uppercase() }.orEmpty() }.getOrDefault("")
        if (model.isNotBlank() && !isInternalModelCode(model)) {
            return if (model.startsWith(manufacturer, ignoreCase = true)) model else "$manufacturer $model".trim()
        }

        // 5. Fallback
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
        val buildModel = runCatching { Build.MODEL }.getOrNull()
        if (!buildModel.isNullOrBlank() && s.equals(buildModel, ignoreCase = true)) return false
        if (isInternalModelCode(s)) return false
        return true
    }

    fun publishDecoder(decoderName: String?) {
        current.value = current.value.copy(decoderName = decoderName)
    }

    fun publishAudioTrack(encoding: Int, sampleRateHz: Int, bufferSize: Int? = null) {
        val validRate = sampleRateHz.takeIf { it > 0 }
            ?: current.value.actualSampleRateHz?.takeIf { it > 0 }
            ?: 44100
        val validEncoding = encoding.takeIf { it > 0 }
            ?: current.value.actualEncoding?.takeIf { it > 0 }
            ?: AudioFormat.ENCODING_PCM_16BIT
        current.value = current.value.copy(
            actualEncoding = validEncoding,
            actualSampleRateHz = validRate,
            bufferSize = bufferSize ?: current.value.bufferSize,
            floatFallback = current.value.requestedPcmMode == OutputPcmMode.FLOAT_32 &&
                validEncoding != AudioFormat.ENCODING_PCM_FLOAT,
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
        val brand = runCatching { Build.BRAND?.lowercase().orEmpty() }.getOrDefault("")
        val manufacturer = runCatching { Build.MANUFACTURER?.lowercase().orEmpty() }.getOrDefault("")
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
