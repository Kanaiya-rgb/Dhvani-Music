package com.music.dhvani.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun WavySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(),
    isPlaying: Boolean = true,
    enabled: Boolean = true,
    strokeWidth: Dp = 4.dp,
    thumbRadius: Dp = 8.dp,
    wavelength: Dp = 24.dp,
) {
    val density = LocalDensity.current
    val strokeWidthPx = with(density) { strokeWidth.toPx() }
    val thumbRadiusPx = with(density) { thumbRadius.toPx() }
    val wavelengthPx = with(density) { wavelength.toPx() }

    val normalizedValue = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start))
        .coerceIn(0f, 1f)

    var isDragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(normalizedValue) }

    val displayValue = if (isDragging) dragValue else normalizedValue

    val animatedAmplitude by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = tween(durationMillis = 200, easing = LinearEasing),
        label = "amplitude",
    )

    var phaseOffset by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        var last = withFrameMillis { it }
        while (isActive) {
            withFrameMillis { now ->
                val dt = (now - last) / 1000f
                phaseOffset += dt * 36f
                phaseOffset %= wavelengthPx
                last = now
            }
        }
    }

    val activeColor = colors.activeTrackColor
    val inactiveColor = colors.inactiveTrackColor
    val thumbColor = colors.thumbColor

    val containerHeight = maxOf(36.dp, thumbRadius * 2 + 16.dp)

    val baseModifier = modifier
        .fillMaxWidth()
        .height(containerHeight)

    val interactiveModifier = if (enabled) {
        baseModifier
            .pointerInput(valueRange) {
                detectTapGestures { offset ->
                    val newValue = (offset.x / size.width).coerceIn(0f, 1f)
                    val mappedValue = valueRange.start + newValue * (valueRange.endInclusive - valueRange.start)
                    onValueChange(mappedValue)
                    onValueChangeFinished?.invoke()
                }
            }
            .pointerInput(valueRange) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        dragValue = (offset.x / size.width).coerceIn(0f, 1f)
                        val mappedValue = valueRange.start + dragValue * (valueRange.endInclusive - valueRange.start)
                        onValueChange(mappedValue)
                    },
                    onDragEnd = {
                        isDragging = false
                        onValueChangeFinished?.invoke()
                    },
                    onDragCancel = {
                        isDragging = false
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        dragValue = (dragValue + dragAmount / size.width).coerceIn(0f, 1f)
                        val mappedValue = valueRange.start + dragValue * (valueRange.endInclusive - valueRange.start)
                        onValueChange(mappedValue)
                    },
                )
            }
    } else {
        baseModifier
    }

    Box(
        modifier = interactiveModifier,
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerY = size.height / 2f
            val totalWidth = size.width
            val progressX = totalWidth * displayValue
            val maxWaveHeight = 4.dp.toPx() * animatedAmplitude

            val activePath = Path()
            val inactivePath = Path()

            val step = 2f
            var x = 0f
            var first = true

            while (x <= totalWidth) {
                val waveY = if (x <= progressX) {
                    centerY + kotlin.math.sin((x - phaseOffset) / wavelengthPx * 2f * Math.PI.toFloat()) * maxWaveHeight
                } else {
                    centerY
                }

                if (x <= progressX) {
                    if (first) {
                        activePath.moveTo(x, waveY)
                        first = false
                    } else {
                        activePath.lineTo(x, waveY)
                    }
                }
                x += step
            }

            if (progressX < totalWidth) {
                inactivePath.moveTo(progressX, centerY)
                inactivePath.lineTo(totalWidth, centerY)
            }

            drawPath(
                path = activePath,
                color = activeColor,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
            )

            drawPath(
                path = inactivePath,
                color = inactiveColor,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
            )

            // Draw thumb circle
            drawCircle(
                color = thumbColor,
                radius = thumbRadiusPx,
                center = Offset(progressX, centerY),
            )
        }
    }
}

