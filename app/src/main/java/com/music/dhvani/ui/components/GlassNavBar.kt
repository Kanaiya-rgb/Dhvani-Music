/*

 * The bar's structure and its inline/expanded behaviour are

 * EchoMusicApp/Echo-Music's AppFloatingNavBar + FloatingMiniPlayer (GPL-3.0),

 * over the FloatingTabBar vendored in [com.music.dhvani.ui.components.floatingtabbar].

 * Dhvani's own tabs, song model, transport and haptics are wired through it.

 */

@file:OptIn(ExperimentalSharedTransitionApi::class)



package com.music.dhvani.ui.components



import androidx.compose.animation.AnimatedVisibilityScope

import androidx.compose.animation.ExperimentalSharedTransitionApi

import androidx.compose.animation.SharedTransitionScope

import androidx.compose.animation.core.Spring

import androidx.compose.animation.core.animateFloatAsState

import androidx.compose.animation.core.spring

import androidx.compose.foundation.background

import androidx.compose.foundation.border

import androidx.compose.ui.graphics.Brush

import androidx.compose.foundation.clickable

import androidx.compose.foundation.gestures.detectHorizontalDragGestures

import androidx.compose.foundation.interaction.MutableInteractionSource

import androidx.compose.foundation.interaction.collectIsPressedAsState
import com.music.dhvani.data.ExplicitDetector

import androidx.compose.foundation.layout.Box

import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.PaddingValues

import androidx.compose.foundation.layout.Row

import androidx.compose.foundation.layout.Spacer

import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.foundation.layout.navigationBarsPadding

import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.layout.size

import androidx.compose.foundation.layout.width

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.rounded.Pause

import androidx.compose.material.icons.rounded.PlayArrow

import androidx.compose.material.icons.rounded.SkipNext

import androidx.compose.material3.CircularProgressIndicator

import androidx.compose.material3.Icon

import androidx.compose.material3.IconButton

import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.Text

import androidx.compose.runtime.Composable

import androidx.compose.runtime.getValue

import androidx.compose.runtime.remember

import androidx.compose.runtime.rememberUpdatedState

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.draw.clip

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.ui.graphics.luminance

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.music.dhvani.data.settings.AppSettings

import com.music.dhvani.ui.theme.DhvaniPrimary

import androidx.compose.ui.input.pointer.pointerInput

import androidx.compose.ui.platform.LocalDensity

import androidx.compose.ui.res.stringResource

import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.ui.unit.dp

import coil3.compose.AsyncImage

import com.music.dhvani.R

import com.music.dhvani.data.model.ROW_ART_PX

import com.music.dhvani.data.model.Song

import com.music.dhvani.data.model.artworkAt

import com.music.dhvani.ui.components.floatingtabbar.FloatingTabBar

import com.music.dhvani.ui.components.floatingtabbar.FloatingTabBarDefaults

import com.music.dhvani.ui.components.floatingtabbar.FloatingTabBarScrollConnection

import com.music.dhvani.ui.haptics.Haptic

import com.music.dhvani.ui.haptics.rememberHaptics

import androidx.compose.animation.core.LinearEasing

import androidx.compose.animation.core.RepeatMode

import androidx.compose.animation.core.animateFloat

import androidx.compose.animation.core.infiniteRepeatable

import androidx.compose.animation.core.rememberInfiniteTransition

import androidx.compose.animation.core.tween

import androidx.compose.foundation.layout.Arrangement

import androidx.compose.foundation.layout.fillMaxHeight

import androidx.compose.foundation.layout.height

import androidx.compose.foundation.shape.CircleShape



