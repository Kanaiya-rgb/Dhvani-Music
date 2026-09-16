package com.music.dhvani.ui.screens

import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.widget.Toast
import com.music.dhvani.playback.DolbyUtils
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
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
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material.icons.rounded.MotionPhotosOff
import androidx.compose.material.icons.rounded.BluetoothAudio
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.SurroundSound
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import android.content.ComponentName
import com.music.dhvani.data.settings.LyricsAnimationStyle
import com.music.dhvani.data.settings.LyricsPosition
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material.icons.rounded.CloudDownload
import com.music.dhvani.data.settings.DownloadNetwork
import com.music.dhvani.ui.components.EqualizerSheet
import com.music.dhvani.playback.eq.EqualizerManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import com.music.dhvani.data.changelog.ChangelogRepository
import com.music.dhvani.data.changelog.ReleaseChangelog
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
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
import com.music.dhvani.data.settings.AudioListeningMode
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
    onTranslationLanguage: () -> Unit = {},
    onSources: () -> Unit,
    onOpenAppearance: () -> Unit,
    onAppLanguage: () -> Unit,
    onSubScreenChange: (title: String?, onBack: (() -> Unit)?) -> Unit = { _, _ -> },
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
    val dolbyAtmosEnabled by AppSettings.dolbyAtmosEnabled.collectAsStateWithLifecycle()
    val audioListeningMode by AppSettings.audioListeningMode.collectAsStateWithLifecycle()
    var showListeningModeDialog by remember { mutableStateOf(false) }
    val nerdStats by AppSettings.showNerdStats.collectAsStateWithLifecycle()
    val reduceAnimation by AppSettings.reduceAnimation.collectAsStateWithLifecycle()
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val fullBleedArtwork by AppSettings.fullBleedArtwork.collectAsStateWithLifecycle()
    val syncedLyrics by AppSettings.syncedLyrics.collectAsStateWithLifecycle()
    val lyricsSources by AppSettings.lyricsSources.collectAsStateWithLifecycle()
    val translationLanguage by AppSettings.translationLanguage.collectAsStateWithLifecycle()
    val lyricsPosition by AppSettings.lyricsPosition.collectAsStateWithLifecycle()
    val lyricsAnimationStyle by AppSettings.lyricsAnimationStyle.collectAsStateWithLifecycle()
    val lyricsGlowEffect by AppSettings.lyricsGlowEffect.collectAsStateWithLifecycle()
    val lyricsTextSize by AppSettings.lyricsTextSize.collectAsStateWithLifecycle()
    val lyricsLineSpacing by AppSettings.lyricsLineSpacing.collectAsStateWithLifecycle()
    val lyricsClickSeek by AppSettings.lyricsClickSeek.collectAsStateWithLifecycle()
    val lyricsAutoScroll by AppSettings.lyricsAutoScroll.collectAsStateWithLifecycle()
    val respectAgentPositioning by AppSettings.respectAgentPositioning.collectAsStateWithLifecycle()

    var showLyricsPositionDialog by remember { mutableStateOf(false) }
    var showLyricsAnimDialog by remember { mutableStateOf(false) }
    var showLyricsTextSizeDialog by remember { mutableStateOf(false) }
    var showLyricsLineSpacingDialog by remember { mutableStateOf(false) }

    val theme by AppSettings.themeMode.collectAsStateWithLifecycle()
    val sessionId by AppSettings.audioSessionId.collectAsStateWithLifecycle()
    val downloadQuality by AppSettings.downloadQuality.collectAsStateWithLifecycle()
    val downloadNetwork by AppSettings.downloadNetwork.collectAsStateWithLifecycle()
    val wifiOnlyDownloads by AppSettings.wifiOnlyDownloads.collectAsStateWithLifecycle()
    val alwaysAskDownloadOptions by AppSettings.alwaysAskDownloadOptions.collectAsStateWithLifecycle()
    val cacheLimitBytes by AppSettings.audioCacheLimitBytes.collectAsStateWithLifecycle()
    val sourceConfigs by SourceRegistry.configs.collectAsStateWithLifecycle()
    val stopOnTaskRemoved by AppSettings.stopOnTaskRemoved.collectAsStateWithLifecycle()
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }
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
    var pickingDownloadNetwork by remember { mutableStateOf(false) }
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

    var currentSubScreen by rememberSaveable { mutableStateOf<SettingsSubScreen?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val changelogReleases by ChangelogRepository.releases.collectAsStateWithLifecycle()
    val isChangelogLoading by ChangelogRepository.isLoading.collectAsStateWithLifecycle()
    val changelogScope = rememberCoroutineScope()

    LaunchedEffect(currentSubScreen) {
        if (currentSubScreen != null) {
            onSubScreenChange(currentSubScreen!!.title) { currentSubScreen = null }
            if (currentSubScreen == SettingsSubScreen.CHANGELOG) {
                ChangelogRepository.refresh()
            }
        } else {
            onSubScreenChange(null, null)
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            onSubScreenChange(null, null)
        }
    }

    BackHandler(enabled = currentSubScreen != null || searchQuery.isNotEmpty()) {
        if (searchQuery.isNotEmpty()) {
            searchQuery = ""
        } else {
            currentSubScreen = null
        }
    }

    val selectedLanguage = try {
        AppCompatDelegate.getApplicationLocales().get(0)?.language
    } catch (_: Throwable) {
        null
    } ?: Locale.getDefault().language

    val searchableItems = remember(
        context,
        wifiQuality,
        cellularQuality,
        downloadQuality,
        downloadNetwork,
        crossfade,
        smartFade,
        spatialAudio,
        skipSilence,
        equalizerPreset,
        equalizerEnabled,
        syncedLyrics,
        lyricsSources,
        theme,
        lastfmUsername,
        listenBrainzToken,
        hasNotificationPermission,
        signedIn,
        account,
        version,
    ) {
        listOf(
            // Appearance & Theming
            SearchableSettingItem(
                title = "Appearance & Theming",
                subtitle = "Theme mode, dynamic colors, dark/light, AMOLED black",
                category = "Appearance",
                icon = Icons.Rounded.Palette,
                onClick = onOpenAppearance,
            ),
            SearchableSettingItem(
                title = "Player slider style",
                subtitle = "Squiggly, wavy, slim, capsule, and Material seekbars",
                category = "Appearance",
                icon = Icons.Rounded.Tune,
                onClick = { showSliderStyleDialog = true },
            ),

            // Playback & Audio
            SearchableSettingItem(
                title = "Audio sources",
                subtitle = "Where audio streams are resolved (lossless / YouTube Music)",
                category = "Playback & Audio",
                icon = Icons.Rounded.Extension,
                onClick = onSources,
            ),
            SearchableSettingItem(
                title = "Streaming quality on Wi-Fi",
                subtitle = "Current: ${wifiQuality.name.lowercase().replaceFirstChar { it.uppercase() }}",
                category = "Playback & Audio",
                icon = Icons.Rounded.Wifi,
                onClick = {
                    currentSubScreen = SettingsSubScreen.PLAYBACK_AUDIO
                    picking = QualityTarget.WIFI
                },
            ),
            SearchableSettingItem(
                title = "Streaming quality on Mobile Data",
                subtitle = "Current: ${cellularQuality.name.lowercase().replaceFirstChar { it.uppercase() }}",
                category = "Playback & Audio",
                icon = Icons.Rounded.SignalCellularAlt,
                onClick = {
                    currentSubScreen = SettingsSubScreen.PLAYBACK_AUDIO
                    picking = QualityTarget.CELLULAR
                },
            ),
            SearchableSettingItem(
                title = "Crossfade",
                subtitle = if (crossfade == 0) "Disabled (0s)" else "${crossfade}s crossfade duration",
                category = "Playback & Audio",
                icon = Icons.Rounded.Waves,
                onClick = { currentSubScreen = SettingsSubScreen.PLAYBACK_AUDIO },
            ),
            SearchableSettingItem(
                title = "Smart fade (Automix)",
                subtitle = "AI beat-matched transitions between songs",
                category = "Playback & Audio",
                icon = Icons.Rounded.AutoAwesome,
                onClick = { currentSubScreen = SettingsSubScreen.PLAYBACK_AUDIO },
            ),
            SearchableSettingItem(
                title = "Equalizer",
                subtitle = if (equalizerEnabled) equalizerPreset else "Built-in parametric equalizer",
                category = "Playback & Audio",
                icon = Icons.Rounded.BarChart,
                onClick = { showEqualizerSheet = true },
            ),
            SearchableSettingItem(
                title = "System equalizer",
                subtitle = "Device hardware audio effects panel",
                category = "Playback & Audio",
                icon = Icons.Rounded.GraphicEq,
                onClick = { openEqualizer(context, sessionId) },
            ),
            SearchableSettingItem(
                title = "Spatial audio",
                subtitle = "Widens stereo soundfield for headphones and speakers",
                category = "Playback & Audio",
                icon = Icons.Rounded.SurroundSound,
                onClick = { currentSubScreen = SettingsSubScreen.PLAYBACK_AUDIO },
            ),
            SearchableSettingItem(
                title = "Listening mode",
                subtitle = "Choose Dolby Atmos, Lossless Audio, Both, or Standard Stereo",
                category = "Playback & Audio",
                icon = Icons.Rounded.Headphones,
                onClick = { currentSubScreen = SettingsSubScreen.PLAYBACK_AUDIO },
            ),
            SearchableSettingItem(
                title = "Dolby Atmos & Spatial Audio",
                subtitle = "Toggle 3D surround soundstage and system Dolby Atmos processing",
                category = "Playback & Audio",
                icon = Icons.Rounded.SurroundSound,
                onClick = { currentSubScreen = SettingsSubScreen.PLAYBACK_AUDIO },
            ),
            SearchableSettingItem(
                title = "Lossless audio module",
                subtitle = "Configure FLAC, ALAC, and hi-res modular streaming plugins",
                category = "Playback & Audio",
                icon = Icons.Rounded.Extension,
                onClick = onSources,
            ),
            SearchableSettingItem(
                title = "Skip silence",
                subtitle = "Automatically bypass dead silent gaps in tracks",
                category = "Playback & Audio",
                icon = Icons.AutoMirrored.Rounded.VolumeOff,
                onClick = { currentSubScreen = SettingsSubScreen.PLAYBACK_AUDIO },
            ),
            SearchableSettingItem(
                title = "Swipe to play next",
                subtitle = "Swiping a song queues it immediately as next track",
                category = "Playback & Audio",
                icon = Icons.AutoMirrored.Rounded.PlaylistPlay,
                onClick = { currentSubScreen = SettingsSubScreen.PLAYBACK_AUDIO },
            ),
            SearchableSettingItem(
                title = "Don't repeat songs",
                subtitle = "Prevent AutoPlay suggestions from repeating in the session",
                category = "Playback & Audio",
                icon = Icons.Rounded.MusicOff,
                onClick = { currentSubScreen = SettingsSubScreen.PLAYBACK_AUDIO },
            ),
            SearchableSettingItem(
                title = "Stop playback on close",
                subtitle = "Kill background playback service when app is swiped away from recents",
                category = "Playback & Audio",
                icon = Icons.Rounded.DeleteSweep,
                onClick = { currentSubScreen = SettingsSubScreen.PLAYBACK_AUDIO },
            ),

            // Lyrics & Content
            SearchableSettingItem(
                title = "App language",
                subtitle = "Select user interface language",
                category = "Lyrics & Content",
                icon = Icons.Rounded.Language,
                onClick = onAppLanguage,
            ),
            SearchableSettingItem(
                title = "Synced lyrics",
                subtitle = "Word-by-word karaoke highlighting and live sing-along",
                category = "Lyrics & Content",
                icon = Icons.AutoMirrored.Rounded.Notes,
                onClick = { currentSubScreen = SettingsSubScreen.LYRICS_CONTENT },
            ),
            SearchableSettingItem(
                title = "Lyrics sources",
                subtitle = "BiniLyrics, BetterLyrics, PaxSenix, YouTube, Musixmatch, LRCLIB",
                category = "Lyrics & Content",
                icon = Icons.Rounded.LocalOffer,
                onClick = onLyricsSources,
            ),
            SearchableSettingItem(
                title = "Translation language",
                subtitle = "The language the translate button turns lyrics into",
                category = "Lyrics & Content",
                icon = Icons.Rounded.Translate,
                onClick = onTranslationLanguage,
            ),
            SearchableSettingItem(
                title = "Lyrics animation style",
                subtitle = "16 kinetic text effects and shaders (Apple, Neon, Glitch, Liquid, Ember)",
                category = "Lyrics & Content",
                icon = Icons.Rounded.AutoAwesome,
                onClick = {
                    currentSubScreen = SettingsSubScreen.LYRICS_CONTENT
                    showLyricsAnimDialog = true
                },
            ),
            SearchableSettingItem(
                title = "Lyrics text position",
                subtitle = "Align lyrics text to left, center, or right",
                category = "Lyrics & Content",
                icon = Icons.Rounded.Tune,
                onClick = {
                    currentSubScreen = SettingsSubScreen.LYRICS_CONTENT
                    showLyricsPositionDialog = true
                },
            ),
            SearchableSettingItem(
                title = "Lyrics font size",
                subtitle = "Adjust size of lyric playback lines",
                category = "Lyrics & Content",
                icon = Icons.Rounded.Tune,
                onClick = {
                    currentSubScreen = SettingsSubScreen.LYRICS_CONTENT
                    showLyricsTextSizeDialog = true
                },
            ),
            SearchableSettingItem(
                title = "Lyrics line spacing",
                subtitle = "Adjust vertical line spacing multiplier",
                category = "Lyrics & Content",
                icon = Icons.Rounded.Tune,
                onClick = {
                    currentSubScreen = SettingsSubScreen.LYRICS_CONTENT
                    showLyricsLineSpacingDialog = true
                },
            ),
            SearchableSettingItem(
                title = "Lyrics glow effect",
                subtitle = "Soft radiant glow highlight behind active lyric lines",
                category = "Lyrics & Content",
                icon = Icons.Rounded.AutoAwesome,
                onClick = { currentSubScreen = SettingsSubScreen.LYRICS_CONTENT },
            ),
            SearchableSettingItem(
                title = "Tap to seek (Lyrics)",
                subtitle = "Jump playback timestamp immediately upon tapping a line",
                category = "Lyrics & Content",
                icon = Icons.Rounded.MusicNote,
                onClick = { currentSubScreen = SettingsSubScreen.LYRICS_CONTENT },
            ),
            SearchableSettingItem(
                title = "Lyrics auto scroll",
                subtitle = "Keep active lines centered in view automatically",
                category = "Lyrics & Content",
                icon = Icons.Rounded.Speed,
                onClick = { currentSubScreen = SettingsSubScreen.LYRICS_CONTENT },
            ),
            SearchableSettingItem(
                title = "Convert video to audio",
                subtitle = "Force audio-only playback for uploaded music videos",
                category = "Lyrics & Content",
                icon = Icons.Rounded.FileDownload,
                onClick = { currentSubScreen = SettingsSubScreen.LYRICS_CONTENT },
            ),

            // Downloads & Storage
            SearchableSettingItem(
                title = "Download audio quality",
                subtitle = "Format & bitrate for saved offline songs",
                category = "Downloads & Storage",
                icon = Icons.Rounded.Download,
                onClick = {
                    currentSubScreen = SettingsSubScreen.DOWNLOADS_STORAGE
                    pickingDownloadQuality = true
                },
            ),
            SearchableSettingItem(
                title = "Download network policy",
                subtitle = "Allow downloads on Mobile Data, Wi-Fi only, or Both",
                category = "Downloads & Storage",
                icon = Icons.Rounded.CloudDownload,
                onClick = {
                    currentSubScreen = SettingsSubScreen.DOWNLOADS_STORAGE
                    pickingDownloadNetwork = true
                },
            ),
            SearchableSettingItem(
                title = "Ask quality before download",
                subtitle = "Prompt to select audio quality and network policy every time you download a song",
                category = "Downloads & Storage",
                icon = Icons.AutoMirrored.Rounded.HelpOutline,
                onClick = { currentSubScreen = SettingsSubScreen.DOWNLOADS_STORAGE },
            ),
            SearchableSettingItem(
                title = "Song cache limit",
                subtitle = "Allocate storage for instant song seeks & replay caching",
                category = "Downloads & Storage",
                icon = Icons.Rounded.Storage,
                onClick = { currentSubScreen = SettingsSubScreen.DOWNLOADS_STORAGE },
            ),
            SearchableSettingItem(
                title = "Clear song cache",
                subtitle = "Frees space used by cached streaming audio",
                category = "Downloads & Storage",
                icon = Icons.Rounded.DeleteSweep,
                onClick = {
                    AudioCache.clear {
                        Toast.makeText(context, "Song cache cleared", Toast.LENGTH_SHORT).show()
                    }
                },
            ),

            // Accounts & Integrations
            SearchableSettingItem(
                title = "Google Account / YouTube Music",
                subtitle = if (signedIn) "Signed in (${account?.name ?: account?.email ?: ""})" else "Sign in for playlists & library sync",
                category = "Accounts & Integrations",
                icon = Icons.Rounded.Person,
                onClick = { currentSubScreen = SettingsSubScreen.ACCOUNTS_INTEGRATIONS },
            ),
            SearchableSettingItem(
                title = "Listen Together",
                subtitle = "Host or join synchronized music sessions with friends",
                category = "Accounts & Integrations",
                icon = Icons.Rounded.GraphicEq,
                onClick = onAccountScrobbling,
            ),
            SearchableSettingItem(
                title = "Last.fm Scrobbling",
                subtitle = if (lastfmUsername.isNotBlank()) "Connected as $lastfmUsername" else "Log in to track playback stats",
                category = "Accounts & Integrations",
                icon = Icons.Rounded.Cloud,
                onClick = {
                    currentSubScreen = SettingsSubScreen.ACCOUNTS_INTEGRATIONS
                    showLastfmLoginDialog = true
                },
            ),
            SearchableSettingItem(
                title = "ListenBrainz Scrobbling",
                subtitle = if (listenBrainzToken.isNotBlank()) "Token configured" else "Enter ListenBrainz API token",
                category = "Accounts & Integrations",
                icon = Icons.Rounded.Cloud,
                onClick = {
                    currentSubScreen = SettingsSubScreen.ACCOUNTS_INTEGRATIONS
                    showListenBrainzTokenDialog = true
                },
            ),
            SearchableSettingItem(
                title = "Discord Rich Presence",
                subtitle = "Show current playing track as your Discord activity status",
                category = "Accounts & Integrations",
                icon = Icons.Rounded.Person,
                onClick = onAccountScrobbling,
            ),

            // Car & External Devices
            SearchableSettingItem(
                title = "Resume on Bluetooth",
                subtitle = "Automatically restart playback when wireless audio connects",
                category = "Car & External Devices",
                icon = Icons.Rounded.BluetoothAudio,
                onClick = { currentSubScreen = SettingsSubScreen.CAR_DEVICES },
            ),
            SearchableSettingItem(
                title = "Android Auto",
                subtitle = "In-car infotainment audio streaming and system configuration",
                category = "Car & External Devices",
                icon = Icons.Rounded.DirectionsCar,
                onClick = { currentSubScreen = SettingsSubScreen.CAR_DEVICES },
            ),

            // Backup & Restore
            SearchableSettingItem(
                title = "Export data & settings",
                subtitle = "Save preferences and listening history as JSON",
                category = "Backup & Restore",
                icon = Icons.Rounded.FileUpload,
                onClick = {
                    currentSubScreen = SettingsSubScreen.BACKUP_RESTORE
                    exportPicker.launch(Backup.suggestedName())
                },
            ),
            SearchableSettingItem(
                title = "Import data & settings",
                subtitle = "Restore app backup file",
                category = "Backup & Restore",
                icon = Icons.Rounded.FileDownload,
                onClick = {
                    currentSubScreen = SettingsSubScreen.BACKUP_RESTORE
                    confirmImport = true
                },
            ),

            // About & Updates
            SearchableSettingItem(
                title = "Check for updates",
                subtitle = "Current version v$version",
                category = "About & Updates",
                icon = Icons.Rounded.SystemUpdate,
                onClick = onCheckForUpdates,
            ),
            SearchableSettingItem(
                title = "Update notification alerts",
                subtitle = if (hasNotificationPermission) "Active & allowed" else "Grant notification permission",
                category = "About & Updates",
                icon = Icons.Rounded.Notifications,
                onClick = { currentSubScreen = SettingsSubScreen.ABOUT },
            ),
            SearchableSettingItem(
                title = "Changelog",
                subtitle = "Release notes and new features in Dhvani Music",
                category = "About & Updates",
                icon = Icons.AutoMirrored.Rounded.Article,
                onClick = { currentSubScreen = SettingsSubScreen.CHANGELOG },
            ),
            SearchableSettingItem(
                title = "Stats for nerds",
                subtitle = "Audio format, codec, bitrate, sample rate on player",
                category = "About & Updates",
                icon = Icons.Rounded.BarChart,
                onClick = { currentSubScreen = SettingsSubScreen.ABOUT },
            ),
            SearchableSettingItem(
                title = "About Dhvani Music",
                subtitle = "Developer credits, GitHub repository, and Telegram community",
                category = "About & Updates",
                icon = Icons.Rounded.Info,
                onClick = { currentSubScreen = SettingsSubScreen.ABOUT },
            ),
        )
    }

    val filteredItems = remember(searchQuery, searchableItems) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) emptyList()
        else searchableItems.filter {
            it.title.lowercase().contains(q) ||
                it.subtitle?.lowercase()?.contains(q) == true ||
                it.category.lowercase().contains(q)
        }
    }

    val mainScrollState = rememberScrollState()
    val subScrollState = rememberScrollState()

    // Ensure sub-screens always open at the top (offset 0) instead of inheriting main scroll position
    LaunchedEffect(currentSubScreen) {
        if (currentSubScreen != null) {
            subScrollState.scrollTo(0)
        }
    }

    if (currentSubScreen == null) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .verticalScroll(mainScrollState)
                .padding(contentPadding),
        ) {
            Spacer(Modifier.height(4.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        "Search settings...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    unfocusedBorderColor = Color.Transparent,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
            )

            if (searchQuery.isNotBlank()) {
                if (filteredItems.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp, horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(44.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "No settings found for \"$searchQuery\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    MeldSettingsGroup(
                        title = "Search Results (${filteredItems.size})",
                        items = filteredItems.map { item ->
                            MeldSettingsItemData(
                                icon = item.icon,
                                title = item.title,
                                subtitle = item.subtitle,
                                badge = item.category,
                                onClick = {
                                    searchQuery = ""
                                    item.onClick()
                                },
                            )
                        },
                    )
                }
            } else {
                Spacer(Modifier.height(8.dp))

                // 1. Appearance & Theming
                MeldSettingsGroup(
                    title = "Appearance & Interface",
                    items = listOf(
                        MeldSettingsItemData(
                            icon = Icons.Rounded.Palette,
                            title = stringResource(R.string.appearance),
                            subtitle = "Theme mode, dynamic colors, player UI, artwork",
                            onClick = onOpenAppearance,
                        ),
                    ),
                )

                Spacer(Modifier.height(12.dp))

                // 2. Playback & Sound
                MeldSettingsGroup(
                    title = "Playback & Sound",
                    items = listOf(
                        MeldSettingsItemData(
                            icon = Icons.Rounded.PlayArrow,
                            title = "Playback and audio",
                            subtitle = "Streaming quality, automix, crossfade, equalizer",
                            onClick = { currentSubScreen = SettingsSubScreen.PLAYBACK_AUDIO },
                        ),
                    ),
                )

                Spacer(Modifier.height(12.dp))

                // 3. Lyrics & Content
                MeldSettingsGroup(
                    title = "Lyrics & Content",
                    items = listOf(
                        MeldSettingsItemData(
                            icon = Icons.AutoMirrored.Rounded.Notes,
                            title = "Lyrics and content",
                            subtitle = "App language, synced lyrics, sources, video-to-audio",
                            onClick = { currentSubScreen = SettingsSubScreen.LYRICS_CONTENT },
                        ),
                    ),
                )

                Spacer(Modifier.height(12.dp))

                // 4. Downloads & Storage
                MeldSettingsGroup(
                    title = "Downloads & Storage",
                    items = listOf(
                        MeldSettingsItemData(
                            icon = Icons.Rounded.Download,
                            title = "Downloads and storage",
                            subtitle = "Download quality, network policy, cache size & cleanup",
                            onClick = { currentSubScreen = SettingsSubScreen.DOWNLOADS_STORAGE },
                        ),
                    ),
                )

                Spacer(Modifier.height(12.dp))

                // 5. Accounts & Integrations
                MeldSettingsGroup(
                    title = "Accounts & Integrations",
                    items = listOf(
                        MeldSettingsItemData(
                            icon = Icons.Rounded.Person,
                            title = "Accounts and integrations",
                            subtitle = if (signedIn) "Signed in • Scrobbling & Listen Together" else "Sign in • Last.fm, ListenBrainz, Discord",
                            onClick = { currentSubScreen = SettingsSubScreen.ACCOUNTS_INTEGRATIONS },
                        ),
                    ),
                )

                Spacer(Modifier.height(12.dp))

                // 6. Car & External Devices
                MeldSettingsGroup(
                    title = "Car & External Devices",
                    items = listOf(
                        MeldSettingsItemData(
                            icon = Icons.Rounded.DirectionsCar,
                            title = "Android Auto & Bluetooth",
                            subtitle = "In-car playback, auto-resume on connect",
                            onClick = { currentSubScreen = SettingsSubScreen.CAR_DEVICES },
                        ),
                    ),
                )

                Spacer(Modifier.height(12.dp))

                // 7. Backup & Restore
                MeldSettingsGroup(
                    title = "Backup & Data",
                    items = listOf(
                        MeldSettingsItemData(
                            icon = Icons.Rounded.Cloud,
                            title = "Backup and restore",
                            subtitle = "Export or restore settings and listening history",
                            onClick = { currentSubScreen = SettingsSubScreen.BACKUP_RESTORE },
                        ),
                    ),
                )

                Spacer(Modifier.height(12.dp))

                // 8. About & Updates
                MeldSettingsGroup(
                    title = "About & Updates",
                    items = listOf(
                        MeldSettingsItemData(
                            icon = Icons.Rounded.SystemUpdate,
                            title = "Check for updates",
                            subtitle = "Installed version v$version",
                            onClick = onCheckForUpdates,
                        ),
                        MeldSettingsItemData(
                            icon = Icons.AutoMirrored.Rounded.Article,
                            title = "Changelog",
                            subtitle = "Release notes & what's new",
                            onClick = { currentSubScreen = SettingsSubScreen.CHANGELOG },
                        ),
                        MeldSettingsItemData(
                            icon = Icons.Rounded.Info,
                            title = "About Dhvani Music",
                            subtitle = "Version, developer, community & licenses",
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
                        withLink(LinkAnnotation.Url("https://github.com/Kanaiya-rgb/Dhvani-Music", linkStyles)) {
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
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .verticalScroll(subScrollState)
                .padding(contentPadding),
        ) {
            Spacer(Modifier.height(4.dp))

            when (currentSubScreen!!) {
                SettingsSubScreen.PLAYBACK_AUDIO -> {
                    SettingsGroup(header = "Audio sources & quality") {
                        SettingsRow(
                            icon = Icons.Rounded.Storage,
                            title = "Audio sources & priority order",
                            subtitle = if (moduleEnabled) "Active: Modular FLAC/ALAC plugins, JioSaavn & YouTube fallback" else "Configure stream sources & modular lossless plugins",
                            trailing = { Chevron() },
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
                    }

                    SettingsGroup(header = "Transitions & Automix") {
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
                            onClick = { openEqualizer(context, sessionId) },
                        )
                        RowDivider()
                        SettingsRow(
                            icon = Icons.Rounded.Headphones,
                            title = "Listening mode",
                            subtitle = audioListeningMode.subtitle,
                            value = audioListeningMode.label,
                            trailing = { Chevron() },
                            onClick = { showListeningModeDialog = true },
                        )
                        RowDivider()
                        val hasDolby = remember(context) { DolbyUtils.isDolbyAtmosAvailable(context) }
                        val hasDolbyPanel = remember(context) { DolbyUtils.isDolbyPanelAvailable(context) }

                        // 1. 3D Spatial Audio (Works on all devices via in-app DSP)
                        SettingsRow(
                            icon = Icons.Rounded.SurroundSound,
                            title = "3D Spatial Audio",
                            subtitle = if (spatialAudio) "Active: 3D surround soundstage enabled" else "Expands soundstage into virtual 3D surround for all headphones",
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
                            onClick = {
                                AppSettings.setSpatialAudio(!spatialAudio)
                            },
                        )
                        RowDivider()

                        // 2. Hardware Dolby Atmos (Disabled on phones that lack Dolby hardware)
                        SettingsRow(
                            icon = Icons.Rounded.SurroundSound,
                            title = "Dolby Atmos",
                            badge = if (!hasDolby) "Not Supported" else null,
                            subtitle = if (hasDolby) {
                                if (dolbyAtmosEnabled) "Active: Hardware Dolby Atmos acoustic processing enabled"
                                else "Enable hardware Dolby Atmos acoustic enhancement"
                            } else {
                                "Hardware Dolby Atmos is not supported on this phone. Use in-app 3D Spatial Audio (above) for 3D surround sound on any headphones."
                            },
                            enabled = hasDolby,
                            trailing = {
                                Switch(
                                    checked = hasDolby && dolbyAtmosEnabled,
                                    enabled = hasDolby,
                                    onCheckedChange = { if (hasDolby) AppSettings.setDolbyAtmosEnabled(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        checkedBorderColor = MaterialTheme.colorScheme.primary,
                                    ),
                                )
                            },
                            onClick = if (hasDolby) {
                                { AppSettings.setDolbyAtmosEnabled(!dolbyAtmosEnabled) }
                            } else null,
                        )
                        RowDivider()

                        // 3. System Dolby Atmos panel
                        SettingsRow(
                            icon = Icons.Rounded.SurroundSound,
                            title = "System Dolby Atmos panel",
                            badge = if (!hasDolbyPanel) "Not Supported" else null,
                            subtitle = if (hasDolbyPanel) {
                                "Launch device hardware Dolby Atmos or acoustic settings"
                            } else {
                                "System Dolby Atmos control panel is not installed on this phone."
                            },
                            enabled = hasDolbyPanel,
                            trailing = if (hasDolbyPanel) { { Chevron() } } else null,
                            onClick = if (hasDolbyPanel) { { DolbyUtils.openDolbyAtmos(context) } } else null,
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

                    SettingsGroup(header = "Playback behavior") {
                        SettingsRow(
                            icon = Icons.AutoMirrored.Rounded.PlaylistPlay,
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

                SettingsSubScreen.LYRICS_CONTENT -> {
                    SettingsGroup(header = stringResource(R.string.language)) {
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
                            title = stringResource(R.string.lyrics_sources),
                            subtitle = lyricsSources.sortedBy { it.ordinal }.joinToString(", ") { it.label },
                            trailing = { Chevron() },
                            onClick = onLyricsSources,
                        )
                        RowDivider()
                        SettingsRow(
                            icon = Icons.Rounded.Translate,
                            title = stringResource(R.string.translation_language),
                            subtitle = if (translationLanguage.isBlank()) {
                                stringResource(R.string.translation_language_subtitle)
                            } else {
                                com.music.dhvani.data.lyrics.translationLanguageName(
                                    translationLanguage,
                                    androidx.appcompat.app.AppCompatDelegate.getApplicationLocales().get(0) ?: java.util.Locale.getDefault(),
                                )
                            },
                            trailing = { Chevron() },
                            onClick = onTranslationLanguage,
                        )
                    }

                    if (syncedLyrics) {
                        SettingsGroup(header = "Typography & Kinetic Animation") {
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
                                    LyricsAnimationStyle.SLIDE -> stringResource(R.string.lyrics_animation_slide)
                                    LyricsAnimationStyle.APPLE -> stringResource(R.string.lyrics_animation_apple)
                                    LyricsAnimationStyle.TYPEWRITER -> stringResource(R.string.lyrics_animation_typewriter)
                                    LyricsAnimationStyle.NEON -> stringResource(R.string.lyrics_animation_neon)
                                    LyricsAnimationStyle.GLITCH -> stringResource(R.string.lyrics_animation_glitch)
                                    LyricsAnimationStyle.LIQUID -> stringResource(R.string.lyrics_animation_liquid)
                                    LyricsAnimationStyle.AURORA -> stringResource(R.string.lyrics_animation_aurora)
                                    LyricsAnimationStyle.EMBER -> stringResource(R.string.lyrics_animation_ember)
                                    LyricsAnimationStyle.CHROME -> stringResource(R.string.lyrics_animation_chrome)
                                    LyricsAnimationStyle.CRT -> stringResource(R.string.lyrics_animation_crt)
                                    LyricsAnimationStyle.WAVE -> stringResource(R.string.lyrics_animation_wave)
                                    LyricsAnimationStyle.SMOKE_SIGNAL -> stringResource(R.string.lyrics_animation_smoke_signal)
                                    LyricsAnimationStyle.EQUALIZER -> stringResource(R.string.lyrics_animation_equalizer)
                                    LyricsAnimationStyle.GHOSTWRITE -> stringResource(R.string.lyrics_animation_ghostwrite)
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
                        }

                        SettingsGroup(header = "Lyrics Playback Behavior") {
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

                SettingsSubScreen.DOWNLOADS_STORAGE -> {
                    SettingsGroup(header = "Download preferences") {
                        SettingsRow(
                            icon = Icons.Rounded.Download,
                            title = stringResource(R.string.download_quality),
                            value = downloadQuality.localizedLabel(),
                            onClick = { pickingDownloadQuality = true },
                        )
                        RowDivider()
                        SettingsRow(
                            icon = when (downloadNetwork) {
                                DownloadNetwork.BOTH -> Icons.Rounded.CloudDownload
                                DownloadNetwork.WIFI_ONLY -> Icons.Rounded.Wifi
                                DownloadNetwork.CELLULAR_ONLY -> Icons.Rounded.SignalCellularAlt
                            },
                            title = stringResource(R.string.download_network_title),
                            value = downloadNetwork.localizedLabel(),
                            subtitle = stringResource(R.string.blocking).takeIf { !AppSettings.downloadsAllowedNow && metered == true },
                            onClick = { pickingDownloadNetwork = true },
                        )
                        RowDivider()
                        SettingsRow(
                            icon = Icons.AutoMirrored.Rounded.HelpOutline,
                            title = "Ask quality before download",
                            subtitle = "Prompt to select audio quality and network policy every time you download a song",
                            trailing = {
                                Switch(
                                    checked = alwaysAskDownloadOptions,
                                    onCheckedChange = AppSettings::setAlwaysAskDownloadOptions,
                                    colors = SwitchDefaults.colors(
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        checkedBorderColor = MaterialTheme.colorScheme.primary,
                                    ),
                                )
                            },
                            onClick = { AppSettings.setAlwaysAskDownloadOptions(!alwaysAskDownloadOptions) },
                        )
                    }

                    val mb = (cacheLimitBytes / (1024L * 1024L)).toInt()
                    val warning = mb >= CACHE_WARNING_MB

                    SettingsGroup(
                        header = stringResource(R.string.storage),
                        footer = "Downloaded audio kept on disk for instant seeking, offline playback and replays",
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
                }

                SettingsSubScreen.ACCOUNTS_INTEGRATIONS -> {
                    AccountCard(
                        signedIn = signedIn,
                        account = account,
                        onSignIn = onSignIn,
                    )

                    if (signedIn) {
                        SettingsGroup {
                            DestructiveRow(label = "Sign out", onClick = onSignOut)
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    SettingsGroup(header = "Connected Services") {
                        SettingsRow(
                            icon = Icons.Rounded.Person,
                            title = stringResource(R.string.account_integrations),
                            subtitle = "Manage Listen Together, Discord RPC and scrobbling hub",
                            trailing = { Chevron() },
                            onClick = onAccountScrobbling,
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

                SettingsSubScreen.CAR_DEVICES -> {
                    SettingsGroup(header = "Bluetooth") {
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
                    }

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

                SettingsSubScreen.BACKUP_RESTORE -> {
                    SettingsGroup(
                        header = "Backup and restore (Unified JSON)",
                        footer = "Exports your complete settings and listening history into a single unified JSON backup file.",
                    ) {
                        SettingsRow(
                            icon = Icons.Rounded.FileUpload,
                            title = stringResource(R.string.export_data),
                            subtitle = exportStatus ?: "Export settings and listening history as a unified JSON file",
                            onClick = { exportPicker.launch(Backup.suggestedName()) },
                        )
                        RowDivider()
                        SettingsRow(
                            icon = Icons.Rounded.FileDownload,
                            title = stringResource(R.string.import_data),
                            subtitle = importStatus ?: "Restore settings and listening history from a JSON backup file",
                            onClick = { confirmImport = true },
                        )
                    }
                }

                SettingsSubScreen.CHANGELOG -> {
                    val releases = changelogReleases.ifEmpty { ChangelogRepository.FALLBACK_RELEASES }
                    val latestRelease = releases.firstOrNull { it.isLatest } ?: releases.first()
                    val pastReleases = releases.filter { it != latestRelease }

                    // Header row with Live status & Manual Refresh button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isChangelogLoading) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary),
                            )
                            Text(
                                text = if (isChangelogLoading) "Fetching from GitHub..." else "Live from GitHub Releases",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        IconButton(
                            onClick = {
                                changelogScope.launch {
                                    ChangelogRepository.refresh(force = true)
                                }
                            },
                            enabled = !isChangelogLoading,
                            modifier = Modifier.size(36.dp),
                        ) {
                            if (isChangelogLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = "Refresh from GitHub",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }

                    if (isChangelogLoading) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .height(2.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        )
                        Spacer(Modifier.height(4.dp))
                    }

                    // 1. Top Featured Latest Release
                    SettingsGroup(header = "Latest Release") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "Dhvani Music ${latestRelease.version}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                ) {
                                    Text(
                                        text = "LATEST",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${latestRelease.summary} • ${latestRelease.date}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(14.dp))
                            latestRelease.items.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.Top,
                                ) {
                                    Text(
                                        text = "•",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(end = 8.dp),
                                    )
                                    Text(
                                        text = item,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }

                    if (pastReleases.isNotEmpty()) {
                        // 2. Previous Releases History
                        SettingsGroup(header = "Previous Releases") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                            ) {
                                pastReleases.forEachIndexed { index, release ->
                                    if (index > 0) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 14.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            text = "Dhvani Music ${release.version}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            text = release.date,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = release.summary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    release.items.forEach { item ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 3.dp),
                                            verticalAlignment = Alignment.Top,
                                        ) {
                                            Text(
                                                text = "•",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(end = 8.dp),
                                            )
                                            Text(
                                                text = item,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
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
                                painter = painterResource(R.drawable.app_logo),
                                contentDescription = "Dhvani Music",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(20.dp)),
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

                    // Developer & Creator Card
                    SettingsGroup(header = "Developer & Creator") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .border(
                                            width = 2.dp,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                            shape = CircleShape,
                                        )
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    AsyncImage(
                                        model = "https://avatars.githubusercontent.com/u/176733324?v=4",
                                        contentDescription = "Kanaiya Soni",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }

                                Spacer(Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Kanaiya Soni",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 2.dp),
                                    ) {
                                        Text(
                                            text = "Creator & Lead Developer",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        text = "@Kanaiya-rgb",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            Spacer(Modifier.height(14.dp))

                            Text(
                                text = "Passionate indie developer building Dhvani Music — dedicated to crafting high-fidelity audio, aesthetic futuristic UI, and a private, ad-free streaming experience.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 20.sp,
                            )

                            Spacer(Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                // GitHub Chip Button
                                Surface(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, "https://github.com/Kanaiya-rgb".toUri()).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        runCatching { context.startActivity(intent) }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Person,
                                            contentDescription = "GitHub",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "GitHub",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.size(12.dp),
                                        )
                                    }
                                }

                                // Telegram Channel Chip Button
                                Surface(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, "https://t.me/DhvaniMusicApp".toUri()).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        runCatching { context.startActivity(intent) }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Link,
                                            contentDescription = "Telegram",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "Telegram",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.size(12.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    SettingsGroup(header = "Updates & Notifications") {
                        SettingsRow(
                            icon = Icons.Rounded.SystemUpdate,
                            title = "Check for updates",
                            subtitle = "Installed version: v$version",
                            onClick = onCheckForUpdates,
                        )
                        RowDivider()
                        SettingsRow(
                            icon = Icons.Rounded.Notifications,
                            title = "Notifications & Update Alerts",
                            subtitle = if (hasNotificationPermission) {
                                "Allowed — you'll get instant alerts for app updates & downloads"
                            } else {
                                "Not allowed — tap to enable alerts for new updates and playback"
                            },
                            trailing = {
                                if (hasNotificationPermission) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "Allowed",
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.ChevronRight,
                                        contentDescription = "Enable",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            },
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    if (!hasNotificationPermission) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        runCatching {
                                            val intent = Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                                putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName)
                                            }
                                            context.startActivity(intent)
                                        }
                                    }
                                } else {
                                    runCatching {
                                        val intent = Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                            putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName)
                                        }
                                        context.startActivity(intent)
                                    }
                                }
                            },
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    SettingsGroup(header = "Diagnostics & System") {
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
                        RowDivider()
                        SettingsRow(
                            icon = Icons.Rounded.Link,
                            title = "Open supported links",
                            subtitle = "Manage default URL links handling for YouTube Music links",
                            onClick = { openSupportedLinks(context) },
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    SettingsGroup(header = "Links & Community") {
                        SettingsRow(
                            icon = Icons.Rounded.Link,
                            title = "GitHub Repository",
                            subtitle = "https://github.com/Kanaiya-rgb/Dhvani-Music",
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, "https://github.com/Kanaiya-rgb/Dhvani-Music".toUri()).apply {
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

    if (showListeningModeDialog) {
        AlertDialog(
            onDismissRequest = { showListeningModeDialog = false },
            icon = {
                Icon(
                    imageVector = when (audioListeningMode) {
                        AudioListeningMode.BOTH, AudioListeningMode.DOLBY_ATMOS -> Icons.Rounded.SurroundSound
                        AudioListeningMode.LOSSLESS -> Icons.Rounded.GraphicEq
                        AudioListeningMode.STANDARD -> Icons.Rounded.Headphones
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            title = {
                Text(
                    text = "Listening Mode",
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val hasDolby = remember(context) { DolbyUtils.isDolbyAtmosAvailable(context) }
                    AudioListeningMode.entries.forEach { mode ->
                        val requiresDolby = mode == AudioListeningMode.DOLBY_ATMOS || mode == AudioListeningMode.BOTH
                        val isSupported = !requiresDolby || hasDolby
                        val isSelected = mode == audioListeningMode && isSupported
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                    else Color.Transparent
                                )
                                .then(
                                    if (isSupported) {
                                        Modifier.clickable {
                                            AppSettings.applyListeningMode(mode)
                                            showListeningModeDialog = false
                                        }
                                    } else Modifier
                                )
                                .alpha(if (isSupported) 1f else 0.4f)
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = mode.label,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    )
                                    if (!isSupported) {
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = "Not Supported",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error,
                                            modifier = Modifier
                                                .background(
                                                    MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = if (!isSupported) "Hardware Dolby Atmos is not supported on this phone" else mode.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                TextButton(onClick = { showListeningModeDialog = false }) {
                    Text("Close")
                }
            },
        )
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

    if (pickingDownloadNetwork) {
        ModalBottomSheet(
            onDismissRequest = { pickingDownloadNetwork = false },
            containerColor = MaterialTheme.colorScheme.background,
        ) {
            DownloadNetworkSheet(
                selected = downloadNetwork,
                onSelect = { network ->
                    AppSettings.setDownloadNetwork(network)
                    pickingDownloadNetwork = false
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = anim.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (lyricsAnimationStyle == anim) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = when (anim) {
                                        LyricsAnimationStyle.NONE -> "Static plain lyrics without animations"
                                        LyricsAnimationStyle.FADE -> "Gentle classic opacity fade"
                                        LyricsAnimationStyle.SLIDE -> "Smooth dynamic horizontal slide-in"
                                        LyricsAnimationStyle.APPLE -> "Apple Music style energetic syllable bounce"
                                        LyricsAnimationStyle.TYPEWRITER -> "Mechanical character-by-character typing with caret"
                                        LyricsAnimationStyle.NEON -> "Electric gas neon sign with buzzing tube flicker & deep glow"
                                        LyricsAnimationStyle.GLITCH -> "Torn digital video slices with cyan & magenta channel shift"
                                        LyricsAnimationStyle.LIQUID -> "Hollow glass typography with rising fluid wave meniscus"
                                        LyricsAnimationStyle.AURORA -> "Living liquid holographic rainbow with shifting hue rotation"
                                        LyricsAnimationStyle.EMBER -> "Volcanic incandescent magma with molten embers & heat pulse"
                                        LyricsAnimationStyle.CHROME -> "Liquid metallic mercury with sweeping specular lens glare"
                                        LyricsAnimationStyle.CRT -> "Retro green phosphor monitor with rolling TV scanlines & flicker"
                                        LyricsAnimationStyle.WAVE -> "Letters rhythmically dancing up and down in a fluid sine wave"
                                        LyricsAnimationStyle.SMOKE_SIGNAL -> "Smoldering amber lyrics with rising smoke vapor plumes"
                                        LyricsAnimationStyle.EQUALIZER -> "Kinetic 5-band audio visualizer frequency bars on words"
                                        LyricsAnimationStyle.GHOSTWRITE -> "Phantom spectral mist with trailing spirit echoes"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
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
}

private enum class SettingsSubScreen(val title: String) {
    PLAYBACK_AUDIO("Playback and audio"),
    LYRICS_CONTENT("Lyrics and content"),
    DOWNLOADS_STORAGE("Downloads and storage"),
    ACCOUNTS_INTEGRATIONS("Accounts and integrations"),
    CAR_DEVICES("Car and external devices"),
    BACKUP_RESTORE("Backup and restore"),
    CHANGELOG("Changelog"),
    ABOUT("About"),
}

private data class SearchableSettingItem(
    val title: String,
    val subtitle: String? = null,
    val category: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)



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
    var launched = false
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        try {
            val intent = Intent(
                AndroidSettings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,
                "package:${context.packageName}".toUri(),
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                launched = true
            }
        } catch (_: Exception) {}
    }

    if (!launched) {
        try {
            val intent = Intent(
                AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS,
                "package:${context.packageName}".toUri(),
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            launched = true
        } catch (_: Exception) {}
    }

    if (!launched) {
        try {
            val intent = Intent(AndroidSettings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            launched = true
        } catch (_: Exception) {}
    }

    if (!launched) {
        Toast.makeText(context, "Could not open settings", Toast.LENGTH_SHORT).show()
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
        AudioQuality.LOSSLESS -> R.string.lossless
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

private fun isDolbyAtmosPanelAvailable(context: Context): Boolean = DolbyUtils.isDolbyPanelAvailable(context)
private fun openDolbyAtmos(context: Context) = DolbyUtils.openDolbyAtmos(context)

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

@Composable
private fun DownloadNetwork.localizedLabel(): String = stringResource(
    when (this) {
        DownloadNetwork.BOTH -> R.string.download_network_both
        DownloadNetwork.WIFI_ONLY -> R.string.download_network_wifi_only
        DownloadNetwork.CELLULAR_ONLY -> R.string.download_network_cellular_only
    },
)

@Composable
private fun DownloadNetwork.localizedDesc(): String = stringResource(
    when (this) {
        DownloadNetwork.BOTH -> R.string.download_network_both_desc
        DownloadNetwork.WIFI_ONLY -> R.string.download_network_wifi_only_desc
        DownloadNetwork.CELLULAR_ONLY -> R.string.download_network_cellular_only_desc
    },
)

@Composable
private fun DownloadNetworkSheet(
    selected: DownloadNetwork,
    onSelect: (DownloadNetwork) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        Row(
            modifier = Modifier.padding(start = 22.dp, end = 22.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.CloudDownload,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = stringResource(R.string.download_network_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Choose connection for song downloads",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)

        DownloadNetwork.entries.forEach { option ->
            val chosen = option == selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelect(option)
                    }
                    .padding(horizontal = 22.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = option.localizedLabel(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = option.localizedDesc(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (chosen) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
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
                    maxLines = 10,
                    softWrap = true,
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
