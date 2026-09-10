<div align="center">

# 🎵 Dhvani Music
### *Aesthetic Indian & Global Music Client*

[![Latest Release](https://img.shields.io/badge/Release-v2.0.0-22c55e?style=for-the-badge&labelColor=0d1117)](https://github.com/Kanaiya-rgb/Dhvani-Music/releases/latest)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white&labelColor=0d1117)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack-Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white&labelColor=0d1117)](https://developer.android.com/jetpack/compose)
[![Material 3](https://img.shields.io/badge/Material_3-Material_You-E879F9?style=for-the-badge&logo=google&logoColor=white&labelColor=0d1117)](https://m3.material.io)
[![Media3](https://img.shields.io/badge/Media3-ExoPlayer-E65100?style=for-the-badge&logo=googleplay&logoColor=white&labelColor=0d1117)](https://developer.android.com/media/media3)
[![License](https://img.shields.io/badge/License-GPLv3-ef4444?style=for-the-badge&labelColor=0d1117)](LICENSE)

<br/>

[📥 Download APK](#-download--installation) · [✨ Features](#-features) · [🎧 Categories](#-category--mood-explorer) · [⚙️ Setup](#-setup--optimization) · [❓ FAQ](#-frequently-asked-questions) · [🙏 Credits](#-credits--acknowledgements) · [⚖️ Disclaimer](#-disclaimer--legal-notice) · [📜 License](#-license)

</div>

---

> [!WARNING]
> **Dhvani Music is not affiliated with, endorsed by, or connected to YouTube, Google, Spotify, or Deezer in any way. Use it at your own discretion.**

---

## 📖 What is Dhvani Music?

**Dhvani Music** is an elegant, open-source Android music client that fuses YouTube Music's colossal audio catalogue with **pristine Hi-Res lossless audio**, deep **Indian musical heritage (Utsav Mode & Raags)**, and an expressive **Material 3 / Liquid Frosted-Glass aesthetic**.

Built entirely in modern Kotlin with Jetpack Compose, Dhvani provides an ad-free, clutter-free listening experience that honors your battery, your privacy, and your ears.

---

## ✨ Features

### 🎧 Playback & Audio Engineering
* **Vast YouTube Music Catalog** — Search, browse, and play anything available on YouTube Music with zero advertisements.
* **Hi-Res Lossless Audio** — Stream audiophile-grade FLAC/ALAC from configured module sources, seamlessly falling back to YouTube Music.
* **Gapless Playback with True Crossfade** — Smooth, configurable crossfades (0–12 seconds) so your music never drops a beat.
* **Automix [Beta]** — DJ-style smooth transitions powered by a native C++ DSP analyzer (`dhvani_analysis`) with beat-matching and tempo alignment.
* **Per-Network Quality Ceilings** — Set independent audio bitrates for Wi-Fi and mobile data to save bandwidth on cellular networks.
* **Parametric Equalizer** — Tailor frequencies with built-in presets (Bass Boost, Acoustic, Vocal Enhancer) or build custom curve profiles.
* **Background Playback** — Reliable background audio powered by AndroidX Media3 / ExoPlayer with a fully synchronized MediaSession.
* **Playback Speed & Skip Silence** — Fine-tune playback speed from 0.5× to 2.0× and automatically bypass dead silence.

---

### 🎨 Visuals & Liquid Aesthetics
* **Pure Material 3 (Material You)** — Built strictly on Android's latest design guidelines with soft curves, tonal elevation, and pill-shaped navigation.
* **Dynamic Artwork Palette** — Liquid color theming dynamically extracted from album covers on every track change.
* **Frosted-Glass UI (Haze)** — Silky, translucent navigation and bottom bars with real-time blurred backdrops.
* **Next-Gen Interactive Sliders** — Liquid physics seek bars and volume sliders with subtle haptic ticks and expand-on-touch ergonomics.
* **Live Word-by-Word Sing-along Lyrics** — Syllable-level synchronized lyrics highlighting with support for multiple providers (Better Lyrics, LrcLib, YouTube).
* **Animated Album Canvas** — Immersive motion artwork backdrop in the player view.
* **Stats for Nerds** — Real-time stream telemetry displaying format, codec, sample rate, bit depth, and cache status.

---

### 🧭 Explore: Moods & Genres, Charts & New Releases *(New in v2.0)*
Dedicated **Explore** tab in the floating bottom navigation bar featuring 4 main discovery avenues and 50+ specialized moods and genres:

* **Top Discovery Rails**:
  - 🌟 **Moods & genres** — Curated catalogs across *For you*, *Moods & moments*, and *Genres*.
  - 💿 **New releases** — Real-time live drops of new albums, singles, and music videos.
  - 📈 **Charts & Trending** — Numbered top 20 trending singles and global charts.
  - 🎙️ **Podcasts & Long Listens** — Popular news, geopolitics,     storytelling, and 1–2 hour continuous jukeboxes.

#### 🎧 Moods & Genres Taxonomy

| Section | Categories & Highlights |
|---|---|
| **For you** | Romance 💖 · Hindi 🎬 · Feel good ✨ · Desi hip-hop 🔥 · Hip-hop 🎧 · Devotional 🕉️ |
| **Moods & moments** | Chill ☕ · Commute 🚗 · Energize ⚡ · Feel good 🌟 · Focus 📚 · Gaming 🎮 · Party 🎉 · Romance 💖 · Sad 💔 · Sleep 🌙 · Workout 🏋️ |
| **Regional & Global Genres** | Bhojpuri 🌾 · Haryanvi 🚜 · Punjabi 🪘 · Bengali 🪕 · Gujarati 🎭 · Marathi 🚩 · Tamil 🛕 · Telugu 🏹 · Kannada 🌴 · Malayalam 🛶 · Indian Indie 🌿 · Indian Pop 🌟 · Ghazal/Sufi 🕯️ · Hindustani Classical 🪕 · Carnatic Classical 🎻 · Monsoon 🌧️ · K-Pop 💜 · J-Pop 🌸 · Arabic 🏜️ · African 🌍 · Latin 💃 · Metal ⚡ · Jazz 🎷 · Dance & Electronic 🪩 · Decades (60s–2000s) ⏳ |

---

### 🇮🇳 Indian Cultural Heritage & Utsav Mode
* **Utsav Mode** — Dedicated seasonal festival celebrations celebrating Diwali, Holi, Navratri, Eid, Christmas, Shivratri, and seasonal Ritus.
* **Native Multi-Language Support** — Full localization across English, Hindi (हिन्दी), Hinglish, Punjabi (ਪੰਜਾਬੀ), Tamil, Telugu, Spanish, French, German, Japanese, and Russian.
* **Smart Voice Search** — Mic input with instant voice search recognition and recent search history management.

---

### 🌐 Connectivity, Accounts & Sync
* **YouTube Music Sync** — Optional Google account login to sync your saved playlists, liked tracks, and listening history.
* **Listen Together** — Create or join synchronized group listening rooms with friends using room codes.
* **Discord Rich Presence** — Showcase what you're listening to on Discord with live album artwork and track progress.
* **Scrobbling** — Native integration with Last.fm and ListenBrainz.
* **Playlist Import & Export** — Export playlists to JSON/M3U and import playlists seamlessly.

---

### 💾 Offline Downloads & Library
* **Full Metadata Embedding** — Downloaded FLAC/M4A/MP3 files automatically embed high-res cover art, artist, album, and lyric tags.
* **Local Device Storage Player** — Scan and play music files stored directly on your phone's internal memory or SD card.

---

## 📦 Download & Installation

### Option 1: Direct GitHub Release (Recommended)
Grab the latest signed APK directly from the [Releases Page](https://github.com/Kanaiya-rgb/Dhvani-Music/releases/latest):
* **[📥 Download DhvaniMusic-v2.0.0.apk](https://github.com/Kanaiya-rgb/Dhvani-Music/releases/download/v2.0.0/DhvaniMusic-v2.0.0.apk)** (Latest v2.0.0)
* Repository file: [`apk/DhvaniMusic-v2.0.0.apk`](apk/DhvaniMusic-v2.0.0.apk)

### Installation Steps
1. Download **`DhvaniMusic-v2.0.0.apk`**.
2. Open your device's **Settings → Apps → Special app access → Install unknown apps** and allow installation for your browser/file manager.
3. Tap on the downloaded APK file and click **Install**.
4. Open **Dhvani Music** and immerse yourself in clean, ad-free music!

---

## ⚙️ Setup & Optimization

### 1. Disable Battery Optimization (Important)
Android aggressively throttles background network connections for third-party music apps:
1. Go to your phone's **Settings → Apps → Dhvani Music → Battery**.
2. Select **Unrestricted** (or "Don't optimize").
3. This ensures unbroken playback when your screen is locked and prevents stream cut-offs.

### 2. Android Auto Setup
To use Dhvani Music on your vehicle's head unit via Android Auto:
1. Open **Android Auto Settings** on your phone.
2. Scroll to **Version** and tap it 10 times consecutively to enable **Developer Settings**.
3. Tap the three-dot menu at top-right → **Developer Settings**.
4. Check **Unknown sources**.
5. Connect to your vehicle; Dhvani Music will appear in your media dashboard.

---

## 🛠️ Build from Source

### Prerequisites
* Android Studio Ladybug (or newer) / IntelliJ IDEA
* JDK 17 or higher
* Android SDK 36 (compileSdk 36, minSdk 26)
* Android NDK `28.2.13676358` with CMake 3.22.1

### Build Instructions
```bash
# Clone the repository
git clone https://github.com/Kanaiya-rgb/Dhvani-Music.git
cd Dhvani-Music

# Build debug APK on Windows (PowerShell)
.\gradlew assembleDevDebug

# Built APK will be located at:
# app/build/outputs/apk/dev/debug/app-dev-debug.apk
```

---

## ❓ Frequently Asked Questions

<details>
<summary><b>Q: Do I need a YouTube Music Premium account?</b></summary>
<br/>
No. Dhvani Music streams freely using public and authenticated YouTube Music InnerTube APIs. All features (background playback, ad-blocking, skip tracks, high quality) work without any paid subscription.
</details>

<details>
<summary><b>Q: How does Lossless Audio streaming work?</b></summary>
<br/>
Dhvani Music supports pluggable module resolvers (FLAC/ALAC up to 24-bit/192kHz). When enabled in Settings, the app queries the lossless catalog and matches tracks deterministically via ISRC. If a lossless stream is unavailable, it falls back seamlessly to YouTube Music's standard AAC stream without interruption.
</details>

<details>
<summary><b>Q: Can my Google / YouTube account get banned?</b></summary>
<br/>
Dhvani Music communicates using standard read-only client protocols. It does not manipulate watch hours, generate bot plays, or abuse platform resources. However, you can freely use Dhvani Music completely signed out without logging into any Google account.
</details>

<details>
<summary><b>Q: Does Dhvani work with Bluetooth headphones and AirPods?</b></summary>
<br/>
Yes. Audio playback is routed through the standard Android AudioManager and Media3 ExoPlayer stack, fully supporting Bluetooth codecs (AAC, LDAC, aptX, SBC), media key controls (play/pause/next/previous), and car audio head units.
</details>

---

## 🙏 Credits & Acknowledgements

Dhvani Music is built with immense gratitude to the open-source community and is inspired by these remarkable projects:

* **[BitChord](https://github.com/kushagrasinghx/BitChord)** by [Kushagra Singh](https://github.com/kushagrasinghx) — For pioneering architectural foundations, sophisticated audio playback pipelines, YouTube Music stream integration, and inspiring aesthetic UI concepts.
* **[Meld](https://github.com/FrancescoGrazioso/Meld)** by [Francesco Grazioso](https://github.com/FrancescoGrazioso) — For elegant Material 3 design implementations, intuitive playlist & library management workflows, and exceptional contributions to the open-source Android music ecosystem.
* **[NewPipe](https://github.com/TeamNewPipe/NewPipeExtractor)** — For robust, lightweight YouTube stream extraction infrastructure.
* **[Jetpack Compose](https://developer.android.com/jetpack/compose)** & **[AndroidX Media3](https://developer.android.com/media/media3)** — The modern foundation powering our audio engine and UI components.

---

## ⚖️ Disclaimer & Legal Notice

**Dhvani Music is an independent, community-driven third-party audio player and client.** It is not associated with, sponsored by, or affiliated with Google LLC, YouTube, Alphabet Inc., Spotify AB, Deezer, or any of their subsidiary entities.

* **No Media Hosting**: Dhvani Music does not host, upload, scrape, or store copyrighted music files on any server. It acts purely as a local client-side interface communicating with public, public-facing, or user-authenticated APIs.
* **Fair Use & Educational Purpose**: This software is developed solely for personal research, educational study, and fair-use purposes. End users are individually responsible for ensuring their usage complies with regional copyright legislation and applicable terms of service.
* **Copyleft**: Dhvani Music is free open-source software licensed under the **GNU General Public License v3.0 (GPLv3)**. Any redistribution or derivative work must remain publicly available under the exact same GPLv3 license terms.

---

## 📜 License

This project is licensed under the **GNU General Public License v3.0**.  
See the [LICENSE](LICENSE) file for details.

---

<div align="center">

Made with ❤️ by [Kanaiya-rgb](https://github.com/Kanaiya-rgb)  
**Dhvani Music Community** • [GitHub Issues](https://github.com/Kanaiya-rgb/Dhvani-Music/issues) • [Releases](https://github.com/Kanaiya-rgb/Dhvani-Music/releases)

</div>
