package com.music.dhvani.playback

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import coil3.compose.AsyncImage
import com.music.dhvani.MainActivity
import com.music.dhvani.data.lyrics.EmbeddedLyrics
import com.music.dhvani.data.lyrics.LyricLine
import com.music.dhvani.data.lyrics.LyricsRepository
import com.music.dhvani.data.model.Song
import com.music.dhvani.data.model.artworkAt
import com.music.dhvani.data.settings.AppSettings
import com.music.dhvani.download.Downloads
import com.music.dhvani.ui.theme.ArtworkPalette
import com.music.dhvani.ui.theme.DhvaniTheme
import com.music.dhvani.ui.theme.rememberArtworkPalette
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Manages the floating system-wide liquid-glass music island overlay that appears
 * over any other Android application when music is playing or transitioning.
 *
 * Adopts OxygenOS 15 / ColorOS 15 aesthetics: translucent dark glass with subtle perimeter
 * highlights, luminous diffuse glow ("弥散光"), interactive progress scrubbing, fluid spring physics,
 * and reliable next-track transitions without overlay disappearance.
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
    private val isLoadingFlow = MutableStateFlow(false)
    private val isTransitioningFlow = MutableStateFlow(false)
    private val playWhenReadyFlow = MutableStateFlow(false)
    private val playbackStateFlow = MutableStateFlow(Player.STATE_IDLE)
    private val currentPositionFlow = MutableStateFlow(0L)
    private val durationMsFlow = MutableStateFlow(0L)
    private val errorMessageFlow = MutableStateFlow<String?>(null)
    private val isDismissedByUserFlow = MutableStateFlow(false)
    private val shouldShowFlow = MutableStateFlow(false)
    private val upcomingTracksFlow = MutableStateFlow<List<Pair<Int, Song>>>(emptyList())
    private val lyricsFlow = MutableStateFlow<List<LyricLine>?>(null)
    private var lyricsFetchJob: kotlinx.coroutines.Job? = null
    private var isOverlayAttached = false

    companion object {
        private val isAppInForegroundFlow = MutableStateFlow(true)
        private val activeLyricsOverride = MutableStateFlow<Pair<String, List<LyricLine>?>?>(null)

        fun setAppForeground(inForeground: Boolean) {
            isAppInForegroundFlow.value = inForeground
        }

        fun updateActiveLyrics(videoId: String, lines: List<LyricLine>?) {
            activeLyricsOverride.value = videoId to lines
        }
    }

    init {
        // Pre-seed state from player immediately
        playerProvider()?.let { player ->
            val initialSong = player.currentMediaItem?.toSong()
            currentSongFlow.value = initialSong
            isPlayingFlow.value = player.isPlaying
            playWhenReadyFlow.value = player.playWhenReady
            playbackStateFlow.value = player.playbackState
            isLoadingFlow.value = player.playbackState == Player.STATE_BUFFERING
            currentPositionFlow.value = player.currentPosition.coerceAtLeast(0L)
            durationMsFlow.value = player.duration.coerceAtLeast(0L)
            fetchLyricsForSong(initialSong)
        }
        updateUpcomingTracks()

        // Live progress updates during active background playback
        scope.launch {
            while (isActive) {
                if (shouldShowFlow.value && isPlayingFlow.value && !isLoadingFlow.value && !isTransitioningFlow.value) {
                    playerProvider()?.let { player ->
                        currentPositionFlow.value = player.currentPosition.coerceAtLeast(0L)
                        val d = player.duration
                        if (d > 0L) {
                            durationMsFlow.value = d
                        }
                    }
                }
                delay(if (isOverlayExpanded) 200 else 350)
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
            isLoadingFlow.collect {
                updateOverlayVisibility()
            }
        }
        scope.launch {
            isTransitioningFlow.collect {
                updateOverlayVisibility()
            }
        }
        scope.launch {
            playWhenReadyFlow.collect {
                updateOverlayVisibility()
            }
        }
        scope.launch {
            playbackStateFlow.collect {
                updateOverlayVisibility()
            }
        }
        scope.launch {
            errorMessageFlow.collect {
                updateOverlayVisibility()
            }
        }
        scope.launch {
            activeLyricsOverride.collect { pair ->
                if (pair != null && pair.first == currentSongFlow.value?.videoId && !pair.second.isNullOrEmpty()) {
                    lyricsFlow.value = pair.second
                }
            }
        }
        scope.launch {
            currentSongFlow.collect { song ->
                if (song != null) {
                    isDismissedByUserFlow.value = false
                    fetchLyricsForSong(song)
                } else {
                    lyricsFetchJob?.cancel()
                    lyricsFlow.value = null
                }
                updateOverlayVisibility()
            }
        }
        scope.launch {
            durationMsFlow.collect { duration ->
                if (duration > 0L && lyricsFlow.value == null) {
                    currentSongFlow.value?.let { song ->
                        if (lyricsFetchJob == null || lyricsFetchJob?.isCompleted == true) {
                            fetchLyricsForSong(song)
                        }
                    }
                }
            }
        }
        scope.launch {
            lyricsFlow.collect {
                if (isOverlayExpanded) {
                    adjustExpandedDimensions()
                }
            }
        }
        scope.launch {
            isDismissedByUserFlow.collect {
                updateOverlayVisibility()
            }
        }
    }

    private fun fetchLyricsForSong(song: Song?) {
        lyricsFetchJob?.cancel()
        if (song == null || song.videoId.isBlank()) {
            lyricsFlow.value = null
            return
        }
        val override = activeLyricsOverride.value
        if (override != null && override.first == song.videoId && !override.second.isNullOrEmpty()) {
            lyricsFlow.value = override.second
            return
        }
        val cached = LyricsRepository.getCached(song.videoId)
        if (cached != null) {
            lyricsFlow.value = cached.lines
            return
        }
        lyricsFetchJob = scope.launch(Dispatchers.IO) {
            try {
                val effectiveLocalUri = song.localUri
                    ?: Downloads.verifiedSavedUri(song.videoId)
                    ?: Downloads.savedUri(context, song.videoId)?.toString()

                if (!effectiveLocalUri.isNullOrBlank() || song.videoId.isNotBlank()) {
                    EmbeddedLyrics.forUri(context, effectiveLocalUri.orEmpty(), song.videoId)?.let { embedded ->
                        lyricsFlow.value = embedded
                        return@launch
                    }
                }

                val playerDuration = durationMsFlow.value.takeIf { it > 0L }
                    ?: (playerProvider()?.duration?.takeIf { it > 0L } ?: 0L)
                val sources = if (AppSettings.syncedLyrics.value) AppSettings.lyricsSources.value else emptySet()
                val result = LyricsRepository.lyrics(
                    videoId = song.videoId,
                    title = song.title,
                    artist = song.artist,
                    durationMs = playerDuration,
                    album = song.albumName,
                    sources = sources.ifEmpty { com.music.dhvani.data.lyrics.LyricsSource.entries.toSet() },
                    order = AppSettings.lyricsSourceOrder.value,
                    prioritizeSyllableSync = AppSettings.prioritizeSyllableSync.value,
                )
                lyricsFlow.value = result?.lines
            } catch (_: Throwable) {
                // Ignore network errors in floating overlay
            }
        }
    }

    fun onSongChanged(song: Song?) {
        if (song != null) {
            currentSongFlow.value = song
            isTransitioningFlow.value = false
            isLoadingFlow.value = false
            errorMessageFlow.value = null
            playerProvider()?.let { player ->
                currentPositionFlow.value = player.currentPosition.coerceAtLeast(0L)
                durationMsFlow.value = player.duration.coerceAtLeast(0L)
            }
            fetchLyricsForSong(song)
            updateUpcomingTracks()
            isDismissedByUserFlow.value = false
        }
        updateOverlayVisibility()
    }

    fun onQueueChanged() {
        updateUpcomingTracks()
    }

    fun onIsPlayingChanged(isPlaying: Boolean) {
        isPlayingFlow.value = isPlaying
        if (isPlaying) {
            isLoadingFlow.value = false
            isTransitioningFlow.value = false
            errorMessageFlow.value = null
            playerProvider()?.let { player ->
                currentPositionFlow.value = player.currentPosition.coerceAtLeast(0L)
                val d = player.duration
                if (d > 0L) durationMsFlow.value = d
            }
        }
        updateOverlayVisibility()
    }

    fun onPlayWhenReadyChanged(playWhenReady: Boolean) {
        playWhenReadyFlow.value = playWhenReady
        if (!playWhenReady) {
            isTransitioningFlow.value = false
            isLoadingFlow.value = false
        }
        updateOverlayVisibility()
    }

    fun onPlaybackStateChanged(state: Int) {
        playbackStateFlow.value = state
        val isBuffering = state == Player.STATE_BUFFERING
        isLoadingFlow.value = isBuffering
        if (state == Player.STATE_READY) {
            isTransitioningFlow.value = false
            errorMessageFlow.value = null
        }
        updateOverlayVisibility()
    }

    fun onPlayerError(error: String) {
        isLoadingFlow.value = false
        isTransitioningFlow.value = false
        errorMessageFlow.value = error
        updateOverlayVisibility()
    }

    private fun notifyTransitionStarted() {
        isTransitioningFlow.value = true
        isLoadingFlow.value = true
        errorMessageFlow.value = null
        updateOverlayVisibility()
    }

    private fun canDrawOverlay(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    private var isOverlayExpanded = false

    fun setOverlayExpanded(expanded: Boolean) {
        if (isOverlayExpanded == expanded) return
        isOverlayExpanded = expanded
        if (expanded) {
            updateUpcomingTracks()
            adjustExpandedDimensions()
        } else {
            // Wait for Compose spring contraction (~260ms) to complete before shrinking window
            scope.launch {
                delay(260)
                if (!isOverlayExpanded) {
                    val currentView = composeView ?: return@launch
                    val currentParams = currentView.layoutParams as? WindowManager.LayoutParams ?: return@launch
                    currentParams.width = WindowManager.LayoutParams.WRAP_CONTENT
                    currentParams.height = WindowManager.LayoutParams.WRAP_CONTENT
                    currentParams.y = getCollapsedY()
                    try {
                        windowManager.updateViewLayout(currentView, currentParams)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun adjustExpandedDimensions() {
        val view = composeView ?: return
        val params = view.layoutParams as? WindowManager.LayoutParams ?: return
        val density = context.resources.displayMetrics.density
        val screenWidth = context.resources.displayMetrics.widthPixels
        val isLandscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val hasUpcoming = upcomingTracksFlow.value.isNotEmpty()
        val hasLyrics = !lyricsFlow.value.isNullOrEmpty()

        val targetWidthDp = ((screenWidth / density) - 20).coerceIn(310f, 356f)
        val targetHeightDp = if (hasUpcoming) {
            if (isLandscape) 180f else (if (hasLyrics) 238f else 208f)
        } else {
            if (isLandscape) 130f else (if (hasLyrics) 180f else 148f)
        }
        params.width = (targetWidthDp * density).toInt()
        params.height = (targetHeightDp * density).toInt()
        params.y = getCollapsedY()
        try {
            windowManager.updateViewLayout(view, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateUpcomingTracks() {
        val player = playerProvider() ?: run {
            upcomingTracksFlow.value = emptyList()
            return
        }
        try {
            val list = mutableListOf<Pair<Int, Song>>()
            val count = player.mediaItemCount
            val timeline = player.currentTimeline
            val curIdx = player.currentMediaItemIndex

            if (curIdx != C.INDEX_UNSET && count > 0) {
                if (!timeline.isEmpty) {
                    var idx = curIdx
                    val visited = mutableSetOf(curIdx)
                    for (step in 0 until 4) {
                        idx = timeline.getNextWindowIndex(
                            idx,
                            player.repeatMode,
                            player.shuffleModeEnabled,
                        )
                        if (idx == C.INDEX_UNSET || !visited.add(idx) || idx !in 0 until count) {
                            break
                        }
                        val item = player.getMediaItemAt(idx)
                        list.add(idx to item.toSong())
                    }
                } else {
                    val nextIndex = player.nextMediaItemIndex
                    if (nextIndex != C.INDEX_UNSET) {
                        val end = (nextIndex + 3).coerceAtMost(count - 1)
                        for (i in nextIndex..end) {
                            val item = player.getMediaItemAt(i)
                            list.add(i to item.toSong())
                        }
                    }
                }
            }
            upcomingTracksFlow.value = list
            if (isOverlayExpanded) {
                adjustExpandedDimensions()
            }
        } catch (_: Throwable) {
            upcomingTracksFlow.value = emptyList()
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

        val inForeground = isAppInForegroundFlow.value
        val isPlaying = isPlayingFlow.value
        val isLoading = isLoadingFlow.value
        val isTransitioning = isTransitioningFlow.value
        val playWhenReady = playWhenReadyFlow.value
        val playbackState = playbackStateFlow.value
        val hasSong = currentSongFlow.value != null
        val dismissed = isDismissedByUserFlow.value
        val hasError = errorMessageFlow.value != null

        // Playback is active while playing, buffering, transitioning to next track,
        // or playWhenReady is true and not ended/idle, or during an error retry state.
        val isPlaybackActive = isPlaying ||
            isLoading ||
            isTransitioning ||
            (playWhenReady && playbackState != Player.STATE_ENDED && playbackState != Player.STATE_IDLE) ||
            hasError

        val shouldShow = !inForeground && hasSong && !dismissed && isPlaybackActive
        shouldShowFlow.value = shouldShow

        if (shouldShow) {
            ensureOverlayAttached()
        } else {
            if (isOverlayExpanded) {
                setOverlayExpanded(false)
            }
            detachOverlay()
        }
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
                y = getCollapsedY()
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
                            isLoadingFlow = isLoadingFlow,
                            isTransitioningFlow = isTransitioningFlow,
                            errorMessageFlow = errorMessageFlow,
                            positionFlow = currentPositionFlow,
                            durationFlow = durationMsFlow,
                            upcomingTracksFlow = upcomingTracksFlow,
                            lyricsFlow = lyricsFlow,
                            onExpandedChanged = { isExp ->
                                setOverlayExpanded(isExp)
                            },
                            onSeek = { targetMs ->
                                playerProvider()?.seekTo(targetMs)
                                currentPositionFlow.value = targetMs
                            },
                            onTrackClick = { queueIndex ->
                                val player = playerProvider() ?: return@SystemDynamicIslandRoot
                                notifyTransitionStarted()
                                player.seekToDefaultPosition(queueIndex)
                                player.play()
                            },
                            onPlayPause = {
                                val player = playerProvider() ?: return@SystemDynamicIslandRoot
                                if (errorMessageFlow.value != null) {
                                    errorMessageFlow.value = null
                                    isLoadingFlow.value = true
                                    player.prepare()
                                    player.play()
                                } else {
                                    if (player.isPlaying) player.pause() else player.play()
                                }
                            },
                            onNext = {
                                notifyTransitionStarted()
                                playerProvider()?.seekToNextMediaItem()
                            },
                            onPrevious = {
                                notifyTransitionStarted()
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

    private fun getCollapsedY(): Int {
        val isLandscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        if (isLandscape) return 8
        val statusBarHeight = getStatusBarHeight()
        return (statusBarHeight * 0.28f).toInt().coerceAtLeast(6)
    }

    private fun getStatusBarHeight(): Int {
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else (28 * context.resources.displayMetrics.density).toInt()
    }
}

/**
 * Root Composable wrapping the dynamic island inside an AnimatedVisibility container.
 * Features OxygenOS 15 liquid-glass materials, subtle diffuse glow, and spring animations.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SystemDynamicIslandRoot(
    shouldShowFlow: MutableStateFlow<Boolean>,
    songFlow: MutableStateFlow<Song?>,
    isPlayingFlow: MutableStateFlow<Boolean>,
    isLoadingFlow: MutableStateFlow<Boolean>,
    isTransitioningFlow: MutableStateFlow<Boolean>,
    errorMessageFlow: MutableStateFlow<String?>,
    positionFlow: MutableStateFlow<Long>,
    durationFlow: MutableStateFlow<Long>,
    upcomingTracksFlow: MutableStateFlow<List<Pair<Int, Song>>>,
    lyricsFlow: MutableStateFlow<List<LyricLine>?>,
    onExpandedChanged: (Boolean) -> Unit,
    onSeek: (Long) -> Unit,
    onTrackClick: (Int) -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onDismiss: () -> Unit,
    onOpenApp: () -> Unit,
) {
    val shouldShow by shouldShowFlow.collectAsState()
    val song by songFlow.collectAsState()
    val isPlaying by isPlayingFlow.collectAsState()
    val isLoading by isLoadingFlow.collectAsState()
    val isTransitioning by isTransitioningFlow.collectAsState()
    val errorMessage by errorMessageFlow.collectAsState()

    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    val effectiveLoading = (isLoading || isTransitioning) && !isPlaying

    // Respect system reduced-motion settings
    val isReducedMotion = remember(context) {
        try {
            val animatorScale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f,
            )
            val transitionScale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.TRANSITION_ANIMATION_SCALE,
                1.0f,
            )
            animatorScale == 0f || transitionScale == 0f
        } catch (_: Throwable) {
            false
        }
    }

    // Reset expanded mode when hidden
    androidx.compose.runtime.LaunchedEffect(shouldShow) {
        if (!shouldShow && expanded) {
            expanded = false
            onExpandedChanged(false)
        }
    }

    AnimatedVisibility(
        visible = shouldShow && song != null,
        enter = if (isReducedMotion) {
            fadeIn(tween(120))
        } else {
            slideInVertically(
                animationSpec = spring(
                    dampingRatio = 0.84f,
                    stiffness = 420f,
                ),
            ) { -it * 2 } + fadeIn(tween(180)) + scaleIn(
                initialScale = 0.80f,
                animationSpec = spring(dampingRatio = 0.84f, stiffness = 420f),
            )
        },
        exit = if (isReducedMotion) {
            fadeOut(tween(100))
        } else {
            slideOutVertically(
                animationSpec = tween(160, easing = FastOutSlowInEasing),
            ) { -it * 2 } + fadeOut(tween(130)) + scaleOut(
                targetScale = 0.80f,
                animationSpec = tween(140),
            )
        },
    ) {
        val currentSong = song ?: return@AnimatedVisibility
        val artworkModel = remember(currentSong.videoId) {
            currentSong.artworkAt(200) ?: currentSong.thumbnailUrl
        }
        val palette = rememberArtworkPalette(
            imageUrl = currentSong.artworkAt(200) ?: currentSong.thumbnailUrl,
            dark = true,
        )

        var dragAccumulator by remember { mutableFloatStateOf(0f) }

        val cornerRadius by animateDpAsState(
            targetValue = if (expanded) 26.dp else 22.dp,
            animationSpec = if (isReducedMotion) tween(80) else spring(
                dampingRatio = 0.84f,
                stiffness = 420f,
            ),
            label = "overlayCornerRadius",
        )
        val shape = RoundedCornerShape(cornerRadius)

        // Pure OLED Black background for both mini and expanded island - no color taken from song cover
        val glassBorder = remember {
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.20f),
                    Color.White.copy(alpha = 0.06f),
                ),
            )
        }

        val canBlur = remember { Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !isReducedMotion }

        Box(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .shadow(
                    elevation = if (expanded) 16.dp else 12.dp,
                    shape = shape,
                    ambientColor = Color.Black.copy(alpha = 0.90f),
                    spotColor = Color.Black.copy(alpha = 0.85f),
                )
                .clip(shape)
                .background(Color.Black)
                .border(
                    width = 1.dp,
                    brush = glassBorder,
                    shape = shape,
                )
                .animateContentSize(
                    animationSpec = if (isReducedMotion) tween(100) else spring(
                        dampingRatio = 0.84f,
                        stiffness = 420f,
                    ),
                )
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
            // Blurred Song Cover in Background (Expanded mode with deep black scrim, zero palette tint)
            if (expanded) {
                AsyncImage(
                    model = artworkModel,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            scaleX = 1.35f
                            scaleY = 1.35f
                            alpha = 0.16f
                        }
                        .then(
                            if (canBlur) Modifier.blur(36.dp, BlurredEdgeTreatment.Unbounded)
                            else Modifier
                        ),
                )

                // Deep black scrim so background remains OLED pitch black
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xEA000000),
                                    Color(0xF5000000),
                                    Color(0xFF000000),
                                ),
                            ),
                        ),
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Subtle swipe up indicator notch pill at top (expanded mode only)
                if (expanded) {
                    Box(
                        modifier = Modifier
                            .padding(top = 3.dp)
                            .width(26.dp)
                            .height(2.5.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.22f)),
                    )
                }

                Crossfade(
                    targetState = expanded,
                    animationSpec = if (isReducedMotion) tween(90) else tween(160, easing = FastOutSlowInEasing),
                    label = "DynamicIslandCrossfade",
                ) { isExp ->
                    if (isExp) {
                        ExpandedOverlayCard(
                            song = currentSong,
                            artworkModel = artworkModel,
                            palette = palette,
                            isPlaying = isPlaying,
                            isLoading = effectiveLoading,
                            errorMessage = errorMessage,
                            isReducedMotion = isReducedMotion,
                            positionFlow = positionFlow,
                            durationFlow = durationFlow,
                            upcomingTracksFlow = upcomingTracksFlow,
                            lyricsFlow = lyricsFlow,
                            onSeek = onSeek,
                            onTrackClick = onTrackClick,
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
                            isLoading = effectiveLoading,
                            isReducedMotion = isReducedMotion,
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

/**
 * Compact state: translucent dark capsule with mini artwork, song title, artist,
 * and animated equalizer/spinner. Tapping anywhere on the capsule smoothly expands it.
 */
/**
 * Compact state: translucent dark capsule matching iOS Dynamic Island with song cover
 * on the left and animated wave effect on the right. Tapping anywhere smoothly expands it.
 */
@Composable
private fun CompactOverlayPill(
    song: Song,
    artworkModel: Any?,
    palette: ArtworkPalette,
    isPlaying: Boolean,
    isLoading: Boolean,
    isReducedMotion: Boolean,
    onExpand: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onExpand,
            )
            .padding(start = 6.dp, end = 10.dp, top = 4.dp, bottom = 4.dp)
            .height(34.dp)
            .widthIn(min = 125.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Left side: Song cover (Album artwork)
        AsyncImage(
            model = artworkModel,
            contentDescription = "Song Cover",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1E202B))
                .border(0.75.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(8.dp)),
        )

        // Right side: Wave effect (Kinetic Audio Equalizer or Loading Spinner)
        if (isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.dp,
                modifier = Modifier.size(14.dp),
            )
        } else {
            OverlayEqualizer(
                isPlaying = isPlaying,
                barColor = Color.White,
                isReducedMotion = isReducedMotion,
            )
        }
    }
}

/**
 * Expanded OxygenOS-inspired media card with large artwork, scrub progress bar,
 * playback controls, and smooth collapse on header/capsule tap without extra arrows.
 */
@Composable
private fun ExpandedOverlayCard(
    song: Song,
    artworkModel: Any?,
    palette: ArtworkPalette,
    isPlaying: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    isReducedMotion: Boolean,
    positionFlow: MutableStateFlow<Long>,
    durationFlow: MutableStateFlow<Long>,
    upcomingTracksFlow: MutableStateFlow<List<Pair<Int, Song>>>,
    lyricsFlow: MutableStateFlow<List<LyricLine>?>,
    onSeek: (Long) -> Unit,
    onTrackClick: (Int) -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onCollapse: () -> Unit,
    onOpenApp: () -> Unit,
) {
    val accentColor = Color.White
    val upcomingTracks by upcomingTracksFlow.collectAsState()
    val lyrics by lyricsFlow.collectAsState()
    val positionMs by positionFlow.collectAsState()

    val activeLyric = remember(lyrics, positionMs) {
        val lines = lyrics
        if (lines.isNullOrEmpty()) null
        else {
            val idx = lines.indexOfLast { it.timeMs <= positionMs }
            if (idx >= 0) lines[idx] else null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onCollapse,
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Top section: Squircle Artwork, Track Info, and Top-Right Collapse Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Squircle album art with glowing border ring - tapping opens full player in app
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .shadow(4.dp, RoundedCornerShape(12.dp), spotColor = Color.Black.copy(alpha = 0.60f))
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
                    .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(12.dp))
                    .clickable(onClick = onOpenApp),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = artworkModel,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
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
                        fontSize = 13.5.sp,
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (errorMessage != null) "Failed to load · Tap Play to retry" else song.artist,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        fontWeight = if (errorMessage != null) FontWeight.Medium else FontWeight.Normal,
                    ),
                    color = if (errorMessage != null) Color(0xFFFF5252) else Color.White.copy(alpha = 0.70f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Top-right collapse button pointing UP
            IconButton(
                onClick = onCollapse,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.10f))
                    .border(0.75.dp, Color.White.copy(alpha = 0.18f), CircleShape),
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowUp,
                    contentDescription = "Collapse music player",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        // Slim interactive progress bar with elapsed and remaining time
        OverlayProgressBar(
            positionFlow = positionFlow,
            durationFlow = durationFlow,
            accentColor = accentColor,
            onSeek = onSeek,
        )

        // Smooth line-by-line synced lyrics banner (no border, clean aesthetic)
        val activeText = (activeLyric?.romanizedText?.takeIf { it.isNotBlank() } ?: activeLyric?.text)?.trim()
        if (!lyrics.isNullOrEmpty() && !activeText.isNullOrBlank()) {
            AnimatedContent(
                targetState = activeText,
                transitionSpec = {
                    if (isReducedMotion) {
                        fadeIn(animationSpec = tween(140)).togetherWith(fadeOut(animationSpec = tween(120)))
                    } else {
                        (fadeIn(animationSpec = tween(260)) +
                                slideInVertically(animationSpec = tween(260, easing = FastOutSlowInEasing)) { it / 2 })
                            .togetherWith(
                                fadeOut(animationSpec = tween(180)) +
                                        slideOutVertically(animationSpec = tween(180, easing = FastOutSlowInEasing)) { -it / 2 },
                            )
                    }
                },
                label = "IslandLyricTransition",
                modifier = Modifier.fillMaxWidth(),
            ) { lineText ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .clickable(onClick = onOpenApp)
                        .padding(horizontal = 8.dp, vertical = 2.5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.80f),
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = lineText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.2.sp,
                        ),
                        color = Color.White.copy(alpha = 0.95f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        // Playback controls row: Audio visualizer on left, centered Previous/Play/Next, open-in-app button on right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Left: live audio visualizer or loading indicator
            Box(
                modifier = Modifier.size(36.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = accentColor,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp),
                    )
                } else {
                    OverlayEqualizer(
                        isPlaying = isPlaying,
                        barColor = accentColor,
                        isReducedMotion = isReducedMotion,
                    )
                }
            }

            // Center controls: Previous, Play/Pause pill, Next
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f)),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }

                // Frosted jewel glass Play/Pause button
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .shadow(6.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.60f))
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.20f),
                                    Color.White.copy(alpha = 0.08f),
                                ),
                            ),
                        )
                        .border(
                            1.dp,
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.35f),
                                    Color.White.copy(alpha = 0.10f),
                                ),
                            ),
                            CircleShape,
                        )
                        .clickable(onClick = onPlayPause),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp),
                        )
                    } else {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }

                IconButton(
                    onClick = onNext,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f)),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            // Right: Launch Dhvani app button
            IconButton(
                onClick = onOpenApp,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f)),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                    contentDescription = "Open Dhvani App",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        // "Up Next" upcoming tracks row directly below playback controls: 54dp square thumbnails only
        if (upcomingTracks.isNotEmpty()) {
            OverlayUpcomingQueueRow(
                upcomingTracks = upcomingTracks,
                accentColor = accentColor,
                onTrackClick = onTrackClick,
            )
        }
    }
}

