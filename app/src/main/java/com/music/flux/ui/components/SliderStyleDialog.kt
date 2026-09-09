package com.music.flux.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.flux.R
import com.music.flux.data.settings.AppSettings
import com.music.flux.data.settings.SliderStyle
import com.music.flux.ui.player.ThinSlider

@Composable
fun SliderStyleDialog(
    onDismissRequest: () -> Unit,
) {
    val currentStyle by AppSettings.sliderStyle.collectAsStateWithLifecycle()
    val isSquiggly by AppSettings.squigglySlider.collectAsStateWithLifecycle()

    val sliderColors = SliderDefaults.colors(
        activeTrackColor = MaterialTheme.colorScheme.primary,
        inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        thumbColor = MaterialTheme.colorScheme.primary,
    )

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = stringResource(R.string.player_slider_style),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // 1. Capsule (Default / Apple Music Style)
                val isCapsuleSelected = (currentStyle == SliderStyle.DEFAULT || currentStyle == SliderStyle.CAPSULE) && !isSquiggly
                SliderOptionCard(
                    title = stringResource(R.string.capsule),
                    subtitle = "Apple-style modern expanding bar",
                    isSelected = isCapsuleSelected,
                    onClick = {
                        AppSettings.setSliderStyle(SliderStyle.DEFAULT)
                        AppSettings.setSquigglySlider(false)
                        onDismissRequest()
                    },
                ) {
                    ThinSlider(
                        value = 0.55f,
                        onValueChange = {},
                        idleHeight = 6.dp,
                        activeHeight = 10.dp,
                        activeColor = MaterialTheme.colorScheme.primary,
                        inactiveColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // 2. Material Slider (Classic with thumb knob)
                val isMaterialSelected = currentStyle == SliderStyle.MATERIAL && !isSquiggly
                SliderOptionCard(
                    title = stringResource(R.string.material),
                    subtitle = "Classic Material 3 slider with thumb knob",
                    isSelected = isMaterialSelected,
                    onClick = {
                        AppSettings.setSliderStyle(SliderStyle.MATERIAL)
                        AppSettings.setSquigglySlider(false)
                        onDismissRequest()
                    },
                ) {
                    Slider(
                        value = 0.5f,
                        onValueChange = {},
                        colors = sliderColors,
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // 3. Wavy Slider
                val isWavySelected = currentStyle == SliderStyle.WAVY && !isSquiggly
                SliderOptionCard(
                    title = stringResource(R.string.wavy),
                    subtitle = "Fluid animated harmonic wave",
                    isSelected = isWavySelected,
                    onClick = {
                        AppSettings.setSliderStyle(SliderStyle.WAVY)
                        AppSettings.setSquigglySlider(false)
                        onDismissRequest()
                    },
                ) {
                    WavySlider(
                        value = 0.5f,
                        onValueChange = {},
                        colors = sliderColors,
                        isPlaying = true,
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // 4. Squiggly Slider
                val isSquigglySelected = currentStyle == SliderStyle.SQUIGGLY || (currentStyle == SliderStyle.WAVY && isSquiggly)
                SliderOptionCard(
                    title = stringResource(R.string.squiggly),
                    subtitle = "Dynamic animated waveform rhythm",
                    isSelected = isSquigglySelected,
                    onClick = {
                        AppSettings.setSliderStyle(SliderStyle.SQUIGGLY)
                        AppSettings.setSquigglySlider(true)
                        onDismissRequest()
                    },
                ) {
                    SquigglySlider(
                        value = 0.55f,
                        onValueChange = {},
                        colors = sliderColors,
                        isPlaying = true,
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // 5. Slim Slider
                val isSlimSelected = currentStyle == SliderStyle.SLIM && !isSquiggly
                SliderOptionCard(
                    title = stringResource(R.string.slim),
                    subtitle = "Minimalist hairline scrubber (2dp)",
                    isSelected = isSlimSelected,
                    onClick = {
                        AppSettings.setSliderStyle(SliderStyle.SLIM)
                        AppSettings.setSquigglySlider(false)
                        onDismissRequest()
                    },
                ) {
                    ThinSlider(
                        value = 0.65f,
                        onValueChange = {},
                        idleHeight = 2.dp,
                        activeHeight = 4.dp,
                        activeColor = MaterialTheme.colorScheme.primary,
                        inactiveColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun SliderOptionCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    previewContent: @Composable () -> Unit,
) {
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    }
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundColor)
            .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 15.sp,
                    ),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
        ) {
            previewContent()
        }
    }
}
