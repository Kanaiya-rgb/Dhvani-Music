package com.music.dhvani.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import com.music.dhvani.ui.theme.DhvaniPrimary
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.dhvani.data.settings.AppSettings
import com.music.dhvani.ui.haptics.Haptic
import com.music.dhvani.ui.haptics.rememberHaptics
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlin.math.abs
import kotlin.math.roundToInt

data class BottomTab(
    val label: String,
    val icon: ImageVector,
)

/**
 * The gap between the pill's glass edge and the tabs inside it.
 *
 * Tighter than the 8 it was, which shows up as a selection indicator reaching
 * closer to the edge on all four sides rather than floating in the middle of a
 * wide margin.
 *
 * Shared with [GlassNavBar], which is meant to measure the same as this bar
 * rather than merely near it.
 */
internal val PILL_INSET = 6.dp

/**
 * Each tab's own vertical padding, and the counterweight to [PILL_INSET].
 *
 * The pill has no height of its own — it is whatever its contents come to — so
 * taking 2dp off the inset above would have shortened the whole bar by 4. The
 * same 2dp is added back here instead, which leaves the bar's outer height
 * exactly where it was and moves the boundary rather than the bar. The two
 * numbers are a pair: change one and the bar's height moves unless the other
 * moves against it.
 */
internal val TAB_VERTICAL_PADDING = 9.dp

/** The gap between a tab's glyph and its label, in both bars. */
internal val TAB_ICON_LABEL_GAP = 2.dp

/**
 * The spring the selection indicator and the tab glyphs both travel on.
 *
 * Damping 0.72 rather than the 0.5 it was: half-damped overshoots two or three
 * times, and a run of diminishing bounces is what makes a control read as a
 * spring rather than as a material. This settles on the second approach — one
 * soft pass beyond the mark and done — which is the difference between bouncy
 * and alive.
 *
 * Stiffness 320 puts the whole movement at roughly a third of a second, quick
 * enough that the tap and the arrival feel like one event.
 */
internal val GlassSpring = spring<Float>(dampingRatio = 0.72f, stiffness = 320f)

/**
 * How far the indicator elongates along its travel, at full stride.
 *
 * This is the part that reads as liquid rather than as a sliding rectangle. A
 * shape crossing a gap under its own momentum does not stay the shape it was:
 * it draws out along the direction it is going and gathers itself back at the
 * end. Driven off how far there is still to go, so it is widest in the middle
 * of the trip and exactly itself once it arrives — no state to keep, and it
 * falls out of a drag for free, since dragging is nothing but a long way still
 * to go.
 *
 * Sixteen percent is enough to be felt and not enough to be caught at: past
 * about a fifth the pill starts reading as a stretched image of itself.
 */
internal const val STRETCH = 0.16f

/**
 * How much of the stretch is taken back out of the indicator's height.
 *
 * Half, not all. Conserving area exactly is what a drop of water does, and it
 * is too much here — the indicator sits behind a glyph that is not deforming
 * with it, and a full counter-squash reads as the pill being crushed rather
 * than drawn. Half keeps the sense of something with a volume to redistribute
 * while leaving the glyph its ground.
 */
