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
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Usb
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.dhvani.data.NerdStats
import com.music.dhvani.data.settings.AppSettings
import com.music.dhvani.playback.AudioOutputStatus
import com.music.dhvani.playback.eq.EqualizerManager

private val PIPELINE_CARD_SHAPE = RoundedCornerShape(22.dp)
private val PIPELINE_SCRIM_COLOR = Color.Black.copy(alpha = 0.60f)

/**
 * Hi-Res Audio Pipeline Inspector dialog.
 *
 * Visualizes the live signal path from decoder to output with an animated vertical timeline:
 * Track Info -> Decoder -> Resampler -> DSP -> Output Device.
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
            decorFitsSystemWindows = false,
        ),
    ) {
        val dialogWindow = (androidx.compose.ui.platform.LocalView.current.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
        androidx.compose.runtime.SideEffect {
            dialogWindow?.setDimAmount(0.60f)
        }
        val nerdStats by NerdStats.current.collectAsStateWithLifecycle()
        val outputStatus by AudioOutputStatus.current.collectAsStateWithLifecycle()

        val eqEnabled by EqualizerManager.enabled.collectAsStateWithLifecycle()
        val eqPreset by EqualizerManager.selectedPreset.collectAsStateWithLifecycle()
        val spatialAudio by AppSettings.spatialAudio.collectAsStateWithLifecycle()

        // Animated glowing bead traveling continuously down the vertical pipeline (single dot)
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
                    .heightIn(max = 640.dp)
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

                    Spacer(Modifier.height(18.dp))

                    // Scrollable Pipeline Timeline Stages List
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth()
                            .nestedScroll(pipelineNestedScroll)
                            .verticalScroll(scrollState),
                    ) {
                        val stats = nerdStats
                        val sourceName = stats?.sourceName
                            ?: stats?.claimed?.source
                            ?: when {
                                stats?.isLossless == true -> "Lossless Plugin"
                                stats?.isHiQuality == true && (stats.mimeType?.contains("mp4") == true || stats.mimeType?.contains("aac") == true) -> "JioSaavn"
                                else -> "YouTube Music"
                            }
                        val rawMime = stats?.mimeType ?: stats?.claimed?.codec ?: "audio/opus"
                        val format = when {
                            rawMime.contains("flac", ignoreCase = true) -> "FLAC Lossless"
                            rawMime.contains("alac", ignoreCase = true) -> "ALAC Lossless"
                            rawMime.contains("opus", ignoreCase = true) -> "Opus"
                            rawMime.contains("mp4a", ignoreCase = true) || rawMime.contains("aac", ignoreCase = true) -> "AAC LC"
                            rawMime.contains("vorbis", ignoreCase = true) -> "Ogg Vorbis"
                            rawMime.contains("mp3", ignoreCase = true) -> "MP3"
                            else -> NerdStats.codecLabel(stats?.mimeType) ?: "Opus"
                        }
                        val bitDepth = stats?.bitDepth?.let { "$it-bit" }
                            ?: stats?.claimed?.bitDepth?.let { "$it-bit" }
                            ?: if (stats?.isLossless == true) "24-bit" else "16-bit"
                        val sampleRate = stats?.sampleRateHz?.let { "$it Hz" }
                            ?: stats?.claimed?.sampleRateHz?.let { "$it Hz" }
                            ?: "${outputStatus.actualSampleRateHz ?: 48000} Hz"
                        val bitrate = stats?.bitrateKbps?.let { "$it kbps" }
                            ?: stats?.claimed?.kbps?.let { "$it kbps" }
                            ?: if (stats?.isLossless == true) "921 kbps" else "160 kbps"
                        val channels = when (stats?.channels) {
                            1 -> "Mono"
                            2 -> "Stereo"
                            null -> "Stereo"
                            else -> "${stats.channels} ch"
                        }

                        // ── STAGE 1: TRACK INFO ──────────────────────────────
                        PipelineTimelineStage(
                            icon = Icons.Rounded.MusicNote,
                            sectionTitle = "TRACK INFO",
                            stageIndex = 0,
                            totalStages = 4,
                            isLast = false,
                            flowProgress = flowProgress,
                        ) {
                            PipelineRow("Source", sourceName)
                            PipelineRow("Format", format)
                            PipelineRow("Bit Depth", bitDepth)
                            PipelineRow("Sample Rate", sampleRate)
                            PipelineRow("Bitrate", bitrate)
                            PipelineRow("Channels", channels)
                        }

                        // ── STAGE 2: DECODER ─────────────────────────────────
                        val decoderName = when {
                            rawMime.contains("flac", ignoreCase = true) -> "c2.android.flac.decoder"
                            rawMime.contains("opus", ignoreCase = true) -> "c2.android.opus.decoder"
                            rawMime.contains("aac", ignoreCase = true) || rawMime.contains("mp4", ignoreCase = true) -> "c2.android.aac.decoder"
                            rawMime.contains("mp3", ignoreCase = true) -> "c2.android.mp3.decoder"
                            rawMime.contains("vorbis", ignoreCase = true) -> "c2.android.vorbis.decoder"
                            else -> "c2.android.audio.decoder"
                        }
                        val pcmFormat = if (stats?.isLossless == true) "24-bit PCM" else "16-bit PCM"

                        PipelineTimelineStage(
                            icon = Icons.Rounded.Memory,
                            sectionTitle = "DECODER",
                            stageIndex = 1,
                            totalStages = 4,
                            isLast = false,
                            flowProgress = flowProgress,
                        ) {
                            PipelineRow("Decoder Name", decoderName, newline = true)
                            PipelineRow("Format", pcmFormat)
                        }

                        // ── STAGE 3: RESAMPLER ───────────────────────────────
                        val targetRate = outputStatus.actualSampleRateHz ?: 48000
                        val isBitExactPassthrough = (stats?.sampleRateHz ?: 48000) == targetRate

                        PipelineTimelineStage(
                            icon = Icons.Rounded.Tune,
                            sectionTitle = "RESAMPLER",
                            stageIndex = 2,
                            totalStages = 4,
                            isLast = false,
                            flowProgress = flowProgress,
                        ) {
                            PipelineRow("I/O Rate", "${stats?.sampleRateHz ?: 48000} Hz → $targetRate Hz")
                            PipelineRow("Type", if (isBitExactPassthrough) "Direct Passthrough" else "Resampler")
                            PipelineRow("Cutoff", "—")
                            PipelineRow("Quality", if (isBitExactPassthrough) "Bit-exact" else "Resampled")
                        }

                        // ── STAGE 4: DSP ─────────────────────────────────────
                        PipelineTimelineStage(
                            icon = Icons.Rounded.GraphicEq,
                            sectionTitle = "DSP",
                            stageIndex = 3,
                            totalStages = 4,
                            isLast = false,
                            flowProgress = flowProgress,
                        ) {
                            PipelineRow("PCM Format", "Float32")
                            PipelineRow("Sample Rate", "$targetRate Hz")
                            PipelineRow("Gain", "—")
                            PipelineRow("Measured", "—")
                            PipelineRow("EQ Preset", if (eqEnabled) eqPreset else "Flat")
                            PipelineRow("Stereo Expand", if (spatialAudio) "140%" else "100%")
                            PipelineRow("Buffers", "2x (500ms, 24000 frames)")
                            PipelineRow("Output API", "AudioTrack")
                            PipelineRow("Bit-exact", if (outputStatus.isUsb) "Yes (Direct Bit-Perfect)" else "Yes (16-bit PCM → 16-bit PCM)")
                        }

                        // ── STAGE 5: OUTPUT DEVICE ───────────────────────────
                        val context = androidx.compose.ui.platform.LocalContext.current
                        val deviceName = if (outputStatus.deviceName.isNotBlank() &&
                            !outputStatus.deviceName.equals("System default", ignoreCase = true) &&
                            !outputStatus.deviceName.equals(android.os.Build.MODEL, ignoreCase = true) &&
                            !outputStatus.deviceName.equals("Phone Speaker", ignoreCase = true)
                        ) {
                            outputStatus.deviceName
                        } else {
                            AudioOutputStatus.getPhoneName(context)
                        }

                        val outputIcon = when {
                            outputStatus.isUsb -> Icons.Rounded.Usb
                            deviceName.contains("Bluetooth", ignoreCase = true) ||
                                deviceName.contains("Buds", ignoreCase = true) ||
                                deviceName.contains("Airdopes", ignoreCase = true) ||
                                deviceName.contains("Headset", ignoreCase = true) ||
                                deviceName.contains("Headphone", ignoreCase = true) ||
                                deviceName.contains("Wireless", ignoreCase = true) -> Icons.Rounded.Headphones
                            else -> Icons.AutoMirrored.Rounded.VolumeUp
                        }

                        PipelineTimelineStage(
                            icon = outputIcon,
                            sectionTitle = "OUTPUT DEVICE",
                            stageIndex = 4,
                            totalStages = 4,
                            isLast = true,
                            flowProgress = flowProgress,
                        ) {
                            PipelineRow("Device Name", deviceName)
                            PipelineRow("Route", if (outputStatus.isUsb) "USB DAC" else if (deviceName.contains("Bluetooth", ignoreCase = true)) "BLUETOOTH" else "PHONE")
                            PipelineRow("Transport", outputStatus.sink.ifBlank { "AudioTrack" })
                            PipelineRow("Direct", if (outputStatus.isUsb) "Supported (Direct Bit-Perfect)" else "Not Supported (Mixed Path)")
                            PipelineRow("AudioTrack", "PCM16 / $targetRate Hz")
                            PipelineRow("System", "AudioFlinger Mixer ${outputStatus.actualSampleRateHz ?: 48000} Hz, HAL PCM24 packed")
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Bottom: Centered "Done" button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onDismiss,
                            )
                            .padding(vertical = 6.dp),
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
 * Vertical timeline stage container with animated glowing signal flow bead.
 */
