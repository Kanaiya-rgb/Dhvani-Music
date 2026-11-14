package com.music.dhvani.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioOutputDeviceClassificationTest {

    @Test
    fun testEarbudsClassification() {
        val earbuds = listOf(
            // Apple
            "AirPods Pro",
            "Apple AirPods",
            "AirPods (3rd generation)",
            "AirPods 2",
            // Samsung
            "Galaxy Buds2 Pro",
            "Galaxy Buds Live",
            "Galaxy Buds FE",
            "Galaxy Buds Pro",
            // Google
            "Pixel Buds Pro",
            "Pixel Buds A-Series",
            // boAt
            "boAt Airdopes 141",
            "boAt Airdopes 131",
            "boAt Airdopes 441",
            "boAt Airdopes 161",
            "boAt Nirvana Ion",
            "boAt Nirvana Space",
            "boAt Immortal 121",
            // Nothing
            "Nothing Ear (2)",
            "Nothing Ear (a)",
            "Nothing Ear (1)",
            "CMF Buds Pro",
            // Sony WF (Wireless Free = Earbuds)
            "Sony WF-1000XM5",
            "Sony WF-1000XM4",
            "Sony WF-C700N",
            "Sony WF-C500",
            "Sony LinkBuds S",
            "Sony LinkBuds Open",
            // JBL
            "JBL Tune Flex",
            "JBL Tune Beam",
            "JBL Wave 200",
            "JBL Wave Beam",
            "JBL Live Pro 2",
            "JBL Tour Pro 2",
            // Anker Soundcore
            "Soundcore Liberty 4 NC",
            "Soundcore Life P2i",
            "Soundcore R50i",
            "Soundcore A20i",
            "Soundcore Space A40",
            // OnePlus & Oppo
            "OnePlus Nord Buds 2",
            "OnePlus Nord Buds 2r",
            "Oppo Enco Air3 Pro",
            "Oppo Enco Buds 2",
            // Realme
            "Realme Buds T300",
            "Realme Buds Air 5 Pro",
            // Noise & Boult
            "Noise Buds VS102",
            "Noise Shots Grooves",
            "Noise Aura Buds",
            "Boult Audio AirBass",
            "Boult Z40",
            "Boult W20",
            // Mivi, PTron, Truke
            "Mivi DuoPods A25",
            "pTron Bassbuds Duo",
            "Truke Clarity 5",
            "trüke Mega 7",
            "trüke Buds Pro",
            "trüke Air Buds",
            "trüke BT",
            // Jabra & Beats
            "Jabra Elite 7 Pro",
            "Jabra Elite 4 Active",
            "Jabra Elite 85t",
            "Beats Studio Buds",
            "Beats Studio Buds +",
            "Beats Fit Pro",
            "Powerbeats Pro",
            // Generic TWS
            "TWS Earbuds i12",
            "F9 TWS Wireless Earphones",
            "Pro 4 True Wireless",
            "Bluetooth In-Ear Buds",
        )

        for (name in earbuds) {
            val category = AudioOutputStatus.detectCategory(null, name)
            assertEquals("Expected EARBUDS for '$name'", AudioOutputStatus.AudioDeviceCategory.EARBUDS, category)
        }
    }

    @Test
    fun testHeadphonesClassification() {
        val headphones = listOf(
            // Sony Over-Ear (WH series, MDR)
            "Sony WH-1000XM4",
            "Sony WH-1000XM5",
            "Sony WH-CH520",
            "Sony WH-CH720N",
            "Sony MDR-XB950B1",
            "Sony MDR-1000X",
            // Bose Over-Ear
            "Bose QuietComfort 45",
            "Bose QC Ultra",
            "Bose QuietComfort 35 II",
            "Bose 700",
            "Bose NC700",
            "Bose SoundLink Around-Ear Wireless Headphones II",
            // Sennheiser Over-Ear
            "Sennheiser Momentum 4 Wireless",
            "Sennheiser HD 450BT",
            "Sennheiser Accentum Plus",
            // JBL Over-Ear
            "JBL Tune 760NC",
            "JBL Tune 770NC",
            "JBL Tune 510BT",
            "JBL Tune 520BT",
            "JBL Live 660NC",
            "JBL Live 770NC",
            "JBL Tour One M2",
            "JBL Quantum 800",
            // Apple & Beats Over-Ear
            "AirPods Max",
            "Beats Studio Pro",
            "Beats Studio3 Wireless",
            "Beats Solo 4",
            "Beats Solo3",
            // Marshall Over-Ear
            "Marshall Major IV",
            "Marshall Monitor II A.N.C.",
            // boAt Over-Ear
            "boAt Rockerz 450",
            "boAt Rockerz 550",
            "boAt Rockerz 650",
            "boAt Nirvana 751 ANC",
            // Audio-Technica Over-Ear
            "Audio-Technica ATH-M50xBT2",
            "ATH-M20xBT",
            // Soundcore Over-Ear
            "Soundcore Life Q30",
            "Soundcore Life Q35",
            "Soundcore Space One",
            "Soundcore Space Q45",
            // Skullcandy Over-Ear
            "Skullcandy Crusher Wireless",
            "Skullcandy Crusher ANC 2",
            "Skullcandy Hesh ANC",
            "Skullcandy Riff Wireless 2",
            // Gaming Headsets
            "HyperX Cloud II Wireless",
            "HyperX Cloud Alpha Wireless",
            "Logitech G733 Gaming Headset",
            "Razer BlackShark V2 Pro",
            "Razer Barracuda X",
            "SteelSeries Arctis Nova 7 Wireless",
            "Corsair HS80 RGB Wireless",
            // Generic Headphones
            "Bluetooth Headset",
            "Wireless Stereo Headset",
            "Over-Ear Bluetooth Headphones",
            "On-Ear Wireless Headset",
        )

        for (name in headphones) {
            val category = AudioOutputStatus.detectCategory(null, name)
            assertEquals("Expected HEADPHONES for '$name'", AudioOutputStatus.AudioDeviceCategory.HEADPHONES, category)
        }
    }

    @Test
    fun testNeckbandClassification() {
        val neckbands = listOf(
            // boAt Rockerz Neckband series
            "boAt Rockerz 255 Pro+",
            "boAt Rockerz 330 ANC",
            "boAt Rockerz 103 Pro",
            "boAt Rockerz 245v2",
            "boAt Rockerz Trinity",
            "boAt Rockerz Apex",
            "boAt Rockerz 205 Pro",
            // OnePlus & Realme Neckbands
            "OnePlus Bullets Wireless Z2",
            "OnePlus Bullets Wireless Z2 ANC",
            "Realme Buds Wireless 3",
            "Realme Buds Wireless 2 Neo",
            // Sony WI (Wireless In-neckband)
            "Sony WI-C310",
            "Sony WI-C200",
            "Sony WI-C100",
            "Sony WI-1000XM2",
            // Samsung & Beats
            "Samsung Level U",
            "Samsung Level U2",
            "Beats Flex",
            // Oppo, Boult, pTron
            "Oppo Enco M32",
            "Boult Audio Curve",
            "Boult Curve ANC",
            "pTron Tangent Duo",
            "pTron Tangent Beat",
            // Generic Neckband
            "Wireless Bluetooth Neckband",
            "Behind-Neck Earphones",
            "Collar Earphones",
            "Around-Neck Stereo Earset",
        )

        for (name in neckbands) {
            val category = AudioOutputStatus.detectCategory(null, name)
            assertEquals("Expected NECKBAND for '$name'", AudioOutputStatus.AudioDeviceCategory.NECKBAND, category)
        }
    }

    @Test
    fun testBluetoothSpeakerClassification() {
        val speakers = listOf(
            // JBL Portable
            "JBL Flip 6",
            "JBL Flip 5",
            "JBL Charge 5",
            "JBL Charge 4",
            "JBL Go 3",
            "JBL Go 4",
            "JBL Clip 4",
            "JBL Boombox 3",
            "JBL PartyBox 110",
            "JBL Pulse 5",
            // boAt Stone
            "boAt Stone 1200",
            "boAt Stone 350",
            "boAt Stone 650",
            "boAt Stone 180",
            // Marshall
            "Marshall Emberton II",
            "Marshall Stanmore III",
            "Marshall Willen",
            "Marshall Acton II",
            // Sony SRS
            "Sony SRS-XB13",
            "Sony SRS-XE200",
            "Sony SRS-XG300",
            // Bose SoundLink
            "Bose SoundLink Flex",
            "Bose SoundLink Revolve+",
            "Bose SoundLink Mini II",
            // Anker Soundcore
            "Soundcore Motion+",
            "Soundcore Motion Boom",
            "Soundcore Flare 2",
            "Soundcore Mini 3",
            // Smart Speakers & Soundbars
            "Amazon Echo Dot",
            "Amazon Echo Show",
            "Apple HomePod mini",
            "Living Room Soundbar",
            "Sonos Roam",
            "Tribit StormBox Micro 2",
            "Mivi Roam 2",
        )

        for (name in speakers) {
            val category = AudioOutputStatus.detectCategory(null, name)
            assertEquals("Expected BLUETOOTH_SPEAKER for '$name'", AudioOutputStatus.AudioDeviceCategory.BLUETOOTH_SPEAKER, category)
        }
    }

    @Test
    fun testCarAudioClassification() {
        val carAudios = listOf(
            "Android Auto",
            "Apple CarPlay",
            "Ford SYNC",
            "BMW Bluetooth",
            "Audi MMI",
            "Hyundai Creta BT",
            "Maruti Suzuki SmartPlay",
            "Tata Motors Connect",
            "Tesla Model 3",
            "Mercedes-Benz BT",
            "Toyota Touch 2",
            "Honda HFT",
            "Kia Motors",
            "Volkswagen BT",
            "Car Audio BT",
            "CarKit 580",
            "MyCar Audio",
            "Uconnect Jeep",
            "Bluetooth Car Kit",
            "Infotainment System",
        )

        for (name in carAudios) {
            val category = AudioOutputStatus.detectCategory(null, name)
            assertEquals("Expected CAR_AUDIO for '$name'", AudioOutputStatus.AudioDeviceCategory.CAR_AUDIO, category)
        }
    }

    @Test
    fun testSingleEarHeadsetClassification() {
        val headsets = listOf(
            "Plantronics Voyager 5200",
            "Plantronics Legend",
            "Plantronics Explorer 50",
            "Jabra Talk 45",
            "Jabra Talk 25",
            "Jabra Talk 65",
            "Jabra Stealth",
            "New Bee M50",
            "Conambo J18 Single Ear",
            "Glazata Earpiece EC200",
            "BlueParrott B450-XT",
            "Mono Bluetooth Calling Earpiece",
            "Trucker Driver Headset",
            "Single Ear Business Earpiece",
        )

        for (name in headsets) {
            val category = AudioOutputStatus.detectCategory(null, name)
            assertEquals("Expected SINGLE_EAR_HEADSET for '$name'", AudioOutputStatus.AudioDeviceCategory.SINGLE_EAR_HEADSET, category)
        }
    }

    @Test
    fun testWiredUsbAndBuiltinSpeaker() {
        assertEquals(AudioOutputStatus.AudioDeviceCategory.WIRED, AudioOutputStatus.detectCategory(null, "Wired Headphones"))
        assertEquals(AudioOutputStatus.AudioDeviceCategory.WIRED, AudioOutputStatus.detectCategory(null, "3.5mm Aux Audio"))
        assertEquals(AudioOutputStatus.AudioDeviceCategory.WIRED, AudioOutputStatus.detectCategory(null, "Headphone Jack"))
        assertEquals(AudioOutputStatus.AudioDeviceCategory.USB, AudioOutputStatus.detectCategory(null, "USB-C Audio Device"))
        assertEquals(AudioOutputStatus.AudioDeviceCategory.USB, AudioOutputStatus.detectCategory(null, "Type-C Digital Earphones"))
        assertEquals(AudioOutputStatus.AudioDeviceCategory.USB, AudioOutputStatus.detectCategory(null, "FiiO KA13 Type-C DAC"))
        assertEquals(AudioOutputStatus.AudioDeviceCategory.PHONE, AudioOutputStatus.detectCategory(null, "Phone Speaker"))
        assertEquals(AudioOutputStatus.AudioDeviceCategory.PHONE, AudioOutputStatus.detectCategory(null, "Built-in Speaker"))
    }

    @Test
    fun testGenericAndFallbackClassification() {
        // Uncategorized Bluetooth devices must safely fallback to HEADPHONES, never EARBUDS
        val genericBluetoothNames = listOf(
            "Bluetooth Audio",
            "Wireless Audio",
            "Media Audio",
            "My Device",
            "BT-Audio-01",
            "BT Audio Sink",
            "HD Audio Device",
            "Audio Receiver",
        )

        for (name in genericBluetoothNames) {
            val category = AudioOutputStatus.detectCategory(null, name)
            assertEquals(
                "Generic Bluetooth device '$name' must fallback to HEADPHONES (over-ear / generic icon), not EARBUDS",
                AudioOutputStatus.AudioDeviceCategory.HEADPHONES,
                category,
            )
        }

        // Empty/null device name fallback
        assertEquals(AudioOutputStatus.AudioDeviceCategory.PHONE, AudioOutputStatus.detectCategory(null, ""))
        assertEquals(AudioOutputStatus.AudioDeviceCategory.PHONE, AudioOutputStatus.detectCategory(null, "   "))

        // Bluetooth A2DP descriptor with unknown or empty name must fallback to HEADPHONES
        val a2dpDesc = AudioOutputStatus.AudioDeviceDescriptor(
            type = AudioOutputStatus.AudioDeviceDescriptor.TYPE_BLUETOOTH_A2DP,
            productName = "",
        )
        assertEquals(
            "Unknown A2DP device must fallback to HEADPHONES, not EARBUDS",
            AudioOutputStatus.AudioDeviceCategory.HEADPHONES,
            AudioOutputStatus.classifyDescriptor(a2dpDesc),
        )
    }

    @Test
    fun testCaseInsensitiveWhitespaceAndSpellingVariations() {
        assertEquals(AudioOutputStatus.AudioDeviceCategory.EARBUDS, AudioOutputStatus.detectCategory(null, "   aIrPoDs PrO   "))
        assertEquals(AudioOutputStatus.AudioDeviceCategory.HEADPHONES, AudioOutputStatus.detectCategory(null, "  sOnY wh-1000xm5  "))
        assertEquals(AudioOutputStatus.AudioDeviceCategory.BLUETOOTH_SPEAKER, AudioOutputStatus.detectCategory(null, "   JBL FLIP 6   "))
        assertEquals(AudioOutputStatus.AudioDeviceCategory.NECKBAND, AudioOutputStatus.detectCategory(null, "boat  rockerz  255 pro+"))
        assertEquals(AudioOutputStatus.AudioDeviceCategory.CAR_AUDIO, AudioOutputStatus.detectCategory(null, " aNdRoId   AuTo "))
    }

    @Test
    fun testSubstringCollisionsAndFalsePositives() {
        // "Focusrite Scarlett" contains "car", but is not car audio -> must be generic headphones
        val scarlettCategory = AudioOutputStatus.detectCategory(null, "Focusrite Scarlett 2i2")
        assertEquals(AudioOutputStatus.AudioDeviceCategory.HEADPHONES, scarlettCategory)

        // "Beats Studio Buds" contains "studio", but has "buds" -> MUST be EARBUDS
        assertEquals(AudioOutputStatus.AudioDeviceCategory.EARBUDS, AudioOutputStatus.detectCategory(null, "Beats Studio Buds"))

        // "AirPods Max" contains "airpods", but is "max" -> MUST be HEADPHONES
        assertEquals(AudioOutputStatus.AudioDeviceCategory.HEADPHONES, AudioOutputStatus.detectCategory(null, "AirPods Max"))

        // "Amazon Echo Dot" contains "dot", but is a speaker -> MUST be BLUETOOTH_SPEAKER
        assertEquals(AudioOutputStatus.AudioDeviceCategory.BLUETOOTH_SPEAKER, AudioOutputStatus.detectCategory(null, "Echo Dot 5th Gen"))

        // "JBL Tune Flex" contains "flex", but is TWS earbuds -> MUST be EARBUDS
        assertEquals(AudioOutputStatus.AudioDeviceCategory.EARBUDS, AudioOutputStatus.detectCategory(null, "JBL Tune Flex"))
    }

    @Test
    fun testDescriptorHardwarePriority() {
        // 1. Hardware sink type takes absolute precedence over deceptive names
        val wiredDesc = AudioOutputStatus.AudioDeviceDescriptor(
            type = AudioOutputStatus.AudioDeviceDescriptor.TYPE_WIRED_HEADSET,
            productName = "Bluetooth Car Audio Earbuds",
        )
        assertEquals(AudioOutputStatus.AudioDeviceCategory.WIRED, AudioOutputStatus.classifyDescriptor(wiredDesc))

        val usbDesc = AudioOutputStatus.AudioDeviceDescriptor(
            type = AudioOutputStatus.AudioDeviceDescriptor.TYPE_USB_HEADSET,
            productName = "AirPods Pro USB",
        )
        assertEquals(AudioOutputStatus.AudioDeviceCategory.USB, AudioOutputStatus.classifyDescriptor(usbDesc))

        val speakerDesc = AudioOutputStatus.AudioDeviceDescriptor(
            type = AudioOutputStatus.AudioDeviceDescriptor.TYPE_BUILTIN_SPEAKER,
            productName = "Speaker",
        )
        assertEquals(AudioOutputStatus.AudioDeviceCategory.PHONE, AudioOutputStatus.classifyDescriptor(speakerDesc))

        val bleSpeakerDesc = AudioOutputStatus.AudioDeviceDescriptor(
            type = AudioOutputStatus.AudioDeviceDescriptor.TYPE_BLE_SPEAKER,
            productName = "BLE Unit",
        )
        assertEquals(AudioOutputStatus.AudioDeviceCategory.BLUETOOTH_SPEAKER, AudioOutputStatus.classifyDescriptor(bleSpeakerDesc))

        val bleHeadsetDesc = AudioOutputStatus.AudioDeviceDescriptor(
            type = AudioOutputStatus.AudioDeviceDescriptor.TYPE_BLE_HEADSET,
            productName = "BLE Audio Sink",
        )
        assertEquals(AudioOutputStatus.AudioDeviceCategory.EARBUDS, AudioOutputStatus.classifyDescriptor(bleHeadsetDesc))

        // 2. Bluetooth class metadata
        val carBtClassDesc = AudioOutputStatus.AudioDeviceDescriptor(
            type = AudioOutputStatus.AudioDeviceDescriptor.TYPE_BLUETOOTH_A2DP,
            bluetoothDeviceClass = AudioOutputStatus.AudioDeviceDescriptor.BT_CLASS_CAR_AUDIO,
            productName = "Custom In-Vehicle Unit",
        )
        assertEquals(AudioOutputStatus.AudioDeviceCategory.CAR_AUDIO, AudioOutputStatus.classifyDescriptor(carBtClassDesc))

        val speakerBtClassDesc = AudioOutputStatus.AudioDeviceDescriptor(
            type = AudioOutputStatus.AudioDeviceDescriptor.TYPE_BLUETOOTH_A2DP,
            bluetoothDeviceClass = AudioOutputStatus.AudioDeviceDescriptor.BT_CLASS_LOUDSPEAKER,
            productName = "Outdoor Audio",
        )
        assertEquals(AudioOutputStatus.AudioDeviceCategory.BLUETOOTH_SPEAKER, AudioOutputStatus.classifyDescriptor(speakerBtClassDesc))

        val headphonesBtClassDesc = AudioOutputStatus.AudioDeviceDescriptor(
            type = AudioOutputStatus.AudioDeviceDescriptor.TYPE_BLUETOOTH_A2DP,
            bluetoothDeviceClass = AudioOutputStatus.AudioDeviceDescriptor.BT_CLASS_HEADPHONES,
            productName = "Studio Monitor",
        )
        assertEquals(AudioOutputStatus.AudioDeviceCategory.HEADPHONES, AudioOutputStatus.classifyDescriptor(headphonesBtClassDesc))
    }

    @Test
    fun testCentralizedIconAndSubtitleMapping() {
        // Verify all categories map to a non-null icon and appropriate subtitle
        for (category in AudioOutputStatus.AudioDeviceCategory.values()) {
            val icon = AudioOutputStatus.iconFor(category)
            assertNotNull("Icon must not be null for category $category", icon)

            val subtitle = AudioOutputStatus.subtitleFor(category)
            assertTrue("Subtitle must not be blank for category $category", subtitle.isNotBlank())
        }

        // Verify specific category subtitles
        assertEquals("Wireless Earbuds", AudioOutputStatus.subtitleFor(AudioOutputStatus.AudioDeviceCategory.EARBUDS))
        assertEquals("Bluetooth Headphones", AudioOutputStatus.subtitleFor(AudioOutputStatus.AudioDeviceCategory.HEADPHONES))
        assertEquals("Wireless Neckband", AudioOutputStatus.subtitleFor(AudioOutputStatus.AudioDeviceCategory.NECKBAND))
        assertEquals("Bluetooth Speaker", AudioOutputStatus.subtitleFor(AudioOutputStatus.AudioDeviceCategory.BLUETOOTH_SPEAKER))
        assertEquals("Car Audio System", AudioOutputStatus.subtitleFor(AudioOutputStatus.AudioDeviceCategory.CAR_AUDIO))
        assertEquals("Wired Audio Port", AudioOutputStatus.subtitleFor(AudioOutputStatus.AudioDeviceCategory.WIRED))
        assertEquals("USB Audio Device", AudioOutputStatus.subtitleFor(AudioOutputStatus.AudioDeviceCategory.USB))
        assertEquals("Phone Speaker", AudioOutputStatus.subtitleFor(AudioOutputStatus.AudioDeviceCategory.PHONE))
        assertEquals("Single-Ear Bluetooth Headset", AudioOutputStatus.subtitleFor(AudioOutputStatus.AudioDeviceCategory.SINGLE_EAR_HEADSET))

        // Verify custom vectors instantiate cleanly
        assertNotNull(com.music.dhvani.ui.icons.DhvaniIcons.Earbuds)
        assertNotNull(com.music.dhvani.ui.icons.DhvaniIcons.Headphone)
        assertNotNull(com.music.dhvani.ui.icons.DhvaniIcons.WiredEarphones)
        assertNotNull(com.music.dhvani.ui.icons.DhvaniIcons.BluetoothSpeaker)
        assertNotNull(com.music.dhvani.ui.icons.DhvaniIcons.CarAudio)
        assertNotNull(com.music.dhvani.ui.icons.DhvaniIcons.SingleEarHeadset)
        assertNotNull(com.music.dhvani.ui.icons.DhvaniIcons.Neckband)
    }

    @Test
    fun testSimulatorPresetsAndState() {
        // Verify all 9 presets are defined with accurate expectations
        val presets = AudioOutputStatus.SimulationPreset.values()
        assertEquals(9, presets.size)

        val presetMap = presets.associateBy { it.name }
        assertEquals(AudioOutputStatus.AudioDeviceCategory.EARBUDS, presetMap["AIRPODS_PRO"]?.expectedCategory)
        assertEquals(AudioOutputStatus.AudioDeviceCategory.HEADPHONES, presetMap["SONY_WH1000XM5"]?.expectedCategory)
        assertEquals(AudioOutputStatus.AudioDeviceCategory.NECKBAND, presetMap["BOAT_ROCKERZ_255"]?.expectedCategory)
        assertEquals(AudioOutputStatus.AudioDeviceCategory.BLUETOOTH_SPEAKER, presetMap["JBL_FLIP_6"]?.expectedCategory)
        assertEquals(AudioOutputStatus.AudioDeviceCategory.CAR_AUDIO, presetMap["ANDROID_AUTO"]?.expectedCategory)
        assertEquals(AudioOutputStatus.AudioDeviceCategory.SINGLE_EAR_HEADSET, presetMap["JABRA_TALK_45"]?.expectedCategory)
        assertEquals(AudioOutputStatus.AudioDeviceCategory.USB, presetMap["USB_C_EARPHONES"]?.expectedCategory)
        assertEquals(AudioOutputStatus.AudioDeviceCategory.PHONE, presetMap["BUILT_IN_SPEAKER"]?.expectedCategory)
        assertEquals(AudioOutputStatus.AudioDeviceCategory.HEADPHONES, presetMap["GENERIC_BLUETOOTH_HEADPHONES"]?.expectedCategory)

        // Test activation and clearing of simulation state
        assertFalse(AudioOutputStatus.simulationState.value.isSimulated)

        AudioOutputStatus.activateSimulation(AudioOutputStatus.SimulationPreset.SONY_WH1000XM5)
        assertTrue(AudioOutputStatus.simulationState.value.isSimulated)
        assertEquals(AudioOutputStatus.SimulationPreset.SONY_WH1000XM5, AudioOutputStatus.simulationState.value.preset)
        assertEquals("Sony WH-1000XM5", AudioOutputStatus.current.value.deviceName)
        assertEquals(AudioOutputStatus.AudioDeviceCategory.HEADPHONES, AudioOutputStatus.current.value.deviceCategory)

        AudioOutputStatus.clearSimulation()
        assertFalse(AudioOutputStatus.simulationState.value.isSimulated)
    }
}
