package com.music.dhvani.ui.player

import android.content.Context
import android.util.Log
import android.graphics.Bitmap
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.SurfaceTexture
import android.os.Build
import android.view.TextureView
import android.view.ViewGroup
import android.widget.FrameLayout
import android.os.Handler
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameMillis
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.music.dhvani.data.Http
import com.music.dhvani.data.canvas.CanvasArtwork
import com.music.dhvani.data.canvas.CanvasCache
import com.music.dhvani.data.settings.AppSettings
import com.music.dhvani.ui.rememberIsForeground
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.ArrayList
import java.util.Locale

private const val REPAINT_TIMEOUT_MS = 700L

/**
 * Isolated, silent video artwork player for Apple Music, Tidal, and Community canvas clips.
 *
 * Runs a dedicated [ExoPlayer] on a [TextureView] with all audio renderers suppressed and
 * audio focus handling explicitly disabled so it never interrupts, ducks, or pauses the main
 * music playback service.
 */
@OptIn(UnstableApi::class)
@Composable
fun CanvasArtworkPlayer(
    artwork: CanvasArtwork,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true,
    isActive: Boolean = true,
    applyBottomGradientMask: Boolean = true,
    fadeFraction: Float = 0.35f,
    onFirstFrameRendered: () -> Unit = {},
    onRenderedChanged: (Boolean) -> Unit = {},
    onCoverChanged: (Float) -> Unit = {},
) {
    val context = LocalContext.current

    var url by remember(artwork.url) { mutableStateOf(artwork.url) }
    var rendered by remember(artwork.url) { mutableStateOf(false) }
    var clipAspect by remember(artwork.url) { mutableFloatStateOf(0f) }
    var bounds by remember { mutableStateOf(IntSize.Zero) }
    var textureView by remember(artwork.url) { mutableStateOf<TextureView?>(null) }
    var frameTick by remember(artwork.url) { mutableIntStateOf(0) }
    var surfaceGeneration by remember(artwork.url) { mutableIntStateOf(0) }

    val player = remember(context, artwork.url) {
        val upstreamFactory = OkHttpDataSource.Factory(Http.client)
        val dataSourceFactory = CanvasCache.getCacheDataSourceFactory(context)
        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(dataSourceFactory)

        // Completely suppress all audio renderers.
        // Canvas video is strictly a silent visual backdrop; suppressing audio renderers
        // ensures ExoPlayer never allocates an AudioTrack, never creates an AudioSink,
        // and never collides with PlaybackService for AudioFocus on any Android device.
        val renderersFactory = object : DefaultRenderersFactory(context) {
            override fun buildAudioRenderers(
                context: Context,
                extensionRendererMode: Int,
                mediaCodecSelector: MediaCodecSelector,
                enableDecoderFallback: Boolean,
                audioSink: AudioSink,
                eventHandler: Handler,
                eventListener: AudioRendererEventListener,
                out: ArrayList<Renderer>
            ) {
                // Video only: no audio renderers built.
            }
        }

        ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(AudioAttributes.DEFAULT, /* handleAudioFocus = */ false)
            .setHandleAudioBecomingNoisy(false)
            .build()
            .apply {
                volume = 0f
                repeatMode = Player.REPEAT_MODE_ONE
                trackSelectionParameters = trackSelectionParameters.buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                    .build()
            }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                Log.d("CanvasArtworkPlayer", "onRenderedFirstFrame for $url")
                rendered = true
                frameTick++
                onFirstFrameRendered()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                Log.d("CanvasArtworkPlayer", "onPlaybackStateChanged: $playbackState (ready=${Player.STATE_READY}) for $url")
                if (playbackState == Player.STATE_READY) {
                    rendered = true
                    frameTick++
                    onFirstFrameRendered()
                }
            }

            override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                if (isPlayingNow) {
                    rendered = true
                    frameTick++
                    onFirstFrameRendered()
                }
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                val width = videoSize.width * videoSize.pixelWidthHeightRatio
                if (width > 0f && videoSize.height > 0) {
                    clipAspect = width / videoSize.height
                    rendered = true
                    frameTick++
                    onFirstFrameRendered()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.w("CanvasArtworkPlayer", "onPlayerError for $url: ${error.message}", error)
                val alternate = artwork.fallbackUrl
                if (alternate != null && alternate != url) {
                    url = alternate
                } else {
                    rendered = false
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(url) {
        rendered = false
        clipAspect = 0f
        val item = MediaItem.Builder().setUri(url)
        mimeTypeOf(url)?.let { item.setMimeType(it) }
        player.setMediaItem(item.build())
        player.prepare()
        if (isPlaying && isActive) {
            player.play()
        }
    }

    val foreground = rememberIsForeground()
    // Read the user setting: if enabled, canvas pauses with audio; otherwise it loops freely.
    val canvasPauseWithAudio by AppSettings.canvasPauseWithAudio.collectAsStateWithLifecycle()
    LaunchedEffect(isPlaying, foreground, isActive, canvasPauseWithAudio) {
        // When canvasPauseWithAudio is OFF (default): video keeps playing as long
        // as the canvas is active and the app is in the foreground — audio pause
        // doesn't affect it.  When the setting is ON the old behaviour is restored
        // and the video pauses/resumes alongside the audio.
        val shouldPlay = if (canvasPauseWithAudio) isPlaying else true
        player.playWhenReady = shouldPlay && foreground && isActive
    }

    LaunchedEffect(surfaceGeneration) {
        if (surfaceGeneration == 0) return@LaunchedEffect
        if (player.playWhenReady || player.playbackState == Player.STATE_IDLE) return@LaunchedEffect
        val before = frameTick
        player.seekTo(player.currentPosition)
        delay(REPAINT_TIMEOUT_MS)
        if (frameTick == before) rendered = false
    }

    LaunchedEffect(rendered) {
        onRenderedChanged(rendered)
    }

    val alpha by animateFloatAsState(
        targetValue = if (rendered && isActive) 1f else 0f,
        animationSpec = tween(durationMillis = 350),
        label = "canvasAlpha",
    )

    val reportCover by rememberUpdatedState(onCoverChanged)
    LaunchedEffect(Unit) {
        snapshotFlow { alpha }.collect { reportCover(it) }
    }
    DisposableEffect(Unit) {
        onDispose { reportCover(0f) }
    }

    val bottomFade = if (applyBottomGradientMask) fadeFraction else 0f

    AndroidView(
        factory = { viewContext ->
            val texture = TextureView(viewContext).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                isOpaque = false
                this.alpha = 0f
                player.setVideoTextureView(this)

                val delegate = surfaceTextureListener
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    private var replacing = false

                    override fun onSurfaceTextureAvailable(
                        surface: SurfaceTexture,
                        width: Int,
                        height: Int,
                    ) {
                        delegate?.onSurfaceTextureAvailable(surface, width, height)
                        if (!replacing) return
                        replacing = false
                        surfaceGeneration++
                    }

                    override fun onSurfaceTextureSizeChanged(
                        surface: SurfaceTexture,
                        width: Int,
                        height: Int,
                    ) {
                        delegate?.onSurfaceTextureSizeChanged(surface, width, height)
                    }

                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                        replacing = true
                        return delegate?.onSurfaceTextureDestroyed(surface) ?: true
                    }

                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                        delegate?.onSurfaceTextureUpdated(surface)
                    }
                }
            }
            textureView = texture

            FadingBottomFrame(viewContext).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                addView(texture)
            }
        },
        update = { frame ->
            val view = frame.getChildAt(0) as TextureView
            player.setVideoTextureView(view)
            view.alpha = alpha
            view.centerCrop(bounds, clipAspect)
            frame.fadeFraction = bottomFade
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                view.setBottomFade(bottomFade, bounds)
            }
        },
        modifier = modifier.onSizeChanged { bounds = it },
    )
}

