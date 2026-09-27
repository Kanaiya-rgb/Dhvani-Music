package com.music.dhvani.ui.player

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.music.dhvani.data.lyrics.LyricLine
import com.music.dhvani.data.model.Song
import com.music.dhvani.data.model.artworkAt
import com.music.dhvani.ui.haptics.Haptic
import com.music.dhvani.ui.haptics.rememberHaptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

/**
 * Visual color themes for the generated lyrics share card.
 */
enum class LyricsCardTheme(
    val displayName: String,
    val previewColors: List<Color>,
) {
    ARTWORK("Ambient Art", listOf(Color(0xFF8B5CF6), Color(0xFFEC4899))),
    MIDNIGHT("Velvet Night", listOf(Color(0xFF2563EB), Color(0xFF1E1B4B))),
    SUNSET("Sunset Ember", listOf(Color(0xFFF43F5E), Color(0xFFEA580C))),
    AURORA("Emerald Beat", listOf(Color(0xFF10B981), Color(0xFF065F46))),
    OLED("Obsidian Pure", listOf(Color(0xFF3F3F46), Color(0xFF09090B))),
}

/**
 * Aspect ratio formats for shared lyrics card.
 * COMPACT: 4:5 (1080 x 1350) for WhatsApp chat & Instagram feed.
 * STORY: 9:16 (1080 x 1920) for Instagram Stories & WhatsApp Status.
 */
enum class LyricsCardFormat(val displayName: String, val width: Int, val height: Int) {
    COMPACT("Compact (4:5)", 1080, 1350),
    STORY("Story (9:16)", 1080, 1920),
}

private const val MAX_SHARE_LINES = 7

/**
 * Spotify / Apple Music style Lyrics Share Card bottom modal sheet.
 * Allows users to choose 1 to 4 lines of lyrics, pick card style themes,
 * preview the card live, and share directly to Instagram Stories, WhatsApp,
 * System Share, or save directly as image to device Gallery.
 */
