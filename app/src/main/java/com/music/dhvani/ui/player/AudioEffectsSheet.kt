package com.music.dhvani.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.dhvani.data.settings.AppSettings
import com.music.dhvani.data.settings.AudioPreset
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioEffectsSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val currentPreset by AppSettings.audioPreset.collectAsStateWithLifecycle()
    val speed by AppSettings.playbackSpeed.collectAsStateWithLifecycle()
    val pitch by AppSettings.playbackPitch.collectAsStateWithLifecycle()
    val reverbDepth by AppSettings.reverbDepth.collectAsStateWithLifecycle()
    val lofiWarmth by AppSettings.lofiWarmth.collectAsStateWithLifecycle()

    var linkSpeedAndPitch by remember { mutableStateOf(true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Column {
                        Text(
                            text = "Audio Vibes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Slowed, Nightcore & Spatial Reverb",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { AppSettings.setAudioPreset(AudioPreset.NORMAL) },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RestartAlt,
                            contentDescription = "Reset to normal",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Preset Pills
            Text(
                text = "PRESETS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
            )

            Spacer(modifier = Modifier.height(10.dp))

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
                    onClick = { AppSettings.setAudioPreset(AudioPreset.NORMAL) },
                )
                PresetPill(
                    label = "Slowed 🌙",
                    subtitle = "0.85x + Reverb",
                    isSelected = currentPreset == AudioPreset.SLOWED_REVERB,
                    onClick = { AppSettings.setAudioPreset(AudioPreset.SLOWED_REVERB) },
                )
                PresetPill(
                    label = "Rainy Lo-Fi 🌧️",
                    subtitle = "Muffled Warmth",
                    isSelected = currentPreset == AudioPreset.RAINY_LOFI,
                    onClick = { AppSettings.setAudioPreset(AudioPreset.RAINY_LOFI) },
                )
                PresetPill(
                    label = "Nightcore ⚡",
                    subtitle = "1.25x Sped Up",
                    isSelected = currentPreset == AudioPreset.NIGHTCORE,
                    onClick = { AppSettings.setAudioPreset(AudioPreset.NIGHTCORE) },
                )
                PresetPill(
                    label = "Custom 🎧",
                    subtitle = "Fine Tune",
                    isSelected = currentPreset == AudioPreset.CUSTOM,
                    onClick = { AppSettings.setAudioPreset(AudioPreset.CUSTOM) },
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Sliders Section
            Text(
                text = "FINE-TUNING",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
            )

            Spacer(modifier = Modifier.height(16.dp))

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

            Spacer(modifier = Modifier.height(16.dp))

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

            Spacer(modifier = Modifier.height(16.dp))

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

            Spacer(modifier = Modifier.height(16.dp))

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

            Spacer(modifier = Modifier.height(20.dp))
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
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        label = "preset_bg",
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        label = "preset_content",
    )

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        } else null,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.7f),
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
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
            ),
        )
    }
}
