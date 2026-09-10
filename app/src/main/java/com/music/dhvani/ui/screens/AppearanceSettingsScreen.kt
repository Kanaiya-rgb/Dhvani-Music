package com.music.dhvani.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BlurOff
import androidx.compose.material.icons.rounded.Brightness4
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.HideImage
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.MotionPhotosOff
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewStream
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.dhvani.R
import com.music.dhvani.data.settings.AppSettings
import com.music.dhvani.data.settings.DensityScale
import com.music.dhvani.data.settings.GridItemSize
import com.music.dhvani.data.settings.LyricsAnimationStyle
import com.music.dhvani.data.settings.LyricsPosition
import com.music.dhvani.data.settings.MiniPlayerBackgroundStyle
import com.music.dhvani.data.settings.PlayerBackgroundStyle
import com.music.dhvani.data.settings.PlayerButtonsStyle
import com.music.dhvani.data.settings.SliderStyle
import com.music.dhvani.data.settings.ThemeMode
import com.music.dhvani.ui.components.PlayerSliderTrack
import com.music.dhvani.ui.components.SliderStyleDialog
import com.music.dhvani.ui.components.SquigglySlider
import com.music.dhvani.ui.components.WavySlider
import com.music.dhvani.ui.player.fullBleedArtworkAvailable
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    windowWidth: Dp,
    onBack: () -> Unit,
    onSpotifyCanvasAuth: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isSystemDark = isSystemInDarkTheme()

    val theme by AppSettings.themeMode.collectAsStateWithLifecycle()
    val dynamicTheme by AppSettings.dynamicTheme.collectAsStateWithLifecycle()
    val enableHighRefreshRate by AppSettings.enableHighRefreshRate.collectAsStateWithLifecycle()
    val densityScale by AppSettings.densityScale.collectAsStateWithLifecycle()

    val sliderStyle by AppSettings.sliderStyle.collectAsStateWithLifecycle()
    val squigglySlider by AppSettings.squigglySlider.collectAsStateWithLifecycle()
    val playerBackgroundStyle by AppSettings.playerBackgroundStyle.collectAsStateWithLifecycle()
    val miniPlayerBackgroundStyle by AppSettings.miniPlayerBackgroundStyle.collectAsStateWithLifecycle()
    val playerButtonsStyle by AppSettings.playerButtonsStyle.collectAsStateWithLifecycle()
    val hidePlayerThumbnail by AppSettings.hidePlayerThumbnail.collectAsStateWithLifecycle()
    val cropAlbumArt by AppSettings.cropAlbumArt.collectAsStateWithLifecycle()
    val hideStatusBarOnFullscreen by AppSettings.hideStatusBarOnFullscreen.collectAsStateWithLifecycle()
    val fullBleedArtwork by AppSettings.fullBleedArtwork.collectAsStateWithLifecycle()
    val animatedCanvas by AppSettings.animatedCanvas.collectAsStateWithLifecycle()
    val canvasOverCellular by AppSettings.canvasOverCellular.collectAsStateWithLifecycle()
    val reduceAnimation by AppSettings.reduceAnimation.collectAsStateWithLifecycle()
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()

    val swipeThumbnail by AppSettings.swipeThumbnail.collectAsStateWithLifecycle()
    val swipeSensitivity by AppSettings.swipeSensitivity.collectAsStateWithLifecycle()

    val syncedLyrics by AppSettings.syncedLyrics.collectAsStateWithLifecycle()
    val lyricsPosition by AppSettings.lyricsPosition.collectAsStateWithLifecycle()
    val lyricsAnimationStyle by AppSettings.lyricsAnimationStyle.collectAsStateWithLifecycle()
    val lyricsGlowEffect by AppSettings.lyricsGlowEffect.collectAsStateWithLifecycle()
    val lyricsTextSize by AppSettings.lyricsTextSize.collectAsStateWithLifecycle()
    val lyricsLineSpacing by AppSettings.lyricsLineSpacing.collectAsStateWithLifecycle()
    val lyricsClickSeek by AppSettings.lyricsClickSeek.collectAsStateWithLifecycle()
    val lyricsAutoScroll by AppSettings.lyricsAutoScroll.collectAsStateWithLifecycle()
    val respectAgentPositioning by AppSettings.respectAgentPositioning.collectAsStateWithLifecycle()

    val defaultOpenTab by AppSettings.defaultOpenTab.collectAsStateWithLifecycle()
    val gridItemSize by AppSettings.gridItemSize.collectAsStateWithLifecycle()
    val slimNavBar by AppSettings.slimNavBar.collectAsStateWithLifecycle()
    val showRecognizeButton by AppSettings.showRecognizeButton.collectAsStateWithLifecycle()
    val showPlayRandomButton by AppSettings.showPlayRandomButton.collectAsStateWithLifecycle()

    val showLikedPlaylist by AppSettings.showLikedPlaylist.collectAsStateWithLifecycle()
    val showDownloadedPlaylist by AppSettings.showDownloadedPlaylist.collectAsStateWithLifecycle()
    val showTopPlaylist by AppSettings.showTopPlaylist.collectAsStateWithLifecycle()
    val showCachedPlaylist by AppSettings.showCachedPlaylist.collectAsStateWithLifecycle()
    val showUploadedPlaylist by AppSettings.showUploadedPlaylist.collectAsStateWithLifecycle()

    // Dialog control states
    var showDensityDialog by remember { mutableStateOf(false) }
    var showDensityRestartDialog by remember { mutableStateOf(false) }
    var showSliderStyleDialog by remember { mutableStateOf(false) }
    var showPlayerBgDialog by remember { mutableStateOf(false) }
    var showMiniPlayerBgDialog by remember { mutableStateOf(false) }
    var showPlayerButtonsDialog by remember { mutableStateOf(false) }
    var showSensitivityDialog by remember { mutableStateOf(false) }
    var showLyricsPositionDialog by remember { mutableStateOf(false) }
    var showLyricsAnimDialog by remember { mutableStateOf(false) }
    var showLyricsTextSizeDialog by remember { mutableStateOf(false) }
    var showLyricsLineSpacingDialog by remember { mutableStateOf(false) }
    var showDefaultTabDialog by remember { mutableStateOf(false) }
    var showGridSizeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
    ) {
        // Top app bar with back navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.cancel),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.appearance),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        // ── 1. Theme & Display ──────────────────────────────────────────
        SettingsGroup(header = stringResource(R.string.theme)) {
            SettingsRow(icon = Icons.Rounded.Brightness4, title = stringResource(R.string.theme))
            SegmentedControl(
                options = ThemeMode.entries.map { it.localizedLabel() },
                selectedIndex = ThemeMode.entries.indexOf(theme),
                onSelect = { AppSettings.setThemeMode(ThemeMode.entries[it]) },
                modifier = Modifier.padding(start = ROW_INSET, end = ROW_INSET, bottom = 14.dp),
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.Palette,
                title = stringResource(R.string.enable_dynamic_theme),
                subtitle = "Uses system wallpaper colors when supported",
                trailing = {
                    Switch(
                        checked = dynamicTheme,
                        onCheckedChange = AppSettings::setDynamicTheme,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedBorderColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                },
                onClick = { AppSettings.setDynamicTheme(!dynamicTheme) },
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.Speed,
                title = stringResource(R.string.enable_high_refresh_rate),
                subtitle = stringResource(R.string.enable_high_refresh_rate_desc),
                trailing = {
                    Switch(
                        checked = enableHighRefreshRate,
                        onCheckedChange = AppSettings::setEnableHighRefreshRate,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedBorderColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                },
                onClick = { AppSettings.setEnableHighRefreshRate(!enableHighRefreshRate) },
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.GridView,
                title = stringResource(R.string.display_density),
                subtitle = "Scale interface elements to fit more on screen",
                value = DensityScale.fromValue(densityScale).label,
                onClick = { showDensityDialog = true },
            )
        }

        // ── 2. Player Controls & Style ──────────────────────────────────
        SettingsGroup(header = stringResource(R.string.player)) {
            SettingsRow(
                icon = Icons.Rounded.Tune,
                title = stringResource(R.string.player_slider_style),
                subtitle = "Choose between Capsule, Material, Wavy, Squiggly, or Slim",
                value = when {
                    sliderStyle == SliderStyle.SQUIGGLY || (sliderStyle == SliderStyle.WAVY && squigglySlider) -> stringResource(R.string.squiggly)
                    sliderStyle == SliderStyle.WAVY -> stringResource(R.string.wavy)
                    sliderStyle == SliderStyle.SLIM -> stringResource(R.string.slim)
                    sliderStyle == SliderStyle.MATERIAL -> stringResource(R.string.material)
                    sliderStyle == SliderStyle.CAPSULE -> stringResource(R.string.capsule)
                    else -> stringResource(R.string.capsule)
                },
                onClick = { showSliderStyleDialog = true },
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.Layers,
                title = stringResource(R.string.player_background_style),
                subtitle = "Background style for the full-screen player",
                value = when (playerBackgroundStyle) {
                    PlayerBackgroundStyle.DEFAULT -> stringResource(R.string.follow_theme)
                    PlayerBackgroundStyle.GRADIENT -> stringResource(R.string.gradient)
                    PlayerBackgroundStyle.BLUR -> stringResource(R.string.player_background_blur)
                },
                onClick = { showPlayerBgDialog = true },
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.Palette,
                title = stringResource(R.string.player_buttons_style),
                subtitle = "Accent coloration on playback action buttons",
                value = when (playerButtonsStyle) {
                    PlayerButtonsStyle.DEFAULT -> stringResource(R.string.default_style)
                    PlayerButtonsStyle.PRIMARY -> stringResource(R.string.primary_color_style)
                    PlayerButtonsStyle.TERTIARY -> stringResource(R.string.tertiary_color_style)
                },
                onClick = { showPlayerButtonsDialog = true },
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.HideImage,
                title = stringResource(R.string.hide_player_thumbnail),
                subtitle = stringResource(R.string.hide_player_thumbnail_desc),
                trailing = {
                    Switch(
                        checked = hidePlayerThumbnail,
                        onCheckedChange = AppSettings::setHidePlayerThumbnail,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedBorderColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                },
                onClick = { AppSettings.setHidePlayerThumbnail(!hidePlayerThumbnail) },
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.Crop,
                title = stringResource(R.string.crop_album_art),
                subtitle = stringResource(R.string.crop_album_art_desc),
                trailing = {
                    Switch(
                        checked = cropAlbumArt,
                        onCheckedChange = AppSettings::setCropAlbumArt,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedBorderColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                },
                onClick = { AppSettings.setCropAlbumArt(!cropAlbumArt) },
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.FullscreenExit,
                title = stringResource(R.string.hide_status_bar_fullscreen),
                subtitle = stringResource(R.string.hide_status_bar_fullscreen_desc),
                trailing = {
                    Switch(
                        checked = hideStatusBarOnFullscreen,
                        onCheckedChange = AppSettings::setHideStatusBarOnFullscreen,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedBorderColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                },
                onClick = { AppSettings.setHideStatusBarOnFullscreen(!hideStatusBarOnFullscreen) },
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.Swipe,
                title = stringResource(R.string.enable_swipe_thumbnail),
                subtitle = "Swipe left/right on cover art to skip songs",
                trailing = {
                    Switch(
                        checked = swipeThumbnail,
                        onCheckedChange = AppSettings::setSwipeThumbnail,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedBorderColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                },
                onClick = { AppSettings.setSwipeThumbnail(!swipeThumbnail) },
            )
            if (swipeThumbnail) {
                RowDivider()
                SettingsRow(
                    icon = Icons.Rounded.Tune,
                    title = stringResource(R.string.swipe_sensitivity),
                    subtitle = "Touch threshold needed to trigger next/previous track",
                    value = "${(swipeSensitivity * 100).roundToInt()}%",
                    onClick = { showSensitivityDialog = true },
                )
            }
            if (fullBleedArtworkAvailable(windowWidth)) {
                RowDivider()
                SettingsRow(
                    icon = Icons.Rounded.Fullscreen,
                    title = stringResource(R.string.full_screen_cover_art),
                    subtitle = stringResource(R.string.full_screen_cover_art_subtitle),
                    trailing = {
                        Switch(
                            checked = fullBleedArtwork,
                            onCheckedChange = AppSettings::setFullBleedArtwork,
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                checkedBorderColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    },
                    onClick = { AppSettings.setFullBleedArtwork(!fullBleedArtwork) },
                )
            }
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.Animation,
                title = stringResource(R.string.animated_cover_art),
                subtitle = stringResource(R.string.animated_cover_art_subtitle),
                trailing = {
                    Switch(
                        checked = animatedCanvas,
                        onCheckedChange = AppSettings::setAnimatedCanvas,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedBorderColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                },
                onClick = { AppSettings.setAnimatedCanvas(!animatedCanvas) },
            )
            if (animatedCanvas) {
                SettingsSubRow(
                    title = stringResource(R.string.animated_cover_cellular),
                    checked = canvasOverCellular,
                    onCheckedChange = AppSettings::setCanvasOverCellular,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onSpotifyCanvasAuth)
                        .padding(start = ROW_INSET, end = ROW_INSET, top = 4.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Integrate Spotify Canvas",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Chevron()
                }
            }
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.MotionPhotosOff,
                title = stringResource(R.string.reduce_animation),
                subtitle = stringResource(R.string.reduce_animation_subtitle),
                trailing = {
                    Switch(
                        checked = reduceAnimation,
                        onCheckedChange = AppSettings::setReduceAnimation,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedBorderColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                },
                onClick = { AppSettings.setReduceAnimation(!reduceAnimation) },
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.BlurOff,
                title = stringResource(R.string.reduce_dynamic_blur),
                subtitle = stringResource(R.string.reduce_dynamic_blur_subtitle),
                trailing = {
                    Switch(
                        checked = reduceDynamicBlur,
                        onCheckedChange = AppSettings::setReduceDynamicBlur,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedBorderColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                },
                onClick = { AppSettings.setReduceDynamicBlur(!reduceDynamicBlur) },
            )
        }

        // ── 3. Mini Player ──────────────────────────────────────────────
        SettingsGroup(header = stringResource(R.string.mini_player)) {
            SettingsRow(
                icon = Icons.Rounded.ViewStream,
                title = stringResource(R.string.mini_player_background_style),
                subtitle = "Appearance of the floating playback strip",
                value = when (miniPlayerBackgroundStyle) {
                    MiniPlayerBackgroundStyle.DEFAULT -> stringResource(R.string.follow_theme)
                    MiniPlayerBackgroundStyle.TRANSPARENT -> stringResource(R.string.transparent)
                    MiniPlayerBackgroundStyle.BLUR -> stringResource(R.string.player_background_blur)
                    MiniPlayerBackgroundStyle.GRADIENT -> stringResource(R.string.gradient)
                    MiniPlayerBackgroundStyle.PURE_BLACK -> stringResource(R.string.pure_black)
                },
                onClick = { showMiniPlayerBgDialog = true },
            )
        }

        // ── 4. Lyrics ───────────────────────────────────────────────────
        SettingsGroup(header = stringResource(R.string.synced_lyrics)) {
            SettingsRow(
                icon = Icons.AutoMirrored.Rounded.Notes,
                title = stringResource(R.string.synced_lyrics),
                subtitle = stringResource(R.string.synced_lyrics_subtitle),
                trailing = {
                    Switch(
                        checked = syncedLyrics,
                        onCheckedChange = AppSettings::setSyncedLyrics,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedBorderColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                },
                onClick = { AppSettings.setSyncedLyrics(!syncedLyrics) },
            )
            if (syncedLyrics) {
                RowDivider()
                SettingsRow(
                    icon = Icons.Rounded.Tune,
                    title = stringResource(R.string.lyrics_text_position),
                    subtitle = "Horizontal alignment of sung lyric lines",
                    value = when (lyricsPosition) {
                        LyricsPosition.LEFT -> stringResource(R.string.left)
                        LyricsPosition.CENTER -> stringResource(R.string.center)
                        LyricsPosition.RIGHT -> stringResource(R.string.right)
                    },
                    onClick = { showLyricsPositionDialog = true },
                )
                RowDivider()
                SettingsRow(
                    icon = Icons.Rounded.AutoAwesome,
                    title = stringResource(R.string.lyrics_animation_style_title),
                    subtitle = "Transition effect as lines are highlighted",
                    value = when (lyricsAnimationStyle) {
                        LyricsAnimationStyle.NONE -> stringResource(R.string.lyrics_animation_none)
                        LyricsAnimationStyle.FADE -> stringResource(R.string.lyrics_animation_fade)
                        LyricsAnimationStyle.GLOW -> stringResource(R.string.lyrics_animation_glow)
                        LyricsAnimationStyle.SLIDE -> stringResource(R.string.lyrics_animation_slide)
                        LyricsAnimationStyle.KARAOKE -> stringResource(R.string.lyrics_animation_karaoke)
                        LyricsAnimationStyle.APPLE -> stringResource(R.string.lyrics_animation_apple)
                    },
                    onClick = { showLyricsAnimDialog = true },
                )
                RowDivider()
                SettingsRow(
                    icon = Icons.Rounded.AutoAwesome,
                    title = stringResource(R.string.lyrics_glow_effect),
                    subtitle = stringResource(R.string.lyrics_glow_effect_desc),
                    trailing = {
                        Switch(
                            checked = lyricsGlowEffect,
                            onCheckedChange = AppSettings::setLyricsGlowEffect,
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                checkedBorderColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    },
                    onClick = { AppSettings.setLyricsGlowEffect(!lyricsGlowEffect) },
                )
                RowDivider()
                SettingsRow(
                    icon = Icons.Rounded.Tune,
                    title = stringResource(R.string.lyrics_text_size),
                    subtitle = "Font size for lyric playback lines",
                    value = "${lyricsTextSize.roundToInt()} sp",
                    onClick = { showLyricsTextSizeDialog = true },
                )
                RowDivider()
                SettingsRow(
                    icon = Icons.Rounded.Tune,
                    title = stringResource(R.string.lyrics_line_spacing),
                    subtitle = "Vertical spacing multiplier between lyric lines",
                    value = String.format(Locale.US, "%.1fx", lyricsLineSpacing),
                    onClick = { showLyricsLineSpacingDialog = true },
                )
                RowDivider()
                SettingsRow(
                    icon = Icons.Rounded.MusicNote,
                    title = stringResource(R.string.lyrics_click_change),
                    subtitle = "Jump playback timestamp immediately upon tapping a line",
                    trailing = {
                        Switch(
                            checked = lyricsClickSeek,
                            onCheckedChange = AppSettings::setLyricsClickSeek,
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                checkedBorderColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    },
                    onClick = { AppSettings.setLyricsClickSeek(!lyricsClickSeek) },
                )
                RowDivider()
                SettingsRow(
                    icon = Icons.Rounded.Speed,
                    title = stringResource(R.string.lyrics_auto_scroll),
                    subtitle = "Keep playing lines centered in view automatically",
                    trailing = {
                        Switch(
                            checked = lyricsAutoScroll,
                            onCheckedChange = AppSettings::setLyricsAutoScroll,
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                checkedBorderColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    },
                    onClick = { AppSettings.setLyricsAutoScroll(!lyricsAutoScroll) },
                )
                RowDivider()
                SettingsRow(
                    icon = Icons.Rounded.Layers,
                    title = stringResource(R.string.respect_agent_positioning),
                    subtitle = stringResource(R.string.respect_agent_positioning_desc),
                    trailing = {
                        Switch(
                            checked = respectAgentPositioning,
                            onCheckedChange = AppSettings::setRespectAgentPositioning,
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                checkedBorderColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    },
                    onClick = { AppSettings.setRespectAgentPositioning(!respectAgentPositioning) },
                )
            }
        }

        // ── 5. Navigation & Miscellaneous ───────────────────────────────
        SettingsGroup(header = stringResource(R.string.miscellaneous)) {
            SettingsRow(
                icon = Icons.Rounded.Dashboard,
                title = stringResource(R.string.default_open_tab),
                subtitle = "Tab selected when the app opens",
                value = when (defaultOpenTab) {
                    0 -> "Play"
                    1 -> "Explore"
                    2 -> "Library"
                    3 -> "Search"
                    else -> "Play"
                },
                onClick = { showDefaultTabDialog = true },
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.GridView,
                title = stringResource(R.string.grid_cell_size),
                subtitle = "Density of card items on grids and albums",
                value = gridItemSize.label,
                onClick = { showGridSizeDialog = true },
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.ViewStream,
                title = stringResource(R.string.slim_navbar),
                subtitle = "Use a more compact bottom navigation dock",
                trailing = {
                    Switch(
                        checked = slimNavBar,
                        onCheckedChange = AppSettings::setSlimNavBar,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedBorderColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                },
                onClick = { AppSettings.setSlimNavBar(!slimNavBar) },
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.MusicNote,
                title = stringResource(R.string.show_recognize_music_button),
                subtitle = stringResource(R.string.show_recognize_music_button_desc),
                trailing = {
                    Switch(
                        checked = showRecognizeButton,
                        onCheckedChange = AppSettings::setShowRecognizeButton,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedBorderColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                },
                onClick = { AppSettings.setShowRecognizeButton(!showRecognizeButton) },
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.Shuffle,
                title = stringResource(R.string.show_play_random_button),
                subtitle = stringResource(R.string.show_play_random_button_desc),
                trailing = {
                    Switch(
                        checked = showPlayRandomButton,
                        onCheckedChange = AppSettings::setShowPlayRandomButton,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedBorderColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                },
                onClick = { AppSettings.setShowPlayRandomButton(!showPlayRandomButton) },
            )
        }

        // ── 6. Auto Playlists Visibility ────────────────────────────────
        SettingsGroup(header = stringResource(R.string.auto_playlists)) {
            SettingsRow(
                icon = Icons.Rounded.Favorite,
                title = stringResource(R.string.show_liked_playlist),
                trailing = {
                    Switch(
                        checked = showLikedPlaylist,
                        onCheckedChange = AppSettings::setShowLikedPlaylist,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedBorderColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                },
                onClick = { AppSettings.setShowLikedPlaylist(!showLikedPlaylist) },
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.Layers,
                title = stringResource(R.string.show_downloaded_playlist),
                trailing = {
                    Switch(
                        checked = showDownloadedPlaylist,
                        onCheckedChange = AppSettings::setShowDownloadedPlaylist,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedBorderColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                },
                onClick = { AppSettings.setShowDownloadedPlaylist(!showDownloadedPlaylist) },
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.Speed,
                title = stringResource(R.string.show_top_playlist),
                trailing = {
                    Switch(
                        checked = showTopPlaylist,
                        onCheckedChange = AppSettings::setShowTopPlaylist,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedBorderColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                },
                onClick = { AppSettings.setShowTopPlaylist(!showTopPlaylist) },
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.Tune,
                title = stringResource(R.string.show_cached_playlist),
                trailing = {
                    Switch(
                        checked = showCachedPlaylist,
                        onCheckedChange = AppSettings::setShowCachedPlaylist,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedBorderColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                },
                onClick = { AppSettings.setShowCachedPlaylist(!showCachedPlaylist) },
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.Dashboard,
                title = stringResource(R.string.show_uploaded_playlist),
                trailing = {
                    Switch(
                        checked = showUploadedPlaylist,
                        onCheckedChange = AppSettings::setShowUploadedPlaylist,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedBorderColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                },
                onClick = { AppSettings.setShowUploadedPlaylist(!showUploadedPlaylist) },
            )
        }

        Spacer(Modifier.height(32.dp))
    }

    // ── Dialogs ─────────────────────────────────────────────────────────

    // 1. Slider Style Selection Dialog
    if (showSliderStyleDialog) {
        SliderStyleDialog(
            onDismissRequest = { showSliderStyleDialog = false },
        )
    }

    // 2. Player Background Style Dialog
    if (showPlayerBgDialog) {
        AlertDialog(
            onDismissRequest = { showPlayerBgDialog = false },
            title = { Text(stringResource(R.string.player_background_style)) },
            text = {
                Column {
                    PlayerBackgroundStyle.entries.forEach { style ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    AppSettings.setPlayerBackgroundStyle(style)
                                    showPlayerBgDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = style.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (playerBackgroundStyle == style) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            if (playerBackgroundStyle == style) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPlayerBgDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    // 3. Mini Player Background Style Dialog
    if (showMiniPlayerBgDialog) {
        AlertDialog(
            onDismissRequest = { showMiniPlayerBgDialog = false },
            title = { Text(stringResource(R.string.mini_player_background_style)) },
            text = {
                Column {
                    MiniPlayerBackgroundStyle.entries.forEach { style ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    AppSettings.setMiniPlayerBackgroundStyle(style)
                                    showMiniPlayerBgDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = style.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (miniPlayerBackgroundStyle == style) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            if (miniPlayerBackgroundStyle == style) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMiniPlayerBgDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    // 4. Player Buttons Color Dialog
    if (showPlayerButtonsDialog) {
        AlertDialog(
            onDismissRequest = { showPlayerButtonsDialog = false },
            title = { Text(stringResource(R.string.player_buttons_style)) },
            text = {
                Column {
                    PlayerButtonsStyle.entries.forEach { style ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    AppSettings.setPlayerButtonsStyle(style)
                                    showPlayerButtonsDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = style.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (playerButtonsStyle == style) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            if (playerButtonsStyle == style) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPlayerButtonsDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    // 5. Swipe Sensitivity Dialog
    if (showSensitivityDialog) {
        var tempSens by remember { mutableFloatStateOf(swipeSensitivity) }
        AlertDialog(
            onDismissRequest = { showSensitivityDialog = false },
            title = { Text(stringResource(R.string.swipe_sensitivity)) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${(tempSens * 100).roundToInt()}%",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                    Slider(
                        value = tempSens,
                        onValueChange = { tempSens = it },
                        valueRange = 0.1f..1.0f,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    AppSettings.setSwipeSensitivity(tempSens)
                    showSensitivityDialog = false
                }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    tempSens = 0.73f
                }) {
                    Text(stringResource(R.string.reset))
                }
            },
        )
    }

    // 6. Lyrics Position Dialog
    if (showLyricsPositionDialog) {
        AlertDialog(
            onDismissRequest = { showLyricsPositionDialog = false },
            title = { Text(stringResource(R.string.lyrics_text_position)) },
            text = {
                Column {
                    LyricsPosition.entries.forEach { pos ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    AppSettings.setLyricsPosition(pos)
                                    showLyricsPositionDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = pos.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (lyricsPosition == pos) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            if (lyricsPosition == pos) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLyricsPositionDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    // 7. Lyrics Animation Style Dialog
    if (showLyricsAnimDialog) {
        AlertDialog(
            onDismissRequest = { showLyricsAnimDialog = false },
            title = { Text(stringResource(R.string.lyrics_animation_style_title)) },
            text = {
                Column {
                    LyricsAnimationStyle.entries.forEach { anim ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    AppSettings.setLyricsAnimationStyle(anim)
                                    showLyricsAnimDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = anim.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (lyricsAnimationStyle == anim) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            if (lyricsAnimationStyle == anim) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLyricsAnimDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    // 8. Lyrics Text Size Dialog
    if (showLyricsTextSizeDialog) {
        var tempSize by remember { mutableFloatStateOf(lyricsTextSize) }
        AlertDialog(
            onDismissRequest = { showLyricsTextSizeDialog = false },
            title = { Text(stringResource(R.string.lyrics_text_size)) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${tempSize.roundToInt()} sp",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                    Slider(
                        value = tempSize,
                        onValueChange = { tempSize = it },
                        valueRange = 14f..42f,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    AppSettings.setLyricsTextSize(tempSize)
                    showLyricsTextSizeDialog = false
                }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { tempSize = 24f }) {
                    Text(stringResource(R.string.reset))
                }
            },
        )
    }

    // 9. Lyrics Line Spacing Dialog
    if (showLyricsLineSpacingDialog) {
        var tempSpacing by remember { mutableFloatStateOf(lyricsLineSpacing) }
        AlertDialog(
            onDismissRequest = { showLyricsLineSpacingDialog = false },
            title = { Text(stringResource(R.string.lyrics_line_spacing)) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format(Locale.US, "%.1fx", tempSpacing),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                    Slider(
                        value = tempSpacing,
                        onValueChange = { tempSpacing = it },
                        valueRange = 1.0f..2.5f,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    AppSettings.setLyricsLineSpacing(tempSpacing)
                    showLyricsLineSpacingDialog = false
                }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { tempSpacing = 1.2f }) {
                    Text(stringResource(R.string.reset))
                }
            },
        )
    }

    // 10. Density Scale Dialog
    if (showDensityDialog) {
        AlertDialog(
            onDismissRequest = { showDensityDialog = false },
            title = { Text(stringResource(R.string.display_density)) },
            text = {
                Column {
                    DensityScale.entries.forEach { scale ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    AppSettings.setDensityScale(scale.value)
                                    showDensityDialog = false
                                    showDensityRestartDialog = true
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = scale.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (kotlin.math.abs(densityScale - scale.value) < 0.01f) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.weight(1f),
                            )
                            if (kotlin.math.abs(densityScale - scale.value) < 0.01f) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDensityDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    // Density Restart Dialog
    if (showDensityRestartDialog) {
        AlertDialog(
            onDismissRequest = { showDensityRestartDialog = false },
            title = { Text(stringResource(R.string.restart_required)) },
            text = { Text(stringResource(R.string.density_restart_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDensityRestartDialog = false
                    val packageManager = context.packageManager
                    val intent = packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    }
                    if (intent != null) {
                        context.startActivity(intent)
                        Runtime.getRuntime().exit(0)
                    }
                }) {
                    Text(stringResource(R.string.restart))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDensityRestartDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    // 11. Default Open Tab Dialog
    if (showDefaultTabDialog) {
        val tabNames = listOf("Play", "Explore", "Library", "Search")
        AlertDialog(
            onDismissRequest = { showDefaultTabDialog = false },
            title = { Text(stringResource(R.string.default_open_tab)) },
            text = {
                Column {
                    tabNames.forEachIndexed { index, name ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    AppSettings.setDefaultOpenTab(index)
                                    showDefaultTabDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (defaultOpenTab == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            if (defaultOpenTab == index) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDefaultTabDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    // 12. Grid Size Dialog
    if (showGridSizeDialog) {
        AlertDialog(
            onDismissRequest = { showGridSizeDialog = false },
            title = { Text(stringResource(R.string.grid_cell_size)) },
            text = {
                Column {
                    GridItemSize.entries.forEach { size ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    AppSettings.setGridItemSize(size)
                                    showGridSizeDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = size.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (gridItemSize == size) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            if (gridItemSize == size) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGridSizeDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}
