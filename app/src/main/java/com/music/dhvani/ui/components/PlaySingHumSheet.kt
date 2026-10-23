package com.music.dhvani.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.rememberCoroutineScope
import coil3.compose.AsyncImage
import com.music.dhvani.data.recognition.ShazamRecognitionClient
import com.music.dhvani.data.recognition.ShazamResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Launches Google's native Sound Search ("Play, sing, or hum a song") activity directly.
 * Uses Google's deep neural acoustic matching model for playing music and hummed melodies.
 */
fun launchGoogleSoundSearch(context: Context): Boolean {
    val intents = listOf(
        Intent("com.google.android.googlequicksearchbox.MUSIC_SEARCH").apply {
            setPackage("com.google.android.googlequicksearchbox")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        },
        Intent("com.google.android.googlequicksearchbox.MUSIC_SEARCH").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        },
        Intent(MediaStore.INTENT_ACTION_MEDIA_SEARCH).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        },
    )

    for (intent in intents) {
        try {
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                return true
            }
        } catch (_: Exception) {}
    }

    try {
        context.startActivity(intents[0])
        return true
    } catch (_: Exception) {
        return false
    }
}

/**
 * Animated Holographic Song Identifier Orb:
 * Features a glowing circular portal with outer amber-to-purple halo, cosmic stardust
 * particles, and multi-layered sinusoidal harmonic wave ribbons that react smoothly to
 * audio level and rhythm.
 */
