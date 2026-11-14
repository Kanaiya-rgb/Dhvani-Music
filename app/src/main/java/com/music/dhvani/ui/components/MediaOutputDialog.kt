package com.music.dhvani.ui.components



import android.Manifest

import android.content.Context

import android.content.Intent

import android.content.pm.PackageManager

import android.media.AudioManager

import android.media.AudioFormat

import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos

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

import androidx.compose.material.icons.rounded.Speaker

import androidx.compose.material.icons.rounded.SpeakerGroup

import androidx.compose.material.icons.rounded.Tune

import com.music.dhvani.ui.icons.DhvaniIcons

import androidx.compose.material3.ButtonDefaults

import androidx.compose.material3.FilledTonalButton

import androidx.compose.material3.Icon

import androidx.compose.material3.IconButton

import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.Surface

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
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

    val simState by AudioOutputStatus.simulationState.collectAsStateWithLifecycle()



    var pendingRoute by remember { mutableStateOf<AudioOutputStatus.OutputRoute?>(null) }
    var changingCategoryRoute by remember { mutableStateOf<AudioOutputStatus.OutputRoute?>(null) }

    var showSimulatorSheet by remember { mutableStateOf(false) }



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
                    val activeRoute = availableRoutes.firstOrNull { it.isSelected || it.id == confirmedActiveRouteId }
                    val headerIcon = activeRoute?.let { AudioOutputStatus.iconFor(it.deviceCategory) }
                        ?: AudioOutputStatus.iconFor(outputStatus.deviceCategory)

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

                                    imageVector = headerIcon,

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



                    // Simulation active banner

                    if (simState.isSimulated && simState.preset != null) {

                        Row(

                            modifier = Modifier

                                .fillMaxWidth()

                                .padding(bottom = 12.dp)

                                .clip(RoundedCornerShape(12.dp))

                                .background(Color(0xFFE2A740).copy(alpha = 0.16f))

                                .border(1.dp, Color(0xFFE2A740).copy(alpha = 0.40f), RoundedCornerShape(12.dp))

                                .padding(horizontal = 12.dp, vertical = 8.dp),

                            verticalAlignment = Alignment.CenterVertically,

                            horizontalArrangement = Arrangement.SpaceBetween,

                        ) {

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {

                                Icon(

                                    imageVector = Icons.Rounded.Tune,

                                    contentDescription = null,

                                    tint = Color(0xFFE2A740),

                                    modifier = Modifier.size(16.dp),

                                )

                                Spacer(Modifier.width(8.dp))

                                Text(

                                    text = "Simulated: ${simState.preset?.deviceName}",

                                    style = MaterialTheme.typography.bodySmall.copy(

                                        fontSize = 12.sp,

                                        fontWeight = FontWeight.Medium,

                                    ),

                                    color = Color(0xFFE2A740),

                                )

                            }

                            Text(

                                text = "Reset",

                                style = MaterialTheme.typography.labelSmall.copy(

                                    fontWeight = FontWeight.Bold,

                                    color = Color.Black,

                                ),

                                modifier = Modifier

                                    .clip(RoundedCornerShape(6.dp))

                                    .background(Color(0xFFE2A740))

                                    .clickable { AudioOutputStatus.clearSimulation(context) }

                                    .padding(horizontal = 8.dp, vertical = 4.dp),

                            )

                        }

                    }



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



                        val icon = AudioOutputStatus.iconFor(route.deviceCategory)

                        val subtitle = AudioOutputStatus.subtitleFor(route.deviceCategory)



                        RouteRow(

                            title = route.name,

                            subtitle = if (route.isBluetooth) "$subtitle · Tap icon to edit" else subtitle,

                            icon = icon,

                            isSelected = isSelected,

                            onIconClick = if (route.isBluetooth) { { changingCategoryRoute = route } } else null,

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



                    // Audio Pipeline Pill Card (Dhvani Audio Architecture)

                    val targetRate = outputStatus.actualSampleRateHz?.takeIf { it > 0 }

                        ?: outputStatus.sampleRatesHz.firstOrNull()

                        ?: 48000

                    val sampleRateText = if (targetRate % 1000 == 0) "${targetRate / 1000} kHz" else "${targetRate / 1000.0} kHz"

                    val bitDepthText = when (outputStatus.actualEncoding) {

                        AudioFormat.ENCODING_PCM_32BIT -> "32-bit PCM"

                        AudioFormat.ENCODING_PCM_FLOAT -> "32-bit float"

                        AudioFormat.ENCODING_PCM_24BIT_PACKED -> "24-bit PCM"

                        AudioFormat.ENCODING_PCM_16BIT -> "16-bit PCM"

                        else -> if (outputStatus.isUsb) "24-bit PCM" else "16-bit PCM"

                    }

                    val pipelineSubtitle = "$bitDepthText · $sampleRateText"



                    Row(

                        modifier = Modifier

                            .fillMaxWidth()

                            .clip(RoundedCornerShape(14.dp))

                            .background(Color.White.copy(alpha = 0.08f))

                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))

                            .clickable {

                                onDismiss()

                                onOpenPipeline()

                            }

                            .padding(horizontal = 14.dp, vertical = 12.dp),

                        verticalAlignment = Alignment.CenterVertically,

                        horizontalArrangement = Arrangement.SpaceBetween,

                    ) {

                        Row(verticalAlignment = Alignment.CenterVertically) {

                            Box(

                                modifier = Modifier

                                    .size(36.dp)

                                    .clip(CircleShape)

                                    .background(Color.White.copy(alpha = 0.10f)),

                                contentAlignment = Alignment.Center,

                            ) {

                                Icon(

                                    imageVector = Icons.Rounded.GraphicEq,

                                    contentDescription = null,

                                    tint = Color.White,

                                    modifier = Modifier.size(18.dp),

                                )

                            }

                            Spacer(Modifier.width(12.dp))

                            Column {

                                Text(

                                    text = "Audio Pipeline",

                                    style = MaterialTheme.typography.bodyMedium.copy(

                                        fontWeight = FontWeight.SemiBold,

                                        fontSize = 14.sp,

                                    ),

                                    color = Color.White,

                                )

                                Text(

                                    text = pipelineSubtitle,

                                    style = MaterialTheme.typography.labelSmall.copy(

                                        fontSize = 11.5.sp,

                                    ),

                                    color = Color.White.copy(alpha = 0.60f),

                                )

                            }

                        }

                        Icon(

                            imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,

                            contentDescription = null,

                            tint = Color.White.copy(alpha = 0.45f),

                            modifier = Modifier.size(14.dp),

                        )

                    }



                    if (com.music.dhvani.BuildConfig.DEBUG) {

                        Spacer(Modifier.height(8.dp))

                        androidx.compose.material3.OutlinedButton(

                            onClick = { showSimulatorSheet = true },

                            modifier = Modifier

                                .fillMaxWidth()

                                .height(40.dp),

                            shape = RoundedCornerShape(12.dp),

                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2A740).copy(alpha = 0.45f)),

                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(

                                contentColor = Color(0xFFE2A740),

                            ),

                        ) {

                            Icon(

                                imageVector = Icons.Rounded.Tune,

                                contentDescription = null,

                                tint = Color(0xFFE2A740),

                                modifier = Modifier.size(15.dp),

                            )

                            Spacer(Modifier.width(8.dp))

                            Text(

                                text = "Audio Device Simulator (Debug)",

                                style = MaterialTheme.typography.labelMedium.copy(

                                    fontWeight = FontWeight.SemiBold,

                                    fontSize = 12.sp,

                                ),

                            )

                        }

                    }

                }

            }

        }



        if (changingCategoryRoute != null) {
        val targetRoute = changingCategoryRoute!!
        AlertDialog(
            onDismissRequest = { changingCategoryRoute = null },
            title = {
                Text(
                    text = "Device Type",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = targetRoute.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.65f),
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                    listOf(
                        AudioOutputStatus.AudioDeviceCategory.EARBUDS to "Wireless Earbuds / AirPods",
                        AudioOutputStatus.AudioDeviceCategory.HEADPHONES to "Over-Ear Headphones",
                        AudioOutputStatus.AudioDeviceCategory.NECKBAND to "Neckband",
                        AudioOutputStatus.AudioDeviceCategory.BLUETOOTH_SPEAKER to "Bluetooth Speaker",
                        AudioOutputStatus.AudioDeviceCategory.CAR_AUDIO to "Car Audio System",
                    ).forEach { (category, label) ->
                        val isCurrent = targetRoute.deviceCategory == category
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isCurrent) Color.White.copy(alpha = 0.15f) else Color.Transparent)
                                .clickable {
                                    AudioOutputStatus.overrideDeviceCategory(targetRoute.name, category, context)
                                    changingCategoryRoute = null
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = AudioOutputStatus.iconFor(category),
                                contentDescription = null,
                                tint = if (isCurrent) MaterialTheme.colorScheme.primary else Color.White,
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.White,
                                modifier = Modifier.weight(1f),
                            )
                            if (isCurrent) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { changingCategoryRoute = null }) {
                    Text("Close", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = Color(0xFF1E1D24),
            shape = RoundedCornerShape(20.dp),
        )
    }

    if (showSimulatorSheet) {

            AudioDeviceSimulatorSheet(onDismiss = { showSimulatorSheet = false })

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

    onIconClick: (() -> Unit)? = null,

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

                )

                .then(

                    if (onIconClick != null) Modifier.clip(CircleShape).clickable(onClick = onIconClick)

                    else Modifier

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



