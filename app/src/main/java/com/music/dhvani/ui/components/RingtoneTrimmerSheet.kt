package com.music.dhvani.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil3.compose.AsyncImage
import com.music.dhvani.data.model.Song
import com.music.dhvani.ringtone.RingtoneHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

private const val MAX_RINGTONE_SEC = 30L
private const val MIN_RINGTONE_SEC = 5L

private enum class DragMode { NONE, START_HANDLE, END_HANDLE, WINDOW }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RingtoneTrimmerSheet(
    song: Song,
    initialPositionMs: Long = 0L,
    onPauseMainPlayer: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Automatically pause main player when trimmer sheet opens
    LaunchedEffect(Unit) {
        onPauseMainPlayer?.invoke()
    }

    // Duration calculation
    val parsedDurationMs = remember(song.durationText) {
        val parts = song.durationText?.split(":")?.mapNotNull { it.toIntOrNull() }
        if (parts != null && parts.size >= 2) {
            val mins = parts[0]
            val secs = parts[1]
            (mins * 60 + secs) * 1000L
        } else {
            180_000L
        }
    }
    val totalDurationMs = parsedDurationMs.coerceAtLeast(30_000L)

    // Trimming selection state (in ms) - starts at current playback position if playing!
    val defaultStart = if (initialPositionMs > 0L) {
        initialPositionMs.coerceIn(0L, (totalDurationMs - (MIN_RINGTONE_SEC * 1000L)).coerceAtLeast(0L))
    } else {
        0L
    }
    val defaultEnd = (defaultStart + (MAX_RINGTONE_SEC * 1000L)).coerceAtMost(totalDurationMs)

    var startMs by remember(song.videoId, initialPositionMs) { mutableLongStateOf(defaultStart) }
    var endMs by remember(song.videoId, initialPositionMs) { mutableLongStateOf(defaultEnd) }
    val durationSec = ((endMs - startMs) / 1000L).coerceAtLeast(1L)

    // Audio file loading state
    var sourceAudioFile by remember { mutableStateOf<File?>(null) }
    var isLoadingSource by remember { mutableStateOf(true) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }

    // Trimming action state
    var isTrimming by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    // Audio preview player with audio focus to prevent dual playback
    var isPreviewPlaying by remember { mutableStateOf(false) }
    var previewPositionMs by remember { mutableLongStateOf(0L) }

    val audioAttributes = remember {
        AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()
    }

    val previewPlayer = remember {
        ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .build().apply {
                repeatMode = Player.REPEAT_MODE_OFF
            }
    }

    DisposableEffect(previewPlayer) {
        onDispose {
            previewPlayer.release()
        }
    }

    // Prepare audio file on launch
    LaunchedEffect(song.videoId) {
        isLoadingSource = true
        val immediateUri = song.localUri ?: com.music.dhvani.playback.StreamChoice.of(song.videoId)?.url
        if (immediateUri != null) {
            previewPlayer.setMediaItem(MediaItem.fromUri(immediateUri))
            previewPlayer.prepare()
        }

        val file = RingtoneHelper.prepareSourceAudioFile(context, song) { p ->
            downloadProgress = p
        }
        sourceAudioFile = file
        isLoadingSource = false

        if (file != null) {
            previewPlayer.setMediaItem(MediaItem.fromUri(file.toURI().toString()))
            previewPlayer.prepare()
        }
    }

    // Keep reference states for preview loop without cancelling/restarting coroutine during drag
    val currentStartMsState by rememberUpdatedState(startMs)
    val currentEndMsState by rememberUpdatedState(endMs)
    var isDraggingTrimmer by remember { mutableStateOf(false) }

    // Monitor preview loop within [startMs, endMs] with smooth 50fps needle updates
    LaunchedEffect(isPreviewPlaying) {
        if (!isPreviewPlaying) {
            previewPlayer.pause()
            previewPositionMs = currentStartMsState
            return@LaunchedEffect
        }

        onPauseMainPlayer?.invoke()
        previewPlayer.seekTo(currentStartMsState)
        previewPlayer.play()

        while (isActive && isPreviewPlaying) {
            val curStart = currentStartMsState
            val curEnd = currentEndMsState
            val pos = previewPlayer.currentPosition
            previewPositionMs = pos

            // If playback reached end of cut or fell outside range, loop back to start smoothly
            if (pos >= curEnd || pos < curStart - 500L) {
                previewPlayer.seekTo(curStart)
                previewPositionMs = curStart
            }
            delay(20L) // 50fps smooth needle animation
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            previewPlayer.stop()
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = Color(0xFF14171C),
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Drag handle pill
            Box(
                modifier = Modifier
                    .size(width = 38.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f)),
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Sheet Title Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF8B5CF6).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = null,
                            tint = Color(0xFFA78BFA),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Set as Ringtone",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                        )
                        Text(
                            text = "Instagram-style cut (Max 30s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f),
                        )
                    }
                }

                IconButton(
                    onClick = {
                        previewPlayer.stop()
                        onDismiss()
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f)),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Track Header Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.05f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(
                        model = song.thumbnailUrl,
                        contentDescription = song.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.65f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Non-blocking background sync pill
            AnimatedVisibility(visible = isLoadingSource) {
                Row(
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF8B5CF6).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFFA78BFA).copy(alpha = 0.3f), CircleShape)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFFA78BFA),
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (downloadProgress > 0f) {
                            "Syncing HD audio (${(downloadProgress * 100).toInt()}%)..."
                        } else {
                            "Syncing audio stream..."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFC4B5FD),
                    )
                }
            }

            // Instagram-Style Interactive Waveform Trimmer Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF1E222B),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.35f)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Header Row: Duration badge + Instagram Quick Duration Chips (15s, 30s)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "✂️ Cut Length: ",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.7f),
                            )
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0xFF8B5CF6).copy(alpha = 0.25f))
                                    .border(1.dp, Color(0xFFA78BFA).copy(alpha = 0.4f), CircleShape)
                                    .padding(horizontal = 10.dp, vertical = 3.dp),
                            ) {
                                Text(
                                    text = "${durationSec}s",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFC4B5FD),
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            DurationChip(
                                label = "15s",
                                isSelected = durationSec == 15L,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val newEnd = (startMs + 15_000L).coerceAtMost(totalDurationMs)
                                    val newStart = (newEnd - 15_000L).coerceAtLeast(0L)
                                    startMs = newStart
                                    endMs = newEnd
                                    if (isPreviewPlaying) {
                                        previewPlayer.seekTo(newStart)
                                        previewPositionMs = newStart
                                    }
                                },
                            )
                            DurationChip(
                                label = "30s Max",
                                isSelected = durationSec == 30L,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val newEnd = (startMs + 30_000L).coerceAtMost(totalDurationMs)
                                    val newStart = (newEnd - 30_000L).coerceAtLeast(0L)
                                    startMs = newStart
                                    endMs = newEnd
                                    if (isPreviewPlaying) {
                                        previewPlayer.seekTo(newStart)
                                        previewPositionMs = newStart
                                    }
                                },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Unified Instagram Story-style Waveform Trimmer with Real-Time Playhead Needle
                    InstagramWaveformTrimmer(
                        totalDurationMs = totalDurationMs,
                        startMs = startMs,
                        endMs = endMs,
                        previewPositionMs = previewPositionMs,
                        isPlaying = isPreviewPlaying,
                        seed = song.videoId.hashCode(),
                        onDragStateChanged = { dragging ->
                            isDraggingTrimmer = dragging
                            if (!dragging) {
                                previewPositionMs = startMs
                                if (isPreviewPlaying) {
                                    onPauseMainPlayer?.invoke()
                                    previewPlayer.seekTo(startMs)
                                }
                            }
                        },
                        onRangeChanged = { newStart, newEnd ->
                            startMs = newStart
                            endMs = newEnd
                        },
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Balanced, Symmetrical Start & End Fine-Tune Steppers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TimeStepperPod(
                            label = "START",
                            timeMs = startMs,
                            onDecrement = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                startMs = (startMs - 1000L).coerceAtLeast(0L)
                            },
                            onIncrement = {
                                if (endMs - (startMs + 1000L) >= MIN_RINGTONE_SEC * 1000L) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    startMs += 1000L
                                }
                            },
                        )

                        Text(
                            text = "↔️ Tap or drag",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color.White.copy(alpha = 0.45f),
                            maxLines = 1,
                            softWrap = false,
                        )

                        TimeStepperPod(
                            label = "END",
                            timeMs = endMs,
                            onDecrement = {
                                if ((endMs - 1000L) - startMs >= MIN_RINGTONE_SEC * 1000L) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    endMs -= 1000L
                                }
                            },
                            onIncrement = {
                                val candidate = endMs + 1000L
                                if (candidate <= totalDurationMs && candidate - startMs <= MAX_RINGTONE_SEC * 1000L) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    endMs = candidate
                                }
                            },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Presets Row with Active Status
            val isIntroActive = startMs == 0L
            val chorusCandidate = if (totalDurationMs >= 75_000L) 45_000L else (totalDurationMs * 0.3f).toLong()
            val isChorusActive = abs(startMs - chorusCandidate) < 2000L
            val dropCandidate = if (totalDurationMs >= 105_000L) 75_000L else (totalDurationMs * 0.5f).toLong()
            val isDropActive = abs(startMs - dropCandidate) < 2000L

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PresetChip(
                    title = "🎯 Intro",
                    subtitle = "0:00 - 0:30",
                    isActive = isIntroActive,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        startMs = 0L
                        endMs = (30_000L).coerceAtMost(totalDurationMs)
                        if (isPreviewPlaying) {
                            previewPlayer.seekTo(startMs)
                            previewPositionMs = startMs
                        }
                    },
                )
                PresetChip(
                    title = "⚡ Chorus",
                    subtitle = "Best Part",
                    isActive = isChorusActive,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        startMs = chorusCandidate
                        endMs = (startMs + 30_000L).coerceAtMost(totalDurationMs)
                        if (isPreviewPlaying) {
                            previewPlayer.seekTo(startMs)
                            previewPositionMs = startMs
                        }
                    },
                )
                PresetChip(
                    title = "🚀 Drop",
                    subtitle = "Climax",
                    isActive = isDropActive,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        startMs = dropCandidate
                        endMs = (startMs + 30_000L).coerceAtMost(totalDurationMs)
                        if (isPreviewPlaying) {
                            previewPlayer.seekTo(startMs)
                            previewPositionMs = startMs
                        }
                    },
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Preview Player Button
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { isPreviewPlaying = !isPreviewPlaying },
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.05f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isPreviewPlaying) Color(0xFFA78BFA).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.08f),
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF8B5CF6), Color(0xFF7C3AED))
                                    )
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (isPreviewPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (isPreviewPlaying) "Pause Preview" else "Play Preview",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isPreviewPlaying) "Playing Ringtone Loop" else "Preview Ringtone Cut",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (isPreviewPlaying) Color(0xFFC4B5FD) else Color.White,
                            )
                            Text(
                                text = "${formatSeconds(startMs)} → ${formatSeconds(endMs)} • ${durationSec}s cut",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.6f),
                            )
                        }
                    }

                    if (isPreviewPlaying) {
                        EqualizerIconWave()
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Main Action Button: Set Ringtone
            Button(
                onClick = {
                    previewPlayer.stop()
                    isPreviewPlaying = false

                    if (!RingtoneHelper.canSetRingtone(context)) {
                        showPermissionDialog = true
                        return@Button
                    }

                    isTrimming = true
                    scope.launch {
                        var source = sourceAudioFile
                        while (source == null && isLoadingSource) {
                            delay(200L)
                            source = sourceAudioFile
                        }
                        if (source == null || !source.exists()) {
                            withContext(Dispatchers.Main) {
                                isTrimming = false
                                Toast.makeText(context, "Could not prepare audio file", Toast.LENGTH_SHORT).show()
                            }
                            return@launch
                        }

                        val tempOut = File(context.cacheDir, "ringtone_trimmed_${song.videoId}.m4a")
                        val trimSuccess = RingtoneHelper.trimAudio(
                            sourceFile = source,
                            outputFile = tempOut,
                            startMs = startMs,
                            endMs = endMs,
                        )

                        if (!trimSuccess) {
                            withContext(Dispatchers.Main) {
                                isTrimming = false
                                Toast.makeText(context, "Failed to trim audio clip", Toast.LENGTH_SHORT).show()
                            }
                            return@launch
                        }

                        val setSuccess = RingtoneHelper.saveAndSetRingtone(context, tempOut, song)
                        withContext(Dispatchers.Main) {
                            isTrimming = false
                            if (setSuccess) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showSuccessDialog = true
                            } else {
                                Toast.makeText(context, "Could not set ringtone on this device", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                enabled = !isTrimming,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(27.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF8B5CF6),
                    disabledContainerColor = Color(0xFF8B5CF6).copy(alpha = 0.4f),
                ),
            ) {
                if (isTrimming) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isLoadingSource) "Downloading & Trimming..." else "Setting Phone Ringtone...",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.NotificationsActive,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Set as Phone Ringtone",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                    )
                }
            }
        }
    }

    // Ringtone Set Success Modal / Popup
    if (showSuccessDialog) {
        Dialog(
            onDismissRequest = {
                showSuccessDialog = false
                onDismiss()
            },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .clip(RoundedCornerShape(28.dp))
                    .border(
                        1.dp,
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(Color(0xFFA78BFA).copy(alpha = 0.5f), Color.White.copy(alpha = 0.1f))
                        ),
                        RoundedCornerShape(28.dp)
                    ),
                color = Color(0xFF161922),
                tonalElevation = 10.dp,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Pulsing Ringtone Bell Icon Badge
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                androidx.compose.ui.graphics.Brush.radialGradient(
                                    listOf(
                                        Color(0xFF8B5CF6).copy(alpha = 0.35f),
                                        Color(0xFF8B5CF6).copy(alpha = 0.08f),
                                    )
                                )
                            )
                            .border(1.5.dp, Color(0xFFA78BFA), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = null,
                            tint = Color(0xFFA78BFA),
                            modifier = Modifier.size(36.dp),
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Ringtone Set Successfully!",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                        ),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Your phone will now play this trimmed audio clip on incoming calls.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.65f),
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Track & Clip Info Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.05f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AsyncImage(
                                model = song.thumbnailUrl,
                                contentDescription = song.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${formatSeconds(startMs)} → ${formatSeconds(endMs)}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                        color = Color(0xFFC4B5FD),
                                    )
                                    Text(
                                        text = " • ${durationSec}s cut",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.5f),
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    Button(
                        onClick = {
                            showSuccessDialog = false
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF8B5CF6),
                        ),
                    ) {
                        Text(
                            text = "Done",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }

    // Permission Prompt Dialog
    if (showPermissionDialog) {
        Dialog(
            onDismissRequest = { showPermissionDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .clip(RoundedCornerShape(28.dp))
                    .border(
                        1.dp,
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(Color(0xFFA78BFA).copy(alpha = 0.4f), Color.White.copy(alpha = 0.1f))
                        ),
                        RoundedCornerShape(28.dp)
                    ),
                color = Color(0xFF161922),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF8B5CF6).copy(alpha = 0.15f))
                            .border(1.2.dp, Color(0xFFA78BFA).copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Security,
                            contentDescription = null,
                            tint = Color(0xFFA78BFA),
                            modifier = Modifier.size(32.dp),
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Permission Required",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                        ),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Android requires permission to 'Modify system settings' in order to set this song as your phone ringtone.\n\nTap Continue to allow it in Android Settings.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            showPermissionDialog = false
                            RingtoneHelper.requestWriteSettingsPermission(context)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                    ) {
                        Text(
                            text = "Continue to Settings",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = { showPermissionDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "Cancel",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.6f),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Unified Instagram Story-style Waveform Trimmer with real-time gliding playhead needle
 */
@Composable
private fun InstagramWaveformTrimmer(
    totalDurationMs: Long,
    startMs: Long,
    endMs: Long,
    previewPositionMs: Long,
    isPlaying: Boolean,
    seed: Int,
    onDragStateChanged: (isDragging: Boolean) -> Unit,
    onRangeChanged: (newStart: Long, newEnd: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val barCount = 62

    // Capture latest parameters in updated state references so gestures never cancel or abort mid-drag
    val currentTotalDurationMs by rememberUpdatedState(totalDurationMs)
    val currentStartMs by rememberUpdatedState(startMs)
    val currentEndMs by rememberUpdatedState(endMs)
    val currentOnRangeChanged by rememberUpdatedState(onRangeChanged)
    val currentOnDragStateChanged by rememberUpdatedState(onDragStateChanged)

    var activeDrag by remember { mutableStateOf(DragMode.NONE) }
    var dragAnchorX by remember { mutableFloatStateOf(0f) }
    var dragInitialStartMs by remember { mutableLongStateOf(0L) }
    var dragInitialEndMs by remember { mutableLongStateOf(0L) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(88.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0F1218))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
    ) {
        val totalWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val startFraction = (startMs.toFloat() / totalDurationMs).coerceIn(0f, 1f)
        val endFraction = (endMs.toFloat() / totalDurationMs).coerceIn(0f, 1f)

        val startPx = startFraction * totalWidthPx
        val endPx = endFraction * totalWidthPx
        val windowWidthPx = (endPx - startPx).coerceAtLeast(with(density) { 36.dp.toPx() })

        val playheadFraction = (previewPositionMs.toFloat() / totalDurationMs).coerceIn(0f, 1f)
        val playheadPx = (playheadFraction * totalWidthPx).coerceIn(startPx, endPx)

        // 1. Base Audio Waveform Bars
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (i in 0 until barCount) {
                val fraction = i.toFloat() / barCount
                val isInsideWindow = fraction in startFraction..endFraction

                val wave1 = abs(sin((i * 0.28 + seed * 0.05))).toFloat()
                val wave2 = abs(sin((i * 0.65 + seed * 0.12))).toFloat()
                val normalizedHeight = (wave1 * 0.6f + wave2 * 0.4f).coerceIn(0.18f, 1f) * 58f

                val isPlayed = isPlaying && fraction in startFraction..playheadFraction

                val barBrush = when {
                    isPlayed -> Brush.verticalGradient(
                        listOf(Color.White, Color(0xFFE9D5FF), Color(0xFFA78BFA))
                    )
                    isInsideWindow -> Brush.verticalGradient(
                        listOf(Color(0xFFC084FC), Color(0xFFA855F7), Color(0xFF7C3AED))
                    )
                    else -> SolidColor(Color.White.copy(alpha = 0.14f))
                }

                Box(
                    modifier = Modifier
                        .width(3.2.dp)
                        .height(normalizedHeight.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(barBrush)
                )
            }
        }

        // 2. Instagram Story-Style Selection Window (Transparent Interior with Touch-Friendly Gripper Handles)
        Box(
            modifier = Modifier
                .offset { IntOffset(startPx.roundToInt(), 0) }
                .width(with(density) { windowWidthPx.toDp() })
                .fillMaxHeight()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF8B5CF6).copy(alpha = 0.10f))
                .border(1.5.dp, Color(0xFFA78BFA), RoundedCornerShape(8.dp))
        ) {
            // Left Bracket Handle Gripper
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(7.dp)
                    .fillMaxHeight()
                    .background(
                        if (activeDrag == DragMode.START_HANDLE) Color.White else Color(0xFFA78BFA),
                        RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 3.dp, height = 20.dp)
                        .clip(CircleShape)
                        .background(if (activeDrag == DragMode.START_HANDLE) Color(0xFF8B5CF6) else Color.White)
                )
            }

            // Right Bracket Handle Gripper
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(7.dp)
                    .fillMaxHeight()
                    .background(
                        if (activeDrag == DragMode.END_HANDLE) Color.White else Color(0xFFA78BFA),
                        RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 3.dp, height = 20.dp)
                        .clip(CircleShape)
                        .background(if (activeDrag == DragMode.END_HANDLE) Color(0xFF8B5CF6) else Color.White)
                )
            }
        }

        // 3. Playhead Laser Needle
        if (isPlaying || previewPositionMs > 0L) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(playheadPx.roundToInt() - with(density) { 1.dp.roundToPx() }, 0) }
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(Color.White)
            )
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            playheadPx.roundToInt() - with(density) { 3.dp.roundToPx() },
                            with(density) { 3.dp.roundToPx() },
                        )
                    }
                    .size(width = 6.dp, height = 10.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White)
            )
        }

        // 4. Stable Touch, Click & Drag Gesture Overlay (Click anywhere on wave to jump cut length!)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val touchX = down.position.x
                        val totalW = size.width.toFloat().coerceAtLeast(1f)
                        val sPx = (currentStartMs.toFloat() / currentTotalDurationMs) * totalW
                        val ePx = (currentEndMs.toFloat() / currentTotalDurationMs) * totalW

                        val distStart = abs(touchX - sPx)
                        val distEnd = abs(touchX - ePx)
                        val threshold = 34.dp.toPx()

                        var dragMode = when {
                            distStart <= threshold && distStart < distEnd -> DragMode.START_HANDLE
                            distEnd <= threshold -> DragMode.END_HANDLE
                            touchX in sPx..ePx -> DragMode.WINDOW
                            else -> DragMode.NONE
                        }

                        var anchorX = touchX
                        var initialStartMs = currentStartMs
                        var initialEndMs = currentEndMs

                        // If user clicked/tapped on wave outside selection window:
                        // Instantly center the cut length right at the tapped spot!
                        if (dragMode == DragMode.NONE) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val windowDuration = currentEndMs - currentStartMs
                            val targetStart = ((touchX / totalW) * currentTotalDurationMs - (windowDuration / 2L)).toLong()
                            val clampedStart = targetStart.coerceIn(0L, currentTotalDurationMs - windowDuration)
                            currentOnRangeChanged(clampedStart, clampedStart + windowDuration)
                            dragMode = DragMode.WINDOW
                            initialStartMs = clampedStart
                            initialEndMs = clampedStart + windowDuration
                            anchorX = touchX
                        }

                        activeDrag = dragMode
                        currentOnDragStateChanged(true)

                        while (true) {
                            val event = awaitPointerEvent()
                            val pointer = event.changes.firstOrNull { it.id == down.id } ?: break

                            if (pointer.pressed) {
                                pointer.consume()
                                val deltaPx = pointer.position.x - anchorX
                                val deltaMs = ((deltaPx / totalW) * currentTotalDurationMs).toLong()

                                when (dragMode) {
                                    DragMode.WINDOW -> {
                                        val windowDuration = initialEndMs - initialStartMs
                                        val newStart = (initialStartMs + deltaMs).coerceIn(0L, currentTotalDurationMs - windowDuration)
                                        currentOnRangeChanged(newStart, newStart + windowDuration)
                                    }
                                    DragMode.START_HANDLE -> {
                                        val maxStart = (currentEndMs - MIN_RINGTONE_SEC * 1000L).coerceAtLeast(0L)
                                        val minStart = (currentEndMs - MAX_RINGTONE_SEC * 1000L).coerceAtLeast(0L)
                                        val newStart = (initialStartMs + deltaMs).coerceIn(minStart, maxStart)
                                        currentOnRangeChanged(newStart, currentEndMs)
                                    }
                                    DragMode.END_HANDLE -> {
                                        val minEnd = (currentStartMs + MIN_RINGTONE_SEC * 1000L).coerceAtMost(currentTotalDurationMs)
                                        val maxEnd = (currentStartMs + MAX_RINGTONE_SEC * 1000L).coerceAtMost(currentTotalDurationMs)
                                        val newEnd = (initialEndMs + deltaMs).coerceIn(minEnd, maxEnd)
                                        currentOnRangeChanged(currentStartMs, newEnd)
                                    }
                                    DragMode.NONE -> {}
                                }
                            } else {
                                // Pointer lifted (Up)
                                pointer.consume()
                                activeDrag = DragMode.NONE
                                currentOnDragStateChanged(false)
                                break
                            }
                        }
                    }
                }
        )
    }
}

