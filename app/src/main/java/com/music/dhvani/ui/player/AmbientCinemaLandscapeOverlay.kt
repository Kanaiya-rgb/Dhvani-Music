package com.music.dhvani.ui.player

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.dhvani.data.settings.AppSettings
import com.music.dhvani.ui.components.DynamicPlayerSlider
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeMute
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.music.dhvani.data.lyrics.LyricLine
import com.music.dhvani.data.model.Song
import com.music.dhvani.data.model.artworkAt
import kotlinx.coroutines.delay
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * Ambient Cinema Landscape Fullscreen Overlay
 *
 * Automatically rotates the screen to horizontal (landscape), like watching a movie
 * on YouTube in full screen.
 * - Left side: Center-aligned high-res Cover Art with ambient halo, title, artist,
 *   and sleek playback controls.
 * - Right side: Synchronized, auto-scrolling Karaoke lyrics.
 * - Edge Gestures:
 *   - Left Edge Vertical Swipe: Adjusts screen brightness (with sleek HUD indicator).
 *   - Right Edge Vertical Swipe: Adjusts media volume (isolated to far corner to prevent lyric scroll interference).
 *   - Top Center Vertical Swipe: Smoothly drags down and exits fullscreen.
 */
@Composable
fun AmbientCinemaLandscapeOverlay(
    song: Song,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    lyrics: List<LyricLine>?,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = remember(context) { context as? Activity }

    // 1. Force screen orientation to landscape on enter, restore to portrait on exit
    DisposableEffect(activity) {
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            activity?.window?.let { win ->
                val insetsController = WindowCompat.getInsetsController(win, win.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    val window = activity?.window
    DisposableEffect(window) {
        window?.let { win ->
            val insetsController = WindowCompat.getInsetsController(win, win.decorView)
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            window?.let { win ->
                val insetsController = WindowCompat.getInsetsController(win, win.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    val scope = rememberCoroutineScope()
    val animOffsetY = remember { Animatable(0f) }
    val contentAlpha = remember { Animatable(0f) }
    var isExiting by remember { mutableStateOf(false) }

    // Smooth content entrance once landscape surface has stabilized
    LaunchedEffect(Unit) {
        delay(120)
        contentAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 240, easing = LinearOutSlowInEasing)
        )
    }

    // Coordinated seamless dismissal:
    // Requests portrait & system bars first so the OS rotation transition happens
    // under the solid opaque dark backdrop without flashing any landscape app layouts!
    fun dismissCinemaMode() {
        if (isExiting) return
        isExiting = true

        // 1. Immediately request portrait orientation and restore system bars so OS starts rotating
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        activity?.window?.let { win ->
            val insetsController = WindowCompat.getInsetsController(win, win.decorView)
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }

        // 2. Animate foreground content downwards and fade out swiftly
        scope.launch {
            launch {
                contentAlpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 180, easing = FastOutLinearInEasing)
                )
            }
            launch {
                animOffsetY.animateTo(
                    targetValue = 500f,
                    animationSpec = tween(durationMillis = 200, easing = FastOutLinearInEasing)
                )
            }
            // 3. Keep the solid dark backdrop covering the screen while OS completes the 90-to-0 rotation
            delay(280)
            onDismiss()
        }
    }

    // 2. Hardware back button dismisses mode with coordinated smooth transition
    BackHandler(enabled = !isExiting) {
        dismissCinemaMode()
    }

    // 3. Slider Style & Scrubbing State (Honors user choice in settings)
    val sliderStyle by AppSettings.sliderStyle.collectAsStateWithLifecycle()
    val squigglySlider by AppSettings.squigglySlider.collectAsStateWithLifecycle()
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubFraction by remember { mutableFloatStateOf(0f) }

    val currentFraction = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    val displayFraction = if (isScrubbing) scrubFraction else currentFraction
    val displayPositionMs = if (isScrubbing) (scrubFraction * durationMs).toLong() else positionMs

    // 4. Brightness Management
    var brightness by remember {
        val currentWinBrightness = activity?.window?.attributes?.screenBrightness ?: -1f
        val initial = if (currentWinBrightness >= 0f) {
            currentWinBrightness
        } else {
            try {
                Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128) / 255f
            } catch (_: Exception) {
                0.5f
            }
        }
        mutableFloatStateOf(initial.coerceIn(0.01f, 1f))
    }

    DisposableEffect(activity) {
        onDispose {
            activity?.window?.let { win ->
                val lp = win.attributes
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                win.attributes = lp
            }
        }
    }

    var showBrightnessHud by remember { mutableStateOf(false) }
    var brightnessTimer by remember { mutableLongStateOf(0L) }
    LaunchedEffect(brightnessTimer) {
        if (brightnessTimer > 0L) {
            showBrightnessHud = true
            delay(1300)
            showBrightnessHud = false
        }
    }

    fun updateBrightness(newVal: Float) {
        val clamped = newVal.coerceIn(0.01f, 1f)
        brightness = clamped
        activity?.window?.let { win ->
            val lp = win.attributes
            lp.screenBrightness = clamped
            win.attributes = lp
        }
        brightnessTimer = System.currentTimeMillis()
    }

    // 4. Volume Management
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }
    val maxVolume = remember(audioManager) {
        audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
    }
    var currentVolume by remember(audioManager) {
        mutableIntStateOf(audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: (maxVolume / 2))
    }

    var showVolumeHud by remember { mutableStateOf(false) }
    var volumeTimer by remember { mutableLongStateOf(0L) }
    LaunchedEffect(volumeTimer) {
        if (volumeTimer > 0L) {
            showVolumeHud = true
            delay(1300)
            showVolumeHud = false
        }
    }

    fun updateVolume(delta: Int) {
        val newVol = (currentVolume + delta).coerceIn(0, maxVolume)
        if (newVol != currentVolume) {
            currentVolume = newVol
            audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
        }
        volumeTimer = System.currentTimeMillis()
    }

    // Root container: Stationary, 100% opaque solid background
    // Covers the physical screen completely so rotating or dragging never reveals underlying app content!
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF06080F)),
    ) {
        // Full-screen ambient blurred artwork backdrop (downscaled for 60/120fps smooth render)
        // PINNED TO SCREEN: stays stationary so dragging down or rotating never reveals the app behind!
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(song.artworkAt(180) ?: song.thumbnailUrl)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(28.dp),
        )

        // Frosted dark overlay gradient (Pinned to screen)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.50f),
                            Color(0xFF06080F).copy(alpha = 0.72f),
                            Color(0xFF030408).copy(alpha = 0.92f),
                        )
                    )
                ),
        )

        // Main Interactive Foreground Content Container
        // ONLY this container moves with the swipe-down gesture and dims gracefully
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, animOffsetY.value.roundToInt()) }
                .alpha(contentAlpha.value * (1f - (animOffsetY.value / 420f)).coerceIn(0f, 1f)),
        ) {
            // Main Horizontal Split View (Left: Artwork & Controls | Right: Live Synced Lyrics)
            Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 44.dp, bottom = 12.dp, start = 24.dp, end = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // LEFT COLUMN: Center-aligned Cover Art + Song Info + Playback Controls
            Column(
                modifier = Modifier
                    .weight(0.42f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Artwork with ambient shadow glow
                Box(contentAlignment = Alignment.Center) {
                    // Ambient halo behind card (smooth radial gradient without GPU blur overhead)
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                        Color.Transparent,
                                    )
                                ),
                                CircleShape,
                            ),
                    )

                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.25f)),
                        shadowElevation = 20.dp,
                        modifier = Modifier.size(190.dp),
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(song.artworkAt(800) ?: song.thumbnailUrl)
                                .build(),
                            contentDescription = song.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Song Title & Artist
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(0.90f),
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.5.sp,
                    ),
                    color = Color.White.copy(alpha = 0.70f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(0.90f),
                )

                Spacer(Modifier.height(6.dp))

                // Dynamic Player Slider (Honors user's chosen SliderStyle: Wavy, Squiggly, Neon, Cosmic, etc.)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .height(34.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    DynamicPlayerSlider(
                        value = displayFraction,
                        onValueChange = { frac ->
                            isScrubbing = true
                            scrubFraction = frac
                        },
                        onValueChangeFinished = {
                            onSeek((scrubFraction * durationMs).toLong())
                            isScrubbing = false
                        },
                        isPlaying = isPlaying,
                        sliderStyle = sliderStyle,
                        squigglySlider = squigglySlider,
                        activeColor = MaterialTheme.colorScheme.primary,
                        inactiveColor = Color.White.copy(alpha = 0.28f),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = formatTime(displayPositionMs),
                        fontSize = 10.5.sp,
                        color = Color.White.copy(alpha = 0.60f),
                    )
                    Text(
                        text = formatTime(durationMs),
                        fontSize = 10.5.sp,
                        color = Color.White.copy(alpha = 0.60f),
                    )
                }

                Spacer(Modifier.height(4.dp))

                // Transport Controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onPrevious) {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = "Previous",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(28.dp),
                        )
                    }

                    // Play / Pause glowing pill
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .size(46.dp)
                            .clickable { onPlayPause() },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                    }

                    IconButton(onClick = onNext) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Next",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
            }

            // RIGHT COLUMN: Synchronized Live Karaoke Lyrics (Centered in Right Half)
            Column(
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxHeight()
                    .padding(start = 20.dp, end = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (lyrics.isNullOrEmpty()) {
                    // No lyrics fallback
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.35f),
                                modifier = Modifier.size(48.dp),
                            )
                            Text(
                                text = "Lyrics unavailable for this track",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.60f),
                            )
                        }
                    }
                } else {
                    val activeIndex = remember(lyrics, positionMs) {
                        val idx = lyrics.indexOfLast { it.timeMs <= positionMs }
                        if (idx >= 0) idx else 0
                    }

                    val listState = rememberLazyListState()

                    // Auto-scroll so the currently active singing line stays centered
                    LaunchedEffect(activeIndex) {
                        if (activeIndex in lyrics.indices) {
                            val targetIndex = (activeIndex - 1).coerceAtLeast(0)
                            listState.animateScrollToItem(
                                index = targetIndex,
                                scrollOffset = -40,
                            )
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 50.dp),
                    ) {
                        itemsIndexed(
                            lyrics,
                            key = { index, line -> "${line.timeMs}_$index" }
                        ) { index, line ->
                            val isActive = index == activeIndex
                            val isUpcoming = index > activeIndex

                            val targetAlpha = when {
                                isActive -> 1f
                                isUpcoming -> 0.42f
                                else -> 0.28f
                            }
                            val alpha by animateFloatAsState(
                                targetValue = targetAlpha,
                                animationSpec = tween(durationMillis = 200),
                                label = "lyricAlpha",
                            )

                            val fontSize = if (isActive) 23.sp else 17.5.sp
                            val fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium

                            Text(
                                text = line.text,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontSize = fontSize,
                                    fontWeight = fontWeight,
                                    lineHeight = if (isActive) 32.sp else 25.sp,
                                    shadow = if (isActive) {
                                        Shadow(
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.70f),
                                            offset = Offset(0f, 0f),
                                            blurRadius = 14f,
                                        )
                                    } else null,
                                ),
                                textAlign = TextAlign.Center,
                                color = if (isActive) Color.White else Color.White.copy(alpha = alpha),
                                modifier = Modifier
                                    .fillMaxWidth(0.94f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSeek(line.timeMs) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }
        }

        // Left Edge Gesture Strip: Brightness (starts below top header so exit button is never blocked)
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .padding(top = 56.dp, bottom = 16.dp)
                .width(54.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            val delta = -dragAmount / 350f
                            updateBrightness(brightness + delta)
                        }
                    )
                }
        )

        // Right Edge Gesture Strip: Volume (starts below top header and isolated to 54dp outer strip)
        var volumeDragAccumulator by remember { mutableFloatStateOf(0f) }
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(top = 56.dp, bottom = 16.dp)
                .width(54.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            volumeDragAccumulator += -dragAmount
                            val stepPx = 20f
                            if (volumeDragAccumulator >= stepPx) {
                                val steps = (volumeDragAccumulator / stepPx).toInt()
                                updateVolume(steps)
                                volumeDragAccumulator -= steps * stepPx
                            } else if (volumeDragAccumulator <= -stepPx) {
                                val steps = ((-volumeDragAccumulator) / stepPx).toInt()
                                updateVolume(-steps)
                                volumeDragAccumulator += steps * stepPx
                            }
                        },
                        onDragEnd = {
                            volumeDragAccumulator = 0f
                        },
                        onDragCancel = {
                            volumeDragAccumulator = 0f
                        }
                    )
                }
        )

        // Floating HUD: Brightness (Left side)
        AnimatedVisibility(
            visible = showBrightnessHud,
            enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.9f),
            exit = fadeOut(tween(250)) + scaleOut(targetScale = 0.9f),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 64.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xDD121420),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                shadowElevation = 16.dp,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WbSunny,
                        contentDescription = "Brightness",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .height(90.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.20f)),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(brightness.coerceIn(0.01f, 1f))
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                        )
                    }
                    Text(
                        text = "${(brightness * 100).roundToInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }
        }

        // Floating HUD: Volume (Right side)
        AnimatedVisibility(
            visible = showVolumeHud,
            enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.9f),
            exit = fadeOut(tween(250)) + scaleOut(targetScale = 0.9f),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 64.dp),
        ) {
            val volFraction = if (maxVolume > 0) (currentVolume.toFloat() / maxVolume.toFloat()).coerceIn(0f, 1f) else 0f
            val volIcon = when {
                currentVolume == 0 -> Icons.AutoMirrored.Rounded.VolumeMute
                currentVolume < maxVolume / 2 -> Icons.AutoMirrored.Rounded.VolumeDown
                else -> Icons.AutoMirrored.Rounded.VolumeUp
            }
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xDD121420),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                shadowElevation = 16.dp,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp),
                ) {
                    Icon(
                        imageVector = volIcon,
                        contentDescription = "Volume",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .height(90.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.20f)),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(volFraction)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                        )
                    }
                    Text(
                        text = "${(volFraction * 100).roundToInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }
        }

        // Top Drag Handle & Exit Controls (Centered "Swipe down to exit") - Topmost layer (zIndex 10)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(10f)
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .pointerInput(isExiting) {
                    if (isExiting) return@pointerInput
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            val next = (animOffsetY.value + dragAmount).coerceAtLeast(0f)
                            scope.launch { animOffsetY.snapTo(next) }
                        },
                        onDragEnd = {
                            if (animOffsetY.value > 90f) {
                                dismissCinemaMode()
                            } else {
                                scope.launch {
                                    animOffsetY.animateTo(
                                        0f,
                                        spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                    )
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                animOffsetY.animateTo(0f)
                            }
                        },
                    )
                },
        ) {
            // Exit button (Tap to close on left)
            IconButton(
                onClick = { dismissCinemaMode() },
                enabled = !isExiting,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(42.dp)
                    .background(Color.White.copy(alpha = 0.16f), CircleShape)
                    .border(BorderStroke(0.8.dp, Color.White.copy(alpha = 0.25f)), CircleShape),
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Exit Fullscreen",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }

            // Top drag handle indicator ("Swipe down to exit") - Centered with vertical drag detection
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = !isExiting) { dismissCinemaMode() }
                    .padding(horizontal = 32.dp, vertical = 6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.45f)),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Swipe down to exit",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp,
                    color = Color.White.copy(alpha = 0.55f),
                )
            }
        }
    }
}
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ms)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return "%d:%02d".format(Locale.ROOT, minutes, seconds)
}
