package com.music.dhvani.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.HideImage
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MotionPhotosOff
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewStream
import androidx.compose.material.icons.rounded.SlowMotionVideo
import androidx.compose.material.icons.rounded.Style
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import com.music.dhvani.data.settings.UiDesignStyle
import com.music.dhvani.ui.theme.uiDesignCard
import com.music.dhvani.ui.haptics.rememberHaptics
import com.music.dhvani.ui.haptics.Haptic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.dhvani.R
import com.music.dhvani.data.settings.AppSettings
import com.music.dhvani.data.settings.CanvasStyle
import com.music.dhvani.data.settings.NowPlayingDefaultMode
import com.music.dhvani.data.settings.DensityScale
import com.music.dhvani.data.settings.GridItemSize
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
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isSystemDark = isSystemInDarkTheme()

    val theme by AppSettings.themeMode.collectAsStateWithLifecycle()
    val dynamicTheme by AppSettings.dynamicTheme.collectAsStateWithLifecycle()
    val uiDesignStyle by AppSettings.uiDesignStyle.collectAsStateWithLifecycle()
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
    val spotifyCanvasStyle by AppSettings.spotifyCanvasStyle.collectAsStateWithLifecycle()
    val appleMusicCanvasStyle by AppSettings.appleMusicCanvasStyle.collectAsStateWithLifecycle()
    val tidalCanvasStyle by AppSettings.tidalCanvasStyle.collectAsStateWithLifecycle()
    val communityCanvasStyle by AppSettings.communityCanvasStyle.collectAsStateWithLifecycle()
    val canvasPauseWithAudio by AppSettings.canvasPauseWithAudio.collectAsStateWithLifecycle()
    val playerDefaultViewMode by AppSettings.playerDefaultViewMode.collectAsStateWithLifecycle()
    val showStatusBarIcon by AppSettings.showStatusBarIcon.collectAsStateWithLifecycle()
    val reduceAnimation by AppSettings.reduceAnimation.collectAsStateWithLifecycle()
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()

    val swipeThumbnail by AppSettings.swipeThumbnail.collectAsStateWithLifecycle()
    val swipeSensitivity by AppSettings.swipeSensitivity.collectAsStateWithLifecycle()

    val defaultOpenTab by AppSettings.defaultOpenTab.collectAsStateWithLifecycle()
    val gridItemSize by AppSettings.gridItemSize.collectAsStateWithLifecycle()
    val slimNavBar by AppSettings.slimNavBar.collectAsStateWithLifecycle()
    val dynamicIslandEnabled by AppSettings.dynamicIslandEnabled.collectAsStateWithLifecycle()
    val systemDynamicIslandEnabled by AppSettings.systemDynamicIslandEnabled.collectAsStateWithLifecycle()
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
    var showDefaultTabDialog by remember { mutableStateOf(false) }
    var showGridSizeDialog by remember { mutableStateOf(false) }
    var showCoverStyleDialog by remember { mutableStateOf(false) }
    var showSpotifyCanvasStyleDialog by remember { mutableStateOf(false) }
    var showAppleCanvasStyleDialog by remember { mutableStateOf(false) }
    var showTidalCanvasStyleDialog by remember { mutableStateOf(false) }
    var showCommunityCanvasStyleDialog by remember { mutableStateOf(false) }
    var showPlayerDefaultModeDialog by remember { mutableStateOf(false) }

    val handleSystemDynamicIslandToggle: (Boolean) -> Unit = { enable ->
        if (enable) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                Toast.makeText(
                    context,
                    "Please grant 'Display over other apps' to show Dynamic Island everywhere",
                    Toast.LENGTH_LONG,
                ).show()
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}"),
                )
                context.startActivity(intent)
            } else {
                AppSettings.setSystemDynamicIslandEnabled(true)
            }
        } else {
            AppSettings.setSystemDynamicIslandEnabled(false)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
    ) {
        Spacer(Modifier.height(4.dp))

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

        // ── 1.5. UI Design & Aesthetic Style ─────────────────────────────
        val haptics = rememberHaptics()
        SettingsGroup(header = "UI Design & Aesthetic Style") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
            ) {
                Text(
                    text = "Choose your favorite app interface design style. All cards, sheets, and bars will adopt this aesthetic.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = ROW_INSET, vertical = 2.dp),
                )
                Spacer(Modifier.height(12.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = ROW_INSET),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(UiDesignStyle.entries, key = { it.id }) { style ->
                        val selected = style == uiDesignStyle
                        UiDesignStyleCard(
                            style = style,
                            selected = selected,
                            onClick = {
                                haptics.play(Haptic.Select)
                                AppSettings.setUiDesignStyle(style)
                            },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${uiDesignStyle.displayName}: ${uiDesignStyle.description}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = ROW_INSET, vertical = 4.dp),
                )
            }
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
                    title = "Album cover display style",
                    subtitle = "Choose layout for static album artwork in player",
                    value = if (fullBleedArtwork) "Full-Bleed Banner" else "1:1 Square Card",
                    onClick = { showCoverStyleDialog = true },
                )
            }
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.SlowMotionVideo,
                title = "Spotify canvas style",
                subtitle = "Choose layout for Spotify video canvas loops",
                value = spotifyCanvasStyle.label,
                onClick = { showSpotifyCanvasStyleDialog = true },
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.SlowMotionVideo,
                title = "Apple Music canvas style",
                subtitle = "Choose layout for Apple Music animated loops",
                value = appleMusicCanvasStyle.label,
                onClick = { showAppleCanvasStyleDialog = true },
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.SlowMotionVideo,
                title = "Tidal canvas style",
                subtitle = "Choose layout for Tidal motion video loops",
                value = tidalCanvasStyle.label,
                onClick = { showTidalCanvasStyleDialog = true },
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.SlowMotionVideo,
                title = "Community canvas style",
                subtitle = "Choose layout for Community video canvas clips",
                value = communityCanvasStyle.label,
                onClick = { showCommunityCanvasStyleDialog = true },
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.SlowMotionVideo,
                title = "Player open mode",
                subtitle = "Which view to show when reopening player after minimising",
                value = playerDefaultViewMode.label,
                onClick = { showPlayerDefaultModeDialog = true },
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.SlowMotionVideo,
                title = "Canvas plays while paused",
                subtitle = "Keep the video looping even when audio is paused",
                trailing = {
                    Switch(
                        checked = !canvasPauseWithAudio,
                        onCheckedChange = { AppSettings.setCanvasPauseWithAudio(!it) },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedBorderColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                },
                onClick = { AppSettings.setCanvasPauseWithAudio(!canvasPauseWithAudio) },
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.Notifications,
                title = "Status bar playback icon",
                subtitle = "Show Dhvani logo in status bar while music is playing",
                trailing = {
                    Switch(
                        checked = showStatusBarIcon,
                        onCheckedChange = AppSettings::setShowStatusBarIcon,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedBorderColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                },
                onClick = { AppSettings.setShowStatusBarIcon(!showStatusBarIcon) },
            )
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

        // ── 4. Dynamic Island ───────────────────────────────────────────
        SettingsGroup(header = "Dynamic Island") {
            SettingsRow(
                icon = Icons.Rounded.GraphicEq,
                title = "Dynamic Island Notch Player",
                subtitle = "iPhone-style floating capsule with live equalizer while playing",
                trailing = {
                    Switch(
                        checked = dynamicIslandEnabled,
                        onCheckedChange = AppSettings::setDynamicIslandEnabled,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedBorderColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                },
                onClick = { AppSettings.setDynamicIslandEnabled(!dynamicIslandEnabled) },
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.Layers,
                title = "System-Wide Floating Island",
                subtitle = "Float Dynamic Island over other apps with swipe-up to hide",
                trailing = {
                    Switch(
                        checked = systemDynamicIslandEnabled,
                        onCheckedChange = handleSystemDynamicIslandToggle,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedBorderColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                },
                onClick = { handleSystemDynamicIslandToggle(!systemDynamicIslandEnabled) },
            )
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

    // 6. Density Scale Dialog
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

    // 7. Default Open Tab Dialog
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

    // 8. Grid Size Dialog
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

    if (showCoverStyleDialog) {
        CoverStyleDialog(
            isFullBleed = fullBleedArtwork,
            onSelect = { AppSettings.setFullBleedArtwork(it) },
            onDismiss = { showCoverStyleDialog = false },
        )
    }

    if (showSpotifyCanvasStyleDialog) {
        CanvasStyleDialog(
            title = "Spotify Canvas Style",
            currentStyle = spotifyCanvasStyle,
            onSelect = { AppSettings.setSpotifyCanvasStyle(it) },
            onDismiss = { showSpotifyCanvasStyleDialog = false },
        )
    }

    if (showAppleCanvasStyleDialog) {
        CanvasStyleDialog(
            title = "Apple Music Canvas Style",
            currentStyle = appleMusicCanvasStyle,
            onSelect = { AppSettings.setAppleMusicCanvasStyle(it) },
            onDismiss = { showAppleCanvasStyleDialog = false },
        )
    }

    if (showTidalCanvasStyleDialog) {
        CanvasStyleDialog(
            title = "Tidal Canvas Style",
            currentStyle = tidalCanvasStyle,
            onSelect = { AppSettings.setTidalCanvasStyle(it) },
            onDismiss = { showTidalCanvasStyleDialog = false },
        )
    }

    if (showCommunityCanvasStyleDialog) {
        CanvasStyleDialog(
            title = "Community Canvas Style",
            currentStyle = communityCanvasStyle,
            onSelect = { AppSettings.setCommunityCanvasStyle(it) },
            onDismiss = { showCommunityCanvasStyleDialog = false },
        )
    }

    if (showPlayerDefaultModeDialog) {
        AlertDialog(
            onDismissRequest = { showPlayerDefaultModeDialog = false },
            title = { Text("Player open mode") },
            text = {
                Column {
                    NowPlayingDefaultMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    AppSettings.setPlayerDefaultViewMode(mode)
                                    showPlayerDefaultModeDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = playerDefaultViewMode == mode,
                                onClick = {
                                    AppSettings.setPlayerDefaultViewMode(mode)
                                    showPlayerDefaultModeDialog = false
                                },
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = mode.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                Text(
                                    text = when (mode) {
                                        NowPlayingDefaultMode.VIDEO -> "Always open on the canvas / video tab"
                                        NowPlayingDefaultMode.MUSIC -> "Always open on the album cover tab"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPlayerDefaultModeDialog = false }) {
                    Text("Close")
                }
            },
        )
    }
}


@Composable
private fun CanvasStyleDialog(
    title: String,
    currentStyle: CanvasStyle,
    onSelect: (CanvasStyle) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                CanvasStyle.entries.forEach { style ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(style)
                                onDismiss()
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = style.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (currentStyle == style) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = style.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            )
                        }
                        if (currentStyle == style) {
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
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun CoverStyleDialog(
    isFullBleed: Boolean,
    onSelect: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = listOf(
        true to ("Full-Bleed Banner" to "Top half banner with smooth bottom fade into background"),
        false to ("1:1 Square Card" to "Centered album sleeve card with rounded corners"),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Album Cover Style") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                options.forEach { (fullBleed, info) ->
                    val (label, desc) = info
                    val isSelected = isFullBleed == fullBleed
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(fullBleed)
                                onDismiss()
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            )
                        }
                        if (isSelected) {
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
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun UiDesignStyleCard(
    style: UiDesignStyle,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    val borderWidth = if (selected) 2.dp else 1.dp
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(112.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .uiDesignCard(style = style, shape = RoundedCornerShape(16.dp))
                .border(borderWidth, borderColor, RoundedCornerShape(16.dp))
                .padding(10.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start,
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected) primaryColor.copy(alpha = 0.25f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Style,
                        contentDescription = null,
                        tint = if (selected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (selected) primaryColor.copy(alpha = 0.7f)
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                            )
                    )
                    Box(
                        modifier = Modifier
                            .weight(0.6f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (selected) primaryColor.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
                            )
                    )
                }
            }

            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(primaryColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = style.displayName,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