/**

 * The liquid glass navigation bar: the iOS 26 shape where the now playing

 * controls and the tabs are one component rather than two stacked bars.

 *

 * Expanded, it is a full width now playing pill sitting over the tab pill and a

 * separate circular Search tab. Scrolling down collapses it inline — the tabs

 * fold down to just the selected one, the now playing controls narrow into the

 * gap between it and Search, and the whole thing becomes a single row the width

 * of the screen. Scrolling back up expands it. The fold is driven by

 * [scrollConnection], which the page's scroll has to be dispatched into for any

 * of this to move; see MainActivity's `nestedScroll`.

 *

 * Every surface here samples the app backdrop through [Modifier.liquidGlass], so

 * this is only ever used where that is supported and switched on — off either,

 * MainActivity draws [MiniPlayer] and [FloatingBottomBar] instead.

 *

 * [song] null means nothing is playing, and the accessory is simply absent: the

 * bar is then the tab pill and Search alone, and the collapse still works.

 */

@Composable

fun GlassNavBar(

    tabs: List<BottomTab>,

    selectedIndex: Int,

    onTabSelected: (Int) -> Unit,

    scrollConnection: FloatingTabBarScrollConnection,

    song: Song?,

    isPlaying: Boolean,

    isLoading: Boolean,

    onPlayPause: () -> Unit,

    onNext: () -> Unit,

    onPrevious: () -> Unit,

    onExpand: () -> Unit,

    modifier: Modifier = Modifier,

) {

    // Held for the same reason [tabs] is. The glass factory below closes over

    // this shape, and a fresh RoundedCornerShape each pass means a fresh factory

    // lambda, which the tab bar sees as a changed argument and recomposes on.

    val pillShape = remember { RoundedCornerShape(percent = 50) }

    val contentColor = glassContentColor()

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

    val haptics = rememberHaptics()



    // The tab content lambdas below are captured once per contentKey and held

    // until it changes, so a click handler that reached back to this call's

    // `onTabSelected` would go stale the moment anything it closes over moved.

    // Held through a state that is always current instead, which also keeps the

    // handler out of the key.

    val currentOnTabSelected by rememberUpdatedState(onTabSelected)



    // Search is the odd one out in the iOS 26 layout: a circle of its own beside

    // the pill rather than a quarter of it. It is the last tab in the

    // order, which is where the shape wants it anyway.

    val standaloneIndex = tabs.lastIndex



    val glassBorderBrush = remember {

        Brush.linearGradient(

            colors = listOf(

                Color.White.copy(alpha = 0.60f),

                Color(0xFF38BDF8).copy(alpha = 0.40f), // cyan prismatic edge

                Color.White.copy(alpha = 0.15f),

                Color(0xFFEC4899).copy(alpha = 0.35f), // magenta refraction

                Color.White.copy(alpha = 0.55f),

            )

        )

    }

    // A factory, not a value — see the note in FloatingTabBar's header. Each of

    // the three surfaces gets its own glass modifier and so its own shape cache.

    val glassSurface: @Composable () -> Modifier = { 

        Modifier

            .liquidGlass(shape = pillShape)

            .border(0.75.dp, glassBorderBrush, pillShape)

    }



    FloatingTabBar(

        selectedTabKey = selectedIndex,

        scrollConnection = scrollConnection,

        modifier = modifier

            .navigationBarsPadding()

            .padding(horizontal = PAGE_GUTTER)

            .padding(bottom = 2.dp)

            .fillMaxWidth(),

        tabBarContentModifier = glassSurface,

        inlineAccessory = song?.let { current ->

            { accessoryModifier, _ ->

                GlassNowPlaying(

                    song = current,

                    isInline = true,

                    isPlaying = isPlaying,

                    isLoading = isLoading,

                    contentColor = contentColor,

                    onPlayPause = onPlayPause,

                    onNext = onNext,

                    onPrevious = onPrevious,

                    onExpand = onExpand,

                    modifier = accessoryModifier.then(glassSurface()),

                )

            }

        },

        expandedAccessory = song?.let { current ->

            { accessoryModifier, _ ->

                GlassNowPlaying(

                    song = current,

                    isInline = false,

                    isPlaying = isPlaying,

                    isLoading = isLoading,

                    contentColor = contentColor,

                    onPlayPause = onPlayPause,

                    onNext = onNext,

                    onPrevious = onPrevious,

                    onExpand = onExpand,

                    modifier = accessoryModifier.fillMaxWidth().then(glassSurface()),

                )

            }

        },

        // Transparent: the glass surface underneath is the background, and a

        // colour over it would be the thing you saw instead of the backdrop.

        colors = FloatingTabBarDefaults.colors(

            backgroundColor = Color.Transparent,

            accessoryBackgroundColor = Color.Transparent,

            indicatorColor = glassIndicatorColor(),

        ),

        // Flat, because the glass is not. Every surface here already draws its

        // own [Shadow.Default] as part of the backdrop pass, and the library's

        // Modifier.shadow on top of that is a second offscreen layer and a

        // second shadow render for each of them — three at rest, six mid-fold,

        // paying twice for a shadow you can only see once.

        elevations = FloatingTabBarDefaults.elevations(

            inlineElevation = 0.dp,

            expandedElevation = 0.dp,

        ),

        // Expanded, this is meant to be the plain [FloatingBottomBar] with a

        // different material — same outer width, same pill inset, same tab

        // padding, same 25dp glyph — so the two bars measure identically and the

        // toggle changes the surface rather than the layout. The horizontal tab

        // padding is gone with it: the tabs divide the pill by weight now, the

        // way the plain bar's do, so a per-tab horizontal padding would only

        // inset the ripple.

        sizes = FloatingTabBarDefaults.sizes(

            tabBarContentPadding = PaddingValues(PILL_INSET),

            tabExpandedContentPadding = PaddingValues(vertical = TAB_VERTICAL_PADDING),

        ),

        // Held too: this is declared `Any?`, so a fresh list every pass is a

        // changed argument by identity and defeats skipping on its own.

        contentKey = remember(selectedIndex, tabs, contentColor, selectedColor, unselectedColor) {

            listOf(selectedIndex, tabs, contentColor, selectedColor, unselectedColor)

        },

    ) {

        tabs.forEachIndexed { index, tab ->

            val isSelected = index == selectedIndex

            val tint = if (isSelected) selectedColor else unselectedColor

            val onClick = {

                if (!isSelected) haptics.play(Haptic.Select)

                currentOnTabSelected(index)

            }

            if (index == standaloneIndex) {

                standaloneTab(

                    key = index,

                    icon = {

                        Icon(

                            imageVector = tab.icon,

                            contentDescription = tab.label,

                            tint = tint,

                            modifier = Modifier.size(25.dp),

                        )

                    },

                    onClick = onClick,

                )

            } else {

                tab(

                    key = index,

                    icon = {

                        Icon(

                            imageVector = tab.icon,

                            contentDescription = tab.label,

                            tint = tint,

                            modifier = Modifier.size(25.dp),

                        )

                    },

                    title = {

                        Text(

                            text = tab.label,

                            style = MaterialTheme.typography.labelSmall,

                            color = tint,

                            maxLines = 1,

                            overflow = TextOverflow.Ellipsis,

                            // The library's Tab stacks the glyph and the label

                            // with nothing between them; the plain bar spaces

                            // them, and this is where that gap goes.

                            modifier = Modifier.padding(top = TAB_ICON_LABEL_GAP),

                        )

                    },

                    onClick = onClick,

                )

            }

        }

    }

}