/**
 * Up Next row directly below playback controls showing up to 4 upcoming tracks as artwork-only square thumbnails (54dp).
 * No song titles, artist names, captions, or placeholder images.
 * If fewer than four tracks are queued, shows only those available.
 * Tapping a cover plays that song and updates the queue.
 */
@Composable
private fun OverlayUpcomingQueueRow(
    upcomingTracks: List<Pair<Int, Song>>,
    accentColor: Color,
    onTrackClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = upcomingTracks.take(4)
    if (items.isEmpty()) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { (queueIndex, song) ->
            UpcomingCoverArt(
                song = song,
                accentColor = accentColor,
                onClick = { onTrackClick(queueIndex) },
            )
        }
    }
}

@Composable
private fun UpcomingCoverArt(
    song: Song,
    accentColor: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(54.dp)
            .shadow(4.dp, RoundedCornerShape(12.dp), spotColor = Color.Black.copy(alpha = 0.50f))
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E202B))
            .border(0.75.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = song.artworkAt(200) ?: song.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/**
 * Interactive player slider bar matching OxygenOS 15 aesthetics.
 * Uses dynamic colors extracted from the current song's album artwork.
 * Isolates progress updates to this composable to avoid parent recompositions.
 */
@Composable
private fun OverlayProgressBar(
    positionFlow: MutableStateFlow<Long>,
    durationFlow: MutableStateFlow<Long>,
    accentColor: Color,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val positionMs by positionFlow.collectAsState()
    val durationMs by durationFlow.collectAsState()

    var draggingFraction by remember { mutableFloatStateOf(-1f) }
    val progressFraction = remember(positionMs, durationMs, draggingFraction) {
        if (draggingFraction >= 0f) {
            draggingFraction
        } else if (durationMs > 0L) {
            (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    }

    var sliderWidthPx by remember { mutableFloatStateOf(1f) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val displayPos = if (draggingFraction >= 0f && durationMs > 0L) {
            (draggingFraction * durationMs).toLong()
        } else {
            positionMs
        }

        Text(
            text = formatTime(displayPos),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = Color.White.copy(alpha = 0.65f),
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(18.dp)
                .onSizeChanged {
                    if (it.width > 0) sliderWidthPx = it.width.toFloat()
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
            // Track background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.5.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.16f)),
            )

            // Active track fill
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = progressFraction.coerceIn(0f, 1f))
                    .height(3.5.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                accentColor.copy(alpha = 0.70f),
                                accentColor,
                            ),
                        ),
                    ),
            )

            // Thumb glow dot at current position
            if (progressFraction > 0.01f) {
                Box(
                    modifier = Modifier.fillMaxWidth(fraction = progressFraction.coerceIn(0f, 1f)),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .shadow(2.dp, CircleShape, spotColor = accentColor),
                    )
                }
            }
        }

        val remainingMs = if (durationMs > displayPos) durationMs - displayPos else 0L
        val remainingText = if (durationMs > 0L) "-${formatTime(remainingMs)}" else "0:00"

        Text(
            text = remainingText,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = Color.White.copy(alpha = 0.65f),
        )
    }
}

@Composable
private fun OverlayEqualizer(
    isPlaying: Boolean,
    barColor: Color = Color(0xFF4CAF50),
    isReducedMotion: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (isReducedMotion) {
        Row(
            modifier = modifier.height(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            val staticHeights = listOf(10.dp, 14.dp, 8.dp)
            for (h in staticHeights) {
                Box(
                    modifier = Modifier
                        .width(2.5.dp)
                        .height(if (isPlaying) h else 4.dp)
                        .clip(CircleShape)
                        .background(barColor),
                )
            }
        }
        return
    }

    val transition = rememberInfiniteTransition(label = "overlayEq")
    val h1 by transition.animateFloat(
        initialValue = 4f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "eq1",
    )
    val h2 by transition.animateFloat(
        initialValue = 5f,
        targetValue = 17f,
        animationSpec = infiniteRepeatable(
            animation = tween(330, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "eq2",
    )
    val h3 by transition.animateFloat(
        initialValue = 3f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(470, easing = FastOutSlowInEasing),
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
        modifier = modifier.height(16.dp),
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

private fun formatTime(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes >= 60) {
        val hours = minutes / 60
        val remMinutes = minutes % 60
        "%d:%02d:%02d".format(hours, remMinutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
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