internal const val SQUASH = 0.5f

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun FloatingBottomBar(
    tabs: List<BottomTab>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    val pillShape = RoundedCornerShape(percent = 50)
    val container = MaterialTheme.colorScheme.surface
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val useGlass = LocalLiquidGlassEnabled.current && isGlassSupported()
    val reduceAnimation by AppSettings.reduceAnimation.collectAsStateWithLifecycle()
    // The glass settle is exactly the motion "reduce animation" promises to
    // drop — snapping both the indicator's travel and the glyph's pop to
    // their target leaves the tap itself instant rather than eased.
    val glassSpec: AnimationSpec<Float> = if (reduceAnimation) snap() else GlassSpring

    var dragOffset by remember { mutableFloatStateOf(0f) }
    val haptics = rememberHaptics()
    val density = LocalDensity.current
    val currentSelectedIndex by rememberUpdatedState(selectedIndex)

    var rowSize by remember { mutableStateOf(IntSize.Zero) }
    val gapPx = with(density) { 6.dp.toPx() }
    val n = tabs.size

    // With weight(1f) + spacedBy(gap):
    //   tabWidth = (rowWidth - gap*(n-1)) / n
    //   tab i left edge = i * (tabWidth + gap) = i * (rowWidth + gap) / n
    val tabWidthPx = if (rowSize.width > 0 && n > 0) {
        (rowSize.width - gapPx * (n - 1)) / n
    } else 0f
    val tabStepPx = if (rowSize.width > 0 && n > 0) {
        (rowSize.width + gapPx) / n
    } else 0f

    var lastValidIndex by remember { mutableIntStateOf(if (selectedIndex >= 0) selectedIndex else 0) }
    if (selectedIndex >= 0) {
        lastValidIndex = selectedIndex
    }

    val pillTargetPx = if (tabStepPx > 0f) {
        (if (selectedIndex >= 0) selectedIndex else lastValidIndex) * tabStepPx + dragOffset
    } else 0f

    val pillAlpha by animateFloatAsState(
        targetValue = if (selectedIndex >= 0) 1f else 0f,
        animationSpec = glassSpec,
        label = "pillAlpha",
    )

    val animatedPillOffset by animateFloatAsState(
        targetValue = pillTargetPx,
        animationSpec = glassSpec,
        label = "pillOffset",
    )

    // How much of a tab's stride is still ahead of the indicator: 0 at rest,
    // toward 1 in the middle of a move or under a drag that has run away from
    // it. The stretch below is a function of this and nothing else, which is
    // what keeps it honest — the shape can only be deformed while it is
    // actually behind where it is going.
    val lag = if (tabStepPx > 0f) {
        (abs(pillTargetPx - animatedPillOffset) / tabStepPx).coerceIn(0f, 1f)
    } else {
        0f
    }

    var lastHapticTab by remember { mutableIntStateOf(selectedIndex) }

    LaunchedEffect(selectedIndex) { dragOffset = 0f }

    Box(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = PAGE_GUTTER)
            .padding(bottom = 2.dp)
            .fillMaxWidth()
            .clip(pillShape)
            .then(
                if (reduceDynamicBlur) {
                    Modifier.background(container)
                } else if (useGlass) {
                    Modifier.liquidGlass(shape = pillShape)
                } else {
                    Modifier.optimizedHazeEffect(
                        state = hazeState,
                        style = HazeMaterials.regular(container),
                    )
                },
            )
            .border(
                if (useGlass) 0.85.dp else GLASS_EDGE_WIDTH,
                if (useGlass) GlassPrismaticEdgeBrush else Brush.linearGradient(listOf(GLASS_EDGE_COLOR, GLASS_EDGE_COLOR)),
                pillShape,
            )
            .padding(horizontal = PILL_INSET, vertical = PILL_INSET),
    ) {
        val chromaticAberration by AppSettings.liquidGlassChromaticAberration.collectAsStateWithLifecycle()
        val depthEffect by AppSettings.liquidGlassDepthEffect.collectAsStateWithLifecycle()
        val customAccent by AppSettings.accentColor.collectAsStateWithLifecycle()
        val dynamicTheme by AppSettings.dynamicTheme.collectAsStateWithLifecycle()
        val isDark = MaterialTheme.colorScheme.surface.luminance() <= 0.5f

        val selectedColor = when {
            customAccent != null -> Color(customAccent!!)
            dynamicTheme && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S &&
                MaterialTheme.colorScheme.primary != DhvaniPrimary -> MaterialTheme.colorScheme.primary
            else -> Color(0xFFFA243C)
        }
        val unselectedColor = if (isDark) Color.White.copy(alpha = 0.88f) else Color(0xFF222428)

        if (tabWidthPx > 0f) {
            Box(
                modifier = Modifier
                    .width(with(density) { tabWidthPx.toDp() })
                    .height(with(density) { rowSize.height.toDp() })
                    .graphicsLayer {
                        translationX = animatedPillOffset
                        // Around its own centre, so the indicator draws out
                        // both ways rather than growing a tail off one edge
                        scaleX = 1f + lag * STRETCH
                        alpha = pillAlpha
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (useGlass) {
                    LiquidGlass3DBubble(
                        modifier = Modifier.fillMaxSize(),
                        chromaticAberration = chromaticAberration,
                        depthEffect = depthEffect,
                        accentColor = selectedColor,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(pillShape)
                            .background(selectedColor.copy(alpha = 0.18f)),
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { rowSize = it }
                .pointerInput(Unit) {
                    var totalDrag = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { totalDrag = 0f },
                        onDragCancel = { dragOffset = 0f },
                        onDragEnd = {
                            if (tabStepPx > 0f) {
                                val ratio = totalDrag / tabStepPx
                                val shift = when {
                                    ratio > 0.35f -> kotlin.math.max(1, ratio.roundToInt())
                                    ratio < -0.35f -> kotlin.math.min(-1, ratio.roundToInt())
                                    else -> 0
                                }
                                val newIndex = (currentSelectedIndex + shift).coerceIn(0, tabs.lastIndex)
                                if (newIndex != currentSelectedIndex) {
                                    onTabSelected(newIndex)
                                }
                            }
                            dragOffset = 0f
                        },
                        onHorizontalDrag = { _, delta ->
                            totalDrag += delta
                            val rawPx = when {
                                totalDrag > 0 && currentSelectedIndex == tabs.lastIndex ->
                                    totalDrag * 0.25f
                                totalDrag < 0 && currentSelectedIndex == 0 ->
                                    totalDrag * 0.25f
                                else -> totalDrag
                            }
                            dragOffset = rawPx

                            val approxTab =
                                (currentSelectedIndex + dragOffset / tabStepPx)
                                    .coerceIn(0f, tabs.lastIndex.toFloat())
                                    .roundToInt()
                            if (approxTab != lastHapticTab) {
                                haptics.play(Haptic.Tick)
                                lastHapticTab = approxTab
                            }
                        },
                    )
                },
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { index, tab ->
                BottomBarItem(
                    tab = tab,
                    selected = index == selectedIndex,
                    glassSpec = glassSpec,
                    useGlass = useGlass,
                    selectedTint = selectedColor,
                    unselectedTint = unselectedColor,
                    onClick = { onTabSelected(index) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BottomBarItem(
    tab: BottomTab,
    selected: Boolean,
    glassSpec: AnimationSpec<Float>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    useGlass: Boolean = false,
    selectedTint: Color? = null,
    unselectedTint: Color? = null,
) {
    // Magnified view inside the glass sphere
    val scale by animateFloatAsState(
        targetValue = if (selected) (if (useGlass) 1.12f else 1.08f) else 1f,
        animationSpec = glassSpec,
        label = "tabScale",
    )
    val haptics = rememberHaptics()
    val tint by animateColorAsState(
        targetValue = if (selected) {
            selectedTint ?: MaterialTheme.colorScheme.primary
        } else {
            unselectedTint ?: MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(200),
        label = "tabTint",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                if (!selected) haptics.play(Haptic.Select)
                onClick()
            }
            .padding(vertical = TAB_VERTICAL_PADDING),
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.label,
            tint = tint,
            modifier = Modifier
                .size(25.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
        )
        Spacer(Modifier.height(TAB_ICON_LABEL_GAP))
        Text(
            text = tab.label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}


/**
 * 3D Spherical Liquid Glass Bubble / Lens Indicator matching PLYR's liquid glass design.
 * Features:
 * - Real chromatic aberration rainbow dispersion rim (red -> orange -> yellow -> green -> cyan -> blue -> violet)
 * - 3D spherical dome convex shading with depth shadow
 * - Top specular curved gloss reflection
 * - Dynamic primary/custom accent illumination
 */
@Composable
internal fun LiquidGlass3DBubble(
    modifier: Modifier = Modifier,
    chromaticAberration: Boolean = true,
    depthEffect: Boolean = true,
    accentColor: Color = MaterialTheme.colorScheme.primary,
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val cx = width / 2f
        val cy = height / 2f
        val r = minOf(width, height) / 2f
        val center = Offset(cx, cy)

        // 1. Soft Ambient Elevation Drop Shadow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Black.copy(alpha = 0.40f),
                    Color.Black.copy(alpha = 0.15f),
                    Color.Transparent,
                ),
                center = Offset(cx, cy + 2.5.dp.toPx()),
                radius = r * 1.15f,
            ),
            radius = r * 1.15f,
            center = Offset(cx, cy + 2.5.dp.toPx()),
        )

        // 2. Crystal Translucent 3D Convex Glass Dome (Spherical volume)
        // Authentic Apple liquid glass: high translucency with soft specular gradient (not opaque black)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.18f),
                    Color.White.copy(alpha = 0.05f),
                    Color(0xFF141622).copy(alpha = 0.35f),
                ),
                center = Offset(cx - r * 0.25f, cy - r * 0.30f),
                radius = r,
            ),
            radius = r,
            center = center,
        )

        // 3. Dynamic Accent Core Glow (internal liquid luminescence)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    accentColor.copy(alpha = 0.30f),
                    accentColor.copy(alpha = 0.08f),
                    Color.Transparent,
                ),
                center = center,
                radius = r * 0.85f,
            ),
            radius = r * 0.85f,
            center = center,
        )

        // 4. Glancing-angle Chromatic Dispersion Arc on top-left curve (matching PLYR / Apple)
        if (chromaticAberration) {
            val rainbowColors = listOf(
                Color(0xFFFF2A55),
                Color(0xFFFF7A00),
                Color(0xFFFFD600),
                Color(0xFF00E676),
                Color(0xFF00E5FF),
                Color(0xFF2979FF),
                Color(0xFFD500F9),
                Color(0xFFFF2A55),
            )
            val arcRect = Rect(center = center, radius = r - 1.2.dp.toPx())
            drawArc(
                brush = Brush.sweepGradient(colors = rainbowColors, center = center),
                startAngle = 140f,
                sweepAngle = 95f,
                useCenter = false,
                topLeft = arcRect.topLeft,
                size = arcRect.size,
                style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round),
                alpha = 0.85f,
            )
        }

        // 5. Brilliant Specular White Crescent Highlight (Overhead light reflecting on convex glass dome)
        val highlightRect = Rect(center = center, radius = r - 1.0.dp.toPx())
        drawArc(
            color = Color.White.copy(alpha = 0.95f),
            startAngle = 145f,
            sweepAngle = 85f,
            useCenter = false,
            topLeft = highlightRect.topLeft,
            size = highlightRect.size,
            style = Stroke(width = 2.0.dp.toPx(), cap = StrokeCap.Round),
        )
        // Soft inner glow of specular highlight
        drawArc(
            color = Color.White.copy(alpha = 0.40f),
            startAngle = 150f,
            sweepAngle = 75f,
            useCenter = false,
            topLeft = highlightRect.topLeft,
            size = highlightRect.size,
            style = Stroke(width = 4.0.dp.toPx(), cap = StrokeCap.Round),
        )

        // 6. Delicate Outer Rim Stroke (0.8dp)
        drawCircle(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.65f),
                    Color.White.copy(alpha = 0.10f),
                    Color.White.copy(alpha = 0.25f),
                ),
                startY = cy - r,
                endY = cy + r,
            ),
            radius = r - 0.5.dp.toPx(),
            center = center,
            style = Stroke(width = 0.8.dp.toPx()),
        )
    }
}