/**

 * The now playing controls docked into [GlassNavBar] as its accessory.

 *

 * Two densities of the same row rather than two components, so the shared

 * element carrying it between the bar's states has one thing to interpolate:

 * [isInline] narrows the artwork, drops the artist line and drops the skip

 * button, which is what makes it fit the collapsed row's height.

 *

 * The content is [MiniPlayer]'s — same artwork, same transport, same haptics —

 * and not Echo's, which reaches into a player connection this app doesn't have.

 * The press response is Echo's, and belongs to the glass rather than the row:

 * a surface you can push on is the whole point of the material.

 */

@Composable

private fun GlassNowPlaying(

    song: Song,

    isInline: Boolean,

    isPlaying: Boolean,

    isLoading: Boolean,

    contentColor: Color,

    onPlayPause: () -> Unit,

    onNext: () -> Unit,

    onPrevious: () -> Unit,

    onExpand: () -> Unit,

    modifier: Modifier = Modifier,

) {

    val haptics = rememberHaptics()

    val pressSource = remember { MutableInteractionSource() }

    val isPressed by pressSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(

        targetValue = if (isPressed) 1.04f else 1f,

        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),

        label = "accessoryPressScale",

    )



    val artSize = if (isInline) 32.dp else 40.dp

    val glyphSlot = if (isInline) 32.dp else 40.dp

    val glyphSize = if (isInline) 24.dp else 32.dp



    Box(

        // Inline, the accessory is stretched to the row's height by the tab bar

        // (`fillMaxHeight` on a row measured at IntrinsicSize.Max), and this row

        // is shorter than that — the tab pill beside it, with a 25dp glyph in

        // 10dp of padding, is the tallest thing in the row and sets the height.

        // A Box defaults to TopStart, so the artwork and the transport sat a

        // couple of dp above the pill's centre line. Expanded the Box wraps its

        // content, so centring is a no-op there.

        contentAlignment = Alignment.Center,

        modifier = Modifier

            .graphicsLayer {

                scaleX = pressScale

                scaleY = pressScale

            }

            .then(modifier),

    ) {

        Row(

            verticalAlignment = Alignment.CenterVertically,

            modifier = Modifier

                .fillMaxWidth()

                .clickable(

                    interactionSource = pressSource,

                    indication = null,

                    onClick = onExpand,

                )

                .miniPlayerTrackSwipe(

                    onNext = {

                        haptics.play(Haptic.SkipNext)

                        onNext()

                    },

                    onPrevious = {

                        haptics.play(Haptic.SkipPrevious)

                        onPrevious()

                    },

                )

                .padding(

                    horizontal = if (isInline) 8.dp else 12.dp,

                    vertical = if (isInline) 4.dp else 8.dp,

                ),

        ) {

            AsyncImage(

                model = song.artworkAt(ROW_ART_PX),

                contentDescription = null,

                modifier = Modifier

                    .size(artSize)

                    .clip(RoundedCornerShape(if (isInline) 6.dp else 8.dp))

                    .background(MaterialTheme.colorScheme.surfaceVariant),

            )

            Spacer(Modifier.width(if (isInline) 8.dp else 10.dp))

            val isExplicit = remember(song.isExplicit, song.title, song.artist, song.albumName) {
                ExplicitDetector.isExplicit(song.isExplicit, song.title, song.artist, song.albumName)
            }
            if (isInline) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    if (isExplicit) {
                        ExplicitBadge()
                    }
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
            } else {
                Column(Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (isExplicit) {
                            ExplicitBadge()
                        }
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = contentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(if (isInline) 6.dp else 8.dp))
            MiniAudioWaveform(

                isPlaying = isPlaying,

                color = contentColor,

            )

            Spacer(Modifier.width(if (isInline) 4.dp else 4.dp))

            if (isLoading) {

                Box(Modifier.size(glyphSlot), contentAlignment = Alignment.Center) {

                    CircularProgressIndicator(

                        color = contentColor,

                        strokeWidth = 2.dp,

                        modifier = Modifier.size(if (isInline) 18.dp else 22.dp),

                    )

                }

            } else {

                IconButton(

                    onClick = {

                        haptics.play(if (isPlaying) Haptic.Pause else Haptic.Resume)

                        onPlayPause()

                    },

                    modifier = Modifier.size(glyphSlot),

                ) {

                    Icon(

                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,

                        contentDescription = stringResource(if (isPlaying) R.string.pause else R.string.play),

                        tint = contentColor,

                        modifier = Modifier.size(glyphSize),

                    )

                }

            }

            // Dropped inline: the collapsed row is sharing its width with the

            // tab pill and the Search circle, and the title is what has to

            // survive that, not a second transport button.

            if (!isInline) {

                Spacer(Modifier.width(8.dp))

                IconButton(

                    onClick = {

                        haptics.play(Haptic.SkipNext)

                        onNext()

                    },

                    modifier = Modifier.size(glyphSlot),

                ) {

                    Icon(

                        imageVector = Icons.Rounded.SkipNext,

                        contentDescription = stringResource(R.string.widget_next),

                        tint = contentColor,

                        modifier = Modifier.size(glyphSize),

                    )

                }

            }

        }

    }

}



