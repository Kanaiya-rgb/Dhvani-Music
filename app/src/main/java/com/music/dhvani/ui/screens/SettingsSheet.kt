package com.music.dhvani.ui.screens

import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BlurOff
import androidx.compose.material.icons.rounded.Brightness4
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material.icons.rounded.MotionPhotosOff
import androidx.compose.material.icons.rounded.BluetoothAudio
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.SurroundSound
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material.icons.rounded.Wifi
import com.music.dhvani.ui.components.EqualizerSheet
import com.music.dhvani.playback.eq.EqualizerManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.core.net.toUri
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import com.music.dhvani.ui.components.languageDisplayNameRes
import com.music.dhvani.ui.components.thumbnailBorder
import com.music.dhvani.data.model.Account
import com.music.dhvani.BuildConfig
import com.music.dhvani.data.scrobbling.LastFM
import com.music.dhvani.data.settings.AppSettings
import com.music.dhvani.R
import com.music.dhvani.data.sources.SourceKind
import com.music.dhvani.data.sources.SourceRegistry
import com.music.dhvani.data.settings.AudioQuality
import com.music.dhvani.data.settings.DownloadQuality
import com.music.dhvani.data.settings.SliderStyle
import com.music.dhvani.data.settings.ThemeMode
import com.music.dhvani.data.stats.Backup
import com.music.dhvani.ui.components.SliderStyleDialog
import com.music.dhvani.playback.AudioCache
import com.music.dhvani.ui.player.fullBleedArtworkAvailable
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import java.util.Locale

