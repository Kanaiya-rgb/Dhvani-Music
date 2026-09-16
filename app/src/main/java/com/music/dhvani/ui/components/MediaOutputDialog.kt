package com.music.dhvani.ui.components

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.SpeakerGroup
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.dhvani.playback.AudioOutputStatus

/**
 * Media Output selector dialog.
 *
 * Allows switching playback route between Phone Speaker and connected
 * Bluetooth/Wired audio devices, and provides a direct shortcut into
 * the detailed studio Audio Pipeline.
 */
@Composable
fun MediaOutputDialog(
    onDismiss: () -> Unit,
    onOpenPipeline: () -> Unit,
    themeColors: List<Color> = emptyList(),
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val outputStatus by AudioOutputStatus.current.collectAsStateWithLifecycle()
    val availableRoutes by AudioOutputStatus.availableRoutes.collectAsStateWithLifecycle()
    val selectedRouteId by AudioOutputStatus.selectedRouteId.collectAsStateWithLifecycle()

    val primaryTint = themeColors.firstOrNull() ?: MaterialTheme.colorScheme.primary
    val secondaryTint = themeColors.getOrNull(1) ?: primaryTint

    val cardBrush = Brush.verticalGradient(
        colors = listOf(
            lerp(Color(0xFF191820), primaryTint, 0.22f),
            Color(0xFF131317),
            lerp(Color(0xFF15141B), secondaryTint, 0.14f),
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            dialogWindow?.setDimAmount(0.35f)
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.25f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(min = 300.dp, max = 360.dp)
                    .fillMaxWidth(0.88f)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(enabled = false) {},
                shape = RoundedCornerShape(24.dp),
                color = Color.Transparent,
                border = borderStroke,
                shadowElevation = 18.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(cardBrush)
                        .padding(20.dp),
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(
                                        primaryTint.copy(alpha = 0.20f),
                                        CircleShape,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SpeakerGroup,
                                    contentDescription = null,
                                    tint = primaryTint,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Media Output",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                    ),
                                    color = Color.White,
                                )
                                Text(
                                    text = "Choose playback destination",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.55f),
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    // List of output options
                    val phoneName = AudioOutputStatus.getPhoneName(context)
                    val routes = if (availableRoutes.isNotEmpty()) {
                        availableRoutes
                    } else {
                        listOf(
                            AudioOutputStatus.OutputRoute(
                                id = "speaker",
                                name = phoneName,
                                isSelected = true,
                                isConnected = true,
                            ),
                        )
                    }

                    val hasBluetooth = routes.any { it.id == "bluetooth" }

                    routes.forEach { route ->
                        val isSelected = if (selectedRouteId == "auto") {
                            route.isSelected
                        } else {
                            route.id == selectedRouteId
                        }

                        val icon = when (route.id) {
                            "bluetooth" -> Icons.Rounded.Headphones
                            "wired" -> Icons.Rounded.Headphones
                            else -> Icons.Rounded.PhoneAndroid
                        }

                        val subtitle = when (route.id) {
                            "bluetooth" -> "Bluetooth Audio"
                            "wired" -> "Wired Audio Port"
                            else -> "Phone Speaker"
                        }

                        RouteRow(
                            title = route.name,
                            subtitle = subtitle,
                            icon = icon,
                            isSelected = isSelected,
                            activeTint = primaryTint,
                            onClick = {
                                AudioOutputStatus.selectRoute(route.id)
                            },
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    if (!hasBluetooth) {
                        // Helpful hint when no bluetooth is connected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.04f))
                                .clickable {
                                    try {
                                        context.startActivity(
                                            Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            },
                                        )
                                    } catch (_: Throwable) {}
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Bluetooth,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.35f),
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Connect Bluetooth Device",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.5.sp,
                                    ),
                                    color = Color.White.copy(alpha = 0.8f),
                                )
                                Text(
                                    text = "Tap to pair headphones or speaker",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.45f),
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Audio Pipeline Shortcut Button
                    FilledTonalButton(
                        onClick = {
                            onDismiss()
                            onOpenPipeline()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = primaryTint.copy(alpha = 0.12f),
                            contentColor = Color.White.copy(alpha = 0.92f),
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = null,
                            tint = primaryTint,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Audio Pipeline Details",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.5.sp,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    activeTint: Color,
    onClick: () -> Unit,
) {
    val activeBorder = if (isSelected) {
        BorderStroke(1.2.dp, activeTint.copy(alpha = 0.85f))
    } else {
        BorderStroke(0.8.dp, Color.White.copy(alpha = 0.08f))
    }

    val activeBg = if (isSelected) {
        activeTint.copy(alpha = 0.14f)
    } else {
        Color.White.copy(alpha = 0.05f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(activeBg)
            .border(activeBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(
                    if (isSelected) activeTint.copy(alpha = 0.28f)
                    else Color.White.copy(alpha = 0.07f),
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) activeTint else Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp),
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    fontSize = 14.sp,
                ),
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.85f),
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) activeTint.copy(alpha = 0.90f)
                else Color.White.copy(alpha = 0.45f),
            )
        }

        if (isSelected) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = "Active Route",
                tint = activeTint,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