@Composable
private fun PipelineTimelineStage(
    icon: ImageVector,
    sectionTitle: String,
    stageIndex: Int = 0,
    totalStages: Int = 4,
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
                    .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(14.dp),
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
                    // Static gray connecting guide
                    drawLine(
                        color = Color.White.copy(alpha = 0.20f),
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
                                    Color(0xFF38BDF8).copy(alpha = 0.45f),
                                    Color.Transparent,
                                ),
                                center = Offset(centerX, beadY),
                                radius = 5.dp.toPx(),
                            ),
                            radius = 5.dp.toPx(),
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

        // Right content block: Uppercase section title + Key-Value rows
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = if (isLast) 4.dp else 16.dp),
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
            Spacer(Modifier.height(4.dp))
            content()
        }
    }
}

/**
 * Key-Value row (Bold label + normal value).
 */
@Composable
private fun PipelineRow(
    label: String,
    value: String,
    newline: Boolean = false,
) {
    if (newline) {
        Column(modifier = Modifier.padding(vertical = 1.5.dp)) {
            Text(
                text = "$label:",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = Color.White.copy(alpha = 0.90f),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                ),
                color = Color.White.copy(alpha = 0.75f),
            )
        }
    } else {
        Row(
            modifier = Modifier.padding(vertical = 1.5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$label: ",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = Color.White.copy(alpha = 0.90f),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                ),
                color = Color.White.copy(alpha = 0.75f),
            )
        }
    }
}
