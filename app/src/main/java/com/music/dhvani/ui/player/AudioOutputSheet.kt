package com.music.dhvani.ui.player

import android.content.Context
import android.media.AudioManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.dhvani.R
import com.music.dhvani.data.settings.AppSettings
import com.music.dhvani.playback.AudioRouting
import com.music.dhvani.ui.components.optimizedHazeEffect
import com.music.dhvani.ui.haptics.Haptic
import com.music.dhvani.ui.haptics.rememberHaptics
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val DRAWER_SHAPE = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
private val DRAWER_MAX_WIDTH = 640.dp
private val ROW_SHAPE = RoundedCornerShape(16.dp)
private val SCRIM_COLOR = Color.Black.copy(alpha = 0.5f)

/**
 * Where the music is coming out, and how loud.
 *
 * Replaces system media output switcher that can't be controlled and doesn't
 * theme properly, providing direct, seamless audio output routing and responsive
 * volume adjustment with haptic feedback.
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun AudioOutputSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    accountName: String? = null,
) {
    val context = LocalContext.current
    val manager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val outputs = rememberAudioOutputs()

    var drag by remember { mutableFloatStateOf(0f) }
    var height by remember { mutableIntStateOf(0) }
    val offset by animateFloatAsState(
        targetValue = drag,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "outputDrawerOffset",
    )
    val scrimAlpha = if (height > 0) (1f - offset / height).coerceIn(0f, 1f) else 1f

    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SCRIM_COLOR.copy(alpha = SCRIM_COLOR.alpha * scrimAlpha))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(
            visible = shown,
            enter = slideInVertically(tween(260, easing = FastOutSlowInEasing)) { it },
            exit = slideOutVertically(tween(180)) { it },
        ) {
            Column(
                modifier = Modifier
                    .heightIn(max = 560.dp)
                    .widthIn(max = DRAWER_MAX_WIDTH)
                    .fillMaxWidth()
                    .onSizeChanged { height = it.height }
                    .offset { IntOffset(0, offset.roundToInt()) }
                    .clip(DRAWER_SHAPE)
                    .then(
                        if (reduceDynamicBlur || hazeState == null) {
                            Modifier.background(Color(0xFF14141B))
                        } else {
                            Modifier
                                .optimizedHazeEffect(
                                    state = hazeState,
                                    style = HazeMaterials.regular(Color(0xFF14141B)),
                                )
                                .background(Color(0xFF14141B).copy(alpha = 0.92f))
                        }
                    )
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = {},
                    )
                    .pointerInput(height) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (height > 0 && drag > height * DISMISS_DRAG_FRACTION) {
                                    onDismiss()
                                } else {
                                    drag = 0f
                                }
                            },
                            onDragCancel = { drag = 0f },
                        ) { _, delta ->
                            drag = (drag + delta).coerceAtLeast(0f)
                        }
                    }
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
                    .padding(top = 10.dp, bottom = 20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Grab handle
                Box(
                    Modifier
                        .padding(bottom = 12.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f)),
                )
                Text(
                    text = stringResource(R.string.audio_output),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, bottom = 14.dp),
                )

                // Outputs list
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    outputs.forEach { device ->
                        OutputRow(
                            device = device,
                            accountName = accountName,
                            onSelect = { AudioRouting.select(device.id) },
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                VolumeRow(manager, routeKey = outputs)
            }
        }
    }
}

/**
 * One output row.
 */
@Composable
private fun OutputRow(
    device: AudioRouting.Device,
    accountName: String?,
    onSelect: () -> Unit,
) {
    val haptics = rememberHaptics()
    val active = device.isActive
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ROW_SHAPE)
            .background(Color.White.copy(alpha = if (active) 0.10f else 0.05f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = !active,
            ) {
                haptics.play(Haptic.Select)
                onSelect()
            }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = if (active) 0.16f else 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = iconFor(device.kind),
                contentDescription = null,
                tint = Color.White.copy(alpha = if (active) 1f else 0.7f),
                modifier = Modifier.size(21.dp),
            )
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = outputLabel(device, accountName),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                ),
                color = Color.White.copy(alpha = if (active) 1f else 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (active) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.audio_output_playing),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.55f),
                )
            }
        }
        if (active) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Live media volume slider row.
 */
@Composable
private fun VolumeRow(manager: AudioManager, routeKey: Any) {
    var max by remember(manager) {
        mutableIntStateOf(manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1))
    }
    var level by remember(manager) {
        mutableFloatStateOf(manager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max)
    }
    var dragging by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(routeKey) {
        repeat(VOLUME_REREADS) {
            if (!dragging) {
                max = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                level = manager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max
            }
            delay(VOLUME_REREAD_GAP_MS)
        }
    }

    DisposableEffect(manager) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context, intent: android.content.Intent) {
                if (dragging) return
                level = manager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max
            }
        }
        val filter = android.content.IntentFilter(VOLUME_CHANGED_ACTION)
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(receiver, filter)
        }
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ROW_SHAPE)
            .background(Color.White.copy(alpha = 0.05f))
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (level > 0f) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        ThinSlider(
            value = level,
            onValueChange = {
                dragging = true
                level = it
                manager.setStreamVolume(AudioManager.STREAM_MUSIC, (it * max).roundToInt(), 0)
            },
            onValueChangeFinished = { dragging = false },
            idleHeight = 6.dp,
            activeHeight = 10.dp,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun iconFor(kind: AudioRouting.Kind): ImageVector = when (kind) {
    AudioRouting.Kind.PHONE -> Icons.Rounded.PhoneAndroid
    AudioRouting.Kind.WIRED -> Icons.Rounded.Headphones
    AudioRouting.Kind.USB -> Icons.Rounded.Usb
    AudioRouting.Kind.BLUETOOTH -> Icons.Rounded.Bluetooth
    AudioRouting.Kind.HDMI -> Icons.Rounded.Tv
    AudioRouting.Kind.OTHER -> Icons.Rounded.Speaker
}

private const val VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION"
private const val VOLUME_REREADS = 4
private const val VOLUME_REREAD_GAP_MS = 250L
private const val DISMISS_DRAG_FRACTION = 0.25f