@Composable
private fun HolographicSongOrb(
    audioLevel: Float,
    isListening: Boolean,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orbHarmonics")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phaseWave",
    )

    val breathingPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "breathingPhase",
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val center = Offset(width / 2f, height / 2f)
        val baseRadius = (width.coerceAtMost(height) / 2f) * 0.82f
        val pulse = sin(breathingPhase) * 4f + (audioLevel * 16f)
        val radius = baseRadius + pulse

        // 1. Soft radial ambient backlight bloom
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFA000).copy(alpha = (0.16f + audioLevel * 0.22f).coerceIn(0f, 0.40f)),
                    Color(0xFF8E24AA).copy(alpha = (0.12f + audioLevel * 0.18f).coerceIn(0f, 0.30f)),
                    Color.Transparent,
                ),
                center = center,
                radius = radius * 1.35f,
            ),
        )

        // 2. Cosmic Stardust Particles around the ring
        val particleCount = 26
        for (i in 0 until particleCount) {
            val angle = (i * (2 * PI / particleCount)).toFloat() + (phase * 0.12f)
            val pRadius = radius + sin(phase * 1.8f + i) * 6.dp.toPx()
            val px = center.x + pRadius * cos(angle)
            val py = center.y + pRadius * sin(angle)
            val pAlpha = ((sin(phase * 2.5f + i * 1.3f) + 1f) / 2f).coerceIn(0.25f, 0.95f)
            val pColor = if (i % 2 == 0) Color(0xFFFFD54F) else Color(0xFFCE93D8)
            val dotSize = if (i % 3 == 0) 2.2.dp.toPx() else 1.4.dp.toPx()

            drawCircle(
                color = pColor.copy(alpha = pAlpha),
                radius = dotSize,
                center = Offset(px, py),
            )
        }

        // 3. Glowing Multi-Chromatic Outer Ring
        val ringGradient = Brush.sweepGradient(
            colors = listOf(
                Color(0xFFFFB300), // Amber-Gold
                Color(0xFFFF7043), // Tangerine
                Color(0xFFE040FB), // Neon Magenta
                Color(0xFF7C4DFF), // Electric Violet
                Color(0xFFFFB300), // Amber-Gold loop
            ),
            center = center,
        )

        // Outer soft diffuse glow
        drawCircle(
            brush = ringGradient,
            radius = radius,
            center = center,
            style = Stroke(width = 9.dp.toPx()),
            alpha = (if (isListening) 0.32f else 0.18f) + (audioLevel * 0.25f).coerceIn(0f, 0.35f),
        )

        // Primary vibrant boundary line
        drawCircle(
            brush = ringGradient,
            radius = radius,
            center = center,
            style = Stroke(width = 2.6.dp.toPx()),
            alpha = if (isListening) 0.95f else 0.75f,
        )

        // 4. Harmonic Fluid Wave Ribbons inside the Orb
        val ribbonSpan = radius * 1.80f
        val startX = center.x - (ribbonSpan / 2f)
        val endX = center.x + (ribbonSpan / 2f)
        val steps = 50
        val strands = 14

        val ribbonGradient = Brush.horizontalGradient(
            colors = listOf(
                Color(0xFFFFB300).copy(alpha = 0.90f), // Golden Amber (Left)
                Color(0xFFFF7043).copy(alpha = 0.92f), // Radiant Orange
                Color(0xFFF06292).copy(alpha = 0.88f), // Soft Pink
                Color(0xFFBA68C8).copy(alpha = 0.92f), // Magenta
                Color(0xFF7C4DFF).copy(alpha = 0.90f), // Electric Violet (Right)
            ),
            startX = startX,
            endX = endX,
        )

        for (s in 0 until strands) {
            val strandNorm = s.toFloat() / strands.toFloat()
            val strandOffset = (strandNorm - 0.5f) * 16.dp.toPx()
            val strandPhase = phase + (s * 0.22f)
            val dynamicAmp = (18.dp.toPx() + (audioLevel * 34.dp.toPx()) + sin(breathingPhase + s * 0.3f) * 3f)

            val strandPath = Path()
            var first = true

            for (i in 0..steps) {
                val t = i.toFloat() / steps.toFloat()
                val x = startX + t * ribbonSpan
                // Window envelope to pinch waves cleanly at both circle edges
                val envelope = sin(t * PI).toFloat()

                val wave1 = sin(t * 2.8 * PI + strandPhase) * dynamicAmp
                val wave2 = cos(t * 1.5 * PI - strandPhase * 0.8) * (dynamicAmp * 0.35)
                val y = (center.y + strandOffset + (wave1 + wave2) * envelope).toFloat()

                if (first) {
                    strandPath.moveTo(x, y)
                    first = false
                } else {
                    strandPath.lineTo(x, y)
                }
            }

            drawPath(
                path = strandPath,
                brush = ribbonGradient,
                style = Stroke(
                    width = if (s in 5..8) 2.2.dp.toPx() else 1.3.dp.toPx(),
                    cap = StrokeCap.Round,
                ),
                alpha = if (s in 5..8) 0.96f else 0.65f,
            )
        }
    }
}

/**
 * Animated Spectrum Equalizer Bars:
 * 5-7 vertical rounded pill bars with vivid golden-orange-magenta-purple gradient,
 * bouncing in response to real-time audio energy and rhythmic phase.
 */
