package com.music.dhvani.ui.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.dhvani.data.model.Song
import com.music.dhvani.data.settings.AppSettings
import com.music.dhvani.ui.haptics.Haptic
import com.music.dhvani.ui.haptics.rememberHaptics
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Clean, minimalistic Lyrics Offset calibration sheet.
 * Matches Apple Music / Dhvani design: large centered offset readout,
 * minus/plus steppers, and single-tap reset.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsOffsetSheet(
    song: Song,
    currentOffsetMs: Long,
    onOffsetChanged: (Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    var sliderSec by remember(currentOffsetMs) {
        mutableFloatStateOf((currentOffsetMs / 1000f).coerceIn(-5f, 5f))
    }

    // Dynamic background adapted from song cover artwork
    val meshColors = rememberArtworkColors(song.thumbnailUrl, null)
    val dominantColor = remember(meshColors) {
        val first = meshColors.colors.firstOrNull() ?: Color(0xFF14151B)
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(first.toArgb(), hsl)
        // Tune to deep, rich ambient velvet backdrop matching player
        hsl[1] = (hsl[1] * 1.25f).coerceIn(0.18f, 0.85f)
        hsl[2] = 0.11f
        Color(ColorUtils.HSLToColor(hsl))
    }
    val sheetBottomColor = remember(dominantColor) {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(dominantColor.toArgb(), hsl)
        hsl[2] = 0.055f
        Color(ColorUtils.HSLToColor(hsl))
    }

    val updateOffset = { sec: Float ->
        val snappedSec = (sec * 10f).roundToInt() / 10f
        sliderSec = snappedSec
        val ms = (snappedSec * 1000f).toLong()
        onOffsetChanged(ms)
        AppSettings.setLyricsOffset(song.videoId, ms)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = dominantColor,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            dominantColor,
                            sheetBottomColor,
                        )
                    )
                )
                .padding(horizontal = 24.dp)
                .padding(top = 12.dp, bottom = 28.dp)
                .navigationBarsPadding(),
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.30f)),
                )
            }

            // Title & Subtitle
            Text(
                text = "Lyrics offset",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                ),
                color = Color.White,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Positive values show lyrics later",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = Color.White.copy(alpha = 0.60f),
            )

            Spacer(Modifier.height(36.dp))

            // Large Hero Readout: e.g. "-5.0 s" or "+1.2 s" or "0.0 s"
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                val displaySec = sliderSec
                val formatted = String.format(Locale.ROOT, "%.1f", displaySec)
                val text = if (displaySec > 0f) "+$formatted s" else "$formatted s"
                Text(
                    text = text,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                    ),
                    color = Color.White,
                )
            }

            Spacer(Modifier.height(32.dp))

            // Stepper controls + Slider Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                // Minus stepper [ – ]
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable {
                            haptics.play(Haptic.Tap)
                            val next = (sliderSec - 0.1f).coerceIn(-5f, 5f)
                            updateOffset(next)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Remove,
                        contentDescription = "Decrease offset",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }

                // Slider (-5.0s to +5.0s)
                Slider(
                    value = sliderSec,
                    onValueChange = { sec ->
                        updateOffset(sec)
                    },
                    valueRange = -5f..5f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.22f),
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 14.dp),
                )

                // Plus stepper [ + ]
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable {
                            haptics.play(Haptic.Tap)
                            val next = (sliderSec + 0.1f).coerceIn(-5f, 5f)
                            updateOffset(next)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Increase offset",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            // Quick increment/decrement jump pills row (matching preset steppers: -0.1s, -0.5s, +0.5s, +1.0s, +0.1s)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val presets = listOf(
                    "-0.1s" to -0.1f,
                    "-0.5s" to -0.5f,
                    "+0.5s" to 0.5f,
                    "+1.0s" to 1.0f,
                    "+0.1s" to 0.1f,
                )
                presets.forEach { (label, deltaSec) ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.10f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                haptics.play(Haptic.Tap)
                                val next = (sliderSec + deltaSec).coerceIn(-5f, 5f)
                                updateOffset(next)
                            },
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                ),
                                color = Color.White,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Reset Button
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Reset",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                    ),
                    color = Color.White.copy(alpha = if (sliderSec != 0f) 0.90f else 0.40f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = sliderSec != 0f) {
                            haptics.play(Haptic.Tap)
                            updateOffset(0f)
                        }
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                )
            }
        }
    }
}

