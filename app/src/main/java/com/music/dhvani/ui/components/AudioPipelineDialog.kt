package com.music.dhvani.ui.components



import android.media.AudioFormat

import androidx.compose.animation.core.LinearEasing

import androidx.compose.animation.core.RepeatMode

import androidx.compose.animation.core.animateFloat

import androidx.compose.animation.core.infiniteRepeatable

import androidx.compose.animation.core.rememberInfiniteTransition

import androidx.compose.animation.core.tween

import androidx.compose.foundation.BorderStroke

import androidx.compose.foundation.Canvas

import androidx.compose.foundation.background

import androidx.compose.foundation.border

import androidx.compose.foundation.clickable

import androidx.compose.foundation.gestures.awaitEachGesture

import androidx.compose.foundation.gestures.awaitFirstDown

import androidx.compose.foundation.interaction.MutableInteractionSource

import androidx.compose.foundation.layout.Arrangement

import androidx.compose.foundation.layout.Box

import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.IntrinsicSize

import androidx.compose.foundation.layout.Row

import androidx.compose.foundation.layout.Spacer

import androidx.compose.foundation.layout.fillMaxHeight

import androidx.compose.foundation.layout.fillMaxSize

import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.foundation.layout.height

import androidx.compose.foundation.layout.heightIn

import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.layout.size

import androidx.compose.foundation.layout.width

import androidx.compose.foundation.layout.widthIn

import androidx.compose.foundation.rememberScrollState

import androidx.compose.foundation.shape.CircleShape

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.foundation.verticalScroll

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.automirrored.rounded.VolumeUp

import androidx.compose.material.icons.rounded.Description

import androidx.compose.material.icons.rounded.GraphicEq

import androidx.compose.material.icons.rounded.Headphones

import androidx.compose.material.icons.rounded.Memory

import androidx.compose.material.icons.rounded.MusicNote

import androidx.compose.material.icons.rounded.PhoneAndroid

import androidx.compose.material.icons.rounded.Tune

import androidx.compose.material.icons.rounded.Usb

import androidx.compose.material3.HorizontalDivider

import androidx.compose.material3.Icon

import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.Surface

import androidx.compose.material3.Text

import androidx.compose.runtime.Composable

import androidx.compose.runtime.getValue

import androidx.compose.runtime.mutableStateOf

import androidx.compose.runtime.remember

import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind

import androidx.compose.ui.geometry.Offset

import androidx.compose.ui.geometry.Rect

import androidx.compose.ui.graphics.Brush

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.graphics.StrokeCap

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.graphics.ColorUtils
import com.music.dhvani.data.history.PlaybackHistory
import com.music.dhvani.ui.player.rememberArtworkColors

import androidx.compose.ui.input.nestedscroll.NestedScrollConnection

import androidx.compose.ui.input.nestedscroll.NestedScrollSource

import androidx.compose.ui.input.nestedscroll.nestedScroll

import androidx.compose.ui.input.pointer.pointerInput

import androidx.compose.ui.input.pointer.positionChange

import androidx.compose.ui.layout.LayoutCoordinates

import androidx.compose.ui.layout.boundsInRoot

import androidx.compose.ui.layout.onGloballyPositioned

import androidx.compose.ui.text.SpanStyle

import androidx.compose.ui.text.buildAnnotatedString

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.text.withStyle

import androidx.compose.ui.unit.Velocity

import androidx.compose.ui.unit.dp

import androidx.compose.ui.unit.sp

import androidx.compose.ui.window.Dialog

import androidx.compose.ui.window.DialogProperties

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.music.dhvani.data.NerdStats

import com.music.dhvani.data.settings.AppSettings

import com.music.dhvani.playback.AudioDecoderResolver

import com.music.dhvani.playback.AudioOutputStatus



private val PIPELINE_CARD_SHAPE = RoundedCornerShape(22.dp)

private val PIPELINE_SCRIM_COLOR = Color.Black.copy(alpha = 0.60f)



