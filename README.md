<div align="center">

<br/>

<img src="Logo.png" alt="Dhvani Music Logo" width="160" />

# Dhvani Music

### *Your Music. Without Limits.*
**A sleek, open-source Android music streaming client powered by YouTube Music & JioSaavn.**

<br/>

[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white&labelColor=0d1117)](https://www.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white&labelColor=0d1117)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack-Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white&labelColor=0d1117)](https://developer.android.com/jetpack/compose)
[![Media3](https://img.shields.io/badge/Media3-ExoPlayer-E65100?style=for-the-badge&logo=googleplay&logoColor=white&labelColor=0d1117)](https://developer.android.com/media/media3)
[![Latest Release](https://img.shields.io/badge/Release-v2.0.0-22c55e?style=for-the-badge&labelColor=0d1117)](https://github.com/Kanaiya-rgb/Dhvani-Music/releases/latest)
[![License](https://img.shields.io/badge/License-GPLv3-ef4444?style=for-the-badge&labelColor=0d1117)](LICENSE)

<br/>

[📥 Download Latest APK](https://github.com/Kanaiya-rgb/Dhvani-Music/releases/latest) • [✨ What's New](#-whats-new-in-v200) • [🚀 Features](#-features) • [🔧 Build](#-build-from-source)

---

</div>

## 🌟 Overview

**Dhvani Music** is a lightweight, ad-free Android music streaming client designed for pure auditory and visual pleasure. Built entirely in Kotlin with modern Jetpack Compose and Material 3, it offers high-resolution audio, offline caching, time-synced lyrics, and rich personalization options.

---

## ✨ What's New in v2.0.0

- 🏷️ **Rebranded to Dhvani Music**: Fresh new identity inspired by Indian musical heritage with modern aesthetics.
- 🎙️ **Voice Search Support**: Built-in Mic button on Search Bar to search your favorite tracks hands-free.
- 🕒 **Instant Search History**: Tap the search bar to view your search history instantly, with individual ✕ remove options.
- 🎛️ **8 Next-Gen Slider Styles**: Neon Glow, Gradient Flow, Cosmic Stars, Liquid Lava, Audio Visualizer, Retro LED Matrix, Vinyl Groove, and Cyber Beam.
- 🎨 **Artwork-Matched Dynamic Slider Color**: Sliders automatically match current playing track's artwork palette.
- 📤 **Playlist Export & Import**: Export your playlists to M3U or JSON format and share with friends.
- 🌟 **New Releases Home Rail**: Dedicated "New Releases" pill right after "All" on the Home Screen.

---

## 🚀 Key Features

### 🎵 High-Fidelity Playback
- **Lossless & High-Bitrate**: YouTube Music backend with multi-source fallback (JioSaavn, lossless modules).
- **Automix & Smart Fade**: Seamless AI beat-matched crossfade between consecutive tracks.
- **Audio Processing**: Custom parametric equalizer, system equalizer bridge, and spatial stereo expansion.
- **Background Playback**: MediaSessionService support with lockscreen controls, notification actions, and Android Auto integration.

### 🎨 Visuals & Customization
- **Spotify Canvas & Motion Art**: Looping video artwork behind full-bleed covers.
- **Synced Karaoke Lyrics**: Line-by-line and syllable-synced lyrics powered by LRCLIB, Musixmatch, and Spotify.
- **Dynamic Theming**: Artwork-derived palettes, pure AMOLED black mode, and customizable density scaling.
- **Full Bluetooth & Earphone Support**: Instant resume on connect and pause on disconnect (*becoming noisy*).

### 📥 Offline & Organization
- **Offline Downloads**: Save tracks locally with high-resolution album covers and embedded lyrics.
- **Smart Caching**: Automatic audio caching with configurable storage budget.
- **Library Management**: Liked songs, custom playlists, pinned items, and local device audio playback.

---

## 📦 Download & Installation

### Option 1: Direct GitHub Release (Recommended)
Download the latest APK from the [Releases Page](https://github.com/Kanaiya-rgb/Dhvani-Music/releases/latest):
* **[DhvaniMusic-v2.0.0.apk](https://github.com/Kanaiya-rgb/Dhvani-Music/releases/download/v2.0.0/DhvaniMusic-v2.0.0.apk)** (v2.0.0 — Pre-built APK)
* Also located in repository: [`apk/DhvaniMusic-v2.0.0.apk`](apk/DhvaniMusic-v2.0.0.apk)

### Installation Steps
1. Download `DhvaniMusic-v2.0.0.apk`.
2. Open your Android device **Settings → Security → Install unknown apps** and enable permission for your browser / file manager.
3. Tap the downloaded APK to install.
4. Open **Dhvani Music** and enjoy ad-free music!

---

## 🔧 Build from Source

### Prerequisites
| Tool | Minimum Version |
| :--- | :--- |
| **Android Studio** | Hedgehog 2023.1.1+ (or CLI) |
| **JDK** | Java 17+ |
| **Android SDK** | API 34+ (compileSdk 36) |
| **Kotlin** | 2.3+ |
| **CMake & NDK** | 3.22.1 / NDK 28+ |

### Compilation Commands

```bash
# Clone the repository
git clone https://github.com/Kanaiya-rgb/Dhvani-Music.git
cd Dhvani-Music

# Build Release APK
# On Windows (PowerShell):
.\gradlew.bat assembleProdRelease

# On Linux / macOS:
./gradlew assembleProdRelease
```

The output APK will be generated at:
```
app/build/outputs/apk/prod/release/app-prod-release-unsigned.apk
```

---

## 🛠 Tech Stack & Architecture

- **Language**: Kotlin 2.3
- **UI Toolkit**: Jetpack Compose, Material 3, Haze (blur/glassmorphism)
- **Audio Engine**: AndroidX Media3 (ExoPlayer), Native C++ DSP
- **Networking**: OkHttp, Kotlinx Serialization, NewPipe Extractor
- **Image Pipeline**: Coil 3, AndroidX Palette
- **Database & State**: StateFlow, SharedPreferences, EncryptedSharedPreferences
- **Build System**: Gradle 8, AGP 8.10.1, R8 8.13.23

---

## 📄 License

This project is licensed under the **GNU General Public License v3.0 (GPL-3.0)**. See the [LICENSE](LICENSE) file for more information.

---

<div align="center">

Made with ❤️ by [Kanaiya-rgb](https://github.com/Kanaiya-rgb)

**Flux Music Community** • [GitHub Issues](https://github.com/Kanaiya-rgb/Dhvani-Music/issues) • [Releases](https://github.com/Kanaiya-rgb/Dhvani-Music/releases)

</div>
