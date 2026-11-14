package com.music.dhvani.ui.components

import android.graphics.Matrix
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.rectangle
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath

/**
 * Official Material 3 Expressive Loading Indicator shapes as defined in:
 * https://m3.material.io/components/loading-indicator/guidelines
 * https://m3.material.io/styles/shape/overview-principles
 */
object M3ExpressiveShapes {
    val Cookie4Sided = RoundedPolygon.star(
        numVerticesPerRadius = 4,
        radius = 1f,
        innerRadius = 0.55f,
        rounding = CornerRounding(0.4f, 0.5f),
    )

    val Sunny = RoundedPolygon.star(
        numVerticesPerRadius = 8,
        radius = 1f,
        innerRadius = 0.75f,
        rounding = CornerRounding(0.25f),
    )

    val Clover4Leaf = RoundedPolygon.rectangle(
        width = 1f,
        height = 1f,
        rounding = CornerRounding(0.42f),
    )

    val SoftBurst = RoundedPolygon.star(
        numVerticesPerRadius = 12,
        radius = 1f,
        innerRadius = 0.82f,
        rounding = CornerRounding(0.18f),
    )

    val Circle = RoundedPolygon.circle(
        numVertices = 8,
        radius = 1f,
    )

    val DefaultMorphSequence = listOf(
        Morph(Cookie4Sided, Sunny),
        Morph(Sunny, Clover4Leaf),
        Morph(Clover4Leaf, SoftBurst),
        Morph(SoftBurst, Circle),
        Morph(Circle, Cookie4Sided),
    )
}

/**
 * Material 3 Expressive Shape-Morphing Loading Indicator.
 *
 * Loops smoothly through M3 Expressive shapes (Cookie4Sided, Sunny, Clover4Leaf,
 * SoftBurst, Circle) while rotating continuously.
 */
@Composable
fun M3ExpressiveLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 28.dp,
    morphs: List<Morph> = remember { M3ExpressiveShapes.DefaultMorphSequence },
) {
    val transition = rememberInfiniteTransition(label = "m3_expressive_loading")

    val totalDurationMs = morphs.size * 550

    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "m3_loading_rotation",
    )

    val sequenceProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = morphs.size.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = totalDurationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "m3_loading_morph_progress",
    )

    val androidMatrix = remember { Matrix() }

    Canvas(
        modifier = modifier
            .size(size)
            .rotate(rotation),
    ) {
        val w = this.size.width
        val h = this.size.height

        val activeIndex = sequenceProgress.toInt().coerceIn(0, morphs.lastIndex)
        val fraction = (sequenceProgress - activeIndex).coerceIn(0f, 1f)

        val activeMorph = morphs[activeIndex]

        val androidPath = activeMorph.toPath(fraction)

        androidMatrix.reset()
        // Center the shape around (0,0) and scale to canvas bounds
        androidMatrix.setScale(w * 0.48f, h * 0.48f)
        androidMatrix.postTranslate(w / 2f, h / 2f)
        androidPath.transform(androidMatrix)

        drawPath(
            path = androidPath.asComposePath(),
            color = color,
        )
    }
}

/**
 * Material 3 Contained Loading Indicator.
 *
 * Wraps the shape-morphing indicator in a rounded/elevated M3 container surface,
 * as recommended in M3 guidelines for pull-to-refresh and floating wait states.
 */
@Composable
fun M3ContainedLoadingIndicator(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    indicatorColor: Color = MaterialTheme.colorScheme.primary,
    containerSize: Dp = 40.dp,
    indicatorSize: Dp = 28.dp,
    elevation: Dp = 4.dp,
) {
    Surface(
        modifier = modifier.size(containerSize),
        shape = CircleShape,
        color = containerColor,
        shadowElevation = elevation,
        tonalElevation = 2.dp,
    ) {
        Box(
            modifier = Modifier.size(containerSize),
            contentAlignment = Alignment.Center,
        ) {
            M3ExpressiveLoadingIndicator(
                color = indicatorColor,
                size = indicatorSize,
            )
        }
    }
}
