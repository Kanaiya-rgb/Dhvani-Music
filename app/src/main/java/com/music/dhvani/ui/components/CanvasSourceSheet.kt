package com.music.dhvani.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.SlowMotionVideo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.music.dhvani.data.canvas.CanvasArtwork
import com.music.dhvani.data.canvas.CanvasSource
import androidx.compose.foundation.border
import com.music.dhvani.ui.haptics.Haptic
import com.music.dhvani.ui.haptics.rememberHaptics

private val COLOR_SPOTIFY = Color(0xFF1DB954)
private val COLOR_APPLE = Color(0xFFFA2D48)
private val COLOR_TIDAL = Color(0xFF00E5FF)
private val COLOR_COMMUNITY = Color(0xFFA78BFA)

/**
 * Bottom dialog sheet allowing the user to view and switch between available
 * video canvas sources (Spotify, Apple Music, Tidal, Community).
 */
@Composable
fun CanvasSourceSheet(
    availableSources: Map<CanvasSource, CanvasArtwork>,
    currentSource: CanvasSource?,
    onSelectSource: (CanvasArtwork) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()

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
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = Color(0xFF14171C),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                modifier = modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .clickable(enabled = false) {}, // Intercept clicks inside card
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 20.dp),
                ) {
                    // Handle bar
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(38.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.25f)),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Title row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = "Video Canvas Source",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                            Text(
                                text = "Choose visual motion loop for this track",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.65f),
                            )
                        }

                        IconButton(
                            onClick = {
                                haptics.play(Haptic.Tap)
                                onDismiss()
                            },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Sources list
                    val allSources = listOf(
                        CanvasSource.SPOTIFY to COLOR_SPOTIFY,
                        CanvasSource.APPLE_MUSIC to COLOR_APPLE,
                        CanvasSource.TIDAL to COLOR_TIDAL,
                        CanvasSource.COMMUNITY to COLOR_COMMUNITY,
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        for ((source, brandColor) in allSources) {
                            val artwork = availableSources[source]
                            val isAvailable = artwork != null
                            val isSelected = currentSource == source && isAvailable

                            val itemBg = if (isSelected) {
                                brandColor.copy(alpha = 0.16f)
                            } else if (isAvailable) {
                                Color.White.copy(alpha = 0.06f)
                            } else {
                                Color.White.copy(alpha = 0.02f)
                            }

                            val borderStroke = if (isSelected) {
                                BorderStroke(1.2.dp, brandColor.copy(alpha = 0.85f))
                            } else if (isAvailable) {
                                BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
                            } else {
                                BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(itemBg)
                                    .border(borderStroke, RoundedCornerShape(16.dp))
                                    .clickable(enabled = isAvailable) {
                                        if (artwork != null) {
                                            haptics.play(Haptic.Select)
                                            onSelectSource(artwork)
                                            onDismiss()
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isAvailable) brandColor.copy(alpha = 0.22f)
                                                else Color.White.copy(alpha = 0.05f)
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.SlowMotionVideo,
                                            contentDescription = source.displayName,
                                            tint = if (isAvailable) brandColor else Color.White.copy(alpha = 0.3f),
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = source.displayName,
                                            fontSize = 15.sp,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                            color = if (isAvailable) Color.White else Color.White.copy(alpha = 0.35f),
                                        )
                                        Text(
                                            text = when {
                                                isSelected -> "Playing Now"
                                                isAvailable -> "Available"
                                                else -> "Not available for this track"
                                            },
                                            fontSize = 12.sp,
                                            color = when {
                                                isSelected -> brandColor
                                                isAvailable -> Color.White.copy(alpha = 0.55f)
                                                else -> Color.White.copy(alpha = 0.25f)
                                            },
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(brandColor),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = "Selected",
                                            tint = Color.Black,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}