@Composable
fun SquigglySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(),
    isPlaying: Boolean = true,
) {
    val primaryColor = colors.activeTrackColor
    val inactiveColor = colors.inactiveTrackColor

    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableFloatStateOf(value) }

    val currentValue = if (isDragging) dragPosition else value
    val duration = valueRange.endInclusive - valueRange.start
    val position = currentValue - valueRange.start

    var phaseOffset by remember { mutableFloatStateOf(0f) }
    var heightFraction by remember { mutableFloatStateOf(if (isPlaying) 1f else 0f) }

    val scope = rememberCoroutineScope()

    val waveLength = 80f
    val lineAmplitude = 6f
    val phaseSpeed = 24f
    val transitionPeriods = 1.5f
    val minWaveEndpoint = 0f
    val matchedWaveEndpoint = 1f
    val transitionEnabled = true

    LaunchedEffect(isPlaying, isDragging) {
        scope.launch {
            val shouldFlatten = !isPlaying || isDragging
            val targetHeight = if (shouldFlatten) 0f else 1f
            val animDuration = if (shouldFlatten) 150 else 200
            val startDelay = if (shouldFlatten) 0L else 30L

            delay(startDelay)

            val animator = Animatable(heightFraction)
            animator.animateTo(
                targetValue = targetHeight,
                animationSpec = tween(
                    durationMillis = animDuration,
                    easing = LinearEasing,
                ),
            ) {
                heightFraction = this.value
            }
        }
    }

    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect

        var lastFrameTime = withFrameMillis { it }
        while (isActive) {
            withFrameMillis { frameTimeMillis ->
                val deltaTime = (frameTimeMillis - lastFrameTime) / 1000f
                phaseOffset += deltaTime * phaseSpeed
                phaseOffset %= waveLength
                lastFrameTime = frameTimeMillis
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .then(
                if (enabled) {
                    Modifier
                        .pointerInput(valueRange) {
                            detectTapGestures { offset ->
                                val newPosition = (offset.x / size.width) * duration
                                val mappedValue = valueRange.start + newPosition.coerceIn(0f, duration)
                                onValueChange(mappedValue)
                                onValueChangeFinished?.invoke()
                            }
                        }
                        .pointerInput(valueRange) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    isDragging = true
                                    val newPosition = (offset.x / size.width) * duration
                                    dragPosition = valueRange.start + newPosition.coerceIn(0f, duration)
                                    onValueChange(dragPosition)
                                },
                                onDragEnd = {
                                    isDragging = false
                                    onValueChangeFinished?.invoke()
                                },
                                onDragCancel = {
                                    isDragging = false
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val newPosition = (change.position.x / size.width) * duration
                                    dragPosition = valueRange.start + newPosition.coerceIn(0f, duration)
                                    onValueChange(dragPosition)
                                },
                            )
                        }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            val strokeWidth = 5.dp.toPx()
            val progress = if (duration > 0f) (position / duration).coerceIn(0f, 1f) else 0f
            val totalWidth = size.width
            val totalProgressPx = totalWidth * progress
            val centerY = size.height / 2f

            val waveProgressPx = if (!transitionEnabled || progress > matchedWaveEndpoint) {
                totalWidth * progress
            } else {
                val t = (progress / matchedWaveEndpoint).coerceIn(0f, 1f)
                totalWidth * (minWaveEndpoint + (matchedWaveEndpoint - minWaveEndpoint) * t)
            }

            fun computeAmplitude(x: Float, sign: Float): Float {
                return if (transitionEnabled) {
                    val length = transitionPeriods * waveLength
                    val coeff = ((waveProgressPx + length / 2f - x) / length).coerceIn(0f, 1f)
                    sign * heightFraction * lineAmplitude * coeff
                } else {
                    sign * heightFraction * lineAmplitude
                }
            }

            val path = Path()
            val waveStart = -phaseOffset - waveLength / 2f
            val waveEnd = if (transitionEnabled) totalWidth else waveProgressPx

            path.moveTo(waveStart, centerY)

            var currentX = waveStart
            var waveSign = 1f
            var currentAmp = computeAmplitude(currentX, waveSign)
            val dist = waveLength / 2f

            while (currentX < waveEnd) {
                waveSign = -waveSign
                val nextX = currentX + dist
                val midX = currentX + dist / 2f
                val nextAmp = computeAmplitude(nextX, waveSign)

                path.cubicTo(
                    midX,
                    centerY + currentAmp,
                    midX,
                    centerY + nextAmp,
                    nextX,
                    centerY + nextAmp,
                )

                currentAmp = nextAmp
                currentX = nextX
            }

            val clipTop = lineAmplitude + strokeWidth
            val disabledAlpha = 77f / 255f
            val inactiveTrackColor = primaryColor.copy(alpha = disabledAlpha)
            val capRadius = strokeWidth / 2f

            fun drawPathSegment(startX: Float, endX: Float, color: Color) {
                if (endX <= startX) return
                clipRect(
                    left = startX,
                    top = centerY - clipTop,
                    right = endX,
                    bottom = centerY + clipTop,
                ) {
                    drawPath(
                        path = path,
                        color = color,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                }
            }

            drawPathSegment(0f, totalProgressPx, primaryColor)
            drawPathSegment(totalProgressPx, totalWidth, inactiveTrackColor)

            fun getWaveY(x: Float): Float {
                val phase = (x - waveStart) / waveLength
                val waveCycle = phase - kotlin.math.floor(phase)
                val waveValue = kotlin.math.cos(waveCycle * 2f * kotlin.math.PI.toFloat())

                val ampCoeff = if (transitionEnabled) {
                    val length = transitionPeriods * waveLength
                    ((waveProgressPx + length / 2f - x) / length).coerceIn(0f, 1f)
                } else {
                    1f
                }

                return centerY + waveValue * lineAmplitude * heightFraction * ampCoeff
            }

            drawCircle(
                color = primaryColor,
                radius = capRadius,
                center = Offset(0f, getWaveY(0f)),
            )

            val endWaveY = getWaveY(totalWidth)
            clipRect(
                left = totalWidth,
                top = centerY - clipTop,
                right = totalWidth + capRadius,
                bottom = centerY + clipTop,
            ) {
                drawCircle(
                    color = inactiveTrackColor,
                    radius = capRadius,
                    center = Offset(totalWidth, endWaveY),
                )
            }

            val barHalfHeight = (lineAmplitude + strokeWidth)
            val barWidth = 5.dp.toPx()

            if (barHalfHeight > 0.5f) {
                drawLine(
                    color = primaryColor,
                    start = Offset(totalProgressPx, centerY - barHalfHeight),
                    end = Offset(totalProgressPx, centerY + barHalfHeight),
                    strokeWidth = barWidth,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSliderTrack(
    sliderState: SliderState,
    modifier: Modifier = Modifier,
    colors: SliderColors = SliderDefaults.colors(),
    trackHeight: Dp = 10.dp,
) {
    val inactiveTrackColor = colors.inactiveTrackColor
    val activeTrackColor = colors.activeTrackColor
    val valueRange = sliderState.valueRange
    Canvas(
        modifier
            .fillMaxWidth()
            .height(trackHeight),
    ) {
        val fraction = ((sliderState.value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
        val centerY = size.height / 2f
        val stroke = trackHeight.toPx()

        drawLine(
            color = inactiveTrackColor,
            start = Offset(0f, centerY),
            end = Offset(size.width, centerY),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = activeTrackColor,
            start = Offset(0f, centerY),
            end = Offset(size.width * fraction, centerY),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

// ==========================================
// 1. NEON GLOW SLIDER (Cyberpunk / Blade Runner style)
// ==========================================
@Composable
fun NeonGlowSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(),
    isPlaying: Boolean = true,
    enabled: Boolean = true,
) {
    val normalizedValue = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
    var isDragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(normalizedValue) }
    val displayValue = if (isDragging) dragValue else normalizedValue

    val transition = rememberInfiniteTransition(label = "neonPulse")
    val pulse by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    val activeColor = colors.activeTrackColor
    val inactiveColor = colors.inactiveTrackColor.copy(alpha = 0.2f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .then(
                if (enabled) {
                    Modifier
                        .pointerInput(valueRange) {
                            detectTapGestures { offset ->
                                val newValue = (offset.x / size.width).coerceIn(0f, 1f)
                                onValueChange(valueRange.start + newValue * (valueRange.endInclusive - valueRange.start))
                                onValueChangeFinished?.invoke()
                            }
                        }
                        .pointerInput(valueRange) {
                            detectHorizontalDragGestures(
                                onDragStart = { offset ->
                                    isDragging = true
                                    dragValue = (offset.x / size.width).coerceIn(0f, 1f)
                                    onValueChange(valueRange.start + dragValue * (valueRange.endInclusive - valueRange.start))
                                },
                                onDragEnd = {
                                    isDragging = false
                                    onValueChangeFinished?.invoke()
                                },
                                onDragCancel = { isDragging = false },
                                onHorizontalDrag = { _, dragAmount ->
                                    dragValue = (dragValue + dragAmount / size.width).coerceIn(0f, 1f)
                                    onValueChange(valueRange.start + dragValue * (valueRange.endInclusive - valueRange.start))
                                },
                            )
                        }
                } else Modifier
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerY = size.height / 2f
            val trackWidth = size.width
            val progressX = (trackWidth * displayValue).coerceIn(0f, trackWidth)

            // Inactive track (hollow dark neon outline)
            drawRoundRect(
                color = inactiveColor,
                topLeft = Offset(0f, centerY - 2.5.dp.toPx()),
                size = Size(trackWidth, 5.dp.toPx()),
                cornerRadius = CornerRadius(10f, 10f),
            )

            // Active Track Multi-layered Neon Bloom
            if (progressX > 0f) {
                // Outer glow 3
                drawLine(
                    color = activeColor.copy(alpha = 0.15f * if (isPlaying) pulse else 1f),
                    start = Offset(0f, centerY),
                    end = Offset(progressX, centerY),
                    strokeWidth = 18.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                // Mid glow 2
                drawLine(
                    color = activeColor.copy(alpha = 0.35f * if (isPlaying) pulse else 1f),
                    start = Offset(0f, centerY),
                    end = Offset(progressX, centerY),
                    strokeWidth = 10.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                // Core beam 1
                drawLine(
                    color = activeColor,
                    start = Offset(0f, centerY),
                    end = Offset(progressX, centerY),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                // Hyper-bright center filament
                drawLine(
                    color = Color.White.copy(alpha = 0.85f),
                    start = Offset(0f, centerY),
                    end = Offset(progressX, centerY),
                    strokeWidth = 1.5.dp.toPx(),
                    cap = StrokeCap.Round,
                )

                // Diamond Neon Knob
                val thumbR = (if (isDragging) 9.dp else 6.5.dp).toPx()
                drawCircle(
                    color = activeColor.copy(alpha = 0.35f),
                    radius = thumbR * 2.2f * if (isPlaying) pulse else 1f,
                    center = Offset(progressX, centerY),
                )
                drawCircle(
                    color = activeColor,
                    radius = thumbR,
                    center = Offset(progressX, centerY),
                )
                drawCircle(
                    color = Color.White,
                    radius = thumbR * 0.5f,
                    center = Offset(progressX, centerY),
                )
            }
        }
    }
}

// ==========================================
// 2. GRADIENT FLOW SLIDER (Aurora Liquid Stream)
// ==========================================
@Composable
fun GradientFlowSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(),
    isPlaying: Boolean = true,
    enabled: Boolean = true,
) {
    val normalizedValue = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
    var isDragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(normalizedValue) }
    val displayValue = if (isDragging) dragValue else normalizedValue

    val transition = rememberInfiniteTransition(label = "gradientFlow")
    val flowOffset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "flow",
    )

    val baseColor = colors.activeTrackColor
    val inactiveColor = colors.inactiveTrackColor.copy(alpha = 0.25f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .then(
                if (enabled) {
                    Modifier
                        .pointerInput(valueRange) {
                            detectTapGestures { offset ->
                                val newValue = (offset.x / size.width).coerceIn(0f, 1f)
                                onValueChange(valueRange.start + newValue * (valueRange.endInclusive - valueRange.start))
                                onValueChangeFinished?.invoke()
                            }
                        }
                        .pointerInput(valueRange) {
                            detectHorizontalDragGestures(
                                onDragStart = { offset ->
                                    isDragging = true
                                    dragValue = (offset.x / size.width).coerceIn(0f, 1f)
                                    onValueChange(valueRange.start + dragValue * (valueRange.endInclusive - valueRange.start))
                                },
                                onDragEnd = {
                                    isDragging = false
                                    onValueChangeFinished?.invoke()
                                },
                                onDragCancel = { isDragging = false },
                                onHorizontalDrag = { _, dragAmount ->
                                    dragValue = (dragValue + dragAmount / size.width).coerceIn(0f, 1f)
                                    onValueChange(valueRange.start + dragValue * (valueRange.endInclusive - valueRange.start))
                                },
                            )
                        }
                } else Modifier
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerY = size.height / 2f
            val trackW = size.width
            val progressX = (trackW * displayValue).coerceIn(0f, trackW)
            val trackH = (if (isDragging) 8.dp else 6.dp).toPx()

            // Inactive track capsule
            drawRoundRect(
                color = inactiveColor,
                topLeft = Offset(0f, centerY - trackH / 2f),
                size = Size(trackW, trackH),
                cornerRadius = CornerRadius(trackH / 2f, trackH / 2f),
            )

            // Dynamic Aurora Gradient across progress
            if (progressX > 0f) {
                val cycleColor1 = baseColor
                val cycleColor2 = Color(
                    red = (baseColor.red * 0.4f + 0.6f).coerceIn(0f, 1f),
                    green = (baseColor.green * 0.9f).coerceIn(0f, 1f),
                    blue = (baseColor.blue * 0.3f + 0.7f).coerceIn(0f, 1f),
                )
                val cycleColor3 = Color(
                    red = (baseColor.red * 0.8f).coerceIn(0f, 1f),
                    green = (baseColor.green * 0.3f + 0.7f).coerceIn(0f, 1f),
                    blue = (baseColor.blue * 0.9f).coerceIn(0f, 1f),
                )

                val shift = if (isPlaying) flowOffset * trackW else 0f
                val brush = Brush.linearGradient(
                    colors = listOf(cycleColor1, cycleColor2, cycleColor3, cycleColor1),
                    start = Offset(shift - trackW, centerY),
                    end = Offset(shift + trackW, centerY),
                )

                drawRoundRect(
                    brush = brush,
                    topLeft = Offset(0f, centerY - trackH / 2f),
                    size = Size(progressX, trackH),
                    cornerRadius = CornerRadius(trackH / 2f, trackH / 2f),
                )

                // Shiny pearl thumb
                val thumbR = (if (isDragging) 8.dp else 6.dp).toPx()
                drawCircle(
                    color = Color.White,
                    radius = thumbR,
                    center = Offset(progressX, centerY),
                )
                drawCircle(
                    color = baseColor,
                    radius = thumbR - 2.dp.toPx(),
                    center = Offset(progressX, centerY),
                )
            }
        }
    }
}

// ==========================================
// 3. COSMIC STARS / PARTICLES SLIDER (Galaxy trail)
// ==========================================
@Composable
fun CosmicSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(),
    isPlaying: Boolean = true,
    enabled: Boolean = true,
) {
    val normalizedValue = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
    var isDragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(normalizedValue) }
    val displayValue = if (isDragging) dragValue else normalizedValue

    val transition = rememberInfiniteTransition(label = "cosmicStars")
    val starPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "stars",
    )

    val primary = colors.activeTrackColor
    val inactive = colors.inactiveTrackColor.copy(alpha = 0.2f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .then(
                if (enabled) {
                    Modifier
                        .pointerInput(valueRange) {
                            detectTapGestures { offset ->
                                val newValue = (offset.x / size.width).coerceIn(0f, 1f)
                                onValueChange(valueRange.start + newValue * (valueRange.endInclusive - valueRange.start))
                                onValueChangeFinished?.invoke()
                            }
                        }
                        .pointerInput(valueRange) {
                            detectHorizontalDragGestures(
                                onDragStart = { offset ->
                                    isDragging = true
                                    dragValue = (offset.x / size.width).coerceIn(0f, 1f)
                                    onValueChange(valueRange.start + dragValue * (valueRange.endInclusive - valueRange.start))
                                },
                                onDragEnd = {
                                    isDragging = false
                                    onValueChangeFinished?.invoke()
                                },
                                onDragCancel = { isDragging = false },
                                onHorizontalDrag = { _, dragAmount ->
                                    dragValue = (dragValue + dragAmount / size.width).coerceIn(0f, 1f)
                                    onValueChange(valueRange.start + dragValue * (valueRange.endInclusive - valueRange.start))
                                },
                            )
                        }
                } else Modifier
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerY = size.height / 2f
            val totalW = size.width
            val progressX = (totalW * displayValue).coerceIn(0f, totalW)

            // Constellation line (inactive)
            drawLine(
                color = inactive,
                start = Offset(0f, centerY),
                end = Offset(totalW, centerY),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round,
            )

            // Galactic core line
            if (progressX > 0f) {
                drawLine(
                    color = primary,
                    start = Offset(0f, centerY),
                    end = Offset(progressX, centerY),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round,
                )

                // Twinkling stars floating along the active path
                val starCount = 8
                for (i in 0 until starCount) {
                    val starFraction = (i.toFloat() / starCount.toFloat())
                    val starX = progressX * starFraction
                    val yOffset = sin(starPhase + i * 1.5f) * 6.dp.toPx()
                    val starAlpha = (0.4f + 0.6f * cos(starPhase + i.toFloat())).coerceIn(0.2f, 1f)
                    val starRadius = (1.5.dp + (i % 3).dp * 0.7f).toPx()

                    drawCircle(
                        color = Color.White.copy(alpha = starAlpha),
                        radius = starRadius,
                        center = Offset(starX, centerY + yOffset),
                    )
                }

                // Supernova Comet Knob
                drawCircle(
                    color = primary.copy(alpha = 0.35f),
                    radius = 12.dp.toPx(),
                    center = Offset(progressX, centerY),
                )
                drawCircle(
                    color = Color.White,
                    radius = 5.dp.toPx(),
                    center = Offset(progressX, centerY),
                )
            }
        }
    }
}

// ==========================================
// 4. LIQUID LAVA SLIDER (Organic morphing droplets)
// ==========================================
@Composable
fun LiquidLavaSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(),
    isPlaying: Boolean = true,
    enabled: Boolean = true,
) {
    val normalizedValue = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
    var isDragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(normalizedValue) }
    val displayValue = if (isDragging) dragValue else normalizedValue

    val transition = rememberInfiniteTransition(label = "lavaMorph")
    val lavaWave by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "lava",
    )

    val primary = colors.activeTrackColor
    val inactive = colors.inactiveTrackColor.copy(alpha = 0.25f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .then(
                if (enabled) {
                    Modifier
                        .pointerInput(valueRange) {
                            detectTapGestures { offset ->
                                val newValue = (offset.x / size.width).coerceIn(0f, 1f)
                                onValueChange(valueRange.start + newValue * (valueRange.endInclusive - valueRange.start))
                                onValueChangeFinished?.invoke()
                            }
                        }
                        .pointerInput(valueRange) {
                            detectHorizontalDragGestures(
                                onDragStart = { offset ->
                                    isDragging = true
                                    dragValue = (offset.x / size.width).coerceIn(0f, 1f)
                                    onValueChange(valueRange.start + dragValue * (valueRange.endInclusive - valueRange.start))
                                },
                                onDragEnd = {
                                    isDragging = false
                                    onValueChangeFinished?.invoke()
                                },
                                onDragCancel = { isDragging = false },
                                onHorizontalDrag = { _, dragAmount ->
                                    dragValue = (dragValue + dragAmount / size.width).coerceIn(0f, 1f)
                                    onValueChange(valueRange.start + dragValue * (valueRange.endInclusive - valueRange.start))
                                },
                            )
                        }
                } else Modifier
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerY = size.height / 2f
            val totalW = size.width
            val progressX = (totalW * displayValue).coerceIn(0f, totalW)

            // Inactive track container
            drawRoundRect(
                color = inactive,
                topLeft = Offset(0f, centerY - 4.dp.toPx()),
                size = Size(totalW, 8.dp.toPx()),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
            )

            if (progressX > 0f) {
                // Organic morphing lava body
                val path = Path()
                path.moveTo(0f, centerY - 4.dp.toPx())

                val steps = 24
                val stepW = progressX / steps
                for (i in 0..steps) {
                    val x = i * stepW
                    val waveOffset = if (isPlaying) sin(lavaWave + i * 0.6f) * 2.5.dp.toPx() else 0f
                    path.lineTo(x, centerY - 4.dp.toPx() - waveOffset)
                }
                path.lineTo(progressX, centerY + 4.dp.toPx())
                for (i in steps downTo 0) {
                    val x = i * stepW
                    val waveOffset = if (isPlaying) cos(lavaWave + i * 0.6f) * 2.5.dp.toPx() else 0f
                    path.lineTo(x, centerY + 4.dp.toPx() + waveOffset)
                }
                path.close()

                drawPath(path = path, color = primary)

                // Giant organic lava drop thumb
                val dropRadius = (if (isDragging) 10.dp else 8.dp).toPx()
                val morphX = if (isPlaying) cos(lavaWave * 2f) * 1.5.dp.toPx() else 0f
                drawCircle(
                    color = primary,
                    radius = dropRadius,
                    center = Offset(progressX + morphX, centerY),
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.7f),
                    radius = dropRadius * 0.35f,
                    center = Offset(progressX - dropRadius * 0.3f, centerY - dropRadius * 0.3f),
                )
            }
        }
    }
}

