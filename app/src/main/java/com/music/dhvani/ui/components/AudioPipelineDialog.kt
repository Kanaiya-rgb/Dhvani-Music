package com.music.dhvani.ui.components

import android.media.AudioFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BluetoothAudio
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Speaker
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.dhvani.R
import com.music.dhvani.data.NerdStats
import com.music.dhvani.data.settings.AppSettings
import com.music.dhvani.playback.AudioOutputStatus
import com.music.dhvani.playback.eq.EqualizerManager

private val PIPELINE_CARD_SHAPE = RoundedCornerShape(26.dp)
private val PIPELINE_SCRIM_COLOR = Color.Black.copy(alpha = 0.25f)

// Vibrant stage accent color palette
private val COLOR_STAGE_TRACK = Color(0xFF38BDF8)     // Bright Sky Cyan
private val COLOR_STAGE_DECODER = Color(0xFF34D399)   // Mint / Emerald Green
private val COLOR_STAGE_RESAMPLER = Color(0xFFFBBF24) // Warm Gold / Amber
private val COLOR_STAGE_DSP = Color(0xFFA78BFA)       // Studio Violet
private val COLOR_STAGE_OUTPUT = Color(0xFFFB7185)    // Coral / Rose

/**
 * Premium studio audio playback pipeline inspection dialog.
 *
 * Displays live, authoritative details for each stage in the audio pipeline:
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
            dialogWindow?.setDimAmount(0.30f)
        }
        val nerdStats by NerdStats.current.collectAsStateWithLifecycle()
        val outputStatus by AudioOutputStatus.current.collectAsStateWithLifecycle()

    val eqEnabled by EqualizerManager.enabled.collectAsStateWithLifecycle()
    val eqPreset by EqualizerManager.selectedPreset.collectAsStateWithLifecycle()
    val spatialAudio by AppSettings.spatialAudio.collectAsStateWithLifecycle()

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
        val primaryTint = themeColors.firstOrNull() ?: MaterialTheme.colorScheme.primary
        val secondaryTint = themeColors.getOrNull(1) ?: primaryTint

        val cardBrush = Brush.verticalGradient(
            colors = listOf(
                androidx.compose.ui.graphics.lerp(Color(0xFF18181D), primaryTint, 0.20f),
                Color(0xFF131316),
                androidx.compose.ui.graphics.lerp(Color(0xFF141418), secondaryTint, 0.12f),
            ),
        )

        val borderStroke = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    primaryTint.copy(alpha = 0.50f),
                    Color.White.copy(alpha = 0.15f),
                    secondaryTint.copy(alpha = 0.25f),
                ),
            ),
        )

        Surface(
            modifier = Modifier
                .onGloballyPositioned { coordinates ->
                    cardBoundsInRoot = coordinates.boundsInRoot()
                }
                .widthIn(min = 310.dp, max = 360.dp)
                .fillMaxWidth(0.90f)
                .heightIn(max = 660.dp)
                .clip(PIPELINE_CARD_SHAPE),
            shape = PIPELINE_CARD_SHAPE,
            color = Color.Transparent,
            border = borderStroke,
            shadowElevation = 18.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardBrush)
                    .padding(horizontal = 20.dp, vertical = 18.dp),
            ) {
                // Top Header Row with Title, Badge, and Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(primaryTint),
                            )
                            Text(
                                text = "STUDIO PIPELINE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp,
                                ),
                                color = primaryTint,
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.audio_pipeline),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = Color.White,
                        )
                    }

                    // Frosted Close Button
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Scrollable Stages List
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .nestedScroll(pipelineNestedScroll)
                        .verticalScroll(scrollState),
                ) {
                    // ── 1. Track Info Stage ─────────────────────────────────
                    val stats = nerdStats
                    val sourceName = stats?.sourceName
                        ?: stats?.claimed?.source
                        ?: when {
                            stats?.isLossless == true -> "Lossless Plugin"
                            stats?.isHiQuality == true && (stats.mimeType?.contains("mp4") == true || stats.mimeType?.contains("aac") == true) -> "JioSaavn"
                            else -> "YouTube Music"
                        }
                    val format = NerdStats.codecLabel(stats?.mimeType) ?: stats?.mimeType ?: "Opus"
                    val bitDepth = stats?.bitDepth?.let { "$it-bit" }
                        ?: stats?.claimed?.bitDepth?.let { "$it-bit" }
                        ?: "16-bit"
                    val sampleRate = stats?.sampleRateHz?.let { "$it Hz" }
                        ?: stats?.claimed?.sampleRateHz?.let { "$it Hz" }
                        ?: "44,100 Hz"
                    val bitrate = stats?.bitrateKbps?.let { "$it kbps" } ?: "160 kbps"
                    val channels = when (stats?.channels) {
                        1 -> stringResource(R.string.mono)
                        2 -> stringResource(R.string.stereo)
                        null -> "Stereo"
                        else -> "${nerdStats?.channels} ch"
                    }

                    PipelineStage(
                        icon = Icons.Rounded.GraphicEq,
                        iconTint = COLOR_STAGE_TRACK,
                        stageNumber = "01",
                        title = stringResource(R.string.pipeline_track_info),
                        statusChip = format,
                        statusColor = COLOR_STAGE_TRACK,
                        isLast = false,
                        nextStageColor = COLOR_STAGE_DECODER,
                    ) {
                        PipelineRow(stringResource(R.string.pipeline_source), sourceName)
                        PipelineRow(stringResource(R.string.pipeline_format), format)
                        PipelineRow(stringResource(R.string.pipeline_bit_depth), bitDepth, isMonospace = true)
                        PipelineRow(stringResource(R.string.pipeline_sample_rate), sampleRate, isMonospace = true)
                        PipelineRow(stringResource(R.string.pipeline_bitrate), bitrate, isMonospace = true)
                        PipelineRow(stringResource(R.string.pipeline_channels), channels)
                    }

                    // ── 2. Decoder Stage ───────────────────────────────────
                    val decoderName = outputStatus.decoderName ?: "ExoPlayer MediaCodec"

                    PipelineStage(
                        icon = Icons.Rounded.Memory,
                        iconTint = COLOR_STAGE_DECODER,
                        stageNumber = "02",
                        title = stringResource(R.string.pipeline_decoder),
                        statusChip = "Hardware",
                        statusColor = COLOR_STAGE_DECODER,
                        isLast = false,
                        nextStageColor = COLOR_STAGE_RESAMPLER,
                    ) {
                        PipelineRow(stringResource(R.string.pipeline_decoder_name), decoderName, isMonospace = true)
                    }

                    // ── 3. Resampler Stage ─────────────────────────────────
                    val inRate = nerdStats?.sampleRateHz ?: 44100
                    val outRate = outputStatus.actualSampleRateHz ?: inRate
                    val isPassthrough = inRate == outRate
                    val ioRateText = "$inRate Hz → $outRate Hz"
                    val resamplerType = if (isPassthrough) "None (Direct)" else "Android AudioResampler"
                    val qualityText = if (isPassthrough) "Bit-Perfect Passthrough" else "Resampled"

                    PipelineStage(
                        icon = Icons.Rounded.AutoAwesome,
                        iconTint = COLOR_STAGE_RESAMPLER,
                        stageNumber = "03",
                        title = stringResource(R.string.pipeline_resampler),
                        statusChip = if (isPassthrough) "Passthrough" else "Resampled",
                        statusColor = if (isPassthrough) COLOR_STAGE_DECODER else COLOR_STAGE_RESAMPLER,
                        isLast = false,
                        nextStageColor = COLOR_STAGE_DSP,
                    ) {
                        PipelineRow(stringResource(R.string.pipeline_io_rate), ioRateText, isMonospace = true)
                        PipelineRow(stringResource(R.string.pipeline_type), resamplerType)
                        PipelineRow(stringResource(R.string.pipeline_cutoff), "—")
                        PipelineRow(stringResource(R.string.pipeline_quality), qualityText)
                    }

                    // ── 4. DSP Stage ───────────────────────────────────────
                    val pcmFormat = when (outputStatus.actualEncoding) {
                        AudioFormat.ENCODING_PCM_FLOAT -> "32-bit Float"
                        AudioFormat.ENCODING_PCM_16BIT -> "16-bit PCM"
                        AudioFormat.ENCODING_PCM_24BIT_PACKED -> "24-bit PCM"
                        AudioFormat.ENCODING_PCM_32BIT -> "32-bit PCM"
                        else -> nerdStats?.bitDepth?.let { "$it-bit PCM" } ?: "16-bit PCM"
                    }
                    val dspRate = outputStatus.actualSampleRateHz ?: nerdStats?.sampleRateHz ?: 44100
                    val dspRateText = "$dspRate Hz"
                    val eqPresetText = if (eqEnabled) eqPreset else "Flat"
                    val stereoExpandText = if (spatialAudio) "Spatial 3D (250%)" else "Stereo (100%)"
                    val buffersText = outputStatus.bufferSize?.let { size ->
                        val rate = outputStatus.actualSampleRateHz ?: 44100
                        val bytesPerSample = when (outputStatus.actualEncoding) {
                            AudioFormat.ENCODING_PCM_FLOAT, AudioFormat.ENCODING_PCM_32BIT -> 4
                            AudioFormat.ENCODING_PCM_24BIT_PACKED -> 3
                            else -> 2
                        }
                        val channelCount = nerdStats?.channels ?: 2
                        val bytesPerFrame = bytesPerSample * channelCount
                        val frames = if (bytesPerFrame > 0) size / bytesPerFrame else 0
                        if (rate > 0 && frames > 0) {
                            val ms = (frames * 1000L) / rate
                            "2x (${ms}ms, $frames frames)"
                        } else {
                            "—"
                        }
                    } ?: "2x (500ms, 22050 frames)"

                    PipelineStage(
                        icon = Icons.Rounded.Tune,
                        iconTint = COLOR_STAGE_DSP,
                        stageNumber = "04",
                        title = stringResource(R.string.pipeline_dsp),
                        statusChip = if (eqEnabled) eqPresetText else "Flat DSP",
                        statusColor = COLOR_STAGE_DSP,
                        isLast = false,
                        nextStageColor = COLOR_STAGE_OUTPUT,
                    ) {
                        PipelineRow(stringResource(R.string.pipeline_pcm_format), pcmFormat, isMonospace = true)
                        PipelineRow(stringResource(R.string.pipeline_sample_rate), dspRateText, isMonospace = true)
                        PipelineRow(stringResource(R.string.pipeline_eq_preset), eqPresetText)
                        PipelineRow(stringResource(R.string.pipeline_stereo_expand), stereoExpandText)
                        PipelineRow(stringResource(R.string.pipeline_buffers), buffersText, isMonospace = true)
                        PipelineRow(stringResource(R.string.pipeline_output_api), outputStatus.sink.ifBlank { "AudioTrack" })
                    }

                    // ── 5. Output Device Stage ─────────────────────────────
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
                    val inDepth = when (outputStatus.actualEncoding) {
                        AudioFormat.ENCODING_PCM_FLOAT -> "32-bit"
                        AudioFormat.ENCODING_PCM_16BIT -> "16-bit"
                        else -> stats?.bitDepth?.let { "$it-bit" } ?: "16-bit"
                    }
                    val outDepth = if (outputStatus.floatFallback) "16-bit" else inDepth
                    val bitDepthOutputText = "In: $inDepth  •  Out: $outDepth"
                    val outputSampleRate = outputStatus.actualSampleRateHz ?: stats?.sampleRateHz ?: 44100
                    val outputSampleRateText = "$outputSampleRate Hz"

                    // Dynamically choose modern hardware device icon
                    val outputIcon = when {
                        outputStatus.isUsb -> Icons.Rounded.Usb
                        deviceName.contains("Bluetooth", ignoreCase = true) ||
                            deviceName.contains("Buds", ignoreCase = true) ||
                            deviceName.contains("Airdopes", ignoreCase = true) ||
                            deviceName.contains("Headset", ignoreCase = true) ||
                            deviceName.contains("Headphone", ignoreCase = true) ||
                            deviceName.contains("Wireless", ignoreCase = true) -> Icons.Rounded.Headphones
                        else -> Icons.Rounded.PhoneAndroid
                    }

                    PipelineStage(
                        icon = outputIcon,
                        iconTint = COLOR_STAGE_OUTPUT,
                        stageNumber = "05",
                        title = stringResource(R.string.pipeline_output_device),
                        statusChip = "Active Route",
                        statusColor = COLOR_STAGE_OUTPUT,
                        isLast = true,
                        nextStageColor = COLOR_STAGE_OUTPUT,
                    ) {
                        PipelineRow(stringResource(R.string.pipeline_device_name), deviceName)
                        PipelineRow(stringResource(R.string.pipeline_bit_depth), bitDepthOutputText, isMonospace = true)
                        PipelineRow(stringResource(R.string.pipeline_sample_rate), outputSampleRateText, isMonospace = true)
                    }

                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}
}

@Composable
private fun PipelineStage(
    icon: ImageVector,
    iconTint: Color,
    stageNumber: String,
    title: String,
    statusChip: String? = null,
    statusColor: Color? = null,
    isLast: Boolean = false,
    nextStageColor: Color = Color.White,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
    ) {
        // Left timeline column with glowing icon disc and connector
        Column(
            modifier = Modifier
                .width(36.dp)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Glowing circular icon disc
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp),
                )
            }

            // Vertical connecting line with gradient and micro arrow
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .width(2.dp)
                        .padding(vertical = 2.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    iconTint.copy(alpha = 0.40f),
                                    nextStageColor.copy(alpha = 0.40f),
                                ),
                            ),
                        ),
                )
                Icon(
                    imageVector = Icons.Rounded.ArrowDownward,
                    contentDescription = null,
                    tint = nextStageColor.copy(alpha = 0.45f),
                    modifier = Modifier.size(11.dp),
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // Right content column
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = if (isLast) 4.dp else 16.dp),
        ) {
            // Stage Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "STAGE $stageNumber",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp,
                        ),
                        color = iconTint,
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = Color.White,
                    )
                }

                if (statusChip != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background((statusColor ?: iconTint).copy(alpha = 0.14f))
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = statusChip,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = statusColor ?: iconTint,
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // Rounded Parameters Container Card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.04f),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun PipelineRow(
    label: String,
    value: String,
    isMonospace: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = Color.White.copy(alpha = 0.60f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
            ),
            color = Color.White.copy(alpha = 0.95f),
        )
    }
}