@Composable
fun LyricsShareCardSheet(
    song: Song,
    lines: List<LyricLine>,
    initialSelectedLine: LyricLine? = null,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()

    var isVisible by remember { mutableStateOf(false) }
    var selectedTheme by remember { mutableStateOf(LyricsCardTheme.ARTWORK) }
    var selectedFormat by remember { mutableStateOf(LyricsCardFormat.COMPACT) }
    var activeTab by remember { mutableStateOf(0) } // 0 = Choose Lines, 1 = Themes & Format
    var isExporting by remember { mutableStateOf(false) }
    var exportProgressMessage by remember { mutableStateOf<String?>(null) }

    // Usable non-gap lines for picking
    val selectableLines = remember(lines) { lines.filter { it.text.isNotBlank() } }

    // 1 to 7 selected lines state
    val selectedLines = remember {
        mutableStateListOf<LyricLine>().apply {
            if (initialSelectedLine != null && initialSelectedLine.text.isNotBlank()) {
                add(initialSelectedLine)
            } else if (selectableLines.isNotEmpty()) {
                add(selectableLines.first())
            }
        }
    }

    // Cached album artwork bitmap for palette extraction & canvas export (HD 1080px)
    var artworkBitmap by remember(song.videoId) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(song.thumbnailUrl) {
        val url = song.artworkAt(1080) ?: song.thumbnailUrl ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            runCatching {
                val req = ImageRequest.Builder(context)
                    .data(url)
                    .allowHardware(false)
                    .build()
                val result = (SingletonImageLoader.get(context).execute(req) as? SuccessResult)?.image?.toBitmap()
                withContext(Dispatchers.Main) { artworkBitmap = result }
            }
        }
    }

    val dismissSmoothly = {
        if (isVisible) {
            isVisible = false
            scope.launch {
                delay(220)
                onDismiss()
            }
        }
    }

    LaunchedEffect(Unit) { isVisible = true }

    // Export & Share logic
    fun executeShare(
        action: (Uri, String) -> Unit,
    ) {
        if (selectedLines.isEmpty()) {
            Toast.makeText(context, "Select at least 1 line to share", Toast.LENGTH_SHORT).show()
            return
        }
        if (isExporting) return
        isExporting = true
        exportProgressMessage = "Generating card..."
        scope.launch {
            try {
                val bitmap = renderLyricsCardBitmap(
                    context = context,
                    song = song,
                    lines = selectedLines.toList(),
                    theme = selectedTheme,
                    format = selectedFormat,
                    artworkBitmap = artworkBitmap,
                )
                val uri = cacheLyricsShareImage(context, bitmap)
                if (uri != null) {
                    val caption = "\"${selectedLines.joinToString("\n") { it.text }}\"\n\n— ${song.title} · ${song.artist}\nShared via Dhvani Music"
                    action(uri, caption)
                } else {
                    Toast.makeText(context, "Failed to create share image", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error sharing: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                isExporting = false
                exportProgressMessage = null
            }
        }
    }

    Dialog(
        onDismissRequest = { dismissSmoothly() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        val scrimAlpha by animateFloatAsState(
            targetValue = if (isVisible) 0.65f else 0f,
            animationSpec = tween(durationMillis = 240),
            label = "scrim",
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
                .clickable { dismissSmoothly() },
            contentAlignment = Alignment.BottomCenter,
        ) {
            AnimatedVisibility(
                visible = isVisible,
                enter = androidx.compose.animation.slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(durationMillis = 280, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                ) + androidx.compose.animation.fadeIn(tween(200)),
                exit = androidx.compose.animation.slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(durationMillis = 220, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                ) + androidx.compose.animation.fadeOut(tween(180)),
            ) {
                Surface(
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = Color(0xFF13161F),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                    shadowElevation = 24.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.92f)
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null,
                        ) {},
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 10.dp, bottom = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Top drag handle
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(4.5.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.28f)),
                        )

                        Spacer(Modifier.height(10.dp))

                        // Header row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text(
                                    text = "Share Lyrics",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                    ),
                                    color = Color.White,
                                )
                                Text(
                                    text = "${selectedLines.size}/$MAX_SHARE_LINES lines selected",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }

                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.08f),
                                modifier = Modifier
                                    .size(34.dp)
                                    .clickable {
                                        haptics.play(Haptic.Tap)
                                        dismissSmoothly()
                                    },
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Close",
                                        tint = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        // 1. PINNED LIVE CARD PREVIEW WITH AMBIENT GLOW BACKDROP
                        val glowColor = remember(selectedTheme, artworkBitmap) {
                            when (selectedTheme) {
                                LyricsCardTheme.ARTWORK -> {
                                    artworkBitmap?.let { extractDominantColor(it) } ?: Color(0xFF7C3AED)
                                }
                                LyricsCardTheme.MIDNIGHT -> Color(0xFF2563EB)
                                LyricsCardTheme.SUNSET -> Color(0xFFF43F5E)
                                LyricsCardTheme.AURORA -> Color(0xFF10B981)
                                LyricsCardTheme.OLED -> Color(0xFF52525B)
                            }
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(295.dp)
                                .padding(horizontal = 16.dp, vertical = 2.dp),
                        ) {
                            // Ambient diffuse backlight halo
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight(0.92f)
                                    .aspectRatio(if (selectedFormat == LyricsCardFormat.COMPACT) 4f / 5f else 9f / 16f)
                                    .blur(36.dp, BlurredEdgeTreatment.Unbounded)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                glowColor.copy(alpha = 0.55f),
                                                glowColor.copy(alpha = 0.15f),
                                                Color.Transparent,
                                            ),
                                        ),
                                        RoundedCornerShape(32.dp),
                                    ),
                            )

                            LyricsPreviewCard(
                                song = song,
                                selectedLines = selectedLines.toList(),
                                theme = selectedTheme,
                                format = selectedFormat,
                                artworkBitmap = artworkBitmap,
                                modifier = Modifier
                                    .fillMaxHeight(0.96f)
                                    .aspectRatio(if (selectedFormat == LyricsCardFormat.COMPACT) 4f / 5f else 9f / 16f),
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        // 2. SEGMENTED NAVIGATION TABS: [ 📝 Lines (x) ] | [ 🎨 Themes & Format ]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            // Tab 0: Choose Lines
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (activeTab == 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.06f),
                                border = BorderStroke(
                                    1.dp,
                                    if (activeTab == 0) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.12f),
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        haptics.play(Haptic.Tap)
                                        activeTab = 0
                                    },
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 7.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "Lyrics Lines (${selectedLines.size})",
                                        fontSize = 12.5.sp,
                                        fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Medium,
                                        color = if (activeTab == 0) Color.White else Color.White.copy(alpha = 0.65f),
                                    )
                                }
                            }

                            // Tab 1: Themes & Format
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (activeTab == 1) MaterialTheme.colorScheme.primary.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.06f),
                                border = BorderStroke(
                                    1.dp,
                                    if (activeTab == 1) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.12f),
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        haptics.play(Haptic.Tap)
                                        activeTab = 1
                                    },
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 7.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "Themes & Format",
                                        fontSize = 12.5.sp,
                                        fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Medium,
                                        color = if (activeTab == 1) Color.White else Color.White.copy(alpha = 0.65f),
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // 3. TAB CONTENT
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp),
                        ) {
                            if (activeTab == 0) {
                                // LYRICS LINES SELECTION LIST
                                Column(modifier = Modifier.fillMaxSize()) {
                                    if (selectedLines.isNotEmpty()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                text = "TAP LINES TO ADD OR REMOVE",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 1.sp,
                                                    fontSize = 10.sp,
                                                ),
                                                color = Color.White.copy(alpha = 0.45f),
                                            )
                                            Text(
                                                text = "Clear selection",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 11.5.sp,
                                                ),
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.clickable {
                                                    haptics.play(Haptic.Tap)
                                                    selectedLines.clear()
                                                },
                                            )
                                        }
                                    }

                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        items(selectableLines) { line ->
                                            val isSelected = selectedLines.contains(line)
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (isSelected) Color(0xFF1E283D) else Color.White.copy(alpha = 0.04f),
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.06f),
                                                ),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        haptics.play(Haptic.Tap)
                                                        if (isSelected) {
                                                            if (selectedLines.size > 1) {
                                                                selectedLines.remove(line)
                                                            } else {
                                                                Toast.makeText(context, "Keep at least 1 line", Toast.LENGTH_SHORT).show()
                                                            }
                                                        } else {
                                                            if (selectedLines.size >= MAX_SHARE_LINES) {
                                                                selectedLines.removeAt(0)
                                                            }
                                                            selectedLines.add(line)
                                                        }
                                                    },
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 12.dp, vertical = 9.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(19.dp)
                                                            .clip(CircleShape)
                                                            .background(
                                                                if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.12f),
                                                            ),
                                                        contentAlignment = Alignment.Center,
                                                    ) {
                                                        if (isSelected) {
                                                            Icon(
                                                                imageVector = Icons.Rounded.Check,
                                                                contentDescription = null,
                                                                tint = Color.Black,
                                                                modifier = Modifier.size(13.dp),
                                                            )
                                                        }
                                                    }

                                                    Spacer(Modifier.width(10.dp))

                                                    Text(
                                                        text = line.text,
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            fontSize = 13.sp,
                                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                                        ),
                                                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.70f),
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                // THEMES & FORMAT CUSTOMIZER
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(14.dp),
                                ) {
                                    // 1. Format Switcher
                                    Column {
                                        Text(
                                            text = "ASPECT RATIO FORMAT",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.sp,
                                                fontSize = 10.sp,
                                            ),
                                            color = Color.White.copy(alpha = 0.45f),
                                            modifier = Modifier.padding(bottom = 6.dp),
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            LyricsCardFormat.entries.forEach { format ->
                                                val isSelected = format == selectedFormat
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.05f),
                                                    border = BorderStroke(
                                                        1.dp,
                                                        if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.12f),
                                                    ),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clickable {
                                                            haptics.play(Haptic.Tap)
                                                            selectedFormat = format
                                                        },
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                                                        horizontalArrangement = Arrangement.Center,
                                                        verticalAlignment = Alignment.CenterVertically,
                                                    ) {
                                                        Text(
                                                            text = format.displayName,
                                                            fontSize = 12.sp,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.65f),
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // 2. Theme Switcher
                                    Column {
                                        Text(
                                            text = "CARD THEME & ATMOSPHERE",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.sp,
                                                fontSize = 10.sp,
                                            ),
                                            color = Color.White.copy(alpha = 0.45f),
                                            modifier = Modifier.padding(bottom = 6.dp),
                                        )

                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            LyricsCardTheme.entries.forEach { theme ->
                                                val isSelected = selectedTheme == theme
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.04f),
                                                    border = BorderStroke(
                                                        1.dp,
                                                        if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.10f),
                                                    ),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            haptics.play(Haptic.Tap)
                                                            selectedTheme = theme
                                                        },
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 14.dp, vertical = 9.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(16.dp)
                                                                    .clip(CircleShape)
                                                                    .background(Brush.linearGradient(theme.previewColors))
                                                                    .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                                                            )
                                                            Text(
                                                                text = theme.displayName,
                                                                fontSize = 13.sp,
                                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.75f),
                                                            )
                                                        }

                                                        if (isSelected) {
                                                            Icon(
                                                                imageVector = Icons.Rounded.Check,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.size(16.dp),
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

                        Spacer(Modifier.height(10.dp))

                        // Bottom Actions Row: WhatsApp, Instagram Stories, System Share, Save Image
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            if (isExporting) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = exportProgressMessage ?: "Generating...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.85f),
                                    )
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    // WhatsApp Button with authentic gradient
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color.Transparent,
                                        modifier = Modifier
                                            .weight(1.1f)
                                            .height(48.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(Color(0xFF25D366), Color(0xFF128C7E))
                                                )
                                            )
                                            .clickable {
                                                haptics.play(Haptic.Tap)
                                                executeShare { uri, caption ->
                                                    shareToWhatsApp(context, uri, caption)
                                                }
                                            },
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "WhatsApp",
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                ),
                                                color = Color.White,
                                            )
                                        }
                                    }

                                    // Instagram Stories Button with official Instagram signature gradient
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color.Transparent,
                                        modifier = Modifier
                                            .weight(1.1f)
                                            .height(48.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(
                                                        Color(0xFF833AB4),
                                                        Color(0xFFFD1D1D),
                                                        Color(0xFFFCB045),
                                                    )
                                                )
                                            )
                                            .clickable {
                                                haptics.play(Haptic.Tap)
                                                executeShare { uri, _ ->
                                                    shareToInstagramStories(context, uri)
                                                }
                                            },
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "Instagram",
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                ),
                                                color = Color.White,
                                            )
                                        }
                                    }

                                    // System Share Button with frosted glass
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color(0xFF202533),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                                        modifier = Modifier
                                            .weight(0.95f)
                                            .height(48.dp)
                                            .clickable {
                                                haptics.play(Haptic.Tap)
                                                executeShare { uri, caption ->
                                                    shareGeneral(context, uri, caption)
                                                }
                                            },
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.fillMaxSize(),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Share,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp),
                                            )
                                            Spacer(Modifier.width(5.dp))
                                            Text(
                                                text = "Share",
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 13.sp,
                                                ),
                                                color = Color.White,
                                            )
                                        }
                                    }

                                    // Save Image / Download button
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color(0xFF202533),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clickable {
                                                haptics.play(Haptic.Tap)
                                                if (isExporting) return@clickable
                                                isExporting = true
                                                exportProgressMessage = "Saving to Gallery..."
                                                scope.launch {
                                                    val bitmap = renderLyricsCardBitmap(
                                                        context = context,
                                                        song = song,
                                                        lines = selectedLines.toList(),
                                                        theme = selectedTheme,
                                                        format = selectedFormat,
                                                        artworkBitmap = artworkBitmap,
                                                    )
                                                    val saved = saveLyricsToGallery(context, bitmap, song.title)
                                                    isExporting = false
                                                    exportProgressMessage = null
                                                    if (saved) {
                                                        Toast.makeText(context, "Saved to Pictures/DhvaniMusic", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Rounded.Download,
                                                contentDescription = "Save to Gallery",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp),
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
    }
}

/**
 * Beautiful Spotify / Apple Music style live preview card in Compose.
 */
@Composable
private fun LyricsPreviewCard(
    song: Song,
    selectedLines: List<LyricLine>,
    theme: LyricsCardTheme,
    format: LyricsCardFormat,
    artworkBitmap: Bitmap?,
    modifier: Modifier = Modifier,
) {
    val primaryColor = remember(artworkBitmap) {
        artworkBitmap?.let { extractDominantColor(it) } ?: Color(0xFF7C3AED)
    }

    val brush = remember(theme, primaryColor) {
        when (theme) {
            LyricsCardTheme.ARTWORK -> {
                Brush.linearGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.95f),
                        primaryColor.copy(alpha = 0.50f),
                        Color(0xFF0A0D14),
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(900f, 1300f),
                )
            }
            LyricsCardTheme.MIDNIGHT -> Brush.linearGradient(
                colors = listOf(Color(0xFF2563EB), Color(0xFF1E1B4B), Color(0xFF030712)),
                start = Offset(0f, 0f),
                end = Offset(900f, 1300f),
            )
            LyricsCardTheme.SUNSET -> Brush.linearGradient(
                colors = listOf(Color(0xFFF43F5E), Color(0xFFEA580C), Color(0xFF18080C)),
                start = Offset(0f, 0f),
                end = Offset(900f, 1300f),
            )
            LyricsCardTheme.AURORA -> Brush.linearGradient(
                colors = listOf(Color(0xFF10B981), Color(0xFF0284C7), Color(0xFF022C22)),
                start = Offset(0f, 0f),
                end = Offset(900f, 1300f),
            )
            LyricsCardTheme.OLED -> Brush.linearGradient(
                colors = listOf(Color(0xFF27272A), Color(0xFF141416), Color(0xFF000000)),
                start = Offset(0f, 0f),
                end = Offset(900f, 1300f),
            )
        }
    }

    Surface(
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(
            1.2.dp,
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.40f),
                    Color.White.copy(alpha = 0.08f),
                    Color.White.copy(alpha = 0.22f),
                )
            )
        ),
        shadowElevation = 24.dp,
        modifier = modifier,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Base dark luxury canvas
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF080B12))
            )

            // 2. High-resolution song cover art (center-cropped, clear, non-pixelated across ALL themes)
            if (artworkBitmap != null) {
                Image(
                    bitmap = artworkBitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(if (theme == LyricsCardTheme.OLED) 0.32f else 0.44f),
                )
            } else {
                AsyncImage(
                    model = song.artworkAt(1080) ?: song.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(if (theme == LyricsCardTheme.OLED) 0.32f else 0.44f),
                )
            }

            // 3. Dynamic theme-specific color wash & vignette overlay
            val overlayBrush = remember(theme, primaryColor) {
                when (theme) {
                    LyricsCardTheme.ARTWORK -> Brush.verticalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.50f),
                            primaryColor.copy(alpha = 0.22f),
                            Color(0xFF04060C).copy(alpha = 0.90f),
                        )
                    )
                    LyricsCardTheme.MIDNIGHT -> Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF2563EB).copy(alpha = 0.60f),
                            Color(0xFF1E1B4B).copy(alpha = 0.45f),
                            Color(0xFF030712).copy(alpha = 0.92f),
                        )
                    )
                    LyricsCardTheme.SUNSET -> Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF43F5E).copy(alpha = 0.60f),
                            Color(0xFFEA580C).copy(alpha = 0.42f),
                            Color(0xFF18080C).copy(alpha = 0.92f),
                        )
                    )
                    LyricsCardTheme.AURORA -> Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF10B981).copy(alpha = 0.60f),
                            Color(0xFF0284C7).copy(alpha = 0.42f),
                            Color(0xFF022C22).copy(alpha = 0.92f),
                        )
                    )
                    LyricsCardTheme.OLED -> Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF27272A).copy(alpha = 0.42f),
                            Color(0xFF141416).copy(alpha = 0.55f),
                            Color(0xFF000000).copy(alpha = 0.95f),
                        )
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(overlayBrush)
            )

            // 2. Soft specular glass highlight in top-left
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.16f), Color.Transparent),
                            center = Offset(80f, 80f),
                            radius = 450f,
                        )
                    ),
            )

            // 3. Subtle editorial watermark quote mark behind lyrics
            Text(
                text = "“",
                fontSize = if (format == LyricsCardFormat.COMPACT) 130.sp else 160.sp,
                fontWeight = FontWeight.Black,
                color = Color.White.copy(alpha = 0.08f),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 10.dp, y = (-20).dp),
            )

            // 4. Content Column
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (format == LyricsCardFormat.COMPACT) 16.dp else 18.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // Top: Subtle luxury brand pill + Quote icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF38BDF8)),
                        )
                        Text(
                            text = "DHVANI MUSIC",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.4.sp,
                                fontSize = 9.sp,
                            ),
                            color = Color.White.copy(alpha = 0.85f),
                        )
                    }

                    Icon(
                        imageVector = Icons.Rounded.FormatQuote,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.40f),
                        modifier = Modifier.size(16.dp),
                    )
                }

                // Middle: Selected Lyrics lines (Vertically Centered)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(
                            when {
                                selectedLines.size <= 2 -> 13.dp
                                selectedLines.size <= 4 -> 8.dp
                                else -> 5.dp
                            }
                        ),
                    ) {
                        val fontSize = when {
                            selectedLines.size <= 2 -> 19.sp
                            selectedLines.size <= 4 -> 15.5.sp
                            else -> 13.sp
                        }
                        val lineHeight = when {
                            selectedLines.size <= 2 -> 26.sp
                            selectedLines.size <= 4 -> 21.5.sp
                            else -> 18.sp
                        }

                        if (selectedLines.isEmpty()) {
                            Text(
                                text = "Select up to $MAX_SHARE_LINES lines of lyrics...",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                color = Color.White.copy(alpha = 0.50f),
                            )
                        } else {
                            selectedLines.forEach { line ->
                                Text(
                                    text = line.text,
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = fontSize,
                                        lineHeight = lineHeight,
                                        letterSpacing = (-0.2).sp,
                                        shadow = Shadow(
                                            color = Color.Black.copy(alpha = 0.65f),
                                            offset = Offset(0f, 2f),
                                            blurRadius = 6f,
                                        ),
                                    ),
                                    color = Color.White,
                                    maxLines = if (selectedLines.size > 4) 2 else 3,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }

                // Bottom: Track Info Capsule with Album Art Thumbnail + Title + Artist + Equalizer
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Black.copy(alpha = 0.45f),
                    border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.20f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.25f)),
                            modifier = Modifier.size(34.dp),
                        ) {
                            AsyncImage(
                                model = song.thumbnailUrl,
                                contentDescription = song.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }

                        Spacer(Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
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
                                color = Color.White.copy(alpha = 0.70f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        // Equalizer sound bars indicator
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 4.dp),
                        ) {
                            Box(modifier = Modifier.width(2.5.dp).height(8.dp).clip(CircleShape).background(Color(0xFF38BDF8)))
                            Box(modifier = Modifier.width(2.5.dp).height(13.dp).clip(CircleShape).background(Color(0xFF38BDF8)))
                            Box(modifier = Modifier.width(2.5.dp).height(7.dp).clip(CircleShape).background(Color(0xFF38BDF8)))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Generates a high-resolution poster Bitmap using Android Canvas.
 * Supports Compact (1080 x 1350) and Story (1080 x 1920).
 */
private suspend fun renderLyricsCardBitmap(
    context: Context,
    song: Song,
    lines: List<LyricLine>,
    theme: LyricsCardTheme,
    format: LyricsCardFormat,
    artworkBitmap: Bitmap?,
): Bitmap = withContext(Dispatchers.Default) {
    val posterWidth = format.width
    val posterHeight = format.height
    val bitmap = Bitmap.createBitmap(posterWidth, posterHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // 1. Draw Background Gradient
    drawPosterBackground(canvas, theme, artworkBitmap, posterWidth.toFloat(), posterHeight.toFloat())

    // 2. Draw Card Container in center
    val marginHorizontal = if (format == LyricsCardFormat.COMPACT) 60f else 76f
    val cardLeft = marginHorizontal
    val cardRight = posterWidth - marginHorizontal
    val cardTop = if (format == LyricsCardFormat.COMPACT) 72f else 220f
    val cardBottom = if (format == LyricsCardFormat.COMPACT) posterHeight - 84f else posterHeight - 220f
    val cardRect = RectF(cardLeft, cardTop, cardRight, cardBottom)

    val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(85, 0, 0, 0) // rich crystal dark glass
    }
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(75, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
    }
    val cardCornerRadius = if (format == LyricsCardFormat.COMPACT) 48f else 54f
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, cardPaint)
    canvas.drawRoundRect(cardRect, cardCornerRadius, cardCornerRadius, borderPaint)

    val contentLeft = cardLeft + (if (format == LyricsCardFormat.COMPACT) 54f else 64f)
    val contentRight = cardRight - (if (format == LyricsCardFormat.COMPACT) 54f else 64f)

    // 3. Card Top: Sleek Brand Header (DHVANI MUSIC + Quote)
    val brandDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#38BDF8")
    }
    val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT_BOLD
        textSize = 24f
        letterSpacing = 0.14f
        color = AndroidColor.argb(225, 255, 255, 255)
    }
    val topHeaderY = cardTop + (if (format == LyricsCardFormat.COMPACT) 62f else 76f)
    canvas.drawCircle(contentLeft + 8f, topHeaderY - 8f, 7f, brandDotPaint)
    canvas.drawText("DHVANI MUSIC", contentLeft + 28f, topHeaderY, brandPaint)

    val quoteTopPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT_BOLD
        textSize = 42f
        color = AndroidColor.argb(120, 255, 255, 255)
    }
    canvas.drawText("”", contentRight - 22f, topHeaderY + 4f, quoteTopPaint)

    // 4. Card Bottom: Track Info Capsule (Song Title & Artist at Bottom)
    val bottomBoxHeight = if (format == LyricsCardFormat.COMPACT) 130f else 144f
    val bottomBoxBottom = cardBottom - (if (format == LyricsCardFormat.COMPACT) 36f else 46f)
    val bottomBoxTop = bottomBoxBottom - bottomBoxHeight
    val bottomRect = RectF(contentLeft, bottomBoxTop, contentRight, bottomBoxBottom)
    val bottomBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(140, 0, 0, 0)
    }
    val bottomBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(55, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    canvas.drawRoundRect(bottomRect, 28f, 28f, bottomBgPaint)
    canvas.drawRoundRect(bottomRect, 28f, 28f, bottomBorderPaint)

    val artSize = if (format == LyricsCardFormat.COMPACT) 92f else 102f
    val artLeft = contentLeft + 18f
    val artTop = bottomBoxTop + (bottomBoxHeight - artSize) / 2f
    drawArtworkTile(canvas, artworkBitmap, artLeft, artTop, artSize)

    val textX = artLeft + artSize + 22f
    val songTextWidth = contentRight - textX - 68f

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT_BOLD
        textSize = if (format == LyricsCardFormat.COMPACT) 30f else 34f
        color = AndroidColor.WHITE
    }
    val artistPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT
        textSize = if (format == LyricsCardFormat.COMPACT) 23f else 26f
        color = AndroidColor.argb(195, 255, 255, 255)
    }

    val ellipsisedTitle = ellipsize(song.title, titlePaint, songTextWidth)
    val ellipsisedArtist = ellipsize(song.artist, artistPaint, songTextWidth)

    canvas.drawText(ellipsisedTitle, textX, artTop + (if (format == LyricsCardFormat.COMPACT) 38f else 43f), titlePaint)
    canvas.drawText(ellipsisedArtist, textX, artTop + (if (format == LyricsCardFormat.COMPACT) 74f else 84f), artistPaint)

    // Equalizer bars on right
    val eqRight = contentRight - 24f
    val eqY = artTop + artSize / 2f
    val eqBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#38BDF8")
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 5.5f
    }
    canvas.drawLine(eqRight - 28f, eqY - 12f, eqRight - 28f, eqY + 12f, eqBarPaint)
    canvas.drawLine(eqRight - 14f, eqY - 20f, eqRight - 14f, eqY + 20f, eqBarPaint)
    canvas.drawLine(eqRight, eqY - 9f, eqRight, eqY + 9f, eqBarPaint)

    // 5. Subtle watermark quote in background
    val watermarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT_BOLD
        textSize = if (format == LyricsCardFormat.COMPACT) 240f else 300f
        color = AndroidColor.argb(20, 255, 255, 255)
    }
    canvas.drawText("“", contentRight - 160f, (topHeaderY + bottomBoxTop) / 2f + 50f, watermarkPaint)

    // 6. Middle: Selected Lyrics lines with word wrapping & dynamic vertical centering
    val lyricsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT_BOLD
        textSize = when {
            lines.size <= 2 -> 62f
            lines.size <= 4 -> 50f
            else -> 40f
        }
        color = AndroidColor.WHITE
        setShadowLayer(10f, 0f, 3f, AndroidColor.argb(160, 0, 0, 0))
    }
    val lineHeight = when {
        lines.size <= 2 -> 82f
        lines.size <= 4 -> 68f
        else -> 54f
    }
    val lineSpacing = when {
        lines.size <= 2 -> 26f
        lines.size <= 4 -> 20f
        else -> 14f
    }

    val textMaxWidth = contentRight - contentLeft
    val lyricsAreaTop = topHeaderY + 40f
    val lyricsAreaBottom = bottomBoxTop - 36f
    val availableHeight = lyricsAreaBottom - lyricsAreaTop

    val allWrappedLines = lines.map { line ->
        wrapText(line.text, lyricsPaint, textMaxWidth)
    }
    val totalLineCount = allWrappedLines.sumOf { it.size }
    val totalTextHeight = totalLineCount * lineHeight + (lines.size - 1).coerceAtLeast(0) * lineSpacing

    var currentY = lyricsAreaTop + maxOf(25f, (availableHeight - totalTextHeight) / 2f) + lineHeight * 0.75f

    for (wrapped in allWrappedLines) {
        for (subLine in wrapped) {
            if (currentY + lineHeight < bottomBoxTop - 15f) {
                canvas.drawText(subLine, contentLeft, currentY, lyricsPaint)
                currentY += lineHeight
            }
        }
        currentY += lineSpacing
    }

    // 7. Bottom Poster Tagline
    val bottomBrandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT_BOLD
        textSize = if (format == LyricsCardFormat.COMPACT) 22f else 26f
        letterSpacing = 0.08f
        color = AndroidColor.argb(130, 255, 255, 255)
        textAlign = Paint.Align.CENTER
    }
    val posterTagY = if (format == LyricsCardFormat.COMPACT) posterHeight - 26f else posterHeight - 96f
    canvas.drawText("Listen on Dhvani Music", posterWidth / 2f, posterTagY, bottomBrandPaint)

    bitmap
}