/**
 * Grouped settings, in the shape phones have taught people to expect: inset
 * cards of rows, a leading glyph per row, the current value on the right, and a
 * plain-language footer under any group whose effect isn't obvious from its
 * title. Anything with more than two choices opens a sheet rather than pushing
 * a row of chips into the layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    /** The window's width, for the gates that depend on it. */
    windowWidth: Dp,
    signedIn: Boolean,
    account: Account?,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onAccountScrobbling: () -> Unit,
    onOpenReplay: () -> Unit,
    onLyricsSources: () -> Unit,
    onSources: () -> Unit,
    onOpenAppearance: () -> Unit,
    onSpotifyCanvasAuth: () -> Unit,
    onAppLanguage: () -> Unit,
    onCheckForUpdates: () -> Unit = {},
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val wifiQuality by AppSettings.audioQualityWifi.collectAsStateWithLifecycle()
    val cellularQuality by AppSettings.audioQualityCellular.collectAsStateWithLifecycle()
    val metered by AppSettings.meteredConnection.collectAsStateWithLifecycle()
    val crossfade by AppSettings.crossfadeSeconds.collectAsStateWithLifecycle()
    val smartFade by AppSettings.smartFadeEnabled.collectAsStateWithLifecycle()
    val skipSilence by AppSettings.skipSilence.collectAsStateWithLifecycle()
    val spatialAudio by AppSettings.spatialAudio.collectAsStateWithLifecycle()
    val nerdStats by AppSettings.showNerdStats.collectAsStateWithLifecycle()
    val reduceAnimation by AppSettings.reduceAnimation.collectAsStateWithLifecycle()
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val animatedCanvas by AppSettings.animatedCanvas.collectAsStateWithLifecycle()
    val canvasOverCellular by AppSettings.canvasOverCellular.collectAsStateWithLifecycle()
    val fullBleedArtwork by AppSettings.fullBleedArtwork.collectAsStateWithLifecycle()
    val syncedLyrics by AppSettings.syncedLyrics.collectAsStateWithLifecycle()
    val lyricsSources by AppSettings.lyricsSources.collectAsStateWithLifecycle()
    val theme by AppSettings.themeMode.collectAsStateWithLifecycle()
    val sessionId by AppSettings.audioSessionId.collectAsStateWithLifecycle()
    val cacheLimitBytes by AppSettings.audioCacheLimitBytes.collectAsStateWithLifecycle()
    val downloadQuality by AppSettings.downloadQuality.collectAsStateWithLifecycle()
    val wifiOnlyDownloads by AppSettings.wifiOnlyDownloads.collectAsStateWithLifecycle()
    val sourceConfigs by SourceRegistry.configs.collectAsStateWithLifecycle()
    val stopOnTaskRemoved by AppSettings.stopOnTaskRemoved.collectAsStateWithLifecycle()
    val resumeOnBluetooth by AppSettings.resumeOnBluetooth.collectAsStateWithLifecycle()
    val sliderStyle by AppSettings.sliderStyle.collectAsStateWithLifecycle()
    val squigglySlider by AppSettings.squigglySlider.collectAsStateWithLifecycle()
    var showSliderStyleDialog by remember { mutableStateOf(false) }
    val hideVolumeBar by AppSettings.hideVolumeBar.collectAsStateWithLifecycle()
    val swipeToPlayNext by AppSettings.swipeToPlayNext.collectAsStateWithLifecycle()
    val dontRepeatSuggestions by AppSettings.dontRepeatSuggestions.collectAsStateWithLifecycle()
    val convertVideoToAudio by AppSettings.convertVideoToAudio.collectAsStateWithLifecycle()
    val equalizerEnabled by EqualizerManager.enabled.collectAsStateWithLifecycle()
    val equalizerPreset by EqualizerManager.selectedPreset.collectAsStateWithLifecycle()
    var showEqualizerSheet by remember { mutableStateOf(false) }

    // Whether the module index URL is baked into this build.
    val losslessConfigured = BuildConfig.MODULE_INDEX_URL.trim().isNotEmpty()
    // Whether the module source is currently enabled (toggle state).
    val moduleEnabled = sourceConfigs.any { it.kind == SourceKind.MODULE && it.enabled && it.isComplete }

    // Scrobbling states
    val lastfmEnabled by AppSettings.lastfmEnabled.collectAsStateWithLifecycle()
    val lastfmUsername by AppSettings.lastfmUsername.collectAsStateWithLifecycle()
    val lastfmSessionKey by AppSettings.lastfmSessionKey.collectAsStateWithLifecycle()
    val lastfmScrobbleEnabled by AppSettings.lastfmScrobbleEnabled.collectAsStateWithLifecycle()
    val lastfmNowPlayingEnabled by AppSettings.lastfmNowPlaying.collectAsStateWithLifecycle()
    val scrobbleMinDuration by AppSettings.scrobbleMinDuration.collectAsStateWithLifecycle()
    val scrobbleDelayPercent by AppSettings.scrobbleDelayPercent.collectAsStateWithLifecycle()
    val scrobbleDelaySeconds by AppSettings.scrobbleDelaySeconds.collectAsStateWithLifecycle()
    val listenBrainzEnabled by AppSettings.listenBrainzEnabled.collectAsStateWithLifecycle()
    val listenBrainzToken by AppSettings.listenBrainzToken.collectAsStateWithLifecycle()

    val replayGenres by AppSettings.replayGenres.collectAsStateWithLifecycle()

    var picking by remember { mutableStateOf<QualityTarget?>(null) }
    var pickingDownloadQuality by remember { mutableStateOf(false) }
    // What the last export or import did, shown on the row that did it rather
    // than as a toast: a backup is the one action here whose outcome nobody can
    // check by looking at the app afterwards. Held per direction, or an import's
    // result reports itself under the word "Export".
    var exportStatus by remember { mutableStateOf<String?>(null) }
    var importStatus by remember { mutableStateOf<String?>(null) }
    var confirmImport by remember { mutableStateOf(false) }
    val backupScope = rememberCoroutineScope()

    /**
     * Both halves go through the system document picker rather than a path of
     * this app's own choosing. That is what puts the file somewhere the user can
     * actually find it — Drive, Files, a folder they already back up — and it
     * means neither direction needs a storage permission, since the grant
     * arrives with the document they picked.
     */
    val exportPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { target ->
        if (target == null) return@rememberLauncherForActivityResult
        backupScope.launch {
            exportStatus = Backup.exportTo(context, target).fold(
                onSuccess = { months ->
                    "Exported settings and ${countOfMonths(months)}"
                },
                onFailure = { "Export failed: ${it.message ?: "unknown error"}" },
            )
        }
    }
    val importPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { source ->
        if (source == null) return@rememberLauncherForActivityResult
        backupScope.launch {
            importStatus = Backup.importFrom(context, source).fold(
                onSuccess = { "Imported ${countOfMonths(it.months)} from v${it.from}" },
                onFailure = { "Import failed: ${it.message ?: "unknown error"}" },
            )
        }
    }
    var showListenBrainzTokenDialog by remember { mutableStateOf(false) }
    var showLastfmLoginDialog by remember { mutableStateOf(false) }
    val scrobbleScope = rememberCoroutineScope()

    val version = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.0"
    }

    var currentSubScreen by remember { mutableStateOf<SettingsSubScreen?>(null) }

    BackHandler(enabled = currentSubScreen != null) {
        currentSubScreen = null
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
    ) {
        if (currentSubScreen == null) {
            Spacer(Modifier.height(4.dp))

            val selectedLanguage = try {
                AppCompatDelegate.getApplicationLocales().get(0)?.language
            } catch (_: Throwable) {
                null
            } ?: Locale.getDefault().language

            // Interface
            MeldSettingsGroup(
                title = "Interface",
                items = listOf(
                    MeldSettingsItemData(
                        icon = Icons.Rounded.Palette,
                        title = stringResource(R.string.appearance),
                        onClick = onOpenAppearance,
                    ),
                    MeldSettingsItemData(
                        icon = Icons.Rounded.Language,
                        title = stringResource(R.string.app_language),
                        subtitle = stringResource(languageDisplayNameRes(selectedLanguage)),
                        onClick = onAppLanguage,
                    ),
                ),
            )

            Spacer(Modifier.height(12.dp))

            // Player & Content
            MeldSettingsGroup(
                title = "Player & Content",
                items = listOf(
                    MeldSettingsItemData(
                        icon = Icons.Rounded.PlayArrow,
                        title = "Player and audio",
                        onClick = { currentSubScreen = SettingsSubScreen.PLAYER_AUDIO },
                    ),
                    MeldSettingsItemData(
                        icon = Icons.Rounded.Language,
                        title = "Content",
                        onClick = { currentSubScreen = SettingsSubScreen.CONTENT },
                    ),
                ),
            )

            Spacer(Modifier.height(12.dp))

            // Android Auto
            MeldSettingsGroup(
                title = "Android Auto",
                items = listOf(
                    MeldSettingsItemData(
                        icon = Icons.Rounded.DirectionsCar,
                        title = "Android Auto",
                        onClick = { currentSubScreen = SettingsSubScreen.ANDROID_AUTO },
                    ),
                ),
            )

            Spacer(Modifier.height(12.dp))

            // Privacy & Security
            MeldSettingsGroup(
                title = "Privacy & Security",
                items = listOf(
                    MeldSettingsItemData(
                        icon = Icons.Rounded.Security,
                        title = "Privacy",
                        onClick = { currentSubScreen = SettingsSubScreen.PRIVACY },
                    ),
                ),
            )

            Spacer(Modifier.height(12.dp))

            // Storage & Data
            MeldSettingsGroup(
                title = "Storage & Data",
                items = listOf(
                    MeldSettingsItemData(
                        icon = Icons.Rounded.Storage,
                        title = stringResource(R.string.storage),
                        onClick = { currentSubScreen = SettingsSubScreen.STORAGE },
                    ),
                    MeldSettingsItemData(
                        icon = Icons.Rounded.Cloud,
                        title = "Backup and restore",
                        onClick = { currentSubScreen = SettingsSubScreen.BACKUP_RESTORE },
                    ),
                ),
            )

            Spacer(Modifier.height(12.dp))

            // System & About
            MeldSettingsGroup(
                title = "System & About",
                items = listOf(
                    MeldSettingsItemData(
                        icon = Icons.Rounded.Link,
                        title = "Open supported links",
                        onClick = { openSupportedLinks(context) },
                    ),
                    MeldSettingsItemData(
                        icon = Icons.Rounded.Article,
                        title = "Changelog",
                        onClick = { currentSubScreen = SettingsSubScreen.CHANGELOG },
                    ),
                    MeldSettingsItemData(
                        icon = Icons.Rounded.SystemUpdate,
                        title = "Check for updates",
                        onClick = onCheckForUpdates,
                    ),
                    MeldSettingsItemData(
                        icon = Icons.Rounded.Info,
                        title = "About",
                        onClick = { currentSubScreen = SettingsSubScreen.ABOUT },
                    ),
                ),
            )

            // Footer with only GitHub and Telegram
            Text(
                text = buildAnnotatedString {
                    append("Dhvani Music $version\n")
                    val linkStyles = TextLinkStyles(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline,
                        ),
                    )
                    withLink(LinkAnnotation.Url("https://github.com/Kanaiya-rgb/Flux-Music", linkStyles)) {
                        append("GitHub")
                    }
                    append("   •   ")
                    withLink(LinkAnnotation.Url("https://t.me/DhvaniMusicApp", linkStyles)) {
                        append("Telegram")
                    }
                    append("\n~YouTube Music Backend")
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 16.dp),
            )
        } else {
            // Sub-screen header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 6.dp, end = 20.dp, top = 4.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { currentSubScreen = null }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text = currentSubScreen!!.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            when (currentSubScreen!!) {
                SettingsSubScreen.PLAYER_AUDIO -> {
                    SettingsGroup(header = "Audio quality") {
                        SettingsRow(
                            icon = Icons.Rounded.Extension,
                            title = "Sources",
                            subtitle = "Where audio comes from, and in what order",
                            onClick = onSources,
                        )
                        RowDivider()
                        SettingsRow(
                            icon = Icons.Rounded.Wifi,
                            title = stringResource(R.string.on_wifi),
                            badge = stringResource(R.string.in_use).takeIf { metered == false },
                            value = wifiQuality.localizedLabel(),
                            onClick = { picking = QualityTarget.WIFI },
                        )
                        RowDivider()
                        SettingsRow(
                            icon = Icons.Rounded.SignalCellularAlt,
                            title = stringResource(R.string.on_mobile_data),
                            badge = stringResource(R.string.in_use).takeIf { metered == true },
                            value = cellularQuality.localizedLabel(),
                            onClick = { picking = QualityTarget.CELLULAR },
                        )
                        RowDivider()
                        SettingsRow(
                            icon = Icons.Rounded.Download,
                            title = stringResource(R.string.download_quality),
                            value = downloadQuality.localizedLabel(),
                            onClick = { pickingDownloadQuality = true },
                        )
                        RowDivider()
                        SettingsRow(
                            icon = Icons.Rounded.Wifi,
                            title = stringResource(R.string.download_wifi_only),
                            subtitle = stringResource(R.string.blocking).takeIf { wifiOnlyDownloads && metered == true },
                            trailing = {
                                Switch(
                                    checked = wifiOnlyDownloads,
                                    onCheckedChange = AppSettings::setWifiOnlyDownloads,
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        checkedBorderColor = MaterialTheme.colorScheme.primary,
                                    ),
                                )
                            },
                            onClick = { AppSettings.setWifiOnlyDownloads(!wifiOnlyDownloads) },
                        )
                    }

                    SettingsGroup(header = "Audio processing") {
                        SettingsRow(
                            icon = Icons.Rounded.BarChart,
                            title = "Equalizer",
                            subtitle = if (equalizerEnabled) equalizerPreset else "Disabled",
                            trailing = { Chevron() },
                            onClick = { showEqualizerSheet = true },
                        )
                        RowDivider()
                        SettingsRow(
                            icon = Icons.Rounded.GraphicEq,
                            title = "System equalizer",
                            subtitle = "Open device audio effects panel",
                            trailing = { Chevron() },
                            onClick = { openEqualizer(context, sessionId ?: 0) },
                        )
                        RowDivider()
                        SettingsRow(
                            icon = Icons.Rounded.SurroundSound,
                            title = stringResource(R.string.spatial_audio),
                            subtitle = stringResource(R.string.spatial_audio_subtitle),
                            trailing = {
                                Switch(
                                    checked = spatialAudio,
                                    onCheckedChange = AppSettings::setSpatialAudio,
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        checkedBorderColor = MaterialTheme.colorScheme.primary,
                                    ),
                                )
                            },
                            onClick = { AppSettings.setSpatialAudio(!spatialAudio) },
                        )
                        RowDivider()
                        SettingsRow(
                            icon = Icons.AutoMirrored.Rounded.VolumeOff,
                            title = stringResource(R.string.skip_silence),
                            subtitle = stringResource(R.string.skip_silence_subtitle),
                            trailing = {
                                Switch(
                                    checked = skipSilence,
                                    onCheckedChange = AppSettings::setSkipSilence,
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        checkedBorderColor = MaterialTheme.colorScheme.primary,
                                    ),
                                )
                            },
                            onClick = { AppSettings.setSkipSilence(!skipSilence) },
                        )
                    }

                    SettingsGroup(header = "Transitions") {
                        SettingsRow(
                            icon = Icons.Rounded.AutoAwesome,
                            title = "Smart fade (Automix)",
                            subtitle = "AI beat-matched smooth transitions between songs",
                            trailing = {
                                Switch(
                                    checked = smartFade,
                                    onCheckedChange = AppSettings::setSmartFadeEnabled,
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        checkedBorderColor = MaterialTheme.colorScheme.primary,
                                    ),
                                )
                            },
                            onClick = { AppSettings.setSmartFadeEnabled(!smartFade) },
                        )
                        RowDivider()
                        SliderRow(
                            icon = Icons.Rounded.Waves,
                            title = stringResource(R.string.crossfade),
                            value = if (crossfade == 0) stringResource(R.string.off) else "${crossfade}s",
                            sliderValue = crossfade.toFloat(),
                            onSliderValue = { AppSettings.setCrossfadeSeconds(it.roundToInt()) },
                            valueRange = 0f..12f,
                            steps = 11,
                        )
                    }

                    SettingsGroup(header = "Playback behavior") {
                        SettingsRow(
                            icon = Icons.Rounded.BluetoothAudio,
                            title = stringResource(R.string.resume_on_bluetooth),
                            subtitle = stringResource(R.string.resume_on_bluetooth_subtitle),
                            trailing = {
                                Switch(
                                    checked = resumeOnBluetooth,
                                    onCheckedChange = AppSettings::setResumeOnBluetooth,
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        checkedBorderColor = MaterialTheme.colorScheme.primary,
                                    ),
                                )
                            },
                            onClick = { AppSettings.setResumeOnBluetooth(!resumeOnBluetooth) },
                        )
                        RowDivider()
                        SettingsRow(
                            icon = Icons.Rounded.Tune,
                            title = stringResource(R.string.player_slider_style),
                            subtitle = "Choose slider style for the main player",
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
                            icon = Icons.Rounded.VolumeOff,
                            title = stringResource(R.string.hide_volume_bar),
                            subtitle = stringResource(R.string.hide_volume_bar_subtitle),
                            trailing = {
                                Switch(
                                    checked = hideVolumeBar,
                                    onCheckedChange = AppSettings::setHideVolumeBar,
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        checkedBorderColor = MaterialTheme.colorScheme.primary,
                                    ),
                                )
                            },
                            onClick = { AppSettings.setHideVolumeBar(!hideVolumeBar) },
                        )
                        RowDivider()
                        SettingsRow(
                            icon = Icons.Rounded.PlaylistPlay,
                            title = stringResource(R.string.play_next_on_swipe),
                            subtitle = if (swipeToPlayNext) "Swiping a song plays it next" else "Swiping a song adds it to the end of the queue when disabled",
                            trailing = {
                                Switch(
                                    checked = swipeToPlayNext,
                                    onCheckedChange = AppSettings::setSwipeToPlayNext,
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        checkedBorderColor = MaterialTheme.colorScheme.primary,
                                    ),
                                )
                            },
                            onClick = { AppSettings.setSwipeToPlayNext(!swipeToPlayNext) },
                        )
                        RowDivider()
                        SettingsRow(
                            icon = Icons.Rounded.MusicOff,
                            title = stringResource(R.string.dont_repeat_songs),
                            subtitle = stringResource(R.string.dont_repeat_songs_subtitle),
                            trailing = {
                                Switch(
                                    checked = dontRepeatSuggestions,
                                    onCheckedChange = AppSettings::setDontRepeatSuggestions,
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        checkedBorderColor = MaterialTheme.colorScheme.primary,
                                    ),
                                )
                            },
                            onClick = { AppSettings.setDontRepeatSuggestions(!dontRepeatSuggestions) },
                        )
                        RowDivider()
                        SettingsRow(
                            icon = Icons.Rounded.DeleteSweep,
                            title = "Stop playback on task remove",
                            subtitle = "Kill background playback service when app is swiped away from recents",
                            trailing = {
                                Switch(
                                    checked = stopOnTaskRemoved,
                                    onCheckedChange = AppSettings::setStopOnTaskRemoved,
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        checkedBorderColor = MaterialTheme.colorScheme.primary,
                                    ),
                                )
                            },
                            onClick = { AppSettings.setStopOnTaskRemoved(!stopOnTaskRemoved) },
                        )
                    }
                }

                SettingsSubScreen.CONTENT -> {
                    SettingsGroup(header = stringResource(R.string.language)) {
                        val selectedLanguage = AppCompatDelegate.getApplicationLocales().get(0)?.language
                            ?: Locale.getDefault().language
                        SettingsRow(
                            icon = Icons.Rounded.Language,
                            title = stringResource(R.string.app_language),
                            subtitle = stringResource(languageDisplayNameRes(selectedLanguage)),
                            onClick = onAppLanguage,
                        )
                    }

                    SettingsGroup(header = stringResource(R.string.lyrics_sources)) {
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
                        RowDivider()
                        SettingsRow(
                            icon = Icons.Rounded.LocalOffer,
                            title = "Lyrics sources",
                            subtitle = lyricsSources.sortedBy { it.ordinal }.joinToString(", ") { it.label },
                            trailing = { Chevron() },
                            onClick = onLyricsSources,
                        )
                    }

                    SettingsGroup(header = "Spotify Canvas & Artwork") {
                        SettingsRow(
                            icon = Icons.Rounded.SmartDisplay,
                            title = "Spotify Canvas authorization",
                            subtitle = "Connect Spotify account for looping canvas videos",
                            trailing = { Chevron() },
                            onClick = onSpotifyCanvasAuth,
                        )
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
                        RowDivider()
                        SettingsRow(
                            icon = Icons.Rounded.SignalCellularAlt,
                            title = stringResource(R.string.animated_cover_cellular),
                            subtitle = "Stream looping canvas video over mobile data",
                            trailing = {
                                Switch(
                                    checked = canvasOverCellular,
                                    onCheckedChange = AppSettings::setCanvasOverCellular,
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        checkedBorderColor = MaterialTheme.colorScheme.primary,
                                    ),
                                )
                            },
                            onClick = { AppSettings.setCanvasOverCellular(!canvasOverCellular) },
                        )
                    }

                    SettingsGroup(header = "Audio conversion") {
                        SettingsRow(
                            icon = Icons.Rounded.FileDownload,
                            title = "Convert video to audio",
                            subtitle = "Extract and prioritize audio-only streams when playing YouTube videos",
                            trailing = {
                                Switch(
                                    checked = convertVideoToAudio,
                                    onCheckedChange = AppSettings::setConvertVideoToAudio,
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        checkedBorderColor = MaterialTheme.colorScheme.primary,
                                    ),
                                )
                            },
                            onClick = { AppSettings.setConvertVideoToAudio(!convertVideoToAudio) },
                        )
                    }
                }

                SettingsSubScreen.ANDROID_AUTO -> {
                    SettingsGroup(header = "Android Auto") {
                        SettingsRow(
                            icon = Icons.Rounded.DirectionsCar,
                            title = "Android Auto Service",
                            subtitle = "Dhvani Music supports Android Auto media browsing and audio playback in vehicles.",
                        )
                        RowDivider()
                        SettingsRow(
                            icon = Icons.Rounded.Tune,
                            title = "System Android Auto settings",
                            subtitle = "Configure connected cars and projection preferences",
                            trailing = { Chevron() },
                            onClick = {
                                try {
                                    val intent = Intent("android.intent.action.MAIN").apply {
                                        setClassName("com.google.android.projection.gearhead", "com.google.android.projection.gearhead.companion.settings.DefaultSettingsActivity")
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    Toast.makeText(context, "Android Auto app not installed or settings unavailable", Toast.LENGTH_SHORT).show()
                                }
                            },
                        )
                        RowDivider()
                        SettingsRow(
                            icon = Icons.Rounded.DeleteSweep,
                            title = "Stop playback on disconnect",
                            subtitle = "Pause playback when vehicle disconnects",
                            trailing = {
                                Switch(
                                    checked = stopOnTaskRemoved,
                                    onCheckedChange = AppSettings::setStopOnTaskRemoved,
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        checkedBorderColor = MaterialTheme.colorScheme.primary,
                                    ),
                                )
                            },
                            onClick = { AppSettings.setStopOnTaskRemoved(!stopOnTaskRemoved) },
                        )
                    }
                }

                SettingsSubScreen.PRIVACY -> {
                    AccountCard(
                        signedIn = signedIn,
                        account = account,
                        onSignIn = onSignIn,
                    )

                    Spacer(Modifier.height(14.dp))

                    SettingsGroup(header = "Account & Integrations") {
                        SettingsRow(
                            icon = Icons.Rounded.Person,
                            title = stringResource(R.string.account_integrations),
                            subtitle = account?.email?.takeIf { it.isNotBlank() }
                                ?: if (signedIn) "Signed in" else "Not signed in",
                            onClick = onAccountScrobbling,
                        )
                        RowDivider()
                        SettingsRow(
                            icon = Icons.Rounded.BarChart,
                            title = stringResource(R.string.show_nerd_stats),
                            subtitle = stringResource(R.string.show_nerd_stats_subtitle),
                            trailing = {
                                Switch(
                                    checked = nerdStats,
                                    onCheckedChange = AppSettings::setShowNerdStats,
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        checkedBorderColor = MaterialTheme.colorScheme.primary,
                                    ),
                                )
                            },
                            onClick = { AppSettings.setShowNerdStats(!nerdStats) },
                        )
                    }

                    SettingsGroup(header = "Last.fm Scrobbling") {
                        SettingsRow(
                            icon = Icons.Rounded.Cloud,
                            title = "Last.fm Account",
                            subtitle = if (lastfmUsername.isNotBlank()) "Logged in as $lastfmUsername" else "Tap to log in",
                            onClick = { showLastfmLoginDialog = true },
                        )
                        if (lastfmUsername.isNotBlank()) {
                            RowDivider()
                            SettingsSubRow(
                                title = "Scrobble tracks",
                                checked = lastfmScrobbleEnabled,
                                onCheckedChange = AppSettings::setLastfmScrobbleEnabled,
                            )
                            RowDivider()
                            SettingsSubRow(
                                title = "Send \"Now Playing\"",
                                checked = lastfmNowPlayingEnabled,
                                onCheckedChange = AppSettings::setLastfmNowPlaying,
                            )
                            RowDivider()
                            SliderRow(
                                icon = Icons.Rounded.Waves,
                                title = "Delay percentage",
                                subtitle = "Scrobble after playing this much of the track",
                                value = "${(scrobbleDelayPercent * 100).roundToInt()}%",
                                sliderValue = scrobbleDelayPercent,
                                onSliderValue = { AppSettings.setScrobbleDelayPercent(it) },
                                valueRange = 0.1f..0.9f,
                                steps = 7,
                            )
                            RowDivider()
                            SliderRow(
                                icon = Icons.Rounded.Waves,
                                title = "Delay seconds",
                                subtitle = "Fixed delay alternative",
                                value = "${scrobbleDelaySeconds}s",
                                sliderValue = scrobbleDelaySeconds.toFloat(),
                                onSliderValue = { AppSettings.setScrobbleDelaySeconds(it.roundToInt()) },
                                valueRange = 10f..240f,
                                steps = 22,
                            )
                            RowDivider()
                            SliderRow(
                                icon = Icons.Rounded.Waves,
                                title = "Minimum duration",
                                subtitle = "Don't scrobble tracks shorter than this",
                                value = "${scrobbleMinDuration}s",
                                sliderValue = scrobbleMinDuration.toFloat(),
                                onSliderValue = { AppSettings.setScrobbleMinDuration(it.roundToInt()) },
                                valueRange = 10f..60f,
                                steps = 4,
                            )
                            RowDivider()
                            DestructiveRow(label = "Log out of Last.fm") {
                                AppSettings.setLastfmSessionKey("")
                                AppSettings.setLastfmUsername("")
                                AppSettings.setLastfmEnabled(false)
                            }
                        }
                    }

                    SettingsGroup(header = "ListenBrainz Scrobbling") {
                        SettingsRow(
                            icon = Icons.Rounded.Cloud,
                            title = "ListenBrainz API Token",
                            subtitle = if (listenBrainzToken.isNotBlank()) "Token configured" else "Tap to configure token",
                            onClick = { showListenBrainzTokenDialog = true },
                        )
                        if (listenBrainzToken.isNotBlank()) {
                            RowDivider()
                            SettingsSubRow(
                                title = "Scrobble to ListenBrainz",
                                checked = listenBrainzEnabled,
                                onCheckedChange = AppSettings::setListenBrainzEnabled,
                            )
                        }
                    }
                }

                SettingsSubScreen.STORAGE -> {
                    val mb = (cacheLimitBytes / (1024L * 1024L)).toInt()
                    val warning = mb >= CACHE_WARNING_MB

                    SettingsGroup(
                        header = stringResource(R.string.storage),
                        footer = "Downloaded audio kept on disk for instant seeking and replays",
                    ) {
                        SliderRow(
                            icon = Icons.Rounded.Storage,
                            title = stringResource(R.string.song_cache_limit),
                            value = formatCacheSize(mb),
                            sliderValue = mb.toFloat(),
                            onSliderValue = {
                                val bytes = it.roundToInt().toLong() * 1024L * 1024L
                                AppSettings.setAudioCacheLimitBytes(bytes)
                            },
                            valueRange = 256f..8192f,
                            steps = 30,
                            subtitle = if (warning) "That's a real chunk of most phones' free storage." else null,
                        )
                        RowDivider()
                        SettingsRow(
                            icon = Icons.Rounded.DeleteSweep,
                            title = stringResource(R.string.clear_song_cache),
                            subtitle = stringResource(R.string.clear_song_cache_subtitle),
                            onClick = {
                                AudioCache.clear {
                                    Toast.makeText(context, "Song cache cleared", Toast.LENGTH_SHORT).show()
                                }
                            },
                        )
                    }

                    SettingsGroup(header = "Downloads") {
                        SettingsRow(
                            icon = Icons.Rounded.Download,
                            title = stringResource(R.string.download_quality),
                            value = downloadQuality.localizedLabel(),
                            onClick = { pickingDownloadQuality = true },
                        )
                        RowDivider()
                        SettingsRow(
                            icon = Icons.Rounded.Wifi,
                            title = stringResource(R.string.download_wifi_only),
                            subtitle = stringResource(R.string.blocking).takeIf { wifiOnlyDownloads && metered == true },
                            trailing = {
                                Switch(
                                    checked = wifiOnlyDownloads,
                                    onCheckedChange = AppSettings::setWifiOnlyDownloads,
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        checkedBorderColor = MaterialTheme.colorScheme.primary,
                                    ),
                                )
                            },
                            onClick = { AppSettings.setWifiOnlyDownloads(!wifiOnlyDownloads) },
                        )
                    }
                }

                SettingsSubScreen.BACKUP_RESTORE -> {
                    SettingsGroup(
                        header = "Backup and restore",
                        footer = "Backup exports settings and history to a JSON file you can keep anywhere.",
                    ) {
                        SettingsRow(
                            icon = Icons.Rounded.FileUpload,
                            title = stringResource(R.string.export_data),
                            subtitle = exportStatus ?: stringResource(R.string.export_data_subtitle),
                            onClick = { exportPicker.launch("dhvani-backup.json") },
                        )
                        RowDivider()
                        SettingsRow(
                            icon = Icons.Rounded.FileDownload,
                            title = stringResource(R.string.import_data),
                            subtitle = importStatus ?: stringResource(R.string.import_data_subtitle),
                            onClick = { confirmImport = true },
                        )
                    }
                }

                SettingsSubScreen.CHANGELOG -> {
                    SettingsGroup(header = "Release Notes") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        ) {
                            Text(
                                text = "Dhvani Music v$version",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "• Meld-style Material 3 settings hub with categorized sections\n" +
                                    "• YouTube Music home feed with Quick Picks and infinite scroll\n" +
                                    "• On-screen volume change percentage feedback\n" +
                                    "• Spotify Canvas video background integration\n" +
                                    "• Full offline downloads & local music library support\n" +
                                    "• Last.fm & ListenBrainz scrobbling\n" +
                                    "• Smart fade & crossfade automix engine",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                SettingsSubScreen.ABOUT -> {
                    SettingsGroup(header = "About Dhvani Music") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_dhvani_logo),
                                contentDescription = "Dhvani Music",
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(18.dp)),
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "Dhvani Music",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Version $version",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "A private, high-fidelity music streaming player with YouTube Music backend and Material 3 design.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    SettingsGroup(header = "Updates") {
                        SettingsRow(
                            icon = Icons.Rounded.SystemUpdate,
                            title = "Check for updates",
                            subtitle = "Installed version: v$version",
                            onClick = onCheckForUpdates,
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    SettingsGroup(header = "Links & Community") {
                        SettingsRow(
                            icon = Icons.Rounded.Link,
                            title = "GitHub Repository",
                            subtitle = "https://github.com/Kanaiya-rgb/Flux-Music",
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, "https://github.com/Kanaiya-rgb/Flux-Music".toUri()).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                runCatching { context.startActivity(intent) }
                            },
                        )
                        RowDivider()
                        SettingsRow(
                            icon = Icons.Rounded.Link,
                            title = "Telegram Channel",
                            subtitle = "https://t.me/DhvaniMusicApp",
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, "https://t.me/DhvaniMusicApp".toUri()).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                runCatching { context.startActivity(intent) }
                            },
                        )
                    }
                }
            }
        }
    }

    picking?.let { target ->
        ModalBottomSheet(
            onDismissRequest = { picking = null },
            containerColor = MaterialTheme.colorScheme.background,
        ) {
            QualitySheet(
                target = target,
                selected = when (target) {
                    QualityTarget.WIFI -> wifiQuality
                    QualityTarget.CELLULAR -> cellularQuality
                },
                onSelect = { quality ->
                    when (target) {
                        QualityTarget.WIFI -> AppSettings.setAudioQualityWifi(quality)
                        QualityTarget.CELLULAR -> AppSettings.setAudioQualityCellular(quality)
                    }
                    picking = null
                },
            )
        }
    }

    if (pickingDownloadQuality) {
        ModalBottomSheet(
            onDismissRequest = { pickingDownloadQuality = false },
            containerColor = MaterialTheme.colorScheme.background,
        ) {
            DownloadQualitySheet(
                selected = downloadQuality,
                onSelect = { quality ->
                    AppSettings.setDownloadQuality(quality)
                    pickingDownloadQuality = false
                },
            )
        }
    }

    // Asked before the picker opens rather than after a file is chosen: the
    // thing being confirmed is that this device's own history is about to be
    // thrown away, and that is true whichever file gets picked.
    if (confirmImport) {
        AlertDialog(
            onDismissRequest = { confirmImport = false },
            title = { Text(stringResource(R.string.import_backup_title)) },
            text = {
                Text(
                    "This replaces the settings and the listening history on this device " +
                        "with whatever is in the file. What is here now cannot be got back, " +
                        "so export it first if you want to keep it.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmImport = false
                    importPicker.launch(arrayOf("application/json", "text/plain", "*/*"))
                }) {
                    Text(stringResource(R.string.choose_file))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmImport = false }) { Text("Cancel") }
            },
        )
    }

    if (showListenBrainzTokenDialog) {
        var tokenInput by remember { mutableStateOf(listenBrainzToken) }
        AlertDialog(
            onDismissRequest = { showListenBrainzTokenDialog = false },
            title = { Text("ListenBrainz Token") },
            text = {
                OutlinedTextField(
                    value = tokenInput,
                    onValueChange = { tokenInput = it },
                    label = { Text("API Token") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    AppSettings.setListenBrainzToken(tokenInput.trim())
                    showListenBrainzTokenDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showListenBrainzTokenDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showLastfmLoginDialog) {
        var usernameInput by remember { mutableStateOf("") }
        var passwordInput by remember { mutableStateOf("") }
        var lastfmError by remember { mutableStateOf<String?>(null) }
        var lastfmLoading by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!lastfmLoading) showLastfmLoginDialog = false },
            title = { Text("Last.fm Login") },
            text = {
                Column {
                    if (lastfmError != null) {
                        Text(
                            text = lastfmError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it },
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        lastfmLoading = true
                        lastfmError = null
                        scrobbleScope.launch {
                            try {
                                // Use the credentials supplied for this build.
                                LastFM.initialize(
                                    apiKey = AppSettings.lastfmApiKey.value,
                                    secret = AppSettings.lastfmSecret.value,
                                )
                                LastFM.getMobileSession(usernameInput.trim(), passwordInput)
                                    .onSuccess { auth ->
                                        AppSettings.setLastfmSessionKey(auth.session.key)
                                        AppSettings.setLastfmUsername(auth.session.name)
                                        AppSettings.setLastfmEnabled(true)
                                        showLastfmLoginDialog = false
                                    }
                                    .onFailure { e ->
                                        lastfmError = e.message ?: "Login failed"
                                    }
                            } catch (e: Exception) {
                                lastfmError = e.message ?: "Login failed"
                            } finally {
                                lastfmLoading = false
                            }
                        }
                    },
                    enabled = !lastfmLoading && usernameInput.isNotBlank() && passwordInput.isNotBlank(),
                ) {
                    Text(if (lastfmLoading) "Signing in..." else "Sign in")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLastfmLoginDialog = false }, enabled = !lastfmLoading) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showEqualizerSheet) {
        EqualizerSheet(onDismiss = { showEqualizerSheet = false })
    }

    if (showSliderStyleDialog) {
        SliderStyleDialog(onDismissRequest = { showSliderStyleDialog = false })
    }
}

private enum class SettingsSubScreen(val title: String) {
    PLAYER_AUDIO("Player and audio"),
    CONTENT("Content"),
    ANDROID_AUTO("Android Auto"),
    PRIVACY("Privacy"),
    STORAGE("Storage"),
    BACKUP_RESTORE("Backup and restore"),
    CHANGELOG("Changelog"),
    ABOUT("About"),
}

private data class MeldSettingsItemData(
    val icon: ImageVector,
    val title: String,
    val subtitle: String? = null,
    val badge: String? = null,
    val onClick: () -> Unit,
)

@Composable
private fun MeldSettingsGroup(
    title: String? = null,
    items: List<MeldSettingsItemData>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, top = 14.dp, bottom = 8.dp),
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items.forEachIndexed { index, item ->
                val shape = when {
                    items.size == 1 -> RoundedCornerShape(24.dp)
                    index == 0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 6.dp, bottomEnd = 6.dp)
                    index == items.size - 1 -> RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
                    else -> RoundedCornerShape(6.dp)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .clickable(onClick = item.onClick)
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    Spacer(Modifier.width(16.dp))

                    Column(Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (item.subtitle != null) {
                            Text(
                                text = item.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    if (item.badge != null) {
                        Spacer(Modifier.width(8.dp))
                        Badge(item.badge)
                    }
                }
            }
        }
    }
}

