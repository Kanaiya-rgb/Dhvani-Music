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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
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
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // 1. Capsule (Default / Apple Music Style)
                item {
                    val isCapsuleSelected = (currentStyle == SliderStyle.DEFAULT || currentStyle == SliderStyle.CAPSULE) && !isSquiggly
                    SliderOptionCard(
                        title = stringResource(R.string.capsule),
                        subtitle = "Apple-style bar",
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
                            idleHeight = 5.dp,
                            activeHeight = 9.dp,
                            activeColor = MaterialTheme.colorScheme.primary,
                            inactiveColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                // 2. Material Slider (Classic with thumb knob)
                item {
                    val isMaterialSelected = currentStyle == SliderStyle.MATERIAL && !isSquiggly
                    SliderOptionCard(
                        title = stringResource(R.string.material),
                        subtitle = "Classic M3 knob",
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
                }

                // 3. Wavy Slider
                item {
                    val isWavySelected = currentStyle == SliderStyle.WAVY && !isSquiggly
                    SliderOptionCard(
                        title = stringResource(R.string.wavy),
                        subtitle = "Harmonic wave",
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
                }

                // 4. Squiggly Slider
                item {
                    val isSquigglySelected = currentStyle == SliderStyle.SQUIGGLY || (currentStyle == SliderStyle.WAVY && isSquiggly)
                    SliderOptionCard(
                        title = stringResource(R.string.squiggly),
                        subtitle = "Animated waveform",
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
                }

                // 5. Slim Slider
                item {
                    val isSlimSelected = currentStyle == SliderStyle.SLIM && !isSquiggly
                    SliderOptionCard(
                        title = stringResource(R.string.slim),
                        subtitle = "Hairline scrubber",
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

                // 6. Neon Glow Slider
                item {
                    val isNeonSelected = currentStyle == SliderStyle.NEON_GLOW
                    SliderOptionCard(
                        title = stringResource(R.string.neon_glow),
                        subtitle = "Cyberpunk bloom",
                        isSelected = isNeonSelected,
                        onClick = {
                            AppSettings.setSliderStyle(SliderStyle.NEON_GLOW)
                            AppSettings.setSquigglySlider(false)
                            onDismissRequest()
                        },
                    ) {
                        NeonGlowSlider(
                            value = 0.6f,
                            onValueChange = {},
                            colors = sliderColors,
                            isPlaying = true,
                            enabled = false,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                // 7. Gradient Flow Slider
                item {
                    val isFlowSelected = currentStyle == SliderStyle.GRADIENT_FLOW
                    SliderOptionCard(
                        title = stringResource(R.string.gradient_flow),
                        subtitle = "Aurora liquid",
                        isSelected = isFlowSelected,
                        onClick = {
                            AppSettings.setSliderStyle(SliderStyle.GRADIENT_FLOW)
                            AppSettings.setSquigglySlider(false)
                            onDismissRequest()
                        },
                    ) {
                        GradientFlowSlider(
                            value = 0.58f,
                            onValueChange = {},
                            colors = sliderColors,
                            isPlaying = true,
                            enabled = false,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                // 8. Cosmic Stars Slider
                item {
                    val isCosmicSelected = currentStyle == SliderStyle.COSMIC
                    SliderOptionCard(
                        title = stringResource(R.string.particles),
                        subtitle = "Galaxy stars",
                        isSelected = isCosmicSelected,
                        onClick = {
                            AppSettings.setSliderStyle(SliderStyle.COSMIC)
                            AppSettings.setSquigglySlider(false)
                            onDismissRequest()
                        },
                    ) {
                        CosmicSlider(
                            value = 0.62f,
                            onValueChange = {},
                            colors = sliderColors,
                            isPlaying = true,
                            enabled = false,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                // 9. Liquid Lava Slider
                item {
                    val isLavaSelected = currentStyle == SliderStyle.LIQUID_LAVA
                    SliderOptionCard(
                        title = stringResource(R.string.liquid_lava),
                        subtitle = "Morphing droplets",
                        isSelected = isLavaSelected,
                        onClick = {
                            AppSettings.setSliderStyle(SliderStyle.LIQUID_LAVA)
                            AppSettings.setSquigglySlider(false)
                            onDismissRequest()
                        },
                    ) {
                        LiquidLavaSlider(
                            value = 0.55f,
                            onValueChange = {},
                            colors = sliderColors,
                            isPlaying = true,
                            enabled = false,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                // 10. Audio Bars Visualizer Slider
                item {
                    val isBarsSelected = currentStyle == SliderStyle.AUDIO_BARS
                    SliderOptionCard(
                        title = stringResource(R.string.audio_bars),
                        subtitle = "Dancing equalizer",
                        isSelected = isBarsSelected,
                        onClick = {
                            AppSettings.setSliderStyle(SliderStyle.AUDIO_BARS)
                            AppSettings.setSquigglySlider(false)
                            onDismissRequest()
                        },
                    ) {
                        AudioBarsSlider(
                            value = 0.52f,
                            onValueChange = {},
                            colors = sliderColors,
                            isPlaying = true,
                            enabled = false,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                // 11. Retro LED Matrix Slider
                item {
                    val isRetroSelected = currentStyle == SliderStyle.RETRO_LED
                    SliderOptionCard(
                        title = stringResource(R.string.retro_led),
                        subtitle = "Vintage Hi-Fi VU",
                        isSelected = isRetroSelected,
                        onClick = {
                            AppSettings.setSliderStyle(SliderStyle.RETRO_LED)
                            AppSettings.setSquigglySlider(false)
                            onDismissRequest()
                        },
                    ) {
                        RetroDotMatrixSlider(
                            value = 0.58f,
                            onValueChange = {},
                            colors = sliderColors,
                            isPlaying = true,
                            enabled = false,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                // 12. Vinyl Groove Slider
                item {
                    val isVinylSelected = currentStyle == SliderStyle.VINYL_GROOVE
                    SliderOptionCard(
                        title = stringResource(R.string.vinyl_groove),
                        subtitle = "Turntable needle",
                        isSelected = isVinylSelected,
                        onClick = {
                            AppSettings.setSliderStyle(SliderStyle.VINYL_GROOVE)
                            AppSettings.setSquigglySlider(false)
                            onDismissRequest()
                        },
                    ) {
                        VinylGrooveSlider(
                            value = 0.6f,
                            onValueChange = {},
                            colors = sliderColors,
                            isPlaying = true,
                            enabled = false,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                // 13. Cyber Beam Laser Slider
                item {
                    val isCyberSelected = currentStyle == SliderStyle.CYBER_BEAM
                    SliderOptionCard(
                        title = stringResource(R.string.cyber_pulse),
                        subtitle = "Sci-Fi laser rail",
                        isSelected = isCyberSelected,
                        onClick = {
                            AppSettings.setSliderStyle(SliderStyle.CYBER_BEAM)
                            AppSettings.setSquigglySlider(false)
                            onDismissRequest()
                        },
                    ) {
                        CyberBeamSlider(
                            value = 0.65f,
                            onValueChange = {},
                            colors = sliderColors,
                            isPlaying = true,
                            enabled = false,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
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
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 13.sp,
                    ),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
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
                .height(40.dp)
                .padding(vertical = 2.dp),
            contentAlignment = Alignment.Center,
        ) {
            previewContent()
        }
    }
}