// ==========================================
// 5. AUDIO VISUALIZER BARS SLIDER (Equalizer dance)
// ==========================================
@Composable
fun AudioBarsSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(),
    isPlaying: Boolean = true,
    enabled: Boolean = true,
) {
    val normalizedValue = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
    var isDragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(normalizedValue) }
    val displayValue = if (isDragging) dragValue else normalizedValue

    val transition = rememberInfiniteTransition(label = "eqBounce")
    val bouncePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "bounce",
    )

    val primary = colors.activeTrackColor
    val inactive = colors.inactiveTrackColor.copy(alpha = 0.22f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .then(
                if (enabled) {
                    Modifier
                        .pointerInput(valueRange) {
                            detectTapGestures { offset ->
                                val newValue = (offset.x / size.width).coerceIn(0f, 1f)
                                onValueChange(valueRange.start + newValue * (valueRange.endInclusive - valueRange.start))
                                onValueChangeFinished?.invoke()
                            }
                        }
                        .pointerInput(valueRange) {
                            detectHorizontalDragGestures(
                                onDragStart = { offset ->
                                    isDragging = true
                                    dragValue = (offset.x / size.width).coerceIn(0f, 1f)
                                    onValueChange(valueRange.start + dragValue * (valueRange.endInclusive - valueRange.start))
                                },
                                onDragEnd = {
                                    isDragging = false
                                    onValueChangeFinished?.invoke()
                                },
                                onDragCancel = { isDragging = false },
                                onHorizontalDrag = { _, dragAmount ->
                                    dragValue = (dragValue + dragAmount / size.width).coerceIn(0f, 1f)
                                    onValueChange(valueRange.start + dragValue * (valueRange.endInclusive - valueRange.start))
                                },
                            )
                        }
                } else Modifier
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerY = size.height / 2f
            val totalW = size.width
            val progressX = (totalW * displayValue).coerceIn(0f, totalW)

            val barCount = 38
            val gap = 3.dp.toPx()
            val barW = (totalW - (barCount - 1) * gap) / barCount

            for (i in 0 until barCount) {
                val barLeft = i * (barW + gap)
                val barCenter = barLeft + barW / 2f
                val isPlayed = barCenter <= progressX

                // Height modulation simulating an audio spectrum
                val factor = ((i * 3.7f) % 7f) / 7f
                val liveBounce = if (isPlaying) sin(bouncePhase + i * 0.7f) * 0.4f + 0.6f else 0.5f
                val barH = (6.dp.toPx() + 18.dp.toPx() * factor * liveBounce)

                val barColor = if (isPlayed) primary else inactive

                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(barLeft, centerY - barH / 2f),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                )
            }

            // High-tech playhead marker
            drawLine(
                color = Color.White,
                start = Offset(progressX, centerY - 14.dp.toPx()),
                end = Offset(progressX, centerY + 14.dp.toPx()),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

// ==========================================
// 6. RETRO LED DOT MATRIX SLIDER (Vintage Hi-Fi VU meter)
// ==========================================
@Composable
fun RetroDotMatrixSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(),
    isPlaying: Boolean = true,
    enabled: Boolean = true,
) {
    val normalizedValue = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
    var isDragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(normalizedValue) }
    val displayValue = if (isDragging) dragValue else normalizedValue

    val primary = colors.activeTrackColor
    val inactive = colors.inactiveTrackColor.copy(alpha = 0.18f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .then(
                if (enabled) {
                    Modifier
                        .pointerInput(valueRange) {
                            detectTapGestures { offset ->
                                val newValue = (offset.x / size.width).coerceIn(0f, 1f)
                                onValueChange(valueRange.start + newValue * (valueRange.endInclusive - valueRange.start))
                                onValueChangeFinished?.invoke()
                            }
                        }
                        .pointerInput(valueRange) {
                            detectHorizontalDragGestures(
                                onDragStart = { offset ->
                                    isDragging = true
                                    dragValue = (offset.x / size.width).coerceIn(0f, 1f)
                                    onValueChange(valueRange.start + dragValue * (valueRange.endInclusive - valueRange.start))
                                },
                                onDragEnd = {
                                    isDragging = false
                                    onValueChangeFinished?.invoke()
                                },
                                onDragCancel = { isDragging = false },
                                onHorizontalDrag = { _, dragAmount ->
                                    dragValue = (dragValue + dragAmount / size.width).coerceIn(0f, 1f)
                                    onValueChange(valueRange.start + dragValue * (valueRange.endInclusive - valueRange.start))
                                },
                            )
                        }
                } else Modifier
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerY = size.height / 2f
            val totalW = size.width
            val progressX = (totalW * displayValue).coerceIn(0f, totalW)

            val rows = 3
            val cols = 36
            val dotRadius = 2.dp.toPx()
            val colGap = (totalW - cols * dotRadius * 2f) / (cols - 1).coerceAtLeast(1)
            val rowGap = 3.dp.toPx()

            for (c in 0 until cols) {
                val cx = c * (dotRadius * 2f + colGap) + dotRadius
                val isLit = cx <= progressX

                for (r in 0 until rows) {
                    val cy = centerY + (r - 1) * (dotRadius * 2f + rowGap)
                    val dotColor = if (isLit) {
                        // Color glow variation
                        if (c >= cols * 0.85f) Color(0xFFFF4D4D) else primary
                    } else inactive

                    drawCircle(
                        color = dotColor,
                        radius = dotRadius,
                        center = Offset(cx, cy),
                    )
                }
            }

            // Knob badge
            drawCircle(
                color = primary,
                radius = 6.dp.toPx(),
                center = Offset(progressX, centerY),
            )
            drawCircle(
                color = Color.White,
                radius = 2.5.dp.toPx(),
                center = Offset(progressX, centerY),
            )
        }
    }
}