private fun openSupportedLinks(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        try {
            val intent = Intent(
                AndroidSettings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,
                "package:${context.packageName}".toUri(),
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = Intent(
                    AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    "package:${context.packageName}".toUri(),
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(context, "Could not open settings", Toast.LENGTH_SHORT).show()
            }
        }
    } else {
        try {
            val intent = Intent(
                AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS,
                "package:${context.packageName}".toUri(),
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Could not open settings", Toast.LENGTH_SHORT).show()
        }
    }
}

/** "3 months of listening" — the unit a backup is actually measured in. */
private fun countOfMonths(months: Int): String =
    if (months == 0) "no listening history" else "$months month${if (months == 1) "" else "s"} of listening"

/** Which ceiling the open picker is editing. */
private enum class QualityTarget(val title: String, val icon: ImageVector) {
    WIFI("Wi-Fi", Icons.Rounded.Wifi),
    CELLULAR("Mobile data", Icons.Rounded.SignalCellularAlt),
}

@Composable
private fun AudioQuality.localizedLabel(): String = stringResource(
    when (this) {
        AudioQuality.LOW -> R.string.low
        AudioQuality.MEDIUM -> R.string.medium
        AudioQuality.HIGH -> R.string.high
    },
)

@Composable
private fun DownloadQuality.localizedLabel(): String = stringResource(
    when (this) {
        DownloadQuality.STANDARD -> R.string.standard
        DownloadQuality.HIGH -> R.string.high
        DownloadQuality.LOSSLESS -> R.string.lossless
    },
)

@Composable
internal fun ThemeMode.localizedLabel(): String = stringResource(
    when (this) {
        ThemeMode.SYSTEM -> R.string.system
        ThemeMode.LIGHT -> R.string.light
        ThemeMode.DARK -> R.string.dark
    },
)

private fun openEqualizer(context: Context, sessionId: Int) {
    val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
        putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
        putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
        putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
    }
    runCatching { context.startActivity(intent) }.onFailure {
        Toast.makeText(context, "No system equalizer on this device", Toast.LENGTH_SHORT).show()
    }
}

