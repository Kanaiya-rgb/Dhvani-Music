<div align="center">

<br/>

<img src="Logo.png" alt="Flux Music Logo" width="180" />

<br/>
<br/>

# Flux Music

### Next-Gen Aesthetic Music Streaming Client for Android

<br/>

[![Platform](https://img.shields.io/badge/Platform-Android%206.0+-3DDC84?style=for-the-badge&logo=android&logoColor=white&labelColor=0d1117)](https://www.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white&labelColor=0d1117)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack-Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white&labelColor=0d1117)](https://developer.android.com/jetpack/compose)
[![Media3](https://img.shields.io/badge/Media3-ExoPlayer-E65100?style=for-the-badge&logo=googleplay&logoColor=white&labelColor=0d1117)](https://developer.android.com/media/media3)
[![APK Size](https://img.shields.io/badge/APK%20Size-~21%20MB-22c55e?style=for-the-badge&logo=files&logoColor=white&labelColor=0d1117)](#download)
[![License](https://img.shields.io/badge/License-GPLv3-ef4444?style=for-the-badge&labelColor=0d1117)](LICENSE)

<br/>

[**Features**](#-features) · [**Screenshots**](#-screenshots) · [**Download**](#-download) · [**Build from Source**](#-build-from-source) · [**Tech Stack**](#-tech-stack) · [**Credits**](#-credits--special-thanks) · [**License**](#-license)

<br/>

</div>

> [!NOTE]
> **Flux Music** is an open-source, independent Android music client. It streams songs from **YouTube Music** and **JioSaavn** — with zero ads, zero account requirements, and a beautiful modern UI. Built for audiophiles who want full control of their listening experience.

---

<div align="center">

<img src="Banner.png" alt="Flux Music — Banner" width="100%" />

</div>

---

## ✨ Features

<table>
  <tr>
    <td width="50%" valign="top">

### 🎧 Audio & Playback
- **Dual Streaming Engine** — YouTube Music (Innertube API) + JioSaavn with seamless auto-fallback.
- **Lossless / Hi-Res Audio** — FLAC, ALAC, and high-bitrate Opus support.
- **Gapless Playback & Crossfade** — Smooth transitions with 0–12s adjustable crossfade.
- **Automix [BETA]** — Native C++ beat-detection & tempo analyzer for DJ-style auto-transitions.
- **Skip Silence** — Automatically trims silent gaps longer than 1 second.
- **Spatial Audio** — Stereo widening for a more immersive feel.
- **Offline Downloads** — Save any song locally with embedded artwork, tags, and lyrics (Wi-Fi only option).
- **Background Playback** — Lock-screen controls with Android 13+ media notification & widget support.

### 🎤 Lyrics
- **Synced Lyrics** — Real-time word-by-word highlight as songs play.
- **Multiple Animation Styles** — Fade, Glow, Slide, Karaoke, and Apple Music-style.
- **Tap to Seek** — Tap any lyric line to jump to that moment.
- **Auto-Scroll** — Lyrics follow along automatically.
- **Configurable Sources** — Multiple lyric providers with fallback.

    </td>
    <td width="50%" valign="top">

### 🎨 Design & Aesthetics
- **Apple Music-Inspired UI** — Modern frosted glass, translucent navigation, Material 3 typography.
- **Dynamic Artwork Palettes** — Mesh gradients auto-generated from your album cover.
- **Live Canvas & Motion Art** — Video and animated backdrops during playback.
- **Customizable Player** — Choose from Wavy, Slim, Squiggly, or Default slider styles.
- **Player Background Modes** — Gradient, Blur, or theme-follow.
- **Multiple Themes** — Dark, Light, System, and Pure Black AMOLED.

### ⚡ Discovery & Library
- **Intelligent Home Feed** — Personalized, adaptive discovery rotating on every launch based on your activity, liked tracks, and trending genres.
- **Guest Mode** — Full discovery without signing into a Google account.
- **YouTube Music Sign-In** — Sync your liked songs, playlists, and history.
- **Spotify Canvas Support** — View animated Spotify canvases on compatible tracks.
- **Discord Rich Presence** — Show what you're listening to on Discord.
- **Last.fm Scrobbling** — Auto-scrobble to your Last.fm profile.

### 📊 Nerd Stats & Song Inspector
- **Technical Details** — Codec, bitrate, sample rate, itag, MIME type, loudness, file size, live views, likes & dislikes (via Return YouTube Dislike API).
- **Structured Credits** — Singers, Lyricists, Composers, Album info at a glance.
- **Tap-to-Copy** — Instantly copy any field.

    </td>
  </tr>
</table>

---

## 📸 Screenshots

<div align="center">

| Home & Discovery | Now Playing | Synced Lyrics | Library |
|:-:|:-:|:-:|:-:|
| Dynamic personalized feed | Full-screen artwork with DSP controls | Word-by-word glow highlights | Playlists, Albums & Artists |

</div>

---

## 📦 Download

### Latest Release — `v1.5.0`

> [!IMPORTANT]
> Flux Music is distributed as a sideloaded APK. You will need to **enable "Install from Unknown Sources"** in your Android settings before installing.

**[⬇️ Download Flux-Music-v1.5.apk](https://github.com/Kanaiya-rgb/Flux-Music/releases/latest)**

Or grab it directly from this repo:

```
Flux-Music-v1.5.apk
```

### Installation Steps

1. Download the APK from the link above.
2. On your Android device, go to **Settings → Security → Install Unknown Apps**.
3. Allow your file manager or browser to install APKs.
4. Open the downloaded APK and tap **Install**.
5. Done! Open **Flux Music** and start listening. 🎵

> [!TIP]
> Flux Music has a **built-in auto-updater** — when a new release is published on GitHub, the app will notify you and let you download and install the update directly from within the app.

---

## 🔧 Build from Source

### Prerequisites

| Tool | Minimum Version |
|---|---|
| Android Studio | Hedgehog (2023.1.1) or newer |
| JDK | 17+ |
| Android SDK | API 34+ |
| Kotlin | 2.3.x |
| Gradle | 8.x |

### 1. Clone the Repository

```bash
git clone https://github.com/Kanaiya-rgb/Flux-Music.git
cd Flux-Music
```

### 2. Build the APK

```bash
# Debug build (for testing)
./gradlew assembleProdDebug

# Release build (for distribution)
./gradlew assembleProdRelease
```

**Windows users:**
```powershell
.\gradlew.bat assembleProdDebug
# or
.\gradlew.bat assembleProdRelease
```

### 3. Locate the Built APK

| Build Type | Output Path |
|---|---|
| Debug | `app/build/outputs/apk/prod/debug/app-prod-debug.apk` |
| Release (unsigned) | `app/build/outputs/apk/prod/release/app-prod-release-unsigned.apk` |

### 4. Sign the Release APK (Optional)

```bash
# Using your own keystore
apksigner sign \
  --ks your-keystore.jks \
  --ks-pass pass:your-password \
  --out Flux-Music-signed.apk \
  app/build/outputs/apk/prod/release/app-prod-release-unsigned.apk

# Or use the debug keystore for testing
apksigner sign \
  --ks ~/.android/debug.keystore \
  --ks-pass pass:android \
  --out Flux-Music-v1.5.apk \
  app/build/outputs/apk/prod/release/app-prod-release-unsigned.apk
```

### 5. Publish a GitHub Release

1. Go to your repository → **Releases** → **Draft a new release**.
2. Create a new tag: `v1.5.0` — Target branch: `main`.
3. Title: `Flux Music v1.5.0 — Stable`.
4. Upload your signed APK as a release asset.
5. Click **Publish release** — users can now download directly from your GitHub page!

---

## 🛠 Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Kotlin 2.3 |
| **UI Framework** | Jetpack Compose + Material 3 |
| **Audio Engine** | Media3 / ExoPlayer |
| **DI** | Hilt |
| **Networking** | OkHttp 4 + Kotlinx Serialization |
| **Image Loading** | Coil 3 |
| **Blur / Glass UI** | Haze |
| **Native DSP** | C++ (CMake) — beat-detection & tempo analysis |
| **Stream Extraction** | NewPipe Extractor (YouTube) |
| **Spotify Integration** | Custom Spotify module |
| **Async** | Kotlin Coroutines + Flow |
| **Persistence** | Room + DataStore |
| **Lyrics Sources** | LrcLib, KuGou, Embedded tags |

---

## 🤝 Contributing

Contributions are welcome! Whether it's a bug fix, feature request, or UI improvement — feel free to open an issue or pull request.

1. Fork the repository.
2. Create a new branch: `git checkout -b feature/my-feature`.
3. Commit your changes: `git commit -m "Add: my feature"`.
4. Push to your branch: `git push origin feature/my-feature`.
5. Open a **Pull Request** on GitHub.

> [!NOTE]
> Please follow the existing code style (Kotlin, Jetpack Compose) and make sure the app builds successfully before submitting a PR.

---

## ❤️ Credits & Special Thanks

Flux Music is built on the shoulders of incredible open-source projects:

<table>
  <tr>
    <td align="center" width="50%">
      <a href="https://github.com/kushagrasinghx/BitChord">
        <h3>🎵 BitChord</h3>
      </a>
      <p>by <strong><a href="https://github.com/kushagrasinghx">@kushagrasinghx</a></strong></p>
      <p>Foundational player UI architecture, Automix DSP integration, Spotify Canvas support, and the core aesthetic design system.</p>
      <a href="https://github.com/kushagrasinghx/BitChord">
        <img src="https://img.shields.io/badge/GitHub-BitChord-181717?style=for-the-badge&logo=github" />
      </a>
    </td>
    <td align="center" width="50%">
      <a href="https://github.com/FrancescoGrazioso/Meld">
        <h3>⚡ Meld</h3>
      </a>
      <p>by <strong><a href="https://github.com/FrancescoGrazioso">@FrancescoGrazioso</a></strong></p>
      <p>Dynamic personalized recommendation engine, real-time discovery feeds, appearance customization settings, and the sleek song details inspector architecture.</p>
      <a href="https://github.com/FrancescoGrazioso/Meld">
        <img src="https://img.shields.io/badge/GitHub-Meld-181717?style=for-the-badge&logo=github" />
      </a>
    </td>
  </tr>
</table>

### Additional Acknowledgments

| Library / API | Purpose |
|---|---|
| [NewPipe Extractor](https://github.com/TeamNewPipe/NewPipeExtractor) | YouTube stream parsing & metadata extraction |
| [Return YouTube Dislike API](https://returnyoutubedislikeapi.com/) | Live dislike counts in the song inspector |
| [Haze](https://github.com/chrisbanes/haze) | High-performance frosted glass blur in Compose |
| [LrcLib](https://lrclib.net/) | Synced & plain lyrics provider |
| [Last.fm API](https://www.last.fm/api) | Scrobbling support |

---

## ⚖️ Disclaimer & Legal Notice

Flux Music is an independent, community-driven third-party audio player and client. It is **not** affiliated with, endorsed by, or associated with Google LLC, YouTube Music, JioSaavn, Spotify, or any of their parent entities.

- **No Media Hosting** — Flux Music does not host, store, or redistribute any copyrighted media files. It operates strictly as an interface to stream content available through public APIs and services.
- **Personal & Educational Use** — This software is intended strictly for personal research, educational, and fair-use experimentation purposes.
- **User Responsibility** — The end user is solely responsible for complying with their local laws, regulations, and third-party terms of service when using this application.

---

## 📜 License

<div align="center">

This project is licensed under the **GNU General Public License v3.0 (GPLv3)**.

See the [LICENSE](LICENSE) file for full details.

<br/>

[![GitHub stars](https://img.shields.io/github/stars/Kanaiya-rgb/Flux-Music?style=for-the-badge&logo=github&labelColor=0d1117&color=f59e0b)](https://github.com/Kanaiya-rgb/Flux-Music/stargazers)
[![GitHub forks](https://img.shields.io/github/forks/Kanaiya-rgb/Flux-Music?style=for-the-badge&logo=github&labelColor=0d1117&color=6366f1)](https://github.com/Kanaiya-rgb/Flux-Music/network/members)
[![GitHub issues](https://img.shields.io/github/issues/Kanaiya-rgb/Flux-Music?style=for-the-badge&logo=github&labelColor=0d1117&color=22c55e)](https://github.com/Kanaiya-rgb/Flux-Music/issues)

<br/>

Made with ❤️ by [Kanaiya-rgb](https://github.com/Kanaiya-rgb)

</div>