@Composable

internal fun Modifier.miniPlayerTrackSwipe(

    onNext: () -> Unit,

    onPrevious: () -> Unit,

): Modifier {

    val currentOnNext by rememberUpdatedState(onNext)

    val currentOnPrevious by rememberUpdatedState(onPrevious)

    val threshold = with(LocalDensity.current) { 48.dp.toPx() }

    return pointerInput(Unit) {

        var totalDrag = 0f

        detectHorizontalDragGestures(

            onDragStart = { totalDrag = 0f },

            onDragCancel = { totalDrag = 0f },

            onDragEnd = {

                when {

                    totalDrag <= -threshold -> currentOnNext()

                    totalDrag >= threshold -> currentOnPrevious()

                }

                totalDrag = 0f

            },

            onHorizontalDrag = { change, amount ->

                change.consume()

                totalDrag += amount

            },

        )

    }

}



/**

 * 4-bar animated audio waveform equalizer, matching iOS/Apple Music style.

 */

@Composable

fun MiniAudioWaveform(

    isPlaying: Boolean,

    modifier: Modifier = Modifier,

    color: Color = Color.White,

) {

    val infiniteTransition = rememberInfiniteTransition(label = "miniWaveform")

    val barHeights = listOf(

        if (isPlaying) {

            val h by infiniteTransition.animateFloat(

                initialValue = 0.25f,

                targetValue = 0.85f,

                animationSpec = infiniteRepeatable(

                    animation = tween(450, easing = LinearEasing),

                    repeatMode = RepeatMode.Reverse,

                ),

                label = "bar_0",

            )

            h

        } else 0.25f,

        if (isPlaying) {

            val h by infiniteTransition.animateFloat(

                initialValue = 0.35f,

                targetValue = 0.70f,

                animationSpec = infiniteRepeatable(

                    animation = tween(320, easing = LinearEasing),

                    repeatMode = RepeatMode.Reverse,

                ),

                label = "bar_1",

            )

            h

        } else 0.35f,

        if (isPlaying) {

            val h by infiniteTransition.animateFloat(

                initialValue = 0.20f,

                targetValue = 1.00f,

                animationSpec = infiniteRepeatable(

                    animation = tween(480, easing = LinearEasing),

                    repeatMode = RepeatMode.Reverse,

                ),

                label = "bar_2",

            )

            h

        } else 0.20f,

        if (isPlaying) {

            val h by infiniteTransition.animateFloat(

                initialValue = 0.30f,

                targetValue = 0.75f,

                animationSpec = infiniteRepeatable(

                    animation = tween(360, easing = LinearEasing),

                    repeatMode = RepeatMode.Reverse,

                ),

                label = "bar_3",

            )

            h

        } else 0.30f,

    )



    Row(

        modifier = modifier

            .height(16.dp)

            .padding(horizontal = 2.dp),

        horizontalArrangement = Arrangement.spacedBy(2.dp),

        verticalAlignment = Alignment.CenterVertically,

    ) {

        barHeights.forEach { fraction ->

            Box(

                modifier = Modifier

                    .width(2.5.dp)

                    .fillMaxHeight(fraction)

                    .clip(CircleShape)

                    .background(color.copy(alpha = if (isPlaying) 0.90f else 0.40f)),

            )

        }

    }

}

