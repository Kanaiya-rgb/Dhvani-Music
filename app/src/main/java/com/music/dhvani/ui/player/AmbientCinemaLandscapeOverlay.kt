package com.music.dhvani.ui.player

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.dhvani.data.settings.AppSettings
import com.music.dhvani.ui.components.DynamicPlayerSlider
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.DragInteraction
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.withFrameNanos
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.music.dhvani.R
import android.os.Build
import android.widget.Toast
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.text.font.FontStyle
import com.music.dhvani.data.lyrics.LyricLine
import com.music.dhvani.data.lyrics.LyricsRomanization
import com.music.dhvani.ui.haptics.Haptic
import com.music.dhvani.ui.haptics.rememberHaptics
import com.music.dhvani.data.NerdStats
import com.music.dhvani.data.model.Song
import com.music.dhvani.data.model.artworkAt
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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
    isLoading: Boolean = false,
    positionMs: Long,
    bufferedPositionMs: Long = 0L,
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
    val window = activity?.window

    // Force screen orientation to landscape on enter, restore to portrait on exit.
    // True immersive fullscreen: hides status bar (time/date/notifications) and navigation bar.
    DisposableEffect(activity, window) {
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        val win = window
        val originalCutoutMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            win?.attributes?.layoutInDisplayCutoutMode ?: 0
        } else 0
        @Suppress("DEPRECATION")
        val originalSystemUi = win?.decorView?.systemUiVisibility ?: 0

        win?.let { w ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                w.attributes = w.attributes.apply {
                    layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }
            @Suppress("DEPRECATION")
            w.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            val insetsController = WindowCompat.getInsetsController(w, w.decorView)
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())

            @Suppress("DEPRECATION")
            w.decorView.systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )

            val insetsListener = android.view.View.OnApplyWindowInsetsListener { v, insets ->
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                v.onApplyWindowInsets(insets)
            }
            w.decorView.setOnApplyWindowInsetsListener(insetsListener)
        }

        onDispose {
            activity?.requestedOrientation = originalOrientation
            win?.let { w ->
                w.decorView.setOnApplyWindowInsetsListener(null)
                @Suppress("DEPRECATION")
                w.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                w.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    w.attributes = w.attributes.apply {
                        layoutInDisplayCutoutMode = originalCutoutMode
                    }
                }
                @Suppress("DEPRECATION")
                w.decorView.systemUiVisibility = originalSystemUi
                val insetsController = WindowCompat.getInsetsController(w, w.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // Re-enforce immersive hiding whenever orientation changes or layout settles
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    LaunchedEffect(configuration.orientation) {
        window?.let { w ->
            @Suppress("DEPRECATION")
            w.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            val insetsController = WindowCompat.getInsetsController(w, w.decorView)
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())

            @Suppress("DEPRECATION")
            w.decorView.systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
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
            win.decorView.setOnApplyWindowInsetsListener(null)
            @Suppress("DEPRECATION")
            win.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            win.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val reduceAnimation by AppSettings.reduceAnimation.collectAsStateWithLifecycle()
    val nerdStats by NerdStats.current.collectAsStateWithLifecycle()

    // Real-time audio quality tag (e.g. FLAC, HI-RES, OPUS, AAC, HQ)
    val qualityTagInfo = remember(nerdStats) {
        val stats = nerdStats
        when {
            stats?.isHiRes == true -> {
                Triple(
                    "HI-RES",
                    Color(0xFF2A2210).copy(alpha = 0.88f),
                    Color(0xFFFFD56B),
                )
            }
            stats?.isLossless == true -> {
                val label = when {
                    stats.mimeType?.contains("alac", ignoreCase = true) == true -> "ALAC"
                    stats.mimeType?.contains("wav", ignoreCase = true) == true -> "WAV"
                    else -> "FLAC"
                }
                Triple(
                    label,
                    Color.White.copy(alpha = 0.16f),
                    Color.White,
                )
            }
            stats?.isDolbyAtmos == true -> {
                Triple(
                    "ATMOS",
                    Color(0xFF101C30).copy(alpha = 0.88f),
                    Color(0xFF8AB8FF),
                )
            }
            stats?.isHiQuality == true -> {
                val codec = when {
                    stats.mimeType?.contains("mp4a", ignoreCase = true) == true || stats.mimeType?.contains("aac", ignoreCase = true) == true -> "AAC"
                    stats.mimeType?.contains("mp3", ignoreCase = true) == true -> "MP3"
                    else -> null
                }
                Triple(
                    if (codec != null) "HQ $codec" else "HQ",
                    Color(0xFF0F261C).copy(alpha = 0.88f),
                    Color(0xFF86EFAC),
                )
            }
            stats?.mimeType != null -> {
                val label = when {
                    stats.mimeType.contains("opus", ignoreCase = true) || stats.decoderName?.contains("opus", ignoreCase = true) == true -> "OPUS"
                    stats.mimeType.contains("mp4a", ignoreCase = true) || stats.mimeType.contains("aac", ignoreCase = true) -> "AAC"
                    stats.mimeType.contains("mp3", ignoreCase = true) || stats.mimeType.contains("mpeg", ignoreCase = true) -> "MP3"
                    else -> stats.mimeType.substringAfter('/').substringBefore(';').uppercase(Locale.ROOT).take(5)
                }
                Triple(
                    label,
                    Color.White.copy(alpha = 0.12f),
                    Color.White.copy(alpha = 0.82f),
                )
            }
            stats?.claimed?.codec != null -> {
                Triple(
                    stats.claimed.codec.uppercase(Locale.ROOT),
                    Color.White.copy(alpha = 0.12f),
                    Color.White.copy(alpha = 0.82f),
                )
            }
            else -> {
                Triple(
                    "OPUS",
                    Color.White.copy(alpha = 0.12f),
                    Color.White.copy(alpha = 0.82f),
                )
            }
        }
    }
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubFraction by remember { mutableFloatStateOf(0f) }

    val currentFraction = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    val displayFraction = if (isScrubbing) scrubFraction else currentFraction
    val displayPositionMs = if (isScrubbing) (scrubFraction * durationMs).toLong() else positionMs
    val bufferedFraction = if (durationMs > 0) (bufferedPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

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
        audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)?.coerceAtLeast(1) ?: 15
    }
    var currentVolume by remember(audioManager) {
        mutableIntStateOf(audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: (maxVolume / 2))
    }
    val initialVolPercent = remember(audioManager, maxVolume) {
        val initialStream = if (maxVolume > 0) {
            (audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: (maxVolume / 2)).toFloat() / maxVolume.toFloat()
        } else 0.5f
        initialStream.coerceIn(0f, 1f)
    }
    var smoothVolumePercent by remember { mutableFloatStateOf(initialVolPercent) }
    var isVolumeDragging by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        com.music.dhvani.playback.eq.EqualizerManager.setLoudnessEnabled(false)
        com.music.dhvani.playback.eq.EqualizerManager.setLoudnessBoostMb(0)
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

    // Hardware volume keys and system panel observer
    DisposableEffect(audioManager) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                if (isVolumeDragging) return
                val current = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: return
                val fraction = (current.toFloat() / maxVolume).coerceIn(0f, 1f)
                smoothVolumePercent = fraction
                currentVolume = current
                volumeTimer = System.currentTimeMillis()
            }
        }
        val uri = Settings.System.CONTENT_URI
        val resolver = context.contentResolver
        try {
            resolver.registerContentObserver(uri, true, observer)
        } catch (_: SecurityException) {
        }
        onDispose {
            try {
                resolver.unregisterContentObserver(observer)
            } catch (_: Exception) {
            }
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
        com.music.dhvani.playback.eq.EqualizerManager.setLoudnessEnabled(false)
        com.music.dhvani.playback.eq.EqualizerManager.setLoudnessBoostMb(0)
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

    val haptics = rememberHaptics()

    // Romanization & Phonetic Pronunciation State (Same as NowPlayingScreen)
    val persistentRomanization by com.music.dhvani.data.settings.AppSettings.lyricsRomanization.collectAsStateWithLifecycle()
    var showingRomanization by remember(song.videoId) { mutableStateOf(persistentRomanization) }
    var romanizationLoading by remember(song.videoId) { mutableStateOf(false) }
    var romanizationStatus by remember(song.videoId) { mutableStateOf<String?>(null) }
    var romanizedLyrics by remember(song.videoId) {
        mutableStateOf<List<LyricLine>?>(
            lyrics?.let { LyricsRomanization.getCached(song.videoId, it) }
        )
    }

    LaunchedEffect(song.videoId, showingRomanization, lyrics) {
        if (showingRomanization && !lyrics.isNullOrEmpty()) {
            val cached = LyricsRomanization.getCached(song.videoId, lyrics)
            if (cached != null) {
                romanizedLyrics = cached
                romanizationLoading = false
                return@LaunchedEffect
            }
            if (romanizedLyrics == null && !romanizationLoading) {
                romanizationLoading = true
                when (val res = LyricsRomanization.romanize(
                    context = context.applicationContext,
                    trackId = song.videoId,
                    lines = lyrics,
                )) {
                    is LyricsRomanization.Result.Romanized -> {
                        romanizedLyrics = res.lines
                        romanizationLoading = false
                    }
                    is LyricsRomanization.Result.AlreadyLatin -> {
                        romanizationLoading = false
                        romanizationStatus = "Lyrics already in Latin script"
                    }
                    LyricsRomanization.Result.Unavailable -> {
                        romanizationLoading = false
                        romanizationStatus = "Pronunciation unavailable"
                    }
                }
            }
        }
    }

    val toggleRomanization: () -> Unit = {
        romanizationStatus = null
        if (showingRomanization) {
            showingRomanization = false
            com.music.dhvani.data.settings.AppSettings.setLyricsRomanization(false)
            haptics.play(Haptic.Select)
        } else if (romanizedLyrics != null) {
            showingRomanization = true
            com.music.dhvani.data.settings.AppSettings.setLyricsRomanization(true)
            haptics.play(Haptic.Select)
        } else {
            showingRomanization = true
            com.music.dhvani.data.settings.AppSettings.setLyricsRomanization(true)
            val cached = lyrics?.let { LyricsRomanization.getCached(song.videoId, it) }
            if (cached != null) {
                romanizedLyrics = cached
                haptics.play(Haptic.ToggleOn)
            } else {
                val source = lyrics.orEmpty()
                if (source.isNotEmpty()) {
                    romanizationLoading = true
                    haptics.play(Haptic.Tap)
                    scope.launch {
                        when (val res = LyricsRomanization.romanize(
                            context = context.applicationContext,
                            trackId = song.videoId,
                            lines = source,
                        )) {
                            is LyricsRomanization.Result.Romanized -> {
                                romanizedLyrics = res.lines
                                showingRomanization = true
                                romanizationLoading = false
                                haptics.play(Haptic.ToggleOn)
                            }
                            is LyricsRomanization.Result.AlreadyLatin -> {
                                romanizationLoading = false
                                romanizationStatus = "Lyrics already in Latin script"
                                Toast.makeText(
                                    context,
                                    "Lyrics are already in English / Roman script",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                            LyricsRomanization.Result.Unavailable -> {
                                romanizationLoading = false
                                romanizationStatus = "Pronunciation unavailable"
                                Toast.makeText(
                                    context,
                                    "Pronunciation unavailable for this song",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        }
                    }
                }
            }
        }
    }

    val displayedLyrics = remember(lyrics, showingRomanization, romanizedLyrics) {
        if (showingRomanization && romanizedLyrics != null && !lyrics.isNullOrEmpty()) {
            val romanMap = romanizedLyrics!!.associateBy { it.timeMs }
            lyrics.map { line ->
                val roman = romanMap[line.timeMs]?.romanizedText
                if (roman != null) line.copy(romanizedText = roman) else line
            }
        } else {
            lyrics
        }
    }

    // Root container: Stationary, 100% opaque solid background
    // Covers the physical screen completely so rotating or dragging never reveals underlying app content!
    // Consumes all unhandled touches to prevent background pass-through to underlying portrait screens/tabs!
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF06080F))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                areControlsVisible = !areControlsVisible
                if (areControlsVisible) {
                    controlsTimer = System.currentTimeMillis()
                }
            },
    ) {
        // Dynamic animated background derived from current song's album art
        DynamicBackground(
            song = song,
            isPlaying = isPlaying,
            modifier = Modifier.fillMaxSize(),
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
                    availableColHeight < 280.dp -> 180.dp
                    availableColHeight < 330.dp -> 215.dp
                    availableColHeight < 380.dp -> 245.dp
                    else -> 275.dp
                }
                else -> when {
                    availableColHeight < 260.dp -> 130.dp
                    availableColHeight < 300.dp -> 150.dp
                    availableColHeight < 360.dp -> 172.dp
                    availableColHeight < 410.dp -> 192.dp
                    else -> 218.dp
                }
            }
            val artworkSize by animateDpAsState(
                targetValue = targetArtworkSize,
                animationSpec = tween(durationMillis = 280, easing = LinearOutSlowInEasing),
                label = "artworkSize",
            )
            val haloSize = artworkSize + 24.dp

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
                    // Rotating Circular Vinyl Record with ambient halo glow
                    Box(contentAlignment = Alignment.Center) {
                        // Subtle vinyl drop shadow to lift record off the background
                        Box(
                            modifier = Modifier
                                .size(haloSize)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.45f),
                                            Color.Black.copy(alpha = 0.15f),
                                            Color.Transparent,
                                        )
                                    ),
                                    CircleShape,
                                ),
                        )

                        CinemaVinylRecord(
                            song = song,
                            isPlaying = isPlaying,
                            size = artworkSize,
                        )

                        // Buffering indicator over vinyl center (visible whether HUD controls are shown or hidden)
                        val bufferingAlpha by animateFloatAsState(
                            targetValue = if (isLoading) 1f else 0f,
                            animationSpec = tween(durationMillis = 200),
                            label = "cinemaBufferingAlpha",
                        )
                        if (bufferingAlpha > 0.01f) {
                            Box(
                                modifier = Modifier
                                    .size(artworkSize * 0.32f)
                                    .alpha(bufferingAlpha)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.65f))
                                    .border(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(artworkSize * 0.18f),
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(artworkToTitleGap))

                    // Song Title & Audio Quality Tag (Smooth auto-marquee moving text effect when text overflows)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .padding(vertical = 4.dp)
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                initialDelayMillis = 1500,
                                repeatDelayMillis = 1200,
                                velocity = 32.dp,
                            ),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = titleFontSize,
                            ),
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false,
                        )
                        qualityTagInfo.let { (tag, bgColor, accentColor) ->
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = bgColor,
                                border = BorderStroke(0.75.dp, accentColor.copy(alpha = 0.40f)),
                            ) {
                                Text(
                                    text = tag,
                                    style = TextStyle(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.6.sp,
                                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                                    ),
                                    color = accentColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                )
                            }
                        }
                    }
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
                                    bufferedValue = bufferedFraction,
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
                                        if (isLoading) {
                                            CircularProgressIndicator(
                                                color = Color.Black,
                                                strokeWidth = 2.5.dp,
                                                modifier = Modifier.size(playPauseIconSize),
                                            )
                                        } else {
                                            Icon(
                                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                                contentDescription = if (isPlaying) "Pause" else "Play",
                                                tint = Color.Black,
                                                modifier = Modifier.size(playPauseIconSize),
                                            )
                                        }
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
                        val activeIndex = remember(displayedLyrics, positionMs) {
                            val idx = displayedLyrics.orEmpty().indexOfLast { it.timeMs <= positionMs }
                            if (idx >= 0) idx else 0
                        }

                        val listState = rememberLazyListState()
                        var isUserDragging by remember { mutableStateOf(false) }
                        var isManualBrowsing by remember { mutableStateOf(false) }

                        LaunchedEffect(listState.interactionSource) {
                            listState.interactionSource.interactions.collect { interaction ->
                                when (interaction) {
                                    is DragInteraction.Start -> {
                                        isUserDragging = true
                                        isManualBrowsing = true
                                    }
                                    is DragInteraction.Stop, is DragInteraction.Cancel -> {
                                        isUserDragging = false
                                    }
                                }
                            }
                        }

                        // While manual browsing: once dragging stops and the scroll/fling finishes, wait 4s before returning to auto-scroll
                        LaunchedEffect(isManualBrowsing, isUserDragging, listState.isScrollInProgress) {
                            if (isManualBrowsing && !isUserDragging && !listState.isScrollInProgress) {
                                delay(4_000L)
                                isManualBrowsing = false
                            }
                        }

                        val isManual = isManualBrowsing || isUserDragging

                        // Auto-scroll so the currently active singing line stays centered (paused during manual scroll)
                        LaunchedEffect(activeIndex, isManual) {
                            if (!isManual && activeIndex in displayedLyrics.orEmpty().indices) {
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
                                displayedLyrics.orEmpty(),
                                key = { index, line -> "${line.timeMs}_$index" }
                            ) { index, line ->
                                val isActive = index == activeIndex

                                // Blur exactly matching NowPlayingScreen full screen lyrics:
                                // - When scrolling manually (isManual): 0.dp for ALL lines so user can read everything clearly!
                                // - While playing:
                                //   - Active line: 0.dp (sharp focus)
                                //   - Next 3 lines: 0.dp (sharp, readable upcoming)
                                //   - Lines before active line: heavily blurred (8.dp to 22.dp)
                                //   - Lines after subsequent 3 lines: heavily blurred (8.dp to 22.dp)
                                val targetBlur = if (isManual || reduceDynamicBlur || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                                    0.dp
                                } else when {
                                    activeIndex < 0 -> 0.dp
                                    isActive -> 0.dp
                                    index in (activeIndex + 1)..(activeIndex + 3) -> 0.dp
                                    index < activeIndex -> {
                                        val distBefore = activeIndex - index
                                        (8f + (distBefore - 1) * 3f).coerceIn(8f, 22f).dp
                                    }
                                    else -> {
                                        val distAfter = index - (activeIndex + 3)
                                        (8f + (distAfter - 1) * 3f).coerceIn(8f, 22f).dp
                                    }
                                }

                                val animatedBlur by animateDpAsState(
                                    targetValue = targetBlur,
                                    animationSpec = tween(durationMillis = 350),
                                    label = "cinemaLyricBlur",
                                )

                                val targetAlpha = when {
                                    isManual -> if (isActive) 0.88f else 0.50f
                                    isActive -> 1f
                                    index in (activeIndex + 1)..(activeIndex + 3) -> 0.55f
                                    index < activeIndex -> 0.30f
                                    else -> 0.22f
                                }
                                val alpha by animateFloatAsState(
                                    targetValue = targetAlpha,
                                    animationSpec = tween(durationMillis = 250),
                                    label = "lyricAlpha",
                                )

                                val fontSize = if (isActive) (if (isCompact) 23.sp else 29.sp) else (if (isCompact) 16.5.sp else 21.sp)
                                val fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .fillMaxWidth(0.94f)
                                        .then(
                                            if (animatedBlur > 0.5.dp) Modifier.blur(animatedBlur, BlurredEdgeTreatment.Unbounded)
                                            else Modifier
                                        )
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            isManualBrowsing = false
                                            isUserDragging = false
                                            onSeek(line.timeMs)
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                ) {
                                    Text(
                                        text = line.text,
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontSize = fontSize,
                                            fontWeight = fontWeight,
                                            lineHeight = if (isActive) (if (isCompact) 32.sp else 39.sp) else (if (isCompact) 23.sp else 29.sp),
                                        ),
                                        textAlign = TextAlign.Center,
                                        color = if (isActive && !isManual) Color.White else Color.White.copy(alpha = alpha),
                                    )

                                    line.romanizedText?.takeIf { it.isNotBlank() }?.let { roman ->
                                        val romanFontSize = if (isActive) (if (isCompact) 14.5.sp else 18.sp) else (if (isCompact) 12.sp else 14.sp)
                                        Text(
                                            text = roman,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontSize = romanFontSize,
                                                lineHeight = if (isActive) (if (isCompact) 20.sp else 25.sp) else (if (isCompact) 16.sp else 19.sp),
                                                fontWeight = FontWeight.Normal,
                                                fontStyle = FontStyle.Italic,
                                            ),
                                            textAlign = TextAlign.Center,
                                            color = if (isActive && !isManual) Color.White.copy(alpha = 0.88f) else Color.White.copy(alpha = alpha * 0.85f),
                                            modifier = Modifier.padding(top = 2.dp),
                                        )
                                    }
                                }
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
                        onDragStart = { isVolumeDragging = true },
                        onDragEnd = { isVolumeDragging = false },
                        onDragCancel = { isVolumeDragging = false },
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
            val primaryColor = MaterialTheme.colorScheme.primary
            val displayPercent = (smoothVolumePercent * 100).roundToInt().coerceIn(0, 100)

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
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val totalFrac = smoothVolumePercent.coerceIn(0f, 1f)
                            val totalHeight = size.height * totalFrac

                            // Base standard volume fill (0% to 100%)
                            if (totalHeight > 0f) {
                                drawRect(
                                    color = primaryColor,
                                    topLeft = Offset(0f, size.height - totalHeight),
                                    size = Size(size.width, totalHeight),
                                )
                            }
                        }
                    }
                    Text(
                        text = "$displayPercent%",
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
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {},
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
                                                if (isLoading) {
                                                    CircularProgressIndicator(
                                                        color = MaterialTheme.colorScheme.primary,
                                                        strokeWidth = 2.dp,
                                                        modifier = Modifier.size(14.dp),
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Rounded.GraphicEq,
                                                        contentDescription = "Playing",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(16.dp),
                                                    )
                                                }
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

            // Top-Right Romanization / Pronunciation Toggle Button
            val hasLyrics = !lyrics.isNullOrEmpty()
            IconButton(
                onClick = {
                    haptics.play(Haptic.Tap)
                    toggleRomanization()
                },
                enabled = !isExiting && hasLyrics,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(42.dp)
                    .alpha(if (hasLyrics) 1f else 0.4f)
                    .background(
                        if (showingRomanization) MaterialTheme.colorScheme.primary.copy(alpha = 0.32f) else Color.White.copy(alpha = 0.16f),
                        CircleShape
                    )
                    .border(
                        BorderStroke(
                            0.8.dp,
                            if (showingRomanization) MaterialTheme.colorScheme.primary.copy(alpha = 0.70f) else Color.White.copy(alpha = 0.25f)
                        ),
                        CircleShape
                    ),
            ) {
                if (romanizationLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_speaking),
                        contentDescription = if (showingRomanization) "Hide pronunciation" else "Show pronunciation (Romanize)",
                        tint = if (showingRomanization) MaterialTheme.colorScheme.primary else Color.White,
                        modifier = Modifier.size(21.dp),
                    )
                }
            }
        }
    }
}
}