private fun formatSampleRate(hz: Int): String {
    val rate = hz.takeIf { it > 0 } ?: 44100
    return if (rate % 1000 == 0) {
        "${rate / 1000} kHz"
    } else {
        "%.1f kHz".format(rate / 1000f)
    }
}



/**

 * Hi-Res Audio Pipeline Inspector dialog matching reference UI.

 *

 * Visualizes the live signal path from decoder to output with an animated vertical timeline:

 * TRACK INFO -> DECODER -> RESAMPLER -> DSP -> OUTPUT DEVICE.

 */

@Composable

fun AudioPipelineDialog(
    onDismiss: () -> Unit,
    themeColors: List<Color> = emptyList(),
    accentColor: Color? = null,
    modifier: Modifier = Modifier,
) {

    Dialog(

        onDismissRequest = onDismiss,

        properties = DialogProperties(

            usePlatformDefaultWidth = false,

        ),

    ) {

        val nerdStats by NerdStats.current.collectAsStateWithLifecycle()

        val outputStatus by AudioOutputStatus.current.collectAsStateWithLifecycle()



        val eqEnabled by AppSettings.equalizerEnabled.collectAsStateWithLifecycle()

        val eqPreset by AppSettings.equalizerPreset.collectAsStateWithLifecycle()

        val spatialAudio by AppSettings.spatialAudio.collectAsStateWithLifecycle()



        // Animated glowing bead traveling continuously down the vertical pipeline

        
        // Extract the exact dominant cover background color that flows down from the artwork
        val artworkColor = remember(accentColor, themeColors) {
            val baseColor = accentColor?.takeIf { it != Color.Unspecified }
                ?: themeColors.firstOrNull()
                ?: Color(0xFF38BDF8)
            val hsl = FloatArray(3)
            ColorUtils.colorToHSL(baseColor.toArgb(), hsl)
            hsl[1] = hsl[1].coerceAtLeast(0.55f)
            hsl[2] = hsl[2].coerceIn(0.60f, 0.85f)
            Color(ColorUtils.HSLToColor(hsl))
        }

val infiniteTransition = rememberInfiniteTransition(label = "pipelineFlow")
        val flowProgressState = infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "flowBead",
        )
        // Zero-recomposition state reader lambda - skips composition & layout phases completely
        val progressProvider = remember { { flowProgressState.value } }



        val scrollState = rememberScrollState()

        val pipelineNestedScroll = remember(scrollState) {

            object : NestedScrollConnection {

                override fun onPostScroll(

                    consumed: Offset,

                    available: Offset,

                    source: NestedScrollSource,

                ): Offset = available



                override suspend fun onPreFling(available: Velocity): Velocity {

                    val trapY = (available.y > 0f && !scrollState.canScrollBackward) ||

                        (available.y < 0f && !scrollState.canScrollForward)

                    return if (trapY) available else Velocity.Zero

                }



                override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity = available

            }

        }



        var cardBoundsInRoot by remember { mutableStateOf(Rect.Zero) }

        var scrimCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }



        Box(

            modifier = modifier

                .fillMaxSize()

                .background(PIPELINE_SCRIM_COLOR)

                .onGloballyPositioned { scrimCoordinates = it }

                .pointerInput(onDismiss) {

                    awaitEachGesture {

                        val down = awaitFirstDown(requireUnconsumed = false)

                        val rootPos = scrimCoordinates?.localToRoot(down.position) ?: down.position

                        if (cardBoundsInRoot != Rect.Zero && cardBoundsInRoot.contains(rootPos)) {

                            return@awaitEachGesture

                        }

                        down.consume()

                        var isTap = true

                        val touchSlop = viewConfiguration.touchSlop

                        var totalMoved = 0f



                        while (true) {

                            val event = awaitPointerEvent()

                            val change = event.changes.firstOrNull { it.id == down.id } ?: break

                            val delta = change.positionChange()

                            totalMoved += delta.getDistance()

                            if (totalMoved > touchSlop) {

                                isTap = false

                            }

                            val isUp = !change.pressed && change.previousPressed

                            change.consume()



                            if (isUp) {

                                if (isTap) {

                                    onDismiss()

                                }

                                break

                            }

                        }

                    }

                },

            contentAlignment = Alignment.Center,

        ) {

            Surface(

                modifier = Modifier

                    .onGloballyPositioned { coordinates ->

                        cardBoundsInRoot = coordinates.boundsInRoot()

                    }

                    .widthIn(min = 310.dp, max = 350.dp)

                    .fillMaxWidth(0.88f)

                    .heightIn(max = 590.dp)

                    .clip(PIPELINE_CARD_SHAPE),

                shape = PIPELINE_CARD_SHAPE,

                color = Color(0xFF141416),

                border = BorderStroke(1.dp, artworkColor.copy(alpha = 0.22f)),

                shadowElevation = 24.dp,

            ) {

                Column(

                    modifier = Modifier

                        .fillMaxWidth()

                        .padding(horizontal = 22.dp, vertical = 20.dp),

                ) {

                    // Header: Centered "Audio Pipeline" & Subtitle

                    Column(

                        modifier = Modifier.fillMaxWidth(),

                        horizontalAlignment = Alignment.CenterHorizontally,

                    ) {

                        Text(

                            text = "Audio Pipeline",

                            style = MaterialTheme.typography.titleMedium.copy(

                                fontSize = 18.sp,

                                fontWeight = FontWeight.Bold,

                            ),

                            color = Color.White,

                        )

                        Spacer(Modifier.height(3.dp))

                        Text(

                            text = "Live signal path, decoder to output",

                            style = MaterialTheme.typography.bodySmall.copy(

                                fontSize = 12.5.sp,

                            ),

                            color = Color.White.copy(alpha = 0.60f),

                        )

                    }



                    Spacer(Modifier.height(14.dp))

                    HorizontalDivider(

                        color = Color.White.copy(alpha = 0.09f),

                        thickness = 0.8.dp,

                    )

                    Spacer(Modifier.height(14.dp))



                    // Scrollable Pipeline Timeline Stages List

                    Column(

                        modifier = Modifier

                            .weight(1f, fill = false)

                            .fillMaxWidth()

                            .nestedScroll(pipelineNestedScroll)

                            .verticalScroll(scrollState),

                    ) {

                        val stats = nerdStats

                        val format = NerdStats.codecLabel(stats?.mimeType)

                            ?: stats?.claimed?.let { NerdStats.codecLabel(it.codec) }

                            ?: if (stats?.isLossless == true) "FLAC" else "Opus"



                        val isFlac = format.contains("FLAC", ignoreCase = true) || stats?.mimeType?.contains("flac", ignoreCase = true) == true

                        val isOpus = !isFlac && format.contains("Opus", ignoreCase = true)

                        val isAac = !isFlac && (format.contains("AAC", ignoreCase = true) || format.contains("MP4", ignoreCase = true))



                        val rawSource = stats?.sourceName ?: stats?.claimed?.source

                        val sourceName = when {
                            !rawSource.isNullOrBlank() -> rawSource
                            isFlac || stats?.isLossless == true -> {
                                val depth = stats?.bitDepth ?: 16
                                val rate = stats?.sampleRateHz ?: 44100
                                val activeLossless = com.music.dhvani.data.sources.SourceRegistry.configs.value.firstOrNull { it.enabled && it.kind.canServeLossless }?.displayName ?: "Lossless FLAC"
                                if (depth > 16 || rate > 48000) "$activeLossless Hi-Res" else activeLossless
                            }
                            isAac || stats?.isHiQuality == true -> "High Quality AAC"
                            else -> "Standard Opus"
                        }

                        val isFlacOrLossless = !isOpus && !isAac && (
                            stats?.isLossless == true ||
                            isFlac ||
                            sourceName.contains("Hi-Res", ignoreCase = true) ||
                            sourceName.contains("Lossless", ignoreCase = true) ||
                            sourceName.contains("Qobuz", ignoreCase = true) ||
                            sourceName.contains("FLAC", ignoreCase = true)
                        )



                        val bitDepthInt = when {
                            isOpus || isAac -> 16
                            isFlacOrLossless -> stats?.bitDepth?.takeIf { it > 0 }
                                ?: stats?.claimed?.bitDepth?.takeIf { it > 0 }
                                ?: if (sourceName.contains("Hi-Res", ignoreCase = true)) 24
                                else 16
                            else -> stats?.bitDepth?.takeIf { it > 0 } ?: 16
                        }

                        val bitDepth = bitDepthInt?.let { "$it-bit" } ?: "—"

                        val sampleRateHz = when {
                            isOpus -> 48000
                            isAac -> stats?.sampleRateHz?.takeIf { it > 0 } ?: 44100
                            isFlacOrLossless -> stats?.sampleRateHz?.takeIf { it > 0 }
                                ?: stats?.claimed?.sampleRateHz?.takeIf { it > 0 }
                                ?: outputStatus.actualSampleRateHz?.takeIf { it > 0 }
                                ?: 44100
                            else -> stats?.sampleRateHz?.takeIf { it > 0 } ?: 44100
                        }

                        val sampleRateDisplay = formatSampleRate(sampleRateHz)



                        val channelsInt = stats?.channels?.takeIf { it > 0 } ?: 2

                        val pcmRateKbps = ((sampleRateHz.toLong() * channelsInt.toLong() * (bitDepthInt?.toLong() ?: 16L)) / 1000L).toInt()



                        val calculatedBitrate = when {

                            isFlacOrLossless -> {

                                stats?.bitrateKbps?.takeIf { it >= 400 }

                                    ?: stats?.claimed?.kbps?.takeIf { it >= 400 }

                                    ?: (if ((bitDepthInt ?: 16) >= 24) (pcmRateKbps * 52) / 100 else (pcmRateKbps * 58) / 100).takeIf { it > 0 }

                            }

                            isOpus -> {

                                stats?.bitrateKbps?.takeIf { it in 50..320 } ?: 160

                            }

                            else -> {

                                stats?.bitrateKbps?.takeIf { it > 0 }

                                    ?: stats?.claimed?.kbps?.takeIf { it > 0 }

                            }

                        }



                        val bitrate = calculatedBitrate?.takeIf { it > 0 }?.let { "$it kbps" } ?: "—"



                        val pcmDataRate = bitDepthInt?.let {

                            pcmRateKbps.takeIf { it > 0 }?.let { "$it kbps" }

                        } ?: "—"



                        val channels = when (channelsInt) {

                            1 -> "Mono"

                            2 -> "Stereo"

                            else -> "$channelsInt ch"

                        }



                        // ── STAGE 0: TRACK INFO ──────────────────────────────

                        PipelineTimelineStage(

                            icon = Icons.Rounded.Description,

                            sectionTitle = "TRACK INFO",

                            stageIndex = 0,

                            totalStages = 5,

                            isLast = false,

                            progressProvider = progressProvider,
                            artworkColor = artworkColor,

                        ) {

                            PipelineRow("Source", sourceName)

                            PipelineRow("Format", format)

                            PipelineRow("Bit Depth", bitDepth)

                            PipelineRow("Sample Rate", sampleRateDisplay)

                            PipelineRow("Bitrate", bitrate)

                            val streamFileSizeDisplay = stats?.fileSizeBytes?.takeIf { it >= 150_000L }?.let { bytes ->
                                val mb = bytes.toDouble() / (1024.0 * 1024.0)
                                String.format(java.util.Locale.US, "%.1f MB", mb)
                            }
                            if (streamFileSizeDisplay != null) {
                                PipelineRow("File Size", streamFileSizeDisplay)
                            }

                            PipelineRow("PCM Data Rate", pcmDataRate)

                            PipelineRow("Channels", channels)

                        }



                        // ── STAGE 1: DECODER ─────────────────────────────────

                        val rawDecoder = stats?.decoderName ?: outputStatus.decoderName

                        val decoderName = AudioDecoderResolver.resolveDecoderName(

                            currentDecoder = rawDecoder,

                            mimeType = stats?.mimeType ?: if (isFlacOrLossless) "audio/flac" else null,

                            isLossless = isFlacOrLossless,

                        )

                        val pcmFormat = bitDepthInt?.let { "$it-bit PCM" }

                            ?: outputStatus.actualEncoding?.let { AudioOutputStatus.encodingLabel(outputStatus) }

                            ?: "16-bit PCM"



                        PipelineTimelineStage(

                            icon = Icons.Rounded.Memory,

                            sectionTitle = "DECODER",

                            stageIndex = 1,

                            totalStages = 5,

                            isLast = false,

                            progressProvider = progressProvider,
                            artworkColor = artworkColor,

                        ) {

                            PipelineRow("Decoder Name", decoderName)

                            PipelineRow("Format", pcmFormat)

                        }



                        // ── STAGE 2: RESAMPLER ───────────────────────────────

                        val targetRate = outputStatus.actualSampleRateHz?.takeIf { it > 0 } ?: 48000

                        val inputRate = stats?.sampleRateHz?.takeIf { it > 0 } ?: stats?.claimed?.sampleRateHz?.takeIf { it > 0 } ?: targetRate

                        val isBitExactPassthrough = (inputRate == targetRate)



                        PipelineTimelineStage(

                            icon = Icons.Rounded.Tune,

                            sectionTitle = "RESAMPLER",

                            stageIndex = 2,

                            totalStages = 5,

                            isLast = false,

                            progressProvider = progressProvider,
                            artworkColor = artworkColor,

                        ) {

                            PipelineRow("I/O Rate", "${formatSampleRate(inputRate)} → ${formatSampleRate(targetRate)}")

                            PipelineRow("Type", if (isBitExactPassthrough) "None" else "Resampler")

                            PipelineRow("Cutoff", "—")

                            PipelineRow("Quality", if (isBitExactPassthrough) "Passthrough" else "Bit-exact rate")

                        }



                        // ── DEVICE RESOLUTION (SHARED BY DSP & OUTPUT) ─────

                        val rawDeviceName = outputStatus.deviceName

                        val isBluetooth = outputStatus.isBluetooth ||

                            rawDeviceName.contains("Bluetooth", ignoreCase = true) ||

                            rawDeviceName.contains("Buds", ignoreCase = true) ||

                            rawDeviceName.contains("Headset", ignoreCase = true) ||

                            rawDeviceName.contains("Headphone", ignoreCase = true) ||

                            rawDeviceName.contains("Airdopes", ignoreCase = true) ||

                            rawDeviceName.contains("Wireless", ignoreCase = true)



                        val deviceName = when {

                            outputStatus.isUsb && rawDeviceName.isNotBlank() && !rawDeviceName.equals("Phone Speaker", ignoreCase = true) -> rawDeviceName

                            isBluetooth && rawDeviceName.isNotBlank() && !rawDeviceName.equals("Phone Speaker", ignoreCase = true) && !rawDeviceName.equals("System default", ignoreCase = true) -> rawDeviceName

                            outputStatus.isSpeaker -> "Phone Speaker"

                            else -> rawDeviceName.ifBlank { android.os.Build.MODEL }

                        }



                        // ── STAGE 3: DSP ─────────────────────────────────────

                        val dspPcm = "Float32"

                        val bufferFrames = targetRate * 4 // standard 4000ms buffer frames matching reference

                        val bufferReport = "2x (4000ms, $bufferFrames frames)"



                        val bitExactDisplay = if (outputStatus.isUsb) {

                            "Yes — bit-perfect to $deviceName"

                        } else {

                            val pcmBits = bitDepthInt ?: 16

                            "Yes ($pcmBits-bit PCM → $pcmBits-bit PCM)"

                        }



                        PipelineTimelineStage(

                            icon = Icons.Rounded.GraphicEq,

                            sectionTitle = "DSP",

                            stageIndex = 3,

                            totalStages = 5,

                            isLast = false,

                            progressProvider = progressProvider,
                            artworkColor = artworkColor,
                            isHighlighted = (eqEnabled || spatialAudio),

                        ) {

                            PipelineRow("PCM Format", dspPcm)

                            PipelineRow("Sample Rate", formatSampleRate(targetRate))

                            PipelineRow("Gain", "—")

                            PipelineRow("Measured", "—")

                            PipelineRow("EQ Preset", if (eqEnabled) eqPreset.name else "Flat")

                            PipelineRow("Stereo Expand", if (spatialAudio) "140%" else "100%")

                            PipelineRow("Buffers", bufferReport)

                            PipelineRow("Output API", outputStatus.sink.ifBlank { "AudioTrack" })

                            PipelineRow("Bit-exact", bitExactDisplay)

                        }



                        // ── STAGE 4: OUTPUT DEVICE ───────────────────────────

                        val routeType = when {

                            outputStatus.isUsb -> "USB"

                            isBluetooth -> "BLUETOOTH"

                            outputStatus.isWired -> "WIRED"

                            else -> "PHONE"

                        }



                        val outputIcon = AudioOutputStatus.iconFor(outputStatus.deviceCategory)



                        val audioTrackLabel = AudioOutputStatus.encodingLabel(outputStatus)



                        val transportDisplay = when {

                            outputStatus.isUsb -> if (outputStatus.sink.contains("AAudio", ignoreCase = true)) "AAudio exclusive" else "Direct USB"

                            else -> outputStatus.sink.ifBlank { "AudioTrack" }

                        }



                        val directDisplay = when {

                            outputStatus.isUsb -> if (outputStatus.sink.contains("AAudio", ignoreCase = true)) {

                                "Active (AAudio exclusive, bypasses mixer)"

                            } else {

                                "Active (Direct USB, bypasses mixer)"

                            }

                            else -> "Not Supported (Mixed Path)"

                        }



                        PipelineTimelineStage(

                            icon = outputIcon,

                            sectionTitle = "OUTPUT DEVICE",

                            stageIndex = 4,

                            totalStages = 5,

                            isLast = true,

                            progressProvider = progressProvider,
                            artworkColor = artworkColor,
                            isHighlighted = (outputStatus.isUsb || isBluetooth),

                        ) {

                            PipelineRow("Device Name", deviceName)

                            PipelineRow("Route", routeType)

                            PipelineRow("Transport", transportDisplay)

                            PipelineRow("Direct", directDisplay)

                            PipelineRow("Output", "${if (audioTrackLabel.contains("16")) "PCM16" else "PCM24"} / ${formatSampleRate(targetRate)}")

                            PipelineRow(

                                "System",

                                if (outputStatus.isUsb) {

                                    "Bypassed (bit-perfect)"

                                } else {

                                    "AudioFlinger Mixer ${formatSampleRate(targetRate)}, HAL PCM24 packed"

                                },

                            )

                            if (outputStatus.isUsb) {

                                val maxSampleRate = outputStatus.sampleRatesHz.maxOrNull() ?: targetRate

                                val maxEncoding = if (outputStatus.encodings.any { it == AudioFormat.ENCODING_PCM_32BIT || it == AudioFormat.ENCODING_PCM_FLOAT }) {

                                    "PCM 32-bit"

                                } else if (outputStatus.encodings.any { it == AudioFormat.ENCODING_PCM_24BIT_PACKED }) {

                                    "PCM 24-bit"

                                } else {

                                    "PCM 16-bit"

                                }

                                PipelineRow("USB Device Capability", "$maxEncoding / ${formatSampleRate(maxSampleRate)}")

                                Text(

                                    text = "Reported by Android for the connected USB device. This describes device capabilities and may differ from the active playback format.",

                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 14.sp),

                                    color = Color.White.copy(alpha = 0.50f),

                                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),

                                )

                            }

                            if (!isBluetooth && !outputStatus.isUsb) {

                                PipelineRow("Fallback", "Speaker output capped at 16-bit to avoid OEM mixer distortion")

                            } else if (outputStatus.isUsb && outputStatus.encodings.isNotEmpty() && !outputStatus.encodings.any { it == AudioFormat.ENCODING_PCM_24BIT_PACKED || it == AudioFormat.ENCODING_PCM_32BIT || it == AudioFormat.ENCODING_PCM_FLOAT }) {

                                PipelineRow("Fallback", "USB device advertises 16-bit PCM only")

                            }

                        }

                    }



                    Spacer(Modifier.height(10.dp))



                    // Bottom: Centered "Done" button

                    Box(

                        modifier = Modifier

                            .fillMaxWidth()

                            .clickable(

                                interactionSource = remember { MutableInteractionSource() },

                                indication = null,

                                onClick = onDismiss,

                            )

                            .padding(vertical = 8.dp),

                        contentAlignment = Alignment.Center,

                    ) {

                        Text(

                            text = "Done",

                            style = MaterialTheme.typography.titleMedium.copy(

                                fontSize = 16.sp,

                                fontWeight = FontWeight.Bold,

                            ),

                            color = Color.White,

                        )

                    }

                }

            }

        }

    }

}



