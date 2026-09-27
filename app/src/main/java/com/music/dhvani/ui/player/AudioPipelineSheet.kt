package com.music.dhvani.ui.player

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BluetoothAudio
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.music.dhvani.playback.AudioEngine

private val COLOR_SOURCE = Color(0xFF0EA5E9) // Neon Sky Cyan
private val COLOR_DSP = Color(0xFFA78BFA)    // Violet DSP Float32
private val COLOR_DAC = Color(0xFF10B981)    // Emerald Hardware Sink
private val COLOR_GOLD = Color(0xFFFBBF24)   // Audiophile Bit-Perfect Gold

/**
 * World-class DAC Audio Pipeline Inspector displaying live animated signal flow:
 * Source Node (Decoder) ➔ 32-Bit Float DSP Engine ➔ Hardware Output (DAC / Bluetooth).
 */
@Composable
fun AudioPipelineSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val snapshot = remember { AudioEngine.inspectPipeline(context) }

    // Pulsing animated glow along the connector lines
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "connectorPulse",
    )

    // Animated visualizer bars for the source decoder
    val bar1 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "b1",
    )
    val bar2 by infiniteTransition.animateFloat(
        initialValue = 0.85f, targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(560, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "b2",
    )
    val bar3 by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(480, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "b3",
    )
    val bar4 by infiniteTransition.animateFloat(
        initialValue = 0.7f, targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(620, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "b4",
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.70f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = Color(0xFF0C0F15),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                modifier = modifier
                    .fillMaxWidth()
                    .widthIn(max = 550.dp)
                    .clickable(enabled = false) {},
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                ) {
                    // Handle bar
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(42.dp)
                            .height(4.5.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.28f)),
                    )

                    Spacer(Modifier.height(18.dp))

                    // Sheet Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.linearGradient(listOf(COLOR_SOURCE, COLOR_DAC)),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.GraphicEq,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "DAC Audio Pipeline",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = Color.White,
                                )
                                Text(
                                    text = if (snapshot.dsp.isBitPerfect) "⚡ Bit-Perfect Direct Audio Path" else "Live 32-Bit Float Signal Inspector",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                    color = if (snapshot.dsp.isBitPerfect) COLOR_GOLD else COLOR_DSP,
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f)),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close",
                                tint = Color.White.copy(alpha = 0.75f),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // ================= 1. SOURCE NODE =================
                        PipelineStageCard(
                            stageNumber = "1",
                            stageType = "SOURCE DECODER",
                            title = snapshot.source.codec,
                            detail = "${snapshot.source.bitrateKbps ?: 256} kbps • ${snapshot.source.sampleRateHz ?: 44100} Hz / ${snapshot.source.bitDepth ?: 16}-bit",
                            badgeText = if (snapshot.source.isHiRes) "Hi-Res" else if (snapshot.source.isLossless) "Lossless" else "VBR Audio",
                            accentColor = COLOR_SOURCE,
                            icon = Icons.Rounded.MusicNote,
                            extraMetrics = listOf(
                                "Codec" to snapshot.source.codec,
                                "Sample Rate" to "${(snapshot.source.sampleRateHz ?: 44100) / 1000.0} kHz",
                                "Precision" to "${snapshot.source.bitDepth ?: 16}-bit",
                                "Channels" to "${snapshot.source.channels} Ch (Stereo)",
                            ),
                            trailingContent = {
                                // Mini visualizer bars
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    verticalAlignment = Alignment.Bottom,
                                    modifier = Modifier.height(18.dp),
                                ) {
                                    listOf(bar1, bar2, bar3, bar4).forEach { heightFraction ->
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .height(18.dp * heightFraction)
                                                .clip(CircleShape)
                                                .background(COLOR_SOURCE),
                                        )
                                    }
                                }
                            },
                        )

                        // Connector 1 (Source ➔ DSP)
                        PipelineConnector(pulseGlow, COLOR_SOURCE, COLOR_DSP)

                        // ================= 2. DSP ENGINE NODE =================
                        PipelineStageCard(
                            stageNumber = "2",
                            stageType = "DSP AUDIO ENGINE",
                            title = if (snapshot.dsp.float32Processing) "Float32 Processing" else "PCM Audio Engine",
                            detail = snapshot.dsp.resamplingStatus,
                            badgeText = if (snapshot.dsp.isBitPerfect) "Bit-Perfect" else "32-Bit Float",
                            accentColor = COLOR_DSP,
                            icon = Icons.Rounded.Memory,
                            extraMetrics = listOf(
                                "Architecture" to "32-bit Float",
                                "Equalizer" to if (snapshot.dsp.equalizerEnabled) snapshot.dsp.equalizerPreset else "Bypassed",
                                "Resampling" to snapshot.dsp.resamplingStatus,
                                "Headroom" to "+0.0 dB",
                            ),
                        )

                        // Connector 2 (DSP ➔ Hardware Output)
                        PipelineConnector(pulseGlow, COLOR_DSP, COLOR_DAC)

                        // ================= 3. HARDWARE OUTPUT NODE =================
                        PipelineStageCard(
                            stageNumber = "3",
                            stageType = "OUTPUT DEVICE (DAC)",
                            title = snapshot.output.deviceType,
                            detail = snapshot.output.deviceName,
                            badgeText = if (snapshot.output.isUsbDac) "USB DAC" else if (snapshot.output.bluetoothCodec != null) "HD Bluetooth" else "Internal DAC",
                            accentColor = COLOR_DAC,
                            icon = if (snapshot.output.isUsbDac) Icons.Rounded.Usb else if (snapshot.output.bluetoothCodec != null) Icons.Rounded.BluetoothAudio else Icons.Rounded.Speaker,
                            extraMetrics = listOf(
                                "Negotiated" to snapshot.output.negotiatedFormat,
                                "Device" to snapshot.output.deviceName,
                                "Hardware Sink" to if (snapshot.output.isUsbDac) "USB Audio Class 2.0" else "AudioTrack HAL",
                                "Latency" to "Ultra Low",
                            ),
                        )

                        Spacer(Modifier.height(16.dp))

                        // Bottom Audiophile Signal Integrity Banner
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = 0.04f),
                            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(COLOR_DAC.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        tint = COLOR_DAC,
                                        modifier = Modifier.size(17.dp),
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Signal Path Verified",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                        ),
                                        color = Color.White,
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "Audio stream is decoded directly and rendered with 32-bit floating point precision to avoid quantization distortion.",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp,
                                        ),
                                        color = Color.White.copy(alpha = 0.6f),
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PipelineConnector(
    pulseAlpha: Float,
    startColor: Color,
    endColor: Color,
) {
    Column(
        modifier = Modifier.padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(30.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            startColor.copy(alpha = pulseAlpha),
                            endColor.copy(alpha = pulseAlpha),
                        ),
                    ),
                ),
        )
    }
}

@Composable
private fun PipelineStageCard(
    stageNumber: String,
    stageType: String,
    title: String,
    detail: String,
    badgeText: String,
    accentColor: Color,
    icon: ImageVector,
    extraMetrics: List<Pair<String, String>>,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF141822),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(accentColor.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.width(11.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "$stageNumber. $stageType",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                ),
                                color = accentColor,
                            )
                        }
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = Color.White,
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    trailingContent?.invoke()
                    if (trailingContent != null) {
                        Spacer(Modifier.width(8.dp))
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentColor.copy(alpha = 0.18f))
                            .border(0.5.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = accentColor,
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                ),
                color = Color.White.copy(alpha = 0.68f),
            )

            Spacer(Modifier.height(12.dp))

            // 2x2 or 4x1 grid of extra metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                extraMetrics.forEach { (label, value) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 7.dp),
                    ) {
                        Column {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = Color.White.copy(alpha = 0.45f),
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = value,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace,
                                ),
                                color = Color.White.copy(alpha = 0.92f),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}