// ==========================================
// 7. VINYL GROOVE SLIDER (Realistic Turntable Stylus)
// ==========================================
@Composable
fun VinylGrooveSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(),
    isPlaying: Boolean = true,
    enabled: Boolean = true,
) {
    val normalizedValue = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
    var isDragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(normalizedValue) }
    val displayValue = if (isDragging) dragValue else normalizedValue

    val primary = colors.activeTrackColor
    val inactive = colors.inactiveTrackColor.copy(alpha = 0.25f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .then(
                if (enabled) {
                    Modifier
                        .pointerInput(valueRange) {
                            detectTapGestures { offset ->
                                val newValue = (offset.x / size.width).coerceIn(0f, 1f)
                                onValueChange(valueRange.start + newValue * (valueRange.endInclusive - valueRange.start))
                                onValueChangeFinished?.invoke()
                            }
                        }
                        .pointerInput(valueRange) {
                            detectHorizontalDragGestures(
                                onDragStart = { offset ->
                                    isDragging = true
                                    dragValue = (offset.x / size.width).coerceIn(0f, 1f)
                                    onValueChange(valueRange.start + dragValue * (valueRange.endInclusive - valueRange.start))
                                },
                                onDragEnd = {
                                    isDragging = false
                                    onValueChangeFinished?.invoke()
                                },
                                onDragCancel = { isDragging = false },
                                onHorizontalDrag = { _, dragAmount ->
                                    dragValue = (dragValue + dragAmount / size.width).coerceIn(0f, 1f)
                                    onValueChange(valueRange.start + dragValue * (valueRange.endInclusive - valueRange.start))
                                },
                            )
                        }
                } else Modifier
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerY = size.height / 2f
            val totalW = size.width
            val progressX = (totalW * displayValue).coerceIn(0f, totalW)

            // Multiple micro-grooves (vinyl sound grooves)
            val grooves = 5
            for (g in -2..2) {
                val gy = centerY + g * 2.5.dp.toPx()
                // Inactive groove
                drawLine(
                    color = inactive,
                    start = Offset(0f, gy),
                    end = Offset(totalW, gy),
                    strokeWidth = 1.dp.toPx(),
                )
                // Active played groove
                if (progressX > 0f) {
                    drawLine(
                        color = primary.copy(alpha = if (g == 0) 1f else 0.6f),
                        start = Offset(0f, gy),
                        end = Offset(progressX, gy),
                        strokeWidth = (if (g == 0) 2.dp else 1.2.dp).toPx(),
                    )
                }
            }

            // Stylus Needle head
            val needleW = 8.dp.toPx()
            val needleH = 18.dp.toPx()
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(progressX - needleW / 2f, centerY - needleH / 2f),
                size = Size(needleW, needleH),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
            )
            // Needle ruby tip
            drawCircle(
                color = primary,
                radius = 2.5.dp.toPx(),
                center = Offset(progressX, centerY),
            )
        }
    }
}

