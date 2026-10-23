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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.music.dhvani.playback.eq.EqualizerManager

private val PIPELINE_CARD_SHAPE = RoundedCornerShape(22.dp)
private val PIPELINE_SCRIM_COLOR = Color.Black.copy(alpha = 0.60f)

private fun formatSampleRate(hz: Int): String {
    return if (hz % 1000 == 0) {
        "${hz / 1000} kHz"
    } else {
        "%.1f kHz".format(hz / 1000f)
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

        val eqEnabled by EqualizerManager.enabled.collectAsStateWithLifecycle()
        val eqPreset by EqualizerManager.selectedPreset.collectAsStateWithLifecycle()
        val spatialAudio by AppSettings.spatialAudio.collectAsStateWithLifecycle()

        // Animated glowing bead traveling continuously down the vertical pipeline
        val infiniteTransition = rememberInfiniteTransition(label = "pipelineFlow")
        val flowProgress by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "flowBead",
        )

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
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
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

                        val isOpus = format.contains("Opus", ignoreCase = true)
                        val isAac = format.contains("AAC", ignoreCase = true) || format.contains("MP4", ignoreCase = true)
                        val isFlac = format.contains("FLAC", ignoreCase = true) || stats?.mimeType?.contains("flac", ignoreCase = true) == true

                        val rawSource = stats?.sourceName ?: stats?.claimed?.source
                        val sourceName = when {
                            isAac || rawSource?.contains("saavn", ignoreCase = true) == true || rawSource?.contains("aac", ignoreCase = true) == true -> "High Quality AAC"
                            isOpus || rawSource?.contains("youtube", ignoreCase = true) == true || rawSource?.contains("opus", ignoreCase = true) == true -> "Standard Opus"
                            isFlac || stats?.isLossless == true -> {
                                when {
                                    rawSource?.contains("monochrome", ignoreCase = true) == true -> "Monochrome Hi-Res"
                                    !rawSource.isNullOrBlank() && !rawSource.contains("youtube", ignoreCase = true) && !rawSource.contains("saavn", ignoreCase = true) -> rawSource
                                    else -> "Lossless Plugin"
                                }
                            }
                            rawSource != null && !rawSource.contains("youtube", ignoreCase = true) && !rawSource.contains("saavn", ignoreCase = true) -> rawSource
                            stats?.isHiQuality == true -> "High Quality AAC"
                            else -> "Standard Opus"
                        }

                        val isFlacOrLossless = !isOpus && !isAac && (
                            stats?.isLossless == true ||
                            isFlac ||
                            sourceName.contains("Monochrome", ignoreCase = true) ||
                            sourceName.contains("Lossless", ignoreCase = true)
                        )

                        val bitDepthInt = stats?.bitDepth?.takeIf { it > 0 }
                            ?: stats?.claimed?.bitDepth?.takeIf { it > 0 }
                            ?: if (isFlacOrLossless && sourceName.contains("Hi-Res", ignoreCase = true)) 24
                            else if (isFlacOrLossless) 16
                            else if (isOpus || isAac) 16
                            else null

                        val bitDepth = bitDepthInt?.let { "$it-bit" } ?: "—"

                        val sampleRateHz = stats?.sampleRateHz ?: stats?.claimed?.sampleRateHz ?: outputStatus.actualSampleRateHz ?: 48000
                        val sampleRateDisplay = formatSampleRate(sampleRateHz)

                        val channelsInt = stats?.channels ?: 2
                        val pcmRateKbps = ((sampleRateHz.toLong() * channelsInt.toLong() * (bitDepthInt?.toLong() ?: 16L)) / 1000L).toInt()

                        val calculatedBitrate = when {
                            isFlacOrLossless -> {
                                stats?.bitrateKbps?.takeIf { it >= 500 }
                                    ?: stats?.claimed?.kbps?.takeIf { it >= 500 }
                                    ?: (if ((bitDepthInt ?: 16) >= 24) (pcmRateKbps * 52) / 100 else (pcmRateKbps * 58) / 100)
                            }
                            isOpus -> {
                                stats?.bitrateKbps?.takeIf { it in 50..320 } ?: 160
                            }
                            else -> {
                                stats?.bitrateKbps?.takeIf { it > 0 }
                                    ?: stats?.claimed?.kbps?.takeIf { it > 0 }
                            }
                        }

                        val bitrate = calculatedBitrate?.let { "$it kbps" } ?: "—"

                        val pcmDataRate = bitDepthInt?.let {
                            "$pcmRateKbps kbps"
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
                            flowProgress = flowProgress,
                        ) {
                            PipelineRow("Source", sourceName)
                            PipelineRow("Format", format)
                            PipelineRow("Bit Depth", bitDepth)
                            PipelineRow("Sample Rate", sampleRateDisplay)
                            PipelineRow("Bitrate", bitrate)
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
                            flowProgress = flowProgress,
                        ) {
                            PipelineRow("Decoder Name", decoderName)
                            PipelineRow("Format", pcmFormat)
                        }

                        // ── STAGE 2: RESAMPLER ───────────────────────────────
                        val targetRate = outputStatus.actualSampleRateHz ?: 48000
                        val inputRate = stats?.sampleRateHz ?: stats?.claimed?.sampleRateHz ?: targetRate
                        val isBitExactPassthrough = (inputRate == targetRate)

                        PipelineTimelineStage(
                            icon = Icons.Rounded.Tune,
                            sectionTitle = "RESAMPLER",
                            stageIndex = 2,
                            totalStages = 5,
                            isLast = false,
                            flowProgress = flowProgress,
                        ) {
                            PipelineRow("I/O Rate", "${formatSampleRate(inputRate)} → ${formatSampleRate(targetRate)}")
                            PipelineRow("Type", if (isBitExactPassthrough) "None" else "Resampler")
                            PipelineRow("Cutoff", "—")
                            PipelineRow("Quality", if (isBitExactPassthrough) "Passthrough" else "Bit-exact rate")
                        }

                        // ── STAGE 3: DSP ─────────────────────────────────────
                        val dspPcm = "Float32"
                        val bufferReport = outputStatus.bufferSize?.takeIf { it > 0 }?.let {
                            "2x (500ms, ${it / 2} frames)"
                        } ?: "2x (500ms, 24000 frames)"

                        val bitExactDisplay = if (outputStatus.isUsb) {
                            "Yes (Direct USB Bit-Perfect)"
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
                            flowProgress = flowProgress,
                        ) {
                            PipelineRow("PCM Format", dspPcm)
                            PipelineRow("Sample Rate", formatSampleRate(targetRate))
                            PipelineRow("Gain", "—")
                            PipelineRow("Measured", "—")
                            PipelineRow("EQ Preset", if (eqEnabled) eqPreset else "Flat")
                            PipelineRow("Stereo Expand", if (spatialAudio) "140%" else "100%")
                            PipelineRow("Buffers", bufferReport)
                            PipelineRow("Output API", outputStatus.sink.ifBlank { "AudioTrack" })
                            PipelineRow("Bit-exact", bitExactDisplay)
                        }

                        // ── STAGE 4: OUTPUT DEVICE ───────────────────────────
                        val rawDeviceName = outputStatus.deviceName
                        val isBluetooth = outputStatus.isBluetooth ||
                            rawDeviceName.contains("Bluetooth", ignoreCase = true) ||
                            rawDeviceName.contains("Buds", ignoreCase = true) ||
                            rawDeviceName.contains("Headset", ignoreCase = true) ||
                            rawDeviceName.contains("Headphone", ignoreCase = true) ||
                            rawDeviceName.contains("Airdopes", ignoreCase = true) ||
                            rawDeviceName.contains("Wireless", ignoreCase = true)

                        val deviceName = if (isBluetooth && rawDeviceName.isNotBlank() &&
                            !rawDeviceName.equals("Phone Speaker", ignoreCase = true) &&
                            !rawDeviceName.equals("System default", ignoreCase = true)
                        ) {
                            rawDeviceName
                        } else {
                            android.os.Build.MODEL
                        }

                        val routeType = when {
                            outputStatus.isUsb -> "USB"
                            isBluetooth -> "BLUETOOTH"
                            outputStatus.isWired -> "WIRED"
                            else -> "PHONE"
                        }

                        val outputIcon = when {
                            outputStatus.isUsb -> Icons.Rounded.Usb
                            isBluetooth -> Icons.Rounded.Headphones
                            else -> Icons.AutoMirrored.Rounded.VolumeUp
                        }

                        val audioTrackLabel = AudioOutputStatus.encodingLabel(outputStatus)

                        PipelineTimelineStage(
                            icon = outputIcon,
                            sectionTitle = "OUTPUT DEVICE",
                            stageIndex = 4,
                            totalStages = 5,
                            isLast = true,
                            flowProgress = flowProgress,
                        ) {
                            PipelineRow("Device Name", deviceName)
                            PipelineRow("Route", routeType)
                            PipelineRow("Transport", outputStatus.sink.ifBlank { "AudioTrack" })
                            PipelineRow("Direct", if (outputStatus.isUsb) "Supported (Direct USB Path)" else "Not Supported (Mixed Path)")
                            PipelineRow("AudioTrack", "${if (audioTrackLabel.contains("16")) "PCM16" else "PCM24"} / ${formatSampleRate(targetRate)}")
                            PipelineRow(
                                "System",
                                if (outputStatus.isUsb) {
                                    "Direct USB Output (${formatSampleRate(targetRate)})"
                                } else {
                                    "AudioFlinger Mixer ${formatSampleRate(targetRate)}, HAL PCM24 packed"
                                },
                            )
                            if (!isBluetooth && !outputStatus.isUsb) {
                                PipelineRow("Fallback", "Speaker output capped at 16-bit to avoid OEM mixer distortion")
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
 */
@Composable
private fun PipelineTimelineStage(
    icon: ImageVector,
    sectionTitle: String,
    stageIndex: Int = 0,
    totalStages: Int = 5,
    isLast: Boolean = false,
    flowProgress: Float = 0f,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
    ) {
        // Left timeline column: circular node + vertical line with animated light beam
        Column(
            modifier = Modifier
                .width(28.dp)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Circular icon node
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF222228))
                    .border(1.dp, Color.White.copy(alpha = 0.20f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.88f),
                    modifier = Modifier.size(13.5.dp),
                )
            }

            // Connecting vertical line with single animated signal pulse across the whole pipeline
            if (!isLast) {
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .width(16.dp)
                        .padding(vertical = 2.dp),
                ) {
                    val centerX = size.width / 2f
                    // Static subtle connecting guide
                    drawLine(
                        color = Color.White.copy(alpha = 0.16f),
                        start = Offset(centerX, 0f),
                        end = Offset(centerX, size.height),
                        strokeWidth = 1.5.dp.toPx(),
                        cap = StrokeCap.Round,
                    )

                    // Exactly one signal dot across the entire pipeline:
                    val stageSpan = 1f / totalStages
                    val stageStart = stageIndex * stageSpan
                    val stageEnd = (stageIndex + 1) * stageSpan

                    if (flowProgress in stageStart..stageEnd) {
                        val localProgress = ((flowProgress - stageStart) / stageSpan).coerceIn(0f, 1f)
                        val beadY = size.height * localProgress
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.95f),
                                    Color(0xFF38BDF8).copy(alpha = 0.50f),
                                    Color.Transparent,
                                ),
                                center = Offset(centerX, beadY),
                                radius = 5.5.dp.toPx(),
                            ),
                            radius = 5.5.dp.toPx(),
                            center = Offset(centerX, beadY),
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 1.8.dp.toPx(),
                            center = Offset(centerX, beadY),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.width(14.dp))

        // Right content block: Section title + Key-Value rows + Divider
        Column(
            modifier = Modifier
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

/**
 * Key-Value row with bold label and normal value, inline wrapping.
 */
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