private fun TextureView.centerCrop(bounds: IntSize, clipAspect: Float) {
    if (bounds.width == 0 || bounds.height == 0 || clipAspect <= 0f) return
    val viewAspect = bounds.width.toFloat() / bounds.height
    val pivotX = bounds.width / 2f
    val pivotY = bounds.height / 2f
    val matrix = Matrix().apply {
        if (clipAspect > viewAspect) {
            setScale(clipAspect / viewAspect, 1f, pivotX, pivotY)
        } else {
            setScale(1f, viewAspect / clipAspect, pivotX, pivotY)
        }
    }
    setTransform(matrix)
}

@RequiresApi(Build.VERSION_CODES.S)
private fun TextureView.setBottomFade(fraction: Float, bounds: IntSize) {
    val height = bounds.height
    if (fraction <= 0.001f || height == 0) {
        setRenderEffect(null)
        return
    }
    val gradient = LinearGradient(
        0f,
        height * (1f - fraction.coerceAtMost(1f)),
        0f,
        height.toFloat(),
        android.graphics.Color.BLACK,
        android.graphics.Color.TRANSPARENT,
        Shader.TileMode.CLAMP,
    )
    setRenderEffect(
        RenderEffect.createBlendModeEffect(
            RenderEffect.createOffsetEffect(0f, 0f),
            RenderEffect.createShaderEffect(gradient),
            BlendMode.DST_IN,
        ),
    )
}

private class FadingBottomFrame(context: Context) : FrameLayout(context) {
    var fadeFraction: Float = 0f
        set(value) {
            val clamped = value.coerceIn(0f, 1f)
            if (clamped == field) return
            field = clamped
            gradient = null
            invalidate()
        }

    private val maskPaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    }
    private var gradient: LinearGradient? = null
    private var gradientHeight = 0

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        gradient = null
    }

    override fun dispatchDraw(canvas: Canvas) {
        val fade = fadeFraction
        if (fade <= 0.001f || height == 0) {
            super.dispatchDraw(canvas)
            return
        }
        val shader = gradient?.takeIf { gradientHeight == height } ?: LinearGradient(
            0f,
            height * (1f - fade),
            0f,
            height.toFloat(),
            android.graphics.Color.BLACK,
            android.graphics.Color.TRANSPARENT,
            Shader.TileMode.CLAMP,
        ).also {
            gradient = it
            gradientHeight = height
        }
        maskPaint.shader = shader
        val layer = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
        super.dispatchDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), maskPaint)
        canvas.restoreToCount(layer)
    }
}

private fun mimeTypeOf(url: String): String? {
    val path = url.substringBefore('?').lowercase(Locale.ROOT)
    return when {
        path.endsWith(".m3u8") -> MimeTypes.APPLICATION_M3U8
        path.endsWith(".mp4") -> MimeTypes.VIDEO_MP4
        else -> null
    }
}
