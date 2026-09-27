# 🗺️ Dhvani Music - Project Structure & Codebase Guide

This comprehensive guide serves as an architectural index to help developers and open-source contributors navigate, understand, and modify the Dhvani Music codebase efficiently.

---

## 📁 1. Project Root Directory
The root directory maintains a clean and minimal footprint containing only essential build, configuration, and documentation assets:

| Directory / File | Description |
|---|---|
| **`app/`** | Primary Android application module (UI, Playback engine, Data, Screens, Room DB). |
| **`spotify/`** | Spotify Canvas & vertical motion video loop resolver microservice. |
| **`dashboard/`** | Real-time administrative telemetry and analytics console (Firebase/Web). |
| **`apk/`** | Pre-built release binaries and distribution packages (`v2.1.1`, `v2.4.0`, etc.). |
| **`build.gradle.kts` & `settings.gradle.kts`** | Top-level project build logic and Gradle dependency catalog. |
| **`gradle.properties` & `local.properties`** | Build daemon flags, JVM memory allocations, and Android SDK path pointers. |
| **`version.json`** | In-app auto-update distribution manifest (Version, Changelog, Direct APK download URL). |
| **`README.md` & `RELEASE_v2.4.0_CHANGELOG.md`** | Project overview, feature showcases, release notes, and documentation. |
| **`CONTRIBUTING.md`** | Contribution guidelines, coding standards, branch rules, and pull request procedures. |
| **`AGENTS.md`** | AI agent directives, development rules, and codebase mapping constraints. |

---

## 📱 2. Main App Module (`app/src/main/java/com/music/dhvani/`)

The application follows single-activity architecture, clean separation of concerns, and reactive state management:

### 🌟 App Entry Point
- **`MainActivity.kt`**: Single Activity container, Jetpack Compose root navigation host, modal sheet coordination, system intent parsing, and bottom bar management.
- **`DhvaniApplication.kt`**: Application subclass initializing crash reporting, logging, background notification channels, and disk cache systems.

---

### 🎨 A. UI Screens (`ui/screens/`)
*Dedicated top-level views and full-page destinations:*

- **`HomeScreen.kt`**: Algorithmic feed, Quick Picks, personalized carousels, and trending audio rails.
- **`SearchScreen.kt`**: Instant search bar with real-time autocompletion, search history, and voice input.
- **`LibraryScreen.kt`**: User playlists, liked songs, offline storage browser, and downloads manager.
- **`CategoryScreen.kt`**: Explore page covering 50+ curated genres, moods, moments, charts, and podcast jukeboxes.
- **`DetailScreen.kt`**: Universal detail view for Albums, Playlists, and Artist profiles.
- **`LocalMusicScreen.kt`**: High-performance local device storage audio scanner and tag-based browser.
- **`SourcesScreen.kt`**: Streaming source resolver selector (YouTube Music, JioSaavn, Extensible modules).
- **`HistoryScreen.kt`**: Chronological playback audit log with clear/filter options.
- **`ListenTogetherScreen.kt`**: Real-time synchronized peer-to-peer and room-based group listening.
- **`DiscordScreen.kt`**: Discord Rich Presence (RPC) configuration, account linking, and live status preview.
- **`AccountAndScrobblingScreen.kt`**: Scrobbling service integration (Last.fm & ListenBrainz).
- **`AppearanceSettingsScreen.kt`**: Dark/Light modes, Pure Black AMOLED, dynamic HSL accent colors, typography, and seekbar style picker.
- **`SettingsSheet.kt`**: Central application settings bottom sheet categorized by subsystem.
- **`SongDetailsSheet.kt`**: Real-time stream telemetry sheet displaying audio format, bit depth, sample rate, codec, and network cache status.

---

### 🎵 B. Player & Visuals (`ui/player/`)
*Audio player interface and animated backdrop renderers:*

- **`NowPlayingScreen.kt`**: Fullscreen player with rotating vinyl turntable artwork, gesture controls, and queue drawer.
- **`CanvasArtworkPlayer.kt`**: Seamless vertical video looping canvas player (Spotify, Apple Music, and Tidal motion covers).
- **`MeshGradient.kt`**: Multi-point dynamic fluid mesh gradient background that adapts dynamically to track cover art.
- **`ThinSlider.kt`**: Precision scrub track seekbar with micro-interaction haptic feedback.
- **`LyricsViews.kt`**: Syllable-level synchronized word-by-word karaoke typography, translation toggle, and romanization display.

---

### 🧩 C. Core Components & Dialogs (`ui/components/`)
*Reusable UI components, sheets, and interactive overlays:*

