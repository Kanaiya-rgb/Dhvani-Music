package com.music.dhvani.playback

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.media3.common.Player
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import coil3.compose.AsyncImage
import com.music.dhvani.MainActivity
import com.music.dhvani.data.model.Song
import com.music.dhvani.data.model.artworkAt
import androidx.compose.ui.graphics.toArgb
import com.music.dhvani.data.settings.AppSettings
import com.music.dhvani.ui.theme.ArtworkPalette
import com.music.dhvani.ui.theme.DhvaniTheme
import com.music.dhvani.ui.theme.rememberArtworkPalette
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Manages the floating system-wide Dynamic Island overlay that appears
 * over any other Android application when music is playing.
 *
 * Uses a pre-warmed persistent view architecture for instant 0ms latency,
 * butter-smooth spring physics, and zero jank.
 */
class DynamicIslandOverlayManager(
    private val context: Context,
    private val playerProvider: () -> Player?,
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var composeView: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    // State flows
    private val currentSongFlow = MutableStateFlow<Song?>(null)
    private val isPlayingFlow = MutableStateFlow(false)
    private val currentPositionFlow = MutableStateFlow(0L)
    private val durationMsFlow = MutableStateFlow(0L)
    private val isDismissedByUserFlow = MutableStateFlow(false)
    private val shouldShowFlow = MutableStateFlow(false)
    private var isOverlayAttached = false

    companion object {
        private val isAppInForegroundFlow = MutableStateFlow(true)

        fun setAppForeground(inForeground: Boolean) {
            isAppInForegroundFlow.value = inForeground
        }
    }

    init {
        // Pre-seed state from player immediately so there is zero initialization lag
        playerProvider()?.let { player ->
            currentSongFlow.value = player.currentMediaItem?.toSong()
            isPlayingFlow.value = player.isPlaying
            currentPositionFlow.value = player.currentPosition.coerceAtLeast(0L)
            durationMsFlow.value = player.duration.coerceAtLeast(0L)
        }

        // Live progress updates during active background playback
        scope.launch {
            while (isActive) {
                if (shouldShowFlow.value && isPlayingFlow.value) {
                    playerProvider()?.let { player ->
                        currentPositionFlow.value = player.currentPosition.coerceAtLeast(0L)
                        val d = player.duration
                        if (d > 0L) {
                            durationMsFlow.value = d
                        }
                    }
                }
                delay(350)
            }
        }

        // Observe settings & triggers
        scope.launch {
            AppSettings.systemDynamicIslandEnabled.collect {
                updateOverlayVisibility()
            }
        }
        scope.launch {
            isAppInForegroundFlow.collect {
                updateOverlayVisibility()
            }
        }
        scope.launch {
            isPlayingFlow.collect {
                updateOverlayVisibility()
            }
        }
        scope.launch {
            currentSongFlow.collect { song ->
                if (song != null) {
                    isDismissedByUserFlow.value = false
                }
                updateOverlayVisibility()
            }
        }
        scope.launch {
            isDismissedByUserFlow.collect {
                updateOverlayVisibility()
            }
        }
    }

    fun onSongChanged(song: Song?) {
        currentSongFlow.value = song
        playerProvider()?.let { player ->
            currentPositionFlow.value = player.currentPosition.coerceAtLeast(0L)
            durationMsFlow.value = player.duration.coerceAtLeast(0L)
        }
    }

    fun onIsPlayingChanged(isPlaying: Boolean) {
        isPlayingFlow.value = isPlaying
        if (isPlaying) {
            playerProvider()?.let { player ->
                currentPositionFlow.value = player.currentPosition.coerceAtLeast(0L)
                val d = player.duration
                if (d > 0L) durationMsFlow.value = d
            }
        }
    }

    private fun canDrawOverlay(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    private var isOverlayExpanded = false

    fun setOverlayExpanded(expanded: Boolean) {
        if (isOverlayExpanded == expanded) return
        isOverlayExpanded = expanded
        val view = composeView ?: return
        val params = view.layoutParams as? WindowManager.LayoutParams ?: return
        val density = context.resources.displayMetrics.density

        if (expanded) {
            // Allocate the exact compact expanded dimensions upfront (305dp x 120dp)
            // so Compose expands with 0 IPC relayout calls and 0 SurfaceFlinger buffer reallocations
            params.width = (305 * density).toInt()
            params.height = (120 * density).toInt()
            try {
                windowManager.updateViewLayout(view, params)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            // Wait for Compose spring contraction (~260ms) to complete before shrinking window
            scope.launch {
                delay(260)
                if (!isOverlayExpanded) {
                    val currentView = composeView ?: return@launch
                    val currentParams = currentView.layoutParams as? WindowManager.LayoutParams ?: return@launch
                    currentParams.width = WindowManager.LayoutParams.WRAP_CONTENT
                    currentParams.height = WindowManager.LayoutParams.WRAP_CONTENT
                    try {
                        windowManager.updateViewLayout(currentView, currentParams)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    @SuppressLint("InflateParams")
    private fun updateOverlayVisibility() {
        val enabled = AppSettings.systemDynamicIslandEnabled.value
        val hasPermission = canDrawOverlay()

        if (!enabled || !hasPermission) {
            shouldShowFlow.value = false
            setOverlayExpanded(false)
            detachOverlay()
            return
        }

        // Ensure the overlay view is attached and pre-warmed in memory
        ensureOverlayAttached()

        val inForeground = isAppInForegroundFlow.value
        val isPlaying = isPlayingFlow.value
        val hasSong = currentSongFlow.value != null
        val dismissed = isDismissedByUserFlow.value

        val shouldShow = !inForeground && isPlaying && hasSong && !dismissed
        if (!shouldShow && isOverlayExpanded) {
            setOverlayExpanded(false)
        }
        shouldShowFlow.value = shouldShow
    }

    private fun ensureOverlayAttached() {
        if (isOverlayAttached) return
        try {
            val owner = OverlayLifecycleOwner()
            owner.onCreate()
            owner.onStart()
            lifecycleOwner = owner

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                val statusBarHeight = getStatusBarHeight()
                y = (statusBarHeight / 3).coerceAtLeast(8)
            }

            val view = ComposeView(context).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                setViewTreeLifecycleOwner(owner)
                setViewTreeSavedStateRegistryOwner(owner)
                setViewTreeViewModelStoreOwner(owner)
                setContent {
                    DhvaniTheme(darkTheme = true) {
                        SystemDynamicIslandRoot(
                            shouldShowFlow = shouldShowFlow,
                            songFlow = currentSongFlow,
                            isPlayingFlow = isPlayingFlow,
                            positionFlow = currentPositionFlow,
                            durationFlow = durationMsFlow,
                            onExpandedChanged = { isExp ->
                                setOverlayExpanded(isExp)
                            },
                            onSeek = { targetMs ->
                                playerProvider()?.seekTo(targetMs)
                                currentPositionFlow.value = targetMs
                            },
                            onPlayPause = {
                                val player = playerProvider() ?: return@SystemDynamicIslandRoot
                                if (player.isPlaying) player.pause() else player.play()
                            },
                            onNext = {
                                playerProvider()?.seekToNextMediaItem()
                            },
                            onPrevious = {
                                playerProvider()?.seekToPreviousMediaItem()
                            },
                            onDismiss = {
                                isDismissedByUserFlow.value = true
                            },
                            onOpenApp = {
                                val intent = Intent(context, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                }
                                context.startActivity(intent)
                            },
                        )
                    }
                }
            }

            windowManager.addView(view, params)
            composeView = view
            isOverlayAttached = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun detachOverlay() {
        if (!isOverlayAttached) return
        try {
            isOverlayExpanded = false
            composeView?.let { windowManager.removeViewImmediate(it) }
            lifecycleOwner?.onDestroy()
            composeView = null
            lifecycleOwner = null
            isOverlayAttached = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun destroy() {
        detachOverlay()
        scope.cancel()
    }

    private fun getStatusBarHeight(): Int {
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else 72
    }
}

/**
 * Root Composable wrapping the dynamic island inside an AnimatedVisibility container.
 * Pre-warmed composition guarantees 0ms delay and instant spring animations.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SystemDynamicIslandRoot(
    shouldShowFlow: MutableStateFlow<Boolean>,
    songFlow: MutableStateFlow<Song?>,
    isPlayingFlow: MutableStateFlow<Boolean>,
    positionFlow: MutableStateFlow<Long>,
    durationFlow: MutableStateFlow<Long>,
    onExpandedChanged: (Boolean) -> Unit,
    onSeek: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onDismiss: () -> Unit,
    onOpenApp: () -> Unit,
) {
    val shouldShow by shouldShowFlow.collectAsState()
    val song by songFlow.collectAsState()
    val isPlaying by isPlayingFlow.collectAsState()
    val haptics = LocalHapticFeedback.current
    var expanded by remember { mutableStateOf(false) }

    // If dismissed or hidden, reset expanded mode and shrink window
    androidx.compose.runtime.LaunchedEffect(shouldShow) {
        if (!shouldShow && expanded) {
            expanded = false
            onExpandedChanged(false)
        }
    }

    AnimatedVisibility(
        visible = shouldShow && song != null,
        enter = slideInVertically(
            animationSpec = spring(
                dampingRatio = 0.72f,
                stiffness = 420f,
            ),
        ) { -it * 2 } + fadeIn(tween(140)) + scaleIn(
            initialScale = 0.78f,
            animationSpec = spring(dampingRatio = 0.72f, stiffness = 420f),
        ),
        exit = slideOutVertically(
            animationSpec = tween(160, easing = FastOutSlowInEasing),
        ) { -it * 2 } + fadeOut(tween(120)) + scaleOut(
            targetScale = 0.78f,
            animationSpec = tween(150),
        ),
    ) {
        val currentSong = song ?: return@AnimatedVisibility
        val artworkModel = remember(currentSong.videoId) {
            currentSong.artworkAt(160) ?: currentSong.thumbnailUrl
        }
        val palette = rememberArtworkPalette(
            imageUrl = currentSong.artworkAt(160) ?: currentSong.thumbnailUrl,
            dark = true,
        )

        // Track vertical drag accumulation for swipe-up detection
        var dragAccumulator by remember { mutableFloatStateOf(0f) }

        val cornerRadius by animateDpAsState(
            targetValue = if (expanded) 24.dp else 20.dp,
            animationSpec = spring(
                dampingRatio = 0.82f,
                stiffness = 450f,
            ),
            label = "overlayCornerRadius",
        )
        val shape = RoundedCornerShape(cornerRadius)

        Box(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = shape,
                    ambientColor = Color.Black.copy(alpha = 0.85f),
                    spotColor = palette.accent.copy(alpha = 0.40f),
                )
                .clip(shape)
                .background(Color(0xFF000000))
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.22f),
                            Color.White.copy(alpha = 0.05f),
                        ),
                    ),
                    shape = shape,
                )
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = 0.82f,
                        stiffness = 450f,
                    ),
                )
                // Use non-blocking draggable for swipe-up dismiss so taps are never delayed
                .draggable(
                    state = rememberDraggableState { delta ->
                        dragAccumulator += delta
                    },
                    orientation = Orientation.Vertical,
                    onDragStopped = {
                        if (dragAccumulator < -25f) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (expanded) {
                                expanded = false
                                onExpandedChanged(false)
                            }
                            onDismiss()
                        }
                        dragAccumulator = 0f
                    },
                ),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Subtle swipe up indicator notch pill at top
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .width(24.dp)
                        .height(2.5.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f)),
                )

                // High-performance Crossfade for zero dual-tree measurement overhead
                Crossfade(
                    targetState = expanded,
                    animationSpec = tween(140, easing = FastOutSlowInEasing),
                    label = "DynamicIslandCrossfade",
                ) { isExp ->
                    if (isExp) {
                        ExpandedOverlayCard(
                            song = currentSong,
                            artworkModel = artworkModel,
                            palette = palette,
                            isPlaying = isPlaying,
                            positionFlow = positionFlow,
                            durationFlow = durationFlow,
                            onSeek = onSeek,
                            onPlayPause = onPlayPause,
                            onNext = onNext,
                            onPrevious = onPrevious,
                            onCollapse = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                expanded = false
                                onExpandedChanged(false)
                            },
                            onOpenApp = onOpenApp,
                        )
                    } else {
                        CompactOverlayPill(
                            song = currentSong,
                            artworkModel = artworkModel,
                            palette = palette,
                            isPlaying = isPlaying,
                            onPlayPause = onPlayPause,
                            onNext = onNext,
                            onExpand = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onExpandedChanged(true)
                                expanded = true
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactOverlayPill(
    song: Song,
    artworkModel: Any?,
    palette: ArtworkPalette,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onExpand: () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "overlayArtSpin")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "artSpin",
    )

    Row(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onExpand,
            )
            .padding(start = 7.dp, end = 8.dp, top = 3.dp, bottom = 5.dp)
            .height(36.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        // Thumbnail
        AsyncImage(
            model = artworkModel,
            contentDescription = null,
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color(0xFF222222))
                .then(if (isPlaying) Modifier.rotate(rotation) else Modifier),
        )

        // Song title & artist - tapping expands into full card
        Column(
            modifier = Modifier
                .widthIn(min = 60.dp, max = 110.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.5.sp,
                ),
                color = Color.White.copy(alpha = 0.65f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Kinetic Audio Equalizer tinted with the artwork's accent color
        OverlayEqualizer(
            isPlaying = isPlaying,
            barColor = palette.accent,
            modifier = Modifier.padding(horizontal = 2.dp),
        )

        // Mini Play / Pause button
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color(0xFF252830))
                .clickable(onClick = onPlayPause),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
        }

        // Mini Next button
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color(0xFF252830))
                .clickable(onClick = onNext),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.SkipNext,
                contentDescription = "Next",
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun ExpandedOverlayCard(
    song: Song,
    artworkModel: Any?,
    palette: ArtworkPalette,
    isPlaying: Boolean,
    positionFlow: MutableStateFlow<Long>,
    durationFlow: MutableStateFlow<Long>,
    onSeek: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onCollapse: () -> Unit,
    onOpenApp: () -> Unit,
) {
    // Dynamic theme colors extracted from album artwork (matching NowPlayingScreen)
    val accentColor = palette.accent
    val sliderActiveColor = accentColor
    val sliderInactiveColor = remember(accentColor) {
        accentColor.copy(alpha = 0.24f)
    }

    // Play/Pause central squircle button background: rich jewel-toned accent
    val playButtonBg = remember(accentColor) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(accentColor.toArgb(), hsv)
        hsv[1] = (hsv[1] * 0.70f).coerceIn(0.35f, 0.75f)
        hsv[2] = 0.42f
        Color(android.graphics.Color.HSVToColor(hsv))
    }

    // Prev & Next circular buttons background: subtle tinted dark
    val sideButtonBg = remember(accentColor) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(accentColor.toArgb(), hsv)
        hsv[1] = (hsv[1] * 0.30f).coerceIn(0.08f, 0.25f)
        hsv[2] = 0.22f
        Color(android.graphics.Color.HSVToColor(hsv))
    }

    Column(
        modifier = Modifier
            .width(295.dp)
            .padding(start = 12.dp, end = 12.dp, top = 5.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Top row: Circular artwork with glowing ring, track title, artist, and collapse button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onOpenApp)
                .padding(vertical = 1.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Circular album art with dynamic glowing border ring
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .border(1.dp, accentColor.copy(alpha = 0.65f), CircleShape)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E1E1E)),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = artworkModel,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 10.5.sp,
                    ),
                    color = Color.White.copy(alpha = 0.65f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Collapse icon
            IconButton(
                onClick = onCollapse,
                modifier = Modifier.size(24.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowUp,
                    contentDescription = "Collapse Island",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        // Sleek Player Progress Slider with colors dynamically matched to artwork
        OverlayPlayerSlider(
            positionFlow = positionFlow,
            durationFlow = durationFlow,
            activeColor = sliderActiveColor,
            inactiveColor = sliderInactiveColor,
            onSeek = onSeek,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
        )

        // Control Buttons Row (circular prev, squircle pill play/pause, circular next)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            // Previous button (circular with subtle artwork tint)
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(sideButtonBg)
                    .clickable(onClick = onPrevious),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipPrevious,
                    contentDescription = "Previous",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }

            Spacer(Modifier.width(14.dp))

            // Play / Pause button (compact squircle pill with rich artwork accent tint)
            Box(
                modifier = Modifier
                    .width(52.dp)
                    .height(34.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(playButtonBg)
                    .clickable(onClick = onPlayPause),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(Modifier.width(14.dp))

            // Next button (circular with subtle artwork tint)
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(sideButtonBg)
                    .clickable(onClick = onNext),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = "Next",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/**
 * Sleek interactive player slider bar matching iOS Dynamic Island aesthetics.
 * Uses dynamic colors extracted from the current song's album artwork.
 * Isolates progress updates to this composable to avoid parent recompositions.
 */
@Composable
private fun OverlayPlayerSlider(
    positionFlow: MutableStateFlow<Long>,
    durationFlow: MutableStateFlow<Long>,
    activeColor: Color,
    inactiveColor: Color,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val positionMs by positionFlow.collectAsState()
    val durationMs by durationFlow.collectAsState()

    var draggingFraction by remember { mutableFloatStateOf(-1f) }
    val progress = remember(positionMs, durationMs, draggingFraction) {
        if (draggingFraction >= 0f) {
            draggingFraction
        } else if (durationMs > 0L) {
            (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    }

    var sliderWidthPx by remember { mutableFloatStateOf(1f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(14.dp)
            .onSizeChanged {
                if (it.width > 0) {
                    sliderWidthPx = it.width.toFloat()
                }
            }
            .pointerInput(durationMs) {
                detectTapGestures(
                    onTap = { offset ->
                        if (sliderWidthPx > 0f && durationMs > 0L) {
                            val fraction = (offset.x / sliderWidthPx).coerceIn(0f, 1f)
                            onSeek((fraction * durationMs).toLong())
                        }
                    },
                )
            }
            .pointerInput(durationMs) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        if (sliderWidthPx > 0f) {
                            draggingFraction = (offset.x / sliderWidthPx).coerceIn(0f, 1f)
                        }
                    },
                    onDragEnd = {
                        if (draggingFraction >= 0f && durationMs > 0L) {
                            onSeek((draggingFraction * durationMs).toLong())
                        }
                        draggingFraction = -1f
                    },
                    onDragCancel = {
                        draggingFraction = -1f
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        if (sliderWidthPx > 0f) {
                            draggingFraction = (change.position.x / sliderWidthPx).coerceIn(0f, 1f)
                        }
                    },
                )
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        // Track background (artwork-tinted inactive line)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.5.dp)
                .clip(CircleShape)
                .background(inactiveColor),
        )

        // Played active portion (artwork dynamic accent line)
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                .height(3.5.dp)
                .clip(CircleShape)
                .background(activeColor),
        )
    }
}

@Composable
private fun OverlayEqualizer(
    isPlaying: Boolean,
    barColor: Color = Color(0xFF4CAF50),
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "overlayEq")
    val h1 by transition.animateFloat(
        initialValue = 4f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "eq1",
    )
    val h2 by transition.animateFloat(
        initialValue = 6f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(320, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "eq2",
    )
    val h3 by transition.animateFloat(
        initialValue = 3f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(
            animation = tween(460, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "eq3",
    )

    val heights = listOf(
        if (isPlaying) h1 else 4f,
        if (isPlaying) h2 else 6f,
        if (isPlaying) h3 else 3f,
    )

    Row(
        modifier = modifier.height(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        for (i in 0 until 3) {
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height(heights[i].dp)
                    .clip(CircleShape)
                    .background(barColor),
            )
        }
    }
}

/**
 * Minimalist LifecycleOwner, SavedStateRegistryOwner, and ViewModelStoreOwner
 * implementation allowing ComposeView to run inside a Service window.
 */
private class OverlayLifecycleOwner : SavedStateRegistryOwner, ViewModelStoreOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store

    fun onCreate() {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    fun onStart() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }
}
