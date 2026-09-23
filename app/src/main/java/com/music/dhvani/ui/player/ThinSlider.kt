package com.music.dhvani.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Apple Music's scrubber: a hairline capsule with no thumb knob, which
 * thickens under your finger and settles back when you let go. Material's
 * Slider can't be shaped like this  it always draws a thumb and a tall
 * track  so this is drawn directly.
 */@Composable
fun ThinSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onValueChangeFinished: (() -> Unit)? = null,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    showOverdriveNotch: Boolean = false,
    overdriveColor: Color = Color(0xFFFF5722),
    /**
     * Sends a sheen travelling along the played portion for as long as it is
     * true. Reserved for a transition that genuinely mixed — see
     * [com.music.dhvani.data.settings.AppSettings.smartMixInProgress].
     */
    mixing: Boolean = false,
    /**
     * Span of the track, as fractions of its duration, that the next Automix
     * transition is planned to occupy. Drawn as a brighter stretch of the
     * unplayed bar so the mix is visible before it arrives.
     */
    transitionWindow: ClosedFloatingPointRange<Float>? = null,
    idleHeight: Dp = 7.dp,
    activeHeight: Dp = 12.dp,
    activeColor: Color = Color.White.copy(alpha = 0.92f),
    inactiveColor: Color = Color.White.copy(alpha = 0.26f),
    /** Halfway between the two track colours: visible against unplayed, invisible under played. */
    markerColor: Color = Color.White.copy(alpha = 0.5f),
    /** How far the stream has buffered ahead: drawn as a translucent white bar, YouTube-style. */
    bufferedValue: Float = 0f,
    bufferedColor: Color = Color.White.copy(alpha = 0.38f),
) {
    var dragging by remember { mutableStateOf(false) }
    val height by animateDpAsState(
        targetValue = if (dragging) activeHeight else idleHeight,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "sliderHeight",
    )

    val rangeSpan = (valueRange.endInclusive - valueRange.start).coerceAtLeast(0.001f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            // Generous invisible touch target — the visible bar is only ~7dp.
            .height(activeHeight + 22.dp)
            // One gesture loop for both taps and drags. Two separate detectors
            // — a drag one plus a tap one — meant taps never landed: the drag
            // detector took the pointer and a tap has no drag to report.
            .pointerInput(valueRange) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    dragging = true
                    val fraction = (down.position.x / size.width).coerceIn(0f, 1f)
                    onValueChange(valueRange.start + fraction * rangeSpan)

                    while (true) {
                        val event = awaitPointerEvent()
                        val pointer = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!pointer.pressed) {
                            pointer.consume()
                            break
                        }
                        if (pointer.positionChanged()) {
                            val moveFraction = (pointer.position.x / size.width).coerceIn(0f, 1f)
                            onValueChange(valueRange.start + moveFraction * rangeSpan)
                            pointer.consume()
                        }
                    }

                    dragging = false
                    onValueChangeFinished?.invoke()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height),
        ) {
            val radius = CornerRadius(size.height / 2f)
            drawRoundRect(color = inactiveColor, cornerRadius = radius)

            // YouTube-style buffered progress: drawn above inactive track, below played progress
            if (bufferedValue > valueRange.start) {
                val bufferedFrac = ((bufferedValue - valueRange.start) / rangeSpan).coerceIn(0f, 1f)
                val bufferedWidth = size.width * bufferedFrac
                if (bufferedWidth > 0f) {
                    drawRoundRect(
                        color = bufferedColor,
                        size = Size(bufferedWidth.coerceAtLeast(size.height), size.height),
                        cornerRadius = radius,
                    )
                }
            }
            // Between the two track colours, and drawn *under* the played fill:
            // once the playhead reaches the window the transition is no longer
            // upcoming, and the ordinary progress colour taking it over is what
            // says so.
            transitionWindow?.let { window ->
                val from = size.width * ((window.start - valueRange.start) / rangeSpan).coerceIn(0f, 1f)
                val to = size.width * ((window.endInclusive - valueRange.start) / rangeSpan).coerceIn(0f, 1f)
                if (to > from) {
                    drawRoundRect(
                        color = markerColor,
                        topLeft = Offset(from, 0f),
                        size = Size(to - from, size.height),
                        cornerRadius = radius,
                    )
                }
            }

            // If overdrive notch is enabled (e.g. 100% threshold on 150% volume bar)
            if (showOverdriveNotch && valueRange.endInclusive > 1.0f) {
                val normalFrac = ((1.0f - valueRange.start) / rangeSpan).coerceIn(0f, 1f)
                val notchX = size.width * normalFrac
                drawLine(
                    color = Color.White.copy(alpha = 0.40f),
                    start = Offset(notchX, 0f),
                    end = Offset(notchX, size.height),
                    strokeWidth = 1.5.dp.toPx(),
                )
            }

            val progressFrac = ((value - valueRange.start) / rangeSpan).coerceIn(0f, 1f)
            val filled = size.width * progressFrac

            if (filled > 0f && !mixing) {
                if (showOverdriveNotch && valueRange.endInclusive > 1.0f && value > 1.0f) {
                    val normalFrac = ((1.0f - valueRange.start) / rangeSpan).coerceIn(0f, 1f)
                    val normalWidth = size.width * normalFrac
                    // Draw standard 0-100% portion
                    drawRoundRect(
                        color = activeColor,
                        size = Size(normalWidth.coerceAtLeast(size.height), size.height),
                        cornerRadius = radius,
                    )
                    // Draw 100-150% overdrive portion in warning color
                    val overdriveWidth = (filled - normalWidth).coerceAtLeast(0f)
                    if (overdriveWidth > 0f) {
                        drawRoundRect(
                            color = overdriveColor,
                            topLeft = Offset(normalWidth, 0f),
                            size = Size(overdriveWidth, size.height),
                            cornerRadius = radius,
                        )
                    }
                } else {
                    drawRoundRect(
                        color = activeColor,
                        size = Size(filled.coerceAtLeast(size.height), size.height),
                        cornerRadius = radius,
                    )
                }
            }
        }
        // Composed only while mixing, rather than drawn conditionally inside the
        // Canvas above: an infinite transition keeps requesting frames for as
        // long as it exists, so the cheap way to stop it costing anything is for
        // it not to exist. AnimatedVisibility keeps it alive through the exit
        // fade, so the sheen dies away with the transition instead of vanishing
        // on the frame the mix ends.
        AnimatedVisibility(
            visible = mixing,
            enter = fadeIn(tween(durationMillis = 420)),
            exit = fadeOut(tween(durationMillis = 520)),
        ) {
            MixSheen(height = height)
        }
    }
}