/**
 * Draws the poster background based on chosen theme and artwork colors.
 */
private fun drawPosterBackground(
    canvas: Canvas,
    theme: LyricsCardTheme,
    artworkBitmap: Bitmap?,
    width: Float,
    height: Float,
) {
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // 1. Base dark luxury background
    bgPaint.color = AndroidColor.parseColor("#080B12")
    canvas.drawRect(0f, 0f, width, height, bgPaint)

    // 2. Draw full-resolution song cover center-cropped without downscaling artifacts (for ALL themes)
    if (artworkBitmap != null) {
        val scale = maxOf(width / artworkBitmap.width.toFloat(), height / artworkBitmap.height.toFloat())
        val scaledW = artworkBitmap.width * scale
        val scaledH = artworkBitmap.height * scale
        val dx = (width - scaledW) / 2f
        val dy = (height - scaledH) / 2f
        val matrix = Matrix().apply {
            setScale(scale, scale)
            postTranslate(dx, dy)
        }
        val artPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            alpha = (255 * (if (theme == LyricsCardTheme.OLED) 0.32f else 0.44f)).toInt()
        }
        canvas.drawBitmap(artworkBitmap, matrix, artPaint)
    }

    // 3. Dynamic theme palette tint + dark vignette so lyrics remain crystal clear
    val (c1, c2, c3) = when (theme) {
        LyricsCardTheme.ARTWORK -> {
            val dom = artworkBitmap?.let { extractDominantColorInt(it) } ?: AndroidColor.parseColor("#7C3AED")
            val hsl = FloatArray(3)
            ColorUtils.colorToHSL(dom, hsl)
            hsl[1] = (hsl[1] * 1.25f).coerceIn(0.55f, 0.95f)
            hsl[2] = 0.38f
            val topColor = ColorUtils.HSLToColor(hsl)
            Triple(
                ColorUtils.setAlphaComponent(topColor, 125),
                ColorUtils.setAlphaComponent(topColor, 55),
                AndroidColor.argb(230, 4, 6, 12),
            )
        }
        LyricsCardTheme.MIDNIGHT -> Triple(
            AndroidColor.argb(150, 37, 99, 235),
            AndroidColor.argb(115, 30, 27, 75),
            AndroidColor.argb(235, 3, 7, 18),
        )
        LyricsCardTheme.SUNSET -> Triple(
            AndroidColor.argb(150, 244, 63, 94),
            AndroidColor.argb(105, 234, 88, 12),
            AndroidColor.argb(235, 24, 8, 12),
        )
        LyricsCardTheme.AURORA -> Triple(
            AndroidColor.argb(150, 16, 185, 129),
            AndroidColor.argb(105, 2, 132, 199),
            AndroidColor.argb(235, 2, 44, 34),
        )
        LyricsCardTheme.OLED -> Triple(
            AndroidColor.argb(105, 39, 39, 42),
            AndroidColor.argb(140, 20, 20, 22),
            AndroidColor.argb(242, 0, 0, 0),
        )
    }

    val tintGradient = LinearGradient(
        0f, 0f, 0f, height,
        intArrayOf(c1, c2, c3),
        floatArrayOf(0f, 0.48f, 1f),
        Shader.TileMode.CLAMP,
    )
    bgPaint.shader = tintGradient
    canvas.drawRect(0f, 0f, width, height, bgPaint)
    bgPaint.shader = null

    // Specular lighting accent at the top-left
    val radialPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    val radialGradient = RadialGradient(
        width * 0.25f, height * 0.18f, width * 0.75f,
        intArrayOf(AndroidColor.argb(70, 255, 255, 255), AndroidColor.TRANSPARENT),
        floatArrayOf(0f, 1f),
        Shader.TileMode.CLAMP,
    )
    radialPaint.shader = radialGradient
    canvas.drawRect(0f, 0f, width, height, radialPaint)
    radialPaint.shader = null
}

