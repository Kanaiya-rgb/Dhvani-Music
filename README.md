<div align="center">

<br/>
<br/>

<img src="Logo.png" alt="Flux Music app icon" width="190" />

# Flux Music

### Next-Gen Aesthetic YouTube Music & JioSaavn Streaming Client

<br/>

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white&labelColor=0d1117)](https://www.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white&labelColor=0d1117)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack-Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white&labelColor=0d1117)](https://developer.android.com/jetpack/compose)
[![Media3](https://img.shields.io/badge/ExoPlayer-Media3-E65100?style=for-the-badge&logo=googleplay&logoColor=white&labelColor=0d1117)](https://developer.android.com/media/media3)
[![License](https://img.shields.io/badge/License-GPLv3-red?style=for-the-badge&labelColor=0d1117)](LICENSE)

<br/>

[**Features**](#features) · [**Download**](#download) · [**GitHub Upload Guide**](#github-upload-guide) · [**Credits & Special Thanks**](#credits--special-thanks) · [**Disclaimer**](#disclaimer) · [**License**](#license)

</div>

> [!NOTE]
> Flux Music is an independent, community-crafted streaming client designed for audiophiles and music lovers. It delivers a fast, fluid, and personalized listening experience with zero intrusive interruptions.

---

<div align="center">

<img src="Banner.png" alt="Flux Music banner" width="100%" />

<h1><a id="features"></a>✨ Features</h1>

<table>
  <tr>
    <td width="50%" valign="top">

#### 🎧 Playback & Dual Engine
- **Dual Streaming Engine** — seamless streaming backed by both **YouTube Music (Innertube)** and **JioSaavn** with instant auto-fallback.
- **Hi-Res Lossless Audio** — Module pipeline supporting FLAC, ALAC, and high-bitrate Opus streams.
- **Gapless Playback & Crossfade** — Smooth audio transitions with customizable 0–12s crossfade.
- **Automix DSP Analyzer** — Native C++ beat-detection and tempo analyzer (`src/main/cpp`) for seamless DJ-style transitions.
- **Offline Downloads** — Save tracks locally with embedded artwork, tags, and lyrics.
- **Background Media Session** — Seamless playback with lock-screen controls and Android 13+ media notification.

#### 🎨 Visuals & Aesthetics
- **Apple Music Inspired Design** — Modern frosted glass, translucent navigation, and rich Material 3 typography.
- **Artwork-Driven Palettes** — Dynamic mesh gradients automatically tailored to the album cover.
- **Live Canvas & Motion Artwork** — Immersive video and visual backdrops during playback.
- **Synchronized Lyrics** — Real-time timed lyrics with word-by-word highlights and translation support.

    </td>
    <td width="50%" valign="top">

#### ⚡ Dynamic Discovery & Recommendations
- **Intelligent Dynamic Feed** — Inspired by Meld's adaptive discovery model, the home page rotates dynamically on every launch based on your recent activity, liked tracks, and trending genres.
- **Prioritized Listening** — Puts your most recent and most-played tracks right at the top for instant resumption.
- **Zero Account Dependency** — Enjoy fresh Indian and global discovery even without signing into a Google account.

#### 📊 Advanced Song Details & Nerd Stats
- **Clean 3-Section Inspector**:
  - **General**: Title, Artists, and Media ID with instant tap-to-copy.
  - **Technical Specs**: Live Views, Likes, and **Dislikes** (powered by Return YouTube Dislike API), Itag, MIME type, Codecs, Bitrate, Sample Rate, Loudness (dB), Volume, and File Size.
  - **Structured Credits**: Clear separation for Singers, Lyricists, Composers, and Album info.
- **Instant 0ms Loading** — Zero-lag opening with in-memory caching and parallel non-blocking requests.

    </td>
  </tr>
</table>

</div>

---

<div align="center">

<h1><a id="download"></a>📦 Download & Installation</h1>

</div>

### Get the APK
Grab the latest built APK directly from the project build folder or GitHub Releases:
- **Dev Debug APK**: `app/build/outputs/apk/dev/debug/app-dev-debug.apk`
- **Production APK**: `app/build/outputs/apk/prod/debug/app-prod-debug.apk`

### Building From Source

```bash
# Clone the repository
git clone https://github.com/<your-username>/Flux-Music.git
cd Flux-Music

# Build Debug APK
./gradlew assembleProdDebug

# Build Release APK
./gradlew assembleProdRelease
```

---

<div align="center">

<h1><a id="github-upload-guide"></a>🚀 GitHub Upload Guide</h1>

</div>

A quick step-by-step guide to publishing this project on your personal GitHub profile:

### 1. Create a New GitHub Repository
1. Open [GitHub](https://github.com/new).
2. Repository name: `Flux-Music` (or any name you prefer).
3. Set visibility to **Public**.
4. **Important**: Leave "Add a README file" and ".gitignore" **unchecked** (they are already included in this repo).
5. Click **Create repository**.

### 2. Push Your Local Code to GitHub
Open PowerShell in this folder and run:

```powershell
# 1. Stage all project files (.gitignore will protect keys and local builds)
git add .

# 2. Commit your code
git commit -m "Initial commit: Flux Music with dynamic feed, lossless DSP and advanced song details"

# 3. Connect your GitHub remote repository (replace with your repo URL)
git remote add origin https://github.com/<your-username>/Flux-Music.git

# 4. Push to main branch
git push -u origin main
```

### 3. Creating a Release with Downloadable APK
1. Build the production APK:
   ```powershell
   .\gradlew.bat :app:assembleProdDebug
   ```
2. In your GitHub repository, click on **Releases** (on the right sidebar) ➔ **Draft a new release**.
3. Create a new tag: `v1.5.0` ➔ Title: `Flux Music v1.5.0 - Stable Release`.
4. Drag and drop the generated APK file from:
   `app/build/outputs/apk/prod/debug/app-prod-debug.apk`
5. Click **Publish release** — your users can now directly download and install the APK!

---

<div align="center">

<h1><a id="credits--special-thanks"></a>❤️ Credits & Special Thanks</h1>

Flux Music is built on the shoulders of giants. Immense gratitude and credit to these incredible open-source projects and creators:

<table>
  <tr>
    <td align="center" width="50%">
      <a href="https://github.com/kushagrasinghx/BitChord">
        <h3>🎵 BitChord</h3>
      </a>
      <p>Created by <strong><a href="https://github.com/kushagrasinghx">@kushagrasinghx</a></strong></p>
      <p>Huge credit and appreciation for the stunning player UI architecture, Automix DSP integration, canvas support, and foundational aesthetic design.</p>
      <a href="https://github.com/kushagrasinghx/BitChord">
        <img src="https://img.shields.io/badge/GitHub-BitChord-181717?style=for-the-badge&logo=github" />
      </a>
    </td>
    <td align="center" width="50%">
      <a href="https://github.com/FrancescoGrazioso/Meld">
        <h3>⚡ Meld</h3>
      </a>
      <p>Created by <strong><a href="https://github.com/FrancescoGrazioso">@FrancescoGrazioso</a></strong></p>
      <p>Special thanks for the dynamic personalized recommendation engine, real-time discovery feeds, and the sleek media info / song details architecture.</p>
      <a href="https://github.com/FrancescoGrazioso/Meld">
        <img src="https://img.shields.io/badge/GitHub-Meld-181717?style=for-the-badge&logo=github" />
      </a>
    </td>
  </tr>
</table>

### Additional Acknowledgments
- **[NewPipe Extractor](https://github.com/TeamNewPipe/NewPipeExtractor)** — Core stream parsing and metadata extraction.
- **[Return YouTube Dislike API](https://returnyoutubedislikeapi.com/)** — Powering live dislike counts in the technical inspector.
- **[Haze](https://github.com/chrisbanes/haze)** — High-performance frosted glass blur for Jetpack Compose.

</div>

---

<div align="center">

<h1><a id="disclaimer"></a>⚖️ Disclaimer & Legal Notice</h1>

Flux Music is an independent, community-driven third-party audio player and client. It is **not** associated with Google LLC, YouTube Music, JioSaavn, or any of their parent entities.

- **No Media Hosting**: Flux Music does not host, store, or redistribute any copyrighted media files. It operates strictly as an interface to stream content available through public APIs.
- **Educational & Personal Use**: This software is intended strictly for personal research, educational, and fair-use experimentation.
- **Compliance**: The user remains responsible for complying with local regulations and third-party terms of service.

</div>

---

<div align="center">

<h1><a id="license"></a>📜 License</h1>

This project is licensed under the **GNU General Public License v3.0 (GPLv3)**. See the [LICENSE](LICENSE) file for complete details.

</div>