// ==========================================
// 8. CYBER BEAM / LASER TRON SLIDER (Futuristic high-energy)
// ==========================================
@Composable
fun CyberBeamSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(),
    isPlaying: Boolean = true,
    enabled: Boolean = true,
) {
    val normalizedValue = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
    var isDragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(normalizedValue) }
    val displayValue = if (isDragging) dragValue else normalizedValue

    val transition = rememberInfiniteTransition(label = "cyberScan")
    val scanOffset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "scan",
    )

    val primary = colors.activeTrackColor
    val inactive = colors.inactiveTrackColor.copy(alpha = 0.2f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .then(
                if (enabled) {
                    Modifier
                        .pointerInput(valueRange) {
                            detectTapGestures { offset ->
                                val newValue = (offset.x / size.width).coerceIn(0f, 1f)
                                onValueChange(valueRange.start + newValue * (valueRange.endInclusive - valueRange.start))
                                onValueChangeFinished?.invoke()
                            }
                        }
                        .pointerInput(valueRange) {
                            detectHorizontalDragGestures(
                                onDragStart = { offset ->
                                    isDragging = true
                                    dragValue = (offset.x / size.width).coerceIn(0f, 1f)
                                    onValueChange(valueRange.start + dragValue * (valueRange.endInclusive - valueRange.start))
                                },
                                onDragEnd = {
                                    isDragging = false
                                    onValueChangeFinished?.invoke()
                                },
                                onDragCancel = { isDragging = false },
                                onHorizontalDrag = { _, dragAmount ->
                                    dragValue = (dragValue + dragAmount / size.width).coerceIn(0f, 1f)
                                    onValueChange(valueRange.start + dragValue * (valueRange.endInclusive - valueRange.start))
                                },
                            )
                        }
                } else Modifier
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerY = size.height / 2f
            val totalW = size.width
            val progressX = (totalW * displayValue).coerceIn(0f, totalW)

            // Geometric tech bounds (brackets on left and right)
            val bracketH = 8.dp.toPx()
            drawLine(inactive, Offset(0f, centerY - bracketH), Offset(0f, centerY + bracketH), 2.dp.toPx())
            drawLine(inactive, Offset(totalW, centerY - bracketH), Offset(totalW, centerY + bracketH), 2.dp.toPx())

            // Laser rail
            drawLine(inactive, Offset(0f, centerY), Offset(totalW, centerY), 3.dp.toPx())

            if (progressX > 0f) {
                // High voltage laser beam
                drawLine(
                    color = primary,
                    start = Offset(0f, centerY),
                    end = Offset(progressX, centerY),
                    strokeWidth = 5.dp.toPx(),
                    cap = StrokeCap.Square,
                )

                // High-speed photon dart traveling down beam
                if (isPlaying) {
                    val dartX = (scanOffset * progressX)
                    drawCircle(
                        color = Color.White,
                        radius = 3.dp.toPx(),
                        center = Offset(dartX, centerY),
                    )
                }

                // Cyber Reticle Cursor
                val reticleSize = 10.dp.toPx()
                drawLine(
                    color = Color.White,
                    start = Offset(progressX, centerY - reticleSize),
                    end = Offset(progressX, centerY + reticleSize),
                    strokeWidth = 3.dp.toPx(),
                )
                drawCircle(
                    color = primary.copy(alpha = 0.4f),
                    radius = reticleSize * 1.4f,
                    center = Offset(progressX, centerY),
                )
            }
        }
    }
}