/**
 * Rotating circular vinyl record presentation of the currently playing song's artwork.
 *
 * - Smooth continuous rotation running exclusively on the GPU layer via [graphicsLayer].
 * - Automatically pauses at the exact angle when playback pauses, resumes seamlessly when playback starts.
 * - Cleanly resets to upright (0°) whenever the track changes.
 * - Completely isolated from song playback position ticks to prevent stutter or animation restarts.
 * - Subtle concentric microgrooves, restrained gloss sheen, fine outer rim, and centered spindle label/hole.
 * - Graceful crossfade on track transitions, falling back to [R.drawable.app_logo] if cover art is unavailable.
 */
@Composable
private fun CinemaVinylRecord(
    song: Song,
    isPlaying: Boolean,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val rotation = remember { Animatable(0f) }

    // Reset rotation cleanly to upright when track changes
    LaunchedEffect(song.videoId) {
        rotation.snapTo(0f)
    }

    // Continuous smooth turntable rotation while actively playing
    // When paused, automatically freezes at the exact current angle without resetting!
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isActive) {
                val current = rotation.value
                rotation.animateTo(
                    targetValue = current + 360f,
                    animationSpec = tween(durationMillis = 32_000, easing = LinearEasing),
                )
            }
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = 16.dp,
                shape = CircleShape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.5f),
                spotColor = Color.Black.copy(alpha = 0.65f),
            )
            .graphicsLayer {
                // Read inside draw-phase lambda: zero recompositions, purely GPU-accelerated!
                rotationZ = rotation.value % 360f
            }
            .clip(CircleShape)
            .background(Color(0xFF0F0F13)),
        contentAlignment = Alignment.Center,
    ) {
        // 1. Current track's cover art cropped into circular record
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(song.artworkAt(800) ?: song.thumbnailUrl ?: R.drawable.app_logo)
                .crossfade(true)
                .build(),
            contentDescription = song.title,
            placeholder = painterResource(R.drawable.app_logo),
            error = painterResource(R.drawable.app_logo),
            fallback = painterResource(R.drawable.app_logo),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        // 2. Restrained vinyl microgrooves & subtle light sheen
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val maxRadius = this.size.width / 2f

            // Hairline concentric grooves (restrained so artwork stays vivid and legible)
            val grooveFractions = floatArrayOf(0.42f, 0.54f, 0.65f, 0.75f, 0.85f, 0.93f)
            val grooveStrokePx = 0.75.dp.toPx()
            val grooveShadowPx = 0.5.dp.toPx()
            for (f in grooveFractions) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.08f),
                    radius = maxRadius * f,
                    center = center,
                    style = Stroke(width = grooveStrokePx),
                )
                drawCircle(
                    color = Color.Black.copy(alpha = 0.12f),
                    radius = (maxRadius * f) - (0.5.dp.toPx()),
                    center = center,
                    style = Stroke(width = grooveShadowPx),
                )
            }

            // Soft radial gloss sheen
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.035f),
                        Color.Transparent,
                        Color.White.copy(alpha = 0.045f),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = maxRadius,
                ),
                radius = maxRadius,
                center = center,
            )
        }

        // 3. Center Spindle Label Ring & Hole (Matching layout reference)
        // Concentric outer center ring (~28% of diameter)
        Box(
            modifier = Modifier
                .size(size * 0.28f)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.22f))
                .border(1.2.dp, Color.White.copy(alpha = 0.38f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            // Dark center spindle hole (~13% of diameter)
            Box(
                modifier = Modifier
                    .size(size * 0.13f)
                    .clip(CircleShape)
                    .background(Color(0xFF141418))
                    .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
            )
        }

        // 4. Fine outer rim of the record
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(1.4.dp, Color.White.copy(alpha = 0.42f), CircleShape),
        )
    }
}



private fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ms)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return "%d:%02d".format(Locale.ROOT, minutes, seconds)
}