/**
 * Vertical timeline stage container matching reference UI.
 *
 * Features:
 * - Dynamic color derived automatically from song cover art.
 * - Animated traveling wave squiggle pulse down the connecting line.
 * - Radiant neon halo on active / highlighted node icons and when the signal wave passes through.
 */
@Composable
private fun PipelineTimelineStage(
    icon: ImageVector,
    sectionTitle: String,
    stageIndex: Int = 0,
    totalStages: Int = 5,
    isLast: Boolean = false,
    progressProvider: () -> Float = { 0f },
    artworkColor: Color = Color(0xFF38BDF8),
    isHighlighted: Boolean = false,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
    ) {
        // Left timeline column: circular node + vertical line with hardware-accelerated draw
        Column(
            modifier = Modifier
                .width(32.dp)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Circular icon node: static layout with zero-allocation drawBehind halo and border
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .drawBehind {
                        val progress = progressProvider()
                        val nodeProgress = stageIndex / 4f
                        val dist = minOf(
                            kotlin.math.abs(progress - nodeProgress),
                            kotlin.math.abs(progress - (nodeProgress + 1f)),
                            kotlin.math.abs((progress + 1f) - nodeProgress),
                        )
                        val waveHit = (1f - (dist / 0.10f)).coerceIn(0f, 1f)
                        val waveGlow = waveHit * waveHit
                        val baseGlow = if (isHighlighted) 0.65f else 0f
                        val glowIntensity = maxOf(baseGlow, waveGlow)

                        val centerOffset = Offset(size.width / 2f, size.height / 2f)

                        // 1. Radiant ambient halo bloom
                        if (glowIntensity > 0.03f) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        artworkColor.copy(alpha = 0.60f * glowIntensity),
                                        artworkColor.copy(alpha = 0.20f * glowIntensity),
                                        Color.Transparent,
                                    ),
                                    center = centerOffset,
                                    radius = size.width / 2f,
                                ),
                                radius = size.width / 2f,
                                center = centerOffset,
                            )
                        }

                        // 2. High-luminance circular border
                        val nodeRadius = 13.dp.toPx()
                        val borderColor = lerp(
                            Color.White.copy(alpha = 0.20f),
                            artworkColor.copy(alpha = 0.95f),
                            glowIntensity,
                        )
                        val strokeWidth = (1.dp + 0.6.dp * glowIntensity).toPx()
                        drawCircle(
                            color = borderColor,
                            radius = nodeRadius,
                            center = centerOffset,
                            style = Stroke(width = strokeWidth),
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF222228)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.92f),
                        modifier = Modifier.size(13.5.dp),
                    )
                }
            }

            // Connecting vertical straight line with animated signal light pulse (draw phase only)
            if (!isLast) {
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .width(20.dp)
                        .padding(vertical = 2.dp),
                ) {
                    val flowProgress = progressProvider()
                    val centerX = size.width / 2f
                    val lineHeight = size.height

                    // 1. Clean straight vertical guide matching song cover color
                    drawLine(
                        color = artworkColor.copy(alpha = 0.25f),
                        start = Offset(centerX, 0f),
                        end = Offset(centerX, lineHeight),
                        strokeWidth = 1.6.dp.toPx(),
                        cap = StrokeCap.Round,
                    )

                    // 4 segments across the 5 stages (0->1, 1->2, 2->3, 3->4)
                    val segDuration = 0.25f
                    val segStart = stageIndex * segDuration
                    val segEnd = (stageIndex + 1) * segDuration

                    if (flowProgress in segStart..segEnd) {
                        val localProgress = ((flowProgress - segStart) / segDuration).coerceIn(0f, 1f)
                        val beadY = lineHeight * localProgress

                        // Trailing luminous light streak along the straight line
                        val tailLength = 16.dp.toPx()
                        val tailStartY = (beadY - tailLength).coerceAtLeast(0f)
                        if (tailStartY < beadY) {
                            drawLine(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        artworkColor.copy(alpha = 0.75f),
                                    ),
                                    startY = tailStartY,
                                    endY = beadY,
                                ),
                                start = Offset(centerX, tailStartY),
                                end = Offset(centerX, beadY),
                                strokeWidth = 2.4.dp.toPx(),
                                cap = StrokeCap.Round,
                            )
                        }

                        // Radiant halo around the moving signal pulse
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.95f),
                                    artworkColor.copy(alpha = 0.70f),
                                    Color.Transparent,
                                ),
                                center = Offset(centerX, beadY),
                                radius = 6.5.dp.toPx(),
                            ),
                            radius = 6.5.dp.toPx(),
                            center = Offset(centerX, beadY),
                        )

                        // Center white light core
                        drawCircle(
                            color = Color.White,
                            radius = 1.8.dp.toPx(),
                            center = Offset(centerX, beadY),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        // Right content block: Section title + Key-Value rows + Divider
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(bottom = if (isLast) 4.dp else 0.dp),
        ) {
            Text(
                text = sectionTitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                ),
                color = Color.White.copy(alpha = 0.85f),
            )
            Spacer(Modifier.height(5.dp))
            content()
            if (!isLast) {
                Spacer(Modifier.height(11.dp))
                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.08f),
                    thickness = 0.8.dp,
                )
                Spacer(Modifier.height(11.dp))
            }
        }
    }
}

@Composable
private fun PipelineRow(

    label: String,

    value: String,

    modifier: Modifier = Modifier,

) {

    Text(

        text = buildAnnotatedString {

            withStyle(

                SpanStyle(

                    fontWeight = FontWeight.Bold,

                    color = Color.White.copy(alpha = 0.95f),

                    fontSize = 12.sp,

                ),

            ) {

                append("$label: ")

            }

            withStyle(

                SpanStyle(

                    fontWeight = FontWeight.Normal,

                    color = Color.White.copy(alpha = 0.72f),

                    fontSize = 12.sp,

                ),

            ) {

                append(value)

            }

        },

        modifier = modifier.padding(vertical = 1.6.dp),

        lineHeight = 16.sp,

    )

}