/**
 * A single soft highlight travelling the length of the bar, over and over,
 * while two tracks are being mixed.
 *
 * Drawn as a moving gradient rather than an opacity pulse because a pulse reads
 * as "loading"  the thing every shimmer in every app means  and this is the
 * opposite claim: not that the app is waiting, but that it is doing something.
 * Motion along the bar also points the same way the music is going.
 *
 * Sweeps the **whole** bar rather than the played portion, which the first
 * version did and which made it invisible twice over. A transition happens in
 * the opening seconds of the incoming track, so the played portion is then a
 * few percent of the width  a highlight travelling across that is a flicker at
 * the far left. And the played portion is already white at 0.92 alpha, so white
 * at 0.55 over it resolves to 0.96: the same hue, four percent brighter. The
 * unplayed track sits at 0.26, and that is where a white band actually reads.
 */
@Composable
private fun MixSheen(height: Dp) {
    val transition = rememberInfiniteTransition(label = "mixSheen")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            // Long enough to read as a sweep rather than a flicker, and slow
            // enough not to compete with the music for attention.
            animation = tween(durationMillis = 500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "mixSheenPhase",
    )
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(height),
    ) {
        val band = size.width * BAND_FRACTION
        // Travels from fully off the left edge to fully off the right, so the
        // highlight enters and leaves rather than materialising mid-bar.
        val centre = -band + (size.width + band * 2f) * phase
        drawRoundRect(
            brush = Brush.linearGradient(
                colorStops = arrayOf(
                    0f to Color.Transparent,
                    0.5f to Color.White.copy(alpha = 0.95f),
                    1f to Color.Transparent,
                ),
                start = Offset(centre - band / 2f, 0f),
                end = Offset(centre + band / 2f, 0f),
            ),
            cornerRadius = CornerRadius(size.height / 2f),
        )
    }
}

/** Width of the travelling highlight, as a fraction of the whole bar. */
private const val BAND_FRACTION = 0.7f