/** Above this, the cache limit slider's subtitle warns rather than reassures. */
private const val CACHE_WARNING_MB = 2048

/** "512 MB", "2 GB", "2.5 GB" — whichever reads more naturally at that size. */
private fun formatCacheSize(mb: Int): String {
    if (mb < 1024) return "$mb MB"
    val gb = mb / 1024f
    return if (gb == gb.toInt().toFloat()) "${gb.toInt()} GB" else "%.1f GB".format(Locale.ROOT, gb)
}

/** Who you're signed in as, straight from YouTube Music's account menu. */
@Composable
internal fun AccountCard(
    signedIn: Boolean,
    account: Account?,
    onSignIn: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = GROUP_INSET)
            .clip(GroupShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(if (signedIn) Modifier else Modifier.clickable(onClick = onSignIn))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (account?.thumbnailUrl != null) {
            AsyncImage(
                model = account.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(52.dp).clip(CircleShape).thumbnailBorder(CircleShape),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outline),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = account?.name ?: if (signedIn) "Signed in" else "Not signed in",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = account?.email?.takeIf { it.isNotBlank() }
                    ?: if (signedIn) "YouTube Music account" else "Tap to sign in with Google",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (!signedIn) {
            Spacer(Modifier.width(8.dp))
            Chevron()
        }
    }
}

