package com.music.dhvani.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.GraphicEq
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
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
    val confirmedActiveRouteId by AudioOutputStatus.confirmedActiveRouteId.collectAsStateWithLifecycle()
    val errorMessage by AudioOutputStatus.errorMessage.collectAsStateWithLifecycle()

    var pendingRoute by remember { mutableStateOf<AudioOutputStatus.OutputRoute?>(null) }

    val btPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        Log.d("MediaOutput", "BLUETOOTH_CONNECT permission result in dialog: isGranted=$isGranted")
        val route = pendingRoute
        pendingRoute = null
        if (isGranted && route != null) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager != null) {
                AudioOutputStatus.updateDevice(context, audioManager)
            }
            AudioOutputStatus.selectRoute(route.id)
        } else if (!isGranted) {
            AudioOutputStatus.errorMessage.value = "Bluetooth permission is required to route audio"
        }
    }

    // Refresh devices immediately upon opening
    LaunchedEffect(Unit) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (audioManager != null) {
            AudioOutputStatus.updateDevice(context, audioManager)
        }
    }

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
        ),
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
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
                color = Color(0xFF14131A),
                border = borderStroke,
                shadowElevation = 8.dp,
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
                                        Color.White.copy(alpha = 0.14f),
                                        CircleShape,
                                    )
                                    .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SpeakerGroup,
                                    contentDescription = null,
                                    tint = Color.White,
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

                    Spacer(Modifier.height(16.dp))

                    // Error banner if any routing failed or device disconnected
                    if (!errorMessage.isNullOrBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFE53935).copy(alpha = 0.16f))
                                .border(1.dp, Color(0xFFE53935).copy(alpha = 0.40f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFFF8A80),
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = errorMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                ),
                                color = Color.White.copy(alpha = 0.95f),
                            )
                        }
                    }

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
                                isSpeaker = true,
                            ),
                        )
                    }

                    val hasBluetooth = routes.any { it.isBluetooth || it.id.startsWith("bluetooth") }

                    routes.forEach { route ->
                        val isSelected = route.isSelected || route.id == confirmedActiveRouteId

                        val icon = when {
                            route.isBluetooth -> Icons.Rounded.Headphones
                            route.isWired -> Icons.Rounded.Headphones
                            route.isUsb -> Icons.Rounded.GraphicEq
                            route.id.startsWith("bluetooth") -> Icons.Rounded.Headphones
                            route.id == "wired" -> Icons.Rounded.Headphones
                            route.id.startsWith("usb") -> Icons.Rounded.GraphicEq
                            else -> Icons.Rounded.PhoneAndroid
                        }

                        val subtitle = when {
                            route.isBluetooth || route.id.startsWith("bluetooth") -> "Bluetooth Audio"
                            route.isWired || route.id == "wired" -> "Wired Audio Port"
                            route.isUsb || route.id.startsWith("usb") -> "USB Audio Device"
                            else -> "Phone Speaker"
                        }

                        RouteRow(
                            title = route.name,
                            subtitle = subtitle,
                            icon = icon,
                            isSelected = isSelected,
                            onClick = {
                                AudioOutputStatus.errorMessage.value = null
                                val needsBtPermission = route.isBluetooth &&
                                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
                                if (needsBtPermission) {
                                    Log.d("MediaOutput", "Requesting BLUETOOTH_CONNECT permission before routing to ${route.name}")
                                    pendingRoute = route
                                    btPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                                } else {
                                    AudioOutputStatus.selectRoute(route.id)
                                }
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
                                .background(Color.White.copy(alpha = 0.05f))
                                .border(0.8.dp, Color.White.copy(alpha = 0.09f), RoundedCornerShape(14.dp))
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
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color.White.copy(alpha = 0.08f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Bluetooth,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Connect Bluetooth Device",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.5.sp,
                                    ),
                                    color = Color.White,
                                )
                                Text(
                                    text = "Tap to pair headphones or speaker",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.50f),
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
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color.White.copy(alpha = 0.10f),
                            contentColor = Color.White,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Audio Pipeline Details",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.5.sp,
                            ),
                            color = Color.White,
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
    onClick: () -> Unit,
) {
    val activeBorder = if (isSelected) {
        BorderStroke(1.5.dp, Color.White.copy(alpha = 0.38f))
    } else {
        BorderStroke(0.8.dp, Color.White.copy(alpha = 0.09f))
    }

    val activeBg = if (isSelected) {
        Color.White.copy(alpha = 0.14f)
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
        // High-contrast, sufficiently opaque icon background & pure white icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (isSelected) Color.White.copy(alpha = 0.22f)
                    else Color.White.copy(alpha = 0.08f),
                    CircleShape,
                )
                .border(
                    0.75.dp,
                    if (isSelected) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f),
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
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
                color = Color.White,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) Color.White.copy(alpha = 0.85f)
                else Color.White.copy(alpha = 0.50f),
            )
        }

        if (isSelected) {
            // Distinct, vibrant green checkmark badge guaranteed visible on any artwork/background
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(Color(0xFF34C759), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Active Route",
                    tint = Color.White,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
    }
}