private fun drawArtworkTile(canvas: Canvas, bitmap: Bitmap?, x: Float, y: Float, size: Float) {
    val bounds = RectF(x, y, x + size, y + size)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    if (bitmap != null) {
        val scale = size / minOf(bitmap.width, bitmap.height).toFloat()
        val matrix = Matrix().apply {
            setScale(scale, scale)
            postTranslate(
                x - (bitmap.width * scale - size) / 2f,
                y - (bitmap.height * scale - size) / 2f,
            )
        }
        paint.shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
            setLocalMatrix(matrix)
        }
        canvas.drawRoundRect(bounds, 24f, 24f, paint)
        paint.shader = null
    } else {
        paint.color = AndroidColor.parseColor("#262A38")
        canvas.drawRoundRect(bounds, 24f, 24f, paint)
    }
}

private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
    val words = text.split(" ")
    val lines = mutableListOf<String>()
    var currentLine = StringBuilder()

    for (word in words) {
        val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
        if (paint.measureText(testLine) <= maxWidth) {
            currentLine = StringBuilder(testLine)
        } else {
            if (currentLine.isNotEmpty()) {
                lines.add(currentLine.toString())
            }
            currentLine = StringBuilder(word)
        }
    }
    if (currentLine.isNotEmpty()) {
        lines.add(currentLine.toString())
    }
    return lines
}

private fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
    if (paint.measureText(text) <= maxWidth) return text
    var count = text.length
    while (count > 0 && paint.measureText(text.take(count) + "…") > maxWidth) {
        count--
    }
    return text.take(count).trimEnd() + "…"
}

private fun extractDominantColor(bitmap: Bitmap): Color {
    val rgb = extractDominantColorInt(bitmap)
    return Color(rgb)
}

private fun extractDominantColorInt(bitmap: Bitmap): Int {
    return runCatching {
        val palette = Palette.from(bitmap).maximumColorCount(24).generate()
        val swatch = palette.vibrantSwatch
            ?: palette.lightVibrantSwatch
            ?: palette.darkVibrantSwatch
            ?: palette.dominantSwatch
        val baseColor = swatch?.rgb ?: AndroidColor.parseColor("#7C3AED")
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(baseColor, hsl)
        // Boost saturation for rich, lively, non-muddy gradients
        hsl[1] = (hsl[1] * 1.35f).coerceIn(0.50f, 0.95f)
        hsl[2] = hsl[2].coerceIn(0.30f, 0.46f)
        ColorUtils.HSLToColor(hsl)
    }.getOrNull() ?: AndroidColor.parseColor("#7C3AED")
}

private suspend fun cacheLyricsShareImage(context: Context, bitmap: Bitmap): Uri? = withContext(Dispatchers.IO) {
    runCatching {
        val folder = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(folder, "dhvani_lyrics_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }.getOrNull()
}

private suspend fun saveLyricsToGallery(context: Context, bitmap: Bitmap, songTitle: String): Boolean = withContext(Dispatchers.IO) {
    val cleanTitle = songTitle.replace(Regex("[^a-zA-Z0-9.-]"), "_").take(30)
    val name = "Dhvani_Lyrics_${cleanTitle}_${System.currentTimeMillis()}.png"
    runCatching {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/DhvaniMusic")
            }
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Failed to insert MediaStore row")
        context.contentResolver.openOutputStream(uri)?.use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        } ?: error("Failed to open stream")
        true
    }.getOrDefault(false)
}

/**
 * Direct Instagram Stories share with fallback to general chooser.
 */
private fun shareToInstagramStories(context: Context, uri: Uri) {
    val intent = Intent("com.instagram.share.ADD_TO_STORY").apply {
        type = "image/png"
        putExtra("interactive_asset_uri", uri)
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
    }
    if (context.packageManager.resolveActivity(intent, 0) != null) {
        context.startActivity(intent)
    } else {
        val chooser = Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }, "Share to Stories")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}

/**
 * Direct WhatsApp share with fallback to general chooser.
 */
private fun shareToWhatsApp(context: Context, uri: Uri, caption: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, caption)
        setPackage("com.whatsapp")
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
    }
    if (context.packageManager.resolveActivity(intent, 0) != null) {
        context.startActivity(intent)
    } else {
        val chooser = Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, caption)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }, "Share via WhatsApp")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}

/**
 * General system share sheet for all apps.
 */
private fun shareGeneral(context: Context, uri: Uri, caption: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, caption)
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    }
    val chooser = Intent.createChooser(intent, "Share Lyrics Card")
    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(chooser)
}