/** The quality options for one connection, with what each costs in data. */
@Composable
private fun QualitySheet(
    target: QualityTarget,
    selected: AudioQuality,
    onSelect: (AudioQuality) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        Row(
            modifier = Modifier.padding(start = 22.dp, end = 22.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = target.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = "Audio quality",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "While on ${target.title.lowercase(Locale.ROOT)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)

        // Best first — the option most people want shouldn't be last.
        AudioQuality.entries.reversed().forEach { quality ->
            val chosen = quality == selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelect(quality)
                    }
                    .padding(horizontal = 22.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = quality.localizedLabel(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "${quality.detail} · ${quality.hourly}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (chosen) {
                    Spacer(Modifier.width(12.dp))
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

/**
 * What to keep when a track is saved, with what each rung costs on disk.
 *
 * Priced per track rather than per hour, the way [QualitySheet] is. That sheet
 * is answering "what will listening cost me this hour", because a stream is
 * spent again on every replay; this one is answering "what will keeping this
 * cost me", and the answer is charged once. Same widget, different question, so
 * the numbers beside the options are in different units on purpose.
 */
@Composable
private fun DownloadQualitySheet(
    selected: DownloadQuality,
    onSelect: (DownloadQuality) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        Row(
            modifier = Modifier.padding(start = 22.dp, end = 22.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Download,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = "Download quality",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "For files kept on this device",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)

        // Best first, matching [QualitySheet] — and here the best rung is also
        // the default, so the checkmark starts where the eye does.
        DownloadQuality.entries.reversed().forEach { quality ->
            val chosen = quality == selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelect(quality)
                    }
                    .padding(horizontal = 22.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = quality.localizedLabel(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "${quality.detail} · ${quality.perTrack} per track",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (chosen) {
                    Spacer(Modifier.width(12.dp))
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

// ---- Building blocks --------------------------------------------------------

internal val GroupShape = RoundedCornerShape(14.dp)
internal val GROUP_INSET = 16.dp
internal val ROW_INSET = 16.dp
internal val ICON_SIZE = 22.dp
internal val ICON_GAP = 14.dp

/** Where a row's text starts — dividers are inset to match, as on iOS. */
internal val TEXT_INSET = ROW_INSET + ICON_SIZE + ICON_GAP

/**
 * One inset card of rows, with an uppercase header above and an optional
 * plain-language [footer] below. Rows are separated by [RowDivider].
 */
@Composable
internal fun SettingsGroup(
    header: String? = null,
    footer: String? = null,
    content: @Composable () -> Unit,
) {
    if (header != null) {
        Text(
            text = header.uppercase(Locale.ROOT),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(
                start = GROUP_INSET + 4.dp,
                end = GROUP_INSET,
                top = 26.dp,
                bottom = 8.dp,
            ),
        )
    } else {
        Spacer(Modifier.height(26.dp))
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = GROUP_INSET)
            .clip(GroupShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        content()
    }
    if (footer != null) {
        Text(
            text = footer,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(
                start = GROUP_INSET + 4.dp,
                end = GROUP_INSET + 4.dp,
                top = 8.dp,
            ),
        )
    }
}

@Composable
internal fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = TEXT_INSET),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outline,
    )
}

/**
 * The standard row: glyph, title, optional subtitle, and on the right either
 * [trailing] (a switch, say) or the current [value] followed by a chevron.
 */
@Composable
internal fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    subtitleContent: (@Composable () -> Unit)? = null,
    value: String? = null,
    badge: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null && enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .alpha(if (enabled) 1f else 0.45f)
            .heightIn(min = 52.dp)
            .padding(horizontal = ROW_INSET, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(ICON_SIZE),
        )
        Spacer(Modifier.width(ICON_GAP))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (badge != null) {
                    Spacer(Modifier.width(8.dp))
                    Badge(badge)
                }
            }
            if (subtitleContent != null) {
                subtitleContent()
            } else if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 5,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        if (trailing != null) {
            trailing()
        } else if (value != null || onClick != null) {
            if (value != null) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Spacer(Modifier.width(4.dp))
            }
            Chevron()
        }
    }
}

/**
 * A toggle that reads as part of the option above it rather than a setting
 * of its own: no icon, no divider, and pulled up close against its parent
 * instead of getting the same breathing room a full [SettingsRow] gets.
 */
@Composable
internal fun SettingsSubRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    badge: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(start = ROW_INSET, end = ROW_INSET, top = 0.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (badge != null) {
                Spacer(Modifier.width(8.dp))
                Badge(badge)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedBorderColor = MaterialTheme.colorScheme.primary,
            ),
        )
    }
}

/** Marks the connection whose ceiling is actually in force right now. */
@Composable
internal fun Badge(text: String) {
    Text(
        text = text.uppercase(Locale.ROOT),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
internal fun Chevron() {
    Icon(
        Icons.Rounded.ChevronRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.size(20.dp),
    )
}

/** A continuous setting: label and current value on one line, track beneath. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SliderRow(
    icon: ImageVector,
    title: String,
    value: String,
    sliderValue: Float,
    onSliderValue: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    subtitle: String? = null,
) {
    val colors = SliderDefaults.colors(
        thumbColor = MaterialTheme.colorScheme.primary,
        activeTrackColor = MaterialTheme.colorScheme.primary,
        inactiveTrackColor = MaterialTheme.colorScheme.outline,
    )
    Column(Modifier.padding(start = ROW_INSET, end = ROW_INSET, top = 12.dp, bottom = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(ICON_SIZE),
            )
            Spacer(Modifier.width(ICON_GAP))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = sliderValue,
            onValueChange = onSliderValue,
            valueRange = valueRange,
            steps = steps,
            colors = colors,
            // Bare track: the step ticks and the end-stop dot are noise when the
            // value is already spelled out on the line above.
            track = { state ->
                SliderDefaults.Track(
                    sliderState = state,
                    colors = colors,
                    drawStopIndicator = null,
                    drawTick = { _, _ -> },
                )
            },
            modifier = Modifier.padding(start = ICON_SIZE + ICON_GAP),
        )
    }
}

/** Sign out: centered, accent-coloured, no glyph — the shape of a real one. */
@Composable
internal fun DestructiveRow(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/** Sliding pill selector, for the handful of settings with two or three states. */
@Composable
internal fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.outline)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEachIndexed { index, label ->
            val chosen = index == selectedIndex
            val pill by animateColorAsState(
                targetValue = if (chosen) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.Transparent
                },
                animationSpec = tween(160),
                label = "segmentPill",
            )
            val labelColor by animateColorAsState(
                targetValue = if (chosen) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                animationSpec = tween(160),
                label = "segmentLabel",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(pill)
                    .clickable {
                        if (!chosen) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelect(index)
                        }
                    }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = labelColor,
                    maxLines = 1,
                )
            }
        }
    }
}