- **`MiniPlayer.kt`**: Expandable persistent bottom audio bar with swipe-to-skip gestures.
- **`DynamicIslandPlayer.kt`**: Multitasking floating notch/pill overlay supporting both in-app and system-level display.
- **`PlayerSliders.kt`**: 12+ interactive seekbar physics styles (Wavy, Squiggly, Neon, Gradient, Cosmic, Lava, Audio Bars, Dot Matrix, Vinyl, Cyber Beam).
- **`SliderStyleDialog.kt`**: Visual modal selector allowing users to preview and choose seekbar aesthetics.
- **`EqualizerSheet.kt`**: 10-band studio graphic equalizer with Bass Boost, Virtualizer, and custom sound presets.
- **`AudioPipelineDialog.kt`**: Audio pipeline configurator (Spatial 3D audio, audio offload, and 32-bit float output).
- **`UpdateAvailableDialog.kt`**: Non-blocking in-app APK update notification dialog with one-click download.
- **`LyricsSourcesDialog.kt`**: Multi-provider lyrics prioritization dialog (LrcLib, Genius, YouTube, Musixmatch).
- **`SongActionsSheet.kt`**: Contextual track actions (Add to playlist, download, share, view artist).
- **`PlaylistPickerSheet.kt`**: Bottom sheet for adding tracks to existing or new playlists.
- **`MediaOutputDialog.kt`**: Audio output destination router (Bluetooth devices, phone speaker, Cast).
- **`FrostedTopBar.kt`**: iOS-style translucent frosted-glass app bar with real-time backdrop blur.

---

### 🔊 D. Audio Playback Subsystem (`playback/`)
*AndroidX Media3, ExoPlayer, and background service lifecycle:*

- **`PlaybackService.kt`**: Foreground `MediaSessionService` coordinating notification media controls, lockscreen sessions, and automated background termination when swiped from recents.
- **`PlayerConnection.kt`**: Clean architectural bridge linking UI/ViewModel state directly to `PlaybackService` via coroutine `StateFlow`.
- **`AudioCache.kt`**: Zero-latency LRU disk cache engine ensuring instant track seek times and low cellular data usage.
- **`CrossfadeController.kt`**: Smooth DJ-grade volume crossfading and gapless track transitions.
- **`LockscreenWallpaperManager.kt`**: High-resolution blurred artwork renderer for the device lockscreen.
- **`QueueBuilder.kt` & `QueueShuffle.kt`**: Queue management and Fisher-Yates smart shuffle algorithms.
- **`SleepTimer.kt`**: Configurable sleep timer with progressive audio fade-out.
- **`SpatialAudioProcessor.kt`**: 3D spatial surround sound virtualization and headphone enhancement.
- **`DynamicIslandOverlayManager.kt`**: System window overlay coordinator for floating Dynamic Island controls.

---

### 🌐 E. Data, Networking & Metadata Engines (`data/`)
*API communication, media parsing, and persistence:*

- **`innertube/`**: YouTube Music InnerTube client, stream decryption pipeline, and response parsers.
  - `StreamResolver.kt`: Stream URL resolution extracting highest available bitrates (160kbps Opus / 256kbps AAC / Hi-Res Lossless).
  - `Innertube.kt` & `InnertubeParser.kt`: Search query handling, browse carousels, and artist disco parsing.
- **`sources/`**: Pluggable multi-source audio stream engine (YouTube Music, JioSaavn, extensible user modules).
- **`lyrics/`**: Multi-provider synchronized lyrics orchestrator:
  - `LyricsRepository.kt`: Coordinates fallback fetching across all integrated providers.
  - `LrcLib.kt`: LRCLIB REST client for accurate syllable-level synchronization.
  - `YouTubeLyrics.kt`: YouTube Music timed text and subtitle extractor.
  - `Genius.kt` & `Musixmatch.kt`: Fallback plain text and synchronized lyrics scrapers.
  - `LyricsTranslation.kt`: Multi-language automated lyric translation engine.
- **`canvas/`**: Video canvas motion artwork resolvers (`SpotifyCanvas`, `AppleMusicCanvas`, `TidalCanvas`).
- **`settings/AppSettings.kt`**: Centralized, reactive DataStore repository preserving user configurations.
- **`download/`**: Background audio file downloader and offline metadata tag embedder (FLAC/M4A/MP3).
- **`history/`**: Playback history repository backed by Room Database.

---

### 📱 F. Home-Screen Media Widgets (`widget/`)
- **`MediaWidgetPill.kt`**: 4×1 modern compact pill widget featuring album art, track details, and playback controls.
- **`MediaWidgetTurntable.kt`**: 3×3 vintage rotating vinyl turntable disc widget with real-time spin animation.

---

## 💡 Quick Reference Guide for Developers:
1. **Adjusting Theme or Accent Colors:** Refer to `ui/theme/Theme.kt` or `ui/screens/AppearanceSettingsScreen.kt`.
2. **Adding or Customizing Icons:** Refer to `ui/icons/DhvaniIcons.kt`.
3. **Modifying Seekbars / Sliders:** Refer to `ui/components/PlayerSliders.kt`.
4. **Updating the Fullscreen Player:** Refer to `ui/player/NowPlayingScreen.kt`.
5. **Tuning Streaming or Audio Resolution:** Refer to `data/innertube/StreamResolver.kt` or `playback/PlaybackService.kt`.
