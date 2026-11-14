package com.music.dhvani.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import com.music.dhvani.ui.icons.DhvaniIcons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.dhvani.data.model.Song
import com.music.dhvani.data.settings.AppSettings
import com.music.dhvani.data.settings.AudioPreset
import com.music.dhvani.ui.haptics.Haptic
import com.music.dhvani.ui.haptics.rememberHaptics
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioEffectsSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    song: Song? = null,
) {
    val haptics = rememberHaptics()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val currentPreset by AppSettings.audioPreset.collectAsStateWithLifecycle()
    val speed by AppSettings.playbackSpeed.collectAsStateWithLifecycle()
    val pitch by AppSettings.playbackPitch.collectAsStateWithLifecycle()
    val reverbDepth by AppSettings.reverbDepth.collectAsStateWithLifecycle()
    val lofiWarmth by AppSettings.lofiWarmth.collectAsStateWithLifecycle()

    var linkSpeedAndPitch by remember { mutableStateOf(true) }

    // Dynamic cover art sampling for ambient sheet background
    val meshColors = rememberArtworkColors(song?.thumbnailUrl, null)
    val dominantColor = remember(meshColors) {
        val first = meshColors.colors.firstOrNull() ?: Color(0xFF14151B)
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(first.toArgb(), hsl)
        hsl[1] = (hsl[1] * 1.25f).coerceIn(0.18f, 0.85f)
        hsl[2] = 0.10f
        Color(ColorUtils.HSLToColor(hsl))
    }
    val sheetBottomColor = remember(dominantColor) {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(dominantColor.toArgb(), hsl)
        hsl[2] = 0.055f
        Color(ColorUtils.HSLToColor(hsl))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
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
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
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

            // Header: Icon + "Slowed & Nightcore" title + Reset & Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                            .border(1.dp, Color.White.copy(alpha = 0.20f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = DhvaniIcons.SpeedDial,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Column {
                        Text(
                            text = "Slowed & Nightcore",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                            ),
                            color = Color.White,
                        )
                        Text(
                            text = "Slowed, Nightcore & Spatial Reverb",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = Color.White.copy(alpha = 0.65f),
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    IconButton(
                        onClick = {
                            haptics.play(Haptic.Tap)
                            AppSettings.setAudioPreset(AudioPreset.NORMAL)
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RestartAlt,
                            contentDescription = "Reset to normal",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    IconButton(
                        onClick = {
                            haptics.play(Haptic.Tap)
                            onDismiss()
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Presets Section Header
            Text(
                text = "PRESETS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.2.sp,
                ),
                color = Color.White.copy(alpha = 0.70f),
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Presets Scrollable Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PresetPill(
                    label = "Normal",
                    subtitle = "1.0x Standard",
                    isSelected = currentPreset == AudioPreset.NORMAL,
                    onClick = {
                        haptics.play(Haptic.Tap)
                        AppSettings.setAudioPreset(AudioPreset.NORMAL)
                    },
                )
                PresetPill(
                    label = "Slowed 🌙",
                    subtitle = "0.85x + Reverb",
                    isSelected = currentPreset == AudioPreset.SLOWED_REVERB,
                    onClick = {
                        haptics.play(Haptic.Tap)
                        AppSettings.setAudioPreset(AudioPreset.SLOWED_REVERB)
                    },
                )
                PresetPill(
                    label = "Rainy Lo-Fi 🌧️",
                    subtitle = "Muffled Warmth",
                    isSelected = currentPreset == AudioPreset.RAINY_LOFI,
                    onClick = {
                        haptics.play(Haptic.Tap)
                        AppSettings.setAudioPreset(AudioPreset.RAINY_LOFI)
                    },
                )
                PresetPill(
                    label = "Nightcore ⚡",
                    subtitle = "1.25x Sped Up",
                    isSelected = currentPreset == AudioPreset.NIGHTCORE,
                    onClick = {
                        haptics.play(Haptic.Tap)
                        AppSettings.setAudioPreset(AudioPreset.NIGHTCORE)
                    },
                )
                PresetPill(
                    label = "Custom 🎧",
                    subtitle = "Fine Tune",
                    isSelected = currentPreset == AudioPreset.CUSTOM,
                    onClick = {
                        haptics.play(Haptic.Tap)
                        AppSettings.setAudioPreset(AudioPreset.CUSTOM)
                    },
                )
            }

            Spacer(modifier = Modifier.height(26.dp))

            // Sliders Section Header
            Text(
                text = "FINE-TUNING",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.2.sp,
                ),
                color = Color.White.copy(alpha = 0.70f),
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Speed Slider
            EffectSlider(
                title = "Playback Speed",
                valueText = String.format(Locale.ROOT, "%.2fx", speed),
                value = speed,
                valueRange = 0.50f..2.00f,
                onValueChange = { newSpeed ->
                    val rounded = (newSpeed * 20f).roundToInt() / 20f
                    AppSettings.setPlaybackSpeed(rounded)
                    if (linkSpeedAndPitch) {
                        AppSettings.setPlaybackPitch(rounded)
                    }
                    if (currentPreset != AudioPreset.CUSTOM) {
                        AppSettings.setAudioPreset(AudioPreset.CUSTOM)
                    }
                },
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Pitch Slider
            EffectSlider(
                title = "Pitch Scaling",
                valueText = String.format(Locale.ROOT, "%.2fx", pitch),
                value = pitch,
                valueRange = 0.50f..1.50f,
                onValueChange = { newPitch ->
                    val rounded = (newPitch * 20f).roundToInt() / 20f
                    linkSpeedAndPitch = false
                    AppSettings.setPlaybackPitch(rounded)
                    if (currentPreset != AudioPreset.CUSTOM) {
                        AppSettings.setAudioPreset(AudioPreset.CUSTOM)
                    }
                },
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Reverb Depth Slider
            EffectSlider(
                title = "Reverb Space (Atmosphere)",
                valueText = "${(reverbDepth * 100f).roundToInt()}%",
                value = reverbDepth,
                valueRange = 0.0f..1.0f,
                onValueChange = { newDepth ->
                    val rounded = (newDepth * 100f).roundToInt() / 100f
                    AppSettings.setReverbDepth(rounded)
                    if (currentPreset != AudioPreset.CUSTOM) {
                        AppSettings.setAudioPreset(AudioPreset.CUSTOM)
                    }
                },
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Lo-Fi Warmth (Low-pass muffle filter)
            EffectSlider(
                title = "Lo-Fi Warmth (Muffled Next-Door Vibe)",
                valueText = if (lofiWarmth <= 0.01f) "Off" else "${(lofiWarmth * 100f).roundToInt()}%",
                value = lofiWarmth,
                valueRange = 0.0f..0.85f,
                onValueChange = { newWarmth ->
                    val rounded = (newWarmth * 100f).roundToInt() / 100f
                    AppSettings.setLofiWarmth(rounded)
                    if (currentPreset != AudioPreset.CUSTOM) {
                        AppSettings.setAudioPreset(AudioPreset.CUSTOM)
                    }
                },
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun PresetPill(
    label: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f),
        label = "preset_bg",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) Color.White.copy(alpha = 0.70f) else Color.White.copy(alpha = 0.14f),
        label = "preset_border",
    )

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.sp,
                ),
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                color = Color.White.copy(alpha = if (isSelected) 0.85f else 0.55f),
            )
        }
    }
}

@Composable
private fun EffectSlider(
    title: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                ),
                color = Color.White.copy(alpha = 0.90f),
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                ),
                color = Color.White,
            )
        }

        Spacer(Modifier.height(4.dp))

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.22f),
            ),
        )
    }
}
