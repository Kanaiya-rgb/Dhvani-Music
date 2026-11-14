package com.music.dhvani.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Placeholder sleeve for playlists, folders, and cached songs that don't have a sleeve image.
 * Uses a soft diagonal gradient with the entity's glyph centered.
 */
@Composable
fun CoverPlaceholder(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    base: Color = MaterialTheme.colorScheme.surfaceVariant,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    iconFraction: Float = 0.36f,
) {
    Box(
        modifier = modifier.background(
            Brush.linearGradient(listOf(lerp(base, tint, 0.16f), base)),
        ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxSize(iconFraction),
        )
    }
}

