package com.music.dhvani.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.music.dhvani.data.settings.UiDesignStyle

val LocalUiDesignStyle = compositionLocalOf { UiDesignStyle.DEFAULT }

/**
 * Applies active [UiDesignStyle] styling attributes to any card or surface:
 * - Dynamic outer/inner shadows
 * - Beveled & specular gradient borders
 * - Translucent glass & soft clay / neumorphic textures
 */
@Composable
fun Modifier.uiDesignCard(
    style: UiDesignStyle = LocalUiDesignStyle.current,
    shape: Shape = RoundedCornerShape(16.dp),
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    borderWidth: Dp? = null,
    onClick: (() -> Unit)? = null,
): Modifier {
    val clickableModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    return when (style) {
        UiDesignStyle.DEFAULT -> {
            val bg = Brush.verticalGradient(
                listOf(
                    backgroundColor,
                    backgroundColor.copy(
                        red = (backgroundColor.red * 0.82f).coerceAtLeast(0.04f),
                        green = (backgroundColor.green * 0.82f).coerceAtLeast(0.04f),
                        blue = (backgroundColor.blue * 0.82f).coerceAtLeast(0.04f),
                    ),
                ),
            )
            this
                .clip(shape)
                .background(bg)
                .border(borderWidth ?: 1.dp, Color.White.copy(alpha = 0.08f), shape)
                .then(clickableModifier)
        }

        UiDesignStyle.SKEUOMORPHISM -> {
            val skeuoGradient = Brush.verticalGradient(
                colors = listOf(
                    backgroundColor.copy(alpha = 0.96f),
                    backgroundColor.copy(
                        red = (backgroundColor.red * 0.55f).coerceAtLeast(0.03f),
                        green = (backgroundColor.green * 0.55f).coerceAtLeast(0.03f),
                        blue = (backgroundColor.blue * 0.55f).coerceAtLeast(0.03f),
                    ),
                ),
            )
            this
                .shadow(elevation = 9.dp, shape = shape, spotColor = Color.Black.copy(alpha = 0.95f), ambientColor = Color.Black.copy(alpha = 0.6f))
                .clip(shape)
                .background(skeuoGradient)
                .border(
                    width = borderWidth ?: 1.8.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.50f), // Physical top metallic bevel highlight
                            Color.White.copy(alpha = 0.15f),
                            Color.Black.copy(alpha = 0.70f), // Bottom shadow groove
                        ),
                    ),
                    shape = shape,
                )
                .then(clickableModifier)
        }

        UiDesignStyle.NEUMORPHISM -> {
            val neumorphicShape = if (shape is RoundedCornerShape) RoundedCornerShape(18.dp) else shape
            val neumorphicGradient = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF232731).copy(alpha = 0.7f),
                    backgroundColor.copy(alpha = 0.65f),
                    Color(0xFF13151A),
                ),
            )
            this
                .shadow(
                    elevation = 7.dp,
                    shape = neumorphicShape,
                    ambientColor = Color.White.copy(alpha = 0.18f),
                    spotColor = Color.Black.copy(alpha = 0.85f),
                )
                .clip(neumorphicShape)
                .background(neumorphicGradient)
                .border(
                    width = borderWidth ?: 1.2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.28f), // Top-left extruded light
                            Color.White.copy(alpha = 0.04f),
                            Color.Black.copy(alpha = 0.65f), // Bottom-right soft recess
                        ),
                    ),
                    shape = neumorphicShape,
                )
                .then(clickableModifier)
        }

        UiDesignStyle.GLASSMORPHISM -> {
            val glassBg = Brush.verticalGradient(
                colors = listOf(
                    backgroundColor.copy(alpha = 0.55f),
                    backgroundColor.copy(alpha = 0.30f),
                ),
            )
            this
                .shadow(elevation = 6.dp, shape = shape, spotColor = Color.Black.copy(alpha = 0.45f))
                .clip(shape)
                .background(glassBg)
                .border(
                    width = borderWidth ?: 1.2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.60f), // Frosted glowing perimeter
                            Color.White.copy(alpha = 0.12f),
                        ),
                    ),
                    shape = shape,
                )
                .then(clickableModifier)
        }

        UiDesignStyle.CLAYMORPHISM -> {
            val clayShape = if (shape is RoundedCornerShape) RoundedCornerShape(22.dp) else shape
            val clayBg = Brush.verticalGradient(
                colors = listOf(
                    backgroundColor.copy(alpha = 0.95f),
                    backgroundColor.copy(
                        red = (backgroundColor.red * 0.70f).coerceAtLeast(0.05f),
                        green = (backgroundColor.green * 0.70f).coerceAtLeast(0.05f),
                        blue = (backgroundColor.blue * 0.70f).coerceAtLeast(0.05f),
                    ),
                ),
            )
            this
                .shadow(
                    elevation = 11.dp,
                    shape = clayShape,
                    ambientColor = Color.Black.copy(alpha = 0.5f),
                    spotColor = backgroundColor.copy(alpha = 0.6f),
                )
                .clip(clayShape)
                .background(clayBg)
                .border(
                    width = borderWidth ?: 2.2.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.45f), // Inflated top light
                            Color.White.copy(alpha = 0.06f),
                        ),
                    ),
                    shape = clayShape,
                )
                .then(clickableModifier)
        }

        UiDesignStyle.MINIMALISM -> {
            this
                .clip(shape)
                .background(
                    backgroundColor.copy(
                        red = (backgroundColor.red * 0.45f).coerceAtLeast(0.06f),
                        green = (backgroundColor.green * 0.45f).coerceAtLeast(0.06f),
                        blue = (backgroundColor.blue * 0.45f).coerceAtLeast(0.06f),
                        alpha = 0.95f,
                    ),
                )
                .border(borderWidth ?: 1.dp, Color.White.copy(alpha = 0.18f), shape)
                .then(clickableModifier)
        }

        UiDesignStyle.LIQUID_GLASS -> {
            val liquidBg = Brush.radialGradient(
                colors = listOf(
                    backgroundColor.copy(alpha = 0.75f),
                    Color(0xFF0D0F14).copy(alpha = 0.90f),
                ),
            )
            val iridescentBorder = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF00F2FE).copy(alpha = 0.75f), // Cyan highlight
                    Color(0xFFFF8A3D).copy(alpha = 0.65f), // Warm amber sheen
                    Color(0xFF9B51E0).copy(alpha = 0.75f), // Violet glow
                ),
            )
            this
                .shadow(elevation = 8.dp, shape = shape, spotColor = Color(0xFF00F2FE).copy(alpha = 0.35f))
                .clip(shape)
                .background(liquidBg)
                .border(width = borderWidth ?: 1.5.dp, brush = iridescentBorder, shape = shape)
                .then(clickableModifier)
        }

        UiDesignStyle.BENTO_GRID -> {
            val bentoShape = if (shape is RoundedCornerShape) RoundedCornerShape(18.dp) else shape
            val bentoBorder = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.28f),
                    Color.White.copy(alpha = 0.08f),
                ),
            )
            val bentoBg = Brush.verticalGradient(
                colors = listOf(
                    backgroundColor.copy(alpha = 0.85f),
                    Color(0xFF14161C),
                ),
            )
            this
                .shadow(elevation = 4.dp, shape = bentoShape, spotColor = Color.Black.copy(alpha = 0.5f))
                .clip(bentoShape)
                .background(bentoBg)
                .border(borderWidth ?: 1.5.dp, bentoBorder, bentoShape)
                .then(clickableModifier)
        }

        UiDesignStyle.SPATIAL_UI -> {
            val spatialShape = if (shape is RoundedCornerShape) RoundedCornerShape(20.dp) else shape
            val spatialGlow = Brush.sweepGradient(
                colors = listOf(
                    Color(0xFF00E5FF).copy(alpha = 0.55f),
                    Color(0xFFFF4081).copy(alpha = 0.45f),
                    Color(0xFF7C4DFF).copy(alpha = 0.55f),
                    Color(0xFF00E5FF).copy(alpha = 0.55f),
                ),
            )
            this
                .shadow(elevation = 14.dp, shape = spatialShape, spotColor = Color(0xFF00E5FF).copy(alpha = 0.45f), ambientColor = Color.Black.copy(alpha = 0.5f))
                .clip(spatialShape)
                .background(backgroundColor.copy(alpha = 0.65f))
                .border(borderWidth ?: 1.4.dp, spatialGlow, spatialShape)
                .then(clickableModifier)
        }
    }
}