@Composable
private fun SpectrumBars(
    audioLevel: Float,
    isListening: Boolean,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "spectrumAnim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phaseSpec",
    )

    val barColors = listOf(
        Color(0xFFFFD54F), // Gold
        Color(0xFFFFB300), // Amber
        Color(0xFFFF7043), // Coral Orange
        Color(0xFFF06292), // Pink
        Color(0xFFBA68C8), // Violet
        Color(0xFF7C4DFF), // Purple
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        barColors.forEachIndexed { index, color ->
            val offset = index * 0.9f
            val baseSine = ((sin(phase * 2f + offset) + 1f) / 2f)
            val barHeight = if (isListening) {
                (6.dp + (8.dp * baseSine) + (18.dp * audioLevel)).coerceIn(6.dp, 28.dp)
            } else {
                (5.dp + (4.dp * baseSine)).coerceIn(5.dp, 12.dp)
            }

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(barHeight)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}



/**
 * Dhvani "Find any song" Recognition Interface:
 * Beautifully matches the user's provided UI design:
 * - "Find any song" title and "Play it, sing it, or hum a tune" subtitle
 * - Glowing holographic harmonic orb with wave ribbons and starry particles
 * - Animated spectrum equalizer bars and real-time audio amplitude reactive feedback
 * - "Tap to identify" pill button with golden microphone
 * - True acoustic fingerprint recognition powered by SongRec (Shazam engine)
 * - Result card with album artwork, track title, and artist
 * - Direct Google Sound Search fallback for melody humming
 * - Clean top bar with Back button (history & more options removed as requested)
 */
@Composable
fun PlaySingHumSheet(
    onDismiss: () -> Unit,
    onPlayNow: (String) -> Unit,
    onViewInSearch: (String) -> Unit,
    history: List<String> = emptyList(),
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasAudioPermission = granted
        if (!granted) {
            Toast.makeText(context, "Microphone permission is required to listen", Toast.LENGTH_SHORT).show()
        }
    }

    var isListening by remember { mutableStateOf(false) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var hasAttempted by remember { mutableStateOf(false) }
    var audioLevel by remember { mutableFloatStateOf(0.05f) }
    var progressFraction by remember { mutableFloatStateOf(0f) }
    var statusText by remember { mutableStateOf("Listening for music...") }
    var foundTrack by remember { mutableStateOf<ShazamResult.Success?>(null) }
    var recognitionJob by remember { mutableStateOf<Job?>(null) }

    val animatedAudioLevel by animateFloatAsState(
        targetValue = if (isListening) audioLevel else 0.05f,
        animationSpec = tween(100, easing = FastOutSlowInEasing),
        label = "audioLevelSmooth",
    )

    fun startIdentification() {
        if (!hasAudioPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        recognitionJob?.cancel()
        foundTrack = null
        isListening = true
        isAnalyzing = false
        progressFraction = 0f
        statusText = "Listening for music or melody..."

        recognitionJob = coroutineScope.launch {
            val result = ShazamRecognitionClient.recognizeFromMic(
                onAudioLevel = { lvl ->
                    audioLevel = lvl
                },
                onProgress = { prog ->
                    progressFraction = prog
                    if (prog >= 0.95f) {
                        isListening = false
                        isAnalyzing = true
                        statusText = "Dhvani is listening…..."
                    }
                }
            )

            isListening = false
            isAnalyzing = false
            audioLevel = 0.05f
            hasAttempted = true

            when (result) {
                is ShazamResult.Success -> {
                    foundTrack = result
                    statusText = "Found: ${result.title} • ${result.artist}"
                    // Screen stays static! User can now choose to Play in Dhvani or Go to search list
                }
                is ShazamResult.NoMatch -> {
                    statusText = "No song found. Tap to identify again"
                }
                is ShazamResult.Error -> {
                    statusText = result.message
                }
            }
        }
    }

    LaunchedEffect(hasAudioPermission) {
        if (hasAudioPermission) {
            startIdentification()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            recognitionJob?.cancel()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        BackHandler(onBack = onDismiss)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F1117)), // Deep cosmic obsidian matching screenshot
        ) {
            // Top Navigation Bar: Back Arrow on left, clean and empty on right (history & more deleted)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            // Main Content: Exact layout from user's screenshot
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // 1. Header Typography
                Column(
                    modifier = Modifier.padding(top = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "Find any song",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp,
                        ),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "Play it, sing it, or hum a tune",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal,
                        ),
                        color = Color(0xFFA5A9B8),
                        textAlign = TextAlign.Center,
                    )
                }

                // 2. Glowing Holographic Harmonic Orb or Track Found Card
                if (foundTrack != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(vertical = 16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(180.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0xFF1E212B))
                                .border(
                                    1.5.dp,
                                    Brush.linearGradient(
                                        listOf(Color(0xFFFFB300), Color(0xFFBA68C8))
                                    ),
                                    RoundedCornerShape(24.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!foundTrack?.artworkUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = foundTrack?.artworkUrl,
                                    contentDescription = foundTrack?.title,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.GraphicEq,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(64.dp)
                                )
                            }
                        }

                        Text(
                            text = foundTrack?.title.orEmpty(),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = foundTrack?.artist.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 15.sp
                            ),
                            color = Color(0xFFA5A9B8),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    HolographicSongOrb(
                        audioLevel = animatedAudioLevel,
                        isListening = isListening,
                        modifier = Modifier
                            .size(270.dp)
                            .padding(vertical = 12.dp),
                    )
                }

                // 3. Spectrum Equalizer Bars & Status Caption
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    SpectrumBars(
                        audioLevel = animatedAudioLevel,
                        isListening = isListening,
                    )

                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Normal,
                        ),
                        color = Color(0xFFECEFF4).copy(alpha = 0.88f),
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }

                // 4. Action Buttons
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (foundTrack != null) {
                        val trackQuery = "${foundTrack?.title} ${foundTrack?.artist}"

                        // "Play in Dhvani" Primary Button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.82f)
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFFFFB300), Color(0xFFBA68C8))
                                    )
                                )
                                .clickable {
                                    onPlayNow(trackQuery)
                                    onDismiss()
                                }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.Black,
                                    modifier = Modifier.size(22.dp),
                                )
                                Text(
                                    text = "Play in Dhvani",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = Color.Black,
                                )
                            }
                        }

                        // "Go to search list" Secondary Button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.82f)
                                .clip(CircleShape)
                                .background(Color(0xFF222533).copy(alpha = 0.92f))
                                .border(
                                    width = 1.dp,
                                    color = Color(0xFF3B4055),
                                    shape = CircleShape,
                                )
                                .clickable {
                                    onViewInSearch(trackQuery)
                                    onDismiss()
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                                    contentDescription = "Search List",
                                    tint = Color(0xFFECEFF4),
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = "Go to search list",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                    color = Color(0xFFECEFF4),
                                )
                            }
                        }
                    } else {
                        // Pill Action Button: "Tap to identify" / "Tap to identify again"
                        val buttonText = when {
                            isListening -> "Listening..."
                            isAnalyzing -> "Searching..."
                            hasAttempted -> "Tap to identify again"
                            else -> "Tap to identify"
                        }

                        val buttonIcon = when {
                            isListening -> Icons.Rounded.GraphicEq
                            hasAttempted -> Icons.Rounded.Refresh
                            else -> Icons.Rounded.Mic
                        }

                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFF222533).copy(alpha = 0.92f))
                                .border(
                                    width = 1.dp,
                                    brush = Brush.horizontalGradient(
                                        listOf(
                                            Color(0xFFFFB300).copy(alpha = 0.40f),
                                            Color(0xFFBA68C8).copy(alpha = 0.40f),
                                            Color(0xFF7C4DFF).copy(alpha = 0.35f),
                                        ),
                                    ),
                                    shape = CircleShape,
                                )
                                .clickable(enabled = !isListening && !isAnalyzing, onClick = { startIdentification() })
                                .padding(horizontal = 32.dp, vertical = 14.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(
                                    imageVector = buttonIcon,
                                    contentDescription = "Identify",
                                    tint = Color(0xFFFFB74D),
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    text = buttonText,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 0.2.sp,
                                    ),
                                    color = Color(0xFFECEFF4),
                                )
                            }
                        }

                        // Google Sound Search alternative option (for singing/humming melody) - ONLY when no song found yet
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    val launched = launchGoogleSoundSearch(context)
                                    if (launched) {
                                        onDismiss()
                                    } else {
                                        Toast.makeText(context, "Google App not installed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = "Google Sound Search",
                                    tint = Color(0xFFA5A9B8),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Try Google Hum Search",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = Color(0xFFA5A9B8)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

