<div align="center">

<br/>

<img src="Logo.png" alt="Flux Music Logo" width="160" />

# Flux Music

### *Your Music. Without Limits.*
**A sleek, open-source Android music streaming client powered by YouTube Music & JioSaavn.**

<br/>

[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white&labelColor=0d1117)](https://www.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white&labelColor=0d1117)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack-Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white&labelColor=0d1117)](https://developer.android.com/jetpack/compose)
[![Media3](https://img.shields.io/badge/Media3-ExoPlayer-E65100?style=for-the-badge&logo=googleplay&logoColor=white&labelColor=0d1117)](https://developer.android.com/media/media3)
[![Latest Release](https://img.shields.io/badge/Release-v1.8.0-22c55e?style=for-the-badge&labelColor=0d1117)](https://github.com/Kanaiya-rgb/Flux-Music/releases/latest)
[![License](https://img.shields.io/badge/License-GPLv3-ef4444?style=for-the-badge&labelColor=0d1117)](LICENSE)

<br/>

[📥 Download Latest APK](https://github.com/Kanaiya-rgb/Flux-Music/releases/latest) • [✨ What's New](#-whats-new-in-v180) • [🚀 Features](#-features) • [🔧 Build](#-build-from-source)

---

</div>

## 🌟 Overview

**Flux Music** is a lightweight, ad-free Android music streaming client designed for pure auditory and visual pleasure. Built entirely in Kotlin with modern Jetpack Compose and Material 3, it offers high-resolution audio, offline caching, time-synced lyrics, and rich personalization options.

---

## ✨ What's New in v1.8.0

- 🎧 **Resume on Bluetooth Connection**: Automatically resumes paused music when your Bluetooth headphones, earbuds, car audio, or wired headsets connect. Toggle it in *Settings ➔ Player & audio ➔ Playback behavior*.
- 🎚️ **Customizable Player Slider Styles**: Choose between 5 distinct slider designs with live interactive visual previews:
  - **Capsule**: Modern Apple Music-style expanding scrubber.
  - **Material**: Classic Material 3 slider with a circular thumb knob.
  - **Wavy**: Fluid harmonic animated sine wave.
  - **Squiggly**: Energetic animated squiggly waveform.
  - **Slim**: Minimalist 2dp hairline bar.
- ❤️ **Liked Music Playlist in Library**: Your favorite tracks are now organized in a dedicated, beautiful "Liked Music" playlist right inside the Library tab.
- ⚡ **Ultra-Smooth 120Hz Rendering**: Optimized Coil image caching (25% RAM ceiling, 250MB disk budget) and eliminated infinite blur loop redraws for 60/90/120Hz fluidity on all devices.
- 🎨 **Brand New 3D Waveform Logo**: Updated modern app icon across all launcher densities and notification panels.
- 📊 **Privacy-First Anonymous Analytics**: Optional real-time telemetry powered by Firebase (Spark plan) to track active listeners and top-played songs with zero personal data collection.

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
Download the latest APK from the [Releases Page](https://github.com/Kanaiya-rgb/Flux-Music/releases/latest):
* **[Flux-Music-v1.8.0.apk](https://github.com/Kanaiya-rgb/Flux-Music/releases/download/v1.8.0/Flux-Music-v1.8.0.apk)**

### Installation Steps
1. Download `Flux-Music-v1.8.0.apk`.
2. Open your Android device **Settings → Security → Install unknown apps** and enable permission for your browser / file manager.
3. Tap the downloaded APK to install.
4. Open **Flux Music** and enjoy ad-free music!

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
git clone https://github.com/Kanaiya-rgb/Flux-Music.git
cd Flux-Music

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

**Flux Music Community** • [GitHub Issues](https://github.com/Kanaiya-rgb/Flux-Music/issues) • [Releases](https://github.com/Kanaiya-rgb/Flux-Music/releases)

</div>
