package com.music.dhvani.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Person
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import coil3.compose.AsyncImage
import com.music.dhvani.R
import com.music.dhvani.data.model.Account
import com.music.dhvani.data.settings.AppSettings

/**
 * The bar's own height, above whatever inset it is sitting under.
 *
 * The single source of truth for it: the bar lays itself out to this, and
 * everything that has to clear the bar — page content padding, [TopFadeBlur]'s
 * ramp, fixed headers that sit directly beneath it — measures from here rather
 * than from a copy of the number.
 */
val TopBarContentHeight = 52.dp

/**
 * The breathing room between the bar's bottom edge and the first thing under
 * it, so content rests below the glass instead of against it.
 */
val TopBarContentGap = 12.dp

/**
 * How far down the window the bar actually ends: the status bar inset it is
 * pinned under, plus its own height.
 *
 * This has to be read at composition rather than baked in as a constant — the
 * inset is a property of the device and of the window, not of the app. A phone
 * with a cutout, one without, and a freeform window with no status bar at all
 * are all different numbers, and a fixed guess is wrong on all but one of them:
 * too tight and content is clipped under the bar, too loose and every page
 * opens on a band of empty space.
 */
@Composable
fun topBarHeight(): Dp {
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val cutoutTop = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
    return maxOf(statusBarTop, cutoutTop) + TopBarContentHeight
}

/**
 * Where page content should start: clear of the bar, plus [TopBarContentGap].
 */
@Composable
fun topBarContentPadding(): Dp = topBarHeight() + TopBarContentGap

@Immutable
data class HomeGreetingState(
    val dateText: String,
    val greetingText: String,
)

internal fun computeHomeGreeting(
    locale: Locale = Locale.getDefault(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): HomeGreetingState {
    val now = ZonedDateTime.now(zoneId)
    val dateText = try {
        val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", locale)
        now.format(formatter).uppercase(locale)
    } catch (_: Exception) {
        val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH)
        now.format(formatter).uppercase(Locale.ENGLISH)
    }

    val hour = now.hour
    val greetingText = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }

    return HomeGreetingState(dateText = dateText, greetingText = greetingText)
}

