package com.music.dhvani.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.dhvani.R
import com.music.dhvani.data.canvas.CanvasArtwork
import com.music.dhvani.data.canvas.CanvasSource
import com.music.dhvani.ui.haptics.Haptic
import com.music.dhvani.ui.haptics.rememberHaptics

private val COLOR_SPOTIFY = Color(0xFF1DB954)
private val COLOR_APPLE = Color(0xFFFA2D48)
private val COLOR_TIDAL = Color(0xFF00E5FF)
private val COLOR_COMMUNITY = Color(0xFFA78BFA)

/**
 * Bottom sheet allowing the user to view and switch between available
 * video canvas sources (Canvas S, Canvas A, Canvas T, Canvas Community).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanvasSourceSheet(
    availableSources: Map<CanvasSource, CanvasArtwork>,
    currentSource: CanvasSource?,
    onSelectSource: (CanvasArtwork) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = Color(0xFF14171C),
        dragHandle = null,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(top = 16.dp, bottom = 28.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
        ) {
            // Drag handle
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
                                        if (isSelected) brandColor.copy(alpha = 0.20f)
                                        else if (isAvailable) brandColor.copy(alpha = 0.10f)
                                        else Color.White.copy(alpha = 0.05f)
                                    )
                                    .alpha(if (isAvailable) 1f else 0.42f),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_canvas_motion),
                                    contentDescription = source.displayName,
                                    tint = if (isSelected) brandColor
                                        else if (isAvailable) brandColor.copy(alpha = 0.85f)
                                        else Color.White.copy(alpha = 0.35f),
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

            if (availableSources.isEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.04f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "No visual motion loop available for this track. Showing album cover artwork.",
                            fontSize = 12.5.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            lineHeight = 16.sp,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