@Composable
private fun TimeStepperPod(
    label: String,
    timeMs: Long,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onDecrement),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Remove,
                    contentDescription = "Minus 1s",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp),
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 6.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = Color.White.copy(alpha = 0.5f),
                    maxLines = 1,
                    softWrap = false,
                )
                Text(
                    text = formatSeconds(timeMs),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 1,
                    softWrap = false,
                )
            }

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onIncrement),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Plus 1s",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun DurationChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) Color(0xFF8B5CF6) else Color.White.copy(alpha = 0.08f)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun PresetChip(
    title: String,
    subtitle: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isActive) Color(0xFF8B5CF6).copy(alpha = 0.22f) else Color.White.copy(alpha = 0.05f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isActive) Color(0xFFA78BFA) else Color.White.copy(alpha = 0.08f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold
                ),
                color = if (isActive) Color(0xFFC4B5FD) else Color.White,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = if (isActive) Color.White.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.45f),
            )
        }
    }
}

@Composable
private fun EqualizerIconWave() {
    val transition = rememberInfiniteTransition(label = "eqAnim")
    val h1 by transition.animateFloat(
        initialValue = 6f, targetValue = 20f,
        animationSpec = infiniteRepeatable(tween(400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h1",
    )
    val h2 by transition.animateFloat(
        initialValue = 18f, targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(450, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h2",
    )
    val h3 by transition.animateFloat(
        initialValue = 10f, targetValue = 22f,
        animationSpec = infiniteRepeatable(tween(350, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h3",
    )

    Row(
        modifier = Modifier.padding(end = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(width = 3.dp, height = h1.dp).background(Color(0xFFA78BFA), CircleShape))
        Box(modifier = Modifier.size(width = 3.dp, height = h2.dp).background(Color(0xFF8B5CF6), CircleShape))
        Box(modifier = Modifier.size(width = 3.dp, height = h3.dp).background(Color(0xFFA78BFA), CircleShape))
    }
}

private fun formatSeconds(timeMs: Long): String {
    val totalSeconds = (timeMs / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

