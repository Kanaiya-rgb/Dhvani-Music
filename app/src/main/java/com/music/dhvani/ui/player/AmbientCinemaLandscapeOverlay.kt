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
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeMute
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
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
    queue: List<Song> = emptyList(),
    queueIndex: Int = 0,
    nextSong: Song? = null,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onJumpTo: (Int) -> Unit = {},
    onRemoveFromQueue: (Int) -> Unit = {},
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

    // 4. Volume Management (Continuous fine-grained 1% to 2% precision)
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }
    val maxVolume = remember(audioManager) {
        audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
    }
    var currentVolume by remember(audioManager) {
        mutableIntStateOf(audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: (maxVolume / 2))
    }
    val initialVolPercent = remember(audioManager, maxVolume) {
        if (maxVolume > 0) (audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: (maxVolume / 2)).toFloat() / maxVolume.toFloat() else 0.5f
    }
    var smoothVolumePercent by remember { mutableFloatStateOf(initialVolPercent) }

    var showVolumeHud by remember { mutableStateOf(false) }
    var volumeTimer by remember { mutableLongStateOf(0L) }
    LaunchedEffect(volumeTimer) {
        if (volumeTimer > 0L) {
            showVolumeHud = true
            delay(1300)
            showVolumeHud = false
        }
    }

    fun updateSmoothVolume(deltaFraction: Float) {
        val newPercent = (smoothVolumePercent + deltaFraction).coerceIn(0f, 1f)
        smoothVolumePercent = newPercent
        val targetSysVol = if (maxVolume > 0) (newPercent * maxVolume).roundToInt().coerceIn(0, maxVolume) else 0
        if (targetSysVol != currentVolume) {
            currentVolume = targetSysVol
            audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, targetSysVol, 0)
        }
        volumeTimer = System.currentTimeMillis()
    }

    // 5. Controls Visibility (Clean view by default, tap left side to show/hide with 5s auto-hide)
    var areControlsVisible by remember { mutableStateOf(false) }
    var controlsTimer by remember { mutableLongStateOf(0L) }
    LaunchedEffect(controlsTimer) {
        if (controlsTimer > 0L && areControlsVisible) {
            delay(5000)
            areControlsVisible = false
        }
    }

    // 6. Queue Panel state (Slide-out Queue drawer with full playlist and song removal)
    var showQueuePanel by remember { mutableStateOf(false) }

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
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, animOffsetY.value.roundToInt()) }
                .alpha(contentAlpha.value * (1f - (animOffsetY.value / 420f)).coerceIn(0f, 1f)),
        ) {
            val totalHeight = maxHeight
            val isVeryCompact = totalHeight < 360.dp
            val isCompact = totalHeight < 410.dp

            val topPadding = when {
                isVeryCompact -> 34.dp
                isCompact -> 38.dp
                else -> 44.dp
            }
            val bottomPadding = when {
                isVeryCompact -> 8.dp
                isCompact -> 12.dp
                else -> 16.dp
            }
            val horizontalPadding = when {
                isVeryCompact -> 20.dp
                isCompact -> 24.dp
                else -> 28.dp
            }
            val columnSpacing = when {
                isVeryCompact -> 16.dp
                isCompact -> 22.dp
                else -> 28.dp
            }

            val availableColHeight = totalHeight - topPadding - bottomPadding

            // Dynamically scale artwork based on device screen height & controls visibility
            val targetArtworkSize = when {
                !areControlsVisible -> when {
                    availableColHeight < 280.dp -> 170.dp
                    availableColHeight < 330.dp -> 205.dp
                    availableColHeight < 380.dp -> 236.dp
                    else -> 265.dp
                }
                else -> when {
                    availableColHeight < 260.dp -> 126.dp
                    availableColHeight < 300.dp -> 146.dp
                    availableColHeight < 360.dp -> 168.dp
                    availableColHeight < 410.dp -> 188.dp
                    else -> 212.dp
                }
            }
            val artworkSize by animateDpAsState(
                targetValue = targetArtworkSize,
                animationSpec = tween(durationMillis = 280, easing = LinearOutSlowInEasing),
                label = "artworkSize",
            )
            val haloSize = artworkSize + 22.dp
            val artworkShapeRadius = if (isCompact) 20.dp else 24.dp

            // Proportionate, compact slider width aligned with artwork
            val sliderWidth = (artworkSize + 28.dp).coerceIn(180.dp, 255.dp)

            // Spacers between elements
            val artworkToTitleGap = when {
                isVeryCompact -> 6.dp
                isCompact -> 8.dp
                else -> 12.dp
            }
            val artistToSliderGap = when {
                isVeryCompact -> 2.dp
                isCompact -> 4.dp
                else -> 6.dp
            }
            val sliderToControlsGap = when {
                isVeryCompact -> 4.dp
                isCompact -> 6.dp
                else -> 8.dp
            }

            // Typography
            val titleFontSize = when {
                isVeryCompact -> 14.5.sp
                isCompact -> 15.5.sp
                else -> 17.sp
            }
            val artistFontSize = when {
                isVeryCompact -> 11.sp
                isCompact -> 11.5.sp
                else -> 12.5.sp
            }

            // Slider height
            val sliderBoxHeight = if (isCompact) 28.dp else 34.dp

            // Controls buttons
            val playPausePillSize = when {
                isVeryCompact -> 38.dp
                isCompact -> 42.dp
                else -> 46.dp
            }
            val playPauseIconSize = when {
                isVeryCompact -> 22.dp
                isCompact -> 24.dp
                else -> 26.dp
            }
            val skipButtonSize = when {
                isVeryCompact -> 36.dp
                isCompact -> 40.dp
                else -> 48.dp
            }
            val skipIconSize = when {
                isVeryCompact -> 22.dp
                isCompact -> 24.dp
                else -> 28.dp
            }
            val controlsSpacing = when {
                isVeryCompact -> 14.dp
                isCompact -> 18.dp
                else -> 20.dp
            }

            // Main Horizontal Split View (Left: Artwork & Controls | Right: Live Synced Lyrics)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = topPadding,
                        bottom = bottomPadding,
                        start = horizontalPadding,
                        end = horizontalPadding,
                    ),
                horizontalArrangement = Arrangement.spacedBy(columnSpacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // LEFT COLUMN: Center-aligned Cover Art + Song Info + Playback Controls
                Column(
                    modifier = Modifier
                        .weight(0.42f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(24.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            areControlsVisible = !areControlsVisible
                            if (areControlsVisible) {
                                controlsTimer = System.currentTimeMillis()
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    // Artwork with ambient shadow glow
                    Box(contentAlignment = Alignment.Center) {
                        // Ambient halo behind card (smooth radial gradient without GPU blur overhead)
                        Box(
                            modifier = Modifier
                                .size(haloSize)
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
                            shape = RoundedCornerShape(artworkShapeRadius),
                            border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.25f)),
                            shadowElevation = if (isCompact) 12.dp else 20.dp,
                            modifier = Modifier.size(artworkSize),
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

                    Spacer(Modifier.height(artworkToTitleGap))

                    // Song Title & Artist (Smooth auto-marquee moving text effect when text overflows)
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = titleFontSize,
                        ),
                        color = Color.White,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                initialDelayMillis = 1500,
                                repeatDelayMillis = 1200,
                                velocity = 32.dp,
                            ),
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = artistFontSize,
                        ),
                        color = Color.White.copy(alpha = 0.70f),
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                initialDelayMillis = 1500,
                                repeatDelayMillis = 1200,
                                velocity = 30.dp,
                            ),
                    )

                    // Player Slider & Playback Transport Buttons (Shown on tap, hidden by default for clean view)
                    AnimatedVisibility(
                        visible = areControlsVisible,
                        enter = fadeIn(tween(220)) + expandVertically(tween(220)),
                        exit = fadeOut(tween(180)) + shrinkVertically(tween(180)),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Spacer(Modifier.height(artistToSliderGap))

                            // Dynamic Player Slider (Proportionate compact width aligned with artwork)
                            Box(
                                modifier = Modifier
                                    .width(sliderWidth)
                                    .height(sliderBoxHeight),
                                contentAlignment = Alignment.Center,
                            ) {
                                DynamicPlayerSlider(
                                    value = displayFraction,
                                    onValueChange = { frac ->
                                        isScrubbing = true
                                        scrubFraction = frac
                                        controlsTimer = System.currentTimeMillis()
                                    },
                                    onValueChangeFinished = {
                                        onSeek((scrubFraction * durationMs).toLong())
                                        isScrubbing = false
                                        controlsTimer = System.currentTimeMillis()
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
                                    .width(sliderWidth)
                                    .padding(horizontal = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = formatTime(displayPositionMs),
                                    fontSize = if (isCompact) 9.5.sp else 10.5.sp,
                                    color = Color.White.copy(alpha = 0.60f),
                                )
                                Text(
                                    text = formatTime(durationMs),
                                    fontSize = if (isCompact) 9.5.sp else 10.5.sp,
                                    color = Color.White.copy(alpha = 0.60f),
                                )
                            }

                            Spacer(Modifier.height(3.dp))

                            // Transport Controls
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(controlsSpacing),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                IconButton(
                                    onClick = {
                                        controlsTimer = System.currentTimeMillis()
                                        onPrevious()
                                    },
                                    modifier = Modifier.size(skipButtonSize),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.SkipPrevious,
                                        contentDescription = "Previous",
                                        tint = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.size(skipIconSize),
                                    )
                                }

                                // Play / Pause glowing pill
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    shadowElevation = if (isCompact) 6.dp else 8.dp,
                                    modifier = Modifier
                                        .size(playPausePillSize)
                                        .clickable {
                                            controlsTimer = System.currentTimeMillis()
                                            onPlayPause()
                                        },
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                            contentDescription = if (isPlaying) "Pause" else "Play",
                                            tint = Color.Black,
                                            modifier = Modifier.size(playPauseIconSize),
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        controlsTimer = System.currentTimeMillis()
                                        onNext()
                                    },
                                    modifier = Modifier.size(skipButtonSize),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.SkipNext,
                                        contentDescription = "Next",
                                        tint = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.size(skipIconSize),
                                    )
                                }
                            }
                        }
                    }
                }

                // RIGHT COLUMN: Synchronized Live Karaoke Lyrics (Centered in Right Half)
                Column(
                    modifier = Modifier
                        .weight(0.58f)
                        .fillMaxHeight()
                        .padding(start = if (isVeryCompact) 10.dp else 18.dp, end = 10.dp),
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
                                    modifier = Modifier.size(if (isCompact) 36.dp else 48.dp),
                                )
                                Text(
                                    text = "Lyrics unavailable for this track",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = if (isCompact) 14.sp else 16.sp,
                                    ),
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
                            verticalArrangement = Arrangement.spacedBy(if (isCompact) 14.dp else 18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = if (isCompact) 32.dp else 52.dp),
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

                                val fontSize = if (isActive) (if (isCompact) 23.sp else 29.sp) else (if (isCompact) 16.5.sp else 21.sp)
                                val fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium

                                Text(
                                    text = line.text,
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontSize = fontSize,
                                        fontWeight = fontWeight,
                                        lineHeight = if (isActive) (if (isCompact) 32.sp else 39.sp) else (if (isCompact) 23.sp else 29.sp),
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

        // Right Edge Gesture Strip: Volume (starts below top header and isolated to 48dp outer strip)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(top = 56.dp, bottom = 56.dp)
                .width(48.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            // Smooth continuous 1% to 2% precision per drag step
                            val delta = -dragAmount / 450f
                            updateSmoothVolume(delta)
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
            val volIcon = when {
                smoothVolumePercent <= 0.01f -> Icons.AutoMirrored.Rounded.VolumeMute
                smoothVolumePercent < 0.5f -> Icons.AutoMirrored.Rounded.VolumeDown
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
                                .fillMaxHeight(smoothVolumePercent.coerceIn(0.01f, 1f))
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                        )
                    }
                    Text(
                        text = "${(smoothVolumePercent * 100).roundToInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }
        }

        // Dim Scrim Backdrop when Queue Drawer is open
        AnimatedVisibility(
            visible = showQueuePanel,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(180)),
            modifier = Modifier.fillMaxSize().zIndex(18f),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { showQueuePanel = false },
            )
        }

        // Full Interactive Queue Drawer (Slides in from the right side)
        AnimatedVisibility(
            visible = showQueuePanel,
            enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(280, easing = LinearOutSlowInEasing)) + fadeIn(tween(200)),
            exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(240, easing = FastOutLinearInEasing)) + fadeOut(tween(180)),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(360.dp)
                .zIndex(20f),
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
                color = Color(0xF5111422),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                shadowElevation = 24.dp,
                modifier = Modifier.fillMaxSize(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 16.dp, bottom = 14.dp, start = 16.dp, end = 16.dp)
                ) {
                    // Header Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "Queue",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                ),
                                color = Color.White,
                            )
                            if (queue.isNotEmpty()) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                                ) {
                                    Text(
                                        text = "${queue.size}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = { showQueuePanel = false },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close Queue",
                                tint = Color.White.copy(alpha = 0.70f),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    if (queue.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No songs in queue",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.50f)
                            )
                        }
                    } else {
                        val queueListState = rememberLazyListState()
                        LaunchedEffect(showQueuePanel, queueIndex) {
                            if (showQueuePanel && queueIndex in queue.indices) {
                                queueListState.scrollToItem(queueIndex.coerceAtLeast(0))
                            }
                        }

                        LazyColumn(
                            state = queueListState,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            itemsIndexed(
                                queue,
                                key = { index, s -> "${s.videoId}_$index" }
                            ) { index, queueSong ->
                                val isCurrent = index == queueIndex
                                val isPast = index < queueIndex

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = when {
                                        isCurrent -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                        else -> Color.White.copy(alpha = 0.04f)
                                    },
                                    border = if (isCurrent) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)) else null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            onJumpTo(index)
                                        },
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        // Index or Equalizer Playing indicator
                                        Box(
                                            modifier = Modifier.width(24.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isCurrent) {
                                                Icon(
                                                    imageVector = Icons.Rounded.GraphicEq,
                                                    contentDescription = "Playing",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp),
                                                )
                                            } else {
                                                Text(
                                                    text = "${index + 1}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color.White.copy(alpha = if (isPast) 0.30f else 0.50f),
                                                )
                                            }
                                        }

                                        Spacer(Modifier.width(6.dp))

                                        // Artwork thumbnail
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(queueSong.artworkAt(120) ?: queueSong.thumbnailUrl)
                                                .build(),
                                            contentDescription = queueSong.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                        )

                                        Spacer(Modifier.width(10.dp))

                                        // Title & Artist
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = queueSong.title,
                                                fontSize = 13.sp,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                                                color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = if (isPast) 0.55f else 0.95f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                text = queueSong.artist,
                                                fontSize = 11.sp,
                                                color = Color.White.copy(alpha = if (isPast) 0.35f else 0.60f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }

                                        // Cross icon to remove song from queue
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .clickable { onRemoveFromQueue(index) },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = "Remove from Queue",
                                                tint = Color.White.copy(alpha = 0.55f),
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom-Right Corner: Floating Queue Toggle Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 12.dp, end = 18.dp)
                .zIndex(10f),
        ) {
            Surface(
                shape = CircleShape,
                color = if (showQueuePanel) MaterialTheme.colorScheme.primary else Color(0x99202434),
                border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.25f)),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .size(42.dp)
                    .clickable {
                        showQueuePanel = !showQueuePanel
                    },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                        contentDescription = "Queue",
                        tint = if (showQueuePanel) Color.Black else Color.White.copy(alpha = 0.90f),
                        modifier = Modifier.size(22.dp),
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