@Composable
fun rememberHomeGreeting(): HomeGreetingState {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var state by remember { mutableStateOf(computeHomeGreeting()) }

    // Re-check when app resumes
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val updated = computeHomeGreeting()
                if (updated != state) {
                    state = updated
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Update on system date, time, timezone, or locale settings change
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val updated = computeHomeGreeting()
                if (updated != state) {
                    state = updated
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_LOCALE_CHANGED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(receiver, filter)
        }
        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    // Efficiently sleep until the next hour or midnight transition (no periodic polling)
    LaunchedEffect(Unit) {
        while (true) {
            val now = ZonedDateTime.now(ZoneId.systemDefault())
            val nextHour = now.truncatedTo(ChronoUnit.HOURS).plusHours(1)
            val delayMs = Duration.between(now, nextHour).toMillis().coerceAtLeast(1000L)
            delay(delayMs)
            val updated = computeHomeGreeting()
            if (updated != state) {
                state = updated
            }
        }
    }

    return state
}

/**
 * Native Home screen greeting header showing the localized uppercase date label
 * and time-based greeting ("Good morning", "Good afternoon", or "Good evening").
 */
@Composable
fun HomeGreetingHeader(
    modifier: Modifier = Modifier,
) {
    val greeting = rememberHomeGreeting()

    Column(
        modifier = modifier.padding(end = 136.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = greeting.dateText,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp,
                lineHeight = 14.sp,
            ),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = greeting.greetingText,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.4).sp,
                lineHeight = 26.sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The top bar's content — title, back affordance, actions — over no backdrop
 * of its own.
 *
 * The glass behind it is [TopFadeBlur]'s, drawn underneath: a blur that starts
 * full at the status bar and ramps to nothing below, so the bar has no bottom
 * edge to draw a line across the page with. A uniform pane would put that line
 * back, which is the one thing every surface here is built to avoid.
 *
 * The exception is Reduce dynamic blur, where there is no fade to sit on and
 * the bar fills itself solid instead — title over raw scrolling content is
 * unreadable, so something has to carry it.
 *
 * Apple Music behaviour: the big in-list header owns the title at rest;
 * once the list scrolls, the small centered title fades in.
 */
@Composable
fun FrostedTopBar(
    title: String,
    scrolled: Boolean,
    modifier: Modifier = Modifier,
    isHome: Boolean = false,
    onBack: (() -> Unit)? = null,
    onLanguageClick: (() -> Unit)? = null,
    refreshing: Boolean = false,
    // A lambda, not a value: the drag changes every frame, and reading it in
    // the caller would recompose the whole app on each one.
    pullFraction: () -> Float = { 0f },
    actions: @Composable () -> Unit = {},
) {
    val titleAlpha by animateFloatAsState(
        targetValue = if (scrolled) 1f else 0f,
        animationSpec = tween(260, easing = FastOutSlowInEasing),
        label = "topBarTitleAlpha",
    )

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(TopBarContentHeight),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.2).sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    // Reserve room for the back button and the actions so a
                    // long title truncates instead of running under them.
                    .padding(horizontal = 96.dp)
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = if (onBack != null || isHome) titleAlpha else 0f
                        translationY = 6.dp.toPx() * (1f - titleAlpha)
                    },
            )
            // On a pushed page the back affordance is always visible, since
            // there is no large in-list header to fall back on.
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            } else if (isHome) {
                HomeGreetingHeader(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 16.dp)
                        .graphicsLayer {
                            val a = if (title.isEmpty()) 1f else (1f - titleAlpha).coerceIn(0f, 1f)
                            alpha = a
                            translationY = -6.dp.toPx() * (1f - a)
                        },
                )
            } else {
                Text(
                    text = title.ifEmpty { "Dhvani" },
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 16.dp),
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                actions()
            }
        }
        // The loader line shares the bar's bottom edge; the box
        // only grows to the line's height while a refresh is actually showing.
        Box(Modifier.fillMaxWidth()) {
            RefreshLine(
                refreshing = refreshing,
                pullFraction = pullFraction,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

/**
 * The account affordance at the right end of the bar.
 *
 * It is the signed-in Google account's own photo — the same one YouTube Music
 * shows there — and tapping it opens Settings, where the account lives. Signed
 * out, or before the account menu has come back, it falls back to a person
 * glyph on a filled circle so the tap target never disappears.
 *
 * The hairline ring is what keeps a photo with light edges from dissolving into
 * the bar's glass; it is the same one thumbnails elsewhere carry.
 */
@Composable
fun TopBarAccountButton(
    account: Account?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Wrapped in an IconButton so it keeps the 48dp target, the ripple and the
    // spacing every other action in this bar has.
    IconButton(onClick = onClick, modifier = modifier) {
        val photo = account?.thumbnailUrl
        if (photo != null) {
            AsyncImage(
                model = photo,
                contentDescription = stringResource(R.string.settings),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(AVATAR_SIZE)
                    .clip(CircleShape)
                    .thumbnailBorder(CircleShape),
            )
        } else {
            UserAvatar(
                size = AVATAR_SIZE,
                borderWidth = 1.dp,
                borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )
        }
    }
}

/**
 * The refresh indicator: a line along the bottom of the bar, directly under the
 * status bar. It tracks the drag on the way down — filling left to right as the
 * pull approaches the threshold — then sweeps indefinitely once the refresh is
 * away, so the two phases read as one continuous gesture.
 */
@Composable
private fun RefreshLine(refreshing: Boolean, pullFraction: () -> Float, modifier: Modifier = Modifier) {
    val fraction = pullFraction()
    val pulling = fraction > 0.01f
    AnimatedVisibility(
        visible = refreshing || pulling,
        enter = fadeIn(tween(120)),
        exit = fadeOut(tween(220)),
        modifier = modifier,
    ) {
        val lineModifier = Modifier
            .fillMaxWidth()
            .height(LINE_HEIGHT)
        if (refreshing) {
            LinearProgressIndicator(
                modifier = lineModifier,
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Transparent,
                strokeCap = StrokeCap.Butt,
                gapSize = 0.dp,
            )
        } else {
            LinearProgressIndicator(
                progress = { fraction.coerceIn(0f, 1f) },
                modifier = lineModifier,
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Transparent,
                strokeCap = StrokeCap.Butt,
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
        }
    }
}

private val LINE_HEIGHT = 2.5.dp

/**
 * The account photo's diameter.
 *
 * Smaller than an icon's 24dp box: a filled circle carries more weight than a
 * glyph does, and at 24 it sat heavier in the bar than the wordmark opposite it.
 */
private val AVATAR_SIZE = 28.dp
